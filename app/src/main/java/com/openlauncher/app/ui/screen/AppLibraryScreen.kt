package com.openlauncher.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.ui.components.CircleIconButton
import com.openlauncher.app.ui.components.PillButton
import com.openlauncher.app.ui.components.Segmented
import com.openlauncher.app.ui.components.pressable
import com.openlauncher.app.ui.theme.Hig

private enum class AppFilter { USER, SYSTEM, ALL }

@Composable
fun AppLibraryScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    isPickerMode: Boolean,
    pickerSlot: Int?,
    isCarPlayPickerMode: Boolean,
    carPlayPickerLabel: String = "Choose CarPlay App",
    onAppClick: (AppInfo) -> Unit,
    onPickerSelect: (Int, AppInfo) -> Unit,
    onCarPlaySelect: (AppInfo) -> Unit,
    onCancelPicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = Hig.colors
    val picking = isPickerMode || isCarPlayPickerMode
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.USER) }

    val shown = remember(apps, query, filter, picking) {
        val byName = if (query.isBlank()) apps else apps.filter { it.appName.contains(query, ignoreCase = true) }
        // Pickers always list everything: head units bury CarPlay/radio receivers as system apps
        if (picking) byName else when (filter) {
            AppFilter.USER -> byName.filter { !it.isSystemApp }
            AppFilter.SYSTEM -> byName.filter { it.isSystemApp }
            AppFilter.ALL -> byName
        }
    }

    Column(modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 22.dp)) {
        if (picking) Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(18.dp))
                .background(c.tint.copy(alpha = 0.16f).compositeOver(c.tile))
                .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isCarPlayPickerMode) carPlayPickerLabel else "Choose an App for the Sidebar",
                style = Hig.type.headline, color = c.label, modifier = Modifier.weight(1f)
            )
            PillButton("Cancel", onCancelPicker)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Apps", style = Hig.type.largeTitle, color = c.label, modifier = Modifier.weight(1f))
            if (!picking) Segmented(
                options = listOf(AppFilter.USER to "Installed", AppFilter.SYSTEM to "System", AppFilter.ALL to "All"),
                selected = filter, onSelect = { filter = it }
            )
            SearchField(query, { query = it }, Modifier.widthIn(max = 380.dp).fillMaxWidth(0.4f))
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.label2, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
            shown.isEmpty() -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.SearchOff, null, tint = c.label3, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text(if (query.isBlank()) "No apps here" else "No apps match “$query”", style = Hig.type.headline, color = c.label)
                if (!picking && filter != AppFilter.ALL) Text("Try All.", style = Hig.type.subhead, color = c.label2)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(124.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shown, key = { it.packageName }) { app ->
                    AppCell(app) {
                        when {
                            isCarPlayPickerMode -> onCarPlaySelect(app)
                            isPickerMode && pickerSlot != null -> onPickerSelect(pickerSlot, app)
                            else -> onAppClick(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val c = Hig.colors
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (c.isDark) c.tile else c.fill).padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = c.label2, modifier = Modifier.size(24.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = Hig.type.body.copy(color = c.label),
            cursorBrush = SolidColor(c.tint),
            modifier = Modifier.weight(1f),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text("Search", style = Hig.type.body, color = c.label2); inner() } }
        )
        if (value.isNotEmpty()) CircleIconButton(Icons.Rounded.Close, "Clear search", { onChange("") }, size = 40.dp, iconSize = 20.dp, tint = c.label2)
    }
}

@Composable
private fun AppCell(app: AppInfo, onClick: () -> Unit) {
    val c = Hig.colors
    val px = with(LocalDensity.current) { 76.dp.roundToPx() }
    val bmp = remember(app.packageName, px) { runCatching { app.icon.toBitmap(px, px) }.getOrNull() }
    Column(
        Modifier.pressable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(76.dp))
        else Box(Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)).background(c.fill), contentAlignment = Alignment.Center) {
            Text(app.appName.take(1).uppercase(), style = Hig.type.title1, color = c.label2)
        }
        Text(
            app.appName, style = Hig.type.subhead, color = c.label,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}
