package com.nova.keyboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Screen(val title: String) {
    ONBOARDING("Welcome"),
    DASHBOARD("NOVA KEYBOARD"),
    THEMES("Themes"),
    THEME_EDITOR("Theme editor"),
    LANGUAGES("Languages"),
    AI("AI Studio"),
    GAMING("Gaming mode"),
    DEVELOPER("Developer tools"),
    CLIPBOARD("Clipboard"),
    SETTINGS("Settings"),
    PRIVACY("Privacy centre"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaApp(startOnboarding: Boolean) {
    var screen by remember { mutableStateOf(if (startOnboarding) Screen.ONBOARDING else Screen.DASHBOARD) }

    BackHandler(enabled = screen != Screen.DASHBOARD && screen != Screen.ONBOARDING) {
        screen = Screen.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screen.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    if (screen != Screen.DASHBOARD && screen != Screen.ONBOARDING) {
                        TextButton(onClick = { screen = Screen.DASHBOARD }) { Text("Back") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF05060A),
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color(0xFF05060A)
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                Screen.ONBOARDING -> OnboardingScreen(onDone = { screen = Screen.DASHBOARD })
                Screen.DASHBOARD -> DashboardScreen(onNavigate = { screen = it })
                Screen.THEMES -> ThemesScreen(onEdit = { screen = Screen.THEME_EDITOR })
                Screen.THEME_EDITOR -> ThemeEditorScreen(onBack = { screen = Screen.THEMES })
                Screen.LANGUAGES -> LanguagesScreen()
                Screen.AI -> AiStudioScreen()
                Screen.GAMING -> GamingScreen()
                Screen.DEVELOPER -> DeveloperScreen()
                Screen.CLIPBOARD -> ClipboardScreen()
                Screen.SETTINGS -> SettingsScreen()
                Screen.PRIVACY -> PrivacyScreen()
                Screen.ABOUT -> AboutScreen()
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: (Float) -> String = { it.toString() },
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(valueLabel(value), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
fun InfoCard(title: String, body: String, accent: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(
                color = if (accent) Color(0xFF101822) else Color(0xFF0C0F14),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}
