package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TinkeringRecipe(
    val id: String,
    val name: String,
    val description: String? = null,
    val base: String,
    val components: List<String>,
    val result: String,
    @Json(name = "success_message")
    val successMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class CookingRecipe(
    val id: String,
    val name: String,
    val description: String? = null,
    val result: String,
    val ingredients: Map<String, Int>
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
    val difficulty: String? = null
)

@JsonClass(generateAdapter = true)
data class FirstAidOutput(
    val perfect: String? = null,
    val success: String,
    val failure: String? = null
)
