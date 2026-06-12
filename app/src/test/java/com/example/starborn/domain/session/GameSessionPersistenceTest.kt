package com.example.starborn.domain.session

import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import java.io.File
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameSessionPersistenceTest {

    private lateinit var baseDir: File
    private lateinit var persistence: GameSessionPersistence

    @Before
    fun setUp() {
        baseDir = File(
            System.getProperty("java.io.tmpdir"),
            "starborn-session-${System.nanoTime()}"
        ).apply { mkdirs() }
        clearStores()
        persistence = GameSessionPersistence(baseDir)
    }

    @After
    fun tearDown() {
        clearStores()
        baseDir.deleteRecursively()
    }

    @Test
    fun importLegacySave_capturesQuestAndMilestoneProgress() {
        val legacy = """
            {
              "game_state": {
                "party": ["nova"],
                "inventory": {},
                "credits": 120,
                "quests": {
                  "quests": [
                    {"id": "quest_active", "status": "active"},
                    {"id": "quest_done", "status": "completed"}
                  ]
                },
                "milestones": ["ms_intro_complete"],
                "learned_schematics": ["schematic_alpha"],
                "map": {
                  "current_world_id": "world_a",
                  "current_hub_id": "hub_a",
                  "current_room_id": "room_a"
                }
              },
              "characters": {
                "nova": {
                  "level": 3,
                  "xp": 180,
                  "hp": 420
                }
              }
            }
        """.trimIndent()
        val tempFile = File.createTempFile("legacy_test", ".json").apply { writeText(legacy) }
        val catalog = object : ItemCatalog {
            override fun load() {}
            override fun findItem(idOrAlias: String): Item? = null
        }

        val state = persistence.importLegacySave(tempFile, catalog)

        assertNotNull(state)
        state!!
        assertTrue("quest_active should be active", state.activeQuests.contains("quest_active"))
        assertTrue("quest_done should be completed", state.completedQuests.contains("quest_done"))
        assertTrue("ms_intro_complete should be migrated", state.completedMilestones.contains("ms_intro_complete"))
        assertTrue("schematic should transfer", state.learnedSchematics.contains("schematic_alpha"))
        assertEquals("world_a", state.worldId)
        assertEquals("hub_a", state.hubId)
        assertEquals("room_a", state.roomId)
        assertEquals(3, state.playerLevel)
    }

    @Test
    fun slotInfo_returnsTimestampForSavedSlot() = runBlocking {
        val state = GameSessionState(
            worldId = "world_a",
            hubId = "hub_a",
            roomId = "room_a",
            playerId = "nova",
            playerLevel = 4,
            playerXp = 240,
            playerCredits = 320
        )

        val before = System.currentTimeMillis()
        persistence.writeSlot(1, state)
        val info = persistence.slotInfo(1)
        val after = System.currentTimeMillis()

        assertNotNull(info)
        val savedAt = info!!.savedAtMillis
        assertNotNull("Expected timestamp to be recorded", savedAt)
        assertTrue(
            String.format(Locale.US, "Timestamp %d should be >= %d", savedAt, before),
            savedAt!! >= before
        )
        assertTrue(
            String.format(Locale.US, "Timestamp %d should be <= %d", savedAt, after + 1000),
            savedAt <= after + 1000
        )
        assertEquals("nova", info.state.playerId)
    }

    @Test
    fun questProgressPersistsAcrossSaves() = runBlocking {
        val state = GameSessionState(
            questStageById = mapOf("quest_alpha" to "stage_two"),
            questTasksCompleted = mapOf("quest_alpha" to setOf("task_intro", "task_power")),
            completedEvents = setOf("evt_once")
        )

        persistence.writeSlot(2, state)
        val restored = persistence.slotInfo(2)?.state

        assertNotNull(restored)
        restored!!
        assertEquals("stage_two", restored.questStageById["quest_alpha"])
        val tasks = restored.questTasksCompleted["quest_alpha"].orEmpty()
        assertTrue(tasks.contains("task_intro"))
        assertTrue(tasks.contains("task_power"))
        assertTrue(restored.completedEvents.contains("evt_once"))
    }

    @Test
    fun tutorialAndUnlockStatePersists() = runBlocking {
        val state = GameSessionState(
            tutorialSeen = setOf("light_switch_touch"),
            tutorialCompleted = setOf("light_switch_touch", "swipe_move"),
            tutorialRoomsSeen = setOf("town_9"),
            unlockedAreas = setOf("mines_2"),
            unlockedExits = setOf("mines_3::north")
        )

        persistence.writeSlot(3, state)
        val restored = persistence.slotInfo(3)?.state

        assertNotNull(restored)
        restored!!
        assertTrue(restored.tutorialSeen.contains("light_switch_touch"))
        assertTrue(restored.tutorialCompleted.contains("swipe_move"))
        assertTrue(restored.tutorialRoomsSeen.contains("town_9"))
        assertTrue(restored.unlockedAreas.contains("mines_2"))
        assertTrue(restored.unlockedExits.contains("mines_3::north"))
    }

    @Test
    fun quickSaveRoundTrip() = runBlocking {
        val state = GameSessionState(
            worldId = "nova_prime",
            hubId = "mining_colony",
            roomId = "town_9",
            playerCredits = 42
        )

        persistence.writeQuickSave(state)
        val restored = persistence.quickSaveInfo()?.state

        assertNotNull(restored)
        restored!!
        assertEquals("nova_prime", restored.worldId)
        assertEquals(42, restored.playerCredits)
    }

    @Test
    fun world1LaunchStatePersistsAcrossSlotAndAutosave() = runBlocking {
        val state = GameSessionState(
            worldId = "world_1",
            hubId = "hub_2_logistics",
            roomId = "launch_pod",
            playerId = "nova",
            playerLevel = 3,
            playerXp = 725,
            playerAp = 2,
            playerCredits = 640,
            partyMembers = listOf("nova", "zeke"),
            partyMemberLevels = mapOf("nova" to 3, "zeke" to 2),
            partyMemberXp = mapOf("nova" to 725, "zeke" to 180),
            partyMemberHp = mapOf("nova" to 460, "zeke" to 390),
            activeQuests = setOf("w1_mq05"),
            completedQuests = setOf("w1_mq01", "w1_mq02", "w1_mq03", "w1_mq04", "w1_sq03"),
            completedMilestones = setOf(
                "ms_w1_mq04_complete",
                "ms_w1_zeke_directed_to_pod",
                "ms_w1_chime_spliced"
            ),
            milestoneHistory = listOf(
                "ms_w1_mq04_complete",
                "ms_w1_zeke_directed_to_pod",
                "ms_w1_chime_spliced"
            ),
            unlockedSkills = setOf("nova_hydraulic_kick", "nova_arc_tether"),
            unlockedWeapons = setOf("mining_pistol", "zeke_tool_hammer"),
            unlockedArmors = setOf("patched_miner_jacket", "zeke_safety_vest"),
            unlockedAreas = setOf("launch_sector"),
            unlockedExits = setOf("launch_bay::north"),
            inventory = mapOf(
                "mine_access_badge" to 1,
                "jed_chime" to 1,
                "resonance_buoy_core" to 1,
                "wiring_bundle" to 2
            ),
            equippedWeapons = mapOf("nova" to "mining_pistol", "zeke" to "zeke_tool_hammer"),
            equippedArmors = mapOf("nova" to "patched_miner_jacket", "zeke" to "zeke_safety_vest"),
            trackedQuestId = "w1_mq05",
            questStageById = mapOf("w1_mq05" to "launch_pod"),
            questTasksCompleted = mapOf(
                "w1_mq05" to setOf(
                    "fight_through_launch_access",
                    "reach_pod_bay",
                    "survive_warden_intro",
                    "defeat_warden",
                    "splice_chime"
                )
            ),
            completedEvents = setOf(
                "w1_mq05_warden_defeated",
                "w1_mq05_launch_pod_ready"
            ),
            roomStates = mapOf(
                "launch_bay" to mapOf("warden_defeated" to true),
                "launch_pod" to mapOf("chime_spliced" to true)
            )
        )

        persistence.writeSlot(1, state)
        persistence.writeAutosave(state)

        listOf(
            "slot" to persistence.slotInfo(1)?.state,
            "autosave" to persistence.autosaveInfo()?.state
        ).forEach { (label, restored) ->
            assertNotNull("Expected $label state to restore", restored)
            restored!!
            assertEquals("world_1", restored.worldId)
            assertEquals("hub_2_logistics", restored.hubId)
            assertEquals("launch_pod", restored.roomId)
            assertEquals(listOf("nova", "zeke"), restored.partyMembers)
            assertEquals("w1_mq05", restored.trackedQuestId)
            assertEquals("launch_pod", restored.questStageById["w1_mq05"])
            assertEquals(true, restored.roomStates["launch_bay"]?.get("warden_defeated"))
            assertEquals(true, restored.roomStates["launch_pod"]?.get("chime_spliced"))
            assertTrue(restored.completedMilestones.contains("ms_w1_chime_spliced"))
            assertTrue(restored.completedMilestones.contains("ms_w1_zeke_directed_to_pod"))
            assertTrue(restored.unlockedSkills.contains("nova_hydraulic_kick"))
            assertTrue(restored.unlockedExits.contains("launch_bay::north"))
            assertTrue(restored.inventory.getValue("jed_chime") == 1)
            assertTrue(restored.inventory.getValue("wiring_bundle") == 2)
            assertEquals(
                setOf(
                    "fight_through_launch_access",
                    "reach_pod_bay",
                    "survive_warden_intro",
                    "defeat_warden",
                    "splice_chime"
                ),
                restored.questTasksCompleted["w1_mq05"]
            )
        }
    }

    @Test
    fun slotWritesCreateBackups() = runBlocking {
        val file = storeFile("game_session_slot1.pb")
        repeat(5) { idx ->
            persistence.writeSlot(
                1,
                GameSessionState(
                    worldId = "world",
                    playerLevel = idx + 1
                )
            )
        }
        val backups = file.parentFile?.listFiles { candidate ->
            candidate.name.startsWith("${file.name}.") && candidate.name.endsWith(".bak")
        }.orEmpty()
        assertTrue("Expected backup files to be created", backups.isNotEmpty())
        assertTrue("Should keep at most 3 backups", backups.size <= 3)
    }

    private fun clearStores() {
        val files = listOf(
            "game_session.pb",
            "game_session_autosave.pb",
            "game_session_quicksave.pb"
        ) + (1..3).map { "game_session_slot$it.pb" }
        files.forEach { name ->
            val file = storeFile(name)
            file.takeIf(File::exists)?.delete()
            file.parentFile?.listFiles { candidate ->
                candidate.name.startsWith("${file.name}.") && candidate.name.endsWith(".bak")
            }?.forEach(File::delete)
        }
    }

    private fun storeFile(name: String): File = File(baseDir, name)
}
