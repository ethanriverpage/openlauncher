package com.openlauncher.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import com.openlauncher.app.ui.theme.HigMotion
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.openlauncher.app.ui.theme.Hig

/* ─────────────────────────────────────────────────────────────────────────────
 * HIG building blocks shared by every screen. Surfaces separate by elevation
 * step (bg → tile → fill), never by borders; one tint marks live state.
 * ───────────────────────────────────────────────────────────────────────────── */

val TileShape = RoundedCornerShape(28.dp)
val ControlShape = RoundedCornerShape(16.dp)
val GroupShape = RoundedCornerShape(16.dp)

/** Gentle press feedback, the way iOS buttons dip under a finger. */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = 900f), label = "press")
    return this
        .scale(s)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
fun HigTile(
    modifier: Modifier = Modifier,
    color: Color = Hig.colors.tile,
    shape: Shape = TileShape,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.clip(shape).background(color), content = content)
}

/** Capsule button. Primary is the filled tint; secondary sits on the fill colour. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Hig.colors
    Row(
        modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (primary) c.tint else c.fill)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val fg = when {
            !enabled -> c.label3
            primary  -> c.onTint
            else     -> c.label
        }
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
        Text(text, style = Hig.type.headline, color = fg, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

/** Round icon button (media transport, steppers). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 40.dp,
    background: Color = Color.Transparent,
    tint: Color = Hig.colors.label,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .pressable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (enabled) tint else Hig.colors.label3, modifier = Modifier.size(iconSize))
    }
}

/** Rounded-rect stepper / control button (climate arrows, fan −/+). */
@Composable
fun StepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Hig.colors
    Box(
        modifier
            .size(width = 76.dp, height = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.fill)
            .pressable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (enabled) c.label else c.label3, modifier = Modifier.size(38.dp))
    }
}

/** iOS segmented control. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Hig.colors
    val density = LocalDensity.current
    // Segment bounds in the row, measured after layout; the thumb slides between them
    val bounds = remember(options.size) { mutableStateListOf<Pair<Float, Float>?>().apply { repeat(options.size) { add(null) } } }
    val index = options.indexOfFirst { it.first == selected }
    val thumbX = remember { Animatable(0f) }
    val thumbW = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    val target = bounds.getOrNull(index)
    LaunchedEffect(target) {
        val (x, w) = target ?: return@LaunchedEffect
        if (!placed) { thumbX.snapTo(x); thumbW.snapTo(w); placed = true; return@LaunchedEffect }
        launch { thumbX.animateTo(x, spring(dampingRatio = 0.86f, stiffness = 520f)) }
        thumbW.animateTo(w, spring(dampingRatio = 0.86f, stiffness = 520f))
    }
    val thumb = if (c.isDark) Color(0xFF636366) else Color.White
    val thumbShadow = Color.Black.copy(alpha = if (c.isDark) 0f else 0.10f)
    val radius = with(density) { 9.dp.toPx() }
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.fill)
            .padding(3.dp)
            // Drawn after the padding, so it shares the segments' coordinate space
            .drawBehind {
                if (!placed || index < 0) return@drawBehind
                val r = androidx.compose.ui.geometry.CornerRadius(radius)
                // A soft 1dp lip stands in for the iOS thumb shadow by day
                drawRoundRect(thumbShadow, Offset(thumbX.value, 1.dp.toPx()), androidx.compose.ui.geometry.Size(thumbW.value, size.height), r)
                drawRoundRect(thumb, Offset(thumbX.value, 0f), androidx.compose.ui.geometry.Size(thumbW.value, size.height), r)
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, (value, label) ->
            val on = value == selected
            val fg by animateColorAsState(if (on) c.label else c.label2, HigMotion.feedback(), label = "segment")
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .onPlaced { lc -> bounds[i] = lc.positionInParent().x to lc.size.width.toFloat() }
                    .clip(RoundedCornerShape(9.dp))
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .semantics { stateDescription = if (on) "Selected" else "" }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = Hig.type.subhead.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/** iOS switch: green track, white knob. */
@Composable
fun HigSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Hig.colors
    val track by animateColorAsState(if (checked) c.green else c.fill2, label = "track")
    val x by animateDpAsState(if (checked) 26.dp else 0.dp, spring(dampingRatio = 0.75f, stiffness = 700f), label = "knob")
    Box(
        modifier
            .size(width = 64.dp, height = 38.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { stateDescription = if (checked) "On" else "Off" }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .size(32.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** iOS slider: thin tinted track, large white knob. */
@Composable
fun HigSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Float = 0f,
    contentDescription: String = "",
) {
    val c = Hig.colors
    var widthPx by remember { mutableIntStateOf(1) }
    val knobPx = with(LocalDensity.current) { 30.dp.toPx() }
    val span = valueRange.endInclusive - valueRange.start
    val frac = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    fun toValue(x: Float): Float {
        val f = ((x - knobPx / 2) / (widthPx - knobPx)).coerceIn(0f, 1f)
        var v = valueRange.start + f * span
        if (steps > 0f) v = (Math.round((v - valueRange.start) / steps) * steps) + valueRange.start
        return v.coerceIn(valueRange.start, valueRange.endInclusive)
    }
    val current by rememberUpdatedState(onValueChange)
    Box(
        modifier
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(valueRange) { detectTapGestures { current(toValue(it.x)) } }
            .pointerInput(valueRange) { detectHorizontalDragGestures { ch, _ -> ch.consume(); current(toValue(ch.position.x)) } },
        contentAlignment = Alignment.CenterStart
    ) {
        val travel = with(LocalDensity.current) { (widthPx - knobPx).coerceAtLeast(0f).toDp() }
        Box(Modifier.fillMaxWidth().padding(horizontal = 15.dp).height(6.dp).clip(CircleShape).background(c.fill2))
        Box(Modifier.padding(start = 15.dp).width(travel * frac).height(6.dp).clip(CircleShape).background(c.tint))
        Box(
            Modifier
                .offset(x = travel * frac)
                .size(30.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/* ── Inset grouped lists (Settings) ───────────────────────────────────────── */

@Composable
fun GroupedSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>,
) {
    val c = Hig.colors
    Column(modifier.fillMaxWidth().padding(bottom = 30.dp)) {
        if (header != null) Text(
            header.uppercase(), style = Hig.type.footnote, color = c.label2,
            modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
        )
        Column(Modifier.fillMaxWidth().clip(GroupShape).background(c.tile)) {
            rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = c.separator, modifier = Modifier.padding(start = 18.dp))
                row()
            }
        }
        if (footer != null) Text(
            footer, style = Hig.type.footnote, color = c.label2,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp)
        )
    }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = Hig.colors.label,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Hig.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = Hig.type.body, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) Text(subtitle, style = Hig.type.subhead, color = c.label2, modifier = Modifier.padding(top = 2.dp))
        }
        trailing?.invoke(this)
    }
}

/** Navigation row: value on the right, then a chevron. */
@Composable
fun DisclosureRow(title: String, value: String = "", subtitle: String? = null, onClick: () -> Unit) {
    val c = Hig.colors
    ListRow(title = title, subtitle = subtitle, onClick = onClick) {
        if (value.isNotEmpty()) Text(value, style = Hig.type.body, color = c.label2, maxLines = 1)
        Icon(Icons.Rounded.ChevronRight, null, tint = c.label3, modifier = Modifier.size(28.dp))
    }
}

/** Coloured rounded-square glyph, as in the iOS Settings list. */
@Composable
fun SquircleGlyph(icon: ImageVector, color: Color, size: Dp = 34.dp, iconSize: Dp = 22.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.27f)).background(color),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(iconSize)) }
}

/* ── Context menu ─────────────────────────────────────────────────────────── */

data class MenuAction(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val checked: Boolean = false,
    val enabled: Boolean = true,
    /** Draws a thick group gap above this item, as iOS separates destructive actions. */
    val groupStart: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * iOS-style context menu anchored to its parent. Place inside the anchor's Box.
 */
@Composable
fun ContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: List<MenuAction>,
    offset: IntOffset = IntOffset(0, 0),
    alignment: Alignment = Alignment.TopEnd,
    header: (@Composable () -> Unit)? = null,
) {
    // Stay composed through the exit so the menu can fade out instead of vanishing
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    if (!visible.currentState && !visible.targetState) return
    val c = Hig.colors
    // Grow out of the corner the menu is anchored to, like an iOS context menu
    val origin = when (alignment) {
        Alignment.TopStart -> TransformOrigin(0f, 0f)
        Alignment.BottomStart -> TransformOrigin(0f, 1f)
        Alignment.BottomEnd -> TransformOrigin(1f, 1f)
        else -> TransformOrigin(1f, 0f)
    }
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = scaleIn(HigMotion.pop(), initialScale = 0.86f, transformOrigin = origin) + fadeIn(HigMotion.feedback()),
            exit = scaleOut(HigMotion.feedback(), targetScale = 0.94f, transformOrigin = origin) + fadeOut(tween(110))
        ) {
        Column(
            Modifier
                .width(300.dp)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(c.menu)
        ) {
            header?.invoke()
            actions.forEachIndexed { i, a ->
                if (i > 0 || header != null) {
                    if (a.groupStart) Box(Modifier.fillMaxWidth().height(8.dp).background(Color.Black.copy(alpha = 0.18f)))
                    else HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(enabled = a.enabled) { a.onClick(); onDismiss() }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fg = when { !a.enabled -> c.label3; a.destructive -> c.red; else -> c.label }
                    Box(Modifier.width(28.dp)) {
                        if (a.checked) Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(22.dp))
                    }
                    Text(a.label, style = Hig.type.body, color = fg, modifier = Modifier.weight(1f))
                    if (a.icon != null) Icon(a.icon, null, tint = fg, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    }
}

/** Centered iOS alert. */
@Composable
fun HigAlert(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Hig.colors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(380.dp).clip(RoundedCornerShape(20.dp)).background(c.menu),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = Hig.type.headline, color = c.label)
                Text(
                    message, style = Hig.type.subhead, color = c.label2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = c.separator)
            Row(Modifier.fillMaxWidth().height(60.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("Cancel", style = Hig.type.headline, color = c.tint)
                }
                Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onConfirm), contentAlignment = Alignment.Center) {
                    Text(confirmLabel, style = Hig.type.body, color = if (destructive) c.red else c.tint)
                }
            }
        }
    }
}

/** Small icon + label line at the top of a widget (e.g. "My Location"). */
@Composable
fun WidgetCaption(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, style: TextStyle = Hig.type.footnote) {
    val c = Hig.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) Icon(icon, null, tint = c.label2, modifier = Modifier.size(18.dp))
        Text(text, style = style.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
