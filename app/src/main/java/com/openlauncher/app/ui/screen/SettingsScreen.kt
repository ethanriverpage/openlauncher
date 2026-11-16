package com.openlauncher.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.data.UnitSystem
import com.openlauncher.app.ui.components.*
import com.openlauncher.app.ui.theme.Hig
import com.openlauncher.app.ui.theme.TintPresets
import com.openlauncher.app.ui.theme.presetFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Section(val title: String, val icon: ImageVector, val color: Color) {
    APPEARANCE("Appearance", Icons.Rounded.Contrast, Color(0xFF0A84FF)),
    HOME("Home Screen", Icons.Rounded.Widgets, Color(0xFF5E5CE6)),
    SIDEBAR("Sidebar", Icons.Rounded.ViewSidebar, Color(0xFFFF9F0A)),
    VEHICLE("Vehicle", Icons.Rounded.DirectionsCar, Color(0xFF34C759)),
    LOCATION("Location & Compass", Icons.Rounded.Explore, Color(0xFFFF453A)),
    PERMISSIONS("Permissions", Icons.Rounded.Lock, Color(0xFF8E8E93)),
    ABOUT("About", Icons.Rounded.Info, Color(0xFF8E8E93)),
}

private val SECTION_GROUPS = listOf(
    listOf(Section.APPEARANCE, Section.HOME, Section.SIDEBAR),
    listOf(Section.VEHICLE, Section.LOCATION),
    listOf(Section.PERMISSIONS, Section.ABOUT),
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    onReset: () -> Unit,
    hasClimate: Boolean = false,
    onAssignCarPlay: () -> Unit = {},
    onAssignAndroidAuto: () -> Unit = {},
    onAssignRadio: () -> Unit = {},
    onAssignShortcut: (Int) -> Unit = {},
    onEditHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = Hig.colors
    var section by rememberSaveableSection()

    Row(modifier.fillMaxSize()) {
        // ── Section list ──────────────────────────────────────────────────────
        Column(
            Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 16.dp, top = 22.dp, bottom = 22.dp)
        ) {
            Text("Settings", style = Hig.type.largeTitle, color = c.label, modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
            SECTION_GROUPS.forEach { group ->
                Column(Modifier.fillMaxWidth().padding(bottom = 22.dp).clip(GroupShape).background(c.tile)) {
                    group.forEachIndexed { i, s ->
                        val on = s == section
                        if (i > 0 && !on && group[i - 1] != section) HorizontalDivider(thickness = 0.5.dp, color = c.separator, modifier = Modifier.padding(start = 62.dp))
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 60.dp)
                                .background(if (on) c.tint else Color.Transparent)
                                .clickable { section = s }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SquircleGlyph(s.icon, s.color)
                            Text(s.title, style = Hig.type.body, color = if (on) c.onTint else c.label, maxLines = 1)
                        }
                    }
                }
            }
        }

        // ── Detail ─────────────────────────────────────────────────────────────
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 28.dp, top = 22.dp, bottom = 40.dp)
        ) {
            Text(section.title, style = Hig.type.largeTitle, color = c.label, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))
            when (section) {
                Section.APPEARANCE -> Appearance(settings, onUpdate)
                Section.HOME -> HomeSection(settings, onUpdate, onEditHome)
                Section.SIDEBAR -> SidebarSection(settings, onUpdate, onAssignShortcut)
                Section.VEHICLE -> VehicleSection(settings, onUpdate, onAssignCarPlay, onAssignAndroidAuto, onAssignRadio)
                Section.LOCATION -> LocationSection(settings, onUpdate)
                Section.PERMISSIONS -> PermissionsSection()
                Section.ABOUT -> AboutSection(onReset)
            }
        }
    }
}

/** Survives leaving and returning to Settings within the session. */
@Composable
private fun rememberSaveableSection(): MutableState<Section> =
    androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(Section.APPEARANCE) }

// ── Appearance ────────────────────────────────────────────────────────────────

@Composable
private fun Appearance(settings: AppSettings, onUpdate: (AppSettings.() -> AppSettings) -> Unit) {
    val c = Hig.colors
    val context = LocalContext.current
    // OpenDocument, not GetContent: only SAF URIs carry a persistable grant across reboots
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onUpdate { copy(wallpaperUri = it.toString()) }
        }
    }

    GroupedSection("Display", footer = "Sunset switches at local sunrise and sunset, calculated offline.", rows = listOf {
        ListRow("Mode") {
            Segmented(
                listOf(DayNightMode.DARK to "Night", DayNightMode.LIGHT to "Day", DayNightMode.AUTO to "Sunset", DayNightMode.SYSTEM to "System"),
                settings.dayNightMode, { m -> onUpdate { copy(dayNightMode = m) } }
            )
        }
    })

    val current = presetFor(settings.accentColor)
    GroupedSection("Accent", footer = "Marks what's live or selected: the current screen, active climate modes, the compass heading.", rows = listOf {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TintPresets.forEach { p ->
                val on = p == current
                val col = if (c.isDark) p.dark else p.light
                Box(
                    Modifier.size(52.dp)
                        .then(if (on) Modifier.background(c.label2, CircleShape).padding(3.dp).background(c.tile, CircleShape).padding(3.dp) else Modifier.padding(6.dp))
                        .clip(CircleShape).background(col)
                        .clickable { onUpdate { copy(accentColor = p.dark.toArgb()) } }
                        .semantics { contentDescription = p.name + if (on) ", selected" else "" }
                )
            }
        }
    })

    GroupedSection("Text and Size", rows = listOf(
        {
            ListRow("Text Size") {
                Text("A", style = Hig.type.footnote, color = c.label2)
                HigSlider(settings.textScale, { v -> onUpdate { copy(textScale = v) } }, 0.85f..1.4f, Modifier.width(420.dp), steps = 0.05f, contentDescription = "Text size")
                Text("A", style = Hig.type.title1, color = c.label2)
            }
        },
        {
            ListRow("Display Scale", subtitle = "Fits the whole layout to your screen.") {
                Icon(Icons.Rounded.ZoomOut, null, tint = c.label2)
                HigSlider(settings.uiScale, { v -> onUpdate { copy(uiScale = v) } }, 0.7f..1.5f, Modifier.width(420.dp), steps = 0.05f, contentDescription = "Display scale")
                Icon(Icons.Rounded.ZoomIn, null, tint = c.label2)
            }
        }
    ))

    val hasWall = settings.wallpaperUri.isNotEmpty()
    GroupedSection(
        "Wallpaper",
        footer = "Tiles turn translucent over the image. Night mode dims it further.",
        rows = buildList<@Composable () -> Unit> {
            add {
                ListRow("Wallpaper", subtitle = if (hasWall) "Custom image" else "Off") {
                    HigSwitch(hasWall, { on -> if (on) picker.launch(arrayOf("image/*")) else onUpdate { copy(wallpaperUri = "") } })
                }
            }
            if (hasWall) {
                add { DisclosureRow("Choose Image", onClick = { picker.launch(arrayOf("image/*")) }) }
                add {
                    ListRow("Dim") {
                        Icon(Icons.Rounded.BrightnessHigh, null, tint = c.label2)
                        HigSlider(settings.wallpaperDim, { v -> onUpdate { copy(wallpaperDim = v) } }, 0.2f..0.85f, Modifier.width(420.dp), steps = 0.05f, contentDescription = "Wallpaper dim")
                        Icon(Icons.Rounded.Brightness3, null, tint = c.label2)
                    }
                }
            }
        }
    )
}

// ── Home Screen ──────────────────────────────────────────────────────────────

@Composable
private fun HomeSection(settings: AppSettings, onUpdate: (AppSettings.() -> AppSettings) -> Unit, onEditHome: () -> Unit) {
    val c = Hig.colors
    GroupedSection("Widgets", footer = "You can also long-press any widget.", rows = listOf {
        ListRow("Edit Home Screen", titleColor = c.tint, onClick = onEditHome)
    })
    GroupedSection("Clock", rows = listOf {
        ListRow("Style") {
            Segmented(listOf(ClockStyle.DIGITAL to "Digital", ClockStyle.ANALOG to "Analog"), settings.clockStyle, { s -> onUpdate { copy(clockStyle = s) } })
        }
    })
    GroupedSection("Units", rows = listOf {
        ListRow("Measurement", subtitle = "Speed, distance, temperature") {
            Segmented(listOf(UnitSystem.IMPERIAL to "US", UnitSystem.METRIC to "Metric"), settings.unitSystem, { u -> onUpdate { copy(unitSystem = u) } })
        }
    })
}

// ── Sidebar ──────────────────────────────────────────────────────────────────

@Composable
private fun SidebarSection(settings: AppSettings, onUpdate: (AppSettings.() -> AppSettings) -> Unit, onAssign: (Int) -> Unit) {
    val c = Hig.colors
    GroupedSection("Position", footer = "Right suits right-hand-drive cars.", rows = buildList<@Composable () -> Unit> {
        add {
            ListRow("Sidebar") {
                Segmented(
                    listOf(SidebarPosition.LEFT to "Left", SidebarPosition.RIGHT to "Right", SidebarPosition.BOTTOM to "Bottom"),
                    settings.sidebarPosition, { p -> onUpdate { copy(sidebarPosition = p) } }
                )
            }
        }
        if (settings.sidebarPosition == SidebarPosition.BOTTOM) add {
            ListRow("Shortcuts on the Right") { HigSwitch(settings.bottomBarShortcutsRight, { v -> onUpdate { copy(bottomBarShortcutsRight = v) } }) }
        }
    })

    GroupedSection(
        "Shortcuts",
        footer = "Drag a shortcut in the sidebar to reorder it. Long-press it to change its icon.",
        rows = settings.shortcuts.mapIndexed { i, sc ->
            @Composable {
                ListRow(
                    title = sc.label.ifEmpty { if (sc.packageName.isEmpty()) "Empty" else sc.packageName },
                    titleColor = if (sc.packageName.isEmpty()) c.label2 else c.label,
                    leading = { ShortcutIcon(sc, null, 40.dp) }
                ) {
                    Text(
                        if (sc.packageName.isEmpty()) "Choose" else "Change", style = Hig.type.body, color = c.tint,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onAssign(i) }.padding(8.dp)
                    )
                    if (settings.shortcuts.size > 1) CircleIconButton(
                        Icons.Rounded.RemoveCircle, "Remove shortcut ${i + 1}",
                        { onUpdate { copy(shortcuts = shortcuts.toMutableList().also { it.removeAt(i) }) } },
                        size = 48.dp, iconSize = 28.dp, tint = c.red
                    )
                }
            }
        } + listOf<@Composable () -> Unit>({
            ListRow("Add Shortcut", titleColor = c.tint, leading = { Icon(Icons.Rounded.AddCircle, null, tint = c.tint) },
                onClick = { onUpdate { copy(shortcuts = shortcuts + ShortcutConfig()) } })
        })
    )
}

// ── Vehicle ──────────────────────────────────────────────────────────────────

@Composable
private fun VehicleSection(
    settings: AppSettings,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onAssignRadio: () -> Unit,
) {
    val c = Hig.colors
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    fun label(pkg: String) = if (pkg.isEmpty()) "None" else runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    var name by remember(settings.vehicleName) { mutableStateOf(settings.vehicleName) }
    GroupedSection(footer = "Shown on the Clock widget.", rows = listOf {
        ListRow("Name") {
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                singleLine = true,
                textStyle = Hig.type.body.copy(color = c.label, textAlign = TextAlign.End),
                cursorBrush = SolidColor(c.tint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onUpdate { copy(vehicleName = name.trim()) }; focus.clearFocus() }),
                modifier = Modifier.width(280.dp).clip(RoundedCornerShape(12.dp)).background(c.fill).padding(horizontal = 14.dp, vertical = 12.dp)
            )
            if (name != settings.vehicleName) PillButton("Save", { onUpdate { copy(vehicleName = name.trim()) }; focus.clearFocus() }, primary = true)
        }
    })
    GroupedSection(
        "Phone Projection",
        footer = "When one of these is connected, Now Playing offers a shortcut to it.",
        rows = listOf(
            { DisclosureRow("CarPlay App", label(settings.carPlayPackage), onClick = onAssignCarPlay) },
            { DisclosureRow("Android Auto App", label(settings.androidAutoPackage), onClick = onAssignAndroidAuto) },
        )
    )
    GroupedSection("Tuner", footer = "On units without direct tuner access, Open Launcher mirrors this app's radio.", rows = listOf {
        DisclosureRow("Radio App", label(settings.radioPackage), onClick = onAssignRadio)
    })
}

// ── Location & Compass ───────────────────────────────────────────────────────

@Composable
private fun LocationSection(settings: AppSettings, onUpdate: (AppSettings.() -> AppSettings) -> Unit) {
    val c = Hig.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var sweep by remember { mutableIntStateOf(0) }

    GroupedSection(
        "Compass",
        footer = status ?: "Calibrate by driving two slow circles in an open lot; Android recalibrates the magnetometer as you turn.",
        rows = listOf(
            {
                val off = settings.compassOffset.roundToInt()
                ListRow("Heading Offset", subtitle = "Lines the compass up with the car's nose.") {
                    Text((if (off > 0) "+" else "") + "$off°", style = Hig.type.body, color = c.label2, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                    HigSlider(settings.compassOffset, { v -> onUpdate { copy(compassOffset = v) } }, -180f..180f, Modifier.width(360.dp), steps = 5f, contentDescription = "Compass heading offset")
                }
            },
            {
                ListRow(
                    if (sweep > 0) "Calibrating… ${sweep}s" else "Calibrate Compass",
                    titleColor = if (sweep > 0) c.label2 else c.tint,
                    onClick = {
                        if (sweep == 0) scope.launch {
                            sweep = 30
                            while (sweep > 0) { delay(1000); sweep-- }
                            status = "Calibration done. If the heading is still off, adjust the offset above."
                        }
                    }
                )
            }
        )
    )
    GroupedSection("GPS", footer = "Use if your position takes a long time to lock. Clears assistance data and forces a fresh satellite fix.", rows = listOf {
        ListRow("Reset A-GPS Data", titleColor = c.tint, onClick = {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            // "delete_aiding_data" is what AOSP's GPS provider recognises (ACCESS_LOCATION_EXTRA_COMMANDS)
            val ok = runCatching {
                val r = lm.sendExtraCommand(android.location.LocationManager.GPS_PROVIDER, "delete_aiding_data", android.os.Bundle())
                lm.sendExtraCommand(android.location.LocationManager.GPS_PROVIDER, "force_xtra_injection", null)
                lm.sendExtraCommand(android.location.LocationManager.GPS_PROVIDER, "force_time_injection", null)
                r
            }.getOrDefault(false)
            status = if (ok) "A-GPS data cleared. Go outdoors for a fresh fix (2–3 minutes)."
                     else "This head unit's GPS driver doesn't support resetting. Nothing was cleared."
        })
    })
}

// ── Permissions ──────────────────────────────────────────────────────────────

@Composable
private fun PermissionsSection() {
    val c = Hig.colors
    val context = LocalContext.current
    val media by com.openlauncher.app.service.MediaListenerService.isConnected.collectAsState()
    // Re-checked on resume so statuses refresh after returning from system settings
    var refresh by remember { mutableIntStateOf(0) }
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val overlay = remember(refresh) { android.os.Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context) }
    val location = remember(refresh) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val isHome = remember(refresh) {
        context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    fun open(i: Intent) = runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    @Composable
    fun Status(granted: Boolean, onAllow: () -> Unit) {
        if (granted) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.CheckCircle, null, tint = c.green, modifier = Modifier.size(24.dp))
            Text("Allowed", style = Hig.type.body, color = c.green)
        } else PillButton("Allow", onAllow, primary = true)
    }

    GroupedSection(footer = "Open Launcher works offline; these only unlock the features named.", rows = listOf(
        {
            ListRow("Default Home App", subtitle = "So the head unit starts in Open Launcher.") {
                Status(isHome) {
                    // Home-role dialog first (API 29+); vendor ROMs often lack it, so fall back
                    var launched = false
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                        if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME))
                            launched = runCatching { roleLauncher.launch(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)) }.isSuccess
                    }
                    if (!launched) launched = open(Intent(Settings.ACTION_HOME_SETTINGS))
                    if (!launched) open(Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"))
                }
            }
        },
        { ListRow("Notification Access", subtitle = "For Now Playing.") { Status(media) { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } } },
        {
            ListRow("Location", subtitle = "For the compass, speed and weather.") {
                Status(location) {
                    locLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }
        },
        {
            ListRow("Display Over Other Apps", subtitle = "For the climate pop-up and picture-in-picture.") {
                Status(overlay) {
                    if (android.os.Build.VERSION.SDK_INT >= 23) open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
            }
        },
    ))
}

// ── About ────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(onReset: () -> Unit) {
    val c = Hig.colors
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }
    GroupedSection(footer = "Open Launcher by David Lam. Free and open source.", rows = listOf(
        { ListRow("Version") { Text(version, style = Hig.type.body, color = c.label2) } },
        {
            DisclosureRow("Check for Updates", "GitHub", onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dw2lam/openlauncher/releases")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        },
    ))
    Spacer(Modifier.height(32.dp))
    GroupedSection(footer = "Restores layout, shortcuts and appearance to their defaults.", rows = listOf {
        Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { confirm = true }, contentAlignment = Alignment.Center) {
            Text("Reset All Settings", style = Hig.type.body, color = c.red)
        }
    })
    if (confirm) HigAlert(
        title = "Reset all settings?",
        message = "Layout, shortcuts and appearance go back to their defaults. This can't be undone.",
        confirmLabel = "Reset", destructive = true,
        onConfirm = { confirm = false; onReset() },
        onDismiss = { confirm = false }
    )
}
