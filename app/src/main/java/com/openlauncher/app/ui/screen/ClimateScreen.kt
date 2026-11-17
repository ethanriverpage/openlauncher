package com.openlauncher.app.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.openlauncher.app.ui.theme.EaseOutExpo
import com.openlauncher.app.ui.theme.HigMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openlauncher.app.R
import com.openlauncher.app.climate.ClimateState
import com.openlauncher.app.climate.Intake
import com.openlauncher.app.climate.intake
import com.openlauncher.app.climate.rearTempDisplay
import com.openlauncher.app.climate.TwClimateClient
import com.openlauncher.app.climate.TwClimateClient.Cmd
import com.openlauncher.app.climate.TwMcu
import com.openlauncher.app.ui.components.*
import com.openlauncher.app.ui.theme.Hig
import kotlin.math.min

/** Sidebar destination. */
@Composable
fun ClimateScreen(modifier: Modifier = Modifier) {
    ClimatePanel(modifier.fillMaxSize().padding(12.dp))
}

/** The controls; used by [ClimateScreen] and the pop-up over other apps. */
@Composable
fun ClimatePanel(modifier: Modifier = Modifier, onInteract: () -> Unit = {}) {
    val context = LocalContext.current
    val client = remember { TwClimateClient.get(context) }
    DisposableEffect(client) {
        client.start()
        onDispose { client.stop() }
    }
    val s by client.state.collectAsState()
    val send: (Cmd) -> Unit = { onInteract(); client.send(it) }
    val c = Hig.colors

    if (!s.connected) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.AcUnit, null, tint = c.label3, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Climate controls aren't responding", style = Hig.type.title2, color = c.label)
            Text("The head unit's A/C service (com.tw.ac) isn't connected.", style = Hig.type.subhead, color = c.label2)
        }
        return
    }

    var rearOpen by remember { mutableStateOf(false) }
    val hasRear = s.rearTemp != null || s.rearFan != null
    if (rearOpen && hasRear) {
        RearPanel(s, modifier, onInteract) { onInteract(); rearOpen = false }
        return
    }

    // The car reports no power flag; "off" is inferred from blower level 0
    val off = s.fan == 0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Zone(
                "Driver", tempLabel(s.leftTemp, s.leftLo, s.leftHi, s.tempUnit), off,
                Modifier.weight(1f).fillMaxHeight(),
                onDown = { send(Cmd.LEFT_TEMP_DOWN) }, onUp = { send(Cmd.LEFT_TEMP_UP) }
            )
            FanTile(
                label = if (off) "Off" else "Fan ${s.fan ?: 0}", level = s.fan ?: 0, dim = off,
                modifier = Modifier.weight(1.25f).fillMaxHeight(),
                onDown = { send(Cmd.FAN_DOWN) }, onUp = { send(Cmd.FAN_UP) }
            ) {
                // Airflow: the car only offers "next mode", so one button shows the current figure
                val res = modeIcon(s.mode)
                Box(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(18.dp)).background(c.fill)
                        .pressable { send(Cmd.MODE_CYCLE) }
                        .semantics { contentDescription = "Airflow: ${airflowName(s.mode)}. Tap for next" },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (res != null) Icon(painterResource(res), null, tint = c.label, modifier = Modifier.size(40.dp))
                        Text(airflowName(s.mode), style = Hig.type.headline, color = c.label)
                    }
                }
            }
            val dual = (s.dual ?: 1) != 0
            Zone(
                if (dual) "Passenger" else "Passenger · Synced", tempLabel(s.rightTemp, s.rightLo, s.rightHi, s.tempUnit), off,
                Modifier.weight(1f).fillMaxHeight(),
                onDown = { send(Cmd.RIGHT_TEMP_DOWN) }, onUp = { send(Cmd.RIGHT_TEMP_UP) }
            )
        }
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            toggles(s).forEach { t -> ToggleTile(t, Modifier.weight(1f).fillMaxHeight()) { send(t.cmd) } }
            if (hasRear) ToggleTile(
                Toggle("Rear Seats", icon = Icons.Rounded.AirlineSeatReclineNormal, on = (s.rearFan ?: 0) > 0, cmd = Cmd.POWER),
                Modifier.weight(1f).fillMaxHeight()
            ) { onInteract(); rearOpen = true }
        }
    }
}

/**
 * GX460 rear climate. com.tw.ac reports rear state but has no command for it, so keys go
 * straight to the MCU via [TwMcu], the frames the stock app sends.
 */
@Composable
private fun RearPanel(s: ClimateState, modifier: Modifier, onInteract: () -> Unit, onBack: () -> Unit) {
    val rearOn = (s.rearFan ?: 0) > 0
    val rearAuto = (s.rearAuto ?: 0) != 0
    val key: (() -> Unit) -> Unit = { onInteract(); it() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Zone(
                "Rear", tempLabel(s.rearTempDisplay, s.rearLo, s.rearHi, s.tempUnit), !rearOn,
                Modifier.weight(1f).fillMaxHeight(),
                onDown = { key { TwMcu.step(TwMcu.KEY_REAR_TEMP_DOWN) } },
                onUp = { key { TwMcu.step(TwMcu.KEY_REAR_TEMP_UP) } }
            )
            FanTile(
                label = if (rearOn) "Rear fan ${s.rearFan}" else "Rear off", level = s.rearFan ?: 0, dim = !rearOn,
                modifier = Modifier.weight(1.25f).fillMaxHeight(),
                onDown = { key { TwMcu.step(TwMcu.KEY_REAR_FAN_DOWN) } },
                onUp = { key { TwMcu.step(TwMcu.KEY_REAR_FAN_UP) } }
            )
            HigTile(Modifier.weight(1f).fillMaxHeight()) {
                PillButton("Front", onBack, icon = Icons.Rounded.ChevronLeft, modifier = Modifier.align(Alignment.Center))
            }
        }
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleTile(Toggle("Rear Auto", icon = Icons.Rounded.HdrAuto, on = rearAuto, cmd = Cmd.POWER), Modifier.weight(1f).fillMaxHeight()) {
                key { TwMcu.toggle(TwMcu.KEY_REAR_AUTO, rearAuto) }
            }
            ToggleTile(Toggle(if (rearOn) "Rear On" else "Rear Off", icon = Icons.Rounded.PowerSettingsNew, on = rearOn, cmd = Cmd.POWER), Modifier.weight(1f).fillMaxHeight()) {
                key { TwMcu.toggle(TwMcu.KEY_REAR_POWER, rearOn) }
            }
        }
    }
}

@Composable
private fun Zone(label: String, temp: String, dim: Boolean, modifier: Modifier, onDown: () -> Unit, onUp: () -> Unit) {
    val c = Hig.colors
    HigTile(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
            val size = with(LocalDensity.current) { min(maxHeight.toPx() * 0.36f, maxWidth.toPx() * 0.34f).toSp() }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = Hig.type.headline, color = c.label2)
                RollingNumber(temp, Hig.type.numeral.copy(fontSize = size, lineHeight = size), if (dim) c.label3 else c.label)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepButton(Icons.Rounded.KeyboardArrowDown, "$label cooler", onDown)
                    StepButton(Icons.Rounded.KeyboardArrowUp, "$label warmer", onUp)
                }
            }
        }
    }
}

@Composable
private fun FanTile(
    label: String,
    level: Int,
    dim: Boolean,
    modifier: Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    val c = Hig.colors
    HigTile(modifier) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = Hig.type.headline, color = c.label2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StepButton(Icons.Rounded.Remove, "Fan lower", onDown)
                Row(
                    Modifier.weight(1f).height(56.dp).semantics { stateDescription = "Fan $level of 7" },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (i in 1..7) {
                        // Bars fill and empty in sequence as the level steps
                        val bar by animateColorAsState(
                            if (i <= level && !dim) c.tint else c.fill,
                            tween(HigMotion.STATE, delayMillis = 18 * i, easing = EaseOutExpo), label = "fanbar"
                        )
                        Box(Modifier.weight(1f).fillMaxHeight(0.3f + 0.7f * i / 7f).clip(RoundedCornerShape(6.dp)).background(bar))
                    }
                }
                StepButton(Icons.Rounded.Add, "Fan higher", onUp)
            }
            extra?.invoke(this)
        }
    }
}

private data class Toggle(
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val res: Int? = null,
    val on: Boolean,
    val cmd: Cmd,
)

private fun toggles(s: ClimateState): List<Toggle> = buildList {
    // The OFF key only switches the system off; any other key turns it back on
    add(Toggle("Off", icon = Icons.Rounded.PowerSettingsNew, on = s.fan == 0, cmd = Cmd.POWER))
    s.auto?.let { add(Toggle("Auto", icon = Icons.Rounded.HdrAuto, on = it != 0, cmd = Cmd.AUTO)) }
    s.ac?.let { add(Toggle("A/C", icon = Icons.Rounded.AcUnit, on = it != 0, cmd = Cmd.AC)) }
    s.intake?.let {
        when (it) {
            Intake.RECIRC -> add(Toggle("Recirculate", res = R.drawable.hvac_recirc, on = true, cmd = Cmd.RECIRC))
            Intake.FRESH  -> add(Toggle("Fresh Air", res = R.drawable.hvac_recirc, on = false, cmd = Cmd.RECIRC))
            Intake.AUTO   -> add(Toggle("Auto Air", res = R.drawable.hvac_recirc, on = true, cmd = Cmd.RECIRC))
        }
    }
    s.dual?.let { add(Toggle("Dual", icon = Icons.Rounded.People, on = it != 0, cmd = Cmd.DUAL)) }
    s.frontDefrost?.let { add(Toggle("Front Defrost", res = R.drawable.hvac_defrost_front, on = it != 0, cmd = Cmd.FRONT_DEFROST)) }
    s.rearDefrost?.let { add(Toggle("Rear Defrost", res = R.drawable.hvac_defrost_rear, on = it != 0, cmd = Cmd.REAR_DEFROST)) }
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

private fun airflowName(mode: Int?): String = when (mode) {
    0 -> "Windshield"
    1 -> "Floor & Windshield"
    2 -> "Face"
    3 -> "Face & Floor"
    4 -> "Floor"
    else -> "Airflow"
}

@Composable
private fun ToggleTile(t: Toggle, modifier: Modifier, onClick: () -> Unit) {
    val c = Hig.colors
    val fg by animateColorAsState(if (t.on) c.onToggleOn else c.label2, HigMotion.state(), label = "toggleFg")
    val bg by animateColorAsState(if (t.on) c.toggleOn else c.tile, HigMotion.state(), label = "toggleBg")
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .pressable(onClick = onClick)
            .semantics { stateDescription = if (t.on) "On" else "Off" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        // One glyph box for every toggle. The AOSP HVAC vectors draw their figure in the middle
        // ~half of a 108-unit frame, so they render larger to match the Material glyphs' weight.
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            when {
                t.res != null -> Icon(painterResource(t.res), null, tint = fg, modifier = Modifier.requiredSize(58.dp))
                t.icon != null -> Icon(t.icon, null, tint = fg, modifier = Modifier.size(32.dp))
            }
        }
        Text(t.label, style = Hig.type.footnote.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = fg, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/**
 * A temperature that rolls in the direction it moved, as iOS numeric text does: warmer
 * rises in from below, cooler drops in from above. LO/HI and "--" simply cross-fade.
 */
@Composable
private fun RollingNumber(text: String, style: androidx.compose.ui.text.TextStyle, color: androidx.compose.ui.graphics.Color) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val from = initialState.removeSuffix("°").toFloatOrNull()
            val to = targetState.removeSuffix("°").toFloatOrNull()
            val dir = if (from == null || to == null) 0 else if (to > from) 1 else -1
            if (dir == 0) fadeIn(HigMotion.state()) togetherWith fadeOut(HigMotion.feedback())
            else (slideInVertically(HigMotion.state()) { h -> dir * h / 3 } + fadeIn(HigMotion.state())) togetherWith
                 (slideOutVertically(HigMotion.feedback()) { h -> -dir * h / 3 } + fadeOut(HigMotion.feedback())) using
                 SizeTransform(clip = false)
        },
        label = "temp"
    ) { t -> Text(t, style = style, color = color, maxLines = 1) }
}

internal fun tempLabel(t: Float?, lo: Boolean, hi: Boolean, unit: String?): String = when {
    lo -> "LO"
    hi -> "HI"
    t == null -> "--"
    else -> (if (t % 1f == 0f) t.toInt().toString() else String.format("%.1f", t)) + "°"
}
