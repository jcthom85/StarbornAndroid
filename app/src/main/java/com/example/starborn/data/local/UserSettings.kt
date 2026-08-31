package com.example.starborn.data.local

/**
 * Universal game user settings model shared across Android, iOS, and Desktop.
 */
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
