package com.example.starborn.domain.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.FirstAidRecipe
import com.example.starborn.domain.model.TinkeringRecipe

class CraftingService(
    private val craftingDataSource: CraftingRecipeSource,
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore
) {
    val tinkeringRecipes: List<TinkeringRecipe> by lazy { craftingDataSource.loadTinkeringRecipes() }
    val cookingRecipes: List<CookingRecipe> by lazy { craftingDataSource.loadCookingRecipes() }
    val firstAidRecipes: List<FirstAidRecipe> by lazy { craftingDataSource.loadFirstAidRecipes() }

    fun canCraft(recipe: CookingRecipe): Boolean = recipe.ingredients.all { (itemId, qty) ->
        inventoryService.hasItem(itemId, qty)
    }

    fun canCraft(recipe: TinkeringRecipe): Boolean {
        val normalizedBase = normalizeToken(recipe.base)
        val componentCounts = recipe.components.groupingBy { normalizeToken(it) }.eachCount()
        val inventoryCounts = mutableMapOf<String, Int>()
        inventoryService.state.value.forEach { entry ->
            val tokens = buildList {
                add(normalizeToken(entry.item.id))
                add(normalizeToken(entry.item.name))
                entry.item.aliases.forEach { add(normalizeToken(it)) }
            }.distinct()
            tokens.forEach { key ->
                inventoryCounts[key] = inventoryCounts.getOrDefault(key, 0) + entry.quantity
            }
        }
        val baseOk = (inventoryCounts[normalizedBase] ?: 0) >= 1
        val componentsOk = componentCounts.all { (id, needed) -> (inventoryCounts[id] ?: 0) >= needed }
        return baseOk && componentsOk
    }

    private fun normalizeToken(raw: String): String =
        raw.trim().lowercase().replace("[^a-z0-9]+".toRegex(), "")

    fun canCraft(recipe: FirstAidRecipe): Boolean = recipe.ingredients.all { (itemId, qty) ->
        inventoryService.hasItem(itemId, qty)
    }

    fun learnSchematic(schematicId: String): Boolean {
        if (schematicId.isBlank()) return false
        if (isSchematicLearned(schematicId)) return false
        sessionStore.learnSchematic(schematicId)
        return true
    }

    fun isSchematicLearned(schematicId: String): Boolean =
        schematicId.isNotBlank() && schematicId in sessionStore.state.value.learnedSchematics

    fun craftCooking(recipeId: String, outcome: MinigameResult = MinigameResult.SUCCESS): CraftingOutcome {
        val recipe = cookingRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing ingredients")
        if (!inventoryService.consumeItems(recipe.ingredients)) return CraftingOutcome.Failure("Unable to consume ingredients")
        val minigame = recipe.minigame
        return when (outcome) {
            MinigameResult.FAILURE -> CraftingOutcome.Failure(
                message = "The recipe went wrong. Try again.",
                audioCue = minigame?.failureCue,
                fxId = minigame?.failureFx
            )
            MinigameResult.SUCCESS -> {
                inventoryService.addItem(recipe.result, 1)
                CraftingOutcome.Success(
                    itemId = recipe.result,
                    message = "Cooked ${recipe.name}",
                    audioCue = minigame?.successCue,
                    fxId = minigame?.successFx
                )
            }
            MinigameResult.PERFECT -> {
                inventoryService.addItem(recipe.result, 1)
                CraftingOutcome.Success(
                    itemId = recipe.result,
                    message = "Perfectly cooked ${recipe.name}!",
                    audioCue = minigame?.perfectCue ?: minigame?.successCue,
                    fxId = minigame?.successFx
                )
            }
        }
    }

    fun craftTinkering(recipeId: String): CraftingOutcome {
        val recipe = tinkeringRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing components")
        val requirements = mutableMapOf<String, Int>()
        requirements[recipe.base] = requirements.getOrDefault(recipe.base, 0) + 1
        recipe.components.forEach { component ->
            requirements[component] = requirements.getOrDefault(component, 0) + 1
        }
        if (!inventoryService.consumeItems(requirements)) return CraftingOutcome.Failure("Unable to consume components")
        val addedId = addCraftedItem(recipe)
        // Keep session inventory in sync for downstream screens (inventory, save).
        sessionStore.setInventory(inventoryService.snapshot())
        recipe.successMessage?.let { return CraftingOutcome.Success(addedId, it) }
        return CraftingOutcome.Success(addedId, "Crafted ${recipe.name}")
    }

    private fun matchingCount(needle: String): Int {
        val token = normalizeToken(needle)
        return inventoryService.state.value.sumOf { entry ->
            val tokens = buildList {
                add(normalizeToken(entry.item.id))
                add(normalizeToken(entry.item.name))
                entry.item.aliases.forEach { add(normalizeToken(it)) }
            }
            if (tokens.any { it == token }) entry.quantity else 0
        }
    }

    private fun addCraftedItem(recipe: TinkeringRecipe): String {
        val candidates = listOf(recipe.result, recipe.id, recipe.name)
            .mapNotNull { it.trim().ifBlank { null } }

        val resolvedId = candidates
            .asSequence()
            .mapNotNull { candidate ->
                inventoryService.catalogItem(candidate)?.id
                    ?: inventoryService.catalogItem(candidate.replace("\\s+".toRegex(), "_"))?.id
                    ?: inventoryService.catalogItem(normalizeToken(candidate))?.id
            }
            .firstOrNull()
            ?: inventoryService.itemDetail(candidates.first())?.id
            ?: normalizeToken(recipe.result.ifBlank { recipe.id.ifBlank { recipe.name } })

        val beforeQty = inventoryService.snapshot()[resolvedId] ?: 0
        inventoryService.addItem(resolvedId, 1)
        val afterQty = inventoryService.snapshot()[resolvedId] ?: 0
        if (afterQty <= beforeQty) {
            // Guarantee the crafted item is present even if the first add failed to change quantity.
            inventoryService.addItem(resolvedId, 1)
        }
        return inventoryService.itemDetail(resolvedId)?.id ?: resolvedId
    }

    fun craftFirstAid(recipeId: String, outcome: MinigameResult = MinigameResult.SUCCESS): CraftingOutcome {
        val recipe = firstAidRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing ingredients")
        if (!inventoryService.consumeItems(recipe.ingredients)) return CraftingOutcome.Failure("Unable to consume ingredients")
        val minigame = recipe.minigame
        return when (outcome) {
            MinigameResult.FAILURE -> {
                val failureId = recipe.output.failure
                if (failureId.isNullOrBlank()) {
                    CraftingOutcome.Failure(
                        message = "The kit falls apart in your hands.",
                        audioCue = minigame?.failureCue,
                        fxId = minigame?.failureFx
                    )
                } else {
                    inventoryService.addItem(failureId, 1)
                    CraftingOutcome.Success(
                        itemId = failureId,
                        message = "You salvage $failureId from the failed attempt.",
                        audioCue = minigame?.failureCue,
                        fxId = minigame?.failureFx
                    )
                }
            }
            MinigameResult.SUCCESS -> {
                val resultId = recipe.output.success
                inventoryService.addItem(resultId, 1)
                CraftingOutcome.Success(
                    itemId = resultId,
                    message = "Prepared ${recipe.name}",
                    audioCue = minigame?.successCue,
                    fxId = minigame?.successFx
                )
            }
            MinigameResult.PERFECT -> {
                val perfectId = recipe.output.perfect ?: recipe.output.success
                inventoryService.addItem(perfectId, 1)
                CraftingOutcome.Success(
                    itemId = perfectId,
                    message = "Perfectly prepared ${recipe.name}!",
                    audioCue = minigame?.perfectCue ?: minigame?.successCue,
                    fxId = minigame?.successFx
                )
            }
        }
    }
}

enum class MinigameResult {
    PERFECT,
    SUCCESS,
    FAILURE
}

sealed interface CraftingOutcome {
    val itemId: String?
    val message: String?
    val audioCue: String?
    val fxId: String?

    data class Success(
        override val itemId: String,
        override val message: String?,
        override val audioCue: String? = null,
        override val fxId: String? = null
    ) : CraftingOutcome
    data class Failure(
        override val message: String,
        override val audioCue: String? = null,
        override val fxId: String? = null
    ) : CraftingOutcome {
        override val itemId: String? = null
    }
}
