package com.openlauncher.app.climate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import android.os.SystemClock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Snapshot of the vehicle climate state as reported by the Topway/TW `com.tw.ac.AirService`.
 * Fields are null until the service has reported them (unsupported features stay null).
 */
data class ClimateState(
    val connected: Boolean = false,
    val power: Int? = null,          // F0
    val ac: Int? = null,             // F1
    val recirc: Int? = null,         // F2
    val auto: Int? = null,           // F3
    val dual: Int? = null,           // F4
    val frontDefrost: Int? = null,   // F5
    val rearDefrost: Int? = null,    // F6
    val fan: Int? = null,            // F8 level
    val leftTemp: Float? = null,     // F11
    val rightTemp: Float? = null,    // F14
    val leftLo: Boolean = false,
    val leftHi: Boolean = false,
    val rightLo: Boolean = false,
    val rightHi: Boolean = false,
    val tempUnit: String? = null,
    val mode: Int? = null,           // F17 airflow mode
    val sync: Int? = null,           // F71
    val seatHeatLeft: Int? = null,   // F26
    val seatHeatRight: Int? = null,  // F27
    val seatVentLeft: Int? = null,   // F28
    val seatVentRight: Int? = null,  // F29
    val extras: Map<String, Number> = emptyMap(), // generic Bundle callback (feature id -> value)
)

/**
 * Client for the TW A/C service's AIDL interface, spoken over raw Binder transactions so no
 * .aidl files are needed. Interface mapped from decompiled com.tw.ac 261022 — see
 * ~/gx460-ts10-backup/AC-INTERFACE.md.
 *
 *  - Controller: descriptor `com.tw.ac.IAirConditionController`
 *  - Callback:   descriptor `com.tw.ac.IAirConditionReceive` (we implement it)
 */
class TwClimateClient(context: Context) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(ClimateState())
    val state: StateFlow<ClimateState> = _state.asStateFlow()

    @Volatile private var remote: IBinder? = null
    private var bound = false
    private var refCount = 0

    /**
     * Emits when the climate state changes in a way that only a person pressing a control
     * produces (see [ClimateFocusPolicy]). Used to bring the climate UI forward.
     */
    private val _userChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val userChanges: SharedFlow<Unit> = _userChanges.asSharedFlow()

    /** Ignore the full-state re-push that follows (re)registration. */
    @Volatile private var armedAtMs = Long.MAX_VALUE
    private val stateLock = Any()

    private fun apply(transform: ClimateState.() -> ClimateState) {
        val (prev, next) = synchronized(stateLock) {
            val p = _state.value
            val n = p.transform()
            _state.value = n
            p to n
        }
        if (SystemClock.uptimeMillis() >= armedAtMs && ClimateFocusPolicy.isUserChange(prev, next)) {
            Log.d(TAG, "user climate change detected")
            _userChanges.tryEmit(Unit)
        }
    }

    /** Controller transaction codes (IAirConditionController). */
    enum class Cmd(val code: Int) {
        POWER(4), AC(5), AUTO(6), DUAL(7), FRONT_DEFROST(8), REAR_DEFROST(9), MAX_AC(10),
        RECIRC(29),
        LEFT_TEMP_UP(25), LEFT_TEMP_DOWN(26), RIGHT_TEMP_UP(27), RIGHT_TEMP_DOWN(28),
        FAN_UP(32), FAN_DOWN(33), MODE_CYCLE(30),
        SEAT_HEAT_LEFT(11), SEAT_HEAT_RIGHT(12), SEAT_VENT_LEFT(13), SEAT_VENT_RIGHT(14),
    }

    // ---- Callback binder (IAirConditionReceive) -------------------------------------------

    private val callback = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString(RECEIVE_DESCRIPTOR)
                return true
            }
            if (code < FIRST_CALL_TRANSACTION || code > LAST_CALL_TRANSACTION) {
                return super.onTransact(code, data, reply, flags)
            }
            try {
                data.enforceInterface(RECEIVE_DESCRIPTOR)
                handleCallback(code, data)
            } catch (e: Exception) {
                Log.w(TAG, "callback $code failed", e)
            }
            reply?.writeNoException()
            return true
        }
    }

    private fun handleCallback(code: Int, p: Parcel) {
        when (code) {
            6  -> p.readInt().let { v -> apply { copy(power = v) } }
            7  -> p.readInt().let { v -> apply { copy(ac = v) } }
            5  -> p.readInt().let { v -> apply { copy(recirc = v) } }
            8  -> p.readInt().let { v -> apply { copy(auto = v) } }
            9  -> p.readInt().let { v -> apply { copy(dual = v) } }
            10 -> p.readInt().let { v -> apply { copy(frontDefrost = v) } }
            11 -> p.readInt().let { v -> apply { copy(rearDefrost = v) } }
            4  -> p.readInt().let { v -> apply { copy(fan = v) } }
            1  -> p.readFloat().let { v -> apply { copy(leftTemp = v) } }
            2  -> p.readFloat().let { v -> apply { copy(rightTemp = v) } }
            47 -> p.readString().let { v -> apply { copy(tempUnit = v) } }
            39 -> (p.readInt() != 0).let { v -> apply { copy(leftLo = v) } }
            40 -> (p.readInt() != 0).let { v -> apply { copy(leftHi = v) } }
            41 -> (p.readInt() != 0).let { v -> apply { copy(rightLo = v) } }
            42 -> (p.readInt() != 0).let { v -> apply { copy(rightHi = v) } }
            3  -> p.readInt().let { v -> apply { copy(mode = v) } }
            49 -> p.readInt().let { v -> apply { copy(sync = v) } }
            13 -> p.readInt().let { v -> apply { copy(seatHeatLeft = v) } }
            14 -> p.readInt().let { v -> apply { copy(seatHeatRight = v) } }
            15 -> p.readInt().let { v -> apply { copy(seatVentLeft = v) } }
            16 -> p.readInt().let { v -> apply { copy(seatVentRight = v) } }
            38 -> if (p.readInt() != 0) {
                val b = Bundle.CREATOR.createFromParcel(p)
                val add = b.keySet().mapNotNull { k ->
                    @Suppress("DEPRECATION")
                    (b.get(k) as? Number)?.let { k to it }
                }.toMap()
                if (add.isNotEmpty()) apply { copy(extras = extras + add) }
            }
            else -> Log.v(TAG, "unhandled callback code $code")
        }
    }

    // ---- Connection -------------------------------------------------------------------------

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remote = service
            armedAtMs = SystemClock.uptimeMillis() + ARM_DELAY_MS
            _state.update { it.copy(connected = service != null) }
            // Register → service re-pushes full state (model.e()).
            transact(CODE_REGISTER) { it.writeStrongBinder(callback) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote = null
            armedAtMs = Long.MAX_VALUE
            _state.value = ClimateState(connected = false)
            // Service may restart (it is persistent); the system rebinds automatically.
        }

        override fun onBindingDied(name: ComponentName?) {
            remote = null
            armedAtMs = Long.MAX_VALUE
            _state.value = ClimateState(connected = false)
            unbindInternal()
            main.postDelayed({ if (refCount > 0) bindInternal() }, 3000)
        }
    }

    /** Ref-counted start; call [stop] for each [start]. */
    fun start() {
        refCount++
        if (refCount == 1) bindInternal()
    }

    fun stop() {
        if (refCount == 0) return
        refCount--
        if (refCount == 0) {
            transact(CODE_UNREGISTER) { it.writeStrongBinder(callback) }
            unbindInternal()
            remote = null
            armedAtMs = Long.MAX_VALUE
            _state.value = ClimateState(connected = false)
        }
    }

    private fun bindInternal() {
        val intent = Intent(SERVICE_ACTION).setClassName(SERVICE_PKG, SERVICE_CLASS)
        bound = try {
            appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(TAG, "bindService failed", e)
            false
        }
        if (!bound) {
            Log.i(TAG, "com.tw.ac AirService not available")
            try { appContext.unbindService(connection) } catch (_: Exception) {}
        }
    }

    private fun unbindInternal() {
        if (bound) {
            try { appContext.unbindService(connection) } catch (_: Exception) {}
            bound = false
        }
    }

    /** Sends a control command. Returns false if the service isn't connected. */
    fun send(cmd: Cmd): Boolean = transact(cmd.code)

    private fun transact(code: Int, write: (Parcel) -> Unit = {}): Boolean {
        val b = remote ?: return false
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(CONTROLLER_DESCRIPTOR)
            write(data)
            b.transact(code, data, reply, 0)
            reply.readException()
            true
        } catch (e: Exception) {
            Log.w(TAG, "transact $code failed", e)
            false
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    companion object {
        private const val TAG = "TwClimate"
        const val SERVICE_PKG = "com.tw.ac"
        private const val SERVICE_CLASS = "com.tw.ac.AirService"
        private const val SERVICE_ACTION = "com.tw.ac.AirService.Bind"
        private const val CONTROLLER_DESCRIPTOR = "com.tw.ac.IAirConditionController"
        private const val RECEIVE_DESCRIPTOR = "com.tw.ac.IAirConditionReceive"
        private const val CODE_REGISTER = 2
        private const val CODE_UNREGISTER = 3
        private const val ARM_DELAY_MS = 3000L

        @Volatile private var instance: TwClimateClient? = null
        fun get(context: Context): TwClimateClient =
            instance ?: synchronized(this) {
                instance ?: TwClimateClient(context).also { instance = it }
            }
    }
}
