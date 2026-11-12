package com.nova.keyboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.keyboard.ai.AiResult
import com.nova.keyboard.core.ServiceLocator
import com.nova.keyboard.core.Settings
import com.nova.keyboard.ui.keyboard.KeyboardLayouts
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun LanguagesScreen() {
    val settings by ServiceLocator.settings.state.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val repo = ServiceLocator.settings

    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionHeader("Keyboard language") }
        item {
            SettingSwitch("English", "QWERTY with suggestions and autocorrection", settings.language == "en") {
                if (it) repo.update { s -> s.copy(language = "en") }
            }
        }
        item {
            SettingSwitch("Sinhala", "Wijesekara Unicode layout or phonetic Singlish", settings.language == "si") {
                if (it) repo.update { s -> s.copy(language = "si") }
            }
        }
        item { SectionHeader("Sinhala input method") }
        item {
            ChoiceRow(
                options = listOf("wijesekara" to "Wijesekara", "phonetic" to "Phonetic (Singlish)"),
                selected = settings.sinhalaInputMethod
            ) { value -> repo.update { it.copy(sinhalaInputMethod = value) } }
        }
        item {
            InfoCard(
                "Wijesekara layout",
                "Standard SLS 1134 based Wij 9 mapping: q=\u0DD4 w=\u0D85 e=\u0DD0 r=\u0DBB, a=\u0DCA s=\u0DD2 d=\u0DCF " +
                    "f=\u0DD9, and the shift layer for \u0DC1 \u0DC2 \u0DB0 \u0DA8 and the rest. Characters that a desktop " +
                    "keyboard reaches with AltGr (\u0DB3 \u0D9F \u0DA6 \u0DAC \u0DB9 \u0D86 \u0D87 ...) are long-press alternates."
            )
        }
        item {
            InfoCard(
                "Phonetic typing",
                "Type romanised Sinhala and it converts as you go: amma \u2192 \u0D85\u0DB8\u0DCA\u0DB8, " +
                    "api \u2192 \u0D85\u0DB4\u0DD2, kohomada \u2192 \u0D9A\u0DDC\u0DC4\u0DDC\u0DB8\u0DAF, " +
                    "suba \u2192 \u0DC3\u0DD4\u0DB6. Double a vowel for the long form (ammaa \u2192 \u0D85\u0DB8\u0DCA\u0DB8\u0DCF). " +
                    "Candidates appear above the keys; tap one to commit it."
            )
        }
        item { SectionHeader("Dictionary and learning") }
        item {
            SettingSwitch(
                "Learn my typing",
                "Stores word and word-pair counts in a local database. Off = nothing is learned.",
                settings.learningEnabled
            ) { repo.update { s -> s.copy(learningEnabled = it) } }
        }
        item {
            SettingSwitch("Next-word prediction", "Uses your own word pairs, on this device", settings.nextWordPrediction) {
                repo.update { s -> s.copy(nextWordPrediction = it) }
            }
        }
        item {
            val words = rememberDbValue(refresh, emptyList<String>()) {
                ServiceLocator.learning.topWords("en", 40)
            }
            InfoCard(
                "Your English words",
                if (words.isEmpty()) "Nothing learned yet." else words.joinToString(", ")
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    ServiceLocator.learning.eraseAll()
                    ServiceLocator.suggestions.invalidateCache()
                    refresh++
                },
                modifier = Modifier.padding(16.dp)
            ) { Text("Erase learned words") }
        }
    }
}

@Composable
fun AiStudioScreen() {
    val context = LocalContext.current
    val settings by ServiceLocator.settings.state.collectAsState()
    val repo = ServiceLocator.settings
    val scope = rememberCoroutineScope()
    var baseUrl by remember(settings.aiBaseUrl) { mutableStateOf(settings.aiBaseUrl) }
    var model by remember(settings.aiModel) { mutableStateOf(settings.aiModel) }
    var keyInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SettingSwitch(
                "Enable AI features",
                "Off by default. Text is sent only when you tap an AI action.",
                settings.aiEnabled
            ) { repo.update { s -> s.copy(aiEnabled = it) } }
        }
        item { SectionHeader("Provider") }
        item {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL (https only)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(if (ServiceLocator.ai.hasKey()) "API key saved \u2014 replace" else "API key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )
        }
        item {
            Row(Modifier.padding(horizontal = 12.dp)) {
                Button(
                    onClick = {
                        repo.update { s -> s.copy(aiBaseUrl = baseUrl.trim(), aiModel = model.trim()) }
                        if (keyInput.isNotBlank()) {
                            ServiceLocator.ai.setKey(keyInput.trim())
                            keyInput = ""
                            status = "Key stored in the Android Keystore."
                        } else {
                            status = "Settings saved."
                        }
                    },
                    modifier = Modifier.weight(1f).padding(4.dp)
                ) { Text("Save", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = {
                        status = "Testing\u2026"
                        scope.launch {
                            val result = ServiceLocator.ai.complete(
                                "You are a connection test. Reply with the single word OK.",
                                "Connection test"
                            )
                            status = when (result) {
                                is AiResult.Success -> "Connected. Provider replied: " + result.text.take(60)
                                is AiResult.Failure -> result.message
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(4.dp)
                ) { Text("Test", fontSize = 13.sp) }
            }
        }
        item {
            if (status.isNotEmpty()) {
                InfoCard("Status", status, accent = true)
            }
        }
        item { SectionHeader("Features") }
        item {
            SettingSwitch("Rewrite and tone", null, settings.aiRewrite) { repo.update { s -> s.copy(aiRewrite = it) } }
        }
        item {
            SettingSwitch("Grammar and spelling", null, settings.aiGrammar) { repo.update { s -> s.copy(aiGrammar = it) } }
        }
        item {
            SettingSwitch("Translation (Sinhala \u2194 English)", null, settings.aiTranslate) {
                repo.update { s -> s.copy(aiTranslate = it) }
            }
        }
        item {
            SettingSwitch("Text generation", null, settings.aiGenerate) { repo.update { s -> s.copy(aiGenerate = it) } }
        }
        item { SectionHeader("Privacy") }
        item {
            InfoCard(
                "How AI works here",
                "NOVA talks to an OpenAI-compatible /chat/completions endpoint that you configure. " +
                    "Your key is encrypted with an AES-256-GCM key held in the Android Keystore and is never written " +
                    "to logs or backups. Password fields never expose text to AI. Nothing is sent automatically: " +
                    "every request starts from a tap. If the network is down the keyboard keeps working normally."
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    ServiceLocator.ai.setKey(null)
                    status = "API key removed."
                },
                modifier = Modifier.padding(16.dp)
            ) { Text("Remove API key") }
        }
        item {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            Text(
                "Tip: provider keys are created in your provider's dashboard. NOVA never bundles a key.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun GamingScreen() {
    val settings by ServiceLocator.settings.state.collectAsState()
    val repo = ServiceLocator.settings

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SettingSwitch(
                "Gaming mode",
                "Bigger keys, faster feedback, gaming theme and a phrase strip instead of predictions.",
                settings.gamingMode
            ) { repo.update { s -> s.copy(gamingMode = it) } }
        }
        item { SectionHeader("Feedback") }
        item {
            SettingSlider("Haptic strength", settings.hapticStrength.toFloat(), 0f..3f, 3, {
                listOf("off", "light", "medium", "strong")[it.toInt().coerceIn(0, 3)]
            }) { value -> repo.update { s -> s.copy(hapticStrength = value.toInt()) } }
        }
        item {
            SettingSwitch("Key sounds", "System click on every key press", settings.soundEnabled) {
                repo.update { s -> s.copy(soundEnabled = it) }
            }
        }
        item {
            SettingSwitch("Haptics", "Vibration feedback on key press", settings.hapticEnabled) {
                repo.update { s -> s.copy(hapticEnabled = it) }
            }
        }
        item { SectionHeader("Phrase library") }
        item {
            InfoCard("Tap-to-insert phrases", KeyboardLayouts.gamingPhrases().joinToString("  \u00B7  "))
        }
        item {
            InfoCard(
                "What gaming mode is not",
                "NOVA is a keyboard. It does not provide aim assistance, input injection, macro automation inside " +
                    "games, anti-cheat bypass or any form of gameplay manipulation, and it never claims to."
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    val settings by ServiceLocator.settings.state.collectAsState()
    val repo = ServiceLocator.settings
    val context = LocalContext.current
    val json = remember { Json { ignoreUnknownKeys = true; encodeDefaults = true } }
    var status by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionHeader("Layout") }
        item {
            SettingSlider("Keyboard height", settings.keyboardHeightScale, 0.75f..1.35f, 11, {
                (it * 100).toInt().toString() + "%"
            }) { value -> repo.update { s -> s.copy(keyboardHeightScale = value) } }
        }
        item {
            SettingSlider("Key spacing", settings.keySpacingDp.toFloat(), 0f..12f, 11, { it.toInt().toString() + " dp" }) {
                repo.update { s -> s.copy(keySpacingDp = it.toInt()) }
            }
        }
        item {
            SettingSwitch("Number row", "Show 1-9 above the letters", settings.numberRow) {
                repo.update { s -> s.copy(numberRow = it) }
            }
        }
        item {
            ChoiceRow(
                options = listOf("0" to "Both hands", "1" to "Left", "2" to "Right"),
                selected = settings.oneHanded.toString()
            ) { value -> repo.update { s -> s.copy(oneHanded = value.toIntOrNull() ?: 0) } }
        }
        item { SectionHeader("Typing") }
        item {
            SettingSwitch("Auto-capitalisation", null, settings.autoCapitalize) {
                repo.update { s -> s.copy(autoCapitalize = it) }
            }
        }
        item {
            SettingSwitch("Auto-correction", "Replace misspelled English words on space", settings.autoCorrect) {
                repo.update { s -> s.copy(autoCorrect = it) }
            }
        }
        item {
            SettingSwitch("Double-space full stop", null, settings.doubleSpacePeriod) {
                repo.update { s -> s.copy(doubleSpacePeriod = it) }
            }
        }
        item {
            SettingSwitch("Smart punctuation", null, settings.smartPunctuation) {
                repo.update { s -> s.copy(smartPunctuation = it) }
            }
        }
        item {
            SettingSwitch("Word suggestions", null, settings.predictionEnabled) {
                repo.update { s -> s.copy(predictionEnabled = it) }
            }
        }
        item { SectionHeader("Sound and haptics") }
        item {
            SettingSwitch("Key sounds", null, settings.soundEnabled) { repo.update { s -> s.copy(soundEnabled = it) } }
        }
        item {
            SettingSwitch("Haptic feedback", null, settings.hapticEnabled) { repo.update { s -> s.copy(hapticEnabled = it) } }
        }
        item { SectionHeader("Backup and restore") }
        item {
            Row(Modifier.padding(horizontal = 12.dp)) {
                OutlinedButton(
                    onClick = {
                        val payload = json.encodeToString(Settings.serializer(), ServiceLocator.settings.current)
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(ClipData.newPlainText("NOVA settings", payload))
                        status = "Settings copied to the clipboard."
                    },
                    modifier = Modifier.weight(1f).padding(4.dp)
                ) { Text("Export", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = {
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = manager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        val parsed = runCatching { json.decodeFromString(Settings.serializer(), text) }.getOrNull()
                        if (parsed == null) {
                            status = "Clipboard does not contain valid NOVA settings."
                        } else {
                            ServiceLocator.settings.replace(parsed)
                            status = "Settings restored."
                        }
                    },
                    modifier = Modifier.weight(1f).padding(4.dp)
                ) { Text("Restore", fontSize = 13.sp) }
            }
        }
        item {
            if (status.isNotEmpty()) InfoCard("Backup", status)
        }
        item {
            OutlinedButton(
                onClick = {
                    ServiceLocator.settings.replace(Settings())
                    status = "Everything reset to defaults."
                },
                modifier = Modifier.padding(16.dp)
            ) { Text("Reset all settings") }
        }
    }
}

@Composable
fun PrivacyScreen() {
    var refresh by remember { mutableIntStateOf(0) }
    val settings by ServiceLocator.settings.state.collectAsState()
    val repo = ServiceLocator.settings
    val stats = rememberDbValue(refresh, 0 to 0) { ServiceLocator.learning.stats() }
    val clipCount = rememberDbValue(refresh, 0) { ServiceLocator.clipboard.count() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            InfoCard(
                "What NOVA stores on this device",
                "Learned words and word pairs (" + stats.first + " words, " + stats.second + " pairs), " +
                    "clipboard entries (" + clipCount + "), snippets and macros, your settings, and your theme. " +
                    "All of it lives in app-private storage.",
                accent = true
            )
        }
        item {
            InfoCard(
                "What NOVA never does",
                "No keystroke logging. No background uploads. No password capture. No trackers or analytics SDKs. " +
                    "Predictions, learning and AI are disabled automatically for password fields, and text is " +
                    "never sent anywhere unless you tap an AI action."
            )
        }
        item { SectionHeader("Controls") }
        item {
            SettingSwitch("Learning", "Record the words I type", settings.learningEnabled) {
                repo.update { s -> s.copy(learningEnabled = it) }
            }
        }
        item {
            SettingSwitch("Clipboard history", "Keep a local clipboard list", settings.clipboardEnabled) {
                repo.update { s -> s.copy(clipboardEnabled = it) }
            }
        }
        item {
            SettingSwitch("AI features", "Allow explicit AI requests", settings.aiEnabled) {
                repo.update { s -> s.copy(aiEnabled = it) }
            }
        }
        item { SectionHeader("Delete data") }
        item {
            OutlinedButton(
                onClick = {
                    ServiceLocator.learning.eraseAll()
                    ServiceLocator.suggestions.invalidateCache()
                    refresh++
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) { Text("Erase learned words") }
        }
        item {
            OutlinedButton(
                onClick = { ServiceLocator.clipboard.clear(keepPinned = false); refresh++ },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) { Text("Clear clipboard history") }
        }
        item {
            OutlinedButton(
                onClick = {
                    ServiceLocator.database.writableDatabase.delete("snippets", null, null)
                    ServiceLocator.database.writableDatabase.delete("macros", null, null)
                    refresh++
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) { Text("Delete snippets and macros") }
        }
        item {
            InfoCard(
                "Permissions",
                "NOVA requests INTERNET (AI requests you start), VIBRATE (haptics) and ACCESS_NETWORK_STATE. " +
                    "It requests no contacts, storage, location, microphone or accessibility permissions."
            )
        }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var crashRefresh by remember { mutableIntStateOf(0) }
    val crashLog = remember(crashRefresh) { com.nova.keyboard.core.CrashReporter.lastCrash(context) }
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.0.0")
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text("NOVA KEYBOARD", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("TYPE BEYOND LIMITS.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text("Version " + version + " (build 1)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Text("Package " + context.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        item {
            if (crashLog != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "LAST CRASH LOG",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Long-press the text to copy it and send it back \u2014 it is the fastest way to fix the crash.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            crashLog,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        com.nova.keyboard.core.CrashReporter.clear(context)
                        crashRefresh++
                    }) { Text("Clear crash log", fontSize = 12.sp) }
                }
            }
        }
        item {
            InfoCard(
                "Privacy policy",
                "NOVA is a local-first keyboard. Typed text, learned words, clipboard entries, snippets and macros " +
                    "are stored only in this app's private storage on this device and are never uploaded. AI actions " +
                    "send the text you explicitly select to the provider you configure, over HTTPS, using your own " +
                    "key. Password fields are excluded from prediction, learning and AI. You can erase every stored " +
                    "dataset from the Privacy centre."
            )
        }
        item {
            InfoCard(
                "Open-source licences",
                "Kotlin (Apache 2.0), AndroidX / Jetpack Compose (Apache 2.0), Kotlin Coroutines (Apache 2.0), " +
                    "kotlinx.serialization (Apache 2.0), OkHttp (Apache 2.0), DataStore (Apache 2.0). " +
                    "Bundled English word frequencies derived from the wordfreq project (Apache 2.0). " +
                    "Sinhala letter mapping follows SLS 1134 / Wijesekara, an open standard."
            )
        }
        item {
            InfoCard(
                "Credits",
                "Designed and built as NOVA KEYBOARD. Sinhala input follows the Wijesekara layout standard; " +
                    "phonetic input uses NOVA's own transliteration scheme, documented in the Languages screen."
            )
        }
        item {
            Text(
                "© 2026 NOVA KEYBOARD",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        options.forEach { (value, label) ->
            val active = value == selected
            Column(
                Modifier
                    .weight(1f)
                    .padding(3.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color(0xFF0C0F14),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }
    }
}
