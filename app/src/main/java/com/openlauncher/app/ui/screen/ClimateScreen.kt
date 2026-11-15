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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.climate.ClimateState
import com.openlauncher.app.climate.TwClimateClient
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

    val off = s.power == 0
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
    val heat = if (left) s.seatHeatLeft else s.seatHeatRight
    val vent = if (left) s.seatVentLeft else s.seatVentRight
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
                if (heat != null || vent != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        heat?.let {
                            LevelChip("HEAT", it, colors) { send(if (left) Cmd.SEAT_HEAT_LEFT else Cmd.SEAT_HEAT_RIGHT) }
                        }
                        vent?.let {
                            LevelChip("VENT", it, colors) { send(if (left) Cmd.SEAT_VENT_LEFT else Cmd.SEAT_VENT_RIGHT) }
                        }
                    }
                }
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
                FanBars(level = if (off) 0 else (s.fan ?: 0), colors = colors, modifier = Modifier.weight(1f).height(56.dp))
                StepButton(Icons.Default.Add, colors) { send(Cmd.FAN_UP) }
            }
            Text(
                when {
                    off -> "OFF"
                    (s.auto ?: 0) != 0 -> "AUTO"
                    s.fan != null -> "SPEED ${s.fan}"
                    else -> "--"
                },
                color = colors.text, fontSize = 16.sp, letterSpacing = 2.sp
            )
            Box(
                Modifier
                    .height(44.dp).fillMaxWidth()
                    .border(1.dp, colors.cardBorder, CARD_SHAPE)
                    .clickable { send(Cmd.MODE_CYCLE) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "MODE  ${s.mode ?: "--"}",
                    color = colors.text, fontSize = 13.sp, letterSpacing = 2.sp
                )
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

private data class Toggle(val label: String, val icon: ImageVector, val on: Boolean, val cmd: Cmd)

private fun toggles(s: ClimateState): List<Toggle> = buildList {
    add(Toggle("POWER", Icons.Default.PowerSettingsNew, s.power == 1, Cmd.POWER))
    s.auto?.let { add(Toggle("AUTO", Icons.Default.Autorenew, it != 0, Cmd.AUTO)) }
    s.ac?.let { add(Toggle("A/C", Icons.Default.AcUnit, it != 0, Cmd.AC)) }
    s.recirc?.let { add(Toggle("RECIRC", Icons.Default.Loop, it != 0, Cmd.RECIRC)) }
    s.dual?.let { add(Toggle("DUAL", Icons.Default.People, it != 0, Cmd.DUAL)) }
    s.frontDefrost?.let { add(Toggle("FRONT", Icons.Default.Air, it != 0, Cmd.FRONT_DEFROST)) }
    s.rearDefrost?.let { add(Toggle("REAR", Icons.Default.Air, it != 0, Cmd.REAR_DEFROST)) }
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
            Icon(t.icon, t.label, tint = if (t.on) colors.accent else colors.dim, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(t.label, color = if (t.on) colors.text else colors.dim, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, colors: ClimateColors, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .border(1.dp, colors.cardBorder, CARD_SHAPE)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = colors.accent, modifier = Modifier.size(28.dp)) }
}

@Composable
private fun LevelChip(label: String, level: Int, colors: ClimateColors, onClick: () -> Unit) {
    Box(
        Modifier
            .height(30.dp)
            .border(1.dp, if (level > 0) colors.accent else colors.cardBorder, CARD_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (level > 0) "$label $level" else label,
            color = if (level > 0) colors.text else colors.dim, fontSize = 11.sp, letterSpacing = 1.sp
        )
    }
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
