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

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    @Synchronized
    fun init(context: Context) {
        if (::settings.isInitialized) return
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
        scope.launch {
            snippets.seedIfEmpty()
            macros.seedIfEmpty()
            clipboard.expireOlderThan(settings.current.clipboardExpiryDays)
        }
    }
}
