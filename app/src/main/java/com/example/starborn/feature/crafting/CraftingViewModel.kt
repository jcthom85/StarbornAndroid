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
    val bench: TinkeringBenchState = TinkeringBenchState(),
    val inventory: List<TinkeringItemChoice> = emptyList(),
    val scrapChoices: List<TinkeringItemChoice> = emptyList()
)

data class TinkeringRecipeUi(
    val id: String,
    val name: String,
    val description: String?,
    val base: String,
    val components: List<String>,
    val resultId: String,
    val canCraft: Boolean,
    val learned: Boolean
)

data class TinkeringBenchState(
    val mainItemId: String? = null,
    val mainItemName: String? = null,
    val componentIds: List<String> = emptyList(),
    val componentNames: List<String> = emptyList(),
    val activeRecipeId: String? = null,
    val preview: TinkeringPreview? = null,
    val requirements: List<TinkeringRequirementStatus> = emptyList(),
    val canCraftSelection: Boolean = false
)

data class TinkeringRequirementStatus(
    val label: String,
    val required: Int,
    val available: Int
)

data class TinkeringItemChoice(
    val id: String,
    val name: String,
    val description: String?,
    val quantity: Int
)

data class TinkeringPreview(
    val recipeId: String,
    val name: String,
    val description: String?,
    val resultId: String,
    val learned: Boolean
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
        val inventoryChoices = inventoryService.state.value.map {
            TinkeringItemChoice(
                id = it.item.id,
                name = it.item.name,
                description = it.item.description,
                quantity = it.quantity
            )
        }
        val recipeResults = craftingService.tinkeringRecipes.flatMap { recipe ->
            listOf(
                normalizeToken(recipe.result),
                normalizeToken(recipe.name),
                normalizeToken(recipe.id)
            )
        }
        val scrapables = inventoryChoices.filter { choice ->
            val choiceTokens = buildList {
                add(normalizeToken(choice.id))
                add(normalizeToken(choice.name))
            }
            choiceTokens.any { token -> recipeResults.any { it == token } }
        }
        val currentBench = rebuildBench(_uiState.value.bench)
        _uiState.update {
            it.copy(
                learnedRecipes = learned,
                lockedRecipes = locked,
                bench = currentBench,
                inventory = inventoryChoices,
                scrapChoices = scrapables,
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
                    craftingService.learnSchematic(id)
                    sessionStore.setInventory(inventoryService.snapshot())
                    outcome.message ?: "Crafted ${outcome.itemId}."
                }
                is CraftingOutcome.Failure -> outcome.message
            }
            _messages.emit(message ?: "Crafting complete.")
            refreshState()
        }
    }

    fun craftFromBench() {
        val bench = _uiState.value.bench
        val recipeId = bench.preview?.recipeId ?: bench.activeRecipeId
        if (recipeId == null) {
            viewModelScope.launch { _messages.emit("Select an item and components to craft.") }
            return
        }
        craft(recipeId)
    }

    fun autoFill(recipeId: String) {
        val recipe = craftingService.tinkeringRecipes.find { it.id == recipeId } ?: return
        _uiState.update { it.copy(bench = benchFromRecipe(recipe)) }
    }

    fun autoFillBest() {
        val learnedRecipes = craftingService.tinkeringRecipes.filter { craftingService.isSchematicLearned(it.id) }
        val craftable = learnedRecipes.filter { craftingService.canCraft(it) }
        val preferredBase = _uiState.value.bench.mainItemId
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

    fun selectMain(itemId: String?) {
        val bench = _uiState.value.bench.copy(
            mainItemId = itemId,
            mainItemName = itemId?.let { inventoryService.itemDetail(it)?.name ?: it }
        )
        _uiState.update { it.copy(bench = rebuildBench(bench)) }
    }

    fun selectComponent(slot: Int, itemId: String?) {
        val components = _uiState.value.bench.componentIds.toMutableList()
        while (components.size < 2) components.add("")
        components[slot] = itemId ?: ""
        val names = components.map { id ->
            id.takeIf { it.isNotBlank() }?.let { inventoryService.itemDetail(it)?.name ?: it } ?: ""
        }
        val bench = _uiState.value.bench.copy(
            componentIds = components.map { it.takeIf { comp -> comp.isNotBlank() } ?: "" },
            componentNames = names
        )
        _uiState.update { it.copy(bench = rebuildBench(bench)) }
    }

    fun scrap(itemId: String) {
        val needle = normalizeToken(itemId)
        val recipe = craftingService.tinkeringRecipes.find { recipe ->
            val tokens = listOf(
                normalizeToken(recipe.result),
                normalizeToken(recipe.name),
                normalizeToken(recipe.id)
            )
            tokens.any { it == needle }
        }
        if (recipe == null) {
            viewModelScope.launch { _messages.emit("No schematic to scrap $itemId.") }
            return
        }
        if (!inventoryService.hasItem(itemId, 1)) {
            viewModelScope.launch { _messages.emit("You don't have $itemId to scrap.") }
            return
        }
        val requirements = mapOf(itemId to 1)
        if (!inventoryService.consumeItems(requirements)) {
            viewModelScope.launch { _messages.emit("Unable to scrap right now.") }
            return
        }
        inventoryService.addItem(recipe.base, 1)
        recipe.components.forEach { inventoryService.addItem(it, 1) }
        sessionStore.setInventory(inventoryService.snapshot())
        viewModelScope.launch {
            _messages.emit("Scrapped $itemId into parts.")
        }
        refreshState()
    }

    private fun benchFromRecipe(recipe: TinkeringRecipe): TinkeringBenchState {
        val requirements = requirementStatuses(recipe)
        return TinkeringBenchState(
            mainItemId = recipe.base,
            mainItemName = inventoryService.itemDetail(recipe.base)?.name ?: recipe.base,
            componentIds = recipe.components,
            componentNames = recipe.components.map { id -> inventoryService.itemDetail(id)?.name ?: id },
            activeRecipeId = recipe.id,
            preview = TinkeringPreview(
                recipeId = recipe.id,
                name = recipe.name,
                description = recipe.description,
                resultId = recipe.result,
                learned = craftingService.isSchematicLearned(recipe.id)
            ),
            requirements = requirements,
            canCraftSelection = craftingService.canCraft(recipe)
        )
    }

    private fun rebuildBench(current: TinkeringBenchState): TinkeringBenchState {
        val mainId = current.mainItemId?.takeIf { it.isNotBlank() }
        val components = current.componentIds.filter { it.isNotBlank() }.take(2)
        val recipe = findMatchingRecipe(mainId, components)
        val requirements = recipe?.let { requirementStatuses(it) } ?: emptyList()
        val canCraft = recipe?.let { craftingService.canCraft(it) } ?: false
        val preview = recipe?.let {
            TinkeringPreview(
                recipeId = it.id,
                name = it.name,
                description = it.description,
                resultId = it.result,
                learned = craftingService.isSchematicLearned(it.id)
            )
        }
        val componentNames = components.map { id -> inventoryService.itemDetail(id)?.name ?: id }
        return current.copy(
            mainItemId = mainId,
            mainItemName = mainId?.let { inventoryService.itemDetail(it)?.name ?: it },
            componentIds = components,
            componentNames = componentNames,
            activeRecipeId = recipe?.id,
            preview = preview,
            requirements = requirements,
            canCraftSelection = canCraft
        )
    }

    private fun findMatchingRecipe(mainId: String?, components: List<String>): TinkeringRecipe? {
        if (mainId == null) return null
        val mainKey = normalizeToken(mainId)
        val componentCounts = components.filter { it.isNotBlank() }.groupingBy { normalizeToken(it) }.eachCount()
        return craftingService.tinkeringRecipes.firstOrNull { recipe ->
            val recipeMain = normalizeToken(recipe.base)
            val recipeComponents = recipe.components.groupingBy { normalizeToken(it) }.eachCount()
            recipeMain == mainKey && countsEqual(componentCounts, recipeComponents)
        }
    }

    private fun countsEqual(a: Map<String, Int>, b: Map<String, Int>): Boolean {
        if (a.size != b.size) return false
        return a.all { (k, v) -> b[k] == v }
    }

    private fun normalizeToken(raw: String): String =
        raw.trim().lowercase().replace("[^a-z0-9]+".toRegex(), "")


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
            resultId = result,
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
