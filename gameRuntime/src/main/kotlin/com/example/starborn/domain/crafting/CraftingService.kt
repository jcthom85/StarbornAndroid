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
    fun isPreparedMeal(itemId: String): Boolean = cookingRecipes.any { it.category == "meal" && it.result == itemId }

    fun isRecipeDiscovered(recipe: CookingRecipe): Boolean = recipe.discoveryRoom == null ||
        sessionStore.state.value.roomId == recipe.discoveryRoom ||
        sessionStore.state.value.roomStates[recipe.discoveryRoom]?.get("cooking_discovered") == true

    fun mealEffects(recipe: CookingRecipe): String {
        val item = inventoryService.catalogItem(recipe.result) ?: return "Unknown result"
        val effect = item.effect ?: return "No effect"
        if (recipe.category == "snack") {
            val effects = effect.buffs.orEmpty() + listOfNotNull(effect.singleBuff)
            val active = effects.joinToString(" · ") { "${MealRules.bonusLabel(it.stat, it.value)} for ${it.duration ?: 1} turns" }
            val passive = item.equipment?.statMods.orEmpty().entries.joinToString { MealRules.bonusLabel(it.key, it.value) }
            return listOfNotNull("Equip in a party member's snack slot.",
                passive.takeIf { it.isNotBlank() }?.let { "While equipped: $it" },
                item.equipment?.hpBonus?.let { "While equipped: Max HP +$it" },
                "Ability: $active", "Target: ${effect.target ?: "self"} · Cooldown: ${effect.cooldown ?: 5} turns.",
                "Using the ability does not consume the snack.").joinToString("\n")
        }
        val scope = if (effect.target in listOf("party", "all_allies")) "all allies" else "one ally"
        val chef = sessionStore.state.value.mealChefId.takeIf { it in availableChefs() }
        val serving = MealRules.buff(item, chef)
        val combatBuffs = (effect.buffs.orEmpty() + listOfNotNull(effect.singleBuff)).joinToString(" · ") {
            "${MealRules.bonusLabel(it.stat, it.value)} (${it.duration ?: 1} turns)"
        }
        return listOfNotNull(effect.restoreHp?.let { "Restores $it HP to $scope." },
            "Eat before combat: party Well-Fed for 3 completed battles, including escapes.",
            MealRules.summary(serving),
            chef?.let { "Serving companion: $it (${MealRules.chefDescription(it)})." },
            combatBuffs.takeIf { it.isNotBlank() }?.let { "Use in combat: $it to $scope; no Well-Fed or companion bonus." }
        ).joinToString("\n")
    }

    fun usesFor(itemId: String): List<String> = buildList {
        val meals = cookingRecipes.filter { itemId in it.ingredients }.map { it.name }
        val gear = tinkeringRecipes.filter { itemId in ingredientsFor(it) || itemId in it.tools }.map { it.name }
        if (meals.isNotEmpty()) add("Cooking: ${meals.take(3).joinToString()}${if (meals.size > 3) " and more" else ""}")
        if (gear.isNotEmpty()) add("Tinkering: ${gear.take(3).joinToString()}${if (gear.size > 3) " and more" else ""}")
        cookingRecipes.filter { recipe ->
            itemId in recipe.ingredients && isRecipeDiscovered(recipe) &&
                !(recipe.category == "snack" && inventoryService.hasItem(recipe.result))
        }.mapNotNull { recipe ->
            val remaining = (recipe.ingredients.getValue(itemId) - availableForCraft(itemId)).coerceAtLeast(0)
            when {
                remaining > 0 -> "${recipe.name}: need $remaining more ${inventoryService.itemDisplayName(itemId)}."
                canCook(recipe) -> "${recipe.name}: ingredients ready. Prepare at a cooking station."
                else -> null
            }
        }.take(2).forEach { add(it) }
    }

    fun sharedIngredientNotes(recipe: CookingRecipe): List<String> = recipe.ingredients.keys.mapNotNull { id ->
        val names = tinkeringRecipes.filter { isSchematicLearned(it.id) && id in ingredientsFor(it) }.map { it.name }
        if (names.isEmpty()) null else "${inventoryService.itemDisplayName(id)} is also used in ${names.joinToString()}."
    }

    fun canCook(recipe: CookingRecipe, batch: Int = 1): Boolean {
        if (batch !in 1..5 || recipe.resultQuantity <= 0 || recipe.ingredients.isEmpty() || recipe.ingredients.values.any { it <= 0 }) return false
        if (!isRecipeDiscovered(recipe)) return false
        val item = inventoryService.catalogItem(recipe.result) ?: return false
        val snack = recipe.category == "snack"
        if (snack && (batch != 1 || inventoryService.hasItem(item.id))) return false
        val maxYield = recipe.resultQuantity.toLong() * batch + if (snack) 0 else batch
        if (maxYield + (inventoryService.snapshot()[item.id] ?: 0) > Int.MAX_VALUE) return false
        return recipe.ingredients.all { (id, count) -> availableForCraft(id).toLong() >= count.toLong() * batch }
    }

    fun cookMeal(recipeId: String, chefId: String? = null, batch: Int = 1): CraftingOutcome {
        val recipe = cookingRecipes.find { it.id == recipeId } ?: return CraftingOutcome.Failure("Unknown recipe")
        if (batch <= 0) return CraftingOutcome.Failure("Invalid batch size")
        if (chefId != null && chefId !in availableChefs()) return CraftingOutcome.Failure("That companion is not available.")
        if (!isRecipeDiscovered(recipe)) return CraftingOutcome.Failure(recipe.discoveryHint ?: "Discover this recipe at its cooking station.")
        if (!canCook(recipe, batch)) return CraftingOutcome.Failure("Missing ingredients, unavailable capacity, or snack already owned.")
        val scaledIngredients = recipe.ingredients.mapValues { it.value * batch }
        if (!inventoryService.consumeItems(scaledIngredients)) return CraftingOutcome.Failure("Unable to consume ingredients")
        val snack = recipe.category == "snack"
        if (!snack && chefId != null) sessionStore.selectMealChef(chefId)
        // One independent bonus-portion roll per recipe batch unit.
        val extra = if (snack) 0 else (1..batch).count { Math.random() < 0.15 }
        val total = recipe.resultQuantity * batch + extra
        inventoryService.addItem(recipe.result, total)
        sessionStore.setInventory(inventoryService.snapshot())
        val bonus = if (extra > 0) " Masterwork: +$extra extra portion${if (extra == 1) "" else "s"}." else ""
        val instruction = if (snack) " Equip it in a snack slot to use its cooldown ability." else " Eat from inventory to activate Well-Fed."
        return CraftingOutcome.Success(recipe.result, "${recipe.successMessage ?: "Prepared ${recipe.name}"} (x$total)$bonus$instruction")
    }

    fun canCraft(recipe: TinkeringRecipe): Boolean {
        if (recipe.requiresSchematic && !isSchematicLearned(recipe.id)) return false
        val result = inventoryService.catalogItem(recipe.result) ?: return false
        if (recipe.resultQuantity <= 0 ||
            (inventoryService.snapshot()[result.id] ?: 0).toLong() + recipe.resultQuantity > Int.MAX_VALUE) return false
        val requirements = ingredientsFor(recipe)
        if (requirements.isEmpty()) return false
        val inventoryCounts = inventoryTokenCounts()
        val hasIngredients = requirements.all { (id, needed) -> availableForCraft(id) >= needed }
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
        if (recipe.requiresSchematic && !isSchematicLearned(recipe.id)) {
            return CraftingOutcome.Failure("Learn the ${recipe.name} schematic from Inventory first.")
        }
        if (!canCraft(recipe)) return CraftingOutcome.Failure("Missing components or tools")
        val requirements = ingredientsFor(recipe)
        if (!inventoryService.consumeItems(requirements)) return CraftingOutcome.Failure("Unable to consume components")
        val addedId = addCraftedItem(recipe)
        // Keep session inventory in sync for downstream screens (inventory, save).
        sessionStore.setInventory(inventoryService.snapshot())
        recipe.successMessage?.let { return CraftingOutcome.Success(addedId, it) }
        return CraftingOutcome.Success(addedId, "Crafted ${recipe.name}")
    }

    fun salvageFor(itemId: String): Map<String, Int> {
        val item = inventoryService.catalogItem(itemId) ?: return emptyMap()
        val session = sessionStore.state.value
        if (item.unsellable || item.type.equals("quest", true) ||
            item.id in session.equippedItems.values || item.id in session.equippedWeapons.values ||
            item.id in session.equippedArmors.values) return emptyMap()
        val recipe = tinkeringRecipes.firstOrNull { it.result == item.id } ?: return emptyMap()
        val salvage = recipe.salvage.filterValues { it > 0 }
        if (salvage.keys.any { inventoryService.catalogItem(it) == null }) return emptyMap()
        return salvage
    }

    fun availableForCraft(itemId: String): Int {
        val item = inventoryService.catalogItem(itemId) ?: return 0
        val session = sessionStore.state.value
        val equipped = session.equippedItems.values + session.equippedWeapons.values + session.equippedArmors.values
        return ((inventoryService.snapshot()[item.id] ?: 0) - equipped.count { it == item.id }).coerceAtLeast(0)
    }

    fun resultSummary(recipe: TinkeringRecipe): String {
        val item = inventoryService.catalogItem(recipe.result) ?: return "Unknown result"
        val stats = item.equipment?.statMods.orEmpty().entries.joinToString(" Â· ") { (stat, value) ->
            "${stat.replace('_', ' ').replaceFirstChar { it.uppercase() }} ${if (value >= 0) "+" else ""}$value"
        }
        val element = item.equipment?.attackElement?.let { "Attack element: $it" }
        return listOfNotNull(recipe.description?.takeIf { recipe.category == "repair" },
            item.description, stats.takeIf { it.isNotBlank() }, element,
            if (item.equipment?.slot == "mod") "Equip in a party member's mod slot." else null,
            "Produces ${recipe.resultQuantity} item${if (recipe.resultQuantity == 1) "" else "s"}.")
            .joinToString("\n")
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
        // canCraft validates the catalog result and capacity before materials are consumed.
        val result = checkNotNull(inventoryService.catalogItem(recipe.result))
        inventoryService.addItem(result.id, recipe.resultQuantity)
        return result.id
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
