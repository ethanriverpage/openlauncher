package com.openlauncher.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.openlauncher.app.ui.theme.HigMotion
import com.openlauncher.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.UnitSystem
import com.openlauncher.app.data.WidgetConfig
import com.openlauncher.app.data.activeWidgetIds
import com.openlauncher.app.data.computeWidgetMove
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.components.*
import com.openlauncher.app.ui.theme.Hig
import com.openlauncher.app.ui.widget.*
import com.openlauncher.app.util.LocationData
import kotlin.math.roundToInt

private data class WidgetTypeInfo(val id: String, val name: String, val icon: ImageVector, val description: String)

private val ALL_WIDGET_TYPES = listOf(
    WidgetTypeInfo("CLOCK",        "Clock",       Icons.Rounded.Schedule,     "Time and date"),
    WidgetTypeInfo("WEATHER",      "Weather",     Icons.Rounded.WbCloudy,     "Current conditions"),
    WidgetTypeInfo("NOW_PLAYING",  "Now Playing", Icons.Rounded.MusicNote,    "Media and radio controls"),
    WidgetTypeInfo("TELEMETRY",    "Compass",     Icons.Rounded.Explore,      "Heading and speed"),
    WidgetTypeInfo("SPEEDOMETER",  "Speed",       Icons.Rounded.Speed,        "GPS speed"),
    WidgetTypeInfo("ALTIMETER",    "Altitude",    Icons.Rounded.Landscape,    "Elevation, pitch and roll"),
    WidgetTypeInfo("TRIP_TRACKER", "Trip",        Icons.Rounded.Route,        "Distance, time and 0–100"),
    WidgetTypeInfo("VITALS",       "Vitals",      Icons.Rounded.Memory,       "Head unit CPU, memory, heat"),
    WidgetTypeInfo("SOUNDBOARD",   "Soundboard",  Icons.Rounded.GraphicEq,    "Six sound pads"),
)

private fun nameOf(id: String) = ALL_WIDGET_TYPES.firstOrNull { it.id == id }?.name ?: id

private fun canAddWidget(settings: AppSettings): Boolean {
    val active = settings.widgetLayout.filter { it.enabled && it.id in settings.activeWidgetIds() }
    val occupied = buildSet { active.forEach { w -> for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy) } }
    val hasFreeCell = (0 until GRID_ROWS).any { r -> (0 until GRID_COLS).any { c -> (c to r) !in occupied } }
    // The view model can shrink a multi-cell widget to make room
    return hasFreeCell || active.any { it.spanX * it.spanY > 1 }
}

private fun fitsAt(layout: List<WidgetConfig>, w: WidgetConfig, sx: Int, sy: Int): Boolean {
    if (w.gridX + sx > GRID_COLS || w.gridY + sy > GRID_ROWS) return false
    val others = layout.filter { it.id != w.id }
    for (dx in 0 until sx) for (dy in 0 until sy) {
        val cx = w.gridX + dx; val cy = w.gridY + dy
        if (others.any { o -> cx in o.gridX until o.gridX + o.spanX && cy in o.gridY until o.gridY + o.spanY }) return false
    }
    return true
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    location: LocationData?,
    bearing: Float,
    isWifi: Boolean,
    isData: Boolean,
    isDayMode: Boolean = false,
    editMode: Boolean = false,
    onEditModeChange: (Boolean) -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onLaunchPip: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onUpdateWidget: (id: String, spanX: Int, spanY: Int) -> Unit,
    onMoveWidget: (id: String, gridX: Int, gridY: Int) -> Unit,
    onAddWidget: (id: String) -> Unit,
    onRemoveWidget: (id: String) -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit = {},
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit = {},
    onUpdateSoundPad: (index: Int, pad: com.openlauncher.app.data.SoundPadConfig) -> Unit = { _, _ -> },
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = Hig.colors
    val accent = c.tint
    val metric = settings.unitSystem == UnitSystem.METRIC
    var galleryOpen by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ── Edit bar (iOS "Edit Home Screen") ──────────────────────────────
            AnimatedVisibility(
                visible = editMode,
                enter = expandVertically(tween(260)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
            ) {
                Row(
                    Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Edit Home Screen", style = Hig.type.title2, color = c.label, modifier = Modifier.weight(1f))
                    PillButton("Add Widget", icon = Icons.Rounded.Add, onClick = { galleryOpen = true })
                    PillButton("Done", primary = true, onClick = { onEditModeChange(false) })
                }
            }

            // ── Widget grid ────────────────────────────────────────────────────
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = if (editMode) 4.dp else 12.dp)
            ) {
                val gap = 12.dp
                val cellW = (maxWidth - gap * (GRID_COLS - 1)) / GRID_COLS
                val cellH = (maxHeight - gap * (GRID_ROWS - 1)) / GRID_ROWS
                val density = LocalDensity.current
                val stepX = with(density) { (cellW + gap).toPx() }
                val stepY = with(density) { (cellH + gap).toPx() }

                val visible = settings.widgetLayout.filter { it.enabled && it.id in settings.activeWidgetIds() }

                var draggingId by remember { mutableStateOf<String?>(null) }
                var dragOffset by remember { mutableStateOf(Offset.Zero) }
                val dragging = draggingId?.let { id -> visible.find { it.id == id } }
                val tx = dragging?.let { (it.gridX + (dragOffset.x / stepX).roundToInt()).coerceIn(0, GRID_COLS - it.spanX) }
                val ty = dragging?.let { (it.gridY + (dragOffset.y / stepY).roundToInt()).coerceIn(0, GRID_ROWS - it.spanY) }
                val proposed = if (dragging != null && tx != null && ty != null) computeWidgetMove(visible, dragging.id, tx, ty) else null

                fun cellRect(x: Int, y: Int, sx: Int, sy: Int) =
                    Triple((cellW + gap) * x to (cellH + gap) * y, cellW * sx + gap * (sx - 1), cellH * sy + gap * (sy - 1))

                // Drop target and displacement ghosts, beneath the tiles
                if (dragging != null && tx != null && ty != null) {
                    val (pos, w, h) = cellRect(tx, ty, dragging.spanX, dragging.spanY)
                    Box(
                        Modifier.absoluteOffset(pos.first, pos.second).size(w, h)
                            .clip(TileShape).background(accent.copy(alpha = 0.16f)).border(2.dp, accent, TileShape)
                    )
                    proposed?.filter { it.id != dragging.id }?.forEach { p ->
                        val o = visible.find { it.id == p.id } ?: return@forEach
                        if (p.gridX != o.gridX || p.gridY != o.gridY) {
                            val (pp, pw, ph) = cellRect(p.gridX, p.gridY, p.spanX, p.spanY)
                            Box(Modifier.absoluteOffset(pp.first, pp.second).size(pw, ph).border(2.dp, c.fill2, TileShape))
                        }
                    }
                }

                // Edit-mode jiggle: one shared clock, phase-shifted per tile. It runs only while
                // editing (parked, by design) and not at all when system animations are off.
                val reduceMotion = rememberReduceMotion()
                val jiggle: State<Float>? = if (editMode && !reduceMotion) {
                    rememberInfiniteTransition(label = "jiggle").animateFloat(
                        0f, (2 * Math.PI).toFloat(),
                        infiniteRepeatable(tween(520, easing = LinearEasing)), label = "phase"
                    )
                } else null
                val scope = rememberCoroutineScope()
                // Two beats: the edit bar opens and the grid settles, then the badges pop,
                // so nothing pops onto a grid that is still moving
                var badgesOn by remember { mutableStateOf(editMode) }
                LaunchedEffect(editMode) {
                    if (editMode) { kotlinx.coroutines.delay(if (reduceMotion) 0L else 140L); badgesOn = true } else badgesOn = false
                }

                visible.forEachIndexed { index, w ->
                    val (pos, width, height) = cellRect(w.gridX, w.gridY, w.spanX, w.spanY)
                    val isDragging = draggingId == w.id
                    val editScale by animateFloatAsState(if (editMode && !isDragging) 0.97f else 1f, HigMotion.state(), label = "edit")
                    // Tiles glide to a new cell (a drop, a displacement, a resize) instead of jumping
                    val target = with(density) { IntOffset(pos.first.roundToPx(), pos.second.roundToPx()) }
                    val place = remember(w.id) { Animatable(target, IntOffset.VectorConverter) }
                    val currentTarget by rememberUpdatedState(target)
                    LaunchedEffect(target) { if (place.targetValue != target) place.animateTo(target, HigMotion.settle) }
                    Box(
                        Modifier
                            .absoluteOffset { if (isDragging) place.value + IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) else place.value }
                            .size(width, height)
                            .zIndex(if (isDragging) 2f else if (place.isRunning) 1f else 0f)
                            .graphicsLayer {
                                val s = if (isDragging) 1.03f else editScale
                                scaleX = s; scaleY = s
                                // Read the phase here so the jiggle redraws without recomposing
                                val ph = jiggle?.value
                                if (ph != null && !isDragging) {
                                    // Small: a 0.35° turn already moves a wide tile's corner ~2dp
                                    rotationZ = 0.35f * kotlin.math.sin(ph + index * 1.7f)
                                }
                            }
                    ) {
                        // The tile itself
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(if (isDragging) Modifier.shadow(18.dp, TileShape) else Modifier)
                                .clip(TileShape)
                                .background(c.tile)
                                .combinedClickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {},
                                    onLongClick = { if (!editMode) onEditModeChange(true) }
                                )
                        ) {
                            WidgetContent(
                                id = w.id, settings = settings, weather = weather, nowPlaying = nowPlaying,
                                location = location, bearing = bearing, isOnline = isWifi || isData,
                                isDayMode = isDayMode, editMode = editMode, metric = metric, accent = accent,
                                onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev,
                                onLaunchCarPlay = onLaunchCarPlay, onLaunchAndroidAuto = onLaunchAndroidAuto,
                                onTapNowPlaying = onTapNowPlaying, onUpdateSoundPad = onUpdateSoundPad,
                                hardwareRadio = hardwareRadio, onLaunchHardwareRadio = onLaunchHardwareRadio,
                                onStopHardwareRadio = onStopHardwareRadio, onRadioSeekUp = onRadioSeekUp,
                                onRadioSeekDown = onRadioSeekDown, onRadioCycleFm = onRadioCycleFm,
                                onRadioSwitchAm = onRadioSwitchAm, onRadioTune = onRadioTune, onAssignRadio = onAssignRadio
                            )
                        }

                        // In edit mode the whole tile is a drag handle and its content is inert
                        if (editMode) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = "${nameOf(w.id)}, drag to move" }
                                    .pointerInput(w.id, w.gridX, w.gridY, w.spanX, w.spanY) {
                                        detectDragGestures(
                                            onDragStart = { draggingId = w.id; dragOffset = Offset.Zero },
                                            onDrag = { ch, amount -> ch.consume(); dragOffset += amount },
                                            onDragEnd = {
                                                val nx = (w.gridX + (dragOffset.x / stepX).roundToInt()).coerceIn(0, GRID_COLS - w.spanX)
                                                val ny = (w.gridY + (dragOffset.y / stepY).roundToInt()).coerceIn(0, GRID_ROWS - w.spanY)
                                                if (nx != w.gridX || ny != w.gridY) {
                                                    val released = place.value + IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                                    // Undispatched, so the frame that drops the drag offset already has it
                                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                                        place.snapTo(released)
                                                        // If the grid refuses the move, the cell never changes: glide home
                                                        kotlinx.coroutines.delay(450)
                                                        if (place.targetValue == released && !place.isRunning) place.animateTo(currentTarget, HigMotion.settle)
                                                    }
                                                    onMoveWidget(w.id, nx, ny)
                                                } else {
                                                    val released = place.value + IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                                    scope.launch(start = CoroutineStart.UNDISPATCHED) { place.snapTo(released); place.animateTo(currentTarget, HigMotion.settle) }
                                                }
                                                draggingId = null; dragOffset = Offset.Zero
                                            },
                                            onDragCancel = { draggingId = null; dragOffset = Offset.Zero }
                                        )
                                    }
                            )
                        }

                        // Badges sit on the corners, outside the tile's clip, like iOS
                        val badge by animateFloatAsState(
                            if (badgesOn && editMode && !isDragging) 1f else 0f,
                            if (badgesOn && editMode && !isDragging) HigMotion.pop() else HigMotion.feedback(), label = "badge"
                        )
                        if (badge > 0.01f) {
                            Box(
                                Modifier.align(Alignment.TopStart).offset((-10).dp, (-10).dp)
                                    .graphicsLayer { alpha = badge.coerceIn(0f, 1f); scaleX = badge; scaleY = badge }
                            ) {
                                CornerBadge(Icons.Rounded.Remove, "Remove ${nameOf(w.id)}") { onRemoveWidget(w.id) }
                            }
                            Box(
                                Modifier.align(Alignment.BottomEnd).offset(10.dp, 10.dp)
                                    .graphicsLayer { alpha = badge.coerceIn(0f, 1f); scaleX = badge; scaleY = badge }
                            ) {
                                CornerBadge(Icons.Rounded.OpenInFull, "${nameOf(w.id)} options") { menuFor = w.id }
                                ContextMenu(
                                    expanded = menuFor == w.id,
                                    onDismiss = { menuFor = null },
                                    alignment = Alignment.BottomEnd,
                                    offset = with(density) { IntOffset(0, -48.dp.roundToPx()) },
                                    actions = widgetMenu(
                                        w, visible, settings, onUpdateWidget, onSetClockStyle, onSetVitalsAsBars,
                                        onSetSpeedometerDigitalOnly, onAssignCarPlay, onClearCarPlay,
                                        onAssignAndroidAuto, onClearAndroidAuto
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Add Widget sheet ───────────────────────────────────────────────────
        WidgetGallery(
            open = galleryOpen,
            settings = settings,
            onAdd = { id -> onAddWidget(id); galleryOpen = false },
            onDismiss = { galleryOpen = false }
        )
    }
}

private fun widgetMenu(
    w: WidgetConfig,
    layout: List<WidgetConfig>,
    settings: AppSettings,
    onUpdateWidget: (String, Int, Int) -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit,
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit,
    onAssignCarPlay: () -> Unit,
    onClearCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearAndroidAuto: () -> Unit,
): List<MenuAction> = buildList {
    listOf(Triple("Small", 1, 1), Triple("Wide", 2, 1), Triple("Tall", 1, 2), Triple("Large", 2, 2), Triple("Extra Wide", 3, 1))
        .forEach { (label, sx, sy) ->
            val current = w.spanX == sx && w.spanY == sy
            add(MenuAction("$label  ${sx}×$sy", checked = current, enabled = current || fitsAt(layout, w, sx, sy)) {
                onUpdateWidget(w.id, sx, sy)
            })
        }
    when (w.id) {
        "CLOCK" -> {
            add(MenuAction("Digital", Icons.Rounded.Schedule, checked = settings.clockStyle == ClockStyle.DIGITAL, groupStart = true) { onSetClockStyle(ClockStyle.DIGITAL) })
            add(MenuAction("Analog", Icons.Rounded.AccessTime, checked = settings.clockStyle == ClockStyle.ANALOG) { onSetClockStyle(ClockStyle.ANALOG) })
        }
        "VITALS" -> {
            add(MenuAction("Dials", Icons.Rounded.DonutLarge, checked = !settings.vitalsAsBars, groupStart = true) { onSetVitalsAsBars(false) })
            add(MenuAction("Bars", Icons.Rounded.BarChart, checked = settings.vitalsAsBars) { onSetVitalsAsBars(true) })
        }
        "SPEEDOMETER" -> {
            add(MenuAction("Dial", Icons.Rounded.Speed, checked = !settings.speedometerDigitalOnly, groupStart = true) { onSetSpeedometerDigitalOnly(false) })
            add(MenuAction("Number Only", Icons.Rounded.Numbers, checked = settings.speedometerDigitalOnly) { onSetSpeedometerDigitalOnly(true) })
        }
        "NOW_PLAYING" -> {
            add(MenuAction(if (settings.carPlayPackage.isEmpty()) "Choose CarPlay App" else "Change CarPlay App", Icons.Rounded.PhoneIphone, groupStart = true) { onAssignCarPlay() })
            if (settings.carPlayPackage.isNotEmpty()) add(MenuAction("Clear CarPlay App", Icons.Rounded.Close, destructive = true) { onClearCarPlay() })
            add(MenuAction(if (settings.androidAutoPackage.isEmpty()) "Choose Android Auto App" else "Change Android Auto App", Icons.Rounded.DirectionsCar) { onAssignAndroidAuto() })
            if (settings.androidAutoPackage.isNotEmpty()) add(MenuAction("Clear Android Auto App", Icons.Rounded.Close, destructive = true) { onClearAndroidAuto() })
        }
    }
}

@Composable
private fun CornerBadge(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = Hig.colors
    Box(
        Modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(c.fill2)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = c.label, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun WidgetContent(
    id: String,
    settings: AppSettings,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    location: LocationData?,
    bearing: Float,
    isOnline: Boolean,
    isDayMode: Boolean,
    editMode: Boolean,
    metric: Boolean,
    accent: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onUpdateSoundPad: (Int, com.openlauncher.app.data.SoundPadConfig) -> Unit,
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState?,
    onLaunchHardwareRadio: () -> Unit,
    onStopHardwareRadio: () -> Unit,
    onRadioSeekUp: () -> Unit,
    onRadioSeekDown: () -> Unit,
    onRadioCycleFm: () -> Unit,
    onRadioSwitchAm: () -> Unit,
    onRadioTune: (String, Float) -> Unit,
    onAssignRadio: () -> Unit,
) {
    val m = Modifier.fillMaxSize()
    when (id) {
        "CLOCK" -> ClockWidget(style = settings.clockStyle, vehicleName = settings.vehicleName, modifier = m)
        "WEATHER" -> WeatherWidget(state = weather, metric = metric, isOnline = isOnline, modifier = m)
        "NOW_PLAYING" -> NowPlayingWidget(
            state = nowPlaying, accent = accent,
            carPlayPackage = settings.carPlayPackage, androidAutoPackage = settings.androidAutoPackage,
            onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev,
            onLaunchCarPlay = onLaunchCarPlay, onLaunchAndroidAuto = onLaunchAndroidAuto,
            onTapToOpenApp = onTapNowPlaying, modifier = m, isEditing = editMode, isDayMode = isDayMode,
            hardwareRadio = hardwareRadio, onLaunchHardwareRadio = onLaunchHardwareRadio,
            onStopHardwareRadio = onStopHardwareRadio, onRadioSeekUp = onRadioSeekUp,
            onRadioSeekDown = onRadioSeekDown, onRadioCycleFm = onRadioCycleFm,
            onRadioSwitchAm = onRadioSwitchAm, onRadioTune = onRadioTune, onAssignRadio = onAssignRadio
        )
        "TELEMETRY" -> TelemetryWidget(
            location = location, bearing = (bearing + settings.compassOffset + 360f) % 360f,
            metric = metric, modifier = m
        )
        "ALTIMETER" -> AltimeterWidget(location = location, isMetric = metric, accent = accent, isDayMode = isDayMode, modifier = m)
        "SPEEDOMETER" -> SpeedometerWidget(location = location, isMetric = metric, accent = accent, isDayMode = isDayMode, digitalOnly = settings.speedometerDigitalOnly, modifier = m)
        "VITALS" -> VitalsWidget(accent = accent, isDayMode = isDayMode, asBars = settings.vitalsAsBars, modifier = m)
        "TRIP_TRACKER" -> TripTrackerWidget(location = location, isMetric = metric, accent = accent, isDayMode = isDayMode, modifier = m)
        "SOUNDBOARD" -> SoundboardWidget(pads = settings.soundboardPads, accent = accent, isDayMode = isDayMode, isEditing = editMode, onUpdatePad = onUpdateSoundPad, modifier = m)
    }
}

/** iOS widget gallery: a sheet that rises over the home screen. */
@Composable
private fun WidgetGallery(open: Boolean, settings: AppSettings, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Hig.colors
    val active = settings.activeWidgetIds()
    val canAdd = canAddWidget(settings)
    AnimatedVisibility(open, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
        Box(
            Modifier.fillMaxSize().background(c.scrim)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDismiss)
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            open,
            enter = slideInVertically(tween(380)) { it },
            exit = slideOutVertically(tween(260)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(c.sheet)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                Box(Modifier.padding(top = 10.dp).size(44.dp, 5.dp).clip(CircleShape).background(c.fill2).align(Alignment.CenterHorizontally))
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Widget", style = Hig.type.largeTitle, color = c.label, modifier = Modifier.weight(1f))
                    PillButton("Cancel", onClick = onDismiss)
                }
                if (!canAdd) Text(
                    "Your Home Screen is full. Remove a widget to make room.",
                    style = Hig.type.callout, color = c.label2,
                    modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 12.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(230.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(ALL_WIDGET_TYPES, key = { it.id }) { info ->
                        val added = info.id in active
                        val enabled = !added && canAdd
                        Column(
                            Modifier
                                .heightIn(min = 150.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (c.isDark) c.tile else Color.White)
                                .graphicsLayer { alpha = if (enabled || added) 1f else 0.45f }
                                .clickable(enabled = enabled) { onAdd(info.id) }
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(info.icon, null, tint = c.tint, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.weight(1f))
                            Text(info.name, style = Hig.type.headline, color = c.label)
                            if (added) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Check, null, tint = c.label2, modifier = Modifier.size(18.dp))
                                Text("On Home Screen", style = Hig.type.footnote, color = c.label2)
                            } else Text(info.description, style = Hig.type.footnote, color = c.label2)
                        }
                    }
                }
            }
        }
    }
}
