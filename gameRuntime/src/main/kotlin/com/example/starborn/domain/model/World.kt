package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class World(
    val id: String,
    val title: String,
    val description: String,
    @Json(name = "default_theme")
    val defaultTheme: String
)
