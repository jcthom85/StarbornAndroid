package com.example.starborn.domain.tutorial

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class TutorialEntry(
    val key: String?,
    val context: String?,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)

data class TutorialRuntimeState(
    val current: TutorialEntry? = null,
    val queue: List<TutorialEntry> = emptyList(),
    val seen: Set<String> = emptySet(),
    val completed: Set<String> = emptySet(),
    val roomsVisited: Set<String> = emptySet()
)

@JsonClass(generateAdapter = true)
data class TutorialScriptStep(
    val key: String? = null,
    val message: String,
    val context: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    @Json(name = "delay_ms") val delayMs: Long? = null
)

@JsonClass(generateAdapter = true)
data class TutorialScript(
    val id: String,
    val steps: List<TutorialScriptStep> = emptyList()
)
