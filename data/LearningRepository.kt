package com.nova.keyboard.data

import android.content.ContentValues

/** On-device typing learning. Nothing here ever leaves the device. */
class LearningRepository(private val db: NovaDatabase) {

    fun recordWord(word: String, language: String) {
        val clean = word.trim().lowercase()
        if (clean.length < 2) return
        // UPDATE-then-INSERT instead of "ON CONFLICT ... DO UPDATE": upsert needs
        // SQLite 3.24 (Android 11+), and this app supports Android 10.
        val updated = db.executeUpdate(
            "UPDATE user_words SET freq = freq + 1 WHERE word = ? AND language = ?",
            clean, language
        )
        if (updated == 0) {
            db.insert("user_words", ContentValues().apply {
                put("word", clean)
                put("freq", 1)
                put("language", language)
            })
        }
    }

    fun topWords(language: String, limit: Int = 200): List<String> {
        val out = ArrayList<String>()
        db.cursor(
            "SELECT word FROM user_words WHERE language = ? ORDER BY freq DESC LIMIT ?",
            language, limit.toString()
        ).use { while (it.moveToNext()) out.add(it.getString(0)) }
        return out
    }

    fun frequency(word: String, language: String): Int {
        db.cursor(
            "SELECT freq FROM user_words WHERE word = ? AND language = ?",
            word.lowercase(), language
        ).use { if (it.moveToFirst()) return it.getInt(0) }
        return 0
    }

    fun recordBigram(previous: String, next: String) {
        val a = previous.trim().lowercase()
        val b = next.trim().lowercase()
        if (a.isEmpty() || b.isEmpty()) return
        val updated = db.executeUpdate(
            "UPDATE bigrams SET count = count + 1 WHERE prev = ? AND next = ?",
            a, b
        )
        if (updated == 0) {
            db.insert("bigrams", ContentValues().apply {
                put("prev", a)
                put("next", b)
                put("count", 1)
            })
        }
    }

    fun nextWords(previous: String, limit: Int = 5): List<String> {
        val out = ArrayList<String>()
        db.cursor(
            "SELECT next FROM bigrams WHERE prev = ? ORDER BY count DESC LIMIT ?",
            previous.trim().lowercase(), limit.toString()
        ).use { while (it.moveToNext()) out.add(it.getString(0)) }
        return out
    }

    fun addCustomWord(word: String, language: String) {
        val values = ContentValues().apply {
            put("word", word.trim())
            put("freq", 50)
            put("language", language)
        }
        db.insert("user_words", values)
    }

    fun customWords(language: String): List<String> = topWords(language, 500)

    fun eraseAll() {
        db.delete("user_words", null)
        db.delete("bigrams", null)
    }

    fun stats(): Pair<Int, Int> {
        val words = db.cursor("SELECT COUNT(*) FROM user_words").use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val bigrams = db.cursor("SELECT COUNT(*) FROM bigrams").use { if (it.moveToFirst()) it.getInt(0) else 0 }
        return words to bigrams
    }
}
