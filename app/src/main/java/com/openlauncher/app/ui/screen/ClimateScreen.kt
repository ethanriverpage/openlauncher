package com.openlauncher.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import com.openlauncher.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.climate.ClimateState
import com.openlauncher.app.climate.Intake
import com.openlauncher.app.climate.intake
import com.openlauncher.app.climate.rearTempDisplay
import com.openlauncher.app.climate.modeName
import com.openlauncher.app.climate.TwClimateClient
import com.openlauncher.app.climate.TwMcu
import com.openlauncher.app.climate.TwClimateClient.Cmd

private val CARD_SHAPE = RoundedCornerShape(0.dp)

/** Colours shared with the home-screen widget cards. */
data class ClimateColors(
    val accent: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val text: Color,
    val dim: Color,
    val label: Color,
)

@Composable
fun climateColors(accent: Color, isDayMode: Boolean, hasWallpaper: Boolean): ClimateColors {
    val onBg = MaterialTheme.colorScheme.onBackground
    return ClimateColors(
        // A light accent (e.g. the default white) disappears on day-mode white cards
        accent     = if (isDayMode && accent.luminance() > 0.6f) Color(0xFF111111) else accent,
        cardBg     = when {
            isDayMode    -> Color(0xFFFFFFFF)
            hasWallpaper -> Color(0xCC000000)
            else         -> Color.Black.copy(alpha = 0.35f)
        },
        cardBorder = when {
            isDayMode    -> Color(0xFFCCCCCC)
            hasWallpaper -> Color(0x22FFFFFF)
            else         -> onBg.copy(alpha = 0.08f)
        },
        text  = if (isDayMode) Color(0xFF111111) else onBg,
        dim   = if (isDayMode) Color(0xFF999999) else onBg.copy(alpha = 0.35f),
        label = if (isDayMode) Color(0xFF999999) else Color(0xFF3A3A3A),
    )
}

/** Sidebar destination: header + climate panel. */
@Composable
fun ClimateScreen(
    accent: Color,
    isDayMode: Boolean,
    hasWallpaper: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = climateColors(accent, isDayMode, hasWallpaper)
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CLIMATE",
                style = MaterialTheme.typography.titleLarge,
                color = if (isDayMode) Color(0xFF111111) else accent,
                letterSpacing = 3.sp, fontSize = 14.sp
            )
        }
        ClimatePanel(colors, Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, bottom = 6.dp))
    }
}

/** The controls themselves; used by [ClimateScreen] and the focus overlay. */
@Composable
fun ClimatePanel(
    colors: ClimateColors,
    modifier: Modifier = Modifier,
    onInteract: () -> Unit = {}
) {
    val context = LocalContext.current
    val client = remember { TwClimateClient.get(context) }
    DisposableEffect(client) {
        client.start()
        onDispose { client.stop() }
    }
    val s by client.state.collectAsState()
    val send: (Cmd) -> Unit = { onInteract(); client.send(it) }

    if (!s.connected) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("A/C service unavailable", color = colors.dim, fontSize = 14.sp)
        }
        return
    }

    var rearOpen by remember { mutableStateOf(false) }
    val hasRear = s.rearTemp != null || s.rearFan != null
    if (rearOpen && hasRear) {
        RearPanel(s, colors, modifier, onInteract, onBack = { onInteract(); rearOpen = false })
        return
    }

    // F0 (power) has no receive data on this car, so "off" is inferred from blower level 0.
    val off = s.fan == 0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TempCard(
                title = "DRIVER", s = s, left = true, off = off, colors = colors,
                modifier = Modifier.weight(1f).fillMaxHeight(), send = send
            )
            FanCard(s, off, colors, Modifier.weight(1.2f).fillMaxHeight(), send)
            TempCard(
                title = "PASSENGER", s = s, left = false, off = off, colors = colors,
                modifier = Modifier.weight(1f).fillMaxHeight(), send = send
            )
        }
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            toggles(s).forEach { t ->
                ToggleTile(t, colors, Modifier.weight(1f).fillMaxHeight()) { send(t.cmd) }
            }
            if (hasRear) {
                val rearOn = (s.rearFan ?: 0) > 0
                ToggleTile(
                    Toggle("REAR", icon = Icons.Default.AirlineSeatReclineNormal, on = rearOn, cmd = Cmd.POWER),
                    colors, Modifier.weight(1f).fillMaxHeight()
                ) { onInteract(); rearOpen = true }
            }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun Card(title: String, colors: ClimateColors, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .background(colors.cardBg, CARD_SHAPE)
            .border(1.dp, colors.cardBorder, CARD_SHAPE)
    ) {
        Text(
            title, color = colors.label, letterSpacing = 2.sp, fontSize = 8.sp,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 7.dp)
        )
        content()
    }
}

@Composable
private fun TempCard(
    title: String,
    s: ClimateState,
    left: Boolean,
    off: Boolean,
    colors: ClimateColors,
    modifier: Modifier,
    send: (Cmd) -> Unit
) {
    val label = if (left) tempLabel(s.leftTemp, s.leftLo, s.leftHi, s.tempUnit)
                else tempLabel(s.rightTemp, s.rightLo, s.rightHi, s.tempUnit)
    Card(title, colors, modifier) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    color = if (off) colors.dim else colors.text,
                    fontSize = 64.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton(Icons.Default.KeyboardArrowUp, colors) {
                    send(if (left) Cmd.LEFT_TEMP_UP else Cmd.RIGHT_TEMP_UP)
                }
                StepButton(Icons.Default.KeyboardArrowDown, colors) {
                    send(if (left) Cmd.LEFT_TEMP_DOWN else Cmd.RIGHT_TEMP_DOWN)
                }
            }
        }
    }
}

@Composable
private fun FanCard(s: ClimateState, off: Boolean, colors: ClimateColors, modifier: Modifier, send: (Cmd) -> Unit) {
    Card("FAN", colors, modifier) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepButton(Icons.Default.Remove, colors) { send(Cmd.FAN_DOWN) }
                FanBars(level = s.fan ?: 0, colors = colors, modifier = Modifier.weight(1f).height(56.dp))
                StepButton(Icons.Default.Add, colors) { send(Cmd.FAN_UP) }
            }
            Box(
                Modifier
                    .height(56.dp).fillMaxWidth()
                    .border(1.dp, colors.cardBorder, CARD_SHAPE)
                    .clickable { send(Cmd.MODE_CYCLE) },
                contentAlignment = Alignment.Center
            ) {
                modeIcon(s.mode)?.let {
                    Icon(painterResource(it), modeName(s.mode), tint = colors.text, modifier = Modifier.size(44.dp))
                }
            }
        }
    }
}

/**
 * GX460 rear climate, opened from the REAR tile. com.tw.ac reports rear state but has no
 * command for it, so keys go straight to the MCU via [TwMcu] — the frames the stock app sends.
 */
@Composable
private fun RearPanel(
    s: ClimateState,
    colors: ClimateColors,
    modifier: Modifier,
    onInteract: () -> Unit,
    onBack: () -> Unit
) {
    val rearOn = (s.rearFan ?: 0) > 0
    val rearAuto = (s.rearAuto ?: 0) != 0
    val key: (() -> Unit) -> Unit = { action -> onInteract(); action() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Temperature
            Card("REAR TEMP", colors, Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tempLabel(s.rearTempDisplay, s.rearLo, s.rearHi, s.tempUnit),
                        color = if (rearOn) colors.text else colors.dim,
                        fontSize = 64.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center,
                        maxLines = 1, modifier = Modifier.weight(1f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepButton(Icons.Default.KeyboardArrowUp, colors) { key { TwMcu.step(TwMcu.KEY_REAR_TEMP_UP) } }
                        StepButton(Icons.Default.KeyboardArrowDown, colors) { key { TwMcu.step(TwMcu.KEY_REAR_TEMP_DOWN) } }
                    }
                }
            }
            // Fan
            Card("REAR FAN", colors, Modifier.weight(1.2f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StepButton(Icons.Default.Remove, colors) { key { TwMcu.step(TwMcu.KEY_REAR_FAN_DOWN) } }
                        FanBars(level = s.rearFan ?: 0, colors = colors, modifier = Modifier.weight(1f).height(56.dp))
                        StepButton(Icons.Default.Add, colors) { key { TwMcu.step(TwMcu.KEY_REAR_FAN_UP) } }
                    }
                }
            }
            // Airflow
            Card("REAR AIRFLOW", colors, Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .clickable { key { TwMcu.press(TwMcu.KEY_REAR_MODE) } }
                        .padding(horizontal = 16.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The car doesn't report the rear vent mode (the only "rear mode" value
                    // com.tw.ac emits is an echo of the front), so this is a plain mode key.
                    Icon(painterResource(R.drawable.hvac_mode_face_floor), "Rear airflow", tint = colors.text, modifier = Modifier.size(96.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleTile(Toggle("FRONT", icon = Icons.Default.ArrowBack, on = false, cmd = Cmd.POWER), colors, Modifier.weight(1f).fillMaxHeight()) { onBack() }
            ToggleTile(Toggle("REAR AUTO", res = R.drawable.hvac_auto, on = rearAuto, cmd = Cmd.POWER), colors, Modifier.weight(1f).fillMaxHeight()) {
                key { TwMcu.toggle(TwMcu.KEY_REAR_AUTO, rearAuto) }
            }
            ToggleTile(Toggle(if (rearOn) "REAR ON" else "REAR OFF", icon = Icons.Default.PowerSettingsNew, on = rearOn, cmd = Cmd.POWER), colors, Modifier.weight(1f).fillMaxHeight()) {
                key { TwMcu.toggle(TwMcu.KEY_REAR_POWER, rearOn) }
            }
        }
    }
}

@Composable
private fun FanBars(level: Int, colors: ClimateColors, modifier: Modifier, max: Int = 7) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..max) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(0.3f + 0.7f * i / max)
                    .background(if (i <= level) colors.accent else colors.cardBorder)
            )
        }
    }
}

// ── Controls ──────────────────────────────────────────────────────────────────

private data class Toggle(
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val res: Int? = null,
    val on: Boolean,
    val cmd: Cmd,
    val badge: String? = null,
)

private fun toggles(s: ClimateState): List<Toggle> = buildList {
    // The car's OFF key only switches the system off; any other key turns it back on.
    add(Toggle("OFF", icon = Icons.Default.PowerSettingsNew, on = s.fan == 0, cmd = Cmd.POWER))
    s.auto?.let { add(Toggle("AUTO", res = R.drawable.hvac_auto, on = it != 0, cmd = Cmd.AUTO)) }
    s.ac?.let { add(Toggle("A/C", res = R.drawable.hvac_ac, on = it != 0, cmd = Cmd.AC)) }
    s.intake?.let {
        when (it) {
            Intake.RECIRC -> add(Toggle("RECIRC", res = R.drawable.hvac_recirc, on = true, cmd = Cmd.RECIRC))
            Intake.FRESH  -> add(Toggle("FRESH AIR", res = R.drawable.hvac_recirc, on = false, cmd = Cmd.RECIRC))
            Intake.AUTO   -> add(Toggle("AUTO AIR", res = R.drawable.hvac_recirc, on = true, cmd = Cmd.RECIRC, badge = "A"))
        }
    }
    s.dual?.let { add(Toggle("DUAL", icon = Icons.Default.People, on = it != 0, cmd = Cmd.DUAL)) }
    s.frontDefrost?.let { add(Toggle("FRONT DEF", res = R.drawable.hvac_defrost_front, on = it != 0, cmd = Cmd.FRONT_DEFROST)) }
    s.rearDefrost?.let { add(Toggle("REAR DEF", res = R.drawable.hvac_defrost_rear, on = it != 0, cmd = Cmd.REAR_DEFROST)) }
}

/** ISO 2575 airflow figure for the GX460's mode values. */
@DrawableRes
private fun modeIcon(mode: Int?): Int? = when (mode) {
    0 -> R.drawable.hvac_defrost_front
    1 -> R.drawable.hvac_mode_floor_defrost
    2 -> R.drawable.hvac_mode_face
    3 -> R.drawable.hvac_mode_face_floor
    4 -> R.drawable.hvac_mode_floor
    else -> null
}

@Composable
private fun ToggleTile(t: Toggle, colors: ClimateColors, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(if (t.on) colors.accent.copy(alpha = 0.18f) else colors.cardBg, CARD_SHAPE)
            .border(1.dp, if (t.on) colors.accent else colors.cardBorder, CARD_SHAPE)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val tint = if (t.on) colors.accent else colors.dim
            Box {
                when {
                    t.res != null -> Icon(painterResource(t.res), t.label, tint = tint, modifier = Modifier.size(44.dp))
                    t.icon != null -> Icon(t.icon, t.label, tint = tint, modifier = Modifier.size(34.dp))
                }
                t.badge?.let {
                    Text(
                        it, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, colors: ClimateColors, size: androidx.compose.ui.unit.Dp = 56.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .border(1.dp, colors.cardBorder, CARD_SHAPE)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = colors.accent, modifier = Modifier.size(28.dp)) }
}

internal fun tempLabel(t: Float?, lo: Boolean, hi: Boolean, unit: String?): String = when {
    lo -> "LO"
    hi -> "HI"
    t == null -> "--"
    else -> {
        val num = if (t % 1f == 0f) t.toInt().toString() else String.format("%.1f", t)
        num + (unit?.takeIf { it.isNotBlank() } ?: "°")
    }
}
