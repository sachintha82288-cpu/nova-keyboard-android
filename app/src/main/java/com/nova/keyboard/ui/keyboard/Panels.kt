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

data class PanelItem(
    val primary: String,
    val secondary: String? = null,
    val payload: String,
    val id: Long = 0L
)

/** Generic scrollable picker used by the clipboard, snippets and phrase panels. */
class ListPanelView(
    context: Context,
    private val theme: NovaTheme,
    title: String,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    var onPick: ((PanelItem) -> Unit)? = null
    var onDelete: ((PanelItem) -> Unit)? = null

    private val listContainer = LinearLayout(context)
    private val emptyLabel = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        setPadding(dp(6), dp(4), dp(6), dp(6))

        addView(header(context, title, onClose))

        listContainer.orientation = VERTICAL
        val scroll = ScrollView(context)
        scroll.addView(listContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(220)))

        emptyLabel.text = "Nothing here yet."
        emptyLabel.setTextColor(theme.suggestionText)
        emptyLabel.textSize = 13f
        emptyLabel.setPadding(dp(10), dp(10), dp(10), dp(10))
        addView(emptyLabel)
    }

    fun setItems(items: List<PanelItem>) {
        listContainer.removeAllViews()
        emptyLabel.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        items.forEach { item ->
            val row = LinearLayout(context)
            row.orientation = HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(8), dp(8), dp(8), dp(8))

            val label = TextView(context)
            label.text = if (item.secondary.isNullOrBlank()) item.primary else item.primary + "\n" + item.secondary
            label.setTextColor(theme.textColor)
            label.textSize = 14f
            label.maxLines = 3
            label.isSingleLine = false
            row.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

            row.isClickable = true
            row.setOnClickListener { onPick?.invoke(item) }
            if (onDelete != null) {
                val delete = TextView(context)
                delete.text = "\u2715"
                delete.setTextColor(theme.accent)
                delete.textSize = 16f
                delete.setPadding(dp(10), dp(4), dp(10), dp(4))
                delete.setOnClickListener { onDelete?.invoke(item) }
                row.addView(delete)
            }
            listContainer.addView(row)
        }
    }

    private fun header(context: Context, title: String, onClose: () -> Unit): LinearLayout {
        val bar = LinearLayout(context)
        bar.orientation = HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        val label = TextView(context)
        label.text = title
        label.setTextColor(theme.accent)
        label.textSize = 13f
        label.typeface = Typeface.DEFAULT_BOLD
        bar.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val close = TextView(context)
        close.text = "\u2715"
        close.setTextColor(theme.specialText)
        close.textSize = 16f
        close.setPadding(dp(12), dp(4), dp(12), dp(4))
        close.setOnClickListener { onClose() }
        bar.addView(close)
        return bar
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
