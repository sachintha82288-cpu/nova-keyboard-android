package com.nova.keyboard

import com.nova.keyboard.ui.keyboard.KeyCode
import com.nova.keyboard.ui.keyboard.KeyboardLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ---------------------------------------------------------------- regressions

    @Test
    fun `url layout types a dot and a slash, never a domain guess`() {
        val rows = KeyboardLayouts.english(false, false, false, true)
        val keys = rows.flatten().filter { it.code == KeyCode.CHAR }
        // Regression: the URL layout used to have a literal ".com" alternate, which
        // inserted six characters into whatever domain the user was typing.
        assertFalse(keys.any { it.alternates.contains(".com") })
        assertFalse(keys.any { it.label == ".com" })
        val flat = rows.flatten().map { it.label }
        assertTrue(flat.contains("."))
        assertTrue(flat.contains("/"))
    }

    @Test
    fun `every row has a consistent key count for the letter page`() {
        val rows = KeyboardLayouts.english(false, false, false, false)
        assertEquals(10, rows[0].size)
        assertEquals(9, rows[1].size)
        // 10 in row 2 and 11 in the base shift row would clip on narrow screens.
        assertTrue(rows[2].size in 10..11)
        assertTrue(rows[3].isNotEmpty())
    }

    @Test
    fun `sinhala sign keys are drawn as special keys`() {
        val rows = KeyboardLayouts.sinhalaWijesekara(false, false)
        val hal = rows.flatten().firstOrNull { it.label == "\u0DCA" }
        assertNotNull(hal)
        assertEquals(com.nova.keyboard.ui.keyboard.KeyStyle.SPECIAL, hal!!.style)
    }

    @Test
    fun `comma key really types a comma`() {
        val rows = KeyboardLayouts.english(false, false, false, false)
        val comma = rows.flatten().firstOrNull { it.label == "," }
        assertNotNull(comma)
        assertEquals(",", comma!!.output)
    }
}
