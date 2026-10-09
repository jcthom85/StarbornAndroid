package com.example.starborn.data.local

import kotlinx.coroutines.flow.Flow

interface GameSettingsStore {
    val settings: Flow<UserSettings>
    suspend fun setMasterVolume(value: Float) {}
    suspend fun setMusicVolume(value: Float)
    suspend fun setSfxVolume(value: Float)
    suspend fun setAmbienceVolume(value: Float) {}
    suspend fun setVoiceVolume(value: Float)
    suspend fun setUiScale(value: Float) {}
    suspend fun setMuteWhenUnfocused(enabled: Boolean) {}
    suspend fun setAutoAdvanceDialogue(enabled: Boolean) {}
    suspend fun setVignetteEnabled(enabled: Boolean)
    suspend fun setTutorialsEnabled(enabled: Boolean)
    suspend fun setScreenshakeDisabled(disabled: Boolean)
    suspend fun setFlashesDisabled(disabled: Boolean)
    suspend fun setHapticsDisabled(disabled: Boolean)
    suspend fun setHighContrastMode(enabled: Boolean)
    suspend fun setLargeTouchTargets(enabled: Boolean)
    suspend fun setThemeBandsEnabled(enabled: Boolean)
    suspend fun setEnvironmentalEffectsQuality(value: EnvironmentalEffectsQuality) {}
    suspend fun setModernFieldMenu(enabled: Boolean)
}
