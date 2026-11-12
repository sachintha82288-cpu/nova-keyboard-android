package com.nova.keyboard.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Every user-facing preference in one immutable snapshot. */
@Serializable
data class Settings(
    val keyboardHeightScale: Float = 1.0f,
    val keySpacingDp: Int = 4,
    val keyRadiusDp: Int = 14,
    val keyAlpha: Float = 1.0f,
    val oneHanded: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val hapticStrength: Int = 2,
    val autoCapitalize: Boolean = true,
    val autoCorrect: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val smartPunctuation: Boolean = true,
    val predictionEnabled: Boolean = true,
    val nextWordPrediction: Boolean = true,
    val learningEnabled: Boolean = true,
    val numberRow: Boolean = false,
    val language: String = "en",
    val sinhalaInputMethod: String = "wijesekara",
    val activeThemeId: String = "amoled_cyber",
    val customThemesJson: String = "[]",
    val gamingMode: Boolean = false,
    val devMode: Boolean = false,
    val devLanguage: String = "kotlin",
    val aiEnabled: Boolean = false,
    val aiBaseUrl: String = "https://api.openai.com/v1",
    val aiModel: String = "gpt-4o-mini",
    val aiTimeoutSeconds: Int = 30,
    val aiRewrite: Boolean = true,
    val aiGrammar: Boolean = true,
    val aiTranslate: Boolean = true,
    val aiGenerate: Boolean = true,
    val clipboardEnabled: Boolean = true,
    val clipboardLimit: Int = 100,
    val clipboardExpiryDays: Int = 30,
    val onboardingDone: Boolean = false,
    val lastBackupAt: Long = 0L
)

private val Context.novaStore by preferencesDataStore(name = "nova_settings")
private val SETTINGS_KEY = stringPreferencesKey("settings_json")

class SettingsRepository(private val context: Context, private val scope: CoroutineScope) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    /** Cached snapshot; safe to read from the IME's main thread. */
    val current: Settings get() = _state.value

    fun start() {
        scope.launch {
            val stored = runCatching { context.novaStore.data.first()[SETTINGS_KEY] }.getOrNull()
            if (stored != null) {
                runCatching { json.decodeFromString(Settings.serializer(), stored) }
                    .onSuccess { _state.value = it }
            }
            context.novaStore.data.collect { prefs ->
                val raw = prefs[SETTINGS_KEY] ?: return@collect
                runCatching { json.decodeFromString(Settings.serializer(), raw) }
                    .onSuccess { if (it != _state.value) _state.value = it }
            }
        }
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        _state.value = next
        scope.launch {
            context.novaStore.edit { it[SETTINGS_KEY] = json.encodeToString(Settings.serializer(), next) }
        }
    }

    fun replace(next: Settings) = update { next }
}
