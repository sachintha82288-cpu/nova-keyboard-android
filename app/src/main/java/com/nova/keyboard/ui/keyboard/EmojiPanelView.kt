package com.nova.keyboard.ui.keyboard

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nova.keyboard.theme.NovaTheme

/** Emoji, kaomoji and Unicode symbol browser with live keyword search. */
class EmojiPanelView(
    context: Context,
    private val theme: NovaTheme,
    private val onPick: (String) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private val grid = GridLayout(context)
    private val tabsRow = LinearLayout(context)
    private var mode = "emoji"

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        setPadding(dp(6), dp(2), dp(6), dp(6))

        val header = LinearLayout(context)
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val search = EditText(context)
        search.hint = "Search emoji"
        search.textSize = 13f
        search.setSingleLine(true)
        search.setTextColor(theme.textColor)
        search.setHintTextColor(theme.specialText)
        search.setBackgroundColor(theme.keyBackground)
        // This EditText lives inside the IME window: if it asked for an input
        // method the keyboard would try to show itself on top of itself.
        search.showSoftInputOnFocus = false
        search.isFocusableInTouchMode = true
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                render(s?.toString().orEmpty())
            }
        })
        header.addView(search, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val close = TextView(context)
        close.text = "\u2715"
        close.setTextColor(theme.specialText)
        close.textSize = 16f
        close.setPadding(dp(12), dp(2), dp(12), dp(2))
        close.setOnClickListener { onClose() }
        header.addView(close)
        addView(header)

        tabsRow.orientation = HORIZONTAL
        val tabsScroll = HorizontalScrollView(context)
        tabsScroll.isHorizontalScrollBarEnabled = false
        tabsScroll.addView(tabsRow)
        addView(tabsScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(32)))
        buildTabs()

        grid.columnCount = 8
        val scroll = ScrollView(context)
        scroll.addView(grid)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(200)))

        render("")
    }

    private fun buildTabs() {
        tabsRow.removeAllViews()
        val tabs = listOf("emoji" to "Emoji", "kaomoji" to "Kaomoji", "symbols" to "Symbols")
        tabs.forEach { (key, label) ->
            val tab = TextView(context)
            tab.text = label
            tab.textSize = 12f
            tab.setPadding(dp(10), dp(4), dp(10), dp(4))
            tab.setTextColor(if (mode == key) theme.accent else theme.specialText)
            if (mode == key) tab.typeface = Typeface.DEFAULT_BOLD
            tab.setOnClickListener {
                mode = key
                buildTabs()
                render("")
            }
            tabsRow.addView(tab)
        }
    }

    private fun render(query: String) {
        grid.removeAllViews()
        val glyphs: List<String> = when (mode) {
            "kaomoji" -> EmojiData.kaomoji
            "symbols" -> EmojiData.unicodeSymbols
            else -> EmojiData.search(query).map { it.glyph }
        }
        val filtered = if (mode == "emoji" && query.isBlank()) glyphs else glyphs
        filtered.take(240).forEach { glyph ->
            val cell = TextView(context)
            cell.text = glyph
            cell.textSize = 20f
            cell.gravity = Gravity.CENTER
            cell.setPadding(dp(4), dp(4), dp(4), dp(4))
            cell.isClickable = true
            cell.setOnClickListener { onPick(glyph) }
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            grid.addView(cell, lp)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
