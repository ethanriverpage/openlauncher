package com.openlauncher.app.ui.widget

import android.graphics.Bitmap
import android.media.MediaMetadata
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.ui.components.*
import com.openlauncher.app.ui.theme.Hig
import com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class Source { MEDIA, RADIO }

/**
 * Now Playing, CarPlay/StandBy style: the tile takes a dim tint from the artwork. Switches
 * between any media session and the head unit's real tuner (radio deck).
 */
@Composable
fun NowPlayingWidget(
    state: NowPlayingState?,
    accent: Color,
    carPlayPackage: String,
    androidAutoPackage: String,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    isDayMode: Boolean = false,
    hardwareRadio: HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {}
) {
    var source by rememberSaveable { mutableStateOf(Source.MEDIA) }
    // Follow the tuner when it appears; keyed on presence so RDS updates don't override a manual pick
    val hasRadio = hardwareRadio != null
    LaunchedEffect(hasRadio) { source = if (hasRadio) Source.RADIO else Source.MEDIA }
    var menu by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        if (source == Source.RADIO) RadioDeck(
            hardwareRadio, onLaunchHardwareRadio, onStopHardwareRadio, onRadioSeekUp, onRadioSeekDown,
            onRadioCycleFm, onRadioSwitchAm, onRadioTune, onAssignRadio, Modifier.fillMaxSize()
        ) else MediaPlayer(
            state, carPlayPackage.isNotEmpty(), androidAutoPackage.isNotEmpty(), isDayMode,
            onPlayPause, onNext, onPrev, onLaunchCarPlay, onLaunchAndroidAuto, onTapToOpenApp, Modifier.fillMaxSize()
        )

        // Source switch (media session ↔ tuner). On the projection launcher the arrow owns the
        // top-right corner, so the switch moves to the bottom-right.
        val projection = source == Source.MEDIA && (state == null || state.title.isEmpty()) &&
            (carPlayPackage.isNotEmpty() || androidAutoPackage.isNotEmpty())
        Box(Modifier.align(if (projection) Alignment.BottomEnd else Alignment.TopEnd).padding(12.dp)) {
            CircleIconButton(
                Icons.Rounded.MoreHoriz, "Choose source", onClick = { menu = true },
                size = 44.dp, iconSize = 28.dp, tint = Hig.colors.label2, background = Hig.colors.fill.copy(alpha = 0.6f)
            )
            ContextMenu(
                expanded = menu, onDismiss = { menu = false },
                alignment = if (projection) Alignment.BottomEnd else Alignment.TopEnd,
                offset = IntOffset(0, if (projection) -120 else 120),
                actions = listOf(
                    MenuAction("Media", Icons.Rounded.MusicNote, checked = source == Source.MEDIA) { source = Source.MEDIA },
                    MenuAction("Radio", Icons.Rounded.Radio, checked = source == Source.RADIO) { source = Source.RADIO },
                )
            )
        }
    }
}

/** A tile-sized launch target: the app's name, and an arrow saying it opens elsewhere. */
@Composable
private fun ProjectionPanel(name: String, onClick: () -> Unit, modifier: Modifier) {
    val c = Hig.colors
    BoxWithConstraints(
        modifier.fillMaxHeight()
            .pressable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Open $name" }
            .padding(24.dp)
    ) {
        // The name is the whole message, so it is sized to the tile like the clock's numerals
        val size = with(androidx.compose.ui.platform.LocalDensity.current) { minOf(maxHeight.toPx() * 0.24f, maxWidth.toPx() * 0.12f, 64.dp.toPx()).toSp() }
        Icon(Icons.Rounded.NorthEast, null, tint = c.label2, modifier = Modifier.align(Alignment.TopEnd).size(minOf(maxHeight * 0.16f, 40.dp).coerceAtLeast(28.dp)))
        Text(name, style = Hig.type.title1.copy(fontSize = size, lineHeight = size), color = c.label, maxLines = 1, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun MediaPlayer(
    state: NowPlayingState?,
    hasCarPlay: Boolean,
    hasAuto: Boolean,
    isDayMode: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapToOpenApp: () -> Unit,
    modifier: Modifier
) {
    val c = Hig.colors
    val connected by MediaListenerService.isConnected.collectAsState()

    if ((state == null || state.title.isEmpty()) && (hasCarPlay || hasAuto)) {
        // Phone projection: the whole tile is the button. Both configured → two halves.
        Row(modifier.fillMaxSize()) {
            if (hasCarPlay) ProjectionPanel("CarPlay", onLaunchCarPlay, Modifier.weight(1f))
            if (hasCarPlay && hasAuto) Box(Modifier.width(0.5.dp).fillMaxHeight().padding(vertical = 20.dp).background(c.separator))
            if (hasAuto) ProjectionPanel("Android Auto", onLaunchAndroidAuto, Modifier.weight(1f))
        }
        return
    }

    if (state == null || state.title.isEmpty()) {
        // Caption top-left like every other widget; the source button owns the top-right corner
        Box(modifier.padding(24.dp)) {
            WidgetCaption("Now Playing", Icons.Rounded.MusicNote, Modifier.align(Alignment.TopStart).padding(end = 56.dp))
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    !connected -> {
                        Text("Not Playing", style = Hig.type.title2, color = c.label, maxLines = 1)
                        Text("Allow notification access in Settings to show media here.", style = Hig.type.subhead, color = c.label2, maxLines = 2)
                    }
                    else -> Text("Not Playing", style = Hig.type.title2, color = c.label, maxLines = 1)
                }
            }
        }
        return
    }

    val ctrl = state.controller
    var positionMs by remember(state.title) { mutableLongStateOf(ctrl?.playbackState?.position ?: 0L) }
    val durationMs = ctrl?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    LaunchedEffect(state.isPlaying, state.title) {
        while (state.isPlaying) {
            positionMs = ctrl?.playbackState?.position ?: positionMs
            delay(1_000)
        }
    }
    val context = LocalContext.current
    val appName = remember(ctrl?.packageName) {
        ctrl?.packageName?.let { pkg ->
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
        }
    }
    val artColor = remember(state.albumArt) { state.albumArt?.let(::averageColor) }
    val bgTarget = artColor?.let { (if (isDayMode) it.copy(alpha = 0.26f) else it.copy(alpha = 0.34f)).compositeOver(c.tile) } ?: c.tile
    // A new track's colour washes in rather than snapping
    val bg by androidx.compose.animation.animateColorAsState(bgTarget, com.openlauncher.app.ui.theme.themeAware(androidx.compose.animation.core.tween(600)), label = "artTint")

    BoxWithConstraints(modifier.background(bg)) {
        val showArt = maxWidth > 640.dp && state.albumArt != null
        val stacked = maxWidth < 420.dp
        val art: @Composable () -> Unit = {
            coil.compose.AsyncImage(
                model = state.artUri ?: state.albumArt,
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                error = state.albumArt?.let { androidx.compose.ui.graphics.painter.BitmapPainter(it.asImageBitmap()) },
                modifier = Modifier.fillMaxHeight(0.78f).aspectRatio(1f).shadow(12.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
            )
        }
        val meta: @Composable (Modifier) -> Unit = { m ->
            Column(m.clickable(onClick = onTapToOpenApp)) {
                if (appName != null) Text(appName, style = Hig.type.caption, color = c.label2, maxLines = 1)
                Text(state.title, style = Hig.type.title2, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Text(state.artist.ifEmpty { "Unknown artist" }, style = Hig.type.body, color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (durationMs > 0) {
                    val f = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(c.label.copy(alpha = 0.18f))) {
                        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(c.label))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMs(positionMs), style = Hig.type.caption, color = c.label2)
                        Text("−" + formatMs((durationMs - positionMs).coerceAtLeast(0)), style = Hig.type.caption, color = c.label2)
                    }
                }
            }
        }
        val controls: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CircleIconButton(Icons.Rounded.SkipPrevious, "Previous track", onPrev)
                CircleIconButton(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (state.isPlaying) "Pause" else "Play", onPlayPause,
                    size = 76.dp, iconSize = 46.dp, background = c.label.copy(alpha = 0.14f)
                )
                CircleIconButton(Icons.Rounded.SkipNext, "Next track", onNext)
            }
        }
        if (stacked) Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            meta(Modifier.fillMaxWidth().padding(end = 48.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { controls() }
        } else Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (showArt) art()
            meta(Modifier.weight(1f))
            controls()
        }
    }
}

/** Real tuner only: the vendor MCU (full control) or the radio app's media session. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadioDeck(
    radio: HardwareRadioState?,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    onSeekUp: () -> Unit,
    onSeekDown: () -> Unit,
    onCycleFm: () -> Unit,
    onSwitchAm: () -> Unit,
    onTune: (String, Float) -> Unit,
    onAssign: () -> Unit,
    modifier: Modifier
) {
    val c = Hig.colors
    if (radio == null) {
        Column(
            modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Icon(Icons.Rounded.Radio, null, tint = c.label3, modifier = Modifier.size(44.dp))
            Text("No radio found", style = Hig.type.title2, color = c.label)
            Text("Start your head unit's radio app, or choose it so Open Launcher can show and control it.",
                style = Hig.type.subhead, color = c.label2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            PillButton("Choose Radio App", onAssign, primary = true)
        }
        return
    }
    var manualOff by remember { mutableStateOf(false) }
    LaunchedEffect(radio) { manualOff = false }
    val on = !manualOff
    val band = radio.band.uppercase()
    val freqClean = radio.freq.replace(Regex("[^0-9.]"), "")
    val freq = freqClean.toFloatOrNull()

    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.padding(end = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (radio.canTune) Segmented(
                options = listOf("FM1" to "FM1", "FM2" to "FM2", "FM3" to "FM3", "AM" to "AM"),
                selected = band, onSelect = { if (it == "AM") onSwitchAm() else onCycleFm() }
            ) else WidgetCaption(band, Icons.Rounded.Radio)
            CircleIconButton(
                Icons.Rounded.PowerSettingsNew, if (on) "Turn radio off" else "Turn radio on",
                onClick = { if (on) { onStop(); manualOff = true } else { onOpen(); manualOff = false } },
                size = 48.dp, iconSize = 26.dp, background = if (on) c.toggleOn else c.fill,
                tint = if (on) c.onToggleOn else c.label2
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIconButton(Icons.Rounded.FastRewind, "Seek down", onSeekDown, enabled = on)
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(freqClean.ifEmpty { radio.freq }, style = Hig.type.numeral.copy(fontSize = 56.sp), color = if (on) c.label else c.label3, maxLines = 1)
                    Text(if (radio.isAm) " kHz" else " MHz", style = Hig.type.headline, color = c.label2, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(if (!on) "Radio off" else radio.stationName ?: "Live", style = Hig.type.body, color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CircleIconButton(Icons.Rounded.FastForward, "Seek up", onSeekUp, enabled = on)
        }
        if (radio.canTune) {
            var fm by rememberSaveable { mutableStateOf(listOf(88.5f, 91.5f, 98.1f, 101.9f, 104.3f, 107.5f)) }
            var am by rememberSaveable { mutableStateOf(listOf(540f, 680f, 820f, 1040f, 1260f, 1420f)) }
            val isFm = !radio.isAm
            val presets = if (isFm) fm else am
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEachIndexed { i, p ->
                    val tuned = freq != null && abs(freq - p) < (if (isFm) 0.15f else 5f)
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (tuned) c.toggleOn else c.fill)
                            .combinedClickable(
                                enabled = on,
                                onClick = { onTune(band, p) },
                                // Long-press stores the tuner's current frequency on this preset
                                onLongClick = { freq?.let { f -> if (isFm) fm = fm.toMutableList().also { it[i] = f } else am = am.toMutableList().also { it[i] = f } } }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isFm) "%.1f".format(p) else "%.0f".format(p), style = Hig.type.subhead, color = if (tuned) c.onToggleOn else c.label)
                    }
                }
            }
        }
    }
}

/** Mean colour of the artwork, for the tile's tint. */
private fun averageColor(bmp: Bitmap): Color? = runCatching {
    val one = Bitmap.createScaledBitmap(bmp, 1, 1, true)
    Color(one.getPixel(0, 0)).copy(alpha = 1f)
}.getOrNull()

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
