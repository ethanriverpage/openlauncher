package com.openlauncher.app.util

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Computes a comfortable UI-scale multiplier for the current display, combining its
 * resolution/density with its physical size. The result is applied on top of the base
 * Compose density (a value > 1 enlarges the whole UI).
 *
 * Aftermarket head units report wildly inconsistent density, so phone-sized elements
 * (48dp buttons, etc.) often render physically tiny. The key signal is the "smallest
 * width" in dp (shortest side px ÷ density): a phone is ~360–400dp, while many head
 * units expose 600–800+dp, which makes fixed-dp UI look small. We scale up toward a
 * phone-like reference, then nudge by physical screen size when the reported DPI looks
 * trustworthy — small screens (viewed from the driver's seat) want a bit more, large
 * screens a bit less.
 *
 * Pure and side-effect free so it can be unit-tested; never returns < 1.0 (shrinking is
 * left to the manual UI-scale trim) and is clamped to a safe maximum.
 */
fun computeAutoUiScale(
    widthPx: Int,
    heightPx: Int,
    density: Float,   // DisplayMetrics.density (dp → px factor)
    xdpi: Float,      // physical dpi; often unreliable on cheap head units
    ydpi: Float
): Float {
    if (widthPx <= 0 || heightPx <= 0 || density <= 0f) return 1f

    val shortestDp = min(widthPx, heightPx) / density

    // Resolution/density factor: reference ~400dp shortest side.
    val byResolution = shortestDp / REFERENCE_SW_DP

    // Physical-size factor: only trust DPI when it is in a sane range.
    var bySize = 1f
    if (xdpi in SANE_DPI_MIN..SANE_DPI_MAX && ydpi in SANE_DPI_MIN..SANE_DPI_MAX) {
        val wIn = widthPx / xdpi
        val hIn = heightPx / ydpi
        val diagIn = sqrt(wIn * wIn + hIn * hIn)
        if (diagIn in SANE_DIAG_MIN..SANE_DIAG_MAX) {
            // 10" reference needs no help; smaller boosts, larger eases off.
            bySize = (REFERENCE_DIAG_IN / diagIn).coerceIn(0.85f, 1.3f)
        }
    }

    return (byResolution * bySize).coerceIn(MIN_AUTO_SCALE, MAX_AUTO_SCALE)
}

private const val REFERENCE_SW_DP = 400f
private const val REFERENCE_DIAG_IN = 10f
private const val SANE_DPI_MIN = 80f
private const val SANE_DPI_MAX = 640f
private const val SANE_DIAG_MIN = 4f
private const val SANE_DIAG_MAX = 20f
private const val MIN_AUTO_SCALE = 1.0f
private const val MAX_AUTO_SCALE = 2.0f
