package io.github.rickybrent.minimal_symlayer_keyboard

import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.lang.reflect.Proxy

class HangulComposerTest {

	@Test
	fun commitComposingText_finalizesSyllableBeforeFollowingDot() {
		val composer = HangulComposer()
		val recording = RecordingInputConnection()

		composer.inputLatinChar('r', recording.connection)
		composer.inputLatinChar('k', recording.connection)
		composer.commitComposingText(recording.connection)
		recording.connection.commitText(".", 1)

		assertEquals("가.", recording.committedText.toString())
		assertFalse(composer.isComposing())
		assertEquals(
			listOf(
				"set:ㄱ",
				"set:가",
				"commit:가",
				"finish",
				"commit:."
			),
			recording.events
		)
	}

	@Test
	fun reset_discardsComposingTextInsteadOfCommittingIt() {
		val composer = HangulComposer()
		val recording = RecordingInputConnection()

		composer.inputLatinChar('r', recording.connection)
		composer.inputLatinChar('k', recording.connection)
		composer.reset(recording.connection)
		recording.connection.commitText(".", 1)

		assertEquals(".", recording.committedText.toString())
		assertFalse(composer.isComposing())
		assertEquals(
			listOf(
				"set:ㄱ",
				"set:가",
				"finish",
				"commit:."
			),
			recording.events
		)
	}

	private class RecordingInputConnection {
		val events = mutableListOf<String>()
		val committedText = StringBuilder()

		val connection: InputConnection = Proxy.newProxyInstance(
			InputConnection::class.java.classLoader,
			arrayOf(InputConnection::class.java)
		) { _, method, args ->
			when (method.name) {
				"setComposingText" -> {
					events += "set:${args[0] as CharSequence}"
					true
				}
				"commitText" -> {
					val text = args[0] as CharSequence
					events += "commit:$text"
					committedText.append(text)
					true
				}
				"finishComposingText" -> {
					events += "finish"
					true
				}
				"toString" -> "RecordingInputConnection"
				else -> defaultValue(method.returnType)
			}
		} as InputConnection

		private fun defaultValue(returnType: Class<*>): Any? {
			return when (returnType) {
				java.lang.Boolean.TYPE -> false
				java.lang.Byte.TYPE -> 0.toByte()
				java.lang.Short.TYPE -> 0.toShort()
				java.lang.Integer.TYPE -> 0
				java.lang.Long.TYPE -> 0L
				java.lang.Float.TYPE -> 0f
				java.lang.Double.TYPE -> 0.0
				java.lang.Character.TYPE -> 0.toChar()
				else -> null
			}
		}
	}
}
