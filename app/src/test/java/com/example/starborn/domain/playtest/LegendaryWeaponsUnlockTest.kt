package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.*
import org.junit.Test

class LegendaryWeaponsUnlockTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    private val items = reader.readList<Item>("items.json").associateBy { it.id }
    private val recipes = reader.readList<TinkeringRecipe>("recipes_tinkering.json").associateBy { it.id }
    private val events = reader.readList<GameEvent>("events.json")
    private val enemies = assets.loadEnemies().associateBy { it.id }
    private val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }

    @Test fun allFourLegendaryWeaponsExistAreNotSoldAndMatchCrewSlots() {
        val expected = mapOf(
            "nova" to "nova_shift_breaker",
            "zeke" to "zeke_breaker_gauntlets",
            "gh0st" to "gh0st_elara_refrain",
            "orion" to "orion_aethel_chorus"
        )
        expected.forEach { (owner, id) ->
            val item = items.getValue(id)
            assertEquals("legendary", item.rarity)
            assertTrue("$id matches $owner weapon slot", GearRules.matchesSlot(item.equipment, "weapon", owner, item.type))
            assertEquals("single", item.equipment?.attackStyle)
            assertNull("$id has no charge delay", item.equipment?.attackChargeTurns)
        }
    }

    @Test fun novaAndZekeLegendaryForgePathsRequireBossCatalystsAndWorldSixEchoes() {
        assertTrue(enemies.getValue("crucible_champion").drops.any { it.id == "crucible_slag_heart" && it.chance >= 1.0 })
        assertTrue(enemies.getValue("hk_droid").drops.any { it.id == "hk_servo_anchor" && it.chance >= 1.0 })

        val novaRecipe = recipes.getValue("craft_nova_shift_breaker")
        assertEquals("nova_shift_breaker", novaRecipe.result)
        assertEquals(setOf("jeds_bench_regulator", "crucible_slag_heart", "power_cell"), novaRecipe.ingredients.keys)

        val zekeRecipe = recipes.getValue("craft_zeke_breaker_gauntlets")
        assertEquals("zeke_breaker_gauntlets", zekeRecipe.result)
        assertEquals(setOf("hk_servo_anchor", "revoked_collar_seal", "composite_plate"), zekeRecipe.ingredients.keys)

        val granted = mutableListOf<String>()
        val store = GameSessionStore().apply {
            setMilestone("ms_w6_jed_lesson_heard")
            setMilestone("ms_w6_hr_revoked")
        }
        val manager = EventManager(events, store, EventHooks(onGiveItem = { id, _ -> granted += id }))
        manager.handleTrigger("player_action", EventPayload.Action("w6_sq26_carry_legacy"))
        manager.handleTrigger("player_action", EventPayload.Action("w6_sq27_free_workers"))
        assertTrue(granted.containsAll(listOf("jeds_bench_regulator", "schematic_shift_breaker", "revoked_collar_seal", "schematic_unshackled_breakers")))

        // Leaving the HR review intact as evidence also awards Zeke's Legendary catalyst + schematic
        val leaveGranted = mutableListOf<String>()
        val leaveStore = GameSessionStore().apply { setMilestone("ms_w6_hr_revoked") }
        val leaveManager = EventManager(events, leaveStore, EventHooks(onGiveItem = { id, _ -> leaveGranted += id }))
        leaveManager.handleTrigger("player_action", EventPayload.Action("w6_sq27_leave_review"))
        assertTrue(leaveGranted.containsAll(listOf("revoked_collar_seal", "schematic_unshackled_breakers")))
    }

    @Test fun allLegendaryWeaponsSchematicsAndCatalystsHaveShippedItemIcons() {
        val assetProvider = DesktopAssetProvider()
        val legendaryItemIds = listOf(
            "crucible_slag_heart",
            "jeds_bench_regulator",
            "schematic_shift_breaker",
            "nova_shift_breaker",
            "hk_servo_anchor",
            "revoked_collar_seal",
            "schematic_unshackled_breakers",
            "zeke_breaker_gauntlets",
            "gh0st_elara_refrain",
            "orion_aethel_chorus"
        )
        legendaryItemIds.forEach { id ->
            val path = items.getValue(id).resolvedIconAssetPath()
            assertNotNull("Expected icon path for $id", path)
            assertTrue("Expected shipped WebP icon at $path for $id", assetProvider.exists(path!!))
        }
    }

    @Test fun ghostElaraRefrainRequiresBothWorldFiveAndWorldSixQuests() {
        assertTrue(rooms.getValue("source_gh0st_kill_suite").description.contains("silenced armory locker", ignoreCase = true))

        val granted = mutableListOf<String>()
        val store = GameSessionStore()
        val manager = EventManager(events, store, EventHooks(onGiveItem = { id, _ -> granted += id }))

        // Locked when neither or only one quest is done
        manager.handleTrigger("player_action", EventPayload.Action("w6_claim_elara_refrain"))
        assertTrue(granted.isEmpty())
        store.completeQuest("w6_sq28")
        manager.handleTrigger("player_action", EventPayload.Action("w6_claim_elara_refrain"))
        assertTrue(granted.isEmpty())

        // Unlocks once both W5 Ghost in the Shell and W6 Elara's Song are complete
        store.completeQuest("w5_sq24")
        manager.handleTrigger("player_action", EventPayload.Action("w6_claim_elara_refrain"))
        assertEquals(listOf("gh0st_elara_refrain"), granted)
        assertTrue("ms_w6_elara_refrain_claimed" in store.state.value.completedMilestones)

        // Does not duplicate
        manager.handleTrigger("player_action", EventPayload.Action("w6_claim_elara_refrain"))
        assertEquals(listOf("gh0st_elara_refrain"), granted)
    }

    @Test fun orionFirstChoirSuperbossSpawnsAfterAethelGraveAndDropsChorus() {
        assertTrue(rooms.getValue("source_orion_nightmare_node_scale_01").description.contains("sealed aethel reliquary", ignoreCase = true))
        val choir = enemies.getValue("aethel_first_choir")
        assertTrue(choir.drops.any { it.id == "orion_aethel_chorus" && it.chance >= 1.0 })

        val spawned = mutableListOf<String>()
        val store = GameSessionStore()
        val manager = EventManager(events, store, EventHooks(onSpawnEncounter = { enc, _ -> if (enc != null) spawned.add(enc) }))

        // Locked before Aethel Grave completion
        manager.handleTrigger("player_action", EventPayload.Action("w6_first_choir_reliquary"))
        assertTrue(spawned.isEmpty())

        // Spawns The First Choir once Orion carries the Aethel chorus
        store.setMilestone("ms_w6_ancestral_grace_unlocked")
        manager.handleTrigger("player_action", EventPayload.Action("w6_first_choir_reliquary"))
        assertEquals(listOf("aethel_first_choir"), spawned)

        // Victory marks milestone and stops re-spawning
        manager.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("aethel_first_choir"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY,
                roomId = "source_orion_nightmare_node_scale_01"
            )
        )
        assertTrue("ms_w6_first_choir_defeated" in store.state.value.completedMilestones)
        manager.handleTrigger("player_action", EventPayload.Action("w6_first_choir_reliquary"))
        assertEquals(listOf("aethel_first_choir"), spawned)
    }
}
