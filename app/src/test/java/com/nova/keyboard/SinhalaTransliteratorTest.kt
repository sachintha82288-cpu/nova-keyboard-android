package com.nova.keyboard

import com.nova.keyboard.language.SinhalaTransliterator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SinhalaTransliteratorTest {

    private fun t(input: String) = SinhalaTransliterator.transliterate(input)

    @Test
    fun `simple words convert`() {
        assertEquals("\u0D85\u0DB4\u0DD2", t("api"))              // api
        assertEquals("\u0DC3\u0DD4\u0DB6", t("suba"))             // suba
        assertEquals("\u0D9A\u0DDC\u0DC4\u0DDC\u0DB8\u0DAF", t("kohomada")) // kohomada
    }

    @Test
    fun `doubled consonant inserts al lakuna`() {
        assertEquals("\u0D85\u0DB8\u0DCA\u0DB8", t("amma"))
        assertEquals("\u0D85\u0DB8\u0DCA\u0DB8\u0DCF", t("ammaa"))
    }

    @Test
    fun `long vowels and signs`() {
        assertEquals("\u0D94\u0DBA\u0DCF", t("oyaa"))
        assertEquals("\u0DC1\u0DCA\u0DBB\u0DD3", t("shrii"))
    }

    @Test
    fun `anusvara and non letter characters pass through`() {
        assertTrue(t("buddhaM").endsWith("\u0D82"))
        assertEquals("123!", t("123!"))
    }

    @Test
    fun `inherent vowel produces no extra sign`() {
        // "k" + inherent a stays a bare consonant, "kaa" takes the aela sign.
        assertEquals("\u0D9A", t("ka"))
        assertEquals("\u0D9A\u0DCF", t("kaa"))
    }

    @Test
    fun `empty input is safe`() {
        assertEquals("", t(""))
    }

    @Test
    fun `candidates always include the direct conversion first`() {
        val candidates = SinhalaTransliterator.candidates("amma", listOf("\u0D85\u0DB8\u0DCA\u0DB8\u0DCF"))
        assertEquals("\u0D85\u0DB8\u0DCA\u0DB8", candidates.first())
    }
}
