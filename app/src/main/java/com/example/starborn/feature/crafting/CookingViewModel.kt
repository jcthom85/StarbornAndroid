package com.example.starborn.feature.crafting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.crafting.MinigameResult
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.model.CookingRecipe
import kotlin.math.roundToLong
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CookingUiState(
    val recipes: List<CookingRecipeUi> = emptyList(),
    val isLoading: Boolean = true,
    val activeMinigame: TimingMinigameUi? = null
)

data class CookingRecipeUi(
    val id: String,
    val name: String,
    val description: String?,
    val ingredients: List<IngredientUi>,
    val canCook: Boolean,
    val difficulty: MinigameDifficulty,
    val resultLabel: String,
    val missingIngredients: List<String>
)

data class IngredientUi(
    val label: String,
    val required: Int,
    val available: Int
)

data class TimingMinigameUi(
    val recipeId: String,
    val recipeName: String,
    val progress: Float,
    val successStart: Float,
    val successEnd: Float,
    val perfectStart: Float,
    val perfectEnd: Float,
    val isRunning: Boolean,
    val difficulty: MinigameDifficulty,
    val timeRemainingSeconds: Float,
    val durationSeconds: Float
)

enum class MinigameDifficulty {
    EASY,
    NORMAL,
    HARD
}

class CookingViewModel(
    private val craftingService: CraftingService,
    private val inventoryService: InventoryService,
    private val tutorialManager: TutorialRuntimeManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private val _feedback = MutableSharedFlow<CraftingFeedback>(extraBufferCapacity = 4)
    val feedback: SharedFlow<CraftingFeedback> = _feedback.asSharedFlow()

    private var minigameJob: Job? = null

    init {
        loadRecipes()
        observeInventory()
    }

    fun startMinigame(recipeId: String) {
        val recipe = craftingService.cookingRecipes.find { it.id == recipeId }
        if (recipe == null) {
            emitFeedback(CraftingFeedback(message = "Recipe unavailable."))
            return
        }
        if (!craftingService.canCraft(recipe)) {
            emitFeedback(CraftingFeedback(message = "Missing ingredients for ${recipe.name}."))
            return
        }
        val difficulty = difficultyFor(recipe)
        val config = configFor(recipe, difficulty)
        val window = config.track

        minigameJob?.cancel()
        val initial = TimingMinigameUi(
            recipeId = recipe.id,
            recipeName = recipe.name,
            progress = 0f,
            successStart = window.successStart,
            successEnd = window.successEnd,
            perfectStart = window.perfectStart,
            perfectEnd = window.perfectEnd,
            isRunning = true,
            difficulty = difficulty,
            timeRemainingSeconds = config.durationMs / 1000f,
            durationSeconds = config.durationMs / 1000f
        )
        _uiState.update { it.copy(activeMinigame = initial) }
        recipe.minigame?.startCue?.let { cue ->
            emitFeedback(CraftingFeedback(audioCue = cue))
        }

        minigameJob = viewModelScope.launch {
            var elapsedMs = 0L
            while (isActive && elapsedMs < config.durationMs) {
                delay(TICK_MS)
                elapsedMs += TICK_MS
                val progress = progressForElapsed(elapsedMs, config.cycleDurationMs)
                _uiState.update { state ->
                    val remaining = ((config.durationMs - elapsedMs).coerceAtLeast(0L)).toFloat() / 1000f
                    state.copy(
                        activeMinigame = state.activeMinigame?.copy(
                            progress = progress,
                            timeRemainingSeconds = remaining,
                            isRunning = remaining > 0f
                        )
                    )
                }
            }
            if (isActive) {
                finishMinigame(
                    result = MinigameResult.FAILURE,
                    overrideMessage = "The dish burned before you plated it.",
                    cancelLoop = false
                )
            }
        }
    }

    fun stopMinigame() {
        val minigame = _uiState.value.activeMinigame ?: return
        val recipe = craftingService.cookingRecipes.find { it.id == minigame.recipeId }
        val result = if (recipe != null) evaluateResult(minigame, recipe) else MinigameResult.FAILURE
        finishMinigame(result)
    }

    fun cancelMinigame() {
        minigameJob?.cancel()
        _uiState.update { it.copy(activeMinigame = null) }
        emitFeedback(CraftingFeedback(message = "Cooking attempt cancelled."))
    }

    private fun evaluateResult(minigame: TimingMinigameUi, @Suppress("UNUSED_PARAMETER") recipe: CookingRecipe): MinigameResult {
        val progress = minigame.progress
        val successStart = minigame.successStart
        val successEnd = minigame.successEnd
        val perfectStart = minigame.perfectStart
        val perfectEnd = minigame.perfectEnd
        return when {
            progress in perfectStart..perfectEnd -> MinigameResult.PERFECT
            progress in successStart..successEnd -> MinigameResult.SUCCESS
            else -> MinigameResult.FAILURE
        }
    }

    private fun finishMinigame(
        result: MinigameResult,
        overrideMessage: String? = null,
        cancelLoop: Boolean = true
    ) {
        val minigame = _uiState.value.activeMinigame ?: return
        if (cancelLoop) {
            minigameJob?.cancel()
        }
        minigameJob = null
        val outcome = craftingService.craftCooking(minigame.recipeId, result)
        handleOutcome(outcome, overrideMessage)
        _uiState.update { it.copy(activeMinigame = null) }
        refreshRecipes()
    }

    private fun handleOutcome(outcome: CraftingOutcome, overrideMessage: String? = null) {
        val message = when (outcome) {
            is CraftingOutcome.Success -> overrideMessage ?: outcome.message ?: "Recipe completed."
            is CraftingOutcome.Failure -> {
                tutorialManager?.playScript("cooking_failure")
                overrideMessage ?: outcome.message ?: "The attempt failed."
            }
        }
        emitFeedback(
            CraftingFeedback(
                message = message,
                audioCue = outcome.audioCue,
                fxId = outcome.fxId
            )
        )
    }

    private fun loadRecipes() {
        val recipes = craftingService.cookingRecipes
        if (recipes.isEmpty()) {
            _uiState.value = CookingUiState(isLoading = false, recipes = emptyList())
        } else {
            _uiState.value = CookingUiState(isLoading = false, recipes = recipes.map { it.toUi() })
        }
    }

    private fun refreshRecipes() {
        _uiState.update { state ->
            state.copy(
                recipes = craftingService.cookingRecipes.map { it.toUi() }
            )
        }
    }

    private fun observeInventory() {
        viewModelScope.launch {
            inventoryService.state.collect {
                refreshRecipes()
            }
        }
    }

    private fun CookingRecipe.toUi(): CookingRecipeUi {
        val difficulty = difficultyFor(this)
        val ingredientUis = ingredients.map { (idOrAlias, required) ->
            val available = currentQuantity(idOrAlias)
            IngredientUi(
                label = idOrAlias,
                required = required,
                available = available
            )
        }
        val canCraft = ingredients.all { (idOrAlias, qty) -> inventoryService.hasItem(idOrAlias, qty) }
        val missing = ingredientUis.filter { it.available < it.required }.map(IngredientUi::label)
        return CookingRecipeUi(
            id = id,
            name = name,
            description = description,
            ingredients = ingredientUis,
            canCook = canCraft,
            difficulty = difficulty,
            resultLabel = result,
            missingIngredients = missing
        )
    }

    private fun currentQuantity(idOrAlias: String): Int {
        val entries = inventoryService.state.value
        return entries.firstOrNull { it.item.id.equals(idOrAlias, ignoreCase = true) ||
            it.item.name.equals(idOrAlias, ignoreCase = true) ||
            it.item.aliases.any { alias -> alias.equals(idOrAlias, ignoreCase = true) }
        }?.quantity ?: 0
    }

    override fun onCleared() {
        super.onCleared()
        minigameJob?.cancel()
    }

    private fun difficultyFor(recipe: CookingRecipe): MinigameDifficulty {
        val override = recipe.minigame?.difficulty?.lowercase(Locale.getDefault())
        return when (override) {
            "easy" -> MinigameDifficulty.EASY
            "hard" -> MinigameDifficulty.HARD
            "normal", "medium" -> MinigameDifficulty.NORMAL
            else -> {
                val ingredientCount = recipe.ingredients.size
                when {
                    ingredientCount <= 2 -> MinigameDifficulty.EASY
                    ingredientCount <= 4 -> MinigameDifficulty.NORMAL
                    else -> MinigameDifficulty.HARD
                }
            }
        }
    }

    private fun configFor(recipe: CookingRecipe, difficulty: MinigameDifficulty): TimingMinigameConfig {
        val base = defaultConfigFor(difficulty)
        val definition = recipe.minigame ?: return base
        val preset = difficulty.defaultPreset().withOverrides(
            successOverride = definition.window?.toFloat(),
            perfectOverride = definition.perfectWindow?.toFloat()
        )
        val duration = definition.durationMs?.coerceIn(4_000L, 15_000L) ?: base.durationMs
        val speedOverride = definition.speed?.toFloat()?.coerceIn(0.6f, 4.0f)
        val cycle = speedOverride?.times(2_000f)?.roundToLong() ?: base.cycleDurationMs
        return base.copy(
            successWidth = preset.successWidth,
            perfectWidth = preset.perfectWidth,
            cycleDurationMs = cycle,
            durationMs = duration
        )
    }

    private fun defaultConfigFor(difficulty: MinigameDifficulty): TimingMinigameConfig {
        val preset = difficulty.defaultPreset()
        return TimingMinigameConfig(
            successWidth = preset.successWidth,
            perfectWidth = preset.perfectWidth,
            cycleDurationMs = preset.cycleDurationMs,
            durationMs = when (difficulty) {
                MinigameDifficulty.EASY -> 10_000L
                MinigameDifficulty.NORMAL -> 8_500L
                MinigameDifficulty.HARD -> 7_500L
            }
        )
    }

    private fun emitFeedback(feedback: CraftingFeedback) {
        if (feedback.message == null && feedback.audioCue == null && feedback.fxId == null) return
        viewModelScope.launch {
            _feedback.emit(feedback)
        }
    }

    companion object {
        private const val TICK_MS = 16L
    }
}
