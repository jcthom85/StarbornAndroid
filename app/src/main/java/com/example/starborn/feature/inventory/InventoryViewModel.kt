package com.example.starborn.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

class InventoryViewModel(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val sessionStore: GameSessionStore
) : ViewModel() {

    val entries: StateFlow<List<InventoryEntry>> = inventoryService.state
    val equippedItems: StateFlow<Map<String, String>> = sessionStore.state
        .map { it.equippedItems }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sessionStore.state.value.equippedItems
        )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun useItem(itemId: String) {
        val result = inventoryService.useItem(itemId)
        viewModelScope.launch {
            val message = when (result) {
                null -> "You don't have that item."
                is ItemUseResult.None -> "Used ${result.item.name}."
                is ItemUseResult.Restore -> buildString {
                    append("Restored ")
                    val parts = mutableListOf<String>()
                    if (result.hp > 0) parts += "${result.hp} HP"
                    if (result.rp > 0) parts += "${result.rp} RP"
                    append(parts.joinToString(" & "))
                }
                is ItemUseResult.Damage -> "${result.item.name} deals ${result.amount} damage."
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
    private val sessionStore: GameSessionStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            return InventoryViewModel(inventoryService, craftingService, sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
