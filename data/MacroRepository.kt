package com.nova.keyboard.data

import android.content.ContentValues

class MacroRepository(private val db: NovaDatabase) {

    fun list(): List<Macro> {
        cache = null
        val out = ArrayList<Macro>()
        db.cursor("SELECT id, shortcut, expansion, enabled FROM macros ORDER BY shortcut ASC").use {
            while (it.moveToNext()) {
                out.add(Macro(it.getLong(0), it.getString(1), it.getString(2), it.getInt(3) == 1))
            }
        }
        return out
    }

    fun save(macro: Macro): Long {
        val values = ContentValues().apply {
            put("shortcut", macro.shortcut)
            put("expansion", macro.expansion)
            put("enabled", if (macro.enabled) 1 else 0)
        }
        cache = null
        return if (macro.id == 0L) {
            db.insert("macros", values)
        } else {
            db.update("macros", values, "id = ?", macro.id.toString()); macro.id
        }
    }

    fun delete(id: Long) {
        cache = null
        db.delete("macros", "id = ?", id.toString())
    }

    @Volatile
    private var cache: Map<String, String>? = null

    private fun snapshot(): Map<String, String> {
        cache?.let { return it }
        val map = HashMap<String, String>()
        db.cursor("SELECT shortcut, expansion FROM macros WHERE enabled = 1").use {
            while (it.moveToNext()) map[it.getString(0)] = it.getString(1)
        }
        cache = map
        return map
    }

    /**
     * Returns the expansion for an exact shortcut, or null. Text insertion only.
     * Served from an in-memory snapshot so a key press never waits on SQLite.
     */
    fun expansionFor(shortcut: String): String? = snapshot()[shortcut]

    fun seedIfEmpty() {
        if (db.cursor("SELECT COUNT(*) FROM macros").use { it.moveToFirst() && it.getInt(0) > 0 }) return
        save(Macro(0, ";email", "user@example.com", true))
        save(Macro(0, ";sig", "Regards,\nSachintha", true))
        save(Macro(0, ";addr", "Colombo, Sri Lanka", true))
        save(Macro(0, ";brb", "Be right back", true))
    }
}
