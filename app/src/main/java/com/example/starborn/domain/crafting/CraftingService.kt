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
        val baseOk = inventoryService.hasItem(recipe.base, 1)
        val componentsOk = recipe.components.all { inventoryService.hasItem(it, 1) }
        return baseOk && componentsOk
    }

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

    fun craftCooking(recipeId: String): CraftingOutcome {
        val recipe = cookingRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing ingredients")
        if (!inventoryService.consumeItems(recipe.ingredients)) return CraftingOutcome.Failure("Unable to consume ingredients")
        inventoryService.addItem(recipe.result, 1)
        return CraftingOutcome.Success(recipe.result, message = "Cooked ${recipe.name}")
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
        inventoryService.addItem(recipe.result, 1)
        recipe.successMessage?.let { return CraftingOutcome.Success(recipe.result, it) }
        return CraftingOutcome.Success(recipe.result, "Crafted ${recipe.name}")
    }

    fun craftFirstAid(recipeId: String): CraftingOutcome {
        val recipe = firstAidRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing ingredients")
        if (!inventoryService.consumeItems(recipe.ingredients)) return CraftingOutcome.Failure("Unable to consume ingredients")
        val resultId = recipe.output.success
        inventoryService.addItem(resultId, 1)
        return CraftingOutcome.Success(resultId, "Prepared ${recipe.name}")
    }
}

sealed interface CraftingOutcome {
    val itemId: String?
    val message: String?

    data class Success(override val itemId: String, override val message: String?) : CraftingOutcome
    data class Failure(override val message: String) : CraftingOutcome {
        override val itemId: String? = null
    }
}
