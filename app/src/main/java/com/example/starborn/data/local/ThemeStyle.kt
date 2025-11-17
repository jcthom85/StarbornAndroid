package com.example.starborn.data.local

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThemeStyle(
    val vignette: VignetteStyle? = null,
    val bands: BandStyle? = null
)

@JsonClass(generateAdapter = true)
data class VignetteStyle(
    @Json(name = "base_intensity") val baseIntensity: Float = 0.0f,
    @Json(name = "dark_boost") val darkBoost: Float = 0.35f,
    val feather: Float = 0.22f,
    @Json(name = "margin_dp") val marginDp: Float = 0f,
    val color: List<Float>? = null
)

@JsonClass(generateAdapter = true)
data class BandStyle(
    @Json(name = "base_alpha") val baseAlpha: Float = 0.65f,
    @Json(name = "dark_boost") val darkBoost: Float = 0.35f,
    @Json(name = "highlight_alpha") val highlightAlpha: Float = 0.35f,
    @Json(name = "noise_alpha") val noiseAlpha: Float = 0.05f,
    @Json(name = "min_height_dp") val minHeightDp: Float = 48f,
    @Json(name = "max_height_dp") val maxHeightDp: Float = 220f
)
