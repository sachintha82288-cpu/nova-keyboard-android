package com.nova.keyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

/**
 * The only place that talks to the host app's InputConnection.
 * Everything is defensive: editors are allowed to reject any edit.
 */
class InputSession(private val host: Host) {

    interface Host {
        val inputConnection: InputConnection?
        val inputMethodManager: InputMethodManager?
        fun appContext(): android.content.Context
        fun finishInputSafely()
    }

    private val ic: InputConnection? get() = host.inputConnection

    fun commit(text: String) {
        if (text.isEmpty()) return
        val connection = ic ?: return
        runCatching { connection.commitText(text, 1) }
    }

    fun setComposing(text: String) {
        val connection = ic ?: return
        runCatching {
            if (text.isEmpty()) connection.finishComposingText()
            else connection.setComposingText(text, 1)
        }
    }

    fun finishComposing() {
        runCatching { ic?.finishComposingText() }
    }

    fun deleteBackward(count: Int = 1) {
        val connection = ic ?: return
        runCatching {
            val selected = connection.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                connection.commitText("", 1)
                return
            }
            // Look at one extra character so an emoji (surrogate pair) is deleted
            // whole instead of leaving half of it behind.
            val window = connection.getTextBeforeCursor(count + 1, 0)?.toString().orEmpty()
            val before = window.takeLast(TextEditing.backspaceLength(window, count))
            if (before.isNotEmpty()) {
                connection.deleteSurroundingText(before.length, 0)
            } else {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
    }

    fun textBeforeCursor(maxChars: Int): String =
        runCatching { ic?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty() }.getOrDefault("")

    fun textAfterCursor(maxChars: Int): String =
        runCatching { ic?.getTextAfterCursor(maxChars, 0)?.toString().orEmpty() }.getOrDefault("")

    fun selectedText(): String =
        runCatching { ic?.getSelectedText(32)?.toString().orEmpty() }.getOrDefault("")

    fun replaceBeforeCursor(charCount: Int, replacement: String) {
        val connection = ic ?: return
        runCatching {
            connection.deleteSurroundingText(charCount, 0)
            connection.commitText(replacement, 1)
        }
    }

    fun replaceSelection(replacement: String) {
        val connection = ic ?: return
        runCatching {
            val selected = connection.getSelectedText(0)
            if (!selected.isNullOrEmpty()) connection.commitText(replacement, 1)
            else connection.commitText(replacement, 1)
        }
    }

    fun sendEnter(imeAction: Int) {
        val connection = ic ?: return
        val handled = when (imeAction) {
            EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_PREVIOUS ->
                runCatching { connection.performEditorAction(imeAction) }.getOrDefault(false)
            else -> false
        }
        if (!handled) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    fun selectAll() {
        val connection = ic ?: return
        runCatching {
            val before = connection.getTextBeforeCursor(10000, 0)?.length ?: 0
            val after = connection.getTextAfterCursor(10000, 0)?.length ?: 0
            connection.setSelection(before, before + after)
        }
    }

    fun moveCursor(delta: Int) {
        val connection = ic ?: return
        runCatching {
            val before = connection.getTextBeforeCursor(10000, 0)?.length ?: return
            val after = connection.getTextAfterCursor(10000, 0)?.length ?: 0
            val total = before + after
            val target = (before + delta).coerceIn(0, total)
            connection.setSelection(target, target)
        }
    }

    fun clipboardText(): String = runCatching {
        val manager = host.appContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager
        manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(host.appContext())
            ?.toString().orEmpty()
    }.getOrDefault("")

    fun copySelection(): String {
        val connection = ic ?: return ""
        return runCatching {
            val selected = connection.getSelectedText(0)?.toString().orEmpty()
            if (selected.isNotEmpty()) {
                val manager = host.appContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as? android.content.ClipboardManager
                manager?.setPrimaryClip(android.content.ClipData.newPlainText("NOVA", selected))
            }
            selected
        }.getOrDefault("")
    }

    fun cutSelection() {
        val selected = copySelection()
        if (selected.isNotEmpty()) runCatching { ic?.commitText("", 1) }
    }
}
