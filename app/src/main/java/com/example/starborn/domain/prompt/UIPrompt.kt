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
    private val onDismissCallback: (() -> Unit)? = null
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
    private val onDismissCallback: (() -> Unit)? = null
) : UIPrompt {
    override val id: String = "item_granted_${itemName}_${quantity}_${java.util.UUID.randomUUID()}"

    override fun onDismiss() {
        onDismissCallback?.invoke()
    }
}

