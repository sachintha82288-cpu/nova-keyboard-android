package com.nova.keyboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.keyboard.core.ServiceLocator
import com.nova.keyboard.theme.NovaTheme

@Composable
fun ThemesScreen(onEdit: () -> Unit) {
    val context = LocalContext.current
    val settings by ServiceLocator.settings.state.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val themes = remember(refresh, settings.customThemesJson) { ServiceLocator.themes.allThemes() }
    val activeId = settings.activeThemeId

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            OutlinedButton(
                onClick = { onEdit() },
                modifier = Modifier.weight(1f).padding(2.dp)
            ) { Text("Edit active", fontSize = 12.sp) }
            OutlinedButton(
                onClick = {
                    val active = ServiceLocator.themes.active()
                    ServiceLocator.themes.duplicate(active, active.name + " copy")
                    refresh++
                },
                modifier = Modifier.weight(1f).padding(2.dp)
            ) { Text("Duplicate", fontSize = 12.sp) }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            OutlinedButton(
                onClick = {
                    val json = ServiceLocator.themes.exportTheme(ServiceLocator.themes.active())
                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("NOVA theme", json))
                },
                modifier = Modifier.weight(1f).padding(2.dp)
            ) { Text("Export", fontSize = 12.sp) }
            OutlinedButton(
                onClick = {
                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = manager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    ServiceLocator.themes.importTheme(text)
                    refresh++
                },
                modifier = Modifier.weight(1f).padding(2.dp)
            ) { Text("Import", fontSize = 12.sp) }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(themes) { theme ->
                ThemeCard(
                    theme = theme,
                    active = theme.id == activeId,
                    onApply = {
                        ServiceLocator.themes.apply(theme.id)
                        refresh++
                    },
                    onDelete = if (!theme.builtIn) {
                        { ServiceLocator.themes.delete(theme.id); refresh++ }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: NovaTheme, active: Boolean, onApply: () -> Unit, onDelete: (() -> Unit)?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color(theme.background), RoundedCornerShape(16.dp))
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) Color(theme.accent) else Color(theme.keyBorder),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(theme.name, color = Color(theme.textColor), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (active) Text("ACTIVE", color = Color(theme.accent), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            listOf(theme.keyBackground, theme.keyPressed, theme.specialKey, theme.accent).forEach { color ->
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .height(28.dp)
                        .weight(1f)
                        .background(Color(color), RoundedCornerShape(8.dp))
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            if (!active) {
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Apply", fontSize = 12.sp) }
            } else {
                Text("Applied", color = Color(theme.accent), fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun ThemeEditorScreen(onBack: () -> Unit) {
    val settings by ServiceLocator.settings.state.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val theme = remember(refresh, settings.activeThemeId, settings.customThemesJson) {
        ServiceLocator.themes.active()
    }
    var radius by remember(theme.id) { mutableIntStateOf(theme.radiusDp) }
    var border by remember(theme.id) { mutableIntStateOf(theme.borderWidthDp) }
    var alpha by remember(theme.id) { androidx.compose.runtime.mutableFloatStateOf(theme.keyAlpha) }
    var keyColor by remember(theme.id) { mutableIntStateOf(theme.keyBackground) }
    var textColor by remember(theme.id) { mutableIntStateOf(theme.textColor) }
    var accentColor by remember(theme.id) { mutableIntStateOf(theme.accent) }
    var background by remember(theme.id) { mutableIntStateOf(theme.background) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SettingSlider("Key radius", radius.toFloat(), 0f..32f, 31, { it.toInt().toString() + " dp" }) { radius = it.toInt() }
            SettingSlider("Key border", border.toFloat(), 0f..4f, 3, { it.toInt().toString() + " dp" }) { border = it.toInt() }
            SettingSlider("Key transparency", alpha, 0.2f..1f, 7, { ((1f - it) * 100).toInt().toString() + "%" }) { alpha = it }
        }
        item { SectionHeader("Key colour") }
        item { SwatchPicker(keyColor) { keyColor = it } }
        item { SectionHeader("Text colour") }
        item { SwatchPicker(textColor) { textColor = it } }
        item { SectionHeader("Accent") }
        item { SwatchPicker(accentColor) { accentColor = it } }
        item { SectionHeader("Background") }
        item { SwatchPicker(background) { background = it } }
        item {
            Row(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        ServiceLocator.themes.updateActive {
                            it.copy(
                                radiusDp = radius,
                                borderWidthDp = border,
                                keyAlpha = alpha,
                                keyBackground = keyColor,
                                textColor = textColor,
                                accent = accentColor,
                                background = background
                            )
                        }
                        refresh++
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save theme") }
                Spacer(Modifier.weight(0.2f))
                OutlinedButton(onClick = onBack) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun SwatchPicker(selected: Int, onPick: (Int) -> Unit) {
    val palette = listOf(
        0xFF000000, 0xFF05060A, 0xFF0C0F14, 0xFF151A22, 0xFF1B2430, 0xFF241640, 0xFF2A0F52,
        0xFF04120A, 0xFF120607, 0xFF0E1A26, 0xFFFFFFFF, 0xFFF4F4F4, 0xFFEAF2FA, 0xFFD6E7F8,
        0xFF3DE1FF, 0xFFB14BFF, 0xFFFF3DCB, 0xFFFF4D5E, 0xFF35FF7A, 0xFF7FC8FF, 0xFFFFB020,
        0xFF9CFFB8, 0xFFE9FBFF, 0xFF9AA0A6
    ).map { it.toInt() }
    Column(Modifier.padding(horizontal = 16.dp)) {
        palette.chunked(8).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                row.forEach { color ->
                    Box(
                        Modifier
                            .padding(2.dp)
                            .height(30.dp)
                            .weight(1f)
                            .background(Color(color), RoundedCornerShape(6.dp))
                            .border(
                                width = if (color == selected) 2.dp else 1.dp,
                                color = if (color == selected) MaterialTheme.colorScheme.primary else Color(0xFF2A323C),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onPick(color) }
                    )
                }
            }
        }
    }
}
