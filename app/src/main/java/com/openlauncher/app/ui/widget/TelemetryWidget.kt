package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.ui.theme.Hig
import com.openlauncher.app.ui.theme.Inter
import com.openlauncher.app.util.LocationData
import kotlin.math.roundToInt

/** iOS Compass-style dial: the card turns, the heading mark stays at the top. */
@Composable
fun TelemetryWidget(
    location: LocationData?,
    bearing: Float,
    metric: Boolean,
    modifier: Modifier = Modifier
) {
    val c = Hig.colors
    val heading = ((bearing % 360f) + 360f) % 360f
    val cardinal = cardinalOf(heading)
    val measurer = rememberTextMeasurer()
    // Follows the Text Size setting like every other label
    val letterStyle = Hig.type.headline.copy(fontWeight = FontWeight.Bold)

    Column(
        modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true)
                .semantics { contentDescription = "Heading ${heading.roundToInt()} degrees $cardinal" }
        ) {
            val r = size.minDimension / 2f
            val ctr = center
            rotate(-heading, ctr) {
                for (i in 0 until 72) {
                    val major = i % 6 == 0
                    val len = if (major) r * 0.13f else r * 0.07f
                    rotate(i * 5f, ctr) {
                        drawLine(
                            if (major) c.label2 else c.label3,
                            Offset(ctr.x, ctr.y - r * 0.95f), Offset(ctr.x, ctr.y - r * 0.95f + len),
                            strokeWidth = if (major) 2.5.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round
                        )
                    }
                }
                listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (t, a) ->
                    rotate(a, ctr) {
                        val layout = measurer.measure(t, letterStyle.copy(color = if (t == "N") c.red else c.label2))
                        drawText(layout, topLeft = Offset(ctr.x - layout.size.width / 2f, ctr.y - r * 0.72f))
                    }
                }
            }
            // Fixed heading mark and pointer
            drawLine(c.tint, Offset(ctr.x, ctr.y - r * 1.02f), Offset(ctr.x, ctr.y - r * 0.82f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            val p = Path().apply {
                moveTo(ctr.x, ctr.y - r * 0.36f)
                lineTo(ctr.x + r * 0.12f, ctr.y + r * 0.12f)
                lineTo(ctr.x, ctr.y + r * 0.04f)
                lineTo(ctr.x - r * 0.12f, ctr.y + r * 0.12f)
                close()
            }
            drawPath(p, c.label)
        }
        Text("${heading.roundToInt()}° $cardinal", style = Hig.type.numeral.copy(fontSize = 44.sp), color = c.label, maxLines = 1)
        val speed = location?.let {
            if (metric) "${(it.speedMps * 3.6f).roundToInt()} km/h" else "${(it.speedMps * 2.23694f).roundToInt()} mph"
        } ?: "No GPS fix"
        Text(speed, style = Hig.type.title2.copy(fontWeight = FontWeight.Normal), color = c.label2, maxLines = 1)
    }
}

private fun cardinalOf(deg: Float): String =
    listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[(((deg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)]
