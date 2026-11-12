package com.nova.keyboard.ui.keyboard

enum class KeyCode {
    CHAR, SHIFT, BACKSPACE, ENTER, SPACE, LANG, EMOJI, PANEL_CLIPBOARD, PANEL_SNIPPETS,
    PANEL_AI, PAGE_NUM, PAGE_SYM, PAGE_SYM2, PAGE_DEV, PAGE_ABC, SETTINGS, TAB,
    CURSOR_LEFT, CURSOR_RIGHT, HIDE, ONE_HAND
}

enum class KeyStyle { NORMAL, SPECIAL, ACCENT, DANGER }

data class KeySpec(
    val label: String,
    val code: KeyCode = KeyCode.CHAR,
    val output: String = label,
    val weight: Float = 1f,
    val shiftLabel: String? = null,
    val alternates: List<String> = emptyList(),
    val style: KeyStyle = KeyStyle.NORMAL,
    val shiftAlternates: List<String>? = null
)
