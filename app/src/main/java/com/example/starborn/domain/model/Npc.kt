package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class Npc(
    val name: String,
    val aliases: List<String>,
    val dialogue: Map<String, String>,
    val interactions: List<Interaction>,
    val role: String? = null,
    val description: String? = null,
    val id: String? = null,
    val portrait: String? = null
)

data class Interaction(
    val label: String,
    val type: String,
    @Json(name = "dialogue_id")
    val dialogueId: String
)
