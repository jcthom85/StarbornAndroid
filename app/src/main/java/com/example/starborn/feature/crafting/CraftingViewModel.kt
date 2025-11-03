package com.example.starborn.feature.crafting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.model.TinkeringRecipe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CraftingViewModel(
    private val craftingService: CraftingService
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<TinkeringRecipe>>(emptyList())
    val recipes: StateFlow<List<TinkeringRecipe>> = _recipes.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _craftResults = MutableSharedFlow<CraftingOutcome.Success>()
    val craftResults: SharedFlow<CraftingOutcome.Success> = _craftResults.asSharedFlow()

    init {
        loadRecipes()
    }

    private fun loadRecipes() {
        _recipes.value = craftingService.tinkeringRecipes
    }

    fun craft(id: String) {
        viewModelScope.launch {
            val outcome = craftingService.craftTinkering(id)
            val message = when (outcome) {
                is CraftingOutcome.Success -> {
                    _craftResults.emit(outcome)
                    outcome.message ?: "Crafted ${outcome.itemId}."
                }
                is CraftingOutcome.Failure -> outcome.message
            }
            _messages.emit(message ?: "Crafting complete.")
            loadRecipes()
        }
    }
}

class CraftingViewModelFactory(
    private val craftingService: CraftingService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CraftingViewModel::class.java)) {
            return CraftingViewModel(craftingService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
