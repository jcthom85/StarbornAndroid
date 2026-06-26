package com.example.starborn.feature.hub.viewmodel

import com.example.starborn.domain.model.Hub

data class HubUiState(
    val isLoading: Boolean = true,
    val hub: Hub? = null,
    val nodes: List<HubNodeUi> = emptyList(),
    val backgroundImage: String? = null,
    val selectedNodeId: String? = null,
    val statusMessage: String? = null,
    val trackedQuest: HubQuestUi? = null,
    val lockedPrompt: HubLockedPrompt? = null
)

data class HubLockedPrompt(
    val title: String,
    val message: String
)

data class HubNodeUi(
    val id: String,
    val title: String,
    val entryRoom: String,
    val centerX: Float,
    val centerY: Float,
    val sizeHint: Float,
    val discovered: Boolean,
    val iconPath: String? = null,
    val description: String? = null,
    val lockedPreview: String? = null,
    val unlocked: Boolean = true,
    val visited: Boolean = false,
    val completed: Boolean = false,
    val canEnter: Boolean = true,
    val lockReason: String? = null,
    val special: String? = null
)

data class HubQuestUi(
    val id: String,
    val title: String,
    val objective: String?,
    val stageTitle: String?
)
