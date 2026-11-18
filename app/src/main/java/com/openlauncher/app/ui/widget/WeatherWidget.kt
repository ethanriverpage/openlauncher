package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.components.WidgetCaption
import com.openlauncher.app.ui.theme.Hig
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Current conditions at the car's position. With no data the tile stays where the user put
 * it and says why, instead of leaving a hole in the grid.
 */
@Composable
fun WeatherWidget(
    state: WeatherState?,
    metric: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val c = Hig.colors
    if (state == null) {
        Column(
            modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Offline is just the glyph; waiting on a GPS fix still says so
            Icon(
                if (isOnline) Icons.Rounded.LocationSearching else Icons.Rounded.CloudOff,
                if (isOnline) null else "Weather unavailable offline",
                tint = c.label3, modifier = Modifier.size(if (isOnline) 44.dp else 56.dp)
            )
            if (isOnline) {
                Spacer(Modifier.height(10.dp))
                Text("Waiting for location", style = Hig.type.callout, color = c.label2, textAlign = TextAlign.Center)
            }
        }
        return
    }
    BoxWithConstraints(modifier.padding(24.dp)) {
        val density = LocalDensity.current
        val tempSize = with(density) { min(maxHeight.toPx() * 0.40f, maxWidth.toPx() * 0.30f).toSp() }
        val (icon, iconTint) = symbolFor(state.weatherCode, state.isDay, c.label, c.yellow)
        WidgetCaption("My Location", Icons.Rounded.NearMe, Modifier.align(Alignment.TopStart))
        Icon(icon, state.conditionLabel, tint = iconTint, modifier = Modifier.align(Alignment.TopEnd).size(min(maxHeight.value * 0.26f, 64f).dp))
        Column(Modifier.align(Alignment.BottomStart)) {
            val t = if (metric) state.temperatureCelsius else state.temperatureCelsius * 9.0 / 5.0 + 32.0
            Text("${t.roundToInt()}°", style = Hig.type.numeral.copy(fontSize = tempSize, lineHeight = tempSize), color = c.label, maxLines = 1)
            Text(state.conditionLabel, style = Hig.type.headline, color = c.label)
            val wind = if (metric) "${state.windspeedKmh.roundToInt()} km/h" else "${(state.windspeedKmh * 0.621371).roundToInt()} mph"
            Text("Wind $wind", style = Hig.type.subhead, color = c.label2)
        }
    }
}

private fun symbolFor(code: Int, isDay: Boolean, label: Color, sun: Color): Pair<ImageVector, Color> = when (code) {
    0 -> if (isDay) Icons.Rounded.WbSunny to sun else Icons.Rounded.Bedtime to label
    1, 2 -> if (isDay) Icons.Rounded.WbCloudy to label else Icons.Rounded.NightsStay to label
    3 -> Icons.Rounded.Cloud to label
    45, 48 -> Icons.Rounded.Dehaze to label
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Rounded.WaterDrop to label
    71, 73, 75 -> Icons.Rounded.AcUnit to label
    95, 96, 99 -> Icons.Rounded.Thunderstorm to label
    else -> Icons.Rounded.Thermostat to label
}
