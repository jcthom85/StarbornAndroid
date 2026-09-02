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

    fun canCook(recipe: CookingRecipe, batch: Int = 1): Boolean {
        val multiplier = batch.coerceAtLeast(1)
        val requirementCounts = recipe.ingredients.mapKeys { (item, _) -> normalizeToken(item) }
        val inventoryCounts = inventoryTokenCounts()
        return requirementCounts.all { (id, needed) -> (inventoryCounts[id] ?: 0) >= (needed * multiplier) }
    }

    fun cookMeal(recipeId: String, chefId: String? = null, batch: Int = 1): CraftingOutcome {
        val recipe = cookingRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        val multiplier = batch.coerceAtLeast(1)
        if (!canCook(recipe, multiplier)) return CraftingOutcome.Failure("Missing ingredients")
        val scaledIngredients = recipe.ingredients.mapValues { it.value * multiplier }
        if (!inventoryService.consumeItems(scaledIngredients)) return CraftingOutcome.Failure("Unable to consume ingredients")

        val isMasterwork = (Math.random() < 0.15)
        val extraYield = if (isMasterwork) 1 else 0
        val totalYield = (recipe.resultQuantity.coerceAtLeast(1) * multiplier) + extraYield
        inventoryService.addItem(recipe.result, totalYield)
        sessionStore.setInventory(inventoryService.snapshot())

        // If a chef is designated, record active meal perk for the session
        if (chefId != null) {
            val chefPerkBuff = when (chefId.lowercase(java.util.Locale.getDefault())) {
                "nova" -> com.example.starborn.domain.session.ActiveMealBuff(
                    recipeId = recipe.id,
                    recipeName = recipe.name,
                    chefId = "nova",
                    remainingEncounters = 3,
                    focusBonus = 10
                )
                "zeke" -> com.example.starborn.domain.session.ActiveMealBuff(
                    recipeId = recipe.id,
                    recipeName = recipe.name,
                    chefId = "zeke",
                    remainingEncounters = 3,
                    hpBonus = 25,
                    stabilityBonus = 3
                )
                "gh0st" -> com.example.starborn.domain.session.ActiveMealBuff(
                    recipeId = recipe.id,
                    recipeName = recipe.name,
                    chefId = "gh0st",
                    remainingEncounters = 3,
                    speedBonus = 5,
                    statusResistBonus = 20
                )
                "orion" -> com.example.starborn.domain.session.ActiveMealBuff(
                    recipeId = recipe.id,
                    recipeName = recipe.name,
                    chefId = "orion",
                    remainingEncounters = 3,
                    critBonus = 0.08
                )
                else -> null
            }
            if (chefPerkBuff != null) {
                sessionStore.applyMealBuff(chefPerkBuff)
            }
        }

        val masterworkMsg = if (isMasterwork) " ⭐ Masterwork Sizzle! (+1 extra portion)" else ""
        val chefMsg = when (chefId?.lowercase(java.util.Locale.getDefault())) {
            "nova" -> " Nova balanced the macros (+Focus)."
            "zeke" -> " Zeke charred it over iron (+HP & Stability)."
            "gh0st" -> " Gh0st steeped pungent herbs (+Spd & Resist)."
            "orion" -> " Orion plated with precision (+Crit)."
            else -> ""
        }
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
