package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class DialogueLine(
    val id: String,
    val speaker: String,
    val text: String,
    val next: String? = null,
    val condition: String? = null,
    val trigger: String? = null,
    val options: List<DialogueOption>? = null,
    val portrait: String? = null,
    @Json(name = "voice")
    val voiceCue: String? = null
)

data class DialogueOption(
    val id: String,
    val text: String,
    val next: String? = null,
    val trigger: String? = null,
    val condition: String? = null
)
