package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Quest(
    val id: String,
    val title: String,
    val summary: String,
    val description: String,
    val flavor: String? = null,
    val giver: String? = null,
    @Json(name = "hub_id")
    val hubId: String? = null,
    val stages: List<QuestStage> = emptyList(),
    val rewards: List<QuestReward> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuestStage(
    val id: String,
    val title: String,
    val description: String,
    val tasks: List<QuestTask> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuestTask(
    val id: String,
    val text: String,
    val done: Boolean = false,
    @Json(name = "tutorial_id")
    val tutorialId: String? = null
)

@JsonClass(generateAdapter = true)
data class QuestReward(
    val type: String,
    val amount: Int? = null,
    @Json(name = "item_id")
    val itemId: String? = null
)
