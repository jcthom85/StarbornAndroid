package com.example.starborn.domain.prompt

import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.tutorial.TutorialEntry

interface UIPrompt {
    val id: String
    fun onShow() {}
    fun onDismiss() {}
}

data class TutorialPrompt(
    val entry: TutorialEntry,
    private val onDismissCallback: (() -> Unit)? = null,
    val primaryAction: (() -> Unit)? = null,
    val afterRewards: Boolean = false
) : UIPrompt {
    override val id: String = entry.key ?: "tutorial_${entry.hashCode()}"

    override fun onDismiss() {
        onDismissCallback?.invoke()
    }
}

data class MilestonePrompt(
    val event: MilestoneEvent
) : UIPrompt {
    override val id: String = "milestone_${event.id}"
}

data class ItemGrantedPrompt(
    val itemName: String,
    val quantity: Int,
    val itemId: String? = null,
    val description: String? = null,
    val category: String? = null,
    val sequenceId: String? = null,
    val sequenceIndex: Int = 1,
    val sequenceTotal: Int = 1,
    private val onDismissCallback: (() -> Unit)? = null
) : UIPrompt {
    override val id: String = "item_granted_${itemName}_${quantity}_${java.util.UUID.randomUUID()}"
    val announcementHeading: String get() = when (category) {
        "consumed" -> "ITEM USED"
        "arcade_schematic" -> "ARCADE SCHEMATIC ACQUIRED"
        else -> "ITEM ACQUIRED"
    }
    private val acquisitionLabel: String get() = if (quantity > 1) "$itemName ×$quantity" else itemName
    val acquisitionDescription: String get() = when (category) {
        "consumed" -> "Item used: $acquisitionLabel. Removed from inventory."
        "arcade_schematic" -> "Arcade repair schematic acquired: $itemName. Repair required before playing."
        else -> "Item acquired: $itemName${if (quantity > 1) " ×$quantity" else ""}."
    }


    override fun onDismiss() {
        onDismissCallback?.invoke()
    }
}

data class ItemBatchGrantedPrompt(
    val summary: String,
    private val onDismissCallback: (() -> Unit)? = null
) : UIPrompt {
    override val id: String = "item_batch_granted_${summary.hashCode()}_${java.util.UUID.randomUUID()}"

    override fun onDismiss() {
        onDismissCallback?.invoke()
    }
}
