package com.openlauncher.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val LocalDayMode = staticCompositionLocalOf { false }

/** iOS semantic colours, resolved for the current appearance and wallpaper state. */
@Immutable
data class HigColors(
    val isDark: Boolean,
    val bg: Color,
    val tile: Color,
    val fill: Color,
    val fill2: Color,
    val separator: Color,
    val label: Color,
    val label2: Color,
    val label3: Color,
    val red: Color,
    val green: Color,
    val orange: Color,
    val yellow: Color,
    val tint: Color,
    val onTint: Color,
    val scrim: Color,
    val sheet: Color,
    val menu: Color,
) {
    /**
     * A selected toggle. By day it is the solid tint; at night it is a dim tinted fill so a
     * row of active buttons never becomes a bright field in a dark cabin.
     */
    val toggleOn: Color get() = if (isDark) tint.copy(alpha = 0.30f).compositeOver(tile) else tint
    val onToggleOn: Color get() = if (isDark) lerp(tint, Color.White, 0.45f) else onTint
}

fun higColors(tint: Color, isDayMode: Boolean, hasWallpaper: Boolean): HigColors {
    val onTint = if (tint.luminance() > 0.55f) Color.Black else Color.White
    return if (!isDayMode) HigColors(
        isDark    = true,
        bg        = Color.Black,
        tile      = if (hasWallpaper) Color(0xB81C1C1E) else Color(0xFF1C1C1E),
        fill      = if (hasWallpaper) Color(0x29FFFFFF) else Color(0xFF2C2C2E),
        fill2     = Color(0xFF3A3A3C),
        separator = Color(0x99545458),
        label     = Color.White,
        label2    = Color(0x9EEBEBF5),
        label3    = Color(0x52EBEBF5),
        red       = Color(0xFFFF453A),
        green     = Color(0xFF30D158),
        orange    = Color(0xFFFF9F0A),
        yellow    = Color(0xFFFFD60A),
        tint      = tint,
        onTint    = onTint,
        scrim     = Color(0x8C000000),
        sheet     = Color(0xFF1C1C1E),
        menu      = Color(0xF52C2C2E),
    ) else HigColors(
        isDark    = false,
        bg        = Color(0xFFF2F2F7),
        tile      = if (hasWallpaper) Color(0xC7FFFFFF) else Color.White,
        fill      = if (hasWallpaper) Color(0x14000000) else Color(0xFFE5E5EA),
        fill2     = Color(0xFFD1D1D6),
        separator = Color(0x383C3C43),
        label     = Color.Black,
        label2    = Color(0x9E3C3C43),
        label3    = Color(0x4D3C3C43),
        red       = Color(0xFFFF3B30),
        green     = Color(0xFF34C759),
        orange    = Color(0xFFFF9500),
        yellow    = Color(0xFFFFCC00),
        tint      = tint,
        onTint    = onTint,
        scrim     = Color(0x40000000),
        sheet     = Color(0xFFF2F2F7),
        menu      = Color(0xF7FAFAFC),
    )
}

val LocalHig = staticCompositionLocalOf { higColors(DefaultTint.dark, isDayMode = false, hasWallpaper = false) }
val LocalHigType = staticCompositionLocalOf { HigType(1f) }

/** True while the palette itself is fading (day/night, accent). See [themeAware]. */
val LocalPaletteFading = staticCompositionLocalOf { false }

object Hig {
    val colors: HigColors @Composable get() = LocalHig.current
    val type: HigType @Composable get() = LocalHigType.current
}

@Composable
fun OpenLauncherTheme(
    accentArgb: Int,
    isDayMode: Boolean,
    textScale: Float = 1f,
    hasWallpaper: Boolean = false,
    content: @Composable () -> Unit
) {
    val tint = resolveTint(accentArgb, isDayMode)
    val target = remember(tint, isDayMode, hasWallpaper) { higColors(tint, isDayMode, hasWallpaper) }
    val c = animatedPalette(target)
    val fading = c != target
    val type = remember(textScale) { HigType(textScale) }
    val scheme = if (isDayMode) lightColorScheme(
        primary = c.tint, onPrimary = c.onTint, background = c.bg, onBackground = c.label,
        surface = c.tile, onSurface = c.label, surfaceVariant = c.fill, onSurfaceVariant = c.label2,
        outline = c.separator, error = c.red, surfaceContainer = c.menu, surfaceContainerHigh = c.menu,
    ) else darkColorScheme(
        primary = c.tint, onPrimary = c.onTint, background = c.bg, onBackground = c.label,
        surface = c.tile, onSurface = c.label, surfaceVariant = c.fill, onSurfaceVariant = c.label2,
        outline = c.separator, error = c.red, surfaceContainer = c.menu, surfaceContainerHigh = c.menu,
    )
    CompositionLocalProvider(
        LocalDayMode provides isDayMode,
        LocalHig provides c,
        LocalHigType provides type,
        LocalPaletteFading provides fading,
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialTypography(textScale), content = content)
    }
}

/**
 * The day/night switch dissolves over [HigMotion.THEME] ms instead of cutting, so sunset
 * never flashes a white screen into a dark cabin. Accent and wallpaper changes use the
 * shorter state duration. An interrupted fade restarts from whatever is on screen.
 */
@Composable
private fun animatedPalette(target: HigColors): HigColors {
    // Plain holders, not state: they are bookkeeping for this composable, not UI inputs
    val shown = remember { arrayOf(target) }
    val from = remember { arrayOf(target) }
    val last = remember { arrayOf(target) }
    val crossover = remember { floatArrayOf(-1f) }
    val restart = remember { booleanArrayOf(false) }
    val progress = remember { Animatable(1f) }
    // Detect the change during composition so the first frame of a switch is the old
    // palette, not a one-frame flash of the new one
    if (last[0] != target) {
        from[0] = shown[0]; last[0] = target; restart[0] = true
        crossover[0] = crossoverPoint(from[0], target)
    }
    LaunchedEffect(target) {
        if (!restart[0]) return@LaunchedEffect
        val ms = if (from[0].isDark != target.isDark) HigMotion.THEME else HigMotion.STATE
        progress.snapTo(0f)
        restart[0] = false
        // Steep through mid-luminance so the grey middle of a day/night fade is brief
        progress.animateTo(1f, tween(ms, easing = if (crossover[0] >= 0f) CubicBezierEasing(0.7f, 0f, 0.3f, 1f) else FastOutSlowInEasing))
    }
    val p = if (restart[0]) 0f else progress.value
    shown[0] = if (p >= 1f) target else lerpPalette(from[0], target, p, crossover[0])
    return shown[0]
}

/**
 * Where a day/night fade crosses mid-luminance (≈0.18, where white and black text read
 * equally well on the tile). Text that faded linearly would meet the surface in the same
 * grey and vanish, so text tokens switch here instead; surfaces keep the full fade.
 * -1 when the fade doesn't cross (accent or wallpaper changes).
 */
private fun crossoverPoint(a: HigColors, b: HigColors): Float {
    if (a.isDark == b.isDark) return -1f
    val start = a.tile.luminance() > 0.18f
    for (i in 1..50) {
        val t = i / 50f
        if ((lerp(a.tile, b.tile, t).luminance() > 0.18f) != start) return t
    }
    return 0.5f
}

private fun lerpPalette(a: HigColors, b: HigColors, t: Float, crossover: Float): HigColors {
    if (crossover < 0f) return blend(a, b, t, t)
    // Text switches in one step at the crossover (a fade would pass through the surface's own
    // grey). Around it, secondary labels borrow the primary label so they never wash out.
    val side = if (t >= crossover) b else a
    val p = blend(a, b, t, if (t >= crossover) 1f else 0f)
    return if (kotlin.math.abs(t - crossover) < 0.15f) p.copy(label2 = side.label, label3 = side.label) else p
}

private fun blend(a: HigColors, b: HigColors, t: Float, tt: Float) = HigColors(
    isDark = if (tt >= 0.5f) b.isDark else a.isDark,
    bg = lerp(a.bg, b.bg, t), tile = lerp(a.tile, b.tile, t), fill = lerp(a.fill, b.fill, t),
    fill2 = lerp(a.fill2, b.fill2, t), separator = lerp(a.separator, b.separator, t),
    label = lerp(a.label, b.label, tt), label2 = lerp(a.label2, b.label2, tt), label3 = lerp(a.label3, b.label3, tt),
    red = lerp(a.red, b.red, t), green = lerp(a.green, b.green, t), orange = lerp(a.orange, b.orange, t),
    yellow = lerp(a.yellow, b.yellow, t), tint = lerp(a.tint, b.tint, t), onTint = lerp(a.onTint, b.onTint, tt),
    scrim = lerp(a.scrim, b.scrim, t), sheet = lerp(a.sheet, b.sheet, t), menu = lerp(a.menu, b.menu, t),
)
