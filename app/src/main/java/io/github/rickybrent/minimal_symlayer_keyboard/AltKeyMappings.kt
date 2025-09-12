package io.github.rickybrent.minimal_symlayer_keyboard

import android.view.KeyEvent

/**
 * Provides manual mappings for printed AltKey characters.
 * Temporary workaround after latest software update.
 */
object AltKeyMappings {
	
	/**
	 * Mapping from Latin key codes to AltKey characters (lowercase)
	 */
	private val lowercaseMap: Map<Int, Char> = mapOf(
		// Top row
		KeyEvent.KEYCODE_Q to '&',
		KeyEvent.KEYCODE_W to '1',
		KeyEvent.KEYCODE_E to '2',
		KeyEvent.KEYCODE_R to '3',
		KeyEvent.KEYCODE_T to '_',
		KeyEvent.KEYCODE_Y to '-',
		KeyEvent.KEYCODE_U to '+',
		KeyEvent.KEYCODE_I to '!',
		KeyEvent.KEYCODE_O to '#',
		KeyEvent.KEYCODE_P to '$',

		// Home row
		KeyEvent.KEYCODE_A to '@',
		KeyEvent.KEYCODE_S to '4',
		KeyEvent.KEYCODE_D to '5',
		KeyEvent.KEYCODE_F to '6',
		KeyEvent.KEYCODE_G to '=',
		KeyEvent.KEYCODE_H to ':',
		KeyEvent.KEYCODE_J to ';',
		KeyEvent.KEYCODE_K to '\'',
		KeyEvent.KEYCODE_L to '"',

		// Bottom row
		KeyEvent.KEYCODE_Z to '7',
		KeyEvent.KEYCODE_X to '8',
		KeyEvent.KEYCODE_C to '9',
		KeyEvent.KEYCODE_V to '*',
		KeyEvent.KEYCODE_B to '%',
		KeyEvent.KEYCODE_N to '?',
		KeyEvent.KEYCODE_M to ',',
		MP01_KEYCODE_EMOJI_PICKER to '0',
	)

	/**
	 * Get the AltKey character for the given key code and shift state
	 * @param keyCode The key code to look up
	 * @param isShifted Whether shift is active (uppercase)
	 * @return The AltKey character, or null if no mapping exists
	 */
	fun getAltKeyChar(keyCode: Int, isShifted: Boolean): Char? {
		return if (isShifted) {
			lowercaseMap[keyCode]
		} else {
			lowercaseMap[keyCode]
		}
	}

	/**
	 * Get all supported key codes for AltKey input
	 * @return Set of key codes that have AltKey mappings
	 */
	fun getSupportedKeyCodes(): Set<Int> = lowercaseMap.keys
}
