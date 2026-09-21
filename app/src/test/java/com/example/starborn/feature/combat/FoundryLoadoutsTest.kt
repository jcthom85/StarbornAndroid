package com.example.starborn.feature.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Item
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class FoundryLoadoutsTest {
    @Test fun apBracketSpendsOnlyAdministratorRewardOnLegalRootNodes() {
        val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
        val base = FoundryLoadouts.session("weak", assets)
        val upgraded = FoundryLoadouts.session("weak_ap", assets)
        val added = upgraded.unlockedSkills - base.unlockedSkills
        var budget = assets.loadEnemies().single { it.id == "administrator_boss" }.apReward!!
        assertEquals(2, budget)
        var unlocked = base.unlockedSkills
        for (id in added) {
            val tree = assets.loadSkillTrees().single { tree -> tree.branches.values.flatten().any { it.id == id } }
            val node = tree.branches.values.flatten().single { it.id == id }
            assertTrue(id, com.example.starborn.feature.exploration.skilltree.evaluateNodeStatus(
                node, tree, unlocked, base.completedMilestones, budget).canPurchase)
            budget -= node.costAp
            unlocked = unlocked + id
        }
        assertEquals(2, added.size)
        assertEquals(upgraded.playerAp, budget)
        assertEquals(base.inventory, upgraded.inventory)
    }

    @Test fun purchasedFixtureUsesCompatibleAvailableGearAndEarnedSkills() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val assets = WorldAssetDataSource(reader)
        val state = FoundryLoadouts.session("purchased", assets)
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        val root = sequenceOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }
        val shops = JSONObject(File(root, "shops.json").readText())
        val rawItems = org.json.JSONArray(File(root, "items.json").readText())
        val prices = (0 until rawItems.length()).map { rawItems.getJSONObject(it) }.associate { it.getString("id") to it.optInt("value") }
        var cost = 0
        for ((shopId, equipment) in listOf("weapon_shop" to state.equippedWeapons, "armor_shop" to state.equippedArmors)) {
            val shop = shops.getJSONObject(shopId)
            val sells = shop.getJSONObject("sells")
            val offered = sells.getJSONArray("items").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
            equipment.forEach { (owner, id) ->
                val item = items.getValue(id)
                assertTrue(id, id in offered)
                val gates = sells.getJSONObject("gates").optJSONObject(id)?.optJSONArray("milestones")
                if (gates != null) for (i in 0 until gates.length()) assertTrue(id, gates.getString(i) in state.completedMilestones)
                assertTrue(id, GearRules.matchesSlot(item.equipment, if(shopId == "weapon_shop") "weapon" else "armor", owner, item.type))
                cost += (prices.getValue(id) * shop.getJSONObject("pricing").getDouble("sell_markup")).roundToInt()
            }
        }
        assertEquals(2328, cost)
        val earned = assets.loadProgressionData()!!.levelUpSkills.values.flatMap { it.filterKeys { level -> level.toInt() <= 9 }.values }.toSet()
        assertEquals(8, earned.size)
        assertTrue(state.unlockedSkills.containsAll(earned))
        assertTrue(assets.loadSkills().map { it.id }.containsAll(earned))
        assertEquals(FoundryLoadouts.party.toSet(), state.equippedWeapons.keys)
        assertEquals(mapOf("medkit" to 3), state.inventory)
    }
}
