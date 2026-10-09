package com.example.starborn.data.local

/**
 * Universal game user settings model shared across Android, iOS, and Desktop.
 */
data class UserSettings(
    val masterVolume: Float = 1f,
    val musicVolume: Float = 1f,
    val sfxVolume: Float = 1f,
    val ambienceVolume: Float = 1f,
    val voiceVolume: Float = 1f,
    val uiScale: Float = 1f,
    val muteWhenUnfocused: Boolean = false,
    val autoAdvanceDialogue: Boolean = false,
    val vignetteEnabled: Boolean = true,
    val tutorialsEnabled: Boolean = true,
    val disableScreenshake: Boolean = false,
    val disableFlashes: Boolean = false,
    val disableHaptics: Boolean = false,
    val highContrastMode: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val themeBandsEnabled: Boolean = false,
    val modernFieldMenu: Boolean = true,
    val environmentalEffectsQuality: EnvironmentalEffectsQuality = EnvironmentalEffectsQuality.FULL
)

/** Full is authored intensity, not maximum intensity. */
enum class EnvironmentalEffectsQuality(val label: String) {
    FULL("Full"), REDUCED("Reduced"), OFF("Off");
    companion object { fun fromId(value: String?) = entries.firstOrNull { it.name.equals(value,true) } ?: FULL }
}
