package com.nova.keyboard.ui.keyboard

import com.nova.keyboard.language.WijesekaraEngine

/**
 * Every on-screen layout is generated here. Layouts are plain data so they can
 * be unit tested without an Android device.
 */
object KeyboardLayouts {

    private val ENGLISH_ALTERNATES = mapOf(
        "a" to listOf("á", "à", "â", "ä", "ã", "å", "@"),
        "e" to listOf("é", "è", "ê", "ë", "€"),
        "i" to listOf("í", "ì", "î", "ï"),
        "o" to listOf("ó", "ò", "ô", "ö", "õ", "ø"),
        "u" to listOf("ú", "ù", "û", "ü"),
        "n" to listOf("ñ"),
        "c" to listOf("ç"),
        "s" to listOf("ß", "$"),
        "y" to listOf("ý", "ÿ"),
        "z" to listOf("ž"),
        "l" to listOf("£"),
        "d" to listOf("°"),
        "t" to listOf("™"),
        "r" to listOf("®"),
        "p" to listOf("¶"),
        "?" to listOf("¿"),
        "!" to listOf("¡"),
        "-" to listOf("–", "—", "•"),
        "." to listOf("…")
    )

    private fun ch(value: String, weight: Float = 1f, alternates: List<String> = emptyList()): KeySpec =
        KeySpec(label = value, output = value, weight = weight, alternates = alternates)

    private fun letter(value: String): KeySpec = KeySpec(
        label = value,
        output = value,
        shiftLabel = value.uppercase(),
        alternates = ENGLISH_ALTERNATES[value].orEmpty()
    )

    private fun special(
        label: String,
        code: KeyCode,
        weight: Float = 1.5f,
        style: KeyStyle = KeyStyle.SPECIAL
    ) = KeySpec(label = label, code = code, output = "", weight = weight, style = style)

    private val shiftKey = special("⇧", KeyCode.SHIFT, 1.5f)
    private val backspaceKey = special("⌫", KeyCode.BACKSPACE, 1.5f)
    private val enterKey = special("⏎", KeyCode.ENTER, 1.5f, KeyStyle.ACCENT)
    private val langKey = special("🌐", KeyCode.LANG, 1.2f)
    private val emojiKey = special("☺", KeyCode.EMOJI, 1.2f)
    private val numKey = special("?123", KeyCode.PAGE_NUM, 1.5f)
    private val abcKey = special("ABC", KeyCode.PAGE_ABC, 1.5f)
    private val symKey = special("=\\<", KeyCode.PAGE_SYM, 1.5f)
    private val sym2Key = special("\\>", KeyCode.PAGE_SYM2, 1.5f)
    private val devKey = special("{ }", KeyCode.PAGE_DEV, 1.4f)

    private fun spaceKey(label: String = "space") =
        KeySpec(label = label, code = KeyCode.SPACE, output = " ", weight = 4f, style = KeyStyle.NORMAL)

    fun numberRow(): List<KeySpec> = listOf(
        ch("1"), ch("2"), ch("3"), ch("4"), ch("5"),
        ch("6"), ch("7"), ch("8"), ch("9"), ch("0")
    )

    fun english(shifted: Boolean, showNumberRow: Boolean, email: Boolean, url: Boolean): List<List<KeySpec>> {
        val rows = ArrayList<List<KeySpec>>()
        if (showNumberRow) rows.add(numberRow())
        rows.add(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { letter(it) })
        rows.add(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { letter(it) })
        rows.add(
            listOf(
                shiftKey,
                letter("z"), letter("x"), letter("c"), letter("v"),
                letter("b"), letter("n"), letter("m"),
                ch(",", 1f, listOf(";", ":")),
                if (email) ch("@", 1f, listOf(".")) else if (url) ch("/", 1f, listOf(":", ".")) else ch(".", 1f, listOf("!", "?")),
                backspaceKey
            )
        )
        rows.add(
            listOf(
                numKey,
                langKey,
                emojiKey,
                ch(",", 1f, listOf("!", "?")),
                spaceKey(),
                if (url) ch(".", 1f, listOf("/", ".com")) else ch(".", 1f, listOf(",", "?")),
                enterKey
            )
        )
        return if (shifted) rows.map { row -> row.map { shiftKey(it) } } else rows
    }

    private fun shiftKey(key: KeySpec): KeySpec {
        val upper = key.shiftLabel ?: return key
        return key.copy(
            label = upper,
            output = upper,
            alternates = key.shiftAlternates ?: key.alternates
        )
    }

    fun sinhalaWijesekara(shifted: Boolean, showNumberRow: Boolean): List<List<KeySpec>> {
        val rows = ArrayList<List<KeySpec>>()
        if (showNumberRow) rows.add(numberRow())
        rows.add(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { siKey(it, shifted) })
        rows.add(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", ";").map { siKey(it, shifted) })
        rows.add(
            listOf(
                shiftKey,
                siKey("z", shifted), siKey("x", shifted), siKey("c", shifted), siKey("v", shifted),
                siKey("b", shifted), siKey("n", shifted), siKey("m", shifted),
                siKey(",", shifted), siKey(".", shifted),
                backspaceKey
            )
        )
        rows.add(
            listOf(
                numKey,
                langKey,
                emojiKey,
                ch("\u0DCA", 1f),          // al-lakuna, handy as its own key
                spaceKey(),
                ch("\u0D82", 1f, listOf("\u0D83")),   // anusvara
                enterKey
            )
        )
        return rows
    }

    private fun siKey(key: String, shifted: Boolean): KeySpec {
        val glyph = if (shifted) {
            WijesekaraEngine.shifted[key] ?: WijesekaraEngine.base[key] ?: key
        } else {
            WijesekaraEngine.base[key] ?: key
        }
        val alternates = WijesekaraEngine.longPress[glyph].orEmpty()
        return KeySpec(
            label = glyph,
            output = glyph,
            weight = if (key == "," || key == ".") 1f else 1f,
            alternates = alternates,
            style = if (WijesekaraEngine.isConsonant(glyph)) KeyStyle.NORMAL else KeyStyle.NORMAL
        )
    }

    fun sinhalaPhonetic(shifted: Boolean, showNumberRow: Boolean, email: Boolean): List<List<KeySpec>> =
        english(shifted, showNumberRow, email, false)

    fun numbers(): List<List<KeySpec>> = listOf(
        numberRow(),
        listOf(ch("@"), ch("#"), ch("$"), ch("%"), ch("&"), ch("-"), ch("+"), ch("("), ch(")"), ch("/")),
        listOf(
            ch("*"), ch("\""), ch("'"), ch(":"), ch(";"), ch("!"), ch("?"),
            ch(","), ch("."), backspaceKey
        ),
        listOf(abcKey, symKey, spaceKey(), enterKey)
    )

    fun symbols(): List<List<KeySpec>> = listOf(
        listOf(ch("~"), ch("`"), ch("|"), ch("•"), ch("√"), ch("π"), ch("÷"), ch("×"), ch("¶"), ch("∆")),
        listOf(ch("£"), ch("¢"), ch("€"), ch("¥"), ch("₹"), ch("©"), ch("®"), ch("™"), ch("§"), ch("∞")),
        listOf(ch("≤"), ch("≥"), ch("≠"), ch("≈"), ch("±"), ch("←"), ch("→"), ch("↑"), ch("↓"), backspaceKey),
        listOf(abcKey, sym2Key, spaceKey(), enterKey)
    )

    fun symbols2(): List<List<KeySpec>> = listOf(
        listOf(ch("["), ch("]"), ch("{"), ch("}"), ch("<"), ch(">"), ch("^"), ch("_"), ch("\\"), ch("|")),
        listOf(ch("“"), ch("”"), ch("‘"), ch("’"), ch("«"), ch("»"), ch("…"), ch("–"), ch("—"), ch("°")),
        listOf(ch("¡"), ch("¿"), ch("§"), ch("†"), ch("‡"), ch("★"), ch("☆"), ch("♥"), ch("♦"), backspaceKey),
        listOf(abcKey, symKey, spaceKey(), enterKey)
    )

    /** Programming symbol pages, with keyword rows per language. */
    fun dev(language: String, showNumberRow: Boolean): List<List<KeySpec>> {
        val rows = ArrayList<List<KeySpec>>()
        if (showNumberRow) rows.add(numberRow())
        rows.add(listOf(ch("{"), ch("}"), ch("["), ch("]"), ch("("), ch(")"), ch("<"), ch(">"), ch("="), ch("==")))
        rows.add(listOf(ch("+"), ch("-"), ch("*"), ch("/"), ch("%"), ch("!"), ch("&&"), ch("||"), ch(";"), ch(":")))
        rows.add(listOf(ch("\""), ch("'"), ch("#"), ch("$"), ch("@"), ch("_"), ch("\\"), ch("|"), ch("`"), ch("~")))
        rows.add(devKeywords(language))
        rows.add(listOf(abcKey, symKey, ch("->", 1f, listOf("=>")), spaceKey(), ch(";", 1f), backspaceKey, enterKey))
        return rows
    }

    private fun devKeywords(language: String): List<KeySpec> {
        val keywords = when (language) {
            "python" -> listOf("def", "return", "import", "print(", "None", "self.", "elif", "for ", "if ")
            "javascript", "typescript" -> listOf("const", "let", "function", "=>", "return", "console.log(", "async", "await", "null")
            "kotlin" -> listOf("fun", "val", "var", "class", "when", "return", "null", "override", "suspend")
            "java" -> listOf("public", "private", "void", "class", "return", "new ", "final", "static", "null")
            "php" -> listOf("<?php", "function", "echo", "return", "foreach", "->", "=>", "$this", "null")
            "c" -> listOf("#include", "int", "void", "return", "struct", "printf(", "char", "for ", "if ")
            "cpp" -> listOf("#include", "std::", "int", "void", "return", "class", "template", "auto", "nullptr")
            "csharp" -> listOf("public", "private", "void", "class", "return", "var", "string", "using", "null")
            "sql" -> listOf("SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "JOIN", "GROUP BY", "ORDER BY", "LIMIT")
            "html" -> listOf("<div>", "</div>", "<span>", "<a href=", "<img src=", "<p>", "<ul>", "<li>", "class=")
            "css" -> listOf("{", "}", ":", ";", "color:", "margin:", "padding:", "display:", "flex")
            "json" -> listOf("{", "}", "[", "]", "\"key\":", "\"value\"", ",", "true", "null")
            "xml" -> listOf("<tag>", "</tag>", "<?", "?>", "<!--", "-->", "attr=", "/>", "xmlns:")
            "bash" -> listOf("echo ", "for ", "do", "done", "if ", "then", "fi", "export ", "|")
            else -> listOf("fun", "val", "return", "class", "if ", "else", "for ", "while ", "null")
        }
        return keywords.take(9).map { KeySpec(label = it, output = it, weight = if (it.length > 4) 1.6f else 1.2f) }
    }

    /** Extra symbol row shown on the letters page while Developer mode is on. */
    fun devQuickRow(): List<KeySpec> = listOf(
        ch("{"), ch("}"), ch("["), ch("]"), ch("("), ch(")"), ch("="), ch(";"), ch(":"), ch("_")
    )

    fun gamingPhrases(): List<String> = listOf(
        "GG", "gg wp", "nice one!", "well played", "brb", "omw", "1 sec", "gl hf",
        "let's go", "clutch!", "ez", "wow", "sorry, lag", "party up?", "good luck!"
    )
}
