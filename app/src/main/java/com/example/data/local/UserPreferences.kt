package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguageOption
import com.example.data.model.PlayerServer
import com.example.ui.theme.AccentColorOption
import com.example.ui.theme.DarkThemePalette
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinetra_user_prefs"
)

data class NotificationPreferences(
    val enabled: Boolean = true,
    val newEpisodesEnabled: Boolean = true,
    val newMoviesEnabled: Boolean = true
)

class UserPreferences(private val context: Context) {

    private val darkPaletteKey = stringPreferencesKey("dark_theme_palette")
    private val preferredHighReadabilityKey = stringPreferencesKey("preferred_high_readability_palette")
    private val defaultServerKey = stringPreferencesKey("default_player_server")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val accentColorKey = stringPreferencesKey("accent_color_option")
    private val appLanguageKey = stringPreferencesKey("app_language_code")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val notifyNewEpisodesKey = booleanPreferencesKey("notify_new_episodes")
    private val notifyNewMoviesKey = booleanPreferencesKey("notify_new_movies")

    val darkThemePaletteFlow: Flow<DarkThemePalette> = context.userPrefsDataStore.data
        .map { prefs -> DarkThemePalette.fromKey(prefs[darkPaletteKey]) }

    val preferredHighReadabilityPaletteFlow: Flow<DarkThemePalette> = context.userPrefsDataStore.data
        .map { prefs ->
            val candidate = DarkThemePalette.fromKey(prefs[preferredHighReadabilityKey])
            if (candidate.isHighReadability) candidate else DarkThemePalette.CRISP_SLATE
        }

    val defaultPlayerServerFlow: Flow<PlayerServer> = context.userPrefsDataStore.data
        .map { prefs ->
            val savedName = prefs[defaultServerKey]
            PlayerServer.entries.firstOrNull { it.name == savedName } ?: PlayerServer.VIDLY
        }

    val themeModeFlow: Flow<ThemeMode> = context.userPrefsDataStore.data
        .map { prefs -> ThemeMode.fromKey(prefs[themeModeKey]) }

    val accentColorFlow: Flow<AccentColorOption> = context.userPrefsDataStore.data
        .map { prefs -> AccentColorOption.fromKey(prefs[accentColorKey]) }

    val appLanguageFlow: Flow<AppLanguageOption> = context.userPrefsDataStore.data
        .map { prefs -> AppLanguageOption.fromCode(prefs[appLanguageKey]) }

    val notificationPreferencesFlow: Flow<NotificationPreferences> = context.userPrefsDataStore.data
        .map { prefs ->
            NotificationPreferences(
                enabled = prefs[notificationsEnabledKey] ?: true,
                newEpisodesEnabled = prefs[notifyNewEpisodesKey] ?: true,
                newMoviesEnabled = prefs[notifyNewMoviesKey] ?: true
            )
        }

    suspend fun saveDarkThemePalette(palette: DarkThemePalette) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[darkPaletteKey] = palette.storageKey
            if (palette.isHighReadability) {
                prefs[preferredHighReadabilityKey] = palette.storageKey
            }
        }
    }

    suspend fun saveDefaultPlayerServer(server: PlayerServer) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[defaultServerKey] = server.name
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.storageKey
        }
    }

    suspend fun saveAccentColor(option: AccentColorOption) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[accentColorKey] = option.storageKey
        }
    }

    suspend fun saveAppLanguage(language: AppLanguageOption) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[appLanguageKey] = language.tmdbCode
        }
    }

    suspend fun saveNotificationPreferences(prefsUpdate: NotificationPreferences) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[notificationsEnabledKey] = prefsUpdate.enabled
            prefs[notifyNewEpisodesKey] = prefsUpdate.newEpisodesEnabled
            prefs[notifyNewMoviesKey] = prefsUpdate.newMoviesEnabled
        }
    }
}
