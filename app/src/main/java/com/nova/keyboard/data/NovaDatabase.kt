package com.nova.keyboard.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NovaDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE clipboard(id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, " +
                "category TEXT NOT NULL DEFAULT 'general')"
        )
        db.execSQL(
            "CREATE TABLE snippets(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, " +
                "body TEXT NOT NULL, language TEXT NOT NULL DEFAULT 'general', " +
                "pinned INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE macros(id INTEGER PRIMARY KEY AUTOINCREMENT, shortcut TEXT NOT NULL UNIQUE, " +
                "expansion TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)"
        )
        db.execSQL(
            "CREATE TABLE user_words(id INTEGER PRIMARY KEY AUTOINCREMENT, word TEXT NOT NULL, " +
                "freq INTEGER NOT NULL DEFAULT 1, language TEXT NOT NULL DEFAULT 'en', " +
                "UNIQUE(word, language))"
        )
        db.execSQL(
            "CREATE TABLE bigrams(id INTEGER PRIMARY KEY AUTOINCREMENT, prev TEXT NOT NULL, " +
                "next TEXT NOT NULL, count INTEGER NOT NULL DEFAULT 1, UNIQUE(prev, next))"
        )
        db.execSQL("CREATE INDEX idx_clip_created ON clipboard(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 is the initial schema; future migrations are added here.
    }

    fun cursor(sql: String, vararg args: String): Cursor = readableDatabase.rawQuery(sql, args)

    fun exec(sql: String, vararg args: Any?) {
        writableDatabase.execSQL(sql, args)
    }

    fun insert(table: String, values: ContentValues): Long =
        writableDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE)

    fun update(table: String, values: ContentValues, where: String, vararg args: String) {
        writableDatabase.update(table, values, where, args)
    }

    fun delete(table: String, where: String?, vararg args: String) {
        writableDatabase.delete(table, where, args)
    }

    companion object {
        private const val NAME = "nova_keyboard.db"
        private const val VERSION = 1
    }
}
