package io.github.rickybrent.minimalpocketkeyboard

data class Emoji(
    val character: String,
    val category: String,
    val subCategory: String,
    val name: String,
    val tags: List<String>
)