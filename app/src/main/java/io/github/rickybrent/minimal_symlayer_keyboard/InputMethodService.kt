package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager
import java.util.Locale
import android.inputmethodservice.InputMethodService as AndroidInputMethodService

/**
 * MP01 keycode sent instead of KeyEvent.KEYCODE_EMOJI_PICKER.
 */
const val MP01_KEYCODE_EMOJI_PICKER = 666;

/**
 * MP01 keycode sent instead of KeyEvent.KEYCODE_DICTATE.
 */
const val MP01_KEYCODE_DICTATE = 667;

// Only for modifier keys we want to force when using sym+keys to navigate.
val forceModifierPairs = listOf(
		KeyEvent.META_SHIFT_ON to KeyEvent.KEYCODE_SHIFT_LEFT,
		KeyEvent.META_META_ON to KeyEvent.KEYCODE_META_LEFT,
		KeyEvent.META_CTRL_ON to KeyEvent.KEYCODE_CTRL_LEFT,
	)

/**
 * @return true if it is suitable to provide suggestions or text transforms in the given editor.
 */
fun canUseSuggestions(editorInfo: EditorInfo): Boolean {
	if(editorInfo.inputType == InputType.TYPE_NULL) {
		return false
	}

	return when(editorInfo.inputType and InputType.TYPE_MASK_VARIATION) {
		InputType.TYPE_TEXT_VARIATION_PASSWORD -> false
		InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> false
		InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> false
		InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> false
		InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> false
		InputType.TYPE_TEXT_VARIATION_URI -> false
		else -> true
	}
}

/**
 * @return A KeyEvent made from the given one, but with the given key code.
 */
fun makeKeyEvent(original: KeyEvent, code: Int): KeyEvent {
	return makeKeyEvent(original, code, original.metaState, original.action, original.source, KeyCharacterMap.VIRTUAL_KEYBOARD)
}

/**
 * @return A KeyEvent made from the given one, but with the given key code, meta state, action, and source.
 */
fun makeKeyEvent(original: KeyEvent, code: Int, metaState: Int, action: Int, source: Int): KeyEvent {
	return makeKeyEvent(original, code, metaState, action, source, KeyCharacterMap.VIRTUAL_KEYBOARD)
}

/**
 * @return A KeyEvent made from the given one, but with the given key code, meta state, action, source, and deviceId.
 */
fun makeKeyEvent(original: KeyEvent, code: Int, metaState: Int, action: Int, source: Int, deviceId: Int): KeyEvent {
	return KeyEvent(original.downTime, original.eventTime, action, code, original.repeatCount, metaState, deviceId, code, 0, source)
}

val templates = hashMapOf(
	"fr" to hashMapOf(
		KeyEvent.KEYCODE_A to arrayOf('`', '^', 'æ', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_E to arrayOf('´', '`', '^', '¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_I to arrayOf('^', '¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('^', 'œ', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('`', '^', '¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_Y to arrayOf('¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_C to arrayOf('ç', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	),
	"fr-ext" to hashMapOf(
		KeyEvent.KEYCODE_A to arrayOf('`', '^', '´', '¨', 'æ', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_E to arrayOf('´', '`', '^', '¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_I to arrayOf('^', '´', '¨', '`', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('^', '´', 'œ', '¨', '~', '`', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('`', '^', '´', '¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_Y to arrayOf('¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_C to arrayOf('ç', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	),
	"es" to hashMapOf(
		KeyEvent.KEYCODE_A to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_E to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_I to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	),
	"de" to hashMapOf(
		KeyEvent.KEYCODE_A to arrayOf('¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('¨', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_S to arrayOf('ß', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	),
	"pt" to hashMapOf(
		KeyEvent.KEYCODE_A to arrayOf('´', '^', '`', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_E to arrayOf('´', '^', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_I to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('´', '^', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('´', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_C to arrayOf('ç', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	),
	"order1" to hashMapOf( // áàâäã
		KeyEvent.KEYCODE_A to arrayOf('´', '`', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_E to arrayOf('´', '`', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_I to arrayOf('´', '`', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('´', '`', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('´', '`', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	),
	"order2" to hashMapOf( // àáâäã
		KeyEvent.KEYCODE_A to arrayOf('`', '´', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_E to arrayOf('`', '´', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_I to arrayOf('`', '´', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_O to arrayOf('`', '´', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_U to arrayOf('`', '´', '^', '¨', '~', MPSUBST_BYPASS),
		KeyEvent.KEYCODE_SPACE to arrayOf(MPSUBST_STR_DOTSPACE)
	)
)

class InputMethodService : AndroidInputMethodService() {
	private lateinit var vibrator: Vibrator
	private var pickerManager: PickerManager? = null
	private var mainInputView: View? = null
	private var inputViewStrip: View? = null
	private var stripStatusIcon: ImageView? = null
    private var stripLanguageText: TextView? = null

	val shift = Modifier()
	private val alt = Modifier()
	private val sym = SimpleModifier()
	private val dotCtrl = TripleModifier()
	private val emojiMeta = TripleModifier()
	private val caps = Modifier()

	private var lastShift = false
	private var lastAlt = false
	private var lastSym = false
	private var lastDotCtrl = false
	private var lastEmojiMeta = false
	private var lastCaps = false

	private var autoCapitalize = false

	enum class DeviceType(val source: Int) {
		TITAN(InputDevice.SOURCE_KEYBOARD),
		MP01(InputDevice.SOURCE_KEYBOARD)
	}
	var lastDeviceId = -1
		private set
	var deviceType = DeviceType.TITAN
		private set

	private val multipress = MultipressController(arrayOf(
		templates["fr-ext"]!!,
		hashMapOf(
			KeyEvent.KEYCODE_Q to arrayOf(MPSUBST_TOGGLE_ALT, '°', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_W to arrayOf(MPSUBST_TOGGLE_ALT, '&', '↑', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_E to arrayOf(MPSUBST_TOGGLE_ALT, '€', '∃', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_R to arrayOf(MPSUBST_TOGGLE_ALT, '®', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_T to arrayOf(MPSUBST_TOGGLE_ALT, '[', '{', '<', '≤', '†', '™', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_Y to arrayOf(MPSUBST_TOGGLE_ALT, ']', '}', '>', '≥', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_U to arrayOf(MPSUBST_TOGGLE_ALT, '—', '–', '∪', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_I to arrayOf(MPSUBST_TOGGLE_ALT, '|', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_O to arrayOf(MPSUBST_TOGGLE_ALT, '\\', 'œ', 'º', '÷', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_P to arrayOf(MPSUBST_TOGGLE_ALT, ';', '¶', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_A to arrayOf(MPSUBST_TOGGLE_ALT, 'æ', 'ª', '←', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_S to arrayOf(MPSUBST_TOGGLE_ALT, 'ß', '§', '↓', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_D to arrayOf(MPSUBST_TOGGLE_ALT, '∂', '→', '⇒', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_F to arrayOf(MPSUBST_TOGGLE_ALT, MPSUBST_CIRCUMFLEX, MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_G to arrayOf(MPSUBST_TOGGLE_ALT, '•', '·', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_H to arrayOf(MPSUBST_TOGGLE_ALT, '²', '♯', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_J to arrayOf(MPSUBST_TOGGLE_ALT, '=', '≠', '≈', '±', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_K to arrayOf(MPSUBST_TOGGLE_ALT, '%', '‰', '‱', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_L to arrayOf(MPSUBST_TOGGLE_ALT, MPSUBST_BACKTICK, MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_Z to arrayOf(MPSUBST_TOGGLE_ALT, '¡', '‽', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_X to arrayOf(MPSUBST_TOGGLE_ALT, '×', 'χ', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_C to arrayOf(MPSUBST_TOGGLE_ALT, 'ç', '©', '¢', '⊂', '⊄', '⊃', '⊅', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_V to arrayOf(MPSUBST_TOGGLE_ALT, '∀', '√', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_B to arrayOf(MPSUBST_TOGGLE_ALT, '…', 'ß', '∫', '♭', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_N to arrayOf(MPSUBST_TOGGLE_ALT, '~', '¬', '∩', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_M to arrayOf(MPSUBST_TOGGLE_ALT, '$', '€', '£', '¿', MPSUBST_TOGGLE_SHIFT, MPSUBST_BYPASS),
			MP01_KEYCODE_EMOJI_PICKER to arrayOf(MPSUBST_TOGGLE_ALT, MPSUBST_BYPASS),
			MP01_KEYCODE_DICTATE to arrayOf(MPSUBST_TOGGLE_ALT, MPSUBST_BYPASS),
			KeyEvent.KEYCODE_SPACE to arrayOf('\t', '⇥', MPSUBST_BYPASS)
		)
	))

	private val unlockReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action == Intent.ACTION_USER_UNLOCKED) {
				ClipboardHistoryManager.loadPinnedClippings(this@InputMethodService)
				updateFromPreferences()
			}
		}
	}

	override fun onCreate() {
		super.onCreate()
		val context = createDeviceProtectedStorageContext()
		ClipboardHistoryManager.initialize(context)
		pickerManager = PickerManager(this, this)

		val preferences = PreferenceManager.getDefaultSharedPreferences(context)
		preferences.registerOnSharedPreferenceChangeListener { _, _ ->
			updateFromPreferences()
		}
		updateFromPreferences()
		val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
		registerReceiver(unlockReceiver, filter)

		vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
			mgr.defaultVibrator
		} else {
			@Suppress("DEPRECATION")
			getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		}
	}

	override fun onCreateInputView(): View {
		mainInputView = layoutInflater.inflate(R.layout.input_view_container, null)

		val pickerContainer = mainInputView?.findViewById<FrameLayout>(R.id.picker_container_inline)
		pickerManager?.setInlineViewContainer(pickerContainer)

		val inputContainer = mainInputView?.findViewById<FrameLayout>(R.id.input_view_container)
		this.inputViewStrip = layoutInflater.inflate(R.layout.input_view_strip, null)
		stripStatusIcon = this.inputViewStrip?.findViewById(R.id.modifier_icon)
		stripLanguageText = this.inputViewStrip?.findViewById(R.id.language_code_text)
		inputContainer?.addView(this.inputViewStrip)

		return mainInputView!!
	}

	override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
		super.onStartInputView(info, restarting)
		updateStatusIconIfNeeded()
	}

	private fun showEmojiPicker() {
		pickerManager?.show()
	}

	private fun showClipboardHistory() {
		pickerManager?.show(PickerManager.ViewType.CLIPBOARD)
	}

	override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
		super.onStartInput(attribute, restarting)

		updateFromPreferences()

		if(!sym.get()) {
			updateAutoCapitalization()
		}
	}

	/**
	 * Reset the shift/caps state when the InputView is closed and update the icons.
	 * Prevents auto-caps's icon from appearing when no text input is active.
	 */
	override fun onFinishInputView(finishingInput: Boolean) {
		super.onFinishInputView(finishingInput)
		shift.reset()
		caps.reset()
		updateStatusIconIfNeeded()
	}

	override fun onUpdateSelection(
		oldSelStart: Int,
		oldSelEnd: Int,
		newSelStart: Int,
		newSelEnd: Int,
		candidatesStart: Int,
		candidatesEnd: Int
	) {
		if(!sym.get()) {
			updateAutoCapitalization()
		}

		super.onUpdateSelection(
			oldSelStart,
			oldSelEnd,
			newSelStart,
			newSelEnd,
			candidatesStart,
			candidatesEnd
		)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		if (pickerManager?.isShowing() == true && pickerManager!!.handleKeyEvent(event)) {
			return true
		}

		updateDeviceType(event)
		// Update modifier states
		if(!event.isLongPress && event.repeatCount == 0) {
			when(event.keyCode) {
				KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
					alt.onKeyDown()
					updateStatusIconIfNeeded(true)
				}
				KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT  -> {
					if (caps.get()) {
						caps.reset()
					} else {
						shift.onKeyDown()
					}
					updateStatusIconIfNeeded(true)
				}
				KeyEvent.KEYCODE_SYM -> {
					sym.onKeyDown()
					onSymPossiblyChanged()
					updateStatusIconIfNeeded(true)
				}
				MP01_KEYCODE_DICTATE -> {
					dotCtrl.onKeyDown()
					updateStatusIconIfNeeded(true)
				}
				MP01_KEYCODE_EMOJI_PICKER -> {
					emojiMeta.onKeyDown()
					updateStatusIconIfNeeded(true)
				}
			}
		}

		if(event.isCtrlPressed) {
			return super.onKeyDown(keyCode, event)
		}

		// Apply any special logic for triple modifiers that may modify key handling.
		if (tripleModifierOnKeyDown(keyCode, event)) {
			return true
		}

		// Use special behavior when the SYM modifier is enabled
		if(sym.get()) {
			return onSymKey(event, true)
		}

		// Apply multipress substitution
		if(event.isPrintingKey || event.keyCode == KeyEvent.KEYCODE_SPACE) {
			val char = multipress.process(event, enhancedMetaState(event))
			if(char != MPSUBST_BYPASS) {
				if(char != MPSUBST_NOTHING) {
					currentInputConnection?.deleteSurroundingText(1, 0)
					updateAutoCapitalization()
					when(char) {
						MPSUBST_STR_DOTSPACE -> currentInputConnection?.commitText(". ", 2)
						else -> sendCharacter(char.toString())
					}

					consumeModifierNext()
				}
				vibrate()
				return true
			}
		}

		// Use default behavior for backspace and delete, with a few tweaks
		if(event.keyCode == KeyEvent.KEYCODE_DEL || event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
			multipress.reset()

			consumeModifierNext()

			return super.onKeyDown(keyCode, event)
		}

		// Ignore all long presses after this point
		if(event.isLongPress || event.repeatCount > 0) {
			return true
		}

		// Print something if it is a simple printing key press
		if((event.isPrintingKey || event.keyCode == KeyEvent.KEYCODE_SPACE || (event.keyCode == KeyEvent.KEYCODE_ENTER && shift.get()))) {
			val str = event.getUnicodeChar(enhancedMetaState(event)).toChar().toString()
			currentInputConnection?.commitText(str, 1)

			consumeModifierNext()
			return true
		}

		if(event.keyCode == KeyEvent.KEYCODE_ENTER) {
			consumeModifierNext()
		}

		return super.onKeyDown(keyCode, event)
	}

	fun tripleModifierOnKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		if (event.keyCode == MP01_KEYCODE_EMOJI_PICKER || event.keyCode == MP01_KEYCODE_DICTATE) {
			val tripleMod =
				if (event.keyCode == MP01_KEYCODE_EMOJI_PICKER) emojiMeta else dotCtrl;
			if (!tripleMod.get()) {
				tripleMod.onKeyDown();
			}
			if (multipress.process(event, enhancedMetaState(event)) == MPSUBST_BYPASS) {
				return true;
			}
			if (!tripleMod.isLongPress()) {
				tripleMod.activateLongPress()
				vibrate()
			}
			return true
		} else if (KeyEvent.isModifierKey(event.keyCode)) {
			// pass
		} else if (dotCtrl.get() && dotCtrl.getModKey() == 0 && dotCtrl.modKeyCode != 0) {
			// mark that we've activated the mod key, then send ctrl.
			dotCtrl.activateModKey()
			sendKey(dotCtrl.getModKey(), event, true)
		} else if (emojiMeta.get() && emojiMeta.getModKey() == 0 && emojiMeta.modKeyCode != 0) {
			// mark that we've activated the mod key, then send meta.
			emojiMeta.activateModKey()
			sendKey(emojiMeta.getModKey(), event, true)
		}
		// Prioritizing ctrl/meta, so commenting this out:
		// if(sym.get()) { return onSymKey(event, true) }
		// Handle emojiMeta + key shortcuts.
		if (emojiMeta.get() && onEmojiMetaShotcut(event)) {
			return true
		}

		// If either modkey is active, send the key as a keypress.
		if (dotCtrl.getModKey() != 0 || emojiMeta.getModKey() != 0) {
			sendKey(keyCode, event, true)
			sendKey(keyCode, event, false)
			return true
		}
		return false
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		if (pickerManager?.isShowing() == true && pickerManager!!.handleKeyEvent(event)) {
			return true
		}

		// Update modifier states
		when(event.keyCode) {
			KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
				alt.onKeyUp()
				updateStatusIconIfNeeded(true)
			}
			KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT  -> {
				shift.onKeyUp()
				updateStatusIconIfNeeded(true)
			}
			KeyEvent.KEYCODE_SYM -> {
				sym.onKeyUp()
				onSymPossiblyChanged()
				updateStatusIconIfNeeded(true)
			}
		}

		// Apply any special logic for triple modifiers that may modify key handling.
		if (tripleModifierOnKeyUp(keyCode, event)) {
			return true
		}
		// Use special behavior when the SYM modifier is enabled
		if(sym.get()) {
			return onSymKey(event, false)
		}

		return super.onKeyUp(keyCode, event)
	}

	/**
	 * Handle a triple modifier key up, which may send additional key presses or actions depending on if a key was pressed or how long it was held.
	 */
	private fun tripleModifierOnKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		if (keyCode == MP01_KEYCODE_DICTATE || keyCode == MP01_KEYCODE_EMOJI_PICKER) {
			val modifier = if (event.keyCode == MP01_KEYCODE_EMOJI_PICKER) emojiMeta else dotCtrl;
			val kbdKey = modifier.getKey()
			val modKey = modifier.getModKey()
			modifier.reset();
			updateStatusIconIfNeeded(true)
			if (modKey != 0) {
				sendKey(modKey, event, false)
				return true
			} else if (kbdKey != 0) {
				// Simulate tapping the shortpress or longpress key.
				simulateKeyTap(kbdKey, event, event.metaState)
				consumeModifierNext()
				return true
			}
		}

		// Prioritizing ctrl/meta, so commenting this out:
		// if(sym.get()) { return onSymKey(event, false) }

		if (dotCtrl.getModKey() != 0 || emojiMeta.getModKey() != 0) {
			sendKey(keyCode, event, false)
			return true
		}

		return false
	}

	/**
	 * Update the active device type from a key event so we can reference it later.
	 */
	private fun updateDeviceType(event: KeyEvent) {
		if (event.deviceId == lastDeviceId)
			return
		lastDeviceId = event.deviceId
		val device = InputDevice.getDevice(event.deviceId)
		if (device?.isVirtual == true)
			return
		deviceType = if (device?.name == "aw9523b-key") DeviceType.MP01 else DeviceType.TITAN
	}

	/**
	 * Handle a key down event when the SYM modifier is enabled.
	 */
	fun onSymKey(event: KeyEvent, pressed: Boolean): Boolean {
		val mapping = SymKeyMappings.getMapping(event.keyCode, deviceType) ?: return if (!event.isPrintingKey) {
			if (pressed) super.onKeyDown(event.keyCode, event) else super.onKeyUp(event.keyCode, event)
		} else true

		if (pressed && event.repeatCount == 0 && !event.isLongPress) {
			when (val action = mapping.action) {
				is SendKey -> sendKey(action.keyCode, event, true)
				is SendChar -> {
					val char = if (shift.get() && action.shiftedCharacter != null) action.shiftedCharacter else action.character
					sendCharacter(char)
				}
				is ShiftPress -> {
					shift.onKeyDown()
					updateStatusIconIfNeeded(true)
				}
			}
		} else if (!pressed) {
			when (val action = mapping.action) {
				is SendKey -> sendKey(action.keyCode, event, false)
				is SendChar -> { /* No action on key up for characters */ }
				is ShiftPress -> {
					shift.onKeyUp()
					updateStatusIconIfNeeded(true)
				}
			}
		}
		return true
	}

	// Event passed back to us from the Popup for sym key presses.
	fun forceSymKeyEvent(event: KeyEvent): Boolean {
		val pressed = event.action == KeyEvent.ACTION_DOWN
		if (event.keyCode == MP01_KEYCODE_DICTATE)
			return onSymKey(makeKeyEvent(event, KeyEvent.KEYCODE_PERIOD), pressed)
		if (onSymKey(event, pressed))
			return true

		return false
	}

	/**
	 * Handle keyboard shortcuts where emojiMeta is held.
	 */
	private fun onEmojiMetaShotcut(event: KeyEvent): Boolean {
		// skip the extra simulateKeyTap logic with sendDownUpKeyEvents.
		currentInputConnection?.sendKeyEvent(makeKeyEvent(event, emojiMeta.modKeyCode, 0, KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD))
		return when (event.keyCode) {
			KeyEvent.KEYCODE_V -> {
				showClipboardHistory()
				true
			}
			KeyEvent.KEYCODE_SPACE -> {
				showEmojiPicker()
				true
			}
			KeyEvent.KEYCODE_M -> {
				sendDownUpKeyEvents(KeyEvent.KEYCODE_MENU)
				true
			}
			KeyEvent.KEYCODE_Q -> {
				sendDownUpKeyEvents(KeyEvent.KEYCODE_TAB)
				true
			}
			KeyEvent.KEYCODE_DEL -> {
				sendDownUpKeyEvents(KeyEvent.KEYCODE_ESCAPE)
				true
			}
			MP01_KEYCODE_DICTATE -> {
				// TODO: latch control, even if disabled from dotCtrl.
				true
			}
			// Use intents in place of system-level key events.
			KeyEvent.KEYCODE_ENTER -> { // Home
				launchApp(Intent.ACTION_MAIN, Intent.CATEGORY_HOME)
				true
			}
			KeyEvent.KEYCODE_E -> { // Email
				launchApp(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
				true
			}
			KeyEvent.KEYCODE_A -> { // Assistant (uses a different action)
				launchApp(Intent.ACTION_ASSIST)
				true
			}
			KeyEvent.KEYCODE_C -> { // Contacts
				launchApp(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS)
				true
			}
			KeyEvent.KEYCODE_B -> { // Browser
				launchApp(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
				true
			}
			KeyEvent.KEYCODE_I -> { // Settings
				launchApp(Settings.ACTION_SETTINGS)
				true
			}
			KeyEvent.KEYCODE_P -> { // Music
				launchApp(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
				true
			}
			KeyEvent.KEYCODE_L -> { // Calendar
				launchApp(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
				true
			}
			// KeyEvent.KEYCODE_N -> // Notification shade. No standard intent for this.
			// We may be able to use an accessibility service, but it's not a priority for me.
			// Menu and Escape will only work for some apps when sent like this as well.
			else -> false
		}
	}

	/**
	 * Send a key press or release.
	 */
	private fun sendKey(code: Int, original: KeyEvent, pressed: Boolean) {
		val newState = enhancedMetaState(original)
		forceMatchMetaState(original, newState, pressed)
		currentInputConnection?.sendKeyEvent(makeKeyEvent(original, code, newState, if(pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD))
		forceMatchMetaState(original, newState, false)
	}

	/**
	 * Send a character, possibly uppercased depending on the Shift modifier.
	 */
	private fun sendCharacter(str: String, strict: Boolean = false) {
		var text = str
		if (!strict && (shift.get() || caps.get())) {
			text = text.uppercase(Locale.getDefault())
		}
		currentInputConnection?.commitText(text, 1)
	}

	private fun simulateKeyTap(code: Int, original: KeyEvent, metaState: Int) {
		if (code == KeyEvent.KEYCODE_PICTSYMBOLS) {
			showEmojiPicker()
			return
		} else if (code == KeyEvent.KEYCODE_VOICE_ASSIST) {
			startVoiceInput()
			return
		}
		val event = makeKeyEvent(original, code, metaState, original.action, original.source, original.deviceId)
		if (sym.get()) {
			onSymKey(event, true)
			onSymKey(event, false)
		} else {
			sendKey(code, event, true)
			sendKey(code, event, false)
		}
	}

	/**
	 * Forcefully match the metastate by pressing any missing modifier keys.
	 */
	private fun forceMatchMetaState(original: KeyEvent, enhanced: Int, pressed: Boolean) {
		val origMeta = original.metaState
		for ((metaOn, metaKey) in forceModifierPairs) {
			if (origMeta and metaOn == 0 && enhanced and metaOn != 0) {
				currentInputConnection?.sendKeyEvent(makeKeyEvent(original, metaKey, enhanced, if(pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, InputDevice.SOURCE_KEYBOARD))
			}
		}
	}

	/**
	 * Make the device vibrate.
	 */
	private fun vibrate() {
		vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
	}

	fun updateModStateIcon() {
		updateStatusIconIfNeeded(true)
	}

	/**
	 * Update the icon in the status bar according to modifier states.
	 */
	private fun updateStatusIconIfNeeded(force: Boolean = false) {
		val shiftState = shift.get()
		val altState = alt.get()
		val symState = sym.get()
		val ctrlState = dotCtrl.get()
		val capsState = caps.get()
		val metaState = emojiMeta.get()
		if(force || symState != lastSym || altState != lastAlt || shiftState != lastShift || capsState != lastCaps || ctrlState != lastDotCtrl || metaState != lastEmojiMeta) {
			if(sym.get()) {
				if (shift.get()) {
					showStatusIcon(R.drawable.symshift)
				} else {
					showStatusIcon(R.drawable.sym)
				}
			} else if(emojiMeta.get()) {
				showStatusIcon(R.drawable.meta)
			} else if(alt.get()) {
				showStatusIcon(if (alt.isLocked()) R.drawable.altlock else R.drawable.alt)
			} else if (dotCtrl.get()) {
				showStatusIcon(if (dotCtrl.isLocked()) R.drawable.ctrllock else R.drawable.ctrl)
			} else if(shift.get()) {
				showStatusIcon(if(shift.isLocked()) R.drawable.shiftlock else R.drawable.shift)
			} else if(caps.get()) {
				showStatusIcon(if(caps.isLocked()) R.drawable.capslock else R.drawable.caps)
			} else {
				hideStatusIcon()
			}
		}
		lastShift = shiftState
		lastAlt = altState
		lastSym = symState
		lastDotCtrl = ctrlState
		lastCaps = capsState
		lastEmojiMeta = metaState
	}

	override fun showStatusIcon(iconResId: Int) {
		super.showStatusIcon(iconResId)
		if (iconResId == R.drawable.capslock || iconResId == R.drawable.caps) {
			updateStripLanguageText()
			return
		}
		val typedValue = TypedValue()
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
		val color = ContextCompat.getColor(this, typedValue.resourceId)

		// Get the drawable and apply the tint
		val originalDrawable = ContextCompat.getDrawable(this, iconResId)
		val wrappedDrawable = DrawableCompat.wrap(originalDrawable!!.mutate())
		DrawableCompat.setTint(wrappedDrawable, color)
		stripStatusIcon?.setImageDrawable(wrappedDrawable)
		stripStatusIcon?.visibility = View.VISIBLE
		stripLanguageText?.visibility = View.GONE
	}

	override fun hideStatusIcon() {
		super.hideStatusIcon()
		updateStripLanguageText()
	}

	private fun updateStripLanguageText() {
		if (caps.isLocked()) {
			stripLanguageText?.text = "EN"
		} else if (caps.get()) {
			stripLanguageText?.text = "En"
		} else {
			stripLanguageText?.text = "en"
		}
		stripStatusIcon?.visibility = View.GONE
		stripLanguageText?.visibility = View.VISIBLE
	}

	/**
	 * Update the Shift modifier state for auto-capitalization.
	 */
	private fun updateAutoCapitalization() {
		if(!autoCapitalize) {
			return
		}
		if(currentInputEditorInfo == null || currentInputConnection == null) {
			return
		}

		if(currentInputConnection.getCursorCapsMode(TextUtils.CAP_MODE_SENTENCES) > 0 && canUseSuggestions(currentInputEditorInfo)) {
			caps.activateForNext()
			updateStatusIconIfNeeded()
		}
	}

	/**
	 * Inform modifiers that the "next" key press has been consumed.
	 */
	private fun consumeModifierNext() {
		shift.nextDidConsume()
		alt.nextDidConsume()
		caps.nextDidConsume()
		dotCtrl.nextDidConsume()
		updateStatusIconIfNeeded()
	}

	/**
	 * @return The metaState of the given event, enhanced with our own modifiers.
	 */
	private fun enhancedMetaState(original: KeyEvent): Int {
		var metaState = original.metaState
		if(shift.get()) {
			metaState = metaState or KeyEvent.META_SHIFT_ON
		}
		if(caps.get()) {
			metaState = metaState or KeyEvent.META_CAPS_LOCK_ON
		}
		if(alt.get()) {
			metaState = metaState or KeyEvent.META_ALT_ON
		}
		if (dotCtrl.getModKey() != 0) {
			metaState = metaState or KeyEvent.META_CTRL_ON
		}
		if (emojiMeta.getModKey() != 0) {
			metaState = metaState or KeyEvent.META_META_ON
		}
		// Strip the sym state if it is pressed.
		return metaState and KeyEvent.META_SYM_ON.inv()
	}

	/**
	 * Handle what happens when the SYM modifier has possibly changed.
	 */
	private fun onSymPossiblyChanged() {
		if(sym.get() && !lastSym) {
			if(shift.get() && !shift.isHeld()) {
				shift.reset()
			}
		} else if(!sym.get() && lastSym) {
			updateAutoCapitalization()
		}
	}

	/**
	 * Update values from the preferences.
	 */
	private fun updateFromPreferences() {
		val context = createDeviceProtectedStorageContext()
		val preferences = PreferenceManager.getDefaultSharedPreferences(context)

		pickerManager?.setPickerMode(preferences.getBoolean("pref_inline_picker", false))
		autoCapitalize = preferences.getBoolean("AutoCapitalize", true)

		val lockThreshold = preferences.getInt("ModifierLockThreshold", 250)
		shift.lockThreshold = lockThreshold
		alt.lockThreshold = lockThreshold
		sym.lockThreshold = lockThreshold

		val nextThreshold = preferences.getInt("ModifierNextThreshold", 350)
		shift.nextThreshold = nextThreshold
		alt.nextThreshold = nextThreshold

		multipress.multipressThreshold = preferences.getInt("MultipressThreshold", 750)
		multipress.ignoreFirstLevel = !preferences.getBoolean("UseFirstLevel", true)
		multipress.ignoreDotSpace = !preferences.getBoolean("DotSpace", true)
		multipress.ignoreConsonantsOnFirstLevel = preferences.getBoolean("FirstLevelOnlyVowels", false)

		val templateId = preferences.getString("FirstLevelTemplate", "fr")
		if(templates.containsKey(templateId)) {
			multipress.substitutions[0] = templates[templateId]!!
		}

		dotCtrl.shortPressKeyCode = preferenceToKeyCode(preferences.getString("pref_dotctrl_tap", "period"))
		dotCtrl.longPressKeyCode = preferenceToKeyCode(preferences.getString("pref_dotctrl_long_press", "voice"))
		dotCtrl.modKeyCode = preferenceToKeyCode(preferences.getString("pref_dotctrl_hold", "ctrl"))

		emojiMeta.shortPressKeyCode = preferenceToKeyCode(preferences.getString("pref_emojimeta_tap", "emoji"))
		emojiMeta.longPressKeyCode = preferenceToKeyCode(preferences.getString("pref_emojimeta_long_press", "0"))
		emojiMeta.modKeyCode = preferenceToKeyCode(preferences.getString("pref_emojimeta_hold", "meta"))
	}

	private fun preferenceToKeyCode(preferenceValue: String?): Int {
		return when (preferenceValue) {
			"period" -> KeyEvent.KEYCODE_PERIOD
			"voice" -> KeyEvent.KEYCODE_VOICE_ASSIST
			"ctrl" -> KeyEvent.KEYCODE_CTRL_RIGHT
			"emoji" -> KeyEvent.KEYCODE_PICTSYMBOLS
			"0" -> KeyEvent.KEYCODE_0
			"meta" -> KeyEvent.KEYCODE_META_LEFT
			else -> 0 // "none" or any other value
		}
	}

	private fun startVoiceInput() {
		val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		val token = window.window?.attributes?.token ?: return

		val voiceImeId = findVoiceIme()
		if (voiceImeId != null) {
			imm.setInputMethod(token, voiceImeId)
		}
	}

	private fun findVoiceIme(): String? {
		val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		for (imi in imm.enabledInputMethodList) {
			for (i in 0 until imi.subtypeCount) {
				val subtype = imi.getSubtypeAt(i)
				if (subtype.mode == "voice") {
					return imi.id
				}
			}
		}
		return null
	}

	/**
	 * Launches an application using an Intent.
	 */
	private fun launchApp(action: String, category: String? = null) {
		val intent = Intent(action)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		if (category != null) {
			intent.addCategory(category)
		}
		try {
			startActivity(intent)
		} catch (e: Exception) {
			// Handle cases where the app isn't found or another error occurs
			e.printStackTrace()
		}
	}

	fun clearModifiers() {
		shift.reset()
		alt.reset()
		sym.reset()
		dotCtrl.reset()
		emojiMeta.reset()
		caps.reset()
		updateStatusIconIfNeeded(true)
	}
}