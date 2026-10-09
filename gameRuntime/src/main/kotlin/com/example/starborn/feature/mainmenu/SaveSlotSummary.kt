package com.example.starborn.feature.mainmenu

import com.example.starborn.domain.session.GameSessionState

data class SaveSlotSummary(
    val slot: Int,
    val state: GameSessionState?,
    val title: String,
    val subtitle: String,
    val isEmpty: Boolean,
    val isAutosave: Boolean = false,
    val isQuickSave: Boolean = false,
    val savedAtMillis: Long? = null,
    val partyPortraits: List<String> = emptyList()
)
