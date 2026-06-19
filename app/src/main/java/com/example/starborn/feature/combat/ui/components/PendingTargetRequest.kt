package com.example.starborn.feature.combat.ui.components

import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Skill


sealed interface PendingTargetRequest {
    val instruction: String
    fun accepts(target: TargetFilter): Boolean

    data object Attack : PendingTargetRequest {
        override val instruction: String = "Choose a target"
        override fun accepts(target: TargetFilter): Boolean = target == TargetFilter.ENEMY
    }

    data class SnackRequest(
        val snackName: String,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
    }

    data class SkillRequest(
        val skill: Skill,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
    }

    data class ItemRequest(
        val entry: InventoryEntry,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
    }
}
