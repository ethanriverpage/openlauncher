package com.openlauncher.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DefaultShortcutIcon
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.ui.theme.Hig
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * CarPlay-style sidebar: status on top, app shortcuts as squircle icons in the middle,
 * and the destinations grouped in one capsule at the end. Lays out vertically on the
 * left/right edge or horizontally along the bottom.
 */
@Composable
fun Sidebar(
    currentDest: NavDestination,
    settings: AppSettings,
    installedIconFor: (String) -> Drawable?,
    onNavigate: (NavDestination) -> Unit,
    onShortcutClick: (Int) -> Unit,
    onShortcutLongPress: (Int) -> Unit,
    onShortcutRemove: (Int) -> Unit,
    onShortcutSetIcon: (Int, DefaultShortcutIcon?) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    isHorizontal: Boolean = false,
    showClimate: Boolean = false,
    isWifi: Boolean = false,
    isData: Boolean = false,
    alignRight: Boolean = false,
    modifier: Modifier = Modifier
) {
    var menuSlot       by remember { mutableStateOf<Int?>(null) }
    var iconPickerSlot by remember { mutableStateOf<Int?>(null) }
    var draggingIndex  by remember { mutableIntStateOf(-1) }
    var dragOffsetPx   by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier) {
        // Short screens shrink the icons rather than scroll them: 1600×597 dp is compact,
        // 1280×480 dp (TS10 at 240 dpi) is tight.
        val tight     = !isHorizontal && maxHeight < 540.dp
        val compact   = !isHorizontal && maxHeight < 660.dp
        val iconSize  = when { tight -> 46.dp; compact -> 54.dp; else -> 64.dp }
        val navBtnH   = when { tight -> 42.dp; compact -> 48.dp; else -> 56.dp }
        val slotGap   = when { tight -> 8.dp;  compact -> 10.dp; else -> 12.dp }
        val navGap    = if (tight) 4.dp else 6.dp
        val stepPx    = with(LocalDensity.current) { (iconSize + slotGap).toPx() }

        fun target(): Int = if (draggingIndex < 0) -1 else
            (draggingIndex + (dragOffsetPx / stepPx).roundToInt()).coerceIn(0, settings.shortcuts.size - 1)

        fun shift(index: Int): Float {
            if (draggingIndex < 0 || index == draggingIndex) return 0f
            val from = draggingIndex; val to = target()
            return when {
                from < to && index in (from + 1)..to -> -stepPx
                from > to && index in to until from  -> stepPx
                else -> 0f
            }
        }

        val shortcuts: @Composable () -> Unit = {
            settings.shortcuts.forEachIndexed { index, shortcut ->
                val dragging = index == draggingIndex
                Box {
                    ShortcutSlot(
                        shortcut     = shortcut,
                        resolvedIcon = shortcut.packageName.takeIf { it.isNotEmpty() }?.let(installedIconFor),
                        size         = iconSize,
                        dragging     = dragging,
                        translation  = if (dragging) dragOffsetPx else shift(index),
                        horizontal   = isHorizontal,
                        onClick      = {
                            if (shortcut.packageName.isNotEmpty()) onShortcutClick(index) else onShortcutLongPress(index)
                        },
                        onLongPress  = { menuSlot = index },
                        onDragStart  = { draggingIndex = index; dragOffsetPx = 0f },
                        onDragDelta  = { dragOffsetPx += it },
                        onDragEnd    = {
                            val from = draggingIndex; val to = target()
                            if (from >= 0 && to != from) onReorder(from, to)
                            draggingIndex = -1; dragOffsetPx = 0f
                        }
                    )
                    val density = LocalDensity.current
                    val gapPx = with(density) { 12.dp.roundToPx() }
                    val sizePx = with(density) { iconSize.roundToPx() }
                    val assigned = shortcut.packageName.isNotEmpty()
                    ContextMenu(
                        expanded  = menuSlot == index,
                        onDismiss = { menuSlot = null },
                        alignment = when {
                            isHorizontal -> Alignment.BottomStart
                            alignRight   -> Alignment.TopEnd
                            else         -> Alignment.TopStart
                        },
                        offset = when {
                            isHorizontal -> IntOffset(0, -(sizePx + gapPx))
                            alignRight   -> IntOffset(-(sizePx + gapPx), 0)
                            else         -> IntOffset(sizePx + gapPx, 0)
                        },
                        actions = buildList {
                            add(MenuAction(if (assigned) "Change App" else "Choose App", Icons.Rounded.SwapHoriz) { onShortcutLongPress(index) })
                            add(MenuAction("Change Icon", Icons.Rounded.Palette) { iconPickerSlot = index })
                            add(MenuAction("Remove", Icons.Rounded.Delete, destructive = true, groupStart = true) { onShortcutRemove(index) })
                        }
                    )
                }
            }
        }

        val nav: @Composable () -> Unit = {
            NavButton(Icons.Rounded.Home, Icons.Outlined.Home, "Home", currentDest == NavDestination.HOME, navBtnH) { onNavigate(NavDestination.HOME) }
            NavButton(Icons.Rounded.Apps, Icons.Outlined.Apps, "Apps", currentDest == NavDestination.APP_LIBRARY, navBtnH) { onNavigate(NavDestination.APP_LIBRARY) }
            if (showClimate) NavButton(Icons.Rounded.AcUnit, Icons.Outlined.AcUnit, "Climate", currentDest == NavDestination.CLIMATE, navBtnH) { onNavigate(NavDestination.CLIMATE) }
            NavButton(Icons.Rounded.Settings, Icons.Outlined.Settings, "Settings", currentDest == NavDestination.SETTINGS, navBtnH) { onNavigate(NavDestination.SETTINGS) }
        }

        val c = Hig.colors
        if (isHorizontal) {
            Row(
                Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val shortcutsRow: @Composable RowScope.() -> Unit = {
                    Row(
                        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(slotGap, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) { shortcuts() }
                }
                val navGroup: @Composable () -> Unit = {
                    Row(
                        Modifier.clip(RoundedCornerShape(22.dp)).background(c.tile).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) { nav() }
                }
                StatusBlock(isWifi, isData, horizontal = true)
                if (settings.bottomBarShortcutsRight) { navGroup(); shortcutsRow() }
                else { shortcutsRow(); navGroup() }
            }
        } else {
            Column(
                Modifier.width(if (tight) 88.dp else 104.dp).fillMaxHeight().padding(vertical = if (tight) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (tight) 8.dp else 10.dp)
            ) {
                StatusBlock(isWifi, isData, horizontal = false)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(slotGap, Alignment.CenterVertically)
                ) { shortcuts() }
                Column(
                    Modifier.clip(RoundedCornerShape(if (tight) 20.dp else 22.dp)).background(c.tile).padding(navGap),
                    verticalArrangement = Arrangement.spacedBy(navGap)
                ) { nav() }
            }
        }
    }

    iconPickerSlot?.let { slot ->
        IconPickerDialog(
            hasNativeIcon   = settings.shortcuts.getOrNull(slot)?.packageName?.isNotEmpty() == true,
            currentOverride = settings.shortcuts.getOrNull(slot)?.customIconOverride,
            onPick    = { iconPickerSlot = null; onShortcutSetIcon(slot, it) },
            onDismiss = { iconPickerSlot = null }
        )
    }
}

@Composable
private fun StatusBlock(isWifi: Boolean, isData: Boolean, horizontal: Boolean) {
    val c = Hig.colors
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    // Same 12/24-hour rule as the Clock widget, so the two never disagree
    val is24 = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)
    val time = if (is24) "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
               else "%d:%02d".format(now.get(Calendar.HOUR).let { if (it == 0) 12 else it }, now.get(Calendar.MINUTE))
    val icons: @Composable () -> Unit = {
        if (isWifi) Icon(Icons.Rounded.Wifi, "Wi-Fi", tint = c.label2, modifier = Modifier.size(16.dp))
        else if (isData) Icon(Icons.Rounded.SignalCellularAlt, "Mobile data", tint = c.label2, modifier = Modifier.size(16.dp))
        else Icon(Icons.Rounded.WifiOff, "Offline", tint = c.label3, modifier = Modifier.size(16.dp))
    }
    if (horizontal) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(time, style = Hig.type.headline, color = c.label); icons()
    } else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(time, style = Hig.type.headline, color = c.label); icons()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutSlot(
    shortcut: ShortcutConfig,
    resolvedIcon: Drawable?,
    size: Dp,
    dragging: Boolean,
    translation: Float,
    horizontal: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val click by rememberUpdatedState(onClick)
    val long  by rememberUpdatedState(onLongPress)
    val start by rememberUpdatedState(onDragStart)
    val delta by rememberUpdatedState(onDragDelta)
    val end   by rememberUpdatedState(onDragEnd)
    val label = shortcut.label.ifEmpty { if (shortcut.packageName.isEmpty()) "Add shortcut" else shortcut.packageName }

    Box(
        Modifier
            .size(size)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                if (horizontal) translationX = translation else translationY = translation
                val s = if (dragging) 1.08f else 1f
                scaleX = s; scaleY = s
            }
            .semantics { contentDescription = label }
            .combinedClickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { click() }, onLongClick = {})
            .pointerInput(horizontal) {
                var significant = false
                var total = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { significant = false; total = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        val d = if (horizontal) amount.x else amount.y
                        total += d
                        if (!significant && kotlin.math.abs(total) > viewConfiguration.touchSlop) { significant = true; start() }
                        if (significant) delta(d)
                    },
                    onDragEnd = { if (significant) end() else long(); significant = false },
                    onDragCancel = { if (significant) end(); significant = false }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ShortcutIcon(shortcut, resolvedIcon, size)
    }
}

/** App shortcut glyph: the installed app's icon, or an HIG-coloured squircle. */
@Composable
fun ShortcutIcon(shortcut: ShortcutConfig, resolvedIcon: Drawable?, size: Dp) {
    val c = Hig.colors
    val override = shortcut.customIconOverride?.takeIf { it != DefaultShortcutIcon.NONE }
    val radius = RoundedCornerShape(size * 0.235f)
    when {
        override != null -> GlyphSquircle(override, size)
        resolvedIcon != null -> {
            val px = with(LocalDensity.current) { size.roundToPx() }
            val bmp = remember(resolvedIcon, px) { runCatching { resolvedIcon.toBitmap(px, px) }.getOrNull() }
            if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(size))
            else GlyphSquircle(DefaultShortcutIcon.NONE, size)
        }
        shortcut.isDefault -> GlyphSquircle(shortcut.defaultIcon, size)
        else -> Box(
            Modifier.size(size).clip(radius).border(2.dp, c.fill2, radius),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Add, null, tint = c.label3, modifier = Modifier.size(size * 0.5f)) }
    }
}

@Composable
private fun GlyphSquircle(icon: DefaultShortcutIcon, size: Dp) {
    val (top, bottom) = icon.colors()
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.235f))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center
    ) { Icon(icon.toIcon(), null, tint = Color.White, modifier = Modifier.size(size * 0.53f)) }
}

@Composable
private fun NavButton(
    selectedIcon: ImageVector,
    icon: ImageVector,
    label: String,
    active: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    val c = Hig.colors
    // The selection fill crossfades from the old destination to the new one
    val fill by androidx.compose.animation.animateColorAsState(
        if (active) c.fill else c.fill.copy(alpha = 0f), com.openlauncher.app.ui.theme.HigMotion.state(), label = "nav"
    )
    Box(
        Modifier
            .size(width = 64.dp, height = height)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(if (active) selectedIcon else icon, null, tint = if (active) c.label else c.label2, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun IconPickerDialog(
    hasNativeIcon: Boolean,
    currentOverride: DefaultShortcutIcon?,
    onPick: (DefaultShortcutIcon?) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Hig.colors
    val options = DefaultShortcutIcon.entries.filter { it != DefaultShortcutIcon.NONE }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(28.dp)).background(c.sheet).padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose Icon", style = Hig.type.title2, color = c.label, modifier = Modifier.weight(1f))
                PillButton("Cancel", onClick = onDismiss)
            }
            if (hasNativeIcon) {
                Spacer(Modifier.height(16.dp))
                ListRow(
                    title = "App's own icon",
                    modifier = Modifier.clip(GroupShape).background(c.tile),
                    onClick = { onPick(null) },
                    trailing = { if (currentOverride == null) Icon(Icons.Rounded.Check, null, tint = c.tint) }
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(options) { opt ->
                    Box(
                        Modifier
                            .size(76.dp)
                            .then(
                                if (opt == currentOverride) Modifier.border(3.dp, c.tint, RoundedCornerShape(20.dp)).padding(5.dp)
                                else Modifier
                            )
                            .clickable { onPick(opt) }
                            .semantics { contentDescription = opt.name.lowercase().replace('_', ' ') },
                        contentAlignment = Alignment.Center
                    ) { GlyphSquircle(opt, 64.dp) }
                }
            }
        }
    }
}

/** HIG system-colour pairs per glyph family, so default shortcuts read like app icons. */
private fun DefaultShortcutIcon.colors(): Pair<Color, Color> = when (this) {
    DefaultShortcutIcon.RADIO, DefaultShortcutIcon.MUSIC, DefaultShortcutIcon.FAVORITE ->
        Color(0xFFFF375F) to Color(0xFFD70F3C)
    DefaultShortcutIcon.PHONE, DefaultShortcutIcon.MESSAGE ->
        Color(0xFF34C759) to Color(0xFF16A34A)
    DefaultShortcutIcon.MAP, DefaultShortcutIcon.NAVIGATION, DefaultShortcutIcon.GLOBE, DefaultShortcutIcon.BLUETOOTH, DefaultShortcutIcon.WIFI ->
        Color(0xFF0A84FF) to Color(0xFF0060DF)
    DefaultShortcutIcon.AC, DefaultShortcutIcon.THERMOSTAT ->
        Color(0xFF40C8E0) to Color(0xFF1C9CC0)
    DefaultShortcutIcon.SPEAKER, DefaultShortcutIcon.HEADSET, DefaultShortcutIcon.EQUALIZER, DefaultShortcutIcon.VOLUME_UP ->
        Color(0xFFFF9F0A) to Color(0xFFE07800)
    DefaultShortcutIcon.LIGHTBULB, DefaultShortcutIcon.BRIGHTNESS, DefaultShortcutIcon.STAR, DefaultShortcutIcon.GAS_STATION ->
        Color(0xFFFFCC00) to Color(0xFFE6A100)
    DefaultShortcutIcon.TV, DefaultShortcutIcon.TIMER, DefaultShortcutIcon.DASHBOARD ->
        Color(0xFF5E5CE6) to Color(0xFF3F3DC8)
    else -> Color(0xFF8E8E93) to Color(0xFF5A5A5F)
}

fun DefaultShortcutIcon.toIcon(): ImageVector = when (this) {
    DefaultShortcutIcon.RADIO       -> Icons.Rounded.Radio
    DefaultShortcutIcon.CAMERA      -> Icons.Rounded.Videocam
    DefaultShortcutIcon.PHONE       -> Icons.Rounded.Call
    DefaultShortcutIcon.MAP         -> Icons.Rounded.Map
    DefaultShortcutIcon.NAVIGATION  -> Icons.Rounded.Navigation
    DefaultShortcutIcon.CAR         -> Icons.Rounded.DirectionsCar
    DefaultShortcutIcon.GAS_STATION -> Icons.Rounded.LocalGasStation
    DefaultShortcutIcon.DASHBOARD   -> Icons.Rounded.Speed
    DefaultShortcutIcon.MUSIC       -> Icons.Rounded.MusicNote
    DefaultShortcutIcon.SPEAKER     -> Icons.Rounded.Speaker
    DefaultShortcutIcon.HEADSET     -> Icons.Rounded.Headset
    DefaultShortcutIcon.EQUALIZER   -> Icons.Rounded.Equalizer
    DefaultShortcutIcon.VOLUME_UP   -> Icons.Rounded.VolumeUp
    DefaultShortcutIcon.BLUETOOTH   -> Icons.Rounded.Bluetooth
    DefaultShortcutIcon.WIFI        -> Icons.Rounded.Wifi
    DefaultShortcutIcon.LIGHTBULB   -> Icons.Rounded.Lightbulb
    DefaultShortcutIcon.BRIGHTNESS  -> Icons.Rounded.BrightnessHigh
    DefaultShortcutIcon.AC          -> Icons.Rounded.AcUnit
    DefaultShortcutIcon.THERMOSTAT  -> Icons.Rounded.Thermostat
    DefaultShortcutIcon.TV          -> Icons.Rounded.Tv
    DefaultShortcutIcon.VIDEOCAM    -> Icons.Rounded.Videocam
    DefaultShortcutIcon.STAR        -> Icons.Rounded.Star
    DefaultShortcutIcon.MESSAGE     -> Icons.Rounded.ChatBubble
    DefaultShortcutIcon.TIMER       -> Icons.Rounded.Timer
    DefaultShortcutIcon.LOCK        -> Icons.Rounded.Lock
    DefaultShortcutIcon.SETTINGS    -> Icons.Rounded.Settings
    DefaultShortcutIcon.FAVORITE    -> Icons.Rounded.Favorite
    DefaultShortcutIcon.GLOBE       -> Icons.Rounded.Language
    DefaultShortcutIcon.NONE        -> Icons.Rounded.Apps
}
