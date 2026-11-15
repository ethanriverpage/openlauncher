package com.openlauncher.app.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.climate.ClimateState
import com.openlauncher.app.climate.TwClimateClient
import com.openlauncher.app.climate.TwClimateClient.Cmd

/**
 * Vehicle climate panel backed by the TW head unit's com.tw.ac AirService.
 * Reflects live A/C state and sends button presses through the same binder interface the
 * stock A/C app uses (no raw CAN frames).
 */
@Composable
fun ClimateWidget(
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val client = remember { TwClimateClient.get(context) }
    DisposableEffect(client) {
        client.start()
        onDispose { client.stop() }
    }
    val s by client.state.collectAsState()

    val onBg = if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground
    val dim = onBg.copy(alpha = 0.35f)

    Box(modifier = modifier.padding(top = 22.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)) {
        if (!s.connected) {
            Text(
                "A/C service unavailable",
                color = dim, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Temps + fan row
            Row(
                Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TempColumn(
                    label = tempLabel(s.leftTemp, s.leftLo, s.leftHi, s.tempUnit),
                    color = onBg, accent = accent,
                    onUp = { client.send(Cmd.LEFT_TEMP_UP) },
                    onDown = { client.send(Cmd.LEFT_TEMP_DOWN) }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (s.power == 0) "OFF" else "FAN ${s.fan ?: "–"}",
                        color = onBg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallButton("−", accent) { client.send(Cmd.FAN_DOWN) }
                        SmallButton("+", accent) { client.send(Cmd.FAN_UP) }
                    }
                    Text(
                        "MODE ${s.mode ?: "–"}",
                        color = dim, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp).clickable { client.send(Cmd.MODE_CYCLE) }
                    )
                }
                TempColumn(
                    label = tempLabel(s.rightTemp, s.rightLo, s.rightHi, s.tempUnit),
                    color = onBg, accent = accent,
                    onUp = { client.send(Cmd.RIGHT_TEMP_UP) },
                    onDown = { client.send(Cmd.RIGHT_TEMP_DOWN) }
                )
            }
            // Toggle chips
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chips(s).forEach { (label, on, cmd) ->
                    Chip(label, on, accent, onBg, Modifier.weight(1f)) { client.send(cmd) }
                }
            }
        }
    }
}

private fun chips(s: ClimateState): List<Triple<String, Boolean?, Cmd>> = listOf(
    Triple("AUTO", s.auto?.let { it != 0 }, Cmd.AUTO),
    Triple("A/C", s.ac?.let { it != 0 }, Cmd.AC),
    Triple("RECIRC", s.recirc?.let { it != 0 }, Cmd.RECIRC),
    Triple("DUAL", s.dual?.let { it != 0 }, Cmd.DUAL),
    Triple("FRONT", s.frontDefrost?.let { it != 0 }, Cmd.FRONT_DEFROST),
    Triple("REAR", s.rearDefrost?.let { it != 0 }, Cmd.REAR_DEFROST),
)

internal fun tempLabel(t: Float?, lo: Boolean, hi: Boolean, unit: String?): String = when {
    lo -> "LO"
    hi -> "HI"
    t == null -> "--"
    else -> {
        val num = if (t % 1f == 0f) t.toInt().toString() else String.format("%.1f", t)
        num + (unit?.takeIf { it.isNotBlank() } ?: "°")
    }
}

@Composable
private fun TempColumn(
    label: String,
    color: Color,
    accent: Color,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SmallButton("▲", accent, onUp)
        Text(
            label, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 64.dp)
        )
        SmallButton("▼", accent, onDown)
    }
}

@Composable
private fun SmallButton(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Chip(
    label: String,
    on: Boolean?,
    accent: Color,
    onBg: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val active = on == true
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) accent.copy(alpha = 0.85f) else Color.Transparent)
            .border(1.dp, if (on == null) onBg.copy(alpha = 0.12f) else accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                active -> Color.Black
                on == null -> onBg.copy(alpha = 0.3f)
                else -> onBg
            },
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
        )
    }
}
