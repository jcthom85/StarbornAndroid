package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import com.example.starborn.domain.movement.EnemyMovementManager

class ExplorationCombatHandler(
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore,
    private val eventManager: EventManager,
    private val postStatus: (String) -> Unit,
    private val emitEvent: (ExplorationEvent) -> Unit,
    private val normalizeLootItemId: (String) -> String
) {
    fun processVictory(
        result: CombatResultPayload,
        enemyMovementManager: EnemyMovementManager?,
        onVictoryProcessed: () -> Unit
    ): String {
        result.sourcePartyId?.let { partyId ->
            enemyMovementManager?.markDefeated(partyId)
        }
        val enemyIds = result.enemyIds
        val rewardParts = mutableListOf<String>()
        if (result.rewardXp > 0) rewardParts += "${result.rewardXp} XP"
        if (result.rewardAp > 0) rewardParts += "${result.rewardAp} AP"
        if (result.rewardCredits > 0) rewardParts += "${result.rewardCredits} credits"
        val grantedItems = mutableListOf<String>()
        result.rewardItems.forEach { (itemId, quantity) ->
            val qty = quantity.coerceAtLeast(0)
            if (qty <= 0) return@forEach
            val canonicalId = normalizeLootItemId(itemId)
            val name = inventoryService.itemDisplayName(canonicalId)
            rewardParts += "$qty x $name"
            emitEvent(ExplorationEvent.ItemGranted(name, qty))
            inventoryService.addItem(canonicalId, qty)
            grantedItems += "$qty x $name"
        }
        if (grantedItems.isNotEmpty()) {
            sessionStore.setInventory(inventoryService.snapshot())
        }
        eventManager.handleTrigger(
            type = "encounter_victory",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        onVictoryProcessed()
        val outcomeMessage = if (rewardParts.isNotEmpty()) {
            "Victory reward: ${rewardParts.joinToString(", ")}"
        } else {
            "Encounter cleared."
        }
        postStatus(outcomeMessage)
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.VICTORY,
                enemyIds = enemyIds,
                message = outcomeMessage
            )
        )
        return outcomeMessage
    }

    fun processDefeat(enemyIds: List<String>, onDefeatProcessed: () -> Unit) {
        eventManager.handleTrigger(
            type = "encounter_defeat",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.DEFEAT
            )
        )
        val message = "Overwhelmed by the enemy. Regroup and recover."
        postStatus(message)
        onDefeatProcessed()
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.DEFEAT,
                enemyIds = enemyIds,
                message = message
            )
        )
    }

    fun processRetreat(enemyIds: List<String>, onRetreatProcessed: () -> Unit) {
        eventManager.handleTrigger(
            type = "encounter_retreat",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.RETREAT
            )
        )
        val message = "Retreated from combat."
        postStatus(message)
        onRetreatProcessed()
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.RETREAT,
                enemyIds = enemyIds,
                message = message
            )
        )
    }
}
