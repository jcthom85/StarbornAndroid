package com.example.starborn.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore,
    playerRoster: List<Player>
) : ViewModel() {

    val entries: StateFlow<List<InventoryEntry>> = inventoryService.state
    val equippedItems: StateFlow<Map<String, String>> = sessionStore.state
        .map { it.equippedItems }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.equippedItems
        )

    private val charactersById: Map<String, Player> = playerRoster.associateBy { it.id }

    val partyMembers: StateFlow<List<PartyMemberStatus>> = sessionStore.state
        .map { state ->
            val party = state.partyMembers.ifEmpty { listOfNotNull(state.playerId ?: charactersById.keys.firstOrNull()) }
            party.filterNotNull().map { id ->
                val character = charactersById[id]
                val baseMaxHp = character?.let { CombatFormulas.maxHp(it.hp, it.vitality) } ?: 0
                val baseMaxRp = baseMaxHp
                val currentHp = state.partyMemberHp[id] ?: baseMaxHp
                val currentRp = state.partyMemberRp[id] ?: baseMaxRp
                PartyMemberStatus(
                    id = id,
                    name = character?.name ?: id,
                    hp = currentHp,
                    maxHp = maxOf(baseMaxHp, currentHp),
                    rp = currentRp,
                    maxRp = maxOf(baseMaxRp, currentRp)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun useItem(itemId: String, targetId: String? = null) {
        val item = inventoryService.itemDetail(itemId)
        val effect = item?.effect
        if (item == null || effect == null) {
            emitMessage("Item can't be used right now.")
            return
        }
        val targetMode = effect.target?.lowercase(Locale.getDefault()) ?: "any"
        val partyState = sessionStore.state.value
        val party = partyState.partyMembers.ifEmpty {
            listOfNotNull(partyState.playerId ?: charactersById.keys.firstOrNull())
        }
        if (party.isEmpty()) {
            emitMessage("No party members available.")
            return
        }
        val resolvedTargets = when (targetMode) {
            "party" -> party
            else -> {
                val fallbackTarget = targetId
                    ?: party.firstOrNull()
                    ?: partyState.playerId
                    ?: charactersById.keys.firstOrNull()
                listOfNotNull(fallbackTarget).filter { party.contains(it) }
            }
        }
        if (resolvedTargets.isEmpty()) {
            emitMessage("Select a valid target.")
            return
        }
        val result = inventoryService.useItem(itemId)
        viewModelScope.launch {
            val message = when (result) {
                null -> "You don't have that item."
                is ItemUseResult.None -> "Used ${result.item.name}."
                is ItemUseResult.Restore -> {
                    applyRestoration(resolvedTargets, result)
                    val parts = mutableListOf<String>()
                    if (result.hp > 0) parts += "${result.hp} HP"
                    if (result.rp > 0) parts += "${result.rp} RP"
                    val label = formatTargetLabel(resolvedTargets)
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
            _messages.emit(message)
        }
    }

    private fun applyRestoration(targets: List<String>, result: ItemUseResult.Restore) {
        val state = sessionStore.state.value
        targets.forEach { targetId ->
            if (result.hp > 0) {
                val maxHp = maxHpFor(targetId)
                if (maxHp != null) {
                    val current = state.partyMemberHp[targetId] ?: maxHp
                    val updated = (current + result.hp).coerceAtMost(maxHp)
                    sessionStore.setPartyMemberHp(targetId, updated)
                }
            }
            if (result.rp > 0) {
                val maxRp = maxHpFor(targetId)
                if (maxRp != null) {
                    val current = state.partyMemberRp[targetId] ?: maxRp
                    val updated = (current + result.rp).coerceAtMost(maxRp)
                    sessionStore.setPartyMemberRp(targetId, updated)
                }
            }
        }
    }

    private fun maxHpFor(id: String): Int? {
        val character = charactersById[id] ?: return null
        return CombatFormulas.maxHp(character.hp, character.vitality)
    }

    private fun formatTargetLabel(targets: List<String>): String {
        val nameMap = partyMembers.value.associate { it.id to it.name }
        val labels = targets.map { nameMap[it] ?: it }
        return when (labels.size) {
            0 -> ""
            1 -> labels.first()
            2 -> labels.joinToString(" and ")
            else -> labels.dropLast(1).joinToString(", ") + " and ${labels.last()}"
        }
    }

    private fun emitMessage(text: String) {
        viewModelScope.launch { _messages.emit(text) }
    }

    fun equipItem(slotId: String, itemId: String?) {
        val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
        if (normalizedSlot.isBlank()) return
        if (itemId.isNullOrBlank()) {
            sessionStore.setEquippedItem(normalizedSlot, null)
            return
        }
        val entry = entries.value.firstOrNull { it.item.id == itemId } ?: return
        val equipment = entry.item.equipment ?: return
        if (!equipment.slot.equals(normalizedSlot, ignoreCase = true)) return
        sessionStore.setEquippedItem(normalizedSlot, entry.item.id)
    }
}

class InventoryViewModelFactory(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore,
    private val playerRoster: List<Player>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            return InventoryViewModel(inventoryService, craftingService, sessionStore, playerRoster) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class PartyMemberStatus(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val rp: Int,
    val maxRp: Int
)
