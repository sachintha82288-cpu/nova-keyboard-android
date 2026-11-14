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
        assertEquals("\u0DC1\u0DCA\u200D\u0DBB\u0DD3", t("shrii"))
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

    // ---------------------------------------------------------------- regressions

    @Test
    fun `word final consonant keeps al lakuna`() {
        // Regression: "ran" used to come out as රන, which reads "rana".
        assertEquals("\u0DBB\u0DB1\u0DCA", t("ran"))
        assertEquals("\u0DC0\u0DA7\u0D9A\u0DCA", t("watak"))
    }

    @Test
    fun `th is the dental ta and t is the retroflex ta`() {
        // Regression: the table had t -> ත and th -> ථ, so "thaththa" came out ථථ්ථ.
        assertEquals("\u0DAD\u0DAD\u0DCA\u0DAD", t("thaththa"))
        assertEquals("\u0D9A\u0DA7\u0DB1\u0DCA", t("katan"))
    }

    @Test
    fun `n before a stop becomes the homorganic nasal`() {
        // Regression: "sinhala" came out with a bare න instead of ං, "Kandy" lost ඩ.
        assertEquals("\u0DC3\u0DD2\u0D82\u0DC4\u0DBD", t("sinhala"))
        assertEquals("\u0D9A\u0DD0\u0DB1\u0DCA\u0DA9\u0DD2", t("Kandy"))
        assertEquals("\u0D9A\u0DB1\u0DCA\u0DA9", t("kanda"))
    }

    @Test
    fun `nasal clusters inside ordinary words stay correct`() {
        assertEquals("\u0DBA\u0DB1\u0DCA\u0DB1", t("yanna"))
        assertEquals("\u0D9A\u0DBB\u0DB1\u0DCA\u0DB1", t("karanna"))
        assertEquals("\u0DB4\u0DD2\u0DC3\u0DCA\u0DC3\u0DD4", t("pissu"))
    }

    @Test
    fun `capitalised singlish is folded, not mangled`() {
        // Regression: "Hello" became "ඃඑල්ලො" because H is the visarga letter.
        assertEquals("\u0DC4\u0DD9\u0DBD\u0DCA\u0DBD\u0DDC", t("Hello"))
    }

    @Test
    fun `sri ligature is produced whole`() {
        assertEquals("\u0DC1\u0DCA\u200D\u0DBB\u0DD3", t("sri"))
        assertTrue(t("sri lanka").startsWith("\u0DC1\u0DCA\u200D\u0DBB\u0DD3 "))
    }

    @Test
    fun `candidates always include the direct conversion first`() {
        val out = SinhalaTransliterator.candidates("api", listOf("\u0D85\u0DB4\u0DD2\u0DA7", "\u0D85\u0DB4\u0DD2"))
        assertEquals("\u0D85\u0DB4\u0DD2", out.first())
    }
}
