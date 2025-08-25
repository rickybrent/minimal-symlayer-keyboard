package io.github.rickybrent.minimal_symlayer_keyboard

import org.junit.Test
import org.junit.Assert.*

/**
 * Test class for Cyrillic layer functionality
 */
class CyrillicLayerTest {
	
	@Test
	fun testCyrillicLayerModifier() {
		val modifier = CyrillicLayerModifier()
		
		// Initially should be inactive
		assertFalse(modifier.isActive())
		
		// Press right shift briefly - should not activate
		modifier.onRightShiftDown()
		Thread.sleep(100) // Short press
		modifier.onRightShiftUp()
		assertFalse(modifier.isActive())
		
		// Press right shift for long duration - should activate Cyrillic layer
		modifier.onRightShiftDown()
		Thread.sleep(600) // Long press (>500ms threshold)
		modifier.onRightShiftUp()
		assertTrue(modifier.isActive())
		assertTrue(modifier.wasJustToggled())
		
		// Long press right shift again - should deactivate
		modifier.onRightShiftDown()
		Thread.sleep(600) // Long press
		modifier.onRightShiftUp()
		assertFalse(modifier.isActive())
		assertTrue(modifier.wasJustToggled())
		
		// Reset should deactivate
		modifier.reset()
		assertFalse(modifier.isActive())
	}
	
	@Test
	fun testCyrillicMappings() {
		// Test lowercase mappings
		assertEquals('а', CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_A, false))
		assertEquals('б', CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_B, false))
		assertEquals('в', CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_V, false))
		
		// Test uppercase mappings
		assertEquals('А', CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_A, true))
		assertEquals('Б', CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_B, true))
		assertEquals('В', CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_V, true))
		
		// Test non-existent mappings
		assertNull(CyrillicMappings.getCyrillicChar(android.view.KeyEvent.KEYCODE_F1, false))
		
		// Test hasCyrillicMapping
		assertTrue(CyrillicMappings.hasCyrillicMapping(android.view.KeyEvent.KEYCODE_A))
		assertFalse(CyrillicMappings.hasCyrillicMapping(android.view.KeyEvent.KEYCODE_F1))
	}
}
