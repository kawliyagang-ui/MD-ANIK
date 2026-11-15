package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PlayerServer
import com.example.ui.theme.DarkThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinetra_user_prefs"
)

class UserPreferences(private val context: Context) {

    private val darkPaletteKey = stringPreferencesKey("dark_theme_palette")
    private val preferredHighReadabilityKey = stringPreferencesKey("preferred_high_readability_palette")
    private val defaultServerKey = stringPreferencesKey("default_player_server")

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
}
