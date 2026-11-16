package com.openlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    val c = remember(tint, isDayMode, hasWallpaper) { higColors(tint, isDayMode, hasWallpaper) }
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
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialTypography(textScale), content = content)
    }
}
