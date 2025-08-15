package io.github.oin.titanpocketkeyboard

import android.view.KeyCharacterMap
import android.view.KeyEvent

// Define the different types of actions a key can perform
sealed class SymAction
data class SendKey(val keyCode: Int) : SymAction()
data class SendChar(val character: String, val shiftedCharacter: String? = null) : SymAction()

// Data class to hold the action and the display text together
data class KeyMapping(val display: String, val action: SymAction)

object SymKeyMappings {
    // --- DEFAULT MAPPINGS ---
    private val defaultMap: Map<Int, KeyMapping> = mapOf(
        KeyEvent.KEYCODE_W to KeyMapping("↑", SendKey(KeyEvent.KEYCODE_DPAD_UP)),
        KeyEvent.KEYCODE_A to KeyMapping("←", SendKey(KeyEvent.KEYCODE_DPAD_LEFT)),
        KeyEvent.KEYCODE_S to KeyMapping("↓", SendKey(KeyEvent.KEYCODE_DPAD_DOWN)),
        KeyEvent.KEYCODE_D to KeyMapping("→", SendKey(KeyEvent.KEYCODE_DPAD_RIGHT)),
        KeyEvent.KEYCODE_K to KeyMapping("↑", SendKey(KeyEvent.KEYCODE_DPAD_UP)),
        KeyEvent.KEYCODE_H to KeyMapping("←", SendKey(KeyEvent.KEYCODE_DPAD_LEFT)),
        KeyEvent.KEYCODE_J to KeyMapping("↓", SendKey(KeyEvent.KEYCODE_DPAD_DOWN)),
        KeyEvent.KEYCODE_L to KeyMapping("→", SendKey(KeyEvent.KEYCODE_DPAD_RIGHT)),
        KeyEvent.KEYCODE_Y to KeyMapping("Home", SendKey(KeyEvent.KEYCODE_MOVE_HOME)),
        KeyEvent.KEYCODE_U to KeyMapping("PgDn", SendKey(KeyEvent.KEYCODE_PAGE_DOWN)),
        KeyEvent.KEYCODE_I to KeyMapping("PgUp", SendKey(KeyEvent.KEYCODE_PAGE_UP)),
        KeyEvent.KEYCODE_O to KeyMapping("End", SendKey(KeyEvent.KEYCODE_MOVE_END)),
        KeyEvent.KEYCODE_P to KeyMapping("Esc", SendKey(KeyEvent.KEYCODE_ESCAPE)),
        KeyEvent.KEYCODE_X to KeyMapping("Cut", SendKey(KeyEvent.KEYCODE_CUT)),
        KeyEvent.KEYCODE_C to KeyMapping("Copy", SendKey(KeyEvent.KEYCODE_COPY)),
        KeyEvent.KEYCODE_V to KeyMapping("Paste", SendKey(KeyEvent.KEYCODE_PASTE)),
        KeyEvent.KEYCODE_Z to KeyMapping("Tab", SendKey(KeyEvent.KEYCODE_TAB)),
        KeyEvent.KEYCODE_Q to KeyMapping("Tab", SendKey(KeyEvent.KEYCODE_TAB)),
        KeyEvent.KEYCODE_B to KeyMapping("$", SendChar("$")),
        KeyEvent.KEYCODE_N to KeyMapping("=", SendChar("=")),
        KeyEvent.KEYCODE_E to KeyMapping("€", SendChar("€")),
        KeyEvent.KEYCODE_M to KeyMapping("%", SendChar("%"))
    )

    // --- MP01 MAPPINGS ---
    private val mp01Map: Map<Int, KeyMapping> = mapOf(
        KeyEvent.KEYCODE_W to KeyMapping("↑", SendKey(KeyEvent.KEYCODE_DPAD_UP)),
        KeyEvent.KEYCODE_A to KeyMapping("←", SendKey(KeyEvent.KEYCODE_DPAD_LEFT)),
        KeyEvent.KEYCODE_S to KeyMapping("↓", SendKey(KeyEvent.KEYCODE_DPAD_DOWN)),
        KeyEvent.KEYCODE_D to KeyMapping("→", SendKey(KeyEvent.KEYCODE_DPAD_RIGHT)),
        KeyEvent.KEYCODE_Q to KeyMapping("⇞", SendKey(KeyEvent.KEYCODE_PAGE_UP)),
        KeyEvent.KEYCODE_E to KeyMapping("⇟", SendKey(KeyEvent.KEYCODE_PAGE_DOWN)),
        KeyEvent.KEYCODE_R to KeyMapping("⇱", SendKey(KeyEvent.KEYCODE_MOVE_HOME)),
        KeyEvent.KEYCODE_F to KeyMapping("⇲", SendKey(KeyEvent.KEYCODE_MOVE_END)),
        KeyEvent.KEYCODE_Z to KeyMapping("⇥", SendKey(KeyEvent.KEYCODE_TAB)),
        KeyEvent.KEYCODE_X to KeyMapping("✂", SendKey(KeyEvent.KEYCODE_CUT)),
        KeyEvent.KEYCODE_C to KeyMapping("⎘", SendKey(KeyEvent.KEYCODE_COPY)),
        KeyEvent.KEYCODE_V to KeyMapping("📋︎", SendKey(KeyEvent.KEYCODE_PASTE)),
        KeyEvent.KEYCODE_T to KeyMapping("~", SendChar("~")),
        KeyEvent.KEYCODE_G to KeyMapping("`", SendChar("`")),
        KeyEvent.KEYCODE_Y to KeyMapping("[", SendChar("[")),
        KeyEvent.KEYCODE_U to KeyMapping("]", SendChar("]")),
        KeyEvent.KEYCODE_I to KeyMapping("(", SendChar("(")),
        KeyEvent.KEYCODE_O to KeyMapping(")", SendChar(")")),
        KeyEvent.KEYCODE_P to KeyMapping("€/£", SendChar("€", "£")),
        KeyEvent.KEYCODE_H to KeyMapping("{", SendChar("{")),
        KeyEvent.KEYCODE_J to KeyMapping("}", SendChar("}")),
        KeyEvent.KEYCODE_K to KeyMapping("^", SendChar("^")),
        KeyEvent.KEYCODE_L to KeyMapping("|", SendChar("|")),
        KeyEvent.KEYCODE_B to KeyMapping("\\", SendChar("\\")),
        KeyEvent.KEYCODE_N to KeyMapping("/", SendChar("/")),
        KeyEvent.KEYCODE_M to KeyMapping("<", SendChar("<")),
        KeyEvent.KEYCODE_PERIOD to KeyMapping(">", SendChar(">"))
    )

    fun getMapping(keyCode: Int, deviceType: InputMethodService.DeviceType): KeyMapping? {
        return when (deviceType) {
            InputMethodService.DeviceType.MP01 -> mp01Map[keyCode]
            InputMethodService.DeviceType.DEFAULT -> defaultMap[keyCode]
        }
    }

    fun getSymKeyDisplay(keyCode: Int, deviceType: InputMethodService.DeviceType): String {
        val mapping = getMapping(keyCode, deviceType)
        if (mapping != null) {
            return mapping.display
        }
        // Fallback for keys not in our map (like Shift, Enter, Bksp)
        val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        return when(keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "⇧"
            KeyEvent.KEYCODE_ENTER -> "↵"
            KeyEvent.KEYCODE_DEL -> "⌫"
            else -> keyCharacterMap.getDisplayLabel(keyCode)?.toString()?.uppercase() ?: ""
        }
    }
}
