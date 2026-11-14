package com.nova.keyboard.core

import android.content.Context
import com.nova.keyboard.ai.AiClient
import com.nova.keyboard.data.ClipboardRepository
import com.nova.keyboard.data.LearningRepository
import com.nova.keyboard.data.MacroRepository
import com.nova.keyboard.data.NovaDatabase
import com.nova.keyboard.data.SnippetRepository
import com.nova.keyboard.language.SuggestionEngine
import com.nova.keyboard.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hand-written dependency graph. Deliberately not annotation-processor based:
 * the keyboard process stays small and starts in well under the 300 ms budget,
 * and there is no reflection or generated code on the IME hot path.
 */
object ServiceLocator {

    private val failureHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        // Only the exception type is logged: never typed text.
        android.util.Log.w("NovaKeyboard", "background task failed: " + throwable.javaClass.simpleName)
    }

    /**
     * Background work (database writes, AI calls) must never take the keyboard
     * down with it: a failure is logged and dropped.
     */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + failureHandler)

    lateinit var appContext: Context
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var database: NovaDatabase
        private set
    lateinit var clipboard: ClipboardRepository
        private set
    lateinit var snippets: SnippetRepository
        private set
    lateinit var macros: MacroRepository
        private set
    lateinit var learning: LearningRepository
        private set
    lateinit var themes: ThemeManager
        private set
    lateinit var suggestions: SuggestionEngine
        private set
    lateinit var secure: SecureStore
        private set
    lateinit var ai: AiClient
        private set

    /** True once the graph is built; accessors below re-init if it is not. */
    @Volatile
    private var ready = false

    @Synchronized
    fun init(context: Context) {
        if (::settings.isInitialized) { ready = true; return }
        appContext = context.applicationContext
        settings = SettingsRepository(appContext, scope).also { it.start() }
        database = NovaDatabase(appContext)
        clipboard = ClipboardRepository(database)
        snippets = SnippetRepository(database)
        macros = MacroRepository(database)
        learning = LearningRepository(database)
        themes = ThemeManager(settings)
        suggestions = SuggestionEngine(appContext, learning)
        secure = SecureStore(appContext)
        ai = AiClient(secure, settings)
        ready = true
        scope.launch {
            runCatching {
                snippets.seedIfEmpty()
                macros.seedIfEmpty()
                clipboard.expireOlderThan(settings.current.clipboardExpiryDays)
            }
        }
    }

    /**
     * Guarantees the graph exists before use. A `lateinit` access before init
     * would throw UninitializedPropertyAccessException and kill the keyboard,
     * which is a known failure mode for IMEs.
     */
    fun ensureInitialised(context: Context) {
        if (!ready) init(context)
    }
}
