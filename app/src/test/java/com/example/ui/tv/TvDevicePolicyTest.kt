package com.example.ui.tv

import android.content.res.Configuration
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvDevicePolicyTest {

    @Test
    fun `isTvDevice returns true for television uiMode even without leanback flag`() {
        assertTrue(
            TvDevicePolicy.isTvDevice(
                uiModeType = Configuration.UI_MODE_TYPE_TELEVISION,
                hasLeanbackFeature = false
            )
        )
    }

    @Test
    fun `isTvDevice returns true when leanback system feature is present`() {
        assertTrue(
            TvDevicePolicy.isTvDevice(
                uiModeType = Configuration.UI_MODE_TYPE_NORMAL,
                hasLeanbackFeature = true
            )
        )
    }

    @Test
    fun `isTvDevice returns false for standard handheld phone`() {
        assertFalse(
            TvDevicePolicy.isTvDevice(
                uiModeType = Configuration.UI_MODE_TYPE_NORMAL,
                hasLeanbackFeature = false
            )
        )
    }

    @Test
    fun `focus memory restores exact item and resets pending flag`() {
        val memory = TvHomeFocusMemory()
        memory.recordSelection(rowKey = "trending", itemId = "m_42", itemIndex = 3)
        assertFalse(memory.pendingRestore)

        memory.markRestorePending()
        assertTrue(memory.pendingRestore)

        val rows = mapOf(
            "trending" to listOf("m_10", "m_20", "m_42", "m_99")
        )
        val restored = memory.consumeRestoreTarget(rows)
        assertEquals(TvFocusTarget(rowKey = "trending", itemId = "m_42", itemIndex = 2), restored)
        assertFalse(memory.pendingRestore)
        assertNull(memory.consumeRestoreTarget(rows))
    }

    @Test
    fun `focus memory clamps to last available index when item disappeared`() {
        val memory = TvHomeFocusMemory()
        memory.recordSelection(rowKey = "movies", itemId = "removed_id", itemIndex = 8)
        memory.markRestorePending()

        val restored = memory.consumeRestoreTarget(
            mapOf("movies" to listOf("m_1", "m_2"))
        )
        assertEquals(TvFocusTarget(rowKey = "movies", itemId = "m_2", itemIndex = 1), restored)
    }

    @Test
    fun `back policy prioritizes overlays before back stack`() {
        assertEquals(
            TvBackAction.CLOSE_OVERLAY,
            TvBackPolicy.resolveBackAction(
                hasOpenSheetOrMenu = true,
                hasOpenTrailer = false,
                backStackSize = 3
            )
        )
        assertEquals(
            TvBackAction.CLOSE_OVERLAY,
            TvBackPolicy.resolveBackAction(
                hasOpenSheetOrMenu = false,
                hasOpenTrailer = true,
                backStackSize = 2
            )
        )
        assertEquals(
            TvBackAction.NAVIGATE_BACK,
            TvBackPolicy.resolveBackAction(
                hasOpenSheetOrMenu = false,
                hasOpenTrailer = false,
                backStackSize = 2
            )
        )
        assertEquals(
            TvBackAction.EXIT_APP,
            TvBackPolicy.resolveBackAction(
                hasOpenSheetOrMenu = false,
                hasOpenTrailer = false,
                backStackSize = 1
            )
        )
    }

    @Test
    fun `player key policy handles media keys dpad reveal and center play pause`() {
        assertEquals(
            TvPlayerKeyCommand.TOGGLE_PLAY_PAUSE,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                controlsVisible = false,
                hasFocusInControls = false
            )
        )
        assertEquals(
            TvPlayerKeyCommand.PLAY,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY,
                controlsVisible = true,
                hasFocusInControls = true
            )
        )
        assertEquals(
            TvPlayerKeyCommand.PAUSE,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE,
                controlsVisible = true,
                hasFocusInControls = true
            )
        )
        assertEquals(
            TvPlayerKeyCommand.SEEK_FORWARD_10S,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                controlsVisible = false,
                hasFocusInControls = false
            )
        )
        assertEquals(
            TvPlayerKeyCommand.SEEK_BACKWARD_10S,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_MEDIA_REWIND,
                controlsVisible = false,
                hasFocusInControls = false
            )
        )
        assertEquals(
            TvPlayerKeyCommand.REVEAL_CONTROLS_AND_FOCUS,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                controlsVisible = false,
                hasFocusInControls = false
            )
        )
        assertEquals(
            TvPlayerKeyCommand.NONE,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                controlsVisible = true,
                hasFocusInControls = true
            )
        )
        assertEquals(
            TvPlayerKeyCommand.TOGGLE_PLAY_PAUSE,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                controlsVisible = false,
                hasFocusInControls = false
            )
        )
        assertEquals(
            TvPlayerKeyCommand.NONE,
            TvPlayerKeyPolicy.resolveCommand(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                controlsVisible = true,
                hasFocusInControls = true
            )
        )
    }
}
