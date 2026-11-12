package com.nova.keyboard.ime

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.inputmethodservice.InputMethodService
import com.nova.keyboard.ai.AiPrompts
import com.nova.keyboard.ai.AiResult
import com.nova.keyboard.core.ServiceLocator
import com.nova.keyboard.data.Snippet
import com.nova.keyboard.language.SinhalaTransliterator
import com.nova.keyboard.language.WijesekaraEngine
import com.nova.keyboard.theme.BuiltInThemes
import com.nova.keyboard.ui.MainActivity
import com.nova.keyboard.ui.keyboard.AiAction
import com.nova.keyboard.ui.keyboard.AiPanelView
import com.nova.keyboard.ui.keyboard.EmojiPanelView
import com.nova.keyboard.ui.keyboard.KeyCode
import com.nova.keyboard.ui.keyboard.KeySpec
import com.nova.keyboard.ui.keyboard.KeyboardLayouts
import com.nova.keyboard.ui.keyboard.KeyboardRootView
import com.nova.keyboard.ui.keyboard.ListPanelView
import com.nova.keyboard.ui.keyboard.PanelItem
import com.nova.keyboard.ui.keyboard.ToolbarAction
import kotlinx.coroutines.launch

/**
 * The keyboard's brain: turns key events into InputConnection edits and keeps
 * the visible layout, suggestions and mode state in sync.
 */
class KeyboardController(
    private val service: InputMethodService,
    private val host: InputSession.Host,
    private val root: KeyboardRootView
) {

    private enum class Page { LETTERS, NUMBERS, SYMBOLS, SYMBOLS2, DEV }

    private val session = InputSession(host)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var profile = EditorProfile()
    private var page = Page.LETTERS
    private var shift = 0
    private var wijeState = WijesekaraEngine.State()
    private var phoneticBuffer = ""
    private var lastWord = ""
    private var lastKeyWasSpace = false
    private var lastSpaceAt = 0L
    private var alternatesMode = false
    private var aiPanel: AiPanelView? = null
    private var currentResult = ""

    private val settings get() = ServiceLocator.settings
    private val prefs get() = ServiceLocator.settings.current

    init {
        root.onKey = { spec -> onKey(spec) }
        root.onLongPressKey = { spec -> showAlternates(spec) }
        root.onCandidate = { value -> onCandidate(value) }
        root.onToolbar = { action -> onToolbar(action) }
    }

    // ---------------------------------------------------------------- lifecycle

    fun onStartInput(info: android.view.inputmethod.EditorInfo?) {
        profile = EditorTypeDetector.detect(info)
        page = if (profile.isNumber) Page.NUMBERS else Page.LETTERS
        shift = 0
        wijeState = WijesekaraEngine.State()
        phoneticBuffer = ""
        lastWord = ""
        alternatesMode = false
        applyTheme()
        refresh()
        updateAutoShift()
        root.clearSuggestions()
        captureClipboard()
    }

    fun onFinishInput() {
        if (wijeState.composing.isNotEmpty()) {
            session.commit(wijeState.composing)
            wijeState = WijesekaraEngine.State()
        }
        if (phoneticBuffer.isNotEmpty()) {
            session.commit(SinhalaTransliterator.transliterate(phoneticBuffer))
            phoneticBuffer = ""
        }
        session.finishComposing()
    }

    // ------------------------------------------------------------------ render

    private fun applyTheme() {
        val s = prefs
        val theme = when {
            s.gamingMode && s.activeThemeId == BuiltInThemes.amoledCyber.id -> BuiltInThemes.gamingRgb
            else -> ServiceLocator.themes.active()
        }
        root.applyTheme(theme)
        root.setSizes(if (s.gamingMode) s.keyboardHeightScale * 1.08f else s.keyboardHeightScale, s.keySpacingDp)
        val constant = when (s.hapticStrength) {
            3 -> android.view.HapticFeedbackConstants.LONG_PRESS
            1 -> android.view.HapticFeedbackConstants.VIRTUAL_KEY
            else -> android.view.HapticFeedbackConstants.KEYBOARD_TAP
        }
        root.setFeedback(s.hapticEnabled, s.soundEnabled, constant)
    }

    private fun refresh() {
        val s = prefs
        val rows = when (page) {
            Page.NUMBERS -> KeyboardLayouts.numbers()
            Page.SYMBOLS -> KeyboardLayouts.symbols()
            Page.SYMBOLS2 -> KeyboardLayouts.symbols2()
            Page.DEV -> KeyboardLayouts.dev(s.devLanguage, s.numberRow)
            Page.LETTERS -> when {
                s.language == "si" && s.sinhalaInputMethod == "wijesekara" ->
                    KeyboardLayouts.sinhalaWijesekara(shift > 0, s.numberRow)
                s.language == "si" ->
                    KeyboardLayouts.sinhalaPhonetic(shift > 0, s.numberRow, profile.isEmail)
                else -> KeyboardLayouts.english(shift > 0, s.numberRow, profile.isEmail, profile.isUrl)
            }
        }
        val finalRows = if (s.devMode && page == Page.LETTERS) rows + listOf(KeyboardLayouts.devQuickRow()) else rows
        val languageLabel = if (s.language == "si") {
            if (s.sinhalaInputMethod == "wijesekara") "SI" else "SI-PH"
        } else "EN"
        val mode = buildString {
            if (s.gamingMode) append("GAME ")
            if (profile.isPassword) append("SECURE ")
            if (shift == 2) append("CAPS ")
            if (page != Page.LETTERS) append(page.name)
        }.trim()
        root.setLabels(languageLabel, mode)
        root.setLayout(finalRows)
    }

    // ------------------------------------------------------------------- input

    private fun onKey(spec: KeySpec) {
        when (spec.code) {
            KeyCode.SHIFT -> {
                shift = if (shift == 0) 1 else if (shift == 1) 2 else 0
                refresh()
            }
            KeyCode.BACKSPACE -> onBackspace()
            KeyCode.ENTER -> onEnter()
            KeyCode.SPACE -> onSpace()
            KeyCode.LANG -> toggleLanguage()
            KeyCode.EMOJI -> showEmojiPanel()
            KeyCode.PAGE_NUM -> { page = Page.NUMBERS; refresh() }
            KeyCode.PAGE_SYM -> { page = Page.SYMBOLS; refresh() }
            KeyCode.PAGE_SYM2 -> { page = Page.SYMBOLS2; refresh() }
            KeyCode.PAGE_DEV -> { page = Page.DEV; refresh() }
            KeyCode.PAGE_ABC -> { page = Page.LETTERS; refresh() }
            KeyCode.PANEL_CLIPBOARD -> showClipboardPanel()
            KeyCode.PANEL_SNIPPETS -> showSnippetPanel()
            KeyCode.PANEL_AI -> showAiPanel()
            KeyCode.SETTINGS -> openSettings()
            KeyCode.HIDE -> service.requestHideSelf(0)
            KeyCode.CURSOR_LEFT -> session.moveCursor(-1)
            KeyCode.CURSOR_RIGHT -> session.moveCursor(1)
            KeyCode.TAB -> session.commit("\t")
            KeyCode.CHAR -> onChar(spec.output)
            else -> Unit
        }
    }

    /**
     * Clipboard history is filled from the system clipboard while NOVA is shown.
     * Skipped for password fields, and it never reads anything in the background.
     */
    private fun captureClipboard() {
        val s = prefs
        if (!s.clipboardEnabled || profile.isSensitive) return
        val text = session.clipboardText().trim()
        if (text.isEmpty() || text.length > 20000) return
        ServiceLocator.scope.launch {
            runCatching {
                ServiceLocator.clipboard.add(text)
                ServiceLocator.clipboard.trim(s.clipboardLimit)
                ServiceLocator.clipboard.expireOlderThan(s.clipboardExpiryDays)
            }
        }
    }

    private fun onChar(value: String) {
        if (value.isEmpty()) return
        val s = prefs
        if (profile.isSensitive) {
            session.commit(value)
            return
        }
        when {
            s.language == "si" && s.sinhalaInputMethod == "wijesekara" -> {
                val out = WijesekaraEngine.input(wijeState, value)
                out.commit?.let { session.commit(it) }
                wijeState = out.state
                if (wijeState.composing.isEmpty()) session.finishComposing()
                else session.setComposing(wijeState.composing)
                root.clearSuggestions()
            }
            s.language == "si" -> {
                phoneticBuffer += value
                updatePhonetic()
            }
            else -> {
                val cased = if (shift > 0 && value.length == 1 && value[0].isLetter()) value.uppercase() else value
                val text = if (s.smartPunctuation) smartPunctuate(cased) else cased
                session.commit(text)
                if (shift == 1) { shift = 0 }
                updateAutoShift()
                updateSuggestions()
            }
        }
    }

    private fun onBackspace() {
        val s = prefs
        if (s.language == "si" && s.sinhalaInputMethod == "phonetic" && phoneticBuffer.isNotEmpty()) {
            phoneticBuffer = phoneticBuffer.dropLast(1)
            updatePhonetic()
            return
        }
        if (s.language == "si" && s.sinhalaInputMethod == "wijesekara" && wijeState.composing.isNotEmpty()) {
            val trimmed = wijeState.composing.dropLast(1)
            wijeState = WijesekaraEngine.State(trimmed, false)
            if (trimmed.isEmpty()) session.finishComposing() else session.setComposing(trimmed)
            return
        }
        session.deleteBackward()
        updateSuggestions()
    }

    private fun onEnter() {
        if (wijeState.composing.isNotEmpty()) {
            session.commit(wijeState.composing)
            wijeState = WijesekaraEngine.State()
        }
        if (phoneticBuffer.isNotEmpty()) commitPhonetic(SinhalaTransliterator.transliterate(phoneticBuffer))
        session.sendEnter(profile.imeAction)
    }

    private fun onSpace() {
        val s = prefs

        if (phoneticBuffer.isNotEmpty()) {
            commitPhonetic(SinhalaTransliterator.transliterate(phoneticBuffer))
            session.commit(" ")
            lastKeyWasSpace = false
            return
        }
        if (wijeState.composing.isNotEmpty()) {
            session.commit(wijeState.composing)
            wijeState = WijesekaraEngine.State()
            session.commit(" ")
            lastKeyWasSpace = false
            return
        }

        val word = currentWord()
        val token = session.textBeforeCursor(48).takeLastWhile { !it.isWhitespace() }

        // Macro expansion: text insertion only, never command execution.
        if (!profile.isSensitive && token.isNotEmpty()) {
            val expansion = runCatching { ServiceLocator.macros.expansionFor(token) }.getOrNull()
            if (expansion != null) {
                session.replaceBeforeCursor(token.length, expansion)
                return
            }
        }

        // Autocorrection of the word just finished.
        if (s.autoCorrect && s.language == "en" && !profile.isSensitive && word.length > 2) {
            val replacement = ServiceLocator.suggestions.correct(word, "en")
            if (replacement != null && !replacement.equals(word, ignoreCase = true)) {
                session.replaceBeforeCursor(word.length, matchCase(word, replacement))
            }
        }

        val now = System.currentTimeMillis()
        if (s.doubleSpacePeriod && lastKeyWasSpace && now - lastSpaceAt < 700) {
            session.deleteBackward(1)
            session.commit(". ")
            lastKeyWasSpace = false
            return
        }

        session.commit(" ")
        lastKeyWasSpace = true
        lastSpaceAt = now

        if (s.learningEnabled && !profile.isSensitive && word.length > 1) {
            ServiceLocator.scope.launch {
                ServiceLocator.suggestions.learn(word, s.language, lastWord)
            }
            lastWord = word
        }
        updateSuggestions()
    }

    /**
     * Smart punctuation: straight quotes become typographic quotes, and three
     * dots collapse into an ellipsis. Purely local, no rewriting of the user's words.
     */
    private fun smartPunctuate(value: String): String {
        val before = session.textBeforeCursor(1)
        return when (value) {
            "\"" -> if (before.isBlank() || before.endsWith("(") || before.endsWith("\n")) "\u201C" else "\u201D"
            "'" -> if (before.isNotBlank() && before.last().isLetterOrDigit()) "\u2019" else value
            "." -> {
                if (session.textBeforeCursor(2) == "..") {
                    session.deleteBackward(2)
                    "\u2026"
                } else {
                    value
                }
            }
            else -> value
        }
    }

    /** Auto-capitalisation: arm shift at the start of a sentence. */
    private fun updateAutoShift() {
        val s = prefs
        if (!s.autoCapitalize || profile.isSensitive || shift == 2) return
        val before = session.textBeforeCursor(3)
        val sentenceStart = before.isEmpty() || before.endsWith(". ") || before.endsWith("! ") ||
            before.endsWith("? ") || before.endsWith("\n")
        if (sentenceStart && shift == 0) {
            shift = 1
            refresh()
        } else if (!sentenceStart && shift == 1) {
            shift = 0
            refresh()
        }
    }

    private fun onCandidate(value: String) {
        if (alternatesMode) {
            alternatesMode = false
            session.commit(value)
            updateSuggestions()
            return
        }
        if (phoneticBuffer.isNotEmpty()) {
            commitPhonetic(value)
            return
        }
        val word = currentWord()
        if (word.isNotEmpty()) session.replaceBeforeCursor(word.length, value + " ")
        else session.commit(value + " ")
        updateSuggestions()
    }

    private fun commitPhonetic(sinhala: String) {
        session.commit(sinhala)
        session.commit(" ")
        phoneticBuffer = ""
        session.finishComposing()
        root.clearSuggestions()
    }

    private fun updatePhonetic() {
        if (phoneticBuffer.isEmpty()) {
            session.finishComposing()
            root.clearSuggestions()
            return
        }
        val converted = SinhalaTransliterator.transliterate(phoneticBuffer)
        session.setComposing(converted)
        val candidates = SinhalaTransliterator.candidates(
            phoneticBuffer, ServiceLocator.suggestions.dictionary("si")
        )
        root.showSuggestions(candidates, phoneticBuffer)
    }

    private fun toggleLanguage() {
        val next = if (prefs.language == "si") "en" else "si"
        settings.update { it.copy(language = next) }
        wijeState = WijesekaraEngine.State()
        phoneticBuffer = ""
        session.finishComposing()
        page = Page.LETTERS
        shift = 0
        applyTheme()
        refresh()
        root.clearSuggestions()
    }

    // -------------------------------------------------------------- suggestions

    private fun currentWord(): String =
        session.textBeforeCursor(48).takeLastWhile { it.isLetter() || it == '\'' || it == '\u2019' }

    private fun matchCase(source: String, replacement: String): String = when {
        source.isEmpty() -> replacement
        source[0].isUpperCase() -> replacement.replaceFirstChar { it.uppercase() }
        else -> replacement
    }

    private fun updateSuggestions() {
        val s = prefs
        if (profile.isSensitive || !s.predictionEnabled) {
            root.clearSuggestions()
            return
        }
        val word = currentWord()
        if (word.isEmpty() && s.gamingMode) {
            root.showSuggestions(KeyboardLayouts.gamingPhrases().take(6))
            return
        }
        if (word.isEmpty()) {
            if (s.nextWordPrediction && lastWord.isNotEmpty()) {
                root.showSuggestions(ServiceLocator.suggestions.nextWords(lastWord, s.language, 3))
            } else {
                root.clearSuggestions()
            }
        } else {
            root.showSuggestions(ServiceLocator.suggestions.suggestions(word, s.language, 3))
        }
    }

    private fun showAlternates(spec: KeySpec) {
        val alternates = spec.alternates
        if (alternates.isEmpty()) return
        alternatesMode = true
        root.showSuggestions(alternates)
    }

    // ------------------------------------------------------------------ toolbar

    private fun onToolbar(action: ToolbarAction) {
        when (action) {
            ToolbarAction.SETTINGS -> openSettings()
            ToolbarAction.AI -> showAiPanel()
            ToolbarAction.CLIPBOARD -> showClipboardPanel()
            ToolbarAction.SNIPPETS -> showSnippetPanel()
            ToolbarAction.EMOJI -> showEmojiPanel()
            ToolbarAction.GAMING -> {
                settings.update { it.copy(gamingMode = !it.gamingMode) }
                applyTheme()
                refresh()
            }
            ToolbarAction.DEV -> {
                page = if (page == Page.DEV) Page.LETTERS else Page.DEV
                refresh()
            }
            ToolbarAction.ONE_HANDED -> {
                settings.update { it.copy(oneHanded = (it.oneHanded + 1) % 3) }
                applyOneHanded()
            }
            ToolbarAction.HIDE -> service.requestHideSelf(0)
            ToolbarAction.CURSOR_LEFT -> session.moveCursor(-1)
            ToolbarAction.CURSOR_RIGHT -> session.moveCursor(1)
        }
    }

    private fun applyOneHanded() {
        val mode = prefs.oneHanded
        val width = root.width
        if (width <= 0) return
        val target = when (mode) {
            1 -> (width * 0.72f).toInt()
            2 -> (width * 0.72f).toInt()
            else -> width
        }
        val lp = root.layoutParams
        if (lp != null) {
            lp.width = target
            root.layoutParams = lp
        }
        root.setPadding(
            if (mode == 2) width - target else 0, 0,
            if (mode == 1) width - target else 0, 0
        )
    }

    private fun openSettings() {
        runCatching {
            service.startActivity(
                Intent(service, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // ------------------------------------------------------------------- panels

    private fun showEmojiPanel() {
        val theme = ServiceLocator.themes.active()
        val panel = EmojiPanelView(
            service, theme,
            onPick = { emoji -> session.commit(emoji) },
            onClose = { root.hidePanel() }
        )
        root.showPanel(panel)
    }

    private fun showClipboardPanel() {
        if (!prefs.clipboardEnabled) {
            openSettings(); return
        }
        captureClipboard()
        val theme = ServiceLocator.themes.active()
        val panel = ListPanelView(service, theme, "Clipboard", { root.hidePanel() })
        panel.onPick = { item -> session.commit(item.payload) }
        panel.onDelete = { item ->
            ServiceLocator.clipboard.delete(item.id)
            showClipboardPanel()
        }
        panel.setItems(
            ServiceLocator.clipboard.list(limit = 60).map {
                PanelItem(
                    primary = it.text.take(80).replace("\n", " "),
                    secondary = android.text.format.DateUtils.getRelativeTimeSpanString(it.createdAt).toString(),
                    payload = it.text,
                    id = it.id
                )
            }
        )
        root.showPanel(panel)
    }

    private fun showSnippetPanel() {
        val theme = ServiceLocator.themes.active()
        val panel = ListPanelView(service, theme, "Snippets", { root.hidePanel() })
        panel.onPick = { item -> session.commit(item.payload) }
        panel.setItems(
            ServiceLocator.snippets.list().map {
                PanelItem(primary = it.title, secondary = it.language, payload = it.body, id = it.id)
            }
        )
        root.showPanel(panel)
    }

    private fun showAiPanel() {
        if (profile.isSensitive) return
        val theme = ServiceLocator.themes.active()
        val panel = AiPanelView(
            service, theme,
            onAction = { action -> runAi(action) },
            onInsert = { text -> session.replaceSelection(text) },
            onClose = { root.hidePanel(); aiPanel = null }
        )
        aiPanel = panel
        root.showPanel(panel)
        if (!prefs.aiEnabled || !ServiceLocator.ai.hasKey()) {
            panel.showError("AI is off. Open NOVA settings \u2192 AI Studio to add a provider key.")
        }
    }

    private fun runAi(action: AiAction) {
        val panel = aiPanel ?: return
        if (!prefs.aiEnabled || !ServiceLocator.ai.hasKey()) {
            panel.showError("AI is off. Add a provider key in AI Studio first.")
            return
        }
        val selected = session.selectedText()
        val source = selected.ifBlank { session.textBeforeCursor(800).trim() }
        if (source.isBlank()) {
            panel.showError("Place the cursor after some text, or select text first.")
            return
        }
        val (system, user) = when (action) {
            AiAction.IMPROVE -> AiPrompts.rewrite("improve", source)
            AiAction.SHORTER -> AiPrompts.rewrite("shorter", source)
            AiAction.LONGER -> AiPrompts.rewrite("longer", source)
            AiAction.PROFESSIONAL -> AiPrompts.rewrite("professional", source)
            AiAction.CASUAL -> AiPrompts.rewrite("casual", source)
            AiAction.FRIENDLY -> AiPrompts.rewrite("friendly", source)
            AiAction.FORMAL -> AiPrompts.rewrite("formal", source)
            AiAction.GRAMMAR -> AiPrompts.grammar(source)
            AiAction.TO_SINHALA -> AiPrompts.translate(source, "the source language", "Sinhala")
            AiAction.TO_ENGLISH -> AiPrompts.translate(source, "Sinhala", "English")
            AiAction.SUMMARIZE -> AiPrompts.summarize(source)
            AiAction.REPLY -> AiPrompts.reply(source)
            AiAction.EMAIL -> AiPrompts.generate("email", source)
            AiAction.CAPTION -> AiPrompts.generate("caption", source)
        }
        panel.setBusy(true)
        ServiceLocator.scope.launch {
            val result = ServiceLocator.ai.complete(system, user)
            mainHandler.post {
                when (result) {
                    is AiResult.Success -> {
                        currentResult = result.text
                        panel.showResult(result.text)
                    }
                    is AiResult.Failure -> panel.showError(result.message)
                }
                panel.setBusy(false)
            }
        }
    }
}
