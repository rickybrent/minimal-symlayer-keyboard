package io.github.rickybrent.minimal_symlayer_keyboard

import android.view.KeyEvent

/**
 * Provides mappings from Latin QWERTY keys to Cyrillic characters.
 * Maps both lowercase and uppercase variants.
 */
object CyrillicMappings {
	
	/**
	 * Mapping from Latin key codes to Cyrillic characters (lowercase)
	 * Classic PC (JCUKEN-like) layout adapted to available keys.
	 */
	private val lowercaseMap: Map<Int, Char> = mapOf(
		// Top row
		KeyEvent.KEYCODE_Q to 'й',
		KeyEvent.KEYCODE_W to 'ц',
		KeyEvent.KEYCODE_E to 'у',
		KeyEvent.KEYCODE_R to 'к',
		KeyEvent.KEYCODE_T to 'е',
		KeyEvent.KEYCODE_Y to 'н',
		KeyEvent.KEYCODE_U to 'г',
		KeyEvent.KEYCODE_I to 'ш',
		KeyEvent.KEYCODE_O to 'щ',
		KeyEvent.KEYCODE_P to 'з',
		// Removed direct mappings for х and ъ (moved to Alt-combos)
		// KeyEvent.KEYCODE_LEFT_BRACKET to 'х',
		// KeyEvent.KEYCODE_RIGHT_BRACKET to 'ъ',
		// Removed MINUS to 'х' fallback per request
		// Removed EQUALS to 'э' fallback per request
		
		// Home row
		KeyEvent.KEYCODE_A to 'ф',
		KeyEvent.KEYCODE_S to 'ы',
		KeyEvent.KEYCODE_D to 'в',
		KeyEvent.KEYCODE_F to 'а',
		KeyEvent.KEYCODE_G to 'п',
		KeyEvent.KEYCODE_H to 'р',
		KeyEvent.KEYCODE_J to 'о',
		KeyEvent.KEYCODE_K to 'л',
		KeyEvent.KEYCODE_L to 'д',
		// Removed direct mappings for ж and э (moved to Alt-combos)
		// KeyEvent.KEYCODE_SEMICOLON to 'ж',
		// KeyEvent.KEYCODE_APOSTROPHE to 'э',
		
		// Bottom row
		KeyEvent.KEYCODE_Z to 'я',
		KeyEvent.KEYCODE_X to 'ч',
		KeyEvent.KEYCODE_C to 'с',
		KeyEvent.KEYCODE_V to 'м',
		KeyEvent.KEYCODE_B to 'и',
		KeyEvent.KEYCODE_N to 'т',
		KeyEvent.KEYCODE_M to 'ь',
		// Removed direct mappings for б and ю (moved to Alt-combos)
		// KeyEvent.KEYCODE_COMMA to 'б',
		// KeyEvent.KEYCODE_PERIOD to 'ю',
		KeyEvent.KEYCODE_SLASH to '.',
		KeyEvent.KEYCODE_BACKSLASH to '/',
		
		// Removed direct mapping for ё (moved to Alt-combo on K)
		// KeyEvent.KEYCODE_GRAVE to 'ё'
	)
	
	/**
	 * Mapping from Latin key codes to Cyrillic characters (uppercase)
	 */
	private val uppercaseMap: Map<Int, Char> = mapOf(
		// Top row
		KeyEvent.KEYCODE_Q to 'Й',
		KeyEvent.KEYCODE_W to 'Ц',
		KeyEvent.KEYCODE_E to 'У',
		KeyEvent.KEYCODE_R to 'К',
		KeyEvent.KEYCODE_T to 'Е',
		KeyEvent.KEYCODE_Y to 'Н',
		KeyEvent.KEYCODE_U to 'Г',
		KeyEvent.KEYCODE_I to 'Ш',
		KeyEvent.KEYCODE_O to 'Щ',
		KeyEvent.KEYCODE_P to 'З',
		// Removed direct mappings for Х and Ъ
		// Removed MINUS/EQUALS fallbacks
		
		// Home row
		KeyEvent.KEYCODE_A to 'Ф',
		KeyEvent.KEYCODE_S to 'Ы',
		KeyEvent.KEYCODE_D to 'В',
		KeyEvent.KEYCODE_F to 'А',
		KeyEvent.KEYCODE_G to 'П',
		KeyEvent.KEYCODE_H to 'Р',
		KeyEvent.KEYCODE_J to 'О',
		KeyEvent.KEYCODE_K to 'Л',
		KeyEvent.KEYCODE_L to 'Д',
		// Removed direct mappings for Ж and Э
		
		// Bottom row
		KeyEvent.KEYCODE_Z to 'Я',
		KeyEvent.KEYCODE_X to 'Ч',
		KeyEvent.KEYCODE_C to 'С',
		KeyEvent.KEYCODE_V to 'М',
		KeyEvent.KEYCODE_B to 'И',
		KeyEvent.KEYCODE_N to 'Т',
		KeyEvent.KEYCODE_M to 'Ь',
		// Removed direct mappings for Б and Ю
		KeyEvent.KEYCODE_SLASH to '.',
		KeyEvent.KEYCODE_BACKSLASH to '/',
		
		// Removed direct mapping for Ё
	)
	
	/**
	 * Alt-combo mappings used when Cyrillic layer is active and ALT is held.
	 * 1) Alt+Y -> х
	 * 2) Alt+U -> ъ
	 * 3) Alt+G -> ж
	 * 4) Alt+H -> э
	 * 5) Alt+J -> ю
	 * 6) Alt+K -> ё
	 * 7) Alt+M -> б
	 */
	private val altLowercaseMap: Map<Int, Char> = mapOf(
		KeyEvent.KEYCODE_Y to 'х',
		KeyEvent.KEYCODE_U to 'ъ',
		KeyEvent.KEYCODE_G to 'ж',
		KeyEvent.KEYCODE_H to 'э',
		KeyEvent.KEYCODE_J to 'ю',
		KeyEvent.KEYCODE_K to 'ё',
		KeyEvent.KEYCODE_M to 'б',
	)
	private val altUppercaseMap: Map<Int, Char> = mapOf(
		KeyEvent.KEYCODE_Y to 'Х',
		KeyEvent.KEYCODE_U to 'Ъ',
		KeyEvent.KEYCODE_G to 'Ж',
		KeyEvent.KEYCODE_H to 'Э',
		KeyEvent.KEYCODE_J to 'Ю',
		KeyEvent.KEYCODE_K to 'Ё',
		KeyEvent.KEYCODE_M to 'Б',
	)
	
	/**
	 * Get the Cyrillic character for the given key code and shift state
	 * @param keyCode The key code to look up
	 * @param isShifted Whether shift is active (uppercase)
	 * @return The Cyrillic character, or null if no mapping exists
	 */
	fun getCyrillicChar(keyCode: Int, isShifted: Boolean): Char? {
		return if (isShifted) {
			uppercaseMap[keyCode]
		} else {
			lowercaseMap[keyCode]
		}
	}
	
	/**
	 * Get the Cyrillic character for Alt+key when Cyrillic layer is active.
	 */
	fun getAltCyrillicChar(keyCode: Int, isShifted: Boolean): Char? {
		return if (isShifted) {
			altUppercaseMap[keyCode]
		} else {
			altLowercaseMap[keyCode]
		}
	}
	
	/**
	 * Check if a key code has a Cyrillic mapping
	 * @param keyCode The key code to check
	 * @return True if the key code has a Cyrillic mapping
	 */
	fun hasCyrillicMapping(keyCode: Int): Boolean {
		return lowercaseMap.containsKey(keyCode)
	}
	
	/**
	 * Check if a key code has an ALT Cyrillic mapping
	 */
	fun hasAltCyrillicMapping(keyCode: Int): Boolean {
		return altLowercaseMap.containsKey(keyCode)
	}
	
	/**
	 * Get all supported key codes for Cyrillic input
	 * @return Set of key codes that have Cyrillic mappings
	 */
	fun getSupportedKeyCodes(): Set<Int> = lowercaseMap.keys
}
