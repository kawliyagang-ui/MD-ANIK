package com.example.ui.player

import android.content.SharedPreferences
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleStylePrefsTest {

    @Test
    fun defaults_matchRequiredDefaultConfiguration() {
        val defaults = SubtitleStylePrefs()
        assertEquals(SubtitleTextSize.DEFAULT, defaults.textSize)
        assertEquals(SubtitleTextColor.WHITE, defaults.textColor)
        assertEquals(SubtitleBackground.TRANSLUCENT_BLACK, defaults.background)
        assertEquals(SubtitleEdgeStyle.OUTLINE, defaults.edgeStyle)
        assertEquals(SubtitlePosition.BOTTOM, defaults.position)

        val mapped = SubtitleStyleMapper.map(defaults)
        assertEquals(1.0f, mapped.sizeMultiplier, 0.0001f)
        assertEquals(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION, mapped.fractionalTextSize, 0.0001f)
        assertEquals(0xFFFFFFFF.toInt(), mapped.captionStyle.foregroundColor)
        assertEquals(0x99000000.toInt(), mapped.captionStyle.backgroundColor)
        assertEquals(CaptionStyleCompat.EDGE_TYPE_OUTLINE, mapped.captionStyle.edgeType)
        assertEquals(0xFF000000.toInt(), mapped.captionStyle.edgeColor)
        assertEquals(0.08f, mapped.bottomPaddingFraction, 0.0001f)
    }

    @Test
    fun sizeMapping_producesExpectedFractionsForAllSizes() {
        val expected = mapOf(
            SubtitleTextSize.SMALL to 0.7f,
            SubtitleTextSize.DEFAULT to 1.0f,
            SubtitleTextSize.LARGE to 1.3f,
            SubtitleTextSize.EXTRA_LARGE to 1.7f
        )

        expected.forEach { (size, fraction) ->
            val mapped = SubtitleStyleMapper.map(SubtitleStylePrefs(textSize = size))
            assertEquals(fraction, mapped.sizeMultiplier, 0.0001f)
            assertEquals(
                SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * fraction,
                mapped.fractionalTextSize,
                0.0001f
            )
        }
    }

    @Test
    fun colorMapping_producesExpectedCaptionStyleCompatForegroundColors() {
        val expected = mapOf(
            SubtitleTextColor.WHITE to 0xFFFFFFFF.toInt(),
            SubtitleTextColor.YELLOW to 0xFFFFFF00.toInt(),
            SubtitleTextColor.CYAN to 0xFF00FFFF.toInt(),
            SubtitleTextColor.GREEN to 0xFF00FF00.toInt(),
            SubtitleTextColor.RED to 0xFFFF0000.toInt(),
            SubtitleTextColor.BLACK to 0xFF000000.toInt()
        )

        expected.forEach { (color, argb) ->
            val mapped = SubtitleStyleMapper.map(SubtitleStylePrefs(textColor = color))
            assertEquals(argb, mapped.captionStyle.foregroundColor)
        }
    }

    @Test
    fun backgroundMapping_producesExpectedCaptionStyleCompatBackgroundColors() {
        val expected = mapOf(
            SubtitleBackground.NONE to 0x00000000,
            SubtitleBackground.TRANSLUCENT_BLACK to 0x99000000.toInt(),
            SubtitleBackground.SOLID_BLACK to 0xFF000000.toInt(),
            SubtitleBackground.SOLID_WHITE to 0xFFFFFFFF.toInt()
        )

        expected.forEach { (bg, argb) ->
            val mapped = SubtitleStyleMapper.map(SubtitleStylePrefs(background = bg))
            assertEquals(argb, mapped.captionStyle.backgroundColor)
        }
    }

    @Test
    fun edgeMapping_producesExpectedCaptionStyleCompatEdgeTypesAndColors() {
        val outline = SubtitleStyleMapper.map(SubtitleStylePrefs(edgeStyle = SubtitleEdgeStyle.OUTLINE))
        assertEquals(CaptionStyleCompat.EDGE_TYPE_OUTLINE, outline.captionStyle.edgeType)
        assertEquals(0xFF000000.toInt(), outline.captionStyle.edgeColor)

        val shadow = SubtitleStyleMapper.map(SubtitleStylePrefs(edgeStyle = SubtitleEdgeStyle.DROP_SHADOW))
        assertEquals(CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, shadow.captionStyle.edgeType)
        assertEquals(0xFF000000.toInt(), shadow.captionStyle.edgeColor)

        val none = SubtitleStyleMapper.map(SubtitleStylePrefs(edgeStyle = SubtitleEdgeStyle.NONE))
        assertEquals(CaptionStyleCompat.EDGE_TYPE_NONE, none.captionStyle.edgeType)
        assertEquals(0x00000000, none.captionStyle.edgeColor)
    }

    @Test
    fun positionMapping_raisedSitsHigherThanBottom() {
        val bottom = SubtitleStyleMapper.map(SubtitleStylePrefs(position = SubtitlePosition.BOTTOM))
        val raised = SubtitleStyleMapper.map(SubtitleStylePrefs(position = SubtitlePosition.RAISED))
        assertEquals(0.08f, bottom.bottomPaddingFraction, 0.0001f)
        assertEquals(0.22f, raised.bottomPaddingFraction, 0.0001f)
        assertTrue(raised.bottomPaddingFraction > bottom.bottomPaddingFraction)
    }

    @Test
    fun store_persistsAndRestoresAllFivePreferences() {
        val fakePrefs = InMemorySharedPreferences()
        val store = SubtitleStyleStore(fakePrefs)
        assertEquals(SubtitleStylePrefs(), store.current)

        val custom = SubtitleStylePrefs(
            textSize = SubtitleTextSize.LARGE,
            textColor = SubtitleTextColor.CYAN,
            background = SubtitleBackground.SOLID_BLACK,
            edgeStyle = SubtitleEdgeStyle.DROP_SHADOW,
            position = SubtitlePosition.RAISED
        )
        store.save(custom)
        assertEquals(custom, store.current)

        val reloadedStore = SubtitleStyleStore(fakePrefs)
        assertEquals(custom, reloadedStore.current)
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            (data[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            defValues
        override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl(data)
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        private class EditorImpl(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = values
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = null
            }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearAll) target.clear()
                pending.forEach { (k, v) ->
                    if (v == null) target.remove(k) else target[k] = v
                }
            }
        }
    }
}
