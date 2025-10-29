package com.example.starborn.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.crafting.CraftingService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService
) : ViewModel() {

    val entries: StateFlow<List<InventoryEntry>> = inventoryService.state

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
}

class InventoryViewModelFactory(
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            return InventoryViewModel(inventoryService, craftingService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
