package com.nova.keyboard.core

import android.app.Application

class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        // A failure while building the dependency graph must not stop the app
        // from starting; ServiceLocator retries on first real use.
        runCatching { ServiceLocator.init(this) }
            .onFailure { CrashReporter.record(this, it) }
    }
}
