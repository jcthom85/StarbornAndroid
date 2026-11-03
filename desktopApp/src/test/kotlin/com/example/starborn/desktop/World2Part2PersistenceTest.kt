package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World2Part2PersistenceTest {
    @get:Rule val compose = createComposeRule()
    private fun services(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("part2-persistence").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            block(s)
        } finally { s.close(); dir.deleteRecursively() }
    }
    @Test fun resonatorColumnsExplainMissingCrystalPrerequisites() = services { s ->
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_archive_tuning_bay",
            activeQuests = setOf("w2_sq04"), completedMilestones = emptySet(), completedEvents = emptySet(),
            questTasksCompleted = emptyMap()))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_archive_tuning_bay" }
        s.exploration.onActionSelected(s.exploration.uiState.value.actions.single { it.name == "resonator columns" })
        compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt?.message?.contains("Seat the west") == true }
        assertFalse("ms_w2_sq04_complete" in s.sessionStore.state.value.completedMilestones)
    }

    @Test fun sourceGateShowsLockUntilResolvedAndAgainAfterEarlierRestore() = services { s ->
        val earlier = s.sessionStore.state.value.copy(roomId = "sector9_source_gate", activeQuests = emptySet(),
            completedMilestones = emptySet(), roomStates = emptyMap(), unlockedExits = emptySet())
        s.sessionStore.restore(earlier)
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == earlier.roomId }
        compose.waitUntil(5000) { s.exploration.uiState.value.directionIndicators["east"]?.status ==
            com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorStatus.LOCKED }
        s.sessionStore.setRoomState(earlier.roomId!!, "barrier_resolved", true)
        compose.waitUntil(5000) { "east" !in s.exploration.uiState.value.blockedDirections }
        s.sessionStore.restore(earlier)
        compose.waitUntil(5000) { s.exploration.uiState.value.directionIndicators["east"]?.status ==
            com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorStatus.LOCKED }
    }

    @Test fun loadingEarlierSaveRemovesFutureRoomFlagsAndRestoresGateArtState() = services { s ->
        val base = s.sessionStore.state.value.copy(roomId = "sector9_temple_gate", activeQuests = emptySet(),
            completedMilestones = emptySet(), roomStates = emptyMap())
        s.sessionStore.restore(base)
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == base.roomId }
        s.sessionStore.setRoomState(base.roomId!!, "temple_open", true)
        compose.waitUntil(5000) { s.exploration.uiState.value.roomState["temple_open"] == true }
        s.sessionStore.restore(base)
        compose.waitUntil(5000) { s.exploration.uiState.value.roomState["temple_open"] != true }
        assertFalse(s.exploration.uiState.value.currentRoom!!.state["temple_open"] == true)
        s.sessionStore.setRoomState(base.roomId!!, "temple_open", true)
        compose.waitUntil(5000) { s.exploration.uiState.value.roomState["temple_open"] == true }
    }
    @Test fun groundItemsSurviveControllerRecreationAndEarlierSaveRestoresThem() = services { s ->
        val room = s.worldDataSource.loadRooms().first { it.items.isNotEmpty() }
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = room.id, activeQuests = emptySet()))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == room.id }
        val before = s.sessionStore.state.value
        val loot = s.exploration.uiState.value.groundItems.toMap()
        assertTrue(loot.isNotEmpty())
        s.exploration.collectAllGroundItems()
        assertEquals(emptyMap<String, Int>(), s.sessionStore.state.value.roomGroundItems[room.id])
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(s.loadSlot(9))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room.id }
        assertTrue(s.exploration.uiState.value.groundItems.isEmpty())
        val inventory = s.inventoryService.snapshot()
        s.exploration.collectAllGroundItems()
        assertEquals(inventory, s.inventoryService.snapshot())
        s.sessionStore.restore(before)
        compose.waitUntil(5000) { s.exploration.uiState.value.groundItems == loot }
    }
    @Test fun copiedPlaytestSaveCanGatherWithoutInventingCraftCredit() {
        val source = System.getenv("STARBORN_PLAYTEST_SAVE")
        org.junit.Assume.assumeNotNull(source)
        val dir = Files.createTempDirectory("copied-playtest").toFile()
        val saves = java.io.File(dir, "saves").apply { mkdirs() }
        java.io.File(requireNotNull(source)).copyTo(java.io.File(saves, "game_session_slot9.pb"))
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            assertTrue(s.loadSlot(9))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            assertTrue("visit_beach" in s.sessionStore.state.value.questTasksCompleted["w2_sq03"].orEmpty())
            assertFalse("craft_source_resin" in s.sessionStore.state.value.questTasksCompleted["w2_sq03"].orEmpty())
            s.sessionStore.setRoom("sector9_beach_pools")
            compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_beach_pools" }
            val herbs = s.inventoryService.snapshot()["herb"] ?: 0
            s.exploration.onActionSelected(s.exploration.uiState.value.actions.single { it.name == "tideglass growth" })
            compose.waitUntil(5000) { s.inventoryService.hasItem("schematic_source_resin") }
            assertEquals(herbs + 1, s.inventoryService.snapshot()["herb"])
            s.exploration.useInventoryItem("schematic_source_resin")
            compose.waitUntil(5000) { s.craftingService.isSchematicLearned("mod_source_resin") }
            assertTrue(s.craftingService.craftTinkering("mod_source_resin") is com.example.starborn.domain.crafting.CraftingOutcome.Success)
            s.exploration.onTinkeringCrafted("source_resin")
            compose.waitUntil(5000) { "w2_sq03" in s.sessionStore.state.value.completedQuests }
        } finally { s.close(); dir.deleteRecursively() }
    }

    @Test fun emptyAndPartialLootSnapshotsRoundTripInProtocol() = runBlocking {
        val dir = Files.createTempDirectory("ground-loot-protocol").toFile()
        try {
            val persistence = GameSessionPersistence(dir)
            val state = GameSessionState(roomGroundItems = mapOf("emptied" to emptyMap(), "partial" to mapOf("herb" to 2)))
            persistence.writeSlot(1, state)
            val restored = requireNotNull(persistence.readSlot(1))
            assertTrue("emptied" in restored.roomGroundItems)
            assertEquals(state.roomGroundItems, restored.roomGroundItems)
        } finally { dir.deleteRecursively() }
    }
    @Test fun tideglassCanBeGatheredAfterVisitCreditAndCraftedWithRealController() = services { s ->
        val before = s.sessionStore.state.value
        s.sessionStore.restore(before.copy(roomId = "sector9_beach_pools", activeQuests = setOf("w2_sq03"),
            questTasksCompleted = before.questTasksCompleted - "w2_sq03", completedEvents = emptySet(),
            completedMilestones = emptySet()))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_beach_pools" }
        compose.waitUntil(5000) { "visit_beach" in s.sessionStore.state.value.questTasksCompleted["w2_sq03"].orEmpty() }
        val growth = s.exploration.uiState.value.actions.single { it.name == "tideglass growth" }
        val herbs = s.inventoryService.snapshot()["herb"] ?: 0
        s.exploration.onActionSelected(growth)
        compose.waitUntil(5000) { "ms_w2_tideglass_gathered" in s.sessionStore.state.value.completedMilestones }
        assertEquals(herbs + 1, s.inventoryService.snapshot()["herb"])
        assertTrue(s.inventoryService.hasItem("schematic_source_resin"))
        s.exploration.useInventoryItem("schematic_source_resin")
        compose.waitUntil(5000) { s.craftingService.isSchematicLearned("mod_source_resin") }
        val crafted = s.craftingService.craftTinkering("mod_source_resin")
        assertTrue(crafted.toString(), crafted is com.example.starborn.domain.crafting.CraftingOutcome.Success)
        s.exploration.onTinkeringCrafted("source_resin")
        compose.waitUntil(5000) { "w2_sq03" in s.sessionStore.state.value.completedQuests }
    }

    @Test fun returningToAnEarlierQuestSaveDoesNotKeepFutureTaskReceiptsInMemory() = services { s ->
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_stasis_chamber", activeQuests = setOf("w2_sq03"),
            questTasksCompleted = emptyMap(), completedEvents = emptySet(), completedMilestones = emptySet()))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_stasis_chamber" }
        val earlier = s.sessionStore.state.value
        s.sessionStore.setRoom("sector9_beach_pools")
        compose.waitUntil(5000) { "visit_beach" in s.sessionStore.state.value.questTasksCompleted["w2_sq03"].orEmpty() }
        s.sessionStore.restore(earlier)
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_stasis_chamber" }
        s.sessionStore.setRoom("sector9_beach_pools")
        compose.waitUntil(5000) { "visit_beach" in s.sessionStore.state.value.questTasksCompleted["w2_sq03"].orEmpty() }
    }
}
