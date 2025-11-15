package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class GameEvent(
    val id: String,
    val description: String? = null,
    val trigger: EventTrigger,
    val repeatable: Boolean = false,
    val actions: List<EventAction>,
    val conditions: List<EventCondition> = emptyList(),
    @Json(name = "on_message")
    val onMessage: String? = null,
    @Json(name = "off_message")
    val offMessage: String? = null,
    val message: String? = null
)

data class EventTrigger(
    val type: String,
    val npc: String? = null,
    @Json(name = "room")
    val room: String? = null,
    @Json(name = "room_id")
    val roomId: String? = null,
    @Json(name = "quest_id")
    val questId: String? = null,
    @Json(name = "action")
    val action: String? = null,
    val enemies: List<String>? = null,
    val item: String? = null,
    @Json(name = "item_id")
    val itemId: String? = null
)

data class EventAction(
    val type: String,
    @Json(name = "quest_id")
    val questId: String? = null,
    @Json(name = "task_id")
    val taskId: String? = null,
    val milestone: String? = null,
    val milestones: List<String>? = null,
    @Json(name = "scene_id")
    val sceneId: String? = null,
    val message: String? = null,
    val note: String? = null,
    val reward: EventReward? = null,
    @Json(name = "set_milestones")
    val setMilestones: List<String>? = null,
    @Json(name = "clear_milestones")
    val clearMilestones: List<String>? = null,
    @Json(name = "start_quest")
    val startQuest: String? = null,
    @Json(name = "complete_quest")
    val completeQuest: String? = null,
    val item: String? = null,
    @Json(name = "item_id")
    val itemId: String? = null,
    val quantity: Int? = null,
    val xp: Int? = null,
    val credits: Int? = null,
    val ap: Int? = null,
    val text: String? = null,
    @Json(name = "tap_to_dismiss")
    val tapToDismiss: Boolean? = null,
    @Json(name = "room_id")
    val roomId: String? = null,
    @Json(name = "state_key")
    val stateKey: String? = null,
    val value: Boolean? = null,
    @Json(name = "encounter_id")
    val encounterId: String? = null,
    @Json(name = "tutorial_id")
    val tutorialId: String? = null,
    @Json(name = "action")
    val action: String? = null,
    val context: String? = null,
    @Json(name = "layer")
    val audioLayer: String? = null,
    @Json(name = "cue_id")
    val audioCueId: String? = null,
    @Json(name = "gain")
    val audioGain: Float? = null,
    @Json(name = "fade_ms")
    val audioFadeMs: Long? = null,
    @Json(name = "loop")
    val audioLoop: Boolean? = null,
    @Json(name = "stop")
    val audioStop: Boolean? = null,
    @Json(name = "on_complete")
    val onComplete: List<EventAction>? = null,
    @Json(name = "items")
    val rewardItems: List<RewardItem>? = null,
    val `do`: List<EventAction>? = null,
    val elseDo: List<EventAction>? = null,
    @Json(name = "to_stage_id")
    val toStageId: String? = null
)

data class EventCondition(
    val type: String,
    val milestone: String? = null,
    @Json(name = "quest_id")
    val questId: String? = null,
    @Json(name = "stage_id")
    val stageId: String? = null,
    @Json(name = "task_id")
    val taskId: String? = null,
    @Json(name = "item_id")
    val itemId: String? = null,
    val item: String? = null,
    val quantity: Int? = null,
    @Json(name = "event_id")
    val eventId: String? = null,
    @Json(name = "tutorial_id")
    val tutorialId: String? = null
)

data class EventReward(
    val xp: Int? = null,
    val credits: Int? = null,
    val ap: Int? = null,
    val items: List<RewardItem> = emptyList()
)

data class RewardItem(
    @Json(name = "item_id")
    val itemId: String,
    val quantity: Int? = null
)
