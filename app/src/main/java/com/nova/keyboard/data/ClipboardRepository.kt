package com.nova.keyboard.data

import android.content.ContentValues
import java.util.concurrent.TimeUnit

class ClipboardRepository(private val db: NovaDatabase) {

    fun add(text: String, category: String = "general"): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return -1
        // Deduplicate: an identical entry is bumped instead of stored twice.
        db.delete("clipboard", "text = ? AND pinned = 0", trimmed)
        val values = ContentValues().apply {
            put("text", trimmed)
            put("created_at", System.currentTimeMillis())
            put("pinned", 0)
            put("category", category)
        }
        return db.insert("clipboard", values)
    }

    fun list(query: String = "", limit: Int = 200): List<ClipItem> {
        val cursor = if (query.isBlank()) {
            db.cursor("SELECT id, text, created_at, pinned, category FROM clipboard " +
                "ORDER BY pinned DESC, created_at DESC LIMIT ?", limit.toString())
        } else {
            db.cursor(
                "SELECT id, text, created_at, pinned, category FROM clipboard " +
                    "WHERE text LIKE ? ORDER BY pinned DESC, created_at DESC LIMIT ?",
                "%" + query + "%", limit.toString()
            )
        }
        val out = ArrayList<ClipItem>()
        cursor.use {
            while (it.moveToNext()) {
                out.add(
                    ClipItem(
                        it.getLong(0), it.getString(1), it.getLong(2),
                        it.getInt(3) == 1, it.getString(4)
                    )
                )
            }
        }
        return out
    }

    fun setPinned(id: Long, pinned: Boolean) {
        val values = ContentValues().apply { put("pinned", if (pinned) 1 else 0) }
        db.update("clipboard", values, "id = ?", id.toString())
    }

    fun delete(id: Long) = db.delete("clipboard", "id = ?", id.toString())

    fun clear(keepPinned: Boolean = true) {
        if (keepPinned) db.delete("clipboard", "pinned = 0") else db.delete("clipboard", null)
    }

    fun trim(limit: Int) {
        db.exec(
            "DELETE FROM clipboard WHERE pinned = 0 AND id NOT IN " +
                "(SELECT id FROM clipboard WHERE pinned = 0 ORDER BY created_at DESC LIMIT ?)",
            limit
        )
    }

    fun expireOlderThan(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        db.delete("clipboard", "pinned = 0 AND created_at < ?", cutoff.toString())
    }

    fun count(): Int = db.cursor("SELECT COUNT(*) FROM clipboard").use { if (it.moveToFirst()) it.getInt(0) else 0 }
}
