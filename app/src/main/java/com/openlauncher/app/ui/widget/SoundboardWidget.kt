package com.openlauncher.app.ui.widget

import com.openlauncher.app.ui.theme.Hig

import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.SoundPadConfig
import com.openlauncher.app.ui.components.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundboardWidget(
    pads: List<SoundPadConfig>,
    accent: Color,
    isDayMode: Boolean = false,
    isEditing: Boolean = false,
    onUpdatePad: (index: Int, pad: SoundPadConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor     = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val borderColor  = if (isDayMode) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val hig = Hig.colors

    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var assigningIndex by remember { mutableStateOf<Int?>(null) }

    val safePads = remember(pads) {
        if (pads.size >= 6) pads.take(6)
        else pads + List(6 - pads.size) { SoundPadConfig("+", synthType = "") }
    }

    // Outer grid Column with top padding = 22.dp to leave room for card header label "SOUNDBOARD"
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) { col ->
                    val idx = row * 3 + col
                    val pad = safePads[idx]
                    val isActive = activePadIndex == idx
                    val hasCustomAudio = pad.audioUri.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                when {
                                    isActive -> hig.toggleOn
                                    pad.label == "+" -> Color.Transparent
                                    else -> hig.fill
                                }
                            )
                            .then(if (pad.label == "+") Modifier.border(2.dp, hig.fill, RoundedCornerShape(18.dp)) else Modifier)
                            .then(
                                if (!isEditing) Modifier.combinedClickable(
                                    onClick = {
                                        if (pad.label == "+" || (pad.audioUri.isEmpty() && pad.synthType.isEmpty())) {
                                            assigningIndex = idx
                                        } else {
                                            activePadIndex = idx
                                            playSoundPad(
                                                context = context,
                                                pad = pad,
                                                onDone = { activePadIndex = null }
                                            )
                                        }
                                    },
                                    onLongClick = { assigningIndex = idx }
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val isPlus = pad.label == "+"
                        Text(
                            text = if (isPlus) "+" else pad.label.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            style = if (isPlus) Hig.type.title1 else Hig.type.headline,
                            color = if (isActive) hig.onToggleOn else if (isPlus) hig.label3 else hig.label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    assigningIndex?.let { idx ->
        PadAssignDialog(
            pad = safePads[idx],
            accent = accent,
            isDayMode = isDayMode,
            onDismiss = { assigningIndex = null },
            onSave = { updated ->
                onUpdatePad(idx, updated)
                assigningIndex = null
            }
        )
    }
}

@Composable
private fun PadAssignDialog(
    pad: SoundPadConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (SoundPadConfig) -> Unit
) {
    val context    = LocalContext.current
    var labelText   by remember { mutableStateOf(pad.label) }
    var synthType   by remember { mutableStateOf(pad.synthType) }
    var audioUri    by remember { mutableStateOf(pad.audioUri) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioUri = uri.toString()
            val rawName = uri.path?.substringAfterLast('/') ?: "custom_sound"
            labelText = rawName.substringAfterLast(':').substringBeforeLast('.')
        }
    }

    val c = Hig.colors
    fun pretty(v: String) = v.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val hasSound = pad.label != "+" || pad.audioUri.isNotEmpty() || pad.synthType.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.sheet)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Pad Sound", style = Hig.type.title2, color = c.label)

            // Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Name", style = Hig.type.footnote, color = c.label2)
                BasicTextField(
                    value = labelText,
                    onValueChange = { if (it.length <= 12) labelText = it },
                    singleLine = true,
                    textStyle = Hig.type.body.copy(color = c.label),
                    cursorBrush = SolidColor(c.tint),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.fill)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            // Built-in sounds
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Built-in", style = Hig.type.footnote, color = c.label2)
                val builtIn = listOf("mario_jump", "mario_coin", "boom", "loud_fart")
                Segmented(
                    builtIn.map { it to pretty(it) },
                    if (audioUri.isEmpty()) synthType else "",
                    { t -> synthType = t; audioUri = ""; labelText = pretty(t) }
                )
            }

            // Custom file
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.fill)
                    .pressable { filePicker.launch(arrayOf("audio/*")) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AudioFile, null, tint = c.tint, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (audioUri.isNotEmpty()) "Custom File" else "Choose Audio File…", style = Hig.type.body, color = c.label)
                    if (audioUri.isNotEmpty()) Text(
                        android.net.Uri.decode(audioUri).substringAfterLast('/').substringAfterLast(':'),
                        style = Hig.type.footnote, color = c.label2, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (audioUri.isNotEmpty()) CircleIconButton(Icons.Default.Clear, "Remove file", { audioUri = "" }, size = 36.dp, iconSize = 20.dp)
            }

            // Actions
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasSound) Text(
                    "Clear Pad", style = Hig.type.headline, color = c.red,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .pressable { onSave(SoundPadConfig(label = "+", audioUri = "", synthType = "")) }
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
                Spacer(Modifier.weight(1f))
                PillButton("Cancel", onDismiss)
                PillButton("Save", {
                    onSave(SoundPadConfig(label = labelText.trim().ifEmpty { pad.label }, audioUri = audioUri, synthType = synthType))
                }, primary = true)
            }
        }
    }
}

private fun playSoundPad(context: android.content.Context, pad: SoundPadConfig, onDone: () -> Unit) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mainHandler.post {
        try {
            if (pad.audioUri.isNotEmpty()) {
                val player = MediaPlayer()
                try {
                    player.setDataSource(context, android.net.Uri.parse(pad.audioUri))
                    player.setOnCompletionListener { mp ->
                        mp.release()
                        onDone()
                    }
                    // Async playback errors (revoked SAF grant, deleted file) must
                    // also release the player and un-highlight the pad
                    player.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        onDone()
                        true
                    }
                    // prepareAsync: a blocking prepare() on the main thread is an
                    // ANR risk for content URIs on slow head-unit storage
                    player.setOnPreparedListener { it.start() }
                    player.prepareAsync()
                } catch (e: Exception) {
                    // Release on synchronous failure — this leaked a native player per failed tap
                    runCatching { player.release() }
                    throw e
                }
            } else {
                var resName = pad.synthType.lowercase().trim()
                var resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                if (resId == 0) {
                    // Fallback mapping for legacy synth types saved in user settings
                    resName = when (resName) {
                        "horn", "beep", "alert" -> "mario_jump"
                        "kick", "snare", "bass" -> "mario_coin"
                        else -> "mario_jump"
                    }
                    resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                }
                if (resId != 0) {
                    val player = MediaPlayer.create(context, resId)
                    if (player != null) {
                        player.setOnCompletionListener { mp ->
                            mp.release()
                            onDone()
                        }
                        player.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            onDone()
                            true
                        }
                        player.start()
                    } else {
                        onDone()
                    }
                } else {
                    onDone()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Soundboard", "Error playing pad", e)
            onDone()
        }
    }
}
