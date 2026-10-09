package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class Hub(
    val id: String,
    @Json(name = "world_id")
    val worldId: String,
    val title: String,
    val description: String,
    @Json(name = "background_image")
    val backgroundImage: String,
    val discovered: Boolean
)
