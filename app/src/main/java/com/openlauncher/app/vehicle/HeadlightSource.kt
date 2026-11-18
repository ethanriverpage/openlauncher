package com.openlauncher.app.vehicle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.tw.carinfoservice.CarServiceAidl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** What the car reports about its lights. UNAVAILABLE = no service, or the MCU hasn't said. */
enum class Headlight { UNAVAILABLE, ON, OFF }

/**
 * Headlight state from the Topway/TW head unit's `com.tw.carinfoservice` (system app).
 *
 * The service relays MCU message 516 (arg1 == 1 → lights on). It counts parking lights and low
 * beam alike and does not distinguish high beam. The initial value comes from getHeadlight()
 * (-1 invalid, 1 on, 0 off); changes arrive through CarServiceCallBack.onHeadlight.
 *
 * The binding lives here, not in a widget, so later features (speed, RPM, gear) can reuse it.
 * The service gets killed and restarted on these units; a lost binder rebinds with backoff and the
 * last known value is held for [UNAVAILABLE_GRACE_MS] so a restart doesn't flip the theme.
 */
class HeadlightSource(private val context: Context, scope: CoroutineScope) {

    companion object {
        private const val TAG = "OL-Headlight"
        private const val BIND_ACTION = "com.tw.carinfoservice.CarService.Bind"
        private const val SERVICE_PACKAGE = "com.tw.carinfoservice"

        /** Lights on → night after this long (filters lock/unlock flashes). */
        const val ON_DEBOUNCE_MS = 1_000L
        /** Lights off → day only after they've stayed off this long (brief toggles, tunnels of habit). */
        const val OFF_DEBOUNCE_MS = 10_000L
        /** How long a lost service may keep its last value before we report UNAVAILABLE. */
        const val UNAVAILABLE_GRACE_MS = 30_000L

        private const val BACKOFF_START_MS = 1_000L
        private const val BACKOFF_MAX_MS = 60_000L
    }

    private val _raw = MutableStateFlow(Headlight.UNAVAILABLE)
    /** Undebounced state, exactly as the service reports it. */
    val raw: StateFlow<Headlight> = _raw.asStateFlow()

    private val _connected = MutableStateFlow(false)
    /** True while bound to the service. Later readers (speed, RPM) can key off this. */
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    // Declared before [state]: its collector may start during construction
    @Volatile private var settled = Headlight.UNAVAILABLE

    /**
     * Debounced state the theme should follow: ON after [ON_DEBOUNCE_MS], OFF after
     * [OFF_DEBOUNCE_MS], UNAVAILABLE after [UNAVAILABLE_GRACE_MS] without a valid reading.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<Headlight> = _raw.transformLatest { h ->
        // The first valid reading (startup, or after a long outage) applies at once; debouncing
        // only smooths changes between known states
        val wait = when {
            h == settled -> 0L
            settled == Headlight.UNAVAILABLE && h != Headlight.UNAVAILABLE -> 0L
            h == Headlight.ON -> ON_DEBOUNCE_MS
            h == Headlight.OFF -> OFF_DEBOUNCE_MS
            else -> UNAVAILABLE_GRACE_MS
        }
        delay(wait)
        if (settled != h) Log.i(TAG, "Theme signal $settled -> $h")
        settled = h
        emit(h)
    }.stateIn(scope, SharingStarted.Eagerly, Headlight.UNAVAILABLE)

    private val main = Handler(Looper.getMainLooper())
    private var service: CarServiceAidl? = null
    private var bound = false
    private var released = false
    private var backoffMs = BACKOFF_START_MS

    private val callback = object : CarServiceCallBackAdapter() {
        override fun onHeadlight(mHeadlight: Boolean) {
            report(if (mHeadlight) Headlight.ON else Headlight.OFF, "callback")
        }
    }

    private val deathRecipient = IBinder.DeathRecipient {
        Log.w(TAG, "CarInfoService binder died")
        main.post { onLost("binderDied") }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null) { onLost("null binder"); return }
            val s = CarServiceAidl.Stub.asInterface(binder)
            try {
                binder.linkToDeath(deathRecipient, 0)
                s.registerCarServiceCallBack(callback)
                service = s
                _connected.value = true
                backoffMs = BACKOFF_START_MS
                val initial = s.headlight
                Log.i(TAG, "CarInfoService connected; getHeadlight() = $initial")
                report(
                    when (initial) { 1 -> Headlight.ON; 0 -> Headlight.OFF; else -> Headlight.UNAVAILABLE },
                    "getHeadlight"
                )
            } catch (e: RemoteException) {
                Log.w(TAG, "CarInfoService call failed on connect", e)
                onLost("remote exception")
            } catch (e: SecurityException) {
                Log.w(TAG, "CarInfoService refused the binding (permission?)", e)
                onLost("security exception")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "CarInfoService disconnected")
            onLost("disconnected")
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.w(TAG, "CarInfoService binding died")
            onLost("binding died")
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.w(TAG, "CarInfoService returned a null binding")
            onLost("null binding")
        }
    }

    init { bind() }

    private fun report(h: Headlight, from: String) {
        if (_raw.value != h) Log.i(TAG, "Headlight ${_raw.value} -> $h ($from)")
        _raw.value = h
    }

    private val serviceInstalled: Boolean by lazy {
        runCatching { context.packageManager.getPackageInfo(SERVICE_PACKAGE, 0) }.isSuccess
            .also { if (!it) Log.i(TAG, "$SERVICE_PACKAGE not installed; headlight signal unavailable") }
    }

    private fun bind() {
        if (released || bound) return
        if (!serviceInstalled) return   // not a TW unit: stay UNAVAILABLE, never retry
        val intent = Intent(BIND_ACTION).setPackage(SERVICE_PACKAGE)
        bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.w(TAG, "bindService refused", e); false
        }
        if (bound) Log.i(TAG, "Binding CarInfoService")
        else { Log.w(TAG, "CarInfoService not available; retry in ${backoffMs}ms"); scheduleRebind() }
    }

    /** Drop the dead connection, report UNAVAILABLE (debounced by the grace period), rebind later. */
    private fun onLost(reason: String) {
        if (released) return
        Log.w(TAG, "CarInfoService lost ($reason); rebinding in ${backoffMs}ms")
        teardown()
        report(Headlight.UNAVAILABLE, reason)
        scheduleRebind()
    }

    private fun scheduleRebind() {
        if (released) return
        main.removeCallbacksAndMessages(null)
        main.postDelayed({ bind() }, backoffMs)
        backoffMs = (backoffMs * 2).coerceAtMost(BACKOFF_MAX_MS)
    }

    private fun teardown() {
        service?.let { s ->
            runCatching { s.unRegisterCarServiceCallBack(callback) }
            runCatching { s.asBinder().unlinkToDeath(deathRecipient, 0) }
        }
        service = null
        _connected.value = false
        if (bound) { runCatching { context.unbindService(connection) }; bound = false }
    }

    /** Unregister and unbind; call from the owner's onCleared/onDestroy. */
    fun release() {
        released = true
        main.removeCallbacksAndMessages(null)
        teardown()
        Log.i(TAG, "Released")
    }
}
