package com.example.starborn.domain.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.TinkeringRecipe

class CraftingService(
    private val craftingDataSource: CraftingRecipeSource,
    private val inventoryService: InventoryService,
    private val sessionStore: GameSessionStore
) {
    val tinkeringRecipes: List<TinkeringRecipe> by lazy { craftingDataSource.loadTinkeringRecipes() }
    val cookingRecipes: List<CookingRecipe> by lazy { craftingDataSource.loadCookingRecipes() }

    val sessionState get() = sessionStore.state
    fun availableChefs(): List<String> = sessionStore.state.value.let { state ->
        state.partyMembers.ifEmpty { listOf(state.playerId ?: "nova") }
            .filter { it in setOf("nova", "zeke", "gh0st", "orion") }
    }
    fun selectChef(id: String) = sessionStore.selectMealChef(id)
    fun mealEffects(recipe: CookingRecipe): String {
        val effect = inventoryService.catalogItem(recipe.result)?.effect ?: return "No meal effect"
        val bonuses = effect.buffs.orEmpty() + listOfNotNull(effect.singleBuff)
        return listOfNotNull(effect.restoreHp?.takeIf { it > 0 }?.let { "Restores $it HP" },
            bonuses.takeIf { it.isNotEmpty() }?.joinToString { "${it.stat} +${it.value}" })
            .joinToString(" · ") + " · Meal bonuses last 3 encounters"
    }

    fun usesFor(itemId: String): List<String> = buildList {
        val meals = cookingRecipes.filter { itemId in it.ingredients }.map { it.name }
        val gear = tinkeringRecipes.filter { itemId in ingredientsFor(it) }.map { it.name }
        if (meals.isNotEmpty()) add("Cooking: ${meals.take(3).joinToString()}${if (meals.size > 3) " and more" else ""}")
        if (gear.isNotEmpty()) add("Tinkering: ${gear.take(3).joinToString()}${if (gear.size > 3) " and more" else ""}")
    }

    fun sharedIngredientNotes(recipe: CookingRecipe): List<String> = recipe.ingredients.keys.mapNotNull { id ->
        val names = tinkeringRecipes.filter { isSchematicLearned(it.id) && id in ingredientsFor(it) }.map { it.name }
        if (names.isEmpty()) null else "${inventoryService.itemDisplayName(id)} is also used in ${names.joinToString()}."
    }

    fun canCook(recipe: CookingRecipe, batch: Int = 1): Boolean {
        if (batch <= 0 || recipe.ingredients.isEmpty() || recipe.ingredients.values.any { it <= 0 }) return false
        val multiplier = batch.coerceAtLeast(1)
        val requirementCounts = recipe.ingredients.mapKeys { (item, _) -> normalizeToken(item) }
        val inventoryCounts = inventoryTokenCounts()
        val resultId = inventoryService.catalogItem(recipe.result)?.id ?: recipe.result
        // Reserve the possible masterwork portion before consuming anything.
        val maxYield = recipe.resultQuantity.coerceAtLeast(1).toLong() * multiplier + 1
        if (maxYield + (inventoryService.snapshot()[resultId] ?: 0) > Int.MAX_VALUE) return false
        return requirementCounts.all { (id, needed) -> (inventoryCounts[id] ?: 0).toLong() >= needed.toLong() * multiplier }
    }

    fun cookMeal(recipeId: String, chefId: String? = null, batch: Int = 1): CraftingOutcome {
        val recipe = cookingRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (batch <= 0) return CraftingOutcome.Failure("Invalid batch size")
        if (chefId != null && chefId !in availableChefs()) return CraftingOutcome.Failure("That companion is not available.")
        val multiplier = batch.coerceAtLeast(1)
        if (!canCook(recipe, multiplier)) return CraftingOutcome.Failure("Missing ingredients")
        val scaledIngredients = recipe.ingredients.mapValues { it.value * multiplier }
        if (!inventoryService.consumeItems(scaledIngredients)) return CraftingOutcome.Failure("Unable to consume ingredients")
        if (chefId != null) sessionStore.selectMealChef(chefId)

        val isMasterwork = (Math.random() < 0.15)
        val extraYield = if (isMasterwork) 1 else 0
        val totalYield = (recipe.resultQuantity.coerceAtLeast(1) * multiplier) + extraYield
        inventoryService.addItem(recipe.result, totalYield)
        sessionStore.setInventory(inventoryService.snapshot())

        val masterworkMsg = if (isMasterwork) " Masterwork! (+1 extra portion)" else ""
        val chefMsg = " Eat from inventory to activate meal benefits."
        val baseMsg = recipe.successMessage ?: "Prepared ${recipe.name}"
        return CraftingOutcome.Success(recipe.result, "$baseMsg (x$totalYield)$masterworkMsg$chefMsg")
    }

    fun canCraft(recipe: TinkeringRecipe): Boolean {
        val requirements = ingredientsFor(recipe)
        if (requirements.isEmpty()) return false
        val requirementCounts = requirements.mapKeys { (item, _) -> normalizeToken(item) }
        val inventoryCounts = inventoryTokenCounts()
        val hasIngredients = requirementCounts.all { (id, needed) -> (inventoryCounts[id] ?: 0) >= needed }
        if (!hasIngredients) return false
        return recipe.tools.all { tool ->
            val normalizedTool = normalizeToken(tool)
            normalizedTool.isNotBlank() && (inventoryCounts[normalizedTool] ?: 0) >= 1
        }
    }

    private fun normalizeToken(raw: String): String =
        raw.trim().lowercase().replace("[^a-z0-9]+".toRegex(), "")

    private fun inventoryTokenCounts(): Map<String, Int> {
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
        return inventoryCounts
    }

    fun learnSchematic(schematicId: String): Boolean {
        if (schematicId.isBlank()) return false
        if (isSchematicLearned(schematicId)) return false
        sessionStore.learnSchematic(schematicId)
        return true
    }

    fun isSchematicLearned(schematicId: String): Boolean =
        schematicId.isNotBlank() && schematicId in sessionStore.state.value.learnedSchematics

    fun craftTinkering(recipeId: String): CraftingOutcome {
        val recipe = tinkeringRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing components or tools")
        val requirements = ingredientsFor(recipe)
        if (!inventoryService.consumeItems(requirements)) return CraftingOutcome.Failure("Unable to consume components")
        val addedId = addCraftedItem(recipe)
        // Keep session inventory in sync for downstream screens (inventory, save).
        sessionStore.setInventory(inventoryService.snapshot())
        recipe.successMessage?.let { return CraftingOutcome.Success(addedId, it) }
        return CraftingOutcome.Success(addedId, "Crafted ${recipe.name}")
    }

    fun ingredientsFor(recipe: TinkeringRecipe): Map<String, Int> {
        if (recipe.ingredients.isNotEmpty()) {
            return recipe.ingredients
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, qty) -> qty.coerceAtLeast(1) }
        }
        val requirements = mutableMapOf<String, Int>()
        recipe.base?.takeIf { it.isNotBlank() }?.let { base ->
            requirements[base] = requirements.getOrDefault(base, 0) + 1
        }
        recipe.components.forEach { component ->
            if (component.isNotBlank()) {
                requirements[component] = requirements.getOrDefault(component, 0) + 1
            }
        }
        return requirements
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
        inventoryService.addItem(resolvedId, recipe.resultQuantity.coerceAtLeast(1))
        val afterQty = inventoryService.snapshot()[resolvedId] ?: 0
        if (afterQty <= beforeQty) {
            // Guarantee the crafted item is present even if the first add failed to change quantity.
            inventoryService.addItem(resolvedId, recipe.resultQuantity.coerceAtLeast(1))
        }
        return inventoryService.itemDetail(resolvedId)?.id ?: resolvedId
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
