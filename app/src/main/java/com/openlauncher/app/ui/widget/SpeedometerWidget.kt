package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.openlauncher.app.ui.components.WidgetCaption
import com.openlauncher.app.ui.theme.Hig
import com.openlauncher.app.util.LocationData
import kotlin.math.min
import kotlin.math.roundToInt

/** GPS speed: a big number, optionally on an Activity-ring-style arc. */
@Composable
fun SpeedometerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val c = Hig.colors
    val max = if (isMetric) 200f else 124f
    val speed = ((location?.speedMps ?: 0f) * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)
    val unit = if (isMetric) "km/h" else "mph"

    BoxWithConstraints(modifier.padding(24.dp)) {
        val density = LocalDensity.current
        WidgetCaption("Speed", Icons.Rounded.Speed, Modifier.align(Alignment.TopStart))
        if (!digitalOnly) Canvas(Modifier.fillMaxSize().padding(top = 28.dp)) {
            val r = size.minDimension / 2f * 0.92f
            val w = r * 0.12f
            val tl = Offset(center.x - r, center.y - r)
            val sz = Size(r * 2, r * 2)
            drawArc(c.fill, 150f, 240f, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
            val sweep = (speed / max).coerceIn(0f, 1f) * 240f
            if (sweep > 0.5f) drawArc(c.tint, 150f, sweep, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        val numSize = with(density) { min(maxHeight.toPx() * (if (digitalOnly) 0.42f else 0.30f), maxWidth.toPx() * 0.4f).toSp() }
        Column(
            Modifier.align(if (digitalOnly) Alignment.BottomStart else Alignment.Center).padding(top = 28.dp),
            horizontalAlignment = if (digitalOnly) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Text(
                if (location == null) "—" else speed.roundToInt().toString(),
                style = Hig.type.numeral.copy(fontSize = numSize, lineHeight = numSize), color = c.label, maxLines = 1
            )
            Text(if (location == null) "No GPS fix" else unit, style = Hig.type.title2.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = c.label2)
        }
    }
}
