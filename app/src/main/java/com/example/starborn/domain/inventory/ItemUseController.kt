package com.example.starborn.domain.inventory

import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.combat.PartyHealth
import com.example.starborn.domain.crafting.MealRules
import com.example.starborn.domain.model.SkillTreeNode
import java.util.Locale

class ItemUseController(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore,
    private val skillNodesProvider: () -> Map<String, SkillTreeNode> = { emptyMap() },
    private val charactersProvider: () -> Map<String, Player>
) {

    sealed interface Result {
        data class Success(val result: ItemUseResult, val message: String) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun useItem(itemId: String, targetId: String? = null, replaceMeal: Boolean = false): Result {
        val item = inventoryService.itemDetail(itemId)
        val effect = item?.effect
        if (item == null || effect == null) {
            return Result.Failure("Item can't be used right now.")
        }
        val isMeal = craftingService.isPreparedMeal(item.id)
        if (item.type.equals("snack", true) || item.equipment?.slot?.equals("snack", true) == true) {
            return Result.Failure("Equip ${item.name} as a snack to use its cooldown ability in combat.")
        }
        if ((effect.damage ?: 0) > 0 || (!isMeal && (effect.singleBuff != null || !effect.buffs.isNullOrEmpty()))) {
            return Result.Failure("Use ${item.name} in combat. It does not replace your meal.")
        }
        if (isMeal && sessionStore.state.value.activeMealBuff != null && !replaceMeal) {
            return Result.Failure("Eating this replaces your active meal. Confirm replacement first.")
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
            "party", "all_allies" -> party
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
                if (isMeal) applyMealBuff(result.item)
                applyRestoration(resolvedTargets, result, characters)
                val parts = mutableListOf<String>()
                if (result.hp > 0) parts += "${result.hp} HP"
                val label = formatTargetLabel(resolvedTargets, characters)
                if (isMeal) {
                    val bonuses = result.buffs.joinToString { "${it.stat}+${it.value}" }
                    val chef = sessionStore.state.value.activeMealBuff?.chefId
                    parts += listOfNotNull(
                        "Party Well-Fed for 3 battles",
                        bonuses.takeIf { it.isNotBlank() },
                        chef?.let { "$it's serving bonus" }
                    ).joinToString("; ")
                }
                if (parts.isEmpty()) "Used ${result.item.name}."
                else "Restored ${parts.joinToString(" and ")} to $label"
            }
            is ItemUseResult.Damage -> "${result.item.name} can't be used outside combat."
            is ItemUseResult.Buff -> {
                applyMealBuff(result.item)
                val buffs = result.buffs.joinToString { "${it.stat}+${it.value}" }
                "Well-Fed: $buffs (party bonus for 3 battles)"
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
        sessionStore.setInventory(inventoryService.snapshot())
        return Result.Success(result, message)
    }

    private fun applyMealBuff(item: com.example.starborn.domain.model.Item) {
        val chef = sessionStore.state.value.mealChefId.takeIf { it in craftingService.availableChefs() }
        sessionStore.applyMealBuff(MealRules.buff(item, chef))
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
                    val updated = (current.toLong() + result.hp).coerceAtMost(maxOf(current, maxHp).toLong()).toInt()
                    sessionStore.setPartyMemberHp(targetId, updated)
                }
            }
        }
    }

    private fun maxHpFor(id: String, characters: Map<String, Player>): Int? {
        val character = characters[id] ?: return null
        return PartyHealth.maxHp(character, sessionStore.state.value, inventoryService::catalogItem, skillNodesProvider())
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
