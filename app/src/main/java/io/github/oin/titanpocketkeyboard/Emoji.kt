package io.github.oin.titanpocketkeyboard

data class Emoji(
    val character: String,
    val category: String,
    val subCategory: String,
    val name: String,
    val tags: List<String>
)