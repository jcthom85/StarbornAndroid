package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class TuningPuzzle(
    val id: String,
    val title: String,
    val prompt: String,
    val sliders: List<TuningPuzzleSlider> = emptyList(),
    @Json(name = "success_event")
    val successEvent: String,
    @Json(name = "success_message")
    val successMessage: String? = null,
    @Json(name = "failure_hint")
    val failureHint: String,
    @Json(name = "audio_cue")
    val audioCue: String? = null
)

data class TuningPuzzleSlider(
    val id: String,
    val label: String,
    val min: Float = 0f,
    val max: Float = 100f,
    val initial: Float = 50f,
    val target: Float,
    val tolerance: Float = 5f,
    val unit: String? = null
)
