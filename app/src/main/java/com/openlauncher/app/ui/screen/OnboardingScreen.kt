package com.openlauncher.app.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.openlauncher.app.ui.components.PillButton
import com.openlauncher.app.ui.components.SquircleGlyph
import com.openlauncher.app.ui.components.pressable
import com.openlauncher.app.ui.theme.Hig

/**
 * First run, in the iOS Setup Assistant pattern: one idea per page, one glyph, a title,
 * two lines of copy and a single primary action, with a quiet way to skip.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    accent: Color,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by rememberSaveable { mutableStateOf(0) }
    var locationGranted by remember { mutableStateOf(false) }
    var mediaGranted by remember { mutableStateOf(false) }

    val checkPermissions = {
        locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        mediaGranted = enabledListeners != null && enabledListeners.contains(context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Trigger initial check
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationGranted = granted
        if (granted) {
            currentStep = 2 // Auto-advance to next step
        }
    }

    val c = Hig.colors
    val last = 3

    data class Page(val glyph: ImageVector, val glyphColor: Color, val title: String, val body: String)
    val page = when (currentStep) {
        0 -> Page(Icons.Rounded.Widgets, c.tint, "Welcome to Open Launcher",
                  "Widgets for the road, your apps a tap away, and climate control. It all works offline.")
        1 -> Page(Icons.Rounded.NearMe, Color(0xFF0A84FF), "Location",
                  "Speed, compass heading, altitude and local weather all come from GPS.")
        2 -> Page(Icons.Rounded.MusicNote, c.red, "Now Playing",
                  "Notification access lets the home screen show what's playing and control it.")
        else -> Page(Icons.Rounded.Home, c.green, "Make It Your Home Screen",
                  "Set Open Launcher as the default home app so it's there whenever the car starts.")
    }
    val granted = when (currentStep) { 1 -> locationGranted; 2 -> mediaGranted; else -> false }

    Box(modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxHeight().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "onboarding_page",
                modifier = Modifier.weight(1f)
            ) { _ ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
                ) {
                    SquircleGlyph(page.glyph, page.glyphColor, size = 72.dp, iconSize = 44.dp)
                    Text(page.title, style = Hig.type.title1, color = c.label, textAlign = TextAlign.Center)
                    Text(page.body, style = Hig.type.body, color = c.label2, textAlign = TextAlign.Center)
                    if (granted) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = c.green, modifier = Modifier.size(22.dp))
                        Text("Allowed", style = Hig.type.callout, color = c.label2)
                    }
                }
            }

            // Primary action, then the quiet secondary one
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val next: () -> Unit = { if (currentStep < last) currentStep++ else onComplete() }
                when {
                    currentStep == 1 && !locationGranted -> PillButton("Allow Location", {
                        locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }, Modifier.widthIn(min = 280.dp), primary = true)
                    currentStep == 2 && !mediaGranted -> PillButton("Open Notification Access", {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    }, Modifier.widthIn(min = 280.dp), primary = true)
                    currentStep == last -> PillButton("Choose Home App", {
                        runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    }, Modifier.widthIn(min = 280.dp), primary = true)
                    else -> PillButton(if (currentStep == 0) "Get Started" else "Continue", next, Modifier.widthIn(min = 280.dp), primary = true)
                }
                val secondary = when {
                    currentStep == last -> "Done"
                    (currentStep == 1 && !locationGranted) || (currentStep == 2 && !mediaGranted) -> "Not Now"
                    else -> null
                }
                Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    if (secondary != null) Text(
                        secondary, style = Hig.type.headline, color = c.tint,
                        modifier = Modifier.clip(CircleShape).pressable(onClick = next).padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..last) Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == currentStep) c.label else c.label3))
                }
            }
        }
    }
}
