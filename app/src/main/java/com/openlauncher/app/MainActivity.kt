package com.openlauncher.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.openlauncher.app.climate.ClimateOverlayActivity
import com.openlauncher.app.climate.ClimateUiState
import com.openlauncher.app.climate.TwClimateClient
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.ui.components.Sidebar
import com.openlauncher.app.ui.screen.*
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import com.openlauncher.app.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    // TW head-unit climate (com.tw.ac). Kept bound for the activity's lifetime so
    // physical HVAC presses can bring the climate UI forward even from another app.
    private val climate by lazy { TwClimateClient.get(this) }
    private val hasClimate by lazy {
        try { packageManager.getPackageInfo(TwClimateClient.SERVICE_PKG, 0); true } catch (_: Exception) { false }
    }

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            vm.startLocationUpdates()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        if (hasClimate) {
            climate.start()
            lifecycleScope.launch {
                climate.userChanges.collect { onPhysicalClimateInput() }
            }
        }

        setContent {
            val settingsLoaded by vm.settingsLoaded.collectAsStateWithLifecycle()
            val settings       by vm.settings.collectAsStateWithLifecycle()
            val nav            by vm.nav.collectAsStateWithLifecycle()
            val apps        by vm.apps.collectAsStateWithLifecycle()
            val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()
            val nowPlaying  by vm.nowPlaying.collectAsStateWithLifecycle()
            val weather     by vm.weather.collectAsStateWithLifecycle()
            val isWifi      by vm.isWifi.collectAsStateWithLifecycle()
            val isData      by vm.isData.collectAsStateWithLifecycle()
            val isDayModeVM by vm.isDayMode.collectAsStateWithLifecycle()
            val hardwareRadio by vm.hardwareRadio.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val isDayMode = if (settings.dayNightMode == DayNightMode.SYSTEM) !systemIsDark else isDayModeVM
            SideEffect { ClimateUiState.isDayMode = isDayMode }
            val pickerSlot      by vm.shortcutPickerSlot.collectAsStateWithLifecycle()
            val appPickerTarget by vm.appPickerTarget.collectAsStateWithLifecycle()

            val hasWallpaper = settings.wallpaperUri.isNotEmpty()

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density   = baseDensity.density * settings.uiScale,
                    fontScale = baseDensity.fontScale
                )
            ) {
                if (!settingsLoaded) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                } else OpenLauncherTheme(
                    accentArgb   = settings.accentColor,
                    isDayMode    = isDayMode,
                    textScale    = settings.textScale,
                    hasWallpaper = hasWallpaper
                ) {
                if (!settings.onboardingCompleted) {
                    OnboardingScreen(
                        accent = com.openlauncher.app.ui.theme.Hig.colors.tint,
                        onComplete = {
                            vm.updateSettings { copy(onboardingCompleted = true) }
                            // Start location updates immediately upon completion
                            vm.startLocationUpdates()
                        }
                    )
                } else {
                    val hig = com.openlauncher.app.ui.theme.Hig.colors
                    Box(modifier = Modifier.fillMaxSize().background(hig.bg)) {
                        // Optional wallpaper, always dimmed; night adds extra dimming so the
                        // cabin stays dark. Tiles turn translucent over it (see higColors).
                        if (hasWallpaper) {
                            AsyncImage(
                                model              = android.net.Uri.parse(settings.wallpaperUri),
                                contentDescription = null,
                                contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            val dim = if (isDayMode) settings.wallpaperDim else (settings.wallpaperDim + 0.15f).coerceAtMost(0.92f)
                            Box(modifier = Modifier.fillMaxSize()
                                .background((if (isDayMode) Color(0xFFF2F2F7) else Color.Black).copy(alpha = dim)))
                        }

                        val isBottomBar    = settings.sidebarPosition == SidebarPosition.BOTTOM

                        val sidebarContent: @Composable () -> Unit = {
                            val sidebarDensity = Density(
                                density = baseDensity.density * (1.0f + (settings.uiScale - 1.0f) * 0.35f),
                                fontScale = baseDensity.fontScale
                            )
                            CompositionLocalProvider(LocalDensity provides sidebarDensity) {
                                Sidebar(
                                    currentDest   = nav,
                                    settings      = settings,
                                    isHorizontal  = isBottomBar,
                                    showClimate   = hasClimate,
                                    isWifi        = isWifi,
                                    isData        = isData,
                                    alignRight    = settings.sidebarPosition == SidebarPosition.RIGHT,
                                    installedIconFor = { pkg ->
                                        apps.find { it.packageName == pkg }?.icon
                                    },
                                    onNavigate    = { dest ->
                                        vm.cancelShortcutPicker()
                                        vm.cancelCarPlayPicker()
                                        vm.exitRearrangeMode()
                                        vm.navigate(dest)
                                    },
                                    onShortcutClick = { slot ->
                                        val shortcut = settings.shortcuts[slot]
                                        if (shortcut.packageName.isNotEmpty()) {
                                            vm.launchApp(shortcut.packageName)
                                        }
                                    },
                                    onShortcutLongPress  = { slot -> vm.startShortcutPicker(slot) },
                                    onShortcutRemove     = { slot -> vm.removeShortcut(slot) },
                                    onShortcutSetIcon    = { slot, icon -> vm.setShortcutIcon(slot, icon) },
                                    onReorder            = { from, to -> vm.reorderShortcut(from, to) }
                                )
                            }
                        }

                        val mainPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            // ── Main content pane ─────────────────────────────
                            AnimatedContent(
                                targetState   = nav,
                                transitionSpec = {
                                    // Quick crossfade with a slight settle, as iOS swaps tabs;
                                    // anything longer reads as lag on head-unit SoCs.
                                    (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.985f)) togetherWith
                                    fadeOut(tween(110))
                                },
                                modifier = paneModifier,
                                label    = "pane_transition"
                            ) { destination ->
                                when (destination) {
                                    NavDestination.HOME -> {
                                    // Collected here, not at the root: the compass/GPS tick
                                    // several times a second and would recompose the whole app.
                                    val location by vm.location.collectAsStateWithLifecycle()
                                    val bearing  by vm.compassBearing.collectAsStateWithLifecycle()
                                    val editMode by vm.rearrangeMode.collectAsStateWithLifecycle()
                                    HomeScreen(
                                        settings            = settings,
                                        editMode            = editMode,
                                        onEditModeChange    = { if (it) vm.enterRearrangeMode() else vm.exitRearrangeMode() },
                                        weather             = weather,
                                        nowPlaying          = nowPlaying,
                                        location            = location,
                                        bearing             = bearing,
                                        isWifi              = isWifi,
                                        isData              = isData,
                                        isDayMode           = isDayMode,
                                        onPlayPause         = vm::playPause,
                                        onNext              = vm::skipNext,
                                        onPrev              = vm::skipPrev,
                                        onLaunchCarPlay     = { vm.launchApp(settings.carPlayPackage) },
                                        onLaunchAndroidAuto = { vm.launchApp(settings.androidAutoPackage) },
                                        onAssignCarPlay     = { vm.startCarPlayPicker() },
                                        onAssignAndroidAuto = { vm.startAndroidAutoPicker() },
                                        onClearCarPlay      = { vm.clearCarPlayApp() },
                                        onClearAndroidAuto  = { vm.clearAndroidAutoApp() },
                                        onAssignPip         = { vm.startPipPicker() },
                                        onClearPip          = { vm.clearPipApp() },
                                        onLaunchPip         = { vm.launchApp(settings.pipAppPackage) },
                                        onTapNowPlaying     = {
                                            val pkg = nowPlaying?.controller?.packageName
                                            if (!pkg.isNullOrEmpty()) vm.launchApp(pkg)
                                        },
                                        onUpdateWidget      = { id, sx, sy -> vm.updateWidgetConfig(id, sx, sy) },
                                        onMoveWidget        = { id, gx, gy -> vm.moveWidgetConfig(id, gx, gy) },
                                        onAddWidget         = { id -> vm.addWidget(id) },
                                        onRemoveWidget      = { id -> vm.removeWidget(id) },
                                        onSetClockStyle     = { style -> vm.updateSettings { copy(clockStyle = style) } },
                                        onSetVitalsAsBars   = { asBars -> vm.updateSettings { copy(vitalsAsBars = asBars) } },
                                        onSetSpeedometerDigitalOnly = { digital -> vm.updateSettings { copy(speedometerDigitalOnly = digital) } },
                                        onUpdateSoundPad    = { idx, pad -> vm.updateSoundboardPad(idx, pad) },
                                        hardwareRadio         = hardwareRadio,
                                        onLaunchHardwareRadio = { vm.launchHardwareRadioApp() },
                                        onStopHardwareRadio   = { vm.stopHardwareRadioApp() },
                                        onRadioSeekUp         = { vm.radioSeekUp() },
                                        onRadioSeekDown       = { vm.radioSeekDown() },
                                        onRadioCycleFm        = { vm.radioCycleFm() },
                                        onRadioSwitchAm       = { vm.radioSwitchAm() },
                                        onRadioTune           = { band, freq -> vm.radioTune(band, freq) },
                                        onAssignRadio         = { vm.startRadioPicker() }
                                    )
                                    }

                                    NavDestination.APP_LIBRARY -> AppLibraryScreen(
                                        apps                = apps,
                                        isLoading           = appsLoading,
                                        isPickerMode        = pickerSlot != null,
                                        pickerSlot          = pickerSlot,
                                        isCarPlayPickerMode = appPickerTarget != null,
                                        carPlayPickerLabel  = when (appPickerTarget) {
                                            com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.ANDROID_AUTO -> "Choose Android Auto App"
                                            com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.PIP          -> "Choose Picture-in-Picture App"
                                            com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.RADIO        -> "Choose Radio App"
                                            else -> "Choose CarPlay App"
                                        },
                                        onAppClick          = { app -> vm.launchApp(app.packageName) },
                                        onPickerSelect      = { slot, app -> vm.assignShortcut(slot, app) },
                                        onCarPlaySelect     = { app -> vm.assignPickerApp(app) },
                                        onCancelPicker      = {
                                            vm.cancelShortcutPicker()
                                            vm.cancelCarPlayPicker()
                                            vm.navigate(vm.pickerReturn)
                                        }
                                    )

                                    NavDestination.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        hasClimate = hasClimate,
                                        onAssignCarPlay     = { vm.startCarPlayPicker() },
                                        onAssignAndroidAuto = { vm.startAndroidAutoPicker() },
                                        onAssignRadio       = { vm.startRadioPicker() },
                                        onAssignShortcut    = { slot -> vm.startShortcutPicker(slot) },
                                        onEditHome          = { vm.navigate(NavDestination.HOME); vm.enterRearrangeMode() },
                                        onUpdate = { block -> vm.updateSettings(block) },
                                        onReset  = { vm.resetSettings() }
                                    )

                                    NavDestination.CLIMATE -> ClimateScreen()
                                }
                            }
                        }

                        if (isBottomBar) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                mainPane(Modifier.weight(1f).fillMaxWidth())
                                sidebarContent()
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (settings.sidebarPosition == SidebarPosition.LEFT) sidebarContent()
                                mainPane(Modifier.weight(1f).fillMaxHeight())
                                if (settings.sidebarPosition == SidebarPosition.RIGHT) sidebarContent()
                            }
                        }
                    }
                }
            }
            } // CompositionLocalProvider
        }
    }

    /** A physical HVAC control was pressed: show climate without stealing focus otherwise. */
    private fun onPhysicalClimateInput() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            vm.navigate(NavDestination.CLIMATE)
        } else {
            try { ClimateOverlayActivity.show(this) } catch (e: Exception) {
                android.util.Log.w("OpenLauncher", "climate overlay blocked", e)
            }
        }
    }

    override fun onDestroy() {
        if (hasClimate) climate.stop()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshConnectivity()
        vm.refreshMedia()
    }

    override fun onStop() {
        super.onStop()
        vm.stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        vm.startLocationUpdates()
    }
}
