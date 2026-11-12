package com.nova.keyboard.data

import android.content.ContentValues

class SnippetRepository(private val db: NovaDatabase) {

    fun list(query: String = ""): List<Snippet> {
        val cursor = if (query.isBlank()) {
            db.cursor("SELECT id, title, body, language, pinned FROM snippets ORDER BY pinned DESC, title ASC")
        } else {
            db.cursor(
                "SELECT id, title, body, language, pinned FROM snippets " +
                    "WHERE title LIKE ? OR body LIKE ? OR language LIKE ? ORDER BY pinned DESC, title ASC",
                "%" + query + "%", "%" + query + "%", "%" + query + "%"
            )
        }
        val out = ArrayList<Snippet>()
        cursor.use {
            while (it.moveToNext()) {
                out.add(Snippet(it.getLong(0), it.getString(1), it.getString(2), it.getString(3), it.getInt(4) == 1))
            }
        }
        return out
    }

    fun save(snippet: Snippet): Long {
        val values = ContentValues().apply {
            put("title", snippet.title)
            put("body", snippet.body)
            put("language", snippet.language)
            put("pinned", if (snippet.pinned) 1 else 0)
        }
        return if (snippet.id == 0L) {
            db.insert("snippets", values)
        } else {
            db.update("snippets", values, "id = ?", snippet.id.toString()); snippet.id
        }
    }

    fun delete(id: Long) = db.delete("snippets", "id = ?", id.toString())

    fun setPinned(id: Long, pinned: Boolean) {
        val values = ContentValues().apply { put("pinned", if (pinned) 1 else 0) }
        db.update("snippets", values, "id = ?", id.toString())
    }

    fun exportAll(): String =
        list().joinToString("\n---\n") { s ->
            "#" + s.language + "|" + s.title + "\n" + s.body
        }

    fun importAll(payload: String): Int {
        var added = 0
        payload.split("\n---\n").forEach { block ->
            val trimmed = block.trim()
            if (!trimmed.startsWith("#")) return@forEach
            val headerEnd = trimmed.indexOf('\n')
            if (headerEnd < 0) return@forEach
            val header = trimmed.substring(1, headerEnd)
            val body = trimmed.substring(headerEnd + 1).trim()
            if (body.isEmpty()) return@forEach
            val language = header.substringBefore('|').ifBlank { "general" }
            val title = header.substringAfter('|', language).ifBlank { "Imported snippet" }
            save(Snippet(0, title, body, language, false))
            added++
        }
        return added
    }

    fun seedIfEmpty() {
        if (count() > 0) return
        DefaultSnippets.all.forEach { save(it) }
    }

    fun count(): Int = db.cursor("SELECT COUNT(*) FROM snippets").use { if (it.moveToFirst()) it.getInt(0) else 0 }
}
