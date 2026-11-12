package com.nova.keyboard

import com.nova.keyboard.ui.keyboard.KeyCode
import com.nova.keyboard.ui.keyboard.KeyboardLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {

    @Test
    fun `english layout has four rows and every letter`() {
        val rows = KeyboardLayouts.english(false, false, false, false)
        assertEquals(4, rows.size)
        val letters = rows.flatten().filter { it.code == KeyCode.CHAR }.map { it.label }
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a", "s", "d", "f", "m", "z", "x", "c")
            .forEach { assertTrue(it + " missing", letters.contains(it)) }
        assertNotNull(rows.last().firstOrNull { it.code == KeyCode.SPACE })
        assertNotNull(rows.flatten().firstOrNull { it.code == KeyCode.BACKSPACE })
    }

    @Test
    fun `shift produces uppercase letters`() {
        val rows = KeyboardLayouts.english(true, false, false, false)
        val letters = rows.flatten().filter { it.code == KeyCode.CHAR }.map { it.label }
        assertTrue(letters.contains("Q"))
        assertTrue(letters.contains("M"))
    }

    @Test
    fun `number row can be added`() {
        val rows = KeyboardLayouts.english(false, true, false, false)
        assertEquals(5, rows.size)
        assertEquals("1", rows.first().first().label)
    }

    @Test
    fun `sinhala layout uses sinhala codepoints`() {
        val rows = KeyboardLayouts.sinhalaWijesekara(false, false)
        val labels = rows.flatten().filter { it.code == KeyCode.CHAR }.map { it.label }
        assertTrue(labels.any { it.contains("\u0D9A") })
        assertTrue(labels.any { it.contains("\u0D85") })
        assertTrue(labels.any { it.contains("\u0DBB") })
    }

    @Test
    fun `developer layout exposes programming symbols`() {
        val rows = KeyboardLayouts.dev("kotlin", false)
        val labels = rows.flatten().map { it.label }.toSet()
        listOf("{", "}", "[", "]", "(", ")", "<", ">", "=", "&&", "||", "->").forEach {
            assertTrue(it + " missing", labels.contains(it))
        }
        assertTrue(labels.contains("fun"))
    }

    @Test
    fun `email layout surfaces the at sign`() {
        val rows = KeyboardLayouts.english(false, false, true, false)
        assertTrue(rows.flatten().any { it.label == "@" })
    }
}
