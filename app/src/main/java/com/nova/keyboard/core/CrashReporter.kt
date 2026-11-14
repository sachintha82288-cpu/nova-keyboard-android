package com.nova.keyboard.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records the last uncaught exception (and any non-fatal failure we catch) to
 * app-private storage so it can be read from the About screen and sent back.
 *
 * This exists because a keyboard crash is otherwise invisible without adb: the
 * user sees "NOVA KEYBOARD has stopped" and nothing else.
 */
object CrashReporter {

    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            record(app, throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun record(context: Context, throwable: Throwable) {
        runCatching {
            val header = buildString {
                append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                append("\n")
                append(android.os.Build.MANUFACTURER).append(' ')
                append(android.os.Build.MODEL)
                append("  Android ").append(android.os.Build.VERSION.RELEASE)
                append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n\n")
                append(android.util.Log.getStackTraceString(throwable))
            }
            File(context.filesDir, FILE).writeText(header)
        }
    }

    fun lastCrash(context: Context): String? = runCatching {
        val file = File(context.filesDir, FILE)
        if (file.exists()) file.readText() else null
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }
}
