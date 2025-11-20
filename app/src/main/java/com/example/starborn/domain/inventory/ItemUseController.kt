package com.example.starborn.domain.inventory

import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.combat.CombatFormulas
import java.util.Locale

class ItemUseController(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore,
    private val charactersProvider: () -> Map<String, Player>
) {

    sealed interface Result {
        data class Success(val result: ItemUseResult, val message: String) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun useItem(itemId: String, targetId: String? = null): Result {
        val item = inventoryService.itemDetail(itemId)
        val effect = item?.effect
        if (item == null || effect == null) {
            return Result.Failure("Item can't be used right now.")
        }
        val characters = charactersProvider()
        val sessionState = sessionStore.state.value
        val party = sessionState.partyMembers.ifEmpty {
            listOfNotNull(sessionState.playerId ?: characters.keys.firstOrNull())
        }
        if (party.isEmpty()) {
            return Result.Failure("No party members available.")
        }
        val targetMode = effect.target?.lowercase(Locale.getDefault()) ?: "any"
        val resolvedTargets = when (targetMode) {
            "party" -> party
            else -> {
                val fallbackTarget = targetId
                    ?: party.firstOrNull()
                    ?: sessionState.playerId
                    ?: characters.keys.firstOrNull()
                listOfNotNull(fallbackTarget).filter { party.contains(it) }
            }
        }
        if (resolvedTargets.isEmpty()) {
            return Result.Failure("Select a valid target.")
        }
        val result = inventoryService.useItem(itemId)
            ?: return Result.Failure("You don't have that item.")
        val message = when (result) {
            is ItemUseResult.None -> "Used ${result.item.name}."
            is ItemUseResult.Restore -> {
                applyRestoration(resolvedTargets, result, characters)
                val parts = mutableListOf<String>()
                if (result.hp > 0) parts += "${result.hp} HP"
                if (result.rp > 0) parts += "${result.rp} RP"
                val label = formatTargetLabel(resolvedTargets, characters)
                if (parts.isEmpty()) "Used ${result.item.name}."
                else "Restored ${parts.joinToString(" and ")} to $label"
            }
            is ItemUseResult.Damage -> "${result.item.name} can't be used outside combat."
            is ItemUseResult.Buff -> {
                val buffs = result.buffs.joinToString { "${it.stat}+${it.value}" }
                "Buffs applied: $buffs"
            }
            is ItemUseResult.LearnSchematic -> {
                val learned = craftingService.learnSchematic(result.schematicId)
                if (learned) {
                    "Learned schematic ${result.schematicId}."
                } else {
                    "You already know schematic ${result.schematicId}."
                }
            }
        }
        return Result.Success(result, message)
    }

    private fun applyRestoration(
        targets: List<String>,
        result: ItemUseResult.Restore,
        characters: Map<String, Player>
    ) {
        val state = sessionStore.state.value
        targets.forEach { targetId ->
            if (result.hp > 0) {
                val maxHp = maxHpFor(targetId, characters)
                if (maxHp != null) {
                    val current = state.partyMemberHp[targetId] ?: maxHp
                    val updated = (current + result.hp).coerceAtMost(maxHp)
                    sessionStore.setPartyMemberHp(targetId, updated)
                }
            }
            if (result.rp > 0) {
                val maxRp = maxHpFor(targetId, characters)
                if (maxRp != null) {
                    val current = state.partyMemberRp[targetId] ?: maxRp
                    val updated = (current + result.rp).coerceAtMost(maxRp)
                    sessionStore.setPartyMemberRp(targetId, updated)
                }
            }
        }
    }

    private fun maxHpFor(id: String, characters: Map<String, Player>): Int? {
        val character = characters[id] ?: return null
        return CombatFormulas.maxHp(character.hp, character.vitality)
    }

    private fun formatTargetLabel(targets: List<String>, characters: Map<String, Player>): String {
        val labels = targets.map { id -> characters[id]?.name ?: id }
        return when (labels.size) {
            0 -> ""
            1 -> labels.first()
            2 -> labels.joinToString(" and ")
            else -> labels.dropLast(1).joinToString(", ") + " and ${labels.last()}"
        }
    }
}
