package com.example.starborn.domain.inventory

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.*
import org.junit.Test

class ModEquipmentRulesTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val items = reader.readList<Item>("items.json").associateBy { it.id }
    private val store = GameSessionStore().apply {
        setPlayer("nova"); setPartyMembers(listOf("nova", "zeke", "orion"))
        listOf("ms_w1_mq01_complete", "ms_w1_mq02_complete", "ms_w1_mq03_complete", "ms_w1_mq05_complete").forEach(::setMilestone)
        setEquippedWeapon("nova", "nova_laser_blaster")
        setEquippedWeapon("zeke", "zeke_shock_fists")
        setEquippedWeapon("orion", "orion_prism_focus")
        setInventory(mapOf("power_lens_mk_i" to 2))
    }
    private val lens = items.getValue("power_lens_mk_i")

    @Test fun copiesEquipIndependentlyAndTransferRequiresAnExplicitSource() {
        val nova = ModLocation("nova", "weapon_mod1")
        val zeke = ModLocation("zeke", "weapon_mod1")
        val orion = ModLocation("orion", "weapon_mod1")
        assertEquals(ModEquipResult.Success, store.tryEquipMod(nova, lens))
        assertEquals(ModEquipResult.Success, store.tryEquipMod(zeke, lens))
        val before = store.state.value
        val request = store.tryEquipMod(orion, lens) as ModEquipResult.TransferRequired
        assertEquals(setOf(nova, zeke), request.sources.toSet())
        assertEquals(before, store.state.value)
        assertEquals(ModEquipResult.Success, store.tryEquipMod(orion, lens, zeke))
        assertEquals(lens.id, store.state.value.equippedItems[nova.key])
        assertEquals(lens.id, store.state.value.equippedItems[orion.key])
        assertFalse(store.state.value.equippedItems.containsKey(zeke.key))
        assertEquals(2, store.state.value.inventory[lens.id])
    }

    @Test fun sameCharacterMovesRatherThanStacksAndStaleTransfersDoNotMutate() {
        val source = ModLocation("nova", "weapon_mod1")
        val target = ModLocation("nova", "weapon_mod2")
        store.tryEquipMod(source, lens)
        assertTrue(store.tryEquipMod(target, lens) is ModEquipResult.TransferRequired)
        assertEquals(ModEquipResult.Success, store.tryEquipMod(target, lens, source))
        assertEquals(1, ModEquipmentRules.locations(store.state.value, lens.id).size)
        val before = store.state.value
        assertTrue(store.tryEquipMod(ModLocation("zeke", "weapon_mod1"), lens, source) is ModEquipResult.Rejected)
        assertEquals(before, store.state.value)
    }

    @Test fun invalidLockedAndParentlessDestinationsRejectWithoutMutation() {
        val before = store.state.value
        assertTrue(store.tryEquipMod(ModLocation("nova", "mod"), lens) is ModEquipResult.Rejected)
        assertTrue(store.tryEquipMod(ModLocation("nova", "armor_mod1"), lens) is ModEquipResult.Rejected)
        assertEquals(before, store.state.value)
        store.restore(before.copy(completedMilestones = emptySet()))
        assertTrue(store.tryEquipMod(ModLocation("nova", "weapon_mod1"), lens) is ModEquipResult.Rejected)
    }

    @Test fun restorePreservesQuantitiesAndPrefersExplicitBindingsOverLegacyClones() {
        val raw = store.state.value.copy(inventory = mapOf(lens.id to 1),
            equippedItems = mapOf("weapon_mod1" to lens.id, "nova:weapon_mod2" to lens.id))
        store.restore(raw)
        assertEquals(mapOf("nova:weapon_mod2" to lens.id), store.state.value.equippedItems)
        assertEquals(raw.inventory, store.state.value.inventory)
        assertNotNull(store.takeEquipmentRestoreNotice())
        store.restore(store.state.value)
        assertNull(store.takeEquipmentRestoreNotice())
    }

    @Test fun spareCopiesCanBeCraftedOrScrappedWithoutTouchingAssignedCopies() {
        val inventory = InventoryService(object : ItemCatalog {
            override fun load() {}
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        }).apply { loadItems(); addItem(lens.id, 2) }
        val crafting = CraftingService(object : CraftingRecipeSource {
            override fun loadCookingRecipes() = reader.readList<CookingRecipe>("recipes_cooking.json")
            override fun loadTinkeringRecipes() = reader.readList<TinkeringRecipe>("recipes_tinkering.json")
        }, inventory, store)
        store.tryEquipMod(ModLocation("nova", "weapon_mod1"), lens)
        assertEquals(1, crafting.availableForCraft(lens.id))
        assertTrue(crafting.salvageFor(lens.id).isNotEmpty())
        inventory.removeItem(lens.id, 1)
        store.setInventory(inventory.snapshot())
        assertEquals(0, crafting.availableForCraft(lens.id))
        assertTrue(crafting.salvageFor(lens.id).isEmpty())
        assertEquals(lens.id, store.state.value.equippedItems["nova:weapon_mod1"])
    }

    @Test fun weaponElementOverridesModsAndFirstWeaponModOverridesSecond() {
        assertEquals("burn", ModEquipmentRules.resolveAttackElement("burn", listOf("source", "shock")))
        assertEquals("source", ModEquipmentRules.resolveAttackElement(null, listOf("source", "shock")))
        assertEquals("shock", ModEquipmentRules.resolveAttackElement(null, listOf(null, "shock")))
    }
}
