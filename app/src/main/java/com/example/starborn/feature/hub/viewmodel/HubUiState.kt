package com.example.starborn.feature.hub.viewmodel

import com.example.starborn.domain.model.Hub

data class HubUiState(
    val isLoading: Boolean = true,
    val hub: Hub? = null,
    val nodes: List<HubNodeUi> = emptyList(),
    val backgroundImage: String? = null,
    val selectedNodeId: String? = null,
    val statusMessage: String? = null
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
    val subtitle: String? = null,
    val unlocked: Boolean = true,
    val visited: Boolean = false,
    val completed: Boolean = false,
    val canEnter: Boolean = true,
    val lockReason: String? = null,
    val special: String? = null
)
