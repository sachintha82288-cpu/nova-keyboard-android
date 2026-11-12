package com.nova.keyboard.ui.keyboard

data class EmojiEntry(val glyph: String, val keywords: String, val category: String)

/** Bundled emoji, kaomoji and Unicode symbol sets. No network, no fonts needed. */
object EmojiData {

    private val raw = listOf(
        "\uD83D\uDE00|grin happy smile|smileys", "\uD83D\uDE02|laugh joy tears|smileys",
        "\uD83D\uDE04|smile happy|smileys", "\uD83D\uDE01|grin happy|smileys",
        "\uD83D\uDE06|sweat smile|smileys", "\uD83D\uDE05|sweat relief|smileys",
        "\uD83D\uDE0D|love heart eyes|smileys", "\uD83D\uDE18|wink|smileys",
        "\uD83D\uDE09|wink playful|smileys", "\uD83D\uDE0A|blush smile|smileys",
        "\uD83D\uDE0B|yum tasty|smileys", "\uD83D\uDE0E|cool sunglasses|smileys",
        "\uD83D\uDE12|neutral|smileys", "\uD83D\uDE14|expressionless|smileys",
        "\uD83D\uDE15|thinking?|smileys", "\uD83E\uDD14|thinking|smileys",
        "\uD83D\uDE10|confused|smileys", "\uD83D\uDE16|sad|smileys",
        "\uD83D\uDE22|cry sad|smileys", "\uD83D\uDE2D|crying loud|smileys",
        "\uD83D\uDE24|angry|smileys", "\uD83D\uDE20|angry mad|smileys",
        "\uD83E\uDD2F|shush|smileys", "\uD83D\uDE34|surprised|smileys",
        "\uD83D\uDE31|kiss|smileys", "\uD83D\uDE36|sleepy|smileys",
        "\uD83D\uDE37|sleep|smileys", "\uD83E\uDD12|yawn|smileys",
        "\uD83D\uDE3A|smile halo angel|smileys", "\uD83D\uDE08|devil|smileys",
        "\uD83E\uDD21|party face|smileys", "\uD83E\uDD73|nerd|smileys",
        "\uD83E\uDD7A|monocle|smileys", "\uD83D\uDE2C|pensive|smileys",
        "\uD83D\uDE29|worried|smileys", "\uD83D\uDE1F|hug|smileys",
        "\uD83D\uDE33|worried sad|smileys", "\uD83D\uDE35|dog face|smileys",
        "\uD83D\uDE38|sick|smileys", "\uD83D\uDE39|vomit|smileys",

        "\uD83D\uDC4D|thumbs up like yes|people", "\uD83D\uDC4E|thumbs down dislike|people",
        "\uD83D\uDC4F|clap applause|people", "\uD83D\uDE4F|pray thanks please|people",
        "\uD83D\uDC4C|ok|people", "\u270C|peace victory|people",
        "\uD83E\uDD1D|handshake deal|people", "\uD83D\uDCAA|wave hello hi|people",
        "\uD83D\uDC49|writing|people", "\uD83D\uDC40|eyes look|people",
        "\uD83D\uDC80|muscle strong|people", "\uD83D\uDC7B|running fast|people",
        "\uD83D\uDC83|walking|people", "\uD83E\uDDD1|standing person|people",
        "\uD83D\uDC68|man|people", "\uD83D\uDC69|woman|people",
        "\uD83D\uDC66|man working|people", "\uD83D\uDC67|woman working|people",
        "\uD83D\uDC76|person programmer dev|people", "\uD83D\uDC70|person tech|people",
        "\uD83E\uDDD8|student|people", "\uD83D\uDC71|teacher|people",
        "\uD83D\uDC6E|police|people", "\uD83E\uDD36|doctor|people",
        "\uD83D\uDC78|person crown|people", "\uD83D\uDC7C|person frown|people",
        "\uD83E\uDD34|brain smart|people", "\uD83D\uDCAB|point up|people",
        "\uD83D\uDC47|point down|people", "\uD83D\uDC48|point left|people",
        "\uD83D\uDC4B|fist|people", "\uD83D\uDC4A|fist bump|people",

        "\uD83D\uDC36|dog pet|animals", "\uD83D\uDC31|cat pet|animals",
        "\uD83D\uDC2D|mouse|animals", "\uD83D\uDC39|rabbit|animals",
        "\uD83E\uDD8A|fox|animals", "\uD83D\uDC3B|bear|animals",
        "\uD83D\uDC3C|panda|animals", "\uD83D\uDC38|monkey|animals",
        "\uD83E\uDD81|lion|animals", "\uD83D\uDC2F|tiger|animals",
        "\uD83D\uDC34|horse|animals", "\uD83D\uDC2E|cow|animals",
        "\uD83D\uDC37|pig|animals", "\uD83D\uDC14|bird|animals",
        "\uD83D\uDC23|fish|animals", "\uD83D\uDC19|elephant|animals",
        "\uD83E\uDD84|butterfly|animals", "\uD83D\uDC1D|snail|animals",
        "\uD83D\uDC0D|snake|animals", "\uD83D\uDC22|bee honey|animals",
        "\uD83C\uDF3F|cactus plant|animals", "\uD83C\uDF31|flower rose|animals",
        "\uD83C\uDF3B|flower sun|animals", "\uD83C\uDF3A|flower tulip|animals",

        "\uD83C\uDF4E|food fruit|food", "\uD83C\uDF4A|strawberry|food",
        "\uD83C\uDF49|grapes|food", "\uD83C\uDF4C|banana|food",
        "\uD83C\uDF4E|mango|food", "\uD83E\uDD65|mango|food",
        "\uD83C\uDF55|pizza|food", "\uD83C\uDF54|burger|food",
        "\uD83C\uDF5F|rice curry|food", "\uD83C\uDF5C|noodles|food",
        "\uD83C\uDF59|chocolate|food", "\uD83C\uDF70|ice cream|food",
        "\uD83C\uDF6E|cake dessert|food", "\u2615|coffee tea hot|food",
        "\uD83C\uDF75|tea drink|food", "\uD83E\uDDC3|juice water drink|food",
        "\uD83C\uDF7A|beer|food", "\uD83E\uDD5A|bread|food",
        "\uD83E\uDD51|spicy food|food", "\uD83C\uDF5B|food breakfast|food",

        "\u26BD|football soccer|activity", "\uD83C\uDFC0|ball sports|activity",
        "\uD83C\uDFC6|basketball|activity", "\uD83C\uDFCF|tennis|activity",
        "\uD83C\uDFAE|game gaming controller|activity", "\uD83C\uDFB2|dice game|activity",
        "\uD83E\uDD47|chess strategy|activity", "\uD83C\uDFAF|target goal|activity",
        "\uD83C\uDFC6|win trophy|activity", "\uD83C\uDFC6|prize|activity",
        "\uD83D\uDCAA|swim water|activity", "\uD83D\uC3CA|cycle|activity",
        "\uD83C\uDFC8|dance music|activity", "\uD83C\uDFB8|music note|activity",
        "\uD83C\uDFA7|headphones|activity", "\uD83D\uDCF7|camera photo|activity",
        "\uD83C\uDFAC|movie film|activity", "\uD83D\uDCFA|tv|activity",

        "\uD83D\uDE80|rocket launch ship|travel", "\u2708|plane flight travel|travel",
        "\uD83D\uDE97|car drive|travel", "\uD83D\uDE95|car taxi|travel",
        "\uD83D\uDE8C|bus|travel", "\uD83D\uDE82|train|travel",
        "\uD83D\uDEB2|bike|travel", "\u26F5|boat ship|travel",
        "\uD83C\uDFD6|beach island|travel", "\uD83C\uDFDD|hotel bed|travel",
        "\uD83C\uDFD4|tent camp|travel", "\uD83D\uDFFA|map|travel",
        "\uD83C\uDF0D|mountain|travel", "\uD83C\uDF06|sun weather hot|travel",
        "\uD83C\uDF27|rain weather|travel", "\u26A1|lightning power fast|travel",

        "\uD83D\uDCBB|computer laptop dev|objects", "\uD83D\uDCBB|pc|objects",
        "\u2328|keyboard typing|objects", "\uD83D\uDCF1|phone mobile|objects",
        "\uD83D\uDD0B|search magnifier|objects", "\uD83D\uDD12|lock security|objects",
        "\uD83D\uDD11|key password|objects", "\uD83D\uDCE7|book read|objects",
        "\uD83D\uDCDD|book notebook|objects", "\u270F|pencil write|objects",
        "\uD83D\uDD8A|link url|objects", "\uD83D\uDCCE|mail email|objects",
        "\uD83D\uDCE6|package box|objects", "\uD83D\uDCA1|money cash|objects",
        "\uD83D\uDCB3|card pay|objects", "\uD83D\uDCC5|chart graph|objects",
        "\uD83D\uDCC8|chart up|objects", "\uD83D\uDCCC|calendar date|objects",
        "\u23F0|alarm time|objects", "\u23F3|hourglass wait|objects",
        "\uD83D\uDCA1|bag money|objects", "\uD83D\uDD27|bulb idea|objects",
        "\uD83C\uDFAF|flag goal|objects", "\uD83D\uDEE0|tool fix|objects",

        "\u2764|heart love|symbols", "\uD83D\uDC94|broken heart|symbols",
        "\uD83D\uDC95|heart beat|symbols", "\u2705|check done yes|symbols",
        "\u274C|cross no wrong|symbols", "\u26A0|warning|symbols",
        "\u2B50|star favourite|symbols", "\uD83D\uDD25|fire hot|symbols",
        "\uD83D\uDCAF|100 perfect|symbols", "\u2757|exclamation|symbols",
        "\u2753|question|symbols", "\uD83D\uDCE2|shock|symbols",
        "\u2714|check|symbols", "\u27A1|arrow right|symbols",
        "\u2B06|arrow up|symbols", "\uD83D\uDD01|arrow down|symbols",
        "\u267B|recycle|symbols", "\u2699|settings gear|symbols",
        "\uD83D\uDD34|clock|symbols", "\uD83D\uDCF8|new|symbols"
    )

    val all: List<EmojiEntry> by lazy {
        raw.map { line ->
            val parts = line.split("|")
            EmojiEntry(parts[0], parts.getOrElse(1) { "" }, parts.getOrElse(2) { "symbols" })
        }.distinctBy { it.glyph }
    }

    val categories: List<String> by lazy { all.map { it.category }.distinct() }

    val kaomoji = listOf(
        "(  \u0361\u00B0 \u035C\u0296 \u0361\u00B0)", "(\uFF61\uFF9F\uFE4F\uFF61)", "(\u00AC\u200A\uFE4F\u200A\u00AC)",
        "(\uFFE3\u2019\u03C9\uFFE3`)", "(\u0E51\u02C3\u1D17\u02C2\u0E51)", "(\u2022\uFE4F\u2022)",
        "\u0295\u0361\u0E4F\u0361\u0294", "(\uFF9F\uFF65\u03C9\uFF65\uFF9F)", "\u0CA0_\u0CA0",
        "(\u2312\uFF61\uFF9F)", "\uFF3C\uFF3C\uFF3C(\uFF9F\u25A1\uFF9F)", "\u30FD(\u0E51\u02C3\u1D17\u02C2\u0E51)\uFF89",
        "(\u256F\u00B0\u25A1\u00B0\uFF09\u256F\uFE35 \u2501\u2501", "\u252C\u2500\u252C\u30CE( \u00BA _ \u00BA\u30CE)",
        "(\u3000\u30FB\u2200\u30FB)", "\u0295\u1D25\u0294"
    )

    val unicodeSymbols = listOf(
        "\u2190", "\u2191", "\u2192", "\u2193", "\u2194", "\u21D2", "\u21D0", "\u21D4",
        "\u2260", "\u2264", "\u2265", "\u2248", "\u221E", "\u221A", "\u03C0", "\u00B1",
        "\u00D7", "\u00F7", "\u2211", "\u2206", "\u2207", "\u222B", "\u2202", "\u00B5",
        "\u20B9", "\u20AC", "\u00A3", "\u00A5", "\u00A2", "\u20BF", "\u00A9", "\u00AE",
        "\u2022", "\u2026", "\u2014", "\u2013", "\u00A7", "\u00B6", "\u2020", "\u2021",
        "\u2605", "\u2606", "\u2665", "\u2666", "\u2663", "\u2660", "\u2713", "\u2717",
        "\u2318", "\u238B", "\u23CE", "\u21E5", "\u2325", "\u2326", "\u2423", "\u00B0"
    )

    fun search(query: String): List<EmojiEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { it.keywords.contains(q) }
    }
}
