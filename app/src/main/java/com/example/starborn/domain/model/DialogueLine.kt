package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class DialogueLine(
    val id: String,
    val speaker: String,
    val text: String,
    val next: String? = null,
    val condition: String? = null,
    val trigger: String? = null
)
