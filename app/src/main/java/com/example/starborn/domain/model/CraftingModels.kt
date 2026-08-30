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
    val successMessage: String? = null,
    val tools: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CookingRecipe(
    val id: String,
    val name: String,
    val description: String? = null,
    val ingredients: Map<String, Int> = emptyMap(),
    val result: String,
    @Json(name = "result_quantity")
    val resultQuantity: Int = 1,
    @Json(name = "success_message")
    val successMessage: String? = null
)
