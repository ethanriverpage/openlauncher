package com.openlauncher.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset

/*
 * Motion on the driving surface explains a change and gets out of the way: feedback in
 * ~150 ms, state changes in 200–300 ms, and one slow fade for the day/night switch.
 * Nothing loops on the driving surface; the only repeating motion is the edit-mode jiggle,
 * which only exists while the driver is parked and rearranging.
 *
 * Compose already scales every animation by the system animator duration scale, so
 * "Remove animations" turns tweens and springs into cuts. [rememberReduceMotion] is for the
 * one thing that scale can't express cleanly: whether to run a repeating animation at all.
 */

/** Confident arrival: fast start, long settle (iOS-like ease-out). */
val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

object HigMotion {
    const val FEEDBACK = 150
    const val STATE = 240
    const val THEME = 700

    /** Moving things on the grid: a critically damped settle, no bounce. */
    val settle = spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f)

    /** Small pieces that appear (badges, menus): one soft overshoot, like iOS. */
    fun <T> pop() = spring<T>(dampingRatio = 0.62f, stiffness = 650f)

    fun <T> state() = tween<T>(STATE, easing = EaseOutExpo)
    fun <T> feedback() = tween<T>(FEEDBACK, easing = EaseOutExpo)
}

/**
 * A colour spec that rides the palette while it fades. Without this, every animateColorAsState
 * would retarget on each frame of a day/night fade and trail ~240ms behind the surface it sits on.
 */
@Composable
fun <T> themeAware(spec: androidx.compose.animation.core.AnimationSpec<T>): androidx.compose.animation.core.AnimationSpec<T> =
    if (LocalPaletteFading.current) androidx.compose.animation.core.snap() else spec

/** True when the system's animator duration scale is 0 ("Remove animations"). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
