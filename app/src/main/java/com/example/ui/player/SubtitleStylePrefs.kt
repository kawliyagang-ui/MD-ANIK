package com.example.ui.player

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubtitleTextSize(
    val storageKey: String,
    val label: String,
    val fraction: Float
) {
    SMALL("small", "Small", 0.7f),
    DEFAULT("default", "Default", 1.0f),
    LARGE("large", "Large", 1.3f),
    EXTRA_LARGE("extra_large", "Extra Large", 1.7f);

    companion object {
        fun fromKey(raw: String?): SubtitleTextSize =
            entries.firstOrNull { it.storageKey.equals(raw?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

enum class SubtitleTextColor(
    val storageKey: String,
    val label: String,
    val argb: Int
) {
    WHITE("white", "White", 0xFFFFFFFF.toInt()),
    YELLOW("yellow", "Yellow", 0xFFFFFF00.toInt()),
    CYAN("cyan", "Cyan", 0xFF00FFFF.toInt()),
    GREEN("green", "Green", 0xFF00FF00.toInt()),
    RED("red", "Red", 0xFFFF0000.toInt()),
    BLACK("black", "Black", 0xFF000000.toInt());

    companion object {
        fun fromKey(raw: String?): SubtitleTextColor =
            entries.firstOrNull { it.storageKey.equals(raw?.trim(), ignoreCase = true) } ?: WHITE
    }
}

enum class SubtitleBackground(
    val storageKey: String,
    val label: String,
    val argb: Int
) {
    NONE("none", "None", 0x00000000),
    TRANSLUCENT_BLACK("translucent_black", "Translucent Black", 0x99000000.toInt()),
    SOLID_BLACK("solid_black", "Solid Black", 0xFF000000.toInt()),
    SOLID_WHITE("solid_white", "Solid White", 0xFFFFFFFF.toInt());

    companion object {
        fun fromKey(raw: String?): SubtitleBackground =
            entries.firstOrNull { it.storageKey.equals(raw?.trim(), ignoreCase = true) } ?: TRANSLUCENT_BLACK
    }
}

@OptIn(UnstableApi::class)
enum class SubtitleEdgeStyle(
    val storageKey: String,
    val label: String,
    val edgeType: Int,
    val edgeColorArgb: Int
) {
    OUTLINE("outline", "Outline", CaptionStyleCompat.EDGE_TYPE_OUTLINE, 0xFF000000.toInt()),
    DROP_SHADOW("drop_shadow", "Drop Shadow", CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, 0xFF000000.toInt()),
    NONE("none", "None", CaptionStyleCompat.EDGE_TYPE_NONE, 0x00000000);

    companion object {
        fun fromKey(raw: String?): SubtitleEdgeStyle =
            entries.firstOrNull { it.storageKey.equals(raw?.trim(), ignoreCase = true) } ?: OUTLINE
    }
}

enum class SubtitlePosition(
    val storageKey: String,
    val label: String,
    val bottomPaddingFraction: Float
) {
    BOTTOM("bottom", "Bottom", 0.08f),
    RAISED("raised", "Raised", 0.22f);

    companion object {
        fun fromKey(raw: String?): SubtitlePosition =
            entries.firstOrNull { it.storageKey.equals(raw?.trim(), ignoreCase = true) } ?: BOTTOM
    }
}

data class SubtitleStylePrefs(
    val textSize: SubtitleTextSize = SubtitleTextSize.DEFAULT,
    val textColor: SubtitleTextColor = SubtitleTextColor.WHITE,
    val background: SubtitleBackground = SubtitleBackground.TRANSLUCENT_BLACK,
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.OUTLINE,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM
)

@OptIn(UnstableApi::class)
data class MappedSubtitleStyle(
    val captionStyle: CaptionStyleCompat,
    val sizeMultiplier: Float,
    val fractionalTextSize: Float,
    val bottomPaddingFraction: Float
)

@OptIn(UnstableApi::class)
object SubtitleStyleMapper {
    const val BASE_TEXT_SIZE_FRACTION: Float = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION

    fun map(prefs: SubtitleStylePrefs): MappedSubtitleStyle {
        val captionStyle = CaptionStyleCompat(
            prefs.textColor.argb,
            prefs.background.argb,
            0x00000000,
            prefs.edgeStyle.edgeType,
            prefs.edgeStyle.edgeColorArgb,
            null
        )
        return MappedSubtitleStyle(
            captionStyle = captionStyle,
            sizeMultiplier = prefs.textSize.fraction,
            fractionalTextSize = BASE_TEXT_SIZE_FRACTION * prefs.textSize.fraction,
            bottomPaddingFraction = prefs.position.bottomPaddingFraction
        )
    }

    fun applyToPlayerView(playerView: PlayerView, prefs: SubtitleStylePrefs) {
        val subtitleView = playerView.subtitleView ?: return
        val mapped = map(prefs)
        subtitleView.setStyle(mapped.captionStyle)
        subtitleView.setFractionalTextSize(mapped.fractionalTextSize)
        subtitleView.setBottomPaddingFraction(mapped.bottomPaddingFraction)
    }
}

class SubtitleStyleStore(
    private val sharedPrefs: SharedPreferences
) {
    private val _state = MutableStateFlow(loadFromPrefs())
    val state: StateFlow<SubtitleStylePrefs> = _state.asStateFlow()

    val current: SubtitleStylePrefs
        get() = _state.value

    fun save(prefs: SubtitleStylePrefs) {
        sharedPrefs.edit()
            .putString(KEY_TEXT_SIZE, prefs.textSize.storageKey)
            .putString(KEY_TEXT_COLOR, prefs.textColor.storageKey)
            .putString(KEY_BACKGROUND, prefs.background.storageKey)
            .putString(KEY_EDGE_STYLE, prefs.edgeStyle.storageKey)
            .putString(KEY_POSITION, prefs.position.storageKey)
            .apply()
        _state.value = prefs
    }

    fun update(transform: (SubtitleStylePrefs) -> SubtitleStylePrefs) {
        save(transform(_state.value))
    }

    private fun loadFromPrefs(): SubtitleStylePrefs {
        return SubtitleStylePrefs(
            textSize = SubtitleTextSize.fromKey(sharedPrefs.getString(KEY_TEXT_SIZE, null)),
            textColor = SubtitleTextColor.fromKey(sharedPrefs.getString(KEY_TEXT_COLOR, null)),
            background = SubtitleBackground.fromKey(sharedPrefs.getString(KEY_BACKGROUND, null)),
            edgeStyle = SubtitleEdgeStyle.fromKey(sharedPrefs.getString(KEY_EDGE_STYLE, null)),
            position = SubtitlePosition.fromKey(sharedPrefs.getString(KEY_POSITION, null))
        )
    }

    companion object {
        const val PREFS_NAME = "subtitle_style"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_BACKGROUND = "background"
        private const val KEY_EDGE_STYLE = "edge_style"
        private const val KEY_POSITION = "position"

        @Volatile
        private var instance: SubtitleStyleStore? = null

        fun get(context: Context): SubtitleStyleStore {
            return instance ?: synchronized(this) {
                instance ?: SubtitleStyleStore(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
