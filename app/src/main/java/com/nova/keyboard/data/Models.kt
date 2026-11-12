package com.nova.keyboard.data

data class ClipItem(
    val id: Long,
    val text: String,
    val createdAt: Long,
    val pinned: Boolean,
    val category: String
)

data class Snippet(
    val id: Long,
    val title: String,
    val body: String,
    val language: String,
    val pinned: Boolean
)

data class Macro(
    val id: Long,
    val shortcut: String,
    val expansion: String,
    val enabled: Boolean
)

data class WordFreq(val word: String, val freq: Int)
