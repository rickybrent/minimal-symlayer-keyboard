package io.github.rickybrent.minimal_symlayer_keyboard

/**
 * A virtual modifier that can be used in the following ways:
 *  - hold to activate, release to deactivate
 *  - press to activate until the next printing key press
 *  - double press to lock until the next press of this modifier
 */
class Modifier {
	/**
	 * The maximum time of a double press needed to lock a modifier, in milliseconds.
	 */
	var lockThreshold = 250

	/**
	 * The minimum time of a long press needed to apply the modifier until it is released, in milliseconds.
	 */
	var nextThreshold = 350

	private var held = false
	private var lock = false
	private var next = false
	private var preventNext = false
	private var lastTime: Long = 0

	fun reset() {
		held = false
		lock = false
		next = false
		preventNext = false
		lastTime = 0
	}

	fun get() : Boolean {
		return lock || held || next
	}
	fun isLocked(): Boolean {
		return lock
	}
	fun isHeld(): Boolean {
		return held
	}

	fun onKeyDown() {
		held = true

		val t = System.currentTimeMillis()
		if(t - lastTime < lockThreshold) {
			lock = !lock
			preventNext = true
		} else {
			preventNext = lock || next
			lock = false
		}
		lastTime = t
	}
	fun onKeyUp() {
		val t = System.currentTimeMillis()
		next = !lock && t - lastTime < nextThreshold && !preventNext
		preventNext = false
		held = false
	}

	/**
	 * Suppress setting the one-shot "next" state on the upcoming key up.
	 *
	 * Use this when a combo (e.g., Right Shift + Space) consumes the modifier
	 * action, so the subsequent key up should not arm a one-shot (like Shift-next
	 * capitalization).
	 */
	fun suppressNextOnKeyUpOnce() {
		preventNext = true
	}

	fun activateForNext() {
		next = true
	}
	fun nextDidConsume() {
		next = false
		preventNext = true
	}
}

/**
 * A virtual modifier that can be used in the following ways:
 *  - hold to activate, release to deactivate
 *  - press to lock until the next press of this modifier
 */
class SimpleModifier {
	var lockThreshold = 350

	private var held = false
	private var lock = false
	private var lastTime: Long = 0

	fun reset() {
		held = false
		lock = false
		lastTime = 0
	}

	fun get() : Boolean {
		return lock || held
	}
	fun isLocked(): Boolean {
		return lock
	}
	fun isHeld(): Boolean {
		return held
	}

	fun onKeyDown() {
		held = true

		lock = !lock
		val t = System.currentTimeMillis()
		lastTime = t
	}
	fun onKeyUp() {
		val t = System.currentTimeMillis()
		if(t - lastTime > lockThreshold) {
			lock = false
		}
		held = false
	}
}

/**
 * A virtual modifier that can be used in the following ways:
 *  - hold to activate a modifier, release to deactivate.
 *  - press and immediately release to trigger a different keypress
 *  - trigger a "long press" event if it was held for long enough without any other keys being pressed.
 */
class TripleModifier {
	/**
	 * The keycode to send when held and other keys are input.
	 */
	var modKeyCode: Int = 0
	var shortPressKeyCode: Int = 0
	var longPressKeyCode: Int = 0

	/**
	 * The maximum time of a double press needed to lock a modifier, in milliseconds.
	 */
	var lockThreshold = 250

	/**
	 * The minimum time of a long press needed to apply the modifier until it is released, in milliseconds.
	 */
	var nextThreshold = 350

	private var held = false
	private var lock = false
	private var next = false
	private var longPress = false
	private var modMode = false
	private var preventNext = false
	private var lastTime: Long = 0
	private var preventKeyUp = false

	fun reset() {
		held = false
		lock = false
		next = false
		longPress = false
		modMode = false
		preventNext = false
		lastTime = 0
	}

	fun get(): Boolean {
		return lock || held || next
	}

	fun isLocked(): Boolean {
		return lock
	}

	fun isHeld(): Boolean {
		return held
	}

	fun isLongPress(): Boolean {
		return longPress
	}

	fun getModKey(): Int {
		return if (modMode) modKeyCode else 0
	}

	fun getKey(): Int {
		return if (longPress) longPressKeyCode else shortPressKeyCode
	}

	fun getAltKey(): Int {
		return longPressKeyCode
	}

	fun onKeyDown() {
		held = true

		val t = System.currentTimeMillis()
		if (t - lastTime < lockThreshold) {
			lock = !lock
			preventNext = true
		} else {
			preventNext = lock || next
			lock = false
		}
		lastTime = t
		preventKeyUp = false
	}

	fun onKeyUp() {
		val t = System.currentTimeMillis()
		next = !lock && t - lastTime < nextThreshold && !preventNext
		preventNext = false
		held = false
		longPress = false
		modMode = false
	}

	fun activateForNext() {
		next = true
	}

	fun nextDidConsume() {
		next = false
		preventNext = true
	}

	fun activateLongPress() {
		longPress = true
	}

	fun activateModKey() {
		if (modKeyCode != 0)
			modMode = true
	}

	fun activateSkipKeyUp() {
		preventKeyUp = true;
	}

	fun skipKeyUp() : Boolean{
		return preventKeyUp;
	}
}

/**
 * A modifier that manages the Cyrillic layout layer.
 * Activated by long pressing the right shift key.
 * Provides Cyrillic character input while maintaining access to modifier keys.
 */
class CyrillicLayerModifier {
	/**
	 * Whether the Cyrillic layer is currently active
	 */
	private var isActive = false
	
	/**
	 * Whether the right shift key is currently pressed
	 */
	private var rightShiftPressed = false
	
	/**
	 * Time threshold for long press detection (in milliseconds)
	 */
	private val longPressThreshold = 500L
	
	/**
	 * Time when right shift was first pressed
	 */
	private var rightShiftPressTime = 0L

	/**
	 * When true, the next right-shift key up will not trigger a long-press toggle.
	 */
	private var suppressNextUp = false
	
	/**
	 * Returns true if the Cyrillic layer is currently active
	 */
	fun isActive(): Boolean = isActive
	
	/**
	 * Returns true if the right shift key is currently pressed
	 */
	fun isRightShiftPressed(): Boolean = rightShiftPressed
	
	/**
	 * Handle right shift key press
	 */
	fun onRightShiftDown() {
		rightShiftPressed = true
		rightShiftPressTime = System.currentTimeMillis()
	}
	
	/**
	 * Handle right shift key release
	 */
	fun onRightShiftUp() {
		val pressDuration = System.currentTimeMillis() - rightShiftPressTime
		if (suppressNextUp) {
			// Skip long-press evaluation because we already toggled via a combo.
			suppressNextUp = false
			rightShiftPressed = false
			rightShiftPressTime = 0L
			return
		}
		
		if (pressDuration >= longPressThreshold) {
			// Long press detected - toggle Cyrillic layer
			isActive = !isActive
			wasJustToggled = true
		}
		
		rightShiftPressed = false
	}
	
	/**
	 * Check if the layer was just toggled in the last call
	 */
	private var wasJustToggled = false
	
	/**
	 * Returns true if the layer was just toggled and resets the flag
	 */
	fun wasJustToggled(): Boolean {
		val result = wasJustToggled
		wasJustToggled = false
		return result
	}
	
	/**
	 * Reset the Cyrillic layer state
	 */
	fun reset() {
		isActive = false
		rightShiftPressed = false
		rightShiftPressTime = 0L
		suppressNextUp = false
	}
	
	/**
	 * Force activate the Cyrillic layer
	 */
	fun activate() {
		isActive = true
	}
	
	/**
	 * Force deactivate the Cyrillic layer
	 */
	fun deactivate() {
		isActive = false
	}

	/**
	 * Instantly toggle the Cyrillic layer (e.g., when using a combo like Right Shift + Space)
	 * and ensure the next right-shift key up does not also toggle.
	 */
	fun instantToggle() {
		isActive = !isActive
		wasJustToggled = true
		// Prevent a subsequent key-up from also toggling
		suppressNextUp = true
	}
}

/**
 * A modifier that manages the Korean (Hangul) input mode.
 * Activated by long pressing the right shift key.
 * Architecturally mirrors CyrillicLayerModifier for consistency.
 */
class KoreanInputModifier {
	/**
	 * Whether Korean input is currently active
	 */
	private var isActive = false

	/**
	 * Whether the right shift key is currently pressed
	 */
	private var rightShiftPressed = false

	/**
	 * Time threshold for long press detection (in milliseconds)
	 */
	private val longPressThreshold = 500L

	/**
	 * Time when right shift was first pressed
	 */
	private var rightShiftPressTime = 0L

	/**
	 * When true, the next right-shift key up will not trigger a long-press toggle.
	 */
	private var suppressNextUp = false

	/**
	 * Returns true if Korean input is currently active
	 */
	fun isActive(): Boolean = isActive

	/**
	 * Returns true if the right shift key is currently pressed
	 */
	fun isRightShiftPressed(): Boolean = rightShiftPressed

	/**
	 * Handle right shift key press
	 */
	fun onRightShiftDown() {
		rightShiftPressed = true
		rightShiftPressTime = System.currentTimeMillis()
	}

	/**
	 * Handle right shift key release
	 */
	fun onRightShiftUp() {
		val pressDuration = System.currentTimeMillis() - rightShiftPressTime
		if (suppressNextUp) {
			// Skip long-press evaluation because we already toggled via a combo.
			suppressNextUp = false
			rightShiftPressed = false
			rightShiftPressTime = 0L
			return
		}
		if (pressDuration >= longPressThreshold) {
			// Long press detected - toggle Korean input
			isActive = !isActive
			wasJustToggled = true
		}
		rightShiftPressed = false
	}

	/**
	 * Check if the mode was just toggled in the last call
	 */
	private var wasJustToggled = false

	/**
	 * Returns true if the mode was just toggled and resets the flag
	 */
	fun wasJustToggled(): Boolean {
		val result = wasJustToggled
		wasJustToggled = false
		return result
	}

	/**
	 * Reset state
	 */
	fun reset() {
		isActive = false
		rightShiftPressed = false
		rightShiftPressTime = 0L
		wasJustToggled = false
		suppressNextUp = false
	}

	/**
	 * Force activate the Korean input
	 */
	fun activate() { isActive = true }

	/**
	 * Force deactivate the Korean input
	 */
	fun deactivate() { isActive = false }

	/**
	 * Instantly toggle the Korean input mode (e.g., when using a combo like Right Shift + Space)
	 * and ensure the next right-shift key up does not also toggle.
	 */
	fun instantToggle() {
		isActive = !isActive
		wasJustToggled = true
		// Prevent a subsequent key-up from also toggling
		suppressNextUp = true
	}
}