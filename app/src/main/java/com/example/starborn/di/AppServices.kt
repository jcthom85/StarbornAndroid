package com.example.starborn.di

import android.content.Context
import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CraftingAssetDataSource
import com.example.starborn.data.assets.DialogueAssetDataSource
import com.example.starborn.data.assets.EventAssetDataSource
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.inventory.InventoryService

class AppServices(context: Context) {
    private val moshi = MoshiProvider.instance
    private val assetReader = AssetJsonReader(context, moshi)

    val worldDataSource = WorldAssetDataSource(assetReader)
    private val dialogueDataSource = DialogueAssetDataSource(assetReader)
    private val eventDataSource = EventAssetDataSource(assetReader)
    private val itemRepository = ItemRepository(ItemAssetDataSource(assetReader))
    private val craftingDataSource = CraftingAssetDataSource(assetReader)
    val questRepository: QuestRepository = QuestRepository(QuestAssetDataSource(assetReader)).apply { load() }

    val inventoryService = InventoryService(itemRepository).apply { loadItems() }
    val sessionStore = GameSessionStore()
    val craftingService = CraftingService(craftingDataSource, inventoryService, sessionStore)
    val events: List<GameEvent> = eventDataSource.loadEvents()

    val dialogueService: DialogueService = DialogueService(
        dialogueDataSource.loadDialogue(),
        DialogueConditionEvaluator { condition ->
            isDialogueConditionMet(condition, sessionStore.state.value)
        },
        DialogueTriggerHandler { trigger ->
            handleDialogueTrigger(trigger, sessionStore)
        }
    )

}

internal fun isDialogueConditionMet(condition: String?, state: GameSessionState): Boolean {
    if (condition.isNullOrBlank()) return true
    val tokens = condition.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    for (token in tokens) {
        val parts = token.split(':', limit = 2)
        if (parts.isEmpty()) continue
        val type = parts[0].trim().lowercase()
        val value = parts.getOrNull(1)?.trim().orEmpty()
        val met = when (type) {
            "quest" -> value in state.activeQuests || value in state.completedQuests
            "milestone" -> value in state.completedMilestones
            "milestone_not_set" -> value !in state.completedMilestones
            "item" -> true
            else -> true
        }
        if (!met) return false
    }
    return true
}

internal fun handleDialogueTrigger(trigger: String, sessionStore: GameSessionStore) {
    if (trigger.isBlank()) return
    trigger.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
        val parts = token.split(':', limit = 2)
        if (parts.isEmpty()) return@forEach
        val type = parts[0].trim().lowercase()
        val value = parts.getOrNull(1)?.trim().orEmpty()
        when (type) {
            "start_quest" -> sessionStore.startQuest(value)
            "complete_quest" -> sessionStore.completeQuest(value)
            "set_milestone" -> sessionStore.setMilestone(value)
            "clear_milestone" -> sessionStore.clearMilestone(value)
        }
    }
}
