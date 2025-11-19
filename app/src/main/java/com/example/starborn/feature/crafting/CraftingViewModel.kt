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

data class TinkeringUiState(
    val isLoading: Boolean = true,
    val filter: TinkeringFilter = TinkeringFilter.LEARNED,
    val learnedRecipes: List<TinkeringRecipeUi> = emptyList(),
    val lockedRecipes: List<TinkeringRecipeUi> = emptyList(),
    val bench: TinkeringBenchState = TinkeringBenchState()
)

data class TinkeringRecipeUi(
    val id: String,
    val name: String,
    val description: String?,
    val base: String,
    val components: List<String>,
    val canCraft: Boolean,
    val learned: Boolean
)

data class TinkeringBenchState(
    val selectedBase: String? = null,
    val selectedComponents: List<String> = emptyList(),
    val activeRecipeId: String? = null,
    val requirements: List<TinkeringRequirementStatus> = emptyList(),
    val canCraftSelection: Boolean = false
)

data class TinkeringRequirementStatus(
    val label: String,
    val required: Int,
    val available: Int
)

enum class TinkeringFilter { LEARNED, ALL }

class CraftingViewModel(
    private val craftingService: CraftingService,
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TinkeringUiState())
    val uiState: StateFlow<TinkeringUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _craftResults = MutableSharedFlow<CraftingOutcome.Success>()
    val craftResults: SharedFlow<CraftingOutcome.Success> = _craftResults.asSharedFlow()

    init {
        refreshState()
        observeInventory()
        observeSchematics()
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryService.state.collect {
                refreshState()
            }
        }
    }

    private fun observeSchematics() {
        viewModelScope.launch {
            sessionStore.state.collect {
                refreshState()
            }
        }
    }

    private fun refreshState() {
        val learnedIds = sessionStore.state.value.learnedSchematics
        val learned = mutableListOf<TinkeringRecipeUi>()
        val locked = mutableListOf<TinkeringRecipeUi>()
        craftingService.tinkeringRecipes.forEach { recipe ->
            val learnedFlag = recipe.id in learnedIds
            val ui = recipe.toUi(learnedFlag)
            if (learnedFlag) {
                learned.add(ui)
            } else {
                locked.add(ui)
            }
        }
        val currentBench = rebuildBench(_uiState.value.bench)
        _uiState.update {
            it.copy(
                learnedRecipes = learned,
                lockedRecipes = locked,
                bench = currentBench,
                isLoading = false
            )
        }
    }

    fun craft(id: String) {
        viewModelScope.launch {
            val outcome = craftingService.craftTinkering(id)
            val message = when (outcome) {
                is CraftingOutcome.Success -> {
                    _craftResults.emit(outcome)
                    if (_uiState.value.bench.activeRecipeId == id) {
                        _uiState.update { it.copy(bench = TinkeringBenchState()) }
                    }
                    outcome.message ?: "Crafted ${outcome.itemId}."
                }
                is CraftingOutcome.Failure -> outcome.message
            }
            _messages.emit(message ?: "Crafting complete.")
            refreshState()
        }
    }

    fun craftFromBench() {
        val recipeId = _uiState.value.bench.activeRecipeId ?: return
        craft(recipeId)
    }

    fun autoFill(recipeId: String) {
        val recipe = craftingService.tinkeringRecipes.find { it.id == recipeId } ?: return
        _uiState.update { it.copy(bench = benchFromRecipe(recipe)) }
    }

    fun autoFillBest() {
        val learnedRecipes = craftingService.tinkeringRecipes.filter { craftingService.isSchematicLearned(it.id) }
        val craftable = learnedRecipes.filter { craftingService.canCraft(it) }
        val preferredBase = _uiState.value.bench.selectedBase
        val match = if (preferredBase != null) {
            craftable.firstOrNull { it.base.equals(preferredBase, ignoreCase = true) } ?: craftable.firstOrNull()
        } else {
            craftable.firstOrNull()
        }
        if (match != null) {
            _uiState.update { it.copy(bench = benchFromRecipe(match)) }
        } else {
            viewModelScope.launch {
                _messages.emit("No craftable schematics with current supplies.")
            }
        }
    }

    fun clearBench() {
        _uiState.update { it.copy(bench = TinkeringBenchState()) }
    }

    fun setFilter(filter: TinkeringFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    private fun rebuildBench(current: TinkeringBenchState): TinkeringBenchState {
        val active = current.activeRecipeId?.let { id -> craftingService.tinkeringRecipes.find { it.id == id } }
        return if (active != null) {
            val requirements = requirementStatuses(active)
            current.copy(
                selectedBase = active.base,
                selectedComponents = active.components,
                requirements = requirements,
                canCraftSelection = craftingService.canCraft(active)
            )
        } else {
            current.copy(
                selectedBase = null,
                selectedComponents = emptyList(),
                activeRecipeId = null,
                requirements = emptyList(),
                canCraftSelection = false
            )
        }
    }

    private fun benchFromRecipe(recipe: TinkeringRecipe): TinkeringBenchState {
        val requirements = requirementStatuses(recipe)
        return TinkeringBenchState(
            selectedBase = recipe.base,
            selectedComponents = recipe.components,
            activeRecipeId = recipe.id,
            requirements = requirements,
            canCraftSelection = craftingService.canCraft(recipe)
        )
    }

    private fun requirementStatuses(recipe: TinkeringRecipe): List<TinkeringRequirementStatus> {
        val counts = mutableMapOf<String, Int>()
        counts[recipe.base] = counts.getOrDefault(recipe.base, 0) + 1
        recipe.components.forEach { component ->
            counts[component] = counts.getOrDefault(component, 0) + 1
        }
        return counts.map { (item, needed) ->
            val available = inventoryQuantity(item)
            TinkeringRequirementStatus(
                label = item,
                required = needed,
                available = available
            )
        }
    }

    private fun inventoryQuantity(idOrName: String): Int {
        val normalized = idOrName.trim()
        return inventoryService.state.value.firstOrNull { entry ->
            val idMatches = entry.item.id.equals(normalized, ignoreCase = true)
            val nameMatches = entry.item.name.equals(normalized, ignoreCase = true)
            val aliasMatches = entry.item.aliases.any { it.equals(normalized, ignoreCase = true) }
            idMatches || nameMatches || aliasMatches
        }?.quantity ?: 0
    }

    private fun TinkeringRecipe.toUi(learned: Boolean): TinkeringRecipeUi =
        TinkeringRecipeUi(
            id = id,
            name = name,
            description = description,
            base = base,
            components = components,
            canCraft = craftingService.canCraft(this),
            learned = learned
        )

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
