@file:OptIn(ExperimentalTextApi::class)

package com.openlauncher.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.openlauncher.app.R

/*
 * Inter stands in for SF Pro, which is licensed for Apple platforms only. One variable file
 * (res/font/inter_variable.ttf, SIL OFL) carries every weight; the display family pins the
 * optical-size axis high so large numerals get Inter's tighter display cut, as SF Display does.
 * On API < 26 the platform ignores variation settings and renders the default instance.
 */
private fun inter(weight: Int, opsz: Float) = Font(
    R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz)
    )
)

val Inter = FontFamily(
    inter(400, 14f), inter(500, 14f), inter(600, 14f), inter(700, 14f)
)

val InterDisplay = FontFamily(
    inter(400, 32f), inter(500, 32f), inter(600, 32f), inter(700, 32f)
)

/**
 * HIG text styles, sized up from iOS points for arm's-length reading on a dash screen.
 * [scale] is the user's Text Size setting.
 */
class HigType(private val scale: Float) {
    private fun s(v: Float): TextUnit = (v * scale).sp
    private val tnum = "tnum"

    val largeTitle = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold,     fontSize = s(34f), letterSpacing = (-0.02).em)
    val title1     = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold,     fontSize = s(28f), letterSpacing = (-0.015).em)
    val title2     = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.SemiBold, fontSize = s(24f), letterSpacing = (-0.01).em)
    val headline   = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.SemiBold, fontSize = s(19f))
    val body       = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.Normal,   fontSize = s(19f))
    val callout    = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.Normal,   fontSize = s(17f))
    val subhead    = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.Normal,   fontSize = s(16f))
    val footnote   = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.Normal,   fontSize = s(14f), fontFeatureSettings = tnum)
    val caption    = TextStyle(fontFamily = Inter,        fontWeight = FontWeight.Medium,   fontSize = s(13f), fontFeatureSettings = tnum)

    /** Big glanceable numerals (clock, temperature, heading). Size is set by the caller. */
    val numeral    = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.035).em, fontFeatureSettings = tnum)
}

/** Material typography still backs stock M3 pieces (text fields, menus); keep it on Inter. */
fun materialTypography(scale: Float): Typography {
    fun st(size: Float, w: FontWeight) = TextStyle(fontFamily = Inter, fontWeight = w, fontSize = (size * scale).sp)
    return Typography(
        displayLarge   = st(57f, FontWeight.SemiBold),
        displayMedium  = st(45f, FontWeight.SemiBold),
        headlineLarge  = st(32f, FontWeight.Bold),
        headlineMedium = st(28f, FontWeight.Bold),
        headlineSmall  = st(24f, FontWeight.SemiBold),
        titleLarge     = st(22f, FontWeight.SemiBold),
        titleMedium    = st(19f, FontWeight.SemiBold),
        titleSmall     = st(16f, FontWeight.SemiBold),
        bodyLarge      = st(19f, FontWeight.Normal),
        bodyMedium     = st(17f, FontWeight.Normal),
        bodySmall      = st(15f, FontWeight.Normal),
        labelLarge     = st(17f, FontWeight.SemiBold),
        labelMedium    = st(15f, FontWeight.Medium),
        labelSmall     = st(13f, FontWeight.Medium),
    )
}
