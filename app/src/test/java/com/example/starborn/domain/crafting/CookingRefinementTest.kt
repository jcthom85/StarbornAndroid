package com.example.starborn.domain.crafting

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CookingRefinementTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    private val items = reader.readList<Item>("items.json").associateBy { it.id }
    private val recipes = reader.readList<CookingRecipe>("recipes_cooking.json")
    private val tinkering = reader.readList<TinkeringRecipe>("recipes_tinkering.json")
    private val store = GameSessionStore().apply { setPlayer("nova"); setPartyMembers(listOf("nova")) }
    private val inventory = InventoryService(object : ItemCatalog {
        override fun load() {}
        override fun findItem(idOrAlias: String) = items[idOrAlias]
    }).apply { loadItems() }
    private val crafting = CraftingService(object : CraftingRecipeSource {
        override fun loadCookingRecipes() = recipes
        override fun loadTinkeringRecipes() = tinkering
    }, inventory, store)
    private val characters = assets.loadCharacters().associateBy { it.id }
    private val use = ItemUseController(inventory, crafting, store,
        skillNodesProvider = { assets.loadSkillNodes() }, charactersProvider = { characters })

    @Test fun replacementNeedsConfirmationAndDoesNotSpendFoodOnCancel() = runTest {
        inventory.addItem("glowfish_broth", 2)
        assertTrue(use.useItem("glowfish_broth") is ItemUseController.Result.Success)
        val active = store.state.value.activeMealBuff
        assertTrue(use.useItem("glowfish_broth") is ItemUseController.Result.Failure)
        assertEquals(1, inventory.snapshot()["glowfish_broth"])
        assertEquals(active, store.state.value.activeMealBuff)
        assertTrue(use.useItem("glowfish_broth", replaceMeal = true) is ItemUseController.Result.Success)
        assertFalse(inventory.hasItem("glowfish_broth"))
        assertEquals(3, store.state.value.activeMealBuff!!.remainingEncounters)
    }

    @Test fun snacksAndStimulantsCannotConsumeOrOverwriteMealsFromInventory() = runTest {
        inventory.addItem("glowfish_broth")
        use.useItem("glowfish_broth")
        val active = store.state.value.activeMealBuff
        listOf("mineral_trail_mix", "adrenaline").forEach { id ->
            inventory.addItem(id)
            assertTrue(use.useItem(id) is ItemUseController.Result.Failure)
            assertEquals(1, inventory.snapshot()[id])
            assertEquals(active, store.state.value.activeMealBuff)
        }
        assertNull(inventory.useItem("mineral_trail_mix"))
    }

    @Test fun percentageBonusesAndServingPerksUseCombatUnits() {
        val orion = MealRules.buff(items.getValue("celestial_fillet"), "orion")
        assertEquals(12, orion.focusBonus)
        assertEquals(12.0, orion.critBonus, 0.0)
        assertEquals(5, MealRules.buff(items.getValue("glowfish_broth"), "nova").accuracyBonus)
        assertEquals(3, MealRules.buff(items.getValue("ration_pack"), "nova").focusBonus)
        assertEquals(10, MealRules.buff(items.getValue("ration_pack"), "gh0st").statusResistBonus)
    }

    @Test fun healingHonorsEquipmentCeilingAndNeverLowersHp() = runTest {
        store.setEquippedArmor("nova", "nova_helioguard_plate")
        val ceiling = PartyHealth.maxHp(characters.getValue("nova"), store.state.value, items::get, assets.loadSkillNodes())
        store.setPartyMemberHp("nova", ceiling - 5)
        inventory.addItem("medkit_i", 2)
        assertTrue(use.useItem("medkit_i", "nova") is ItemUseController.Result.Success)
        assertEquals(ceiling, store.state.value.partyMemberHp["nova"])
        store.setPartyMemberHp("nova", ceiling + 20)
        use.useItem("medkit_i", "nova")
        assertEquals(ceiling + 20, store.state.value.partyMemberHp["nova"])
    }

    @Test fun regionalDiscoveryPersistsAndSnackCraftingDoesNotDuplicateOwnedAbility() {
        val recipe = recipes.single { it.id == "snack_mineral_trail_mix" }
        inventory.restore(recipe.ingredients)
        assertFalse(crafting.canCook(recipe))
        store.setRoomState(recipe.discoveryRoom!!, "cooking_discovered", true)
        assertTrue(crafting.canCook(recipe))
        assertTrue(crafting.cookMeal(recipe.id) is CraftingOutcome.Success)
        assertEquals(1, inventory.snapshot()[recipe.result])
        inventory.restore(inventory.snapshot() + recipe.ingredients)
        val before = inventory.snapshot()
        assertTrue(crafting.cookMeal(recipe.id) is CraftingOutcome.Failure)
        assertEquals(before, inventory.snapshot())
        assertFalse(crafting.canCook(recipe, 2))
    }

    @Test fun spicedRationsHaveCorrectBaseYieldAndBatchCosts() {
        val recipe = recipes.single { it.id == "provision_spiced_ration_cache" }
        inventory.restore(recipe.ingredients.mapValues { it.value * 3 })
        assertEquals(2, recipe.resultQuantity)
        assertTrue(crafting.cookMeal(recipe.id, batch = 3) is CraftingOutcome.Success)
        assertTrue(inventory.snapshot().getValue("ration_pack") in 6..9)
        recipe.ingredients.keys.forEach { assertFalse(inventory.hasItem(it)) }
    }

    private fun actor(id: String, side: CombatSide, resistance: Int = 0) = Combatant(
        id, id, side, StatBlock(100, 10, 5, 5, 5, 5, 5, statusResistance = resistance))

    @Test fun combatMealAppliesHealingAndBuffWithoutWellFed() {
        val engine = CombatEngine()
        val base = engine.beginEncounter(CombatSetup(listOf(actor("nova", CombatSide.PLAYER)), listOf(actor("enemy", CombatSide.ENEMY))))
        val wounded = base.copy(combatants = base.combatants + ("nova" to base.combatants.getValue("nova").copy(hp = 10)))
        inventory.addItem("glowfish_broth")
        val processor = CombatActionProcessor(engine = engine, statusRegistry = StatusRegistry(), skillLookup = { null }, consumeItem = inventory::useItem)
        val result = processor.execute(wounded, CombatAction.ItemUse("nova", "glowfish_broth", "nova")) { CombatReward() }
        assertEquals(90, result.combatants.getValue("nova").hp)
        assertTrue(result.combatants.getValue("nova").buffs.any { it.effect.stat == "accuracy" && it.effect.value == 5 })
        assertNull(store.state.value.activeMealBuff)
        assertFalse(inventory.hasItem("glowfish_broth"))
    }

    @Test fun statusResistanceProtectsAgainstHostileEffectsButNotFriendlyEffects() {
        val engine = CombatEngine()
        val base = engine.beginEncounter(CombatSetup(listOf(actor("nova", CombatSide.PLAYER, 100)), listOf(actor("enemy", CombatSide.ENEMY))))
        assertTrue(engine.applyStatus(base, "nova", "stun", 2, sourceId = "enemy").combatants.getValue("nova").statusEffects.isEmpty())
        assertTrue(engine.applyStatus(base, "nova", "guard", 2, sourceId = "nova").combatants.getValue("nova").statusEffects.any { it.id == "guard" })
    }

    @Test fun salvageProtectsQuestToolsAndEquippedModsAndUsesExplicitReturns() {
        assertTrue(crafting.salvageFor("functional_cryo_inductor").isEmpty())
        assertTrue(crafting.salvageFor("thermal_cutter").isEmpty())
        assertEquals(mapOf("scrap_metal" to 1), crafting.salvageFor("salvage_lure"))
        store.setEquippedItem("weapon_mod1", "power_lens_mk_i", "nova")
        inventory.addItem("power_lens_mk_i")
        assertTrue(crafting.salvageFor("power_lens_mk_i").isEmpty())
        assertEquals(0, crafting.availableForCraft("power_lens_mk_i"))
    }
}
