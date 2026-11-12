package com.nova.keyboard.core

import android.app.Application

class NovaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
