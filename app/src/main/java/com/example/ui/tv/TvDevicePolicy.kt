package com.example.ui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.KeyEvent

/**
 * Pure TV device detection and TV remote / focus-restoration policies.
 */
object TvDevicePolicy {
    fun isTvDevice(uiModeType: Int, hasLeanbackFeature: Boolean): Boolean {
        return uiModeType == Configuration.UI_MODE_TYPE_TELEVISION || hasLeanbackFeature
    }

    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val uiModeType = uiModeManager?.currentModeType
            ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK)
        val hasLeanback = runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }.getOrDefault(false)
        return isTvDevice(uiModeType = uiModeType, hasLeanbackFeature = hasLeanback)
    }
}

data class TvFocusTarget(
    val rowKey: String,
    val itemId: String,
    val itemIndex: Int = 0
)

/**
 * Remembers the user's last selected shelf & item on TV Home so pressing Back from Details
 * restores focus to the exact poster card instead of jumping back to the top hero.
 */
class TvHomeFocusMemory {
    var lastTarget: TvFocusTarget? = null
        private set
    var pendingRestore: Boolean = false
        private set

    fun recordSelection(rowKey: String, itemId: String, itemIndex: Int) {
        lastTarget = TvFocusTarget(
            rowKey = rowKey,
            itemId = itemId,
            itemIndex = itemIndex.coerceAtLeast(0)
        )
        pendingRestore = false
    }

    fun markRestorePending() {
        if (lastTarget != null) {
            pendingRestore = true
        }
    }

    fun consumeRestoreTarget(availableRows: Map<String, List<String>>): TvFocusTarget? {
        if (!pendingRestore) return null
        pendingRestore = false
        val target = lastTarget ?: return null
        val rowIds = availableRows[target.rowKey].orEmpty()
        if (rowIds.isEmpty()) return null
        val exactIndex = rowIds.indexOf(target.itemId)
        return if (exactIndex >= 0) {
            target.copy(itemIndex = exactIndex)
        } else {
            val clampedIndex = target.itemIndex.coerceIn(0, rowIds.lastIndex)
            TvFocusTarget(
                rowKey = target.rowKey,
                itemId = rowIds[clampedIndex],
                itemIndex = clampedIndex
            )
        }
    }

    fun clear() {
        lastTarget = null
        pendingRestore = false
    }
}

enum class TvBackAction {
    CLOSE_OVERLAY,
    NAVIGATE_BACK,
    EXIT_APP
}

object TvBackPolicy {
    fun resolveBackAction(
        hasOpenSheetOrMenu: Boolean,
        hasOpenTrailer: Boolean,
        backStackSize: Int
    ): TvBackAction {
        return when {
            hasOpenSheetOrMenu || hasOpenTrailer -> TvBackAction.CLOSE_OVERLAY
            backStackSize > 1 -> TvBackAction.NAVIGATE_BACK
            else -> TvBackAction.EXIT_APP
        }
    }
}

enum class TvPlayerKeyCommand {
    TOGGLE_PLAY_PAUSE,
    PLAY,
    PAUSE,
    SEEK_FORWARD_10S,
    SEEK_BACKWARD_10S,
    REVEAL_CONTROLS_AND_FOCUS,
    NONE
}

object TvPlayerKeyPolicy {
    fun resolveCommand(
        keyCode: Int,
        controlsVisible: Boolean,
        hasFocusInControls: Boolean
    ): TvPlayerKeyCommand {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> TvPlayerKeyCommand.TOGGLE_PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY -> TvPlayerKeyCommand.PLAY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> TvPlayerKeyCommand.PAUSE
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> TvPlayerKeyCommand.SEEK_FORWARD_10S
            KeyEvent.KEYCODE_MEDIA_REWIND -> TvPlayerKeyCommand.SEEK_BACKWARD_10S
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE -> {
                if (!controlsVisible || !hasFocusInControls) {
                    TvPlayerKeyCommand.TOGGLE_PLAY_PAUSE
                } else {
                    TvPlayerKeyCommand.NONE
                }
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!controlsVisible) {
                    TvPlayerKeyCommand.REVEAL_CONTROLS_AND_FOCUS
                } else {
                    TvPlayerKeyCommand.NONE
                }
            }
            else -> TvPlayerKeyCommand.NONE
        }
    }
}
