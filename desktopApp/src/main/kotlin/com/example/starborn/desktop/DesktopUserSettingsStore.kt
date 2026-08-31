package com.example.starborn.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.example.starborn.data.local.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Desktop implementation of UserSettingsStore using file-backed DataStore.
 */
class DesktopUserSettingsStore(
    baseDir: File = File(System.getProperty("user.home"), ".starborn")
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            baseDir.mkdirs()
            File(baseDir, "user_settings.preferences_pb")
        }
    )

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            musicVolume = prefs[MUSIC_VOLUME] ?: 1f,
            sfxVolume = prefs[SFX_VOLUME] ?: 1f,
            voiceVolume = prefs[VOICE_VOLUME] ?: 1f,
            vignetteEnabled = prefs[VIGNETTE_ENABLED] ?: true,
            tutorialsEnabled = prefs[TUTORIALS_ENABLED] ?: true,
            disableScreenshake = prefs[DISABLE_SCREENSHAKE] ?: false,
            disableFlashes = prefs[DISABLE_FLASHES] ?: false,
            disableHaptics = prefs[DISABLE_HAPTICS] ?: true,
            highContrastMode = prefs[HIGH_CONTRAST_MODE] ?: false,
            largeTouchTargets = prefs[LARGE_TOUCH_TARGETS] ?: false,
            themeBandsEnabled = prefs[THEME_BANDS_ENABLED] ?: false
        )
    }

    suspend fun setMusicVolume(value: Float) {
        dataStore.edit { it[MUSIC_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setSfxVolume(value: Float) {
        dataStore.edit { it[SFX_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setVoiceVolume(value: Float) {
        dataStore.edit { it[VOICE_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setVignetteEnabled(enabled: Boolean) {
        dataStore.edit { it[VIGNETTE_ENABLED] = enabled }
    }

    suspend fun setTutorialsEnabled(enabled: Boolean) {
        dataStore.edit { it[TUTORIALS_ENABLED] = enabled }
    }

    suspend fun setScreenshakeDisabled(disabled: Boolean) {
        dataStore.edit { it[DISABLE_SCREENSHAKE] = disabled }
    }

    suspend fun setFlashesDisabled(disabled: Boolean) {
        dataStore.edit { it[DISABLE_FLASHES] = disabled }
    }

    suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { it[HIGH_CONTRAST_MODE] = enabled }
    }

    suspend fun setThemeBandsEnabled(enabled: Boolean) {
        dataStore.edit { it[THEME_BANDS_ENABLED] = enabled }
    }

    companion object {
        private val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        private val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        private val VIGNETTE_ENABLED = booleanPreferencesKey("vignette_enabled")
        private val TUTORIALS_ENABLED = booleanPreferencesKey("tutorials_enabled")
        private val DISABLE_SCREENSHAKE = booleanPreferencesKey("disable_screenshake")
        private val DISABLE_FLASHES = booleanPreferencesKey("disable_flashes")
        private val DISABLE_HAPTICS = booleanPreferencesKey("disable_haptics")
        private val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        private val LARGE_TOUCH_TARGETS = booleanPreferencesKey("large_touch_targets")
        private val THEME_BANDS_ENABLED = booleanPreferencesKey("theme_bands_enabled")
        private val VOICE_VOLUME = floatPreferencesKey("voice_volume")
    }
}
