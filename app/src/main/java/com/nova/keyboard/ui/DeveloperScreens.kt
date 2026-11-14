package com.nova.keyboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.keyboard.core.ServiceLocator
import com.nova.keyboard.data.Macro
import com.nova.keyboard.data.Snippet

private val DEV_LANGUAGES = listOf(
    "kotlin", "java", "javascript", "typescript", "python", "html", "css",
    "php", "c", "cpp", "csharp", "sql", "json", "xml", "bash"
)

@Composable
fun DeveloperScreen() {
    val settings by ServiceLocator.settings.state.collectAsState()
    val repo = ServiceLocator.settings
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var showSnippetDialog by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var showMacroDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val snippets = rememberDbValue(refresh, emptyList<Snippet>()) { ServiceLocator.snippets.list() }
    val macros = rememberDbValue(refresh, emptyList<Macro>()) { ServiceLocator.macros.list() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SettingSwitch(
                "Developer mode",
                "Programming symbol page and language keyword rows on the keyboard",
                settings.devMode
            ) { repo.update { s -> s.copy(devMode = it) } }
        }
        item { SectionHeader("Programming layout") }
        item {
            Column(Modifier.padding(horizontal = 12.dp)) {
                DEV_LANGUAGES.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { language ->
                            val active = language == settings.devLanguage
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else Color(0xFF0C0F14),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { repo.update { s -> s.copy(devLanguage = language) } }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                Text(
                                    language,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            InfoCard(
                "Programming symbols",
                "{ } [ ] ( ) < >  =  ==  !=  +  -  *  /  %  &&  ||  !  ->  =>  ;  :  ,  .  #  $  @  _  \\  |  `  ~",
                accent = true
            )
        }

        item { SectionHeader("Snippets (" + snippets.size + ")") }
        item {
            Row(Modifier.padding(horizontal = 12.dp)) {
                Button(
                    onClick = { editingSnippet = null; showSnippetDialog = true },
                    modifier = Modifier.weight(1f).padding(3.dp)
                ) { Text("New", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = {
                        val payload = ServiceLocator.snippets.exportAll()
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(ClipData.newPlainText("NOVA snippets", payload))
                        status = "Exported " + snippets.size + " snippets to the clipboard."
                    },
                    modifier = Modifier.weight(1f).padding(3.dp)
                ) { Text("Export", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = {
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = manager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        val added = ServiceLocator.snippets.importAll(text)
                        status = if (added > 0) "Imported " + added + " snippets." else "Nothing importable found."
                        refresh++
                    },
                    modifier = Modifier.weight(1f).padding(3.dp)
                ) { Text("Import", fontSize = 12.sp) }
            }
        }
        item { if (status.isNotEmpty()) InfoCard("Status", status) }
        items(snippets.size) { index ->
            val snippet = snippets[index]
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Color(0xFF0C0F14), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row {
                    Text(
                        snippet.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(snippet.language, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    snippet.body.take(120),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    TextButton(onClick = { editingSnippet = snippet; showSnippetDialog = true }) {
                        Text("Edit", fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        ServiceLocator.snippets.setPinned(snippet.id, !snippet.pinned); refresh++
                    }) { Text(if (snippet.pinned) "Unpin" else "Pin", fontSize = 11.sp) }
                    TextButton(onClick = {
                        ServiceLocator.snippets.delete(snippet.id); refresh++
                    }) { Text("Delete", fontSize = 11.sp) }
                }
            }
        }

        item { SectionHeader("Macros (" + macros.size + ")") }
        item {
            Row(Modifier.padding(horizontal = 12.dp)) {
                Button(
                    onClick = { showMacroDialog = true },
                    modifier = Modifier.weight(1f).padding(3.dp)
                ) { Text("New macro", fontSize = 12.sp) }
            }
        }
        item {
            InfoCard(
                "How macros work",
                "Type the shortcut and press space. NOVA inserts the expansion as plain text \u2014 it never runs " +
                    "commands or background actions."
            )
        }
        items(macros.size) { index ->
            val macro = macros[index]
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .background(Color(0xFF0C0F14), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(macro.shortcut, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    Text(macro.expansion, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    ServiceLocator.macros.delete(macro.id); refresh++
                }) { Text("Delete", fontSize = 11.sp) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showSnippetDialog) {
        var title by remember { mutableStateOf(editingSnippet?.title ?: "") }
        var body by remember { mutableStateOf(editingSnippet?.body ?: "") }
        var language by remember { mutableStateOf(editingSnippet?.language ?: "general") }
        AlertDialog(
            onDismissRequest = { showSnippetDialog = false },
            title = { Text(if (editingSnippet == null) "New snippet" else "Edit snippet") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body") }, minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        ServiceLocator.snippets.save(
                            Snippet(editingSnippet?.id ?: 0L, title.trim(), body, language.trim().ifBlank { "general" }, editingSnippet?.pinned ?: false)
                        )
                        refresh++
                    }
                    showSnippetDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSnippetDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMacroDialog) {
        var shortcut by remember { mutableStateOf(";") }
        var expansion by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMacroDialog = false },
            title = { Text("New macro") },
            text = {
                Column {
                    OutlinedTextField(value = shortcut, onValueChange = { shortcut = it }, label = { Text("Shortcut") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = expansion, onValueChange = { expansion = it }, label = { Text("Expansion") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (shortcut.isNotBlank() && expansion.isNotBlank()) {
                        val saved = ServiceLocator.macros.save(Macro(0, shortcut.trim(), expansion, true))
                        status = if (saved == -1L) {
                            "That shortcut already exists \u2014 delete the old macro first."
                        } else {
                            "Macro saved."
                        }
                        refresh++
                    }
                    showMacroDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showMacroDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ClipboardScreen() {
    val settings by ServiceLocator.settings.state.collectAsState()
    val repo = ServiceLocator.settings
    var refresh by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val items = rememberDbValue(refresh, emptyList()) { ServiceLocator.clipboard.list(query, 200) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; refresh++ },
                label = { Text("Search clipboard") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )
        }
        item {
            SettingSwitch("Clipboard history", "Capture text you copy while NOVA is active", settings.clipboardEnabled) {
                repo.update { s -> s.copy(clipboardEnabled = it) }
            }
        }
        item {
            SettingSlider("History limit", settings.clipboardLimit.toFloat(), 20f..300f, 13, { it.toInt().toString() }) {
                repo.update { s -> s.copy(clipboardLimit = it.toInt()) }
            }
        }
        item {
            SettingSlider("Expire after (days)", settings.clipboardExpiryDays.toFloat(), 1f..90f, 88, {
                it.toInt().toString() + " d"
            }) { repo.update { s -> s.copy(clipboardExpiryDays = it.toInt()) } }
        }
        item {
            Row(Modifier.padding(horizontal = 12.dp)) {
                OutlinedButton(
                    onClick = {
                        ServiceLocator.clipboard.trim(settings.clipboardLimit)
                        ServiceLocator.clipboard.expireOlderThan(settings.clipboardExpiryDays)
                        refresh++
                    },
                    modifier = Modifier.weight(1f).padding(3.dp)
                ) { Text("Apply limits", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { ServiceLocator.clipboard.clear(keepPinned = false); refresh++ },
                    modifier = Modifier.weight(1f).padding(3.dp)
                ) { Text("Clear all", fontSize = 12.sp) }
            }
        }
        item { SectionHeader("History (" + items.size + ")") }
        items(items.size) { index ->
            val item = items[index]
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .background(Color(0xFF0C0F14), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(item.text.take(160), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Row {
                    TextButton(onClick = {
                        ServiceLocator.clipboard.setPinned(item.id, !item.pinned); refresh++
                    }) { Text(if (item.pinned) "Unpin" else "Pin", fontSize = 11.sp) }
                    TextButton(onClick = {
                        ServiceLocator.clipboard.delete(item.id); refresh++
                    }) { Text("Delete", fontSize = 11.sp) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
