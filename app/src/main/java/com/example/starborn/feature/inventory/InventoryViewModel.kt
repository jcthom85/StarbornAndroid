package com.example.starborn.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseController
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val itemUseController = ItemUseController(
        inventoryService = inventoryService,
        craftingService = craftingService,
        sessionStore = sessionStore,
        charactersProvider = { charactersById }
    )

    init {
        // Prime inventory from the current session immediately so the Gear tab
        // has data on first render before the flow collector runs.
        val initialInventory = sessionStore.state.value.inventory
        if (inventoryService.snapshot() != initialInventory) {
            inventoryService.restore(initialInventory)
        }
        viewModelScope.launch {
            sessionStore.state
                .map { it.inventory }
                .distinctUntilChanged()
                .collect { inventory ->
                    if (inventoryService.snapshot() != inventory) {
                        inventoryService.restore(inventory)
                    }
                }
        }
    }

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
        viewModelScope.launch {
            when (val outcome = itemUseController.useItem(itemId, targetId)) {
                is ItemUseController.Result.Success -> _messages.emit(outcome.message)
                is ItemUseController.Result.Failure -> _messages.emit(outcome.message)
            }
            sessionStore.setInventory(inventoryService.snapshot())
        }
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
