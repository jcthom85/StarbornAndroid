package com.example.starborn.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val musicVolume: Float = 1f,
    val sfxVolume: Float = 1f,
    val voiceVolume: Float = 1f,
    val vignetteEnabled: Boolean = true,
    val tutorialsEnabled: Boolean = true,
    val disableScreenshake: Boolean = false,
    val disableFlashes: Boolean = false,
    val disableHaptics: Boolean = false,
    val highContrastMode: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val themeBandsEnabled: Boolean = false
)

class UserSettingsStore(context: Context) {
    private val dataStore = context.userSettingsDataStore

    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            musicVolume = prefs[MUSIC_VOLUME] ?: 1f,
            sfxVolume = prefs[SFX_VOLUME] ?: 1f,
            voiceVolume = prefs[VOICE_VOLUME] ?: 1f,
            vignetteEnabled = prefs[VIGNETTE_ENABLED] ?: true,
            tutorialsEnabled = prefs[TUTORIALS_ENABLED] ?: true,
            disableScreenshake = prefs[DISABLE_SCREENSHAKE] ?: false,
            disableFlashes = prefs[DISABLE_FLASHES] ?: false,
            disableHaptics = prefs[DISABLE_HAPTICS] ?: false,
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

    suspend fun setHapticsDisabled(disabled: Boolean) {
        dataStore.edit { it[DISABLE_HAPTICS] = disabled }
    }

    suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { it[HIGH_CONTRAST_MODE] = enabled }
    }

    suspend fun setLargeTouchTargets(enabled: Boolean) {
        dataStore.edit { it[LARGE_TOUCH_TARGETS] = enabled }
    }

    suspend fun setThemeBandsEnabled(enabled: Boolean) {
        dataStore.edit { it[THEME_BANDS_ENABLED] = enabled }
    }

    companion object {
        private val MUSIC_VOLUME: Preferences.Key<Float> = floatPreferencesKey("music_volume")
        private val SFX_VOLUME: Preferences.Key<Float> = floatPreferencesKey("sfx_volume")
        private val VIGNETTE_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("vignette_enabled")
        private val TUTORIALS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("tutorials_enabled")
        private val DISABLE_SCREENSHAKE: Preferences.Key<Boolean> = booleanPreferencesKey("disable_screenshake")
        private val DISABLE_FLASHES: Preferences.Key<Boolean> = booleanPreferencesKey("disable_flashes")
        private val DISABLE_HAPTICS: Preferences.Key<Boolean> = booleanPreferencesKey("disable_haptics")
        private val HIGH_CONTRAST_MODE: Preferences.Key<Boolean> = booleanPreferencesKey("high_contrast_mode")
        private val LARGE_TOUCH_TARGETS: Preferences.Key<Boolean> = booleanPreferencesKey("large_touch_targets")
        private val THEME_BANDS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("theme_bands_enabled")
        private val VOICE_VOLUME: Preferences.Key<Float> = floatPreferencesKey("voice_volume")
    }
}
