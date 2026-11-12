package com.nova.keyboard.theme

import com.nova.keyboard.core.SettingsRepository
import kotlinx.serialization.json.Json

class ThemeManager(private val settings: SettingsRepository) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun customThemes(): List<NovaTheme> = runCatching {
        json.decodeFromString(ListSerializerHolder.serializer, settings.current.customThemesJson)
    }.getOrDefault(emptyList())

    fun allThemes(): List<NovaTheme> = BuiltInThemes.all + customThemes()

    fun active(): NovaTheme {
        val id = settings.current.activeThemeId
        return allThemes().firstOrNull { it.id == id } ?: BuiltInThemes.amoledCyber
    }

    fun apply(id: String) = settings.update { it.copy(activeThemeId = id) }

    fun save(theme: NovaTheme) {
        val existing = customThemes().filterNot { it.id == theme.id }
        writeCustom(existing + theme.copy(builtIn = false))
    }

    fun duplicate(theme: NovaTheme, newName: String): NovaTheme {
        val copy = theme.copy(
            id = "custom_" + System.currentTimeMillis(),
            name = newName,
            builtIn = false
        )
        save(copy)
        return copy
    }

    fun delete(id: String) {
        writeCustom(customThemes().filterNot { it.id == id })
        if (settings.current.activeThemeId == id) apply(BuiltInThemes.amoledCyber.id)
    }

    fun exportTheme(theme: NovaTheme): String = json.encodeToString(NovaTheme.serializer(), theme)

    fun importTheme(payload: String): NovaTheme? {
        val parsed = runCatching { json.decodeFromString(NovaTheme.serializer(), payload) }.getOrNull()
            ?: return null
        val theme = parsed.copy(id = "custom_" + System.currentTimeMillis(), builtIn = false)
        save(theme)
        return theme
    }

    fun reset(id: String) {
        val builtIn = BuiltInThemes.all.firstOrNull { it.id == id } ?: return
        save(builtIn.copy(id = "custom_" + System.currentTimeMillis(), builtIn = false))
    }

    fun updateActive(transform: (NovaTheme) -> NovaTheme) {
        val current = active()
        val updated = transform(current)
        if (current.builtIn) {
            val copy = updated.copy(id = "custom_" + System.currentTimeMillis(), name = current.name + " (edited)", builtIn = false)
            save(copy)
            apply(copy.id)
        } else {
            save(updated)
        }
    }

    private fun writeCustom(list: List<NovaTheme>) {
        val encoded = json.encodeToString(ListSerializerHolder.serializer, list)
        settings.update { it.copy(customThemesJson = encoded) }
    }
}

internal object ListSerializerHolder {
    val serializer = kotlinx.serialization.builtins.ListSerializer(NovaTheme.serializer())
}
