package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TinkeringRecipe(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String = "gear",
    val method: String? = null,
    val base: String? = null,
    val components: List<String> = emptyList(),
    val ingredients: Map<String, Int> = emptyMap(),
    val result: String,
    @Json(name = "result_quantity")
    val resultQuantity: Int = 1,
    @Json(name = "success_message")
    val successMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class FirstAidRecipe(
    val id: String,
    val name: String,
    val description: String? = null,
    val ingredients: Map<String, Int>,
    val minigame: MinigameDefinition? = null,
    val output: FirstAidOutput
)

@JsonClass(generateAdapter = true)
data class MinigameDefinition(
    val type: String,
    val difficulty: String? = null,
    val window: Double? = null,
    val speed: Double? = null,
    @Json(name = "duration_ms")
    val durationMs: Long? = null,
    @Json(name = "perfect_window")
    val perfectWindow: Double? = null,
    @Json(name = "start_cue")
    val startCue: String? = null,
    @Json(name = "success_cue")
    val successCue: String? = null,
    @Json(name = "perfect_cue")
    val perfectCue: String? = null,
    @Json(name = "failure_cue")
    val failureCue: String? = null,
    @Json(name = "success_fx")
    val successFx: String? = null,
    @Json(name = "failure_fx")
    val failureFx: String? = null
)

@JsonClass(generateAdapter = true)
data class FirstAidOutput(
    val perfect: String? = null,
    val success: String,
    val failure: String? = null
)
