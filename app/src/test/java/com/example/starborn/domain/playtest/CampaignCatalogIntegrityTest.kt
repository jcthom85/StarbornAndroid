package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.*
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test
import java.io.File

// Catalog validation only; this does not execute gameplay or prove reachability.
class CampaignCatalogIntegrityTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance

    @Test
    fun `reviewed world two interactions are named in room prose`() {
        val rooms = readList<Room>("rooms.json").associateBy { it.id }
        // A room may have several reviewed actions; a map would discard duplicates.
        val expected = listOf(
            "sector9_conduit_junction" to "service map",
            "sector9_archive_vault" to "Aethel extinction log",
            "sector9_archive_reading_room" to "research terminal",
            "sector9_beach_cliffs" to "basalt cliffs",
            "sector9_beach_cargo" to "rusted hull",
            "sector9_beach_cave" to "mineral shelter",
            "sector9_conduit_crawlspace" to "maintenance spores",
            "sector9_conduit_access_shaft" to "shaft shortcut",
            "sector9_beach_cove" to "warm sand",
            "sector9_beach_pillar" to "resonant pillar",
            "sector9_beach_dunes" to "wind-sculpted ridges",
            "sector9_stream_wetlands" to "spore warning",
            "sector9_stream_wetlands" to "wind-sung reeds",
            "sector9_stream_flats" to "half-sunken causeway",
            "sector9_stream_cave_entrance" to "cave glyphs",
            "sector9_stream_cave_entrance" to "waterfall curtain",
            "sector9_stream_sunken_passage" to "drowned console",
            "sector9_stream_sunken_passage" to "warm current",
            "sector9_wilds_overlook" to "canopy route",
            "sector9_wilds_overlook" to "canopy vista",
            "sector9_wilds_canopy_walk" to "razor-vine shortcut",
            "sector9_wilds_canopy_walk" to "aerial root bridges",
            "sector9_wilds_archway" to "Aethel boundary stones",
            "sector9_wilds_dense_brush" to "toxic bloom",
            "sector9_ridge_climb" to "medic satchel",
            "sector9_ridge_ledges" to "blue-frond cluster",
            "sector9_ridge_overhang" to "weathered mural",
            "sector9_ridge_viewpoint" to "facility sightline",
            "sector9_ridge_crevice" to "razor-vine cache",
            "sector9_ridge_crevice" to "fault line",
            "sector9_landing_glade" to "broadleaf canopy",
            "sector9_stream_tidepools" to "star crystals",
            "sector9_stream_cave_depths" to "glowing moss",
            "sector9_stream_cave_depths" to "waterproof cargo case",
            "sector9_wilds_hollow" to "ancient stump",
            "sector9_wilds_clearance" to "shattered trunks",
            "sector9_ridge_plateau" to "ridge overlook cache",
            "sector9_beach_pools" to "gather tideglass",
            "sector9_beach_pools" to "luminescent anemones",
            "sector9_beach_grotto" to "cache",
            "sector9_beach_grotto" to "sea-worn arches",
            "sector9_beach_grotto" to "bioluminescent prism",
            "sector9_temple_plaza" to "processional mural",
            "sector9_temple_plaza" to "flagstone courtyard",
            "sector9_temple_vestibule" to "sanitation console",
            "sector9_foyer_grand_hall" to "welcome console",
            "sector9_foyer_left_gallery" to "botanical research log",
            "sector9_foyer_right_gallery" to "light-mural procession",
            "sector9_foyer_security_hub" to "terminal",
            "sector9_foyer_vault_door" to "security palimpsest",
            "sector9_hall_echo_alcoves" to "choir alcove log",
            "sector9_hall_reflection_pool" to "reflection pool",
            "sector9_power_turbine" to "turbine inscription",
            "sector9_power_turbine" to "dynamo housing",
            "sector9_power_breaker_deck" to "generator breakers",
            "sector9_power_breaker_deck" to "bus bar array",
            "sector9_hangar_scaffolding" to "hangar overlook",
            "sector9_crash_site" to "wreckage",
            "sector9_crash_site" to "glow-water",
            "sector9_crash_site" to "scan moss",
            "sector9_canopy" to "Scan spores",
            "sector9_canopy" to "Scavenge remains",
            "sector9_canopy_ridge" to "Look at Shield",
            "sector9_stasis_chamber" to "stasis pod",
            "sector9_stasis_chamber" to "root lattice",
            "sector9_stasis_chamber" to "ceiling vent"
        )
        expected.forEach { (roomId, actionName) ->
            val room = requireNotNull(rooms[roomId])
            assertTrue("$roomId must retain action $actionName", room.actions.any { it["name"] == actionName })
            assertTrue("$roomId must expose $actionName in its description",
                room.description.orEmpty().contains(actionName, ignoreCase = true))
        }
    }

    @Test
    fun `reviewed world two persistent interactions remain named after local objectives`() {
        val rooms = readList<Room>("rooms.json").associateBy { it.id }
        val expected = listOf(
            Triple("sector9_landing_glade", "ms_w2_herb_glade_foraged", listOf("broadleaf canopy")),
            Triple("sector9_stream_tidepools", "ms_w2_meat_tidepool_foraged", listOf("star crystals")),
            Triple("sector9_stream_cave_depths", "ms_w2_meat_cave_foraged", listOf("glowing moss", "waterproof cargo case")),
            Triple("sector9_wilds_hollow", "ms_w2_beacon_beta_traced", listOf("ancient stump")),
            Triple("sector9_wilds_clearance", "ms_w2_transceiver_recovered", listOf("shattered trunks")),
            Triple("sector9_beach_pools", "ms_w2_flora_weeds_scanned", listOf("gather tideglass", "luminescent anemones")),
            Triple("sector9_beach_grotto", "ms_w2_grotto_relic_gathered", listOf("sea-worn arches", "bioluminescent prism")),
            Triple("sector9_power_breaker_deck", "ms_w2_source_breakers_starved", listOf("bus bar array")),
            Triple("sector9_power_breaker_deck", "ms_w2_breakers_overloaded", listOf("bus bar array")),
            Triple("sector9_crash_site", "ms_w2_mq01_complete", listOf("wreckage", "glow-water", "scan moss")),
            Triple("sector9_crash_site", "ms_w2_flora_moss_scanned", listOf("wreckage", "glow-water")),
            Triple("sector9_crash_site", "ms_w2_pod_examined", listOf("wreckage", "glow-water", "scan moss")),
            Triple("sector9_canopy", "ms_w2_flora_ferns_scanned", listOf("Scan spores", "Scavenge remains")),
            Triple("sector9_stasis_chamber", "ms_w2_mq03_complete", listOf("root lattice", "ceiling vent")),
            Triple("sector9_stasis_chamber", "ms_w2_roots_scanned", listOf("stasis pod", "ceiling vent")),
            Triple("sector9_stasis_chamber", "ms_w2_pod_inspected", listOf("root lattice", "ceiling vent")),
            Triple("sector9_pod_interior", "ms_w2_zeke_stabilized", listOf("locker")),
            Triple("sector9_landing_brush", "ms_w2_herb_landing_foraged", listOf("muddy clearing")),
            Triple("sector9_landing_drop", "ms_w2_conduits_collected", listOf("cargo panel")),
            Triple("sector9_stream_pools", "ms_w2_herb_reeds_foraged", listOf("still basins")),
            Triple("sector9_hall_of_echoes", "ms_w2_murals_inspected", listOf("central relief")),
            Triple("sector9_stasis_observation", "ms_w2_stasis_overview_read", listOf("stasis mural overview")),
            Triple("sector9_hangar_bay", "ms_w2_astra_inspected", listOf("bridge relic", "launch cradle"))
        )
        expected.forEach { (roomId, milestone, names) ->
            val room = rooms.getValue(roomId)
            val completed = room.descriptionVariants.single { milestone in it.requiresMilestones }
            names.forEach { name ->
                assertTrue("$roomId must retain $name after $milestone",
                    completed.description.contains(name, ignoreCase = true))
                val action = room.actions.single { it["name"] == name }
                assertNotEquals("$name must not retire with the unrelated objective",
                    milestone, action["requires_milestone_not_set"])
            }
        }
    }

    @Test
    fun `ridge keeps shield inspection across stages without reopening retired objectives`() {
        val ridge = readList<Room>("rooms.json").single { it.id == "sector9_canopy_ridge" }
        (listOf(ridge.description) + ridge.descriptionVariants.map { it.description }).forEach {
            assertTrue(it.contains("Look at Shield", ignoreCase = true))
        }
        val confront = ridge.actions.single { it["name"] == "Confront stalker" }
        val beast = ridge.actions.single { it["name"] == "Face the Beast" }
        val drill = ridge.actions.single { it["name"] == "Anchor Drill" }
        assertEquals("hunter_confronted", confront["hide_when_state"])
        assertEquals("hunter_confronted", beast["show_when_state"])
        assertEquals("beast_defeated", beast["hide_when_state"])
        assertEquals("beast_defeated", drill["show_when_state"])
        assertEquals("anchor_drill_complete", drill["hide_when_state"])
        assertEquals("ms_w2_mq04_complete", drill["requires_milestone_not_set"])
        val confronted = ridge.descriptionVariants.single { it.requiresState["hunter_confronted"] == true }
        assertTrue(confronted.description.contains("Face the Beast"))
        assertFalse(confronted.description.contains("Confront stalker"))
        val defeated = ridge.descriptionVariants.single {
            it.requiresState["beast_defeated"] == true && it.forbiddenState.isEmpty()
        }
        assertTrue(defeated.description.contains("Anchor Drill"))
        assertFalse(defeated.description.contains("Face the Beast"))
    }

    @Test
    fun `dunes retain ridge discovery after harvesting without advertising the retired grazer`() {
        val dunes = readList<Room>("rooms.json").single { it.id == "sector9_beach_dunes" }
        val harvestMilestone = "ms_w2_meat_dunes_foraged"
        val harvested = dunes.descriptionVariants.single { harvestMilestone in it.requiresMilestones }
        val grazer = dunes.actions.single { it["name"] == "burrowing grazer" }
        val ridges = dunes.actions.single { it["name"] == "wind-sculpted ridges" }

        assertEquals(harvestMilestone, grazer["requires_milestone_not_set"])
        assertTrue(dunes.description.contains("burrowing grazer", ignoreCase = true))
        assertFalse(harvested.description.contains("burrowing grazer", ignoreCase = true))
        assertTrue(harvested.description.contains("wind-sculpted ridges", ignoreCase = true))
        assertNull(ridges["requires_milestone_not_set"])
    }

    @Test
    fun `main quest catalog contains thirty quests with valid encounter references`() {
        val quests = readList<Quest>("quests.json")
        val events = readList<GameEvent>("events.json")
        val rooms = readList<Room>("rooms.json")
        val dialogue = readList<DialogueLine>("dialogue.json")
        val enemies = readList<Enemy>("enemies.json")
        val skills = readList<Skill>("skills.json")

        val roomById = rooms.associateBy { it.id }
        val dialogueById = dialogue.associateBy { it.id }
        val enemyById = enemies.associateBy { it.id }
        val skillById = skills.associateBy { it.id }

        // 1. Verify 30 Main Quests exist (5 per world across 6 worlds)
        val mainQuests = quests.filter { it.id.contains("_mq") }
        assertEquals("Campaign must contain exactly 30 main quests across 6 worlds", 30, mainQuests.size)

        for (world in 1..6) {
            val worldMqs = mainQuests.filter { it.id.startsWith("w${world}_mq") }
            assertEquals("World $world must have 5 sequential main quests", 5, worldMqs.size)
        }

        // 2. Walk each main quest and verify complete task-event-room chain
        val missingRooms = mutableListOf<String>()
        val missingEnemies = mutableListOf<String>()

        mainQuests.forEach { quest ->
            quest.stages.forEach { stage ->
                stage.tasks.forEach { task ->
                    // Find all events associated with this quest and stage
                    val matchingEvents = events.filter { event ->
                        event.conditions.any { it.questId == quest.id && (it.stageId == null || it.stageId == stage.id) }
                    }

                    matchingEvents.forEach { event ->
                        if (event.trigger.type == "action" || event.trigger.type == "inspect") {
                            val roomId = event.trigger.room ?: event.trigger.roomId
                            if (roomId != null && !roomById.containsKey(roomId)) {
                                missingRooms.add("Quest ${quest.id}: Room $roomId does not exist")
                            }
                        }

                        // Check combat encounters
                        if (event.trigger.type == "encounter_victory") {
                            val enemyIds = event.trigger.enemies.orEmpty()
                            enemyIds.forEach { enemyId ->
                                if (!enemyById.containsKey(enemyId)) {
                                    missingEnemies.add("Quest ${quest.id}: Enemy $enemyId does not exist")
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue("All main quest rooms must resolve: $missingRooms", missingRooms.isEmpty())
        assertTrue("All main quest enemies must resolve: $missingEnemies", missingEnemies.isEmpty())
    }

    @Test
    fun `verify enemy skills and combat readiness across all campaign encounters`() {
        val enemies = readList<Enemy>("enemies.json")
        val skills = readList<Skill>("skills.json")
        val skillById = skills.associateBy { it.id }

        val missingSkills = mutableListOf<String>()
        enemies.forEach { enemy ->
            enemy.abilities.forEach { skillId ->
                if (!skillById.containsKey(skillId)) {
                    missingSkills.add("Enemy ${enemy.id} missing skill $skillId")
                }
            }
        }

        assertTrue("All enemy skills must exist in skills.json: $missingSkills", missingSkills.isEmpty())
    }

    @Test
    fun `verify all key debug scenario rooms resolve cleanly`() {
        val rooms = readList<Room>("rooms.json")
        val roomById = rooms.associateBy { it.id }

        // Verify key scenario start rooms exist
        val keyScenarioRooms = listOf(
            "pit_nova_bunk", "sector9_stream_falls", "spire_prism_gallery",
            "orbital_executive_dock", "orbital_grand_concourse"
        )
        keyScenarioRooms.forEach { roomId ->
            assertTrue("Scenario room $roomId must exist in rooms.json", roomById.containsKey(roomId))
        }
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
