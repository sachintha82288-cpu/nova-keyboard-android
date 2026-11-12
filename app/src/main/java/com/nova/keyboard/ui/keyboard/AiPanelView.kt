package com.nova.keyboard.ui.keyboard

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nova.keyboard.theme.NovaTheme

enum class AiAction {
    IMPROVE, SHORTER, LONGER, PROFESSIONAL, CASUAL, FRIENDLY, FORMAL,
    GRAMMAR, TO_SINHALA, TO_ENGLISH, SUMMARIZE, REPLY, EMAIL, CAPTION
}

/** AI Studio inside the keyboard: every action is an explicit user tap. */
class AiPanelView(
    context: Context,
    private val theme: NovaTheme,
    private val onAction: (AiAction) -> Unit,
    private val onInsert: (String) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private val status = TextView(context)
    private val resultView = TextView(context)
    private val actionsGrid = GridLayout(context)
    private var result = ""

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        setPadding(dp(8), dp(4), dp(8), dp(8))

        val header = LinearLayout(context)
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(context)
        title.text = "NOVA AI"
        title.setTextColor(theme.accent)
        title.textSize = 13f
        title.typeface = Typeface.DEFAULT_BOLD
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val close = TextView(context)
        close.text = "\u2715"
        close.setTextColor(theme.specialText)
        close.textSize = 16f
        close.setPadding(dp(10), dp(2), dp(10), dp(2))
        close.setOnClickListener { onClose() }
        header.addView(close)
        addView(header)

        actionsGrid.columnCount = 4
        addView(actionsGrid)

        status.setTextColor(theme.specialText)
        status.textSize = 12f
        status.setPadding(0, dp(4), 0, dp(4))
        status.text = "Select text (or place the cursor) then choose an action."
        addView(status)

        resultView.setTextColor(theme.textColor)
        resultView.textSize = 14f
        resultView.setPadding(dp(6), dp(6), dp(6), dp(6))
        val scroll = ScrollView(context)
        scroll.addView(resultView)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(120)))

        val footer = LinearLayout(context)
        footer.orientation = HORIZONTAL
        val insert = actionButton("Insert")
        insert.setOnClickListener {
            if (result.isNotBlank()) {
                onInsert(result)
                result = ""
                resultView.text = ""
                status.text = "Inserted."
            }
        }
        val discard = actionButton("Clear")
        discard.setOnClickListener {
            result = ""
            resultView.text = ""
            status.text = "Cleared."
        }
        footer.addView(insert, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        footer.addView(discard, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(footer)

        buildActions()
    }

    private fun buildActions() {
        val actions = listOf(
            AiAction.IMPROVE to "Improve", AiAction.SHORTER to "Shorter",
            AiAction.LONGER to "Longer", AiAction.PROFESSIONAL to "Pro",
            AiAction.CASUAL to "Casual", AiAction.FRIENDLY to "Friendly",
            AiAction.FORMAL to "Formal", AiAction.GRAMMAR to "Grammar",
            AiAction.TO_SINHALA to "EN\u2192SI", AiAction.TO_ENGLISH to "SI\u2192EN",
            AiAction.SUMMARIZE to "Summary", AiAction.REPLY to "Reply",
            AiAction.EMAIL to "Email", AiAction.CAPTION to "Caption"
        )
        actionsGrid.removeAllViews()
        actions.forEach { (action, label) ->
            val chip = actionButton(label)
            chip.setOnClickListener { onAction(action) }
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            lp.setMargins(dp(2), dp(2), dp(2), dp(2))
            actionsGrid.addView(chip, lp)
        }
    }

    private fun actionButton(label: String): TextView {
        val button = TextView(context)
        button.text = label
        button.textSize = 12f
        button.gravity = Gravity.CENTER
        button.setTextColor(theme.textColor)
        button.setBackgroundColor(theme.keyBackground)
        button.setPadding(dp(4), dp(8), dp(4), dp(8))
        button.isClickable = true
        return button
    }

    fun setBusy(busy: Boolean) {
        status.text = if (busy) "Thinking\u2026" else status.text
        actionsGrid.alpha = if (busy) 0.5f else 1f
        actionsGrid.isEnabled = !busy
    }

    fun showResult(text: String) {
        result = text
        resultView.text = text
        status.text = "Result ready \u2014 tap Insert to use it."
    }

    fun showError(message: String) {
        result = ""
        status.text = message
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
