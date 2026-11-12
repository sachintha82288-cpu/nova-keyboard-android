package com.nova.keyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads a database-backed value off the main thread and refreshes on key change. */
@Composable
fun <T> rememberDbValue(key: Any?, initial: T, loader: suspend () -> T): T {
    var value by remember(key) { mutableStateOf(initial) }
    LaunchedEffect(key) {
        value = withContext(Dispatchers.IO) { runCatching { loader() }.getOrDefault(initial) }
    }
    return value
}
