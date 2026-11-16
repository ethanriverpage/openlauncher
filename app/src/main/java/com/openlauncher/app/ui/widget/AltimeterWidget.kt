package com.openlauncher.app.ui.widget

import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.ui.platform.LocalDensity
import com.openlauncher.app.ui.components.WidgetCaption
import com.openlauncher.app.ui.theme.Hig

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.*

import androidx.compose.material3.MaterialTheme

@Composable
fun AltimeterWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Accelerometer sensor state
    var rollDeg  by remember { mutableFloatStateOf(0f) }
    var pitchDeg by remember { mutableFloatStateOf(0f) }
    val gravBuf  = remember { FloatArray(3) { 0f } }
    var hasSensor by remember { mutableStateOf(false) }

    // GPS fallback state for sensor-less devices
    var lastLoc by remember { mutableStateOf<LocationData?>(null) }
    var gpsPitch by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                hasSensor = true
                val alpha = 0.12f
                gravBuf[0] = alpha * event.values[0] + (1f - alpha) * gravBuf[0]
                gravBuf[1] = alpha * event.values[1] + (1f - alpha) * gravBuf[1]
                gravBuf[2] = alpha * event.values[2] + (1f - alpha) * gravBuf[2]
                rollDeg  = Math.toDegrees(
                    atan2(gravBuf[1].toDouble(), gravBuf[2].toDouble())
                ).toFloat()
                pitchDeg = Math.toDegrees(
                    atan2(-gravBuf[0].toDouble(),
                        sqrt(gravBuf[1].toDouble().pow(2) + gravBuf[2].toDouble().pow(2)))
                ).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    // Mathematical GPS incline (pitch) calculation when offline/no accelerometer
    LaunchedEffect(location) {
        val newLoc = location ?: return@LaunchedEffect
        val oldLoc = lastLoc
        if (oldLoc != null && (oldLoc.latitude != newLoc.latitude || oldLoc.longitude != newLoc.longitude)) {
            val dist = FloatArray(1)
            runCatching {
                android.location.Location.distanceBetween(
                    oldLoc.latitude, oldLoc.longitude,
                    newLoc.latitude, newLoc.longitude,
                    dist
                )
                val distance = dist[0]
                val heightDiff = newLoc.altitude - oldLoc.altitude
                if (distance > 3f) {
                    val pitchRad = atan2(heightDiff, distance.toDouble())
                    val newPitchDeg = Math.toDegrees(pitchRad).toFloat().coerceIn(-45f, 45f)
                    val alpha = 0.15f
                    gpsPitch = alpha * newPitchDeg + (1f - alpha) * gpsPitch
                    lastLoc = newLoc
                }
            }
        } else {
            lastLoc = newLoc
        }
    }

    val displayRoll = if (hasSensor) rollDeg else 0f
    val displayPitch = if (hasSensor) pitchDeg else gpsPitch

    val c = Hig.colors
    BoxWithConstraints(modifier.padding(24.dp)) {
        val numSize = with(LocalDensity.current) { kotlin.math.min(maxHeight.toPx() * 0.30f, maxWidth.toPx() * 0.24f).toSp() }
        WidgetCaption("Altitude", Icons.Rounded.Landscape, Modifier.align(Alignment.TopStart))
        // Car glyph tilts with the measured roll, like a level
        Icon(
            Icons.Rounded.DirectionsCar, "Roll ${"%.0f".format(displayRoll)} degrees", tint = c.label2,
            modifier = Modifier.align(Alignment.TopEnd).size(52.dp).graphicsLayer { rotationZ = displayRoll }
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            val alt = location?.let { if (isMetric) it.altitude else it.altitude * 3.28084 }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(alt?.let { "%,.0f".format(it) } ?: "—", style = Hig.type.numeral.copy(fontSize = numSize, lineHeight = numSize), color = c.label, maxLines = 1)
                Text(if (isMetric) " m" else " ft", style = Hig.type.title2, color = c.label2, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(
                "Pitch ${"%.0f".format(displayPitch)}°  ·  Roll ${"%.0f".format(displayRoll)}°",
                style = Hig.type.subhead, color = c.label2, maxLines = 1
            )
        }
    }
}
