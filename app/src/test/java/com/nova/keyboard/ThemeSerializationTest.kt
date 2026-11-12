package com.nova.keyboard

import com.nova.keyboard.theme.BuiltInThemes
import com.nova.keyboard.theme.NovaTheme
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `theme round trips through json`() {
        val theme = BuiltInThemes.neonFusion
        val encoded = json.encodeToString(NovaTheme.serializer(), theme)
        val decoded = json.decodeFromString(NovaTheme.serializer(), encoded)
        assertEquals(theme, decoded)
    }

    @Test
    fun `ten built in themes exist with unique ids and distinct palettes`() {
        assertEquals(10, BuiltInThemes.all.size)
        assertEquals(10, BuiltInThemes.all.map { it.id }.distinct().size)
        assertEquals(10, BuiltInThemes.all.map { it.keyBackground }.distinct().size)
        assertTrue(BuiltInThemes.all.all { it.builtIn })
    }

    @Test
    fun `themes are genuinely different not just renamed`() {
        assertNotEquals(BuiltInThemes.amoledCyber.background, BuiltInThemes.liquidGlass.background)
        assertNotEquals(BuiltInThemes.minimalWhite.textColor, BuiltInThemes.developerTerminal.textColor)
        assertNotEquals(BuiltInThemes.gamingRgb.accent, BuiltInThemes.midnightPurple.accent)
    }
}
