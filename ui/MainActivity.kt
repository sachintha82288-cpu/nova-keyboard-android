package com.nova.keyboard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nova.keyboard.core.CrashReporter
import com.nova.keyboard.core.ServiceLocator

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)
        runCatching { ServiceLocator.init(this) }
            .onFailure { CrashReporter.record(this, it) }
        try {
        setContent {
            val settings by ServiceLocator.settings.state.collectAsState()
            val theme = ServiceLocator.themes.active()
            val scheme = darkColorScheme(
                primary = Color(theme.accent),
                onPrimary = Color(0xFF04121A),
                secondary = Color(theme.accent),
                background = Color(0xFF05060A),
                onBackground = Color(0xFFE9FBFF),
                surface = Color(0xFF0C0F14),
                onSurface = Color(0xFFE9FBFF),
                surfaceVariant = Color(0xFF151A22),
                onSurfaceVariant = Color(0xFF9FB3BF)
            )
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
                    NovaApp(startOnboarding = !settings.onboardingDone)
                }
            }
        }
        } catch (t: Throwable) {
            CrashReporter.record(this, t)
            val label = android.widget.TextView(this).apply {
                text = "NOVA could not open its settings.\n\n" +
                    t.javaClass.simpleName + ": " + (t.message ?: "") +
                    "\n\nThis log is saved; open About after a restart to share it."
                setTextColor(0xFFFF6B6B.toInt())
                textSize = 14f
                setPadding(40, 80, 40, 40)
            }
            setContentView(label)
        }
    }
}
