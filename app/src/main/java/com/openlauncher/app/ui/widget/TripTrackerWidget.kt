package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openlauncher.app.ui.components.PillButton
import com.openlauncher.app.ui.components.Segmented
import com.openlauncher.app.ui.components.WidgetCaption
import com.openlauncher.app.ui.theme.Hig
import com.openlauncher.app.util.LocationData
import kotlinx.coroutines.delay

@Composable
fun TripTrackerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var driveTimeSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var idleTimeSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var totalSpeedSum by rememberSaveable { mutableDoubleStateOf(0.0) }
    var movingSecondsCount by rememberSaveable { mutableLongStateOf(0L) }
    var tripDistanceMeters by rememberSaveable { mutableDoubleStateOf(0.0) }

    // The trip loop is keyed on isRunning only, so it must read location through
    // rememberUpdatedState — a plain parameter capture would freeze the GPS fix
    // at the moment tracking started and the trip would record nothing.
    val currentLocation by rememberUpdatedState(location)

    var activeMode by rememberSaveable { mutableStateOf("TRIP") } // "TRIP" or "0-100"
    
    // Accel Test state
    var accelState by rememberSaveable { mutableStateOf("READY") } // "READY", "RUNNING", "COMPLETE"
    var accelStartTime by rememberSaveable { mutableLongStateOf(0L) }
    var accelEndTime by rememberSaveable { mutableLongStateOf(0L) }
    var accelTimeDisplay by remember { mutableStateOf("0.00s") }
    var bestAccelTime by rememberSaveable { mutableStateOf<Float?>(null) }
    // A run only counts from a standstill; switching modes at speed must not start the clock
    var accelArmed by rememberSaveable { mutableStateOf(false) }
    

    val currentSpeedMps = location?.speedMps ?: 0f
    val speedDisplay = if (isMetric) currentSpeedMps * 3.6f else currentSpeedMps * 2.23694f
    val targetSpeed = if (isMetric) 100f else 60f
    val targetSpeedUnit = if (isMetric) "KM/H" else "MPH"

    // High precision stopwatch update loop
    LaunchedEffect(accelState, accelStartTime) {
        if (accelState == "RUNNING") {
            while (accelState == "RUNNING") {
                val elapsed = android.os.SystemClock.elapsedRealtime() - accelStartTime
                accelTimeDisplay = "%.2fs".format(elapsed / 1000f)
                delay(30)
            }
        } else if (accelState == "COMPLETE") {
            accelTimeDisplay = "%.2fs".format((accelEndTime - accelStartTime) / 1000f)
        } else {
            accelTimeDisplay = "0.00s"
        }
    }

    // GPS real-time speed run tracking
    LaunchedEffect(location, activeMode) {
        if (activeMode == "0-100") {
            val speed = location?.speedMps ?: 0f
            val speedDisplayVal = if (isMetric) speed * 3.6f else speed * 2.23694f
            
            if (accelState == "READY") {
                if (speedDisplayVal <= 0.8f) accelArmed = true
                else if (accelArmed) { // start timer when vehicle moves off from a stop
                    accelStartTime = android.os.SystemClock.elapsedRealtime()
                    accelState = "RUNNING"
                }
            } else if (accelState == "RUNNING") {
                if (speedDisplayVal >= targetSpeed) {
                    accelEndTime = android.os.SystemClock.elapsedRealtime()
                    accelState = "COMPLETE"
                    accelArmed = false
                    val finalTime = (accelEndTime - accelStartTime) / 1000f
                    bestAccelTime = if (bestAccelTime == null) finalTime else minOf(bestAccelTime!!, finalTime)
                }
            }
        }
    }

    // Trip update loop
    LaunchedEffect(isRunning) {
        var lastTickMs = android.os.SystemClock.elapsedRealtime()
        while (isRunning) {
            delay(1000)
            val now = android.os.SystemClock.elapsedRealtime()
            // Measure the real interval instead of assuming exactly 1 s per tick
            val dtSeconds = ((now - lastTickMs) / 1000.0).coerceIn(0.0, 5.0)
            lastTickMs = now
            val currentSpeed = currentLocation?.speedMps ?: 0f
            if (currentSpeed > 0.5f) {
                driveTimeSeconds++
                totalSpeedSum += currentSpeed
                movingSecondsCount++
                tripDistanceMeters += currentSpeed * dtSeconds
            } else {
                idleTimeSeconds++
            }
        }
    }

    // Calculations
    val averageSpeedMps = if (movingSecondsCount > 0) totalSpeedSum / movingSecondsCount else 0.0
    val avgSpeedDisplay = if (isMetric) averageSpeedMps * 3.6 else averageSpeedMps * 2.23694
    val speedUnit = if (isMetric) "KM/H" else "MPH"

    val distanceDisplay = if (isMetric) tripDistanceMeters / 1000.0 else tripDistanceMeters / 1609.34
    val distUnit = if (isMetric) "KM" else "MI"

    fun formatTime(seconds: Long): String {
        val h = seconds / 3600; val m = (seconds % 3600) / 60; val sec = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    val c = Hig.colors
    val hasData = driveTimeSeconds > 0 || idleTimeSeconds > 0
    val accelMode = activeMode == "0-100"

    BoxWithConstraints(modifier.fillMaxSize().padding(20.dp)) {
        val roomy = maxHeight >= 220.dp
        val wide = maxWidth >= 300.dp
        val numeralSize = with(LocalDensity.current) { (minOf(maxHeight.value * 0.30f, maxWidth.value * 0.20f)).dp.toSp() }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Caption + mode switch
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetCaption(if (accelMode) (if (isMetric) "0–100 km/h" else "0–60 mph") else "Trip",
                    if (accelMode) Icons.Rounded.Speed else Icons.Rounded.Route, Modifier.weight(1f))
                if (wide) Segmented(
                    listOf("TRIP" to "Trip", "0-100" to (if (isMetric) "0–100" else "0–60")),
                    activeMode, { activeMode = it }
                )
            }

            // Primary reading
            Column {
                val (value, unit) = if (accelMode) accelTimeDisplay.removeSuffix("s") to "s"
                                    else "%.1f".format(distanceDisplay) to distUnit.lowercase()
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(value, style = Hig.type.numeral.copy(fontSize = numeralSize, lineHeight = numeralSize), color = c.label, maxLines = 1)
                    Text(unit, style = Hig.type.title2, color = c.label2, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    when {
                        accelMode && accelState == "READY"    -> if (accelArmed) "Starts when the car moves" else "Come to a stop to begin"
                        accelMode && accelState == "RUNNING"  -> "Timing…"
                        accelMode                              -> "Run complete"
                        isRunning                              -> "Recording"
                        hasData                                -> "Paused"
                        else                                   -> "Not started"
                    },
                    style = Hig.type.subhead, color = if (isRunning || accelState == "RUNNING") c.tint else c.label2, maxLines = 1
                )
            }

            // Secondary stats
            if (roomy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (accelMode) {
                    Stat("Speed", "%.0f %s".format(speedDisplay, targetSpeedUnit.lowercase()))
                    Stat("Best", bestAccelTime?.let { "%.2f s".format(it) } ?: "—")
                } else {
                    Stat("Driving", formatTime(driveTimeSeconds))
                    Stat("Idle", formatTime(idleTimeSeconds))
                    Stat("Average", "%.0f %s".format(avgSpeedDisplay, speedUnit.lowercase()))
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (accelMode) {
                    PillButton("New Run", {
                        accelState = "READY"; accelArmed = false; accelStartTime = 0L; accelEndTime = 0L
                    }, primary = accelState == "COMPLETE", enabled = accelState != "READY")
                    PillButton("Clear Best", { bestAccelTime = null }, enabled = bestAccelTime != null)
                } else {
                    PillButton(
                        if (isRunning) "Pause" else if (hasData) "Resume" else "Start",
                        { isRunning = !isRunning },
                        icon = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        primary = !isRunning
                    )
                    PillButton("Reset", {
                        driveTimeSeconds = 0L; idleTimeSeconds = 0L; totalSpeedSum = 0.0
                        movingSecondsCount = 0L; tripDistanceMeters = 0.0
                    }, enabled = !isRunning && hasData)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    val c = Hig.colors
    Column {
        Text(label, style = Hig.type.footnote, color = c.label2, maxLines = 1)
        Text(value, style = Hig.type.headline.copy(fontFeatureSettings = "tnum"), color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
