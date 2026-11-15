package com.openlauncher.app.climate

/**
 * Decides whether a climate state transition was caused by someone pressing a control on the
 * car's HVAC panel (→ bring the climate UI forward) versus the system adjusting itself
 * (→ stay quiet). The stock com.tw.ac popped on *any* value change, so in AUTO it stole focus
 * every time the car nudged the blower or vents.
 *
 * Rules:
 *  - Only a change between two known values counts; a field's first report (null → value),
 *    e.g. right after connecting or ignition-on, never counts.
 *  - Setpoints and toggles always count: A/C, AUTO, DUAL, SYNC, defrost, temperatures, LO/HI.
 *  - Fan speed, airflow mode and intake (fresh/recirc/auto) count only while AUTO is off —
 *    in AUTO the car changes them on its own (incl. holding the blower at 0 during cold-start
 *    warm-up). Pressing fan/mode/OFF in AUTO also drops AUTO, which counts by itself.
 *  - Rear defrost turning *off* doesn't count: it has its own shut-off timer.
 */
object ClimateFocusPolicy {

    fun isUserChange(prev: ClimateState, next: ClimateState): Boolean {
        if (!prev.connected || !next.connected) return false

        fun <T> changed(a: T?, b: T?) = a != null && b != null && a != b

        if (changed(prev.power, next.power)) return true
        if (changed(prev.ac, next.ac)) return true
        if (changed(prev.auto, next.auto)) return true
        if (changed(prev.dual, next.dual)) return true
        if (changed(prev.sync, next.sync)) return true
        if (changed(prev.frontDefrost, next.frontDefrost)) return true
        if (changed(prev.rearDefrost, next.rearDefrost) && next.rearDefrost != 0) return true
        if (changed(prev.leftTemp, next.leftTemp)) return true
        if (changed(prev.rightTemp, next.rightTemp)) return true
        if (prev.leftLo != next.leftLo || prev.leftHi != next.leftHi) return prev.leftTemp != null || next.leftTemp != null
        if (prev.rightLo != next.rightLo || prev.rightHi != next.rightHi) return prev.rightTemp != null || next.rightTemp != null
        if (changed(prev.seatHeatLeft, next.seatHeatLeft)) return true
        if (changed(prev.seatHeatRight, next.seatHeatRight)) return true
        if (changed(prev.seatVentLeft, next.seatVentLeft)) return true
        if (changed(prev.seatVentRight, next.seatVentRight)) return true

        val autoOn = (next.auto ?: prev.auto ?: 0) != 0
        if (!autoOn) {
            if (changed(prev.fan, next.fan)) return true
            if (changed(prev.mode, next.mode)) return true
            // Compare intake category, not raw bits: in AUTO-intake the car flips recirc itself.
            if (changed(prev.intake, next.intake)) return true
        }
        return false
    }
}
