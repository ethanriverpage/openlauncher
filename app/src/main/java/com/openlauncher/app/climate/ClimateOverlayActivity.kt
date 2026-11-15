package com.openlauncher.app.climate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.SettingsRepository
import com.openlauncher.app.ui.screen.ClimatePanel
import com.openlauncher.app.ui.screen.climateColors
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import kotlinx.coroutines.delay

/**
 * Shown over whatever app is in front (CarPlay, maps…) when a physical climate control is
 * pressed. Lives in its own task so finishing it drops straight back to that app.
 * Auto-closes after [IDLE_MS] with no climate change or touch; a tap outside closes it.
 */
class ClimateOverlayActivity : ComponentActivity() {

    /** Bumped on every reopen / interaction / climate change to restart the idle timer. */
    private var activity by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SettingsRepository(applicationContext)
        val client = TwClimateClient.get(this)

        setContent {
            val settings by repo.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val accent = Color(settings.accentColor)
            val isDayMode = ClimateUiState.isDayMode

            // Any state change from the car keeps the overlay up.
            val state by client.state.collectAsStateWithLifecycle()
            LaunchedEffect(state) { activity++ }
            LaunchedEffect(activity) {
                delay(IDLE_MS)
                finish()
            }

            OpenLauncherTheme(
                accent = accent,
                textColor = if (isDayMode) Color(0xFF111111) else Color(settings.fontColor),
                fontBold = settings.fontBold,
                textScale = settings.textScale,
                appFont = settings.appFont,
                isDayMode = isDayMode,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    val colors = climateColors(accent, isDayMode, hasWallpaper = false)
                    Box(
                        Modifier
                            .fillMaxWidth(0.88f)
                            .fillMaxHeight(0.82f)
                            .background(if (isDayMode) Color(0xFFEEEEEE) else Color.Black)
                            // swallow taps so they don't reach the dismiss scrim
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { activity++ }
                            .padding(6.dp)
                    ) {
                        ClimatePanel(colors, Modifier.fillMaxSize(), onInteract = { activity++ })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        activity++
    }

    override fun onStop() {
        super.onStop()
        // Leaving (home pressed, another app took over) ends the overlay.
        if (!isFinishing) finish()
    }

    companion object {
        private const val IDLE_MS = 8_000L

        fun show(context: Context) {
            context.startActivity(
                Intent(context, ClimateOverlayActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            )
        }
    }
}

/** Small bridge so the overlay matches the launcher's current day/night look. */
object ClimateUiState {
    @Volatile var isDayMode: Boolean = false
}
