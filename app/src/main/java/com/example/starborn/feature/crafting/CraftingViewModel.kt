package com.example.starborn.feature.crafting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CraftingViewModel(
    private val craftingService: CraftingService,
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore
) : ViewModel() {
    val runtime = CraftingController(craftingService, inventoryService, sessionStore, parentScope = viewModelScope)
    val uiState get() = runtime.uiState
    val messages get() = runtime.messages
    val craftResults get() = runtime.craftResults
    fun dismissFeedback() = runtime.dismissFeedback()
    fun learnFoundSchematic(itemId: String) = runtime.learnFoundSchematic(itemId)
    fun craft(id: String) = runtime.craft(id)
    fun craftFromBench() = runtime.craftFromBench()
    fun autoFill(recipeId: String) = runtime.autoFill(recipeId)
    fun autoFillBest() = runtime.autoFillBest()
    fun clearBench() = runtime.clearBench()
    fun setFilter(filter: TinkeringFilter) = runtime.setFilter(filter)
    fun setInitialFilter(filter: TinkeringFilter?) = runtime.setInitialFilter(filter)
    fun selectMain(itemId: String?) = runtime.selectMain(itemId)
    fun selectComponent(slot: Int, itemId: String?) = runtime.selectComponent(slot, itemId)
    fun scrap(itemId: String) = runtime.scrap(itemId)
    override fun onCleared() { runtime.close() }
}

class CraftingViewModelFactory(
    private val craftingService: CraftingService,
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CraftingViewModel::class.java)) {
            return CraftingViewModel(craftingService, inventoryService, sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
