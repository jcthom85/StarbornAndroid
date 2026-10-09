package com.example.starborn.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.starborn.data.local.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

enum class DesktopDisplayMode(val label: String) {
    WINDOWED("Windowed (1280x800)"),
    BORDERLESS("Borderless Windowed"),
    FULLSCREEN("Exclusive Fullscreen")
}

/**
 * Desktop implementation of UserSettingsStore using file-backed DataStore.
 */
class DesktopUserSettingsStore(
    baseDir: File = File(System.getProperty("user.home"), ".starborn")
) : com.example.starborn.data.local.GameSettingsStore, AutoCloseable {
    private val settingsJob = kotlinx.coroutines.SupervisorJob()
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = kotlinx.coroutines.CoroutineScope(settingsJob + kotlinx.coroutines.Dispatchers.IO),
        produceFile = {
            baseDir.mkdirs()
            File(baseDir, "user_settings.preferences_pb")
        }
    )

    override fun close() { settingsJob.cancel() }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            masterVolume = prefs[MASTER_VOLUME] ?: 1f,
            musicVolume = prefs[MUSIC_VOLUME] ?: 1f,
            sfxVolume = prefs[SFX_VOLUME] ?: 1f,
            ambienceVolume = prefs[AMBIENCE_VOLUME] ?: 1f,
            voiceVolume = prefs[VOICE_VOLUME] ?: 1f,
            uiScale = prefs[UI_SCALE] ?: 1f,
            muteWhenUnfocused = prefs[MUTE_WHEN_UNFOCUSED] ?: false,
            autoAdvanceDialogue = prefs[AUTO_ADVANCE_DIALOGUE] ?: false,
            vignetteEnabled = prefs[VIGNETTE_ENABLED] ?: true,
            tutorialsEnabled = prefs[TUTORIALS_ENABLED] ?: true,
            disableScreenshake = prefs[DISABLE_SCREENSHAKE] ?: false,
            disableFlashes = prefs[DISABLE_FLASHES] ?: false,
            disableHaptics = prefs[DISABLE_HAPTICS] ?: true,
            highContrastMode = prefs[HIGH_CONTRAST_MODE] ?: false,
            largeTouchTargets = prefs[LARGE_TOUCH_TARGETS] ?: false,
            themeBandsEnabled = prefs[THEME_BANDS_ENABLED] ?: false,
            modernFieldMenu = prefs[booleanPreferencesKey("modern_field_menu")] ?: true,
            environmentalEffectsQuality = com.example.starborn.data.local.EnvironmentalEffectsQuality.fromId(prefs[stringPreferencesKey("environmental_effects_quality")])
        )
    }

    override suspend fun setMasterVolume(value: Float) {
        dataStore.edit { it[MASTER_VOLUME] = value.coerceIn(0f, 1f) }
    }

    override suspend fun setEnvironmentalEffectsQuality(value: com.example.starborn.data.local.EnvironmentalEffectsQuality) {
        dataStore.edit { it[stringPreferencesKey("environmental_effects_quality")] = value.name }
    }

    override suspend fun setMusicVolume(value: Float) {
        dataStore.edit { it[MUSIC_VOLUME] = value.coerceIn(0f, 1f) }
    }

    override suspend fun setAmbienceVolume(value: Float) {
        dataStore.edit { it[AMBIENCE_VOLUME] = value.coerceIn(0f, 1f) }
    }

    override suspend fun setUiScale(value: Float) {
        dataStore.edit { it[UI_SCALE] = value.coerceIn(0.75f, 2.5f) }
    }

    override suspend fun setMuteWhenUnfocused(enabled: Boolean) {
        dataStore.edit { it[MUTE_WHEN_UNFOCUSED] = enabled }
    }

    override suspend fun setAutoAdvanceDialogue(enabled: Boolean) {
        dataStore.edit { it[AUTO_ADVANCE_DIALOGUE] = enabled }
    }

    override suspend fun setSfxVolume(value: Float) {
        dataStore.edit { it[SFX_VOLUME] = value.coerceIn(0f, 1f) }
    }

    override suspend fun setVoiceVolume(value: Float) {
        dataStore.edit { it[VOICE_VOLUME] = value.coerceIn(0f, 1f) }
    }

    override suspend fun setVignetteEnabled(enabled: Boolean) {
        dataStore.edit { it[VIGNETTE_ENABLED] = enabled }
    }

    override suspend fun setTutorialsEnabled(enabled: Boolean) {
        dataStore.edit { it[TUTORIALS_ENABLED] = enabled }
    }

    override suspend fun setScreenshakeDisabled(disabled: Boolean) {
        dataStore.edit { it[DISABLE_SCREENSHAKE] = disabled }
    }

    override suspend fun setFlashesDisabled(disabled: Boolean) {
        dataStore.edit { it[DISABLE_FLASHES] = disabled }
    }

    override suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { it[HIGH_CONTRAST_MODE] = enabled }
    }

    val displayMode: Flow<DesktopDisplayMode> = dataStore.data.map { prefs ->
        val modeStr = prefs[DISPLAY_MODE] ?: DesktopDisplayMode.WINDOWED.name
        try {
            DesktopDisplayMode.valueOf(modeStr)
        } catch (_: Throwable) {
            DesktopDisplayMode.WINDOWED
        }
    }

    suspend fun setDisplayMode(mode: DesktopDisplayMode) {
        dataStore.edit { it[DISPLAY_MODE] = mode.name }
    }

    val windowWidth: Flow<Int> = dataStore.data.map { prefs -> prefs[WINDOW_WIDTH] ?: 1280 }
    val windowHeight: Flow<Int> = dataStore.data.map { prefs -> prefs[WINDOW_HEIGHT] ?: 800 }
    val windowX: Flow<Int?> = dataStore.data.map { prefs -> prefs[WINDOW_X] }
    val windowY: Flow<Int?> = dataStore.data.map { prefs -> prefs[WINDOW_Y] }
    val windowMaximized: Flow<Boolean> = dataStore.data.map { prefs -> prefs[WINDOW_MAXIMIZED] ?: false }

    suspend fun saveWindowBounds(width: Int, height: Int, x: Int?, y: Int?, isMaximized: Boolean) {
        dataStore.edit { prefs ->
            prefs[WINDOW_MAXIMIZED] = isMaximized
            if (!isMaximized) {
                if (width >= 1024) prefs[WINDOW_WIDTH] = width
                if (height >= 720) prefs[WINDOW_HEIGHT] = height
                if (x != null) prefs[WINDOW_X] = x
                if (y != null) prefs[WINDOW_Y] = y
            }
        }
    }

    override suspend fun setThemeBandsEnabled(enabled: Boolean) {
        dataStore.edit { it[THEME_BANDS_ENABLED] = enabled }
    }

    override suspend fun setHapticsDisabled(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("disable_haptics")] = enabled }
    }

    override suspend fun setLargeTouchTargets(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("large_touch_targets")] = enabled }
    }

    override suspend fun setModernFieldMenu(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("modern_field_menu")] = enabled }
    }

    companion object {
        private val MASTER_VOLUME = floatPreferencesKey("master_volume")
        private val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        private val AMBIENCE_VOLUME = floatPreferencesKey("ambience_volume")
        private val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        private val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        private val UI_SCALE = floatPreferencesKey("ui_scale")
        private val MUTE_WHEN_UNFOCUSED = booleanPreferencesKey("mute_when_unfocused")
        private val AUTO_ADVANCE_DIALOGUE = booleanPreferencesKey("auto_advance_dialogue")
        private val VIGNETTE_ENABLED = booleanPreferencesKey("vignette_enabled")
        private val TUTORIALS_ENABLED = booleanPreferencesKey("tutorials_enabled")
        private val DISABLE_SCREENSHAKE = booleanPreferencesKey("disable_screenshake")
        private val DISABLE_FLASHES = booleanPreferencesKey("disable_flashes")
        private val DISABLE_HAPTICS = booleanPreferencesKey("disable_haptics")
        private val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        private val LARGE_TOUCH_TARGETS = booleanPreferencesKey("large_touch_targets")
        private val THEME_BANDS_ENABLED = booleanPreferencesKey("theme_bands_enabled")
        private val DISPLAY_MODE = stringPreferencesKey("display_mode")
        private val WINDOW_WIDTH = androidx.datastore.preferences.core.intPreferencesKey("window_width")
        private val WINDOW_HEIGHT = androidx.datastore.preferences.core.intPreferencesKey("window_height")
        private val WINDOW_X = androidx.datastore.preferences.core.intPreferencesKey("window_x")
        private val WINDOW_Y = androidx.datastore.preferences.core.intPreferencesKey("window_y")
        private val WINDOW_MAXIMIZED = androidx.datastore.preferences.core.booleanPreferencesKey("window_maximized")
    }
}
