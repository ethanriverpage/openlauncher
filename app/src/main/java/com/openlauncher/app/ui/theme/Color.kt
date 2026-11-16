package com.openlauncher.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * Apple HIG system colours. Each accent preset carries its dark (night) and light (day)
 * variant, the way iOS resolves systemBlue etc. per appearance.
 */
data class TintPreset(val name: String, val dark: Color, val light: Color)

val TintPresets = listOf(
    TintPreset("Blue",     Color(0xFF0A84FF), Color(0xFF007AFF)),
    TintPreset("Indigo",   Color(0xFF5E5CE6), Color(0xFF5856D6)),
    TintPreset("Purple",   Color(0xFFBF5AF2), Color(0xFFAF52DE)),
    TintPreset("Pink",     Color(0xFFFF375F), Color(0xFFFF2D55)),
    TintPreset("Red",      Color(0xFFFF453A), Color(0xFFFF3B30)),
    TintPreset("Orange",   Color(0xFFFF9F0A), Color(0xFFFF9500)),
    TintPreset("Yellow",   Color(0xFFFFD60A), Color(0xFFFFCC00)),
    TintPreset("Green",    Color(0xFF30D158), Color(0xFF34C759)),
    TintPreset("Teal",     Color(0xFF40C8E0), Color(0xFF30B0C7)),
    TintPreset("Graphite", Color(0xFF8E8E93), Color(0xFF8E8E93)),
)

val DefaultTint = TintPresets.first()

/**
 * Resolves a stored accent int to the tint for the current appearance. Presets map to their
 * day/night variant; a legacy free-picked colour is kept, except white/black, which cannot
 * work as a tint on HIG surfaces and fall back to system blue.
 */
fun resolveTint(stored: Int, isDayMode: Boolean): Color {
    val c = Color(stored)
    TintPresets.firstOrNull { it.dark.toArgb() == stored || it.light.toArgb() == stored }?.let {
        return if (isDayMode) it.light else it.dark
    }
    val lum = c.luminance()
    if (lum > 0.85f || lum < 0.02f) return if (isDayMode) DefaultTint.light else DefaultTint.dark
    return c
}

fun presetFor(stored: Int): TintPreset? =
    TintPresets.firstOrNull { it.dark.toArgb() == stored || it.light.toArgb() == stored }
