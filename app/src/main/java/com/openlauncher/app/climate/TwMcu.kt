package com.openlauncher.app.climate

import android.util.Log

/**
 * Sends HVAC panel keys straight to the head unit's MCU through the framework's
 * `android.tw.john.TWUtil` (backed by /dev/tw), exactly as com.tw.ac does for keys it doesn't
 * expose over its AIDL interface — the GX460's rear climate panel among them.
 *
 * Frame (captured from com.tw.ac's own "Send2Mcu" log): TWUtil.write(1281, 0x33, 2, [key, b2])
 *  - toggles:  b2 = 0x22 to switch on, 0xCC to switch off
 *  - up/down:  b2 = 0x00
 * Keys come from the vehicle's AirConfig.xml (AirConditionTheme_98F_251211).
 */
object TwMcu {
    private const val TAG = "TwMcu"
    private const val MSG_SEND = 1281        // 0x501
    private const val DATA_TYPE = 0x33       // AirConfig dataType-send

    const val ON = 0x22
    const val OFF = 0xCC
    const val STEP = 0x00

    // Rear panel keys (vehicle variant with a rear control panel)
    const val KEY_REAR_TEMP_UP = 0x33
    const val KEY_REAR_TEMP_DOWN = 0x34
    const val KEY_REAR_FAN_UP = 0x2D
    const val KEY_REAR_FAN_DOWN = 0x2E
    // No rear airflow key: 0x2F (F50) and 0x2A (F68) both reach the MCU but the GX460
    // ignores them, and the car never reports rear vent mode.
    const val KEY_REAR_POWER = 0x30          // stock UI: "Rear on"
    const val KEY_REAR_AUTO = 0x31

    private var util: Any? = null
    private var writeMethod: java.lang.reflect.Method? = null

    @Synchronized
    private fun ensure(): Boolean {
        if (util != null && writeMethod != null) return true
        return try {
            val cls = Class.forName("android.tw.john.TWUtil")
            val u = cls.getConstructor().newInstance()
            val rc = cls.getMethod("open", ShortArray::class.java)
                .invoke(u, shortArrayOf(MSG_SEND.toShort())) as Int
            Log.i(TAG, "TWUtil.open(1281) -> $rc")
            if (rc != 0) return false
            writeMethod = cls.getMethod(
                "write",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Any::class.java
            )
            util = u
            true
        } catch (e: Throwable) {
            Log.w(TAG, "TWUtil unavailable", e)
            false
        }
    }

    /** Sends one key frame. Returns false if TWUtil isn't available on this unit. */
    @Synchronized
    fun sendKey(key: Int, b2: Int): Boolean {
        if (!ensure()) return false
        val frame = byteArrayOf(key.toByte(), b2.toByte())
        return try {
            val rc = writeMethod!!.invoke(util, MSG_SEND, DATA_TYPE, frame.size, frame) as Int
            Log.d(TAG, "send 0x%02x 0x%02x -> %d".format(key, b2, rc))
            rc >= 0
        } catch (e: Throwable) {
            Log.w(TAG, "write failed", e)
            false
        }
    }

    fun toggle(key: Int, currentlyOn: Boolean) = sendKey(key, if (currentlyOn) OFF else ON)

    fun step(key: Int) = sendKey(key, STEP)
}
