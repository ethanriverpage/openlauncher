package com.openlauncher.app.ui.widget

import android.text.format.DateFormat
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.ui.components.WidgetCaption
import com.openlauncher.app.ui.theme.Hig
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** StandBy-style clock: large numerals, date beneath, vehicle name as the caption. */
@Composable
fun ClockWidget(
    style: ClockStyle,
    vehicleName: String,
    modifier: Modifier = Modifier
) {
    var cal by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(style) {
        while (true) {
            cal = Calendar.getInstance()
            // Digital only changes on the minute; analog sweeps its second hand
            delay(if (style == ClockStyle.ANALOG) 1_000L else 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val c = Hig.colors
    val is24 = DateFormat.is24HourFormat(LocalContext.current)

    BoxWithConstraints(modifier.padding(24.dp)) {
        val density = LocalDensity.current
        if (vehicleName.isNotBlank()) WidgetCaption(vehicleName, Icons.Rounded.DirectionsCar, Modifier.align(Alignment.TopStart))
        when (style) {
            ClockStyle.DIGITAL -> {
                val h = cal.get(Calendar.HOUR_OF_DAY).let { if (is24) it else (it % 12).let { x -> if (x == 0) 12 else x } }
                val time = if (is24) "%02d:%02d".format(h, cal.get(Calendar.MINUTE)) else "%d:%02d".format(h, cal.get(Calendar.MINUTE))
                val size = with(density) { min(maxHeight.toPx() * 0.46f, maxWidth.toPx() * 0.30f).toSp() }
                val tileWidthPx = with(density) { maxWidth.toPx() }
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(time, style = Hig.type.numeral.copy(fontSize = size, lineHeight = size), color = c.label, maxLines = 1)
                    // Full date when it fits, "Mon, Nov 16" on narrow tiles; never wraps mid-date
                    val dateStyle = Hig.type.title2.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    val measurer = rememberTextMeasurer()
                    val full = dateString(cal, short = false)
                    val fits = remember(full, tileWidthPx, dateStyle) {
                        measurer.measure(full, dateStyle, maxLines = 1, softWrap = false).size.width <= tileWidthPx
                    }
                    Text(if (fits) full else dateString(cal, short = true), style = dateStyle, color = c.label2,
                        maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            ClockStyle.ANALOG -> AnalogFace(cal, Modifier.fillMaxSize().padding(top = 28.dp))
        }
    }
}

@Composable
private fun AnalogFace(cal: Calendar, modifier: Modifier) {
    val c = Hig.colors
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val ctr = Offset(size.width / 2f, size.height / 2f)
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90).toFloat()
            val hour = i % 5 == 0
            val inner = if (hour) 0.84f else 0.91f
            drawLine(
                if (hour) c.label2 else c.label3,
                Offset(ctr.x + cos(a) * r * inner, ctr.y + sin(a) * r * inner),
                Offset(ctr.x + cos(a) * r * 0.97f, ctr.y + sin(a) * r * 0.97f),
                strokeWidth = if (hour) 4.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round
            )
        }
        fun hand(deg: Float, len: Float, w: Float, color: androidx.compose.ui.graphics.Color) {
            val a = Math.toRadians(deg - 90.0).toFloat()
            drawLine(color, Offset(ctr.x - cos(a) * r * 0.1f, ctr.y - sin(a) * r * 0.1f),
                Offset(ctr.x + cos(a) * r * len, ctr.y + sin(a) * r * len), strokeWidth = w, cap = StrokeCap.Round)
        }
        val m = cal.get(Calendar.MINUTE); val s = cal.get(Calendar.SECOND)
        hand((cal.get(Calendar.HOUR) + m / 60f) * 30f, 0.5f, 8.dp.toPx(), c.label)
        hand((m + s / 60f) * 6f, 0.78f, 5.dp.toPx(), c.label)
        hand(s * 6f, 0.86f, 2.dp.toPx(), c.tint)
        drawCircle(c.tint, 6.dp.toPx(), ctr)
    }
}

private fun dateString(cal: Calendar, short: Boolean): String {
    val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val d = days[cal.get(Calendar.DAY_OF_WEEK) - 1]; val m = months[cal.get(Calendar.MONTH)]
    return if (short) "${d.take(3)}, ${m.take(3)} ${cal.get(Calendar.DAY_OF_MONTH)}"
           else "$d, $m ${cal.get(Calendar.DAY_OF_MONTH)}"
}
