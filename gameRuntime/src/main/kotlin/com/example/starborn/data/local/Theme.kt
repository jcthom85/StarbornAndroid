package com.example.starborn.data.local

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Theme(
    val bg: List<Float>,
    val fg: List<Float>,
    val border: List<Float>,
    val accent: List<Float>,
    @Json(name = "background_image") val backgroundImage: String
)
