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

fun TutorialEntry.formattedTitle(): String {
    val existing = metadata["title"]
    if (existing != null && existing.trimStart().startsWith("[")) {
        return existing
    }
    val rawCategory = (context ?: "").lowercase(java.util.Locale.ROOT).trim()
    val rawKey = (key ?: "").lowercase(java.util.Locale.ROOT)
    val tag = when {
        rawCategory.contains("combat") || rawKey.contains("combat") || rawCategory.contains("source art") -> "[Combat]"
        rawCategory.contains("navigation") || rawCategory.contains("movement") || rawKey.contains("move") || rawKey.contains("nav") -> "[Navigation]"
        rawCategory.contains("exploration") -> "[Navigation]"
        rawCategory.contains("tinker") || rawCategory.contains("craft") || rawCategory.contains("inventory") ||
            rawCategory.contains("equipment") || rawCategory.contains("gear") || rawCategory.contains("bag") ||
            rawCategory.contains("snack") || rawCategory.contains("market") || rawCategory.contains("astra") -> "[Field]"
        else -> "[System]"
    }
    val base = existing ?: context?.takeIf { it.isNotBlank() } ?: "Tutorial"
    return "$tag $base"
}

