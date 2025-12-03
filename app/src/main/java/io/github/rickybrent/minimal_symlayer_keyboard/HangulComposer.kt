package io.github.rickybrent.minimal_symlayer_keyboard

import android.view.inputmethod.InputConnection

/**
 * Minimal Hangul automata for 2-beolsik hardware layout.
 * Maps Latin QWERTY keys to Hangul Jamo and composes syllables.
 *
 * This is intentionally compact and covers the common combinations.
 */
class HangulComposer {
	private var cho = -1
	private var jung = -1
	private var jong = -1

	private var composing = ""

	fun reset(ic: InputConnection?) {
		cho = -1; jung = -1; jong = -1
		composing = ""
		ic?.finishComposingText()
	}

	fun isComposing(): Boolean = cho != -1 || jung != -1 || jong != -1

	fun inputLatinChar(ch: Char, ic: InputConnection?) {
		val jamo = latinToJamo(ch) ?: run {
			// Non-mapped key: commit current and pass through
			commitCurrent(ic)
			ic?.commitText(ch.toString(), 1)
			return
		}

		when (jamo.type) {
			JamoType.CHO -> handleCho(jamo, ic)
			JamoType.JUNG -> handleJung(jamo, ic)
			JamoType.JONG -> handleJong(jamo, ic)
		}
		updateComposing(ic)
	}

	fun handleSpaceOrEnter(ic: InputConnection?, text: String) {
		// Finalize current syllable before committing space/enter
		if (isComposing()) {
			commitCurrent(ic)
		}
		ic?.commitText(text, 1)
	}

	fun backspace(ic: InputConnection?): Boolean {
		if (!isComposing()) return false
		if (jong != -1) {
			// Try split complex jong first
			val split = splitComplexJong(jong)
			if (split != null) {
				jong = split.first
			} else {
				jong = -1
			}
		} else if (jung != -1) {
			val split = splitComplexJung(jung)
			if (split != null) {
				jung = split.first
			} else {
				jung = -1
			}
		} else if (cho != -1) {
			cho = -1
		}
		// If we've removed the last remaining jamo, explicitly clear the composing text
		if (!isComposing()) {
			composing = ""
			// Replace the composing region with empty to actually remove the visible jamo
			ic?.setComposingText("", 1)
			ic?.finishComposingText()
			return true
		}
		updateComposing(ic)
		return true
	}

	private fun handleCho(j: Jamo, ic: InputConnection?) {
		if (cho == -1) {
			if (jung != -1) {
				// Isolated vowel handling
				// Commit the isolated vowel and start a new syllable with this consonant as initial (cho).
				commitCurrent(ic)
				cho = j.choIndex
			} else {
				cho = j.choIndex
			}
			return
		}
		if (jung == -1) {
			// Combine double initial (e.g., ㄱ + ㄱ => ㄲ) if possible
			val combined = combineDoubleCho(cho, j.choIndex)
			if (combined != -1) {
				cho = combined
			} else {
				// Start new syllable
				commitCurrent(ic)
				cho = j.choIndex; jung = -1; jong = -1
			}
			return
		}
		// We have cho+jung (and maybe jong). Treat consonant as jong or start new syllable
		if (jong == -1) {
			jong = j.jongIndex
		} else {
			// Try compound jong
			val compound = combineJong(jong, j.jongIndex)
			if (compound != -1) {
				jong = compound
			} else {
				// Commit current syllable and start new with this consonant as cho
				commitCurrent(ic)
				cho = j.choIndex; jung = -1; jong = -1
			}
		}
	}

	private fun handleJung(j: Jamo, ic: InputConnection?) {
		// Allow standalone vowels: do NOT auto-insert ㅇ when starting with a vowel
		if (cho == -1) {
			if (jung == -1) {
				// Start an isolated vowel
				jung = j.jungIndex
			} else {
				// We are composing an isolated vowel already; try to combine
				val combined = combineJung(jung, j.jungIndex)
				if (combined != -1) {
					jung = combined
				} else {
					// Commit previous isolated vowel and start a new isolated vowel
					commitCurrent(ic)
					jung = j.jungIndex
				}
			}
			return
		}

		// There is a leading consonant (cho != -1)
		if (jung == -1) {
			jung = j.jungIndex
		} else if (jong == -1) {
			// Try to combine vowels (e.g., ㅗ + ㅏ => ㅘ)
			val combined = combineJung(jung, j.jungIndex)
			if (combined != -1) {
				jung = combined
			} else {
				// Commit previous syllable and start a new isolated vowel
				commitCurrent(ic)
				cho = -1
				jung = j.jungIndex
				jong = -1
			}
		} else {
			// We had final consonant; move it to initial of next syllable if possible
			val split = splitComplexJong(jong)
			if (split != null) {
				// Move second part to next syllable initial
				val movedCho = jongToCho(split.second)
				jong = split.first
				commitCurrent(ic)
				cho = movedCho
				jung = j.jungIndex
				jong = -1
			} else {
				val movedCho = jongToCho(jong)
				jong = -1
				commitCurrent(ic)
				cho = movedCho
				jung = j.jungIndex
			}
		}
	}

	private fun handleJong(j: Jamo, ic: InputConnection?) {
		// Treat as consonant input; route to handleCho
		handleCho(j, ic)
	}

	private fun updateComposing(ic: InputConnection?) {
		if (!isComposing()) {
			composing = ""
			ic?.finishComposingText()
			return
		}
		val text = composeSyllable()
		composing = text
		ic?.setComposingText(text, 1)
	}

	private fun commitCurrent(ic: InputConnection?) {
		if (!isComposing()) return
		val text = composeSyllable()
		ic?.commitText(text, 1)
		cho = -1; jung = -1; jong = -1
		composing = ""
		ic?.finishComposingText()
	}

	private fun composeSyllable(): String {
		return when {
			jung == -1 -> {
				// Isolated consonant: output compatibility Jamo for display
				COMPAT_CHO[cho].toString()
			}
			cho == -1 -> {
				// Isolated vowel
				COMPAT_JUNG[jung].toString()
			}
			else -> {
				val base = 0xAC00
				val c = base + ((cho * 21) + jung) * 28 + (jong + 1)
				String(Character.toChars(c))
			}
		}
	}

	// Mapping structures
	data class Jamo(val type: JamoType, val choIndex: Int = -1, val jungIndex: Int = -1, val jongIndex: Int = -1)
	enum class JamoType { CHO, JUNG, JONG }

	private fun latinToJamo(ch: Char): Jamo? {
		return when (ch) {
			// Row 1
			'q' -> jCho(CHO_ㅂ); 'Q' -> jCho(CHO_ㅃ)
			'w' -> jCho(CHO_ㅈ); 'W' -> jCho(CHO_ㅉ)
			'e' -> jCho(CHO_ㄷ); 'E' -> jCho(CHO_ㄸ)
			'r' -> jCho(CHO_ㄱ); 'R' -> jCho(CHO_ㄲ)
			't' -> jCho(CHO_ㅅ); 'T' -> jCho(CHO_ㅆ)
			'y' -> jJung(JUNG_ㅛ)
			'u' -> jJung(JUNG_ㅕ)
			'i' -> jJung(JUNG_ㅑ)
			'o' -> jJung(JUNG_ㅐ); 'O' -> jJung(JUNG_ㅒ)
			'p' -> jJung(JUNG_ㅔ); 'P' -> jJung(JUNG_ㅖ)

			// Row 2
			'a' -> jCho(CHO_ㅁ)
			's' -> jCho(CHO_ㄴ)
			'd' -> jCho(CHO_ㅇ)
			'f' -> jCho(CHO_ㄹ)
			'g' -> jCho(CHO_ㅎ)
			'h' -> jJung(JUNG_ㅗ)
			'j' -> jJung(JUNG_ㅓ)
			'k' -> jJung(JUNG_ㅏ)
			'l' -> jJung(JUNG_ㅣ)

			// Row 3
			'z' -> jCho(CHO_ㅋ)
			'x' -> jCho(CHO_ㅌ)
			'c' -> jCho(CHO_ㅊ)
			'v' -> jCho(CHO_ㅍ)
			'b' -> jJung(JUNG_ㅠ)
			'n' -> jJung(JUNG_ㅜ)
			'm' -> jJung(JUNG_ㅡ)

			else -> null
		}
	}

	private fun jCho(idx: Int) = Jamo(JamoType.CHO, choIndex = idx, jongIndex = CHO_TO_JONG[idx])
	private fun jJung(idx: Int) = Jamo(JamoType.JUNG, jungIndex = idx)

	private fun combineDoubleCho(first: Int, second: Int): Int {
		return when {
			first == CHO_ㄱ && second == CHO_ㄱ -> CHO_ㄲ
			first == CHO_ㄷ && second == CHO_ㄷ -> CHO_ㄸ
			first == CHO_ㅂ && second == CHO_ㅂ -> CHO_ㅃ
			first == CHO_ㅅ && second == CHO_ㅅ -> CHO_ㅆ
			first == CHO_ㅈ && second == CHO_ㅈ -> CHO_ㅉ
			else -> -1
		}
	}

	private fun combineJung(first: Int, second: Int): Int {
		return when (first to second) {
			JUNG_ㅗ to JUNG_ㅏ -> JUNG_ㅘ
			JUNG_ㅗ to JUNG_ㅐ -> JUNG_ㅙ
			JUNG_ㅗ to JUNG_ㅣ -> JUNG_ㅚ
			JUNG_ㅜ to JUNG_ㅓ -> JUNG_ㅝ
			JUNG_ㅜ to JUNG_ㅔ -> JUNG_ㅞ
			JUNG_ㅜ to JUNG_ㅣ -> JUNG_ㅟ
			JUNG_ㅡ to JUNG_ㅣ -> JUNG_ㅢ
			else -> -1
		}
	}

	private fun splitComplexJung(idx: Int): Pair<Int, Int>? {
		return when (idx) {
			JUNG_ㅘ -> JUNG_ㅗ to JUNG_ㅏ
			JUNG_ㅙ -> JUNG_ㅗ to JUNG_ㅐ
			JUNG_ㅚ -> JUNG_ㅗ to JUNG_ㅣ
			JUNG_ㅝ -> JUNG_ㅜ to JUNG_ㅓ
			JUNG_ㅞ -> JUNG_ㅜ to JUNG_ㅔ
			JUNG_ㅟ -> JUNG_ㅜ to JUNG_ㅣ
			JUNG_ㅢ -> JUNG_ㅡ to JUNG_ㅣ
			else -> null
		}
	}

	private fun combineJong(first: Int, second: Int): Int {
		return when (first to second) {
			JONG_ㄱ to JONG_ㅅ -> JONG_ㄳ
			JONG_ㄴ to JONG_ㅈ -> JONG_ㄵ
			JONG_ㄴ to JONG_ㅎ -> JONG_ㄶ
			JONG_ㄹ to JONG_ㄱ -> JONG_ㄺ
			JONG_ㄹ to JONG_ㅁ -> JONG_ㄻ
			JONG_ㄹ to JONG_ㅂ -> JONG_ㄼ
			JONG_ㄹ to JONG_ㅅ -> JONG_ㄽ
			JONG_ㄹ to JONG_ㅌ -> JONG_ㄾ
			JONG_ㄹ to JONG_ㅍ -> JONG_ㄿ
			JONG_ㄹ to JONG_ㅎ -> JONG_ㅀ
			JONG_ㅂ to JONG_ㅅ -> JONG_ㅄ
			else -> -1
		}
	}

	private fun splitComplexJong(idx: Int): Pair<Int, Int>? {
		return when (idx) {
			JONG_ㄳ -> JONG_ㄱ to JONG_ㅅ
			JONG_ㄵ -> JONG_ㄴ to JONG_ㅈ
			JONG_ㄶ -> JONG_ㄴ to JONG_ㅎ
			JONG_ㄺ -> JONG_ㄹ to JONG_ㄱ
			JONG_ㄻ -> JONG_ㄹ to JONG_ㅁ
			JONG_ㄼ -> JONG_ㄹ to JONG_ㅂ
			JONG_ㄽ -> JONG_ㄹ to JONG_ㅅ
			JONG_ㄾ -> JONG_ㄹ to JONG_ㅌ
			JONG_ㄿ -> JONG_ㄹ to JONG_ㅍ
			JONG_ㅀ -> JONG_ㄹ to JONG_ㅎ
			JONG_ㅄ -> JONG_ㅂ to JONG_ㅅ
			else -> null
		}
	}

	private fun jongToCho(jongIdx: Int): Int {
		return JONG_TO_CHO[jongIdx]
	}

	companion object {
		// Choseong indices 0..18 in Unicode order
		const val CHO_ㄱ = 0
		const val CHO_ㄲ = 1
		const val CHO_ㄴ = 2
		const val CHO_ㄷ = 3
		const val CHO_ㄸ = 4
		const val CHO_ㄹ = 5
		const val CHO_ㅁ = 6
		const val CHO_ㅂ = 7
		const val CHO_ㅃ = 8
		const val CHO_ㅅ = 9
		const val CHO_ㅆ = 10
		const val CHO_ㅇ = 11
		const val CHO_ㅈ = 12
		const val CHO_ㅉ = 13
		const val CHO_ㅊ = 14
		const val CHO_ㅋ = 15
		const val CHO_ㅌ = 16
		const val CHO_ㅍ = 17
		const val CHO_ㅎ = 18

		// Jungseong indices 0..20 in Unicode order
		// Unicode Jungseong indices (21 entries: 0..20)
		const val JUNG_ㅏ = 0
		const val JUNG_ㅐ = 1
		const val JUNG_ㅑ = 2
		const val JUNG_ㅒ = 3
		const val JUNG_ㅓ = 4
		const val JUNG_ㅔ = 5
		const val JUNG_ㅕ = 6
		const val JUNG_ㅖ = 7
		const val JUNG_ㅗ = 8
		const val JUNG_ㅘ = 9
		const val JUNG_ㅙ = 10
		const val JUNG_ㅚ = 11
		const val JUNG_ㅛ = 12
		const val JUNG_ㅜ = 13
		const val JUNG_ㅝ = 14
		const val JUNG_ㅞ = 15
		const val JUNG_ㅟ = 16
		const val JUNG_ㅠ = 17
		const val JUNG_ㅡ = 18
		const val JUNG_ㅢ = 19
		const val JUNG_ㅣ = 20

		// Jongseong indices: -1 means none. In Unicode order 0..27 map to actual final (but we use 0..27 as we add +1 when composing)
		const val JONG_NONE = -1
		const val JONG_ㄱ = 0
		const val JONG_ㄲ = 1
		const val JONG_ㄳ = 2
		const val JONG_ㄴ = 3
		const val JONG_ㄵ = 4
		const val JONG_ㄶ = 5
		const val JONG_ㄷ = 6
		const val JONG_ㄹ = 7
		const val JONG_ㄺ = 8
		const val JONG_ㄻ = 9
		const val JONG_ㄼ = 10
		const val JONG_ㄽ = 11
		const val JONG_ㄾ = 12
		const val JONG_ㄿ = 13
		const val JONG_ㅀ = 14
		const val JONG_ㅁ = 15
		const val JONG_ㅂ = 16
		const val JONG_ㅄ = 17
		const val JONG_ㅅ = 18
		const val JONG_ㅆ = 19
		const val JONG_ㅇ = 20
		const val JONG_ㅈ = 21
		const val JONG_ㅊ = 22
		const val JONG_ㅋ = 23
		const val JONG_ㅌ = 24
		const val JONG_ㅍ = 25
		const val JONG_ㅎ = 26

		// Mapping between CHO index and a display compatibility jamo char for isolated initial
		private val COMPAT_CHO = charArrayOf(
			'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
		)

		// Mapping between JUNG index and a display compatibility jamo char for isolated vowel
		private val COMPAT_JUNG = charArrayOf(
			'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ'
		)

		// CHO to JONG for simple consonants
		private val CHO_TO_JONG = intArrayOf(
			JONG_ㄱ, JONG_ㄲ, JONG_ㄴ, JONG_ㄷ, JONG_ㄷ, JONG_ㄹ, JONG_ㅁ, JONG_ㅂ, JONG_ㅂ, JONG_ㅅ, JONG_ㅆ, JONG_ㅇ, JONG_ㅈ, JONG_ㅈ, JONG_ㅊ, JONG_ㅋ, JONG_ㅌ, JONG_ㅍ, JONG_ㅎ
		)

		// JONG to CHO for moving finals
		private val JONG_TO_CHO = intArrayOf(
			CHO_ㄱ, CHO_ㄲ, CHO_ㄱ, CHO_ㄴ, CHO_ㅈ, CHO_ㅎ, CHO_ㄷ, CHO_ㄹ, CHO_ㄹ, CHO_ㄹ, CHO_ㄹ, CHO_ㄹ, CHO_ㄹ, CHO_ㄹ, CHO_ㅎ, CHO_ㅁ, CHO_ㅂ, CHO_ㅂ, CHO_ㅅ, CHO_ㅆ, CHO_ㅇ, CHO_ㅈ, CHO_ㅊ, CHO_ㅋ, CHO_ㅌ, CHO_ㅍ, CHO_ㅎ
		)
	}
}
