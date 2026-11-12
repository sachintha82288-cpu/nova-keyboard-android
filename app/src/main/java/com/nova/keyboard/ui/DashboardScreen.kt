package com.nova.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

@Composable
fun DashboardScreen(onNavigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val settings by ServiceLocator.settings.state.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val enabled = remember(refresh) { KeyboardStatus.isEnabled(context) }
    val selected = remember(refresh) { KeyboardStatus.isSelected(context) }
    val theme = ServiceLocator.themes.active()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "TYPE BEYOND LIMITS.",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "AI POWERED. DEVELOPER READY. GAMING OPTIMISED. PRIVACY FIRST.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        InfoCard(
            title = when {
                selected -> "Keyboard active"
                enabled -> "Enabled \u2014 not selected"
                else -> "Keyboard not enabled"
            },
            body = when {
                selected -> "NOVA is the current input method. Tap any text field to type."
                enabled -> "Android has NOVA enabled. Choose it as the active keyboard."
                else -> "Android does not list NOVA as an input method yet. Enable it to start typing."
            },
            accent = true
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            OutlinedButton(
                onClick = { KeyboardStatus.openKeyboardSettings(context); refresh++ },
                modifier = Modifier.weight(1f).padding(4.dp)
            ) { Text("Settings", fontSize = 13.sp) }
            OutlinedButton(
                onClick = { KeyboardStatus.showPicker(context) },
                modifier = Modifier.weight(1f).padding(4.dp)
            ) { Text("Select", fontSize = 13.sp) }
            OutlinedButton(
                onClick = { refresh++ },
                modifier = Modifier.weight(1f).padding(4.dp)
            ) { Text("Refresh", fontSize = 13.sp) }
        }

        SectionHeader("Status")
        InfoCard("Theme", theme.name)
        InfoCard(
            "Language",
            if (settings.language == "si") {
                "Sinhala \u2014 " + if (settings.sinhalaInputMethod == "wijesekara") "Wijesekara layout" else "Phonetic (Singlish)"
            } else "English"
        )
        InfoCard(
            "AI",
            if (settings.aiEnabled && ServiceLocator.ai.hasKey()) "Configured \u2014 " + settings.aiModel
            else "Off or missing API key. Text is never sent without your tap."
        )
        InfoCard("Gaming mode", if (settings.gamingMode) "On" else "Off")
        InfoCard("Developer mode", if (settings.devMode) "On \u2014 " + settings.devLanguage else "Off")

        SectionHeader("Modules")
        Row(Modifier.padding(horizontal = 12.dp)) {
            NavCard("Themes", Modifier.weight(1f)) { onNavigate(Screen.THEMES) }
            NavCard("Languages", Modifier.weight(1f)) { onNavigate(Screen.LANGUAGES) }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            NavCard("AI Studio", Modifier.weight(1f)) { onNavigate(Screen.AI) }
            NavCard("Gaming", Modifier.weight(1f)) { onNavigate(Screen.GAMING) }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            NavCard("Developer", Modifier.weight(1f)) { onNavigate(Screen.DEVELOPER) }
            NavCard("Clipboard", Modifier.weight(1f)) { onNavigate(Screen.CLIPBOARD) }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            NavCard("Settings", Modifier.weight(1f)) { onNavigate(Screen.SETTINGS) }
            NavCard("Privacy", Modifier.weight(1f)) { onNavigate(Screen.PRIVACY) }
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            NavCard("About", Modifier.weight(1f)) { onNavigate(Screen.ABOUT) }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavCard(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .background(Color(0xFF0C0F14), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("Open", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
    }
}
