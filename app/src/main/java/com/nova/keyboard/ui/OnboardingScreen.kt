package com.nova.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.keyboard.core.ServiceLocator
import com.nova.keyboard.theme.BuiltInThemes

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val settings = ServiceLocator.settings

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("NOVA", color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text("TYPE BEYOND LIMITS.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        when (step) {
            0 -> {
                InfoCard(
                    "AI powered. Developer ready. Gaming optimised. Privacy first.",
                    "NOVA is a real Android input method: Sinhala and English typing, AI writing tools, " +
                        "a gaming profile, programming layouts, snippets, macros, clipboard history and ten themes. " +
                        "Everything you type stays on this device unless you explicitly send text to an AI provider you configure."
                )
                Spacer(Modifier.height(10.dp))
                InfoCard(
                    "What NOVA never does",
                    "No hidden keystroke collection. No background uploads. No password logging. " +
                        "Predictions, learning and AI are switched off automatically in password fields."
                )
            }
            1 -> {
                InfoCard(
                    "Step 1 \u2014 enable the keyboard",
                    "Android must list NOVA as an input method before it can be used. " +
                        "Open keyboard settings, turn NOVA KEYBOARD on, then come back."
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { KeyboardStatus.openKeyboardSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open keyboard settings") }
                Spacer(Modifier.height(10.dp))
                InfoCard(
                    "Step 2 \u2014 select it",
                    "Now choose NOVA as the active keyboard."
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { KeyboardStatus.showPicker(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose keyboard") }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Status: " + when {
                        KeyboardStatus.isSelected(context) -> "NOVA is the active keyboard"
                        KeyboardStatus.isEnabled(context) -> "enabled, not yet selected"
                        else -> "not enabled yet"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }
            2 -> {
                InfoCard("Language", "Pick what the keyboard should start with. You can switch any time with the globe key.")
                Spacer(Modifier.height(8.dp))
                Row2(
                    left = "English",
                    leftSelected = ServiceLocator.settings.current.language == "en",
                    right = "Sinhala",
                    rightSelected = ServiceLocator.settings.current.language == "si",
                    onLeft = { settings.update { it.copy(language = "en") } },
                    onRight = { settings.update { it.copy(language = "si") } }
                )
                Spacer(Modifier.height(16.dp))
                InfoCard("Theme", "Ten built-in themes ship with the app.")
                Spacer(Modifier.height(8.dp))
                Row2(
                    left = "AMOLED Cyber",
                    leftSelected = ServiceLocator.settings.current.activeThemeId == BuiltInThemes.amoledCyber.id,
                    right = "Neon Fusion",
                    rightSelected = ServiceLocator.settings.current.activeThemeId == BuiltInThemes.neonFusion.id,
                    onLeft = { settings.update { it.copy(activeThemeId = BuiltInThemes.amoledCyber.id) } },
                    onRight = { settings.update { it.copy(activeThemeId = BuiltInThemes.neonFusion.id) } }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (step < 2) step++ else {
                    settings.update { it.copy(onboardingDone = true) }
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (step < 2) "Continue" else "Start using NOVA")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                settings.update { it.copy(onboardingDone = true) }
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Skip") }
    }
}

@Composable
private fun Row2(
    left: String,
    leftSelected: Boolean,
    right: String,
    rightSelected: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onLeft,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .background(
                    if (leftSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
        ) { Text(left) }
        OutlinedButton(
            onClick = onRight,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .background(
                    if (rightSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
        ) { Text(right) }
    }
}
