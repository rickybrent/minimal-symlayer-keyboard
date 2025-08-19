package io.github.rickybrent.minimal_symlayer_keyboard

data class Clipping(
    val text: String,
    var isPinned: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Clipping

        return text == other.text
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }
}
