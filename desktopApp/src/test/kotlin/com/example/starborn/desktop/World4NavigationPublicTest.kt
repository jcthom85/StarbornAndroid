package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.viewmodel.ScriptedTravelStyle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World4NavigationPublicTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world4-navigation").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { services.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !services.exploration.uiState.value.isLoading }
            block(services)
        } finally { compose.runOnIdle { services.close() }; dir.deleteRecursively() }
    }

    private fun restore(s: DesktopAppServices, state: GameSessionState) {
        compose.runOnIdle { s.sessionStore.restore(state) }
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == state.roomId }
        dismissNarration(s)
    }

    private fun dismissNarration(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            compose.runOnIdle { s.exploration.dismissNarration() }
            compose.waitForIdle()
        }
    }

    private fun act(s: DesktopAppServices, event: String) {
        val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.actionEvent == event }
        compose.runOnIdle { s.exploration.onActionSelected(action) }
        compose.waitForIdle()
    }

    private fun finish(s: DesktopAppServices, seen: MutableList<String> = mutableListOf()) {
        repeat(25) {
            val line = s.exploration.uiState.value.activeDialogue?.line ?: return
            seen += line.id
            val choices = s.exploration.uiState.value.dialogueChoices
            if (choices.isEmpty()) compose.runOnIdle { s.exploration.advanceDialogue() }
            else compose.runOnIdle { s.exploration.onDialogueChoiceSelected(choices.single { it.id == "correct" }.id) }
            compose.waitForIdle()
        }
        error("World 3 dialogue did not finish")
    }

    private fun go(s: DesktopAppServices, direction: String, expected: String) {
        assertTrue(direction in setOf("north", "south", "east", "west"))
        dismissNarration(s)
        compose.runOnIdle { s.exploration.travel(direction) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == expected }
        dismissNarration(s)
        assertEquals(expected, s.sessionStore.state.value.roomId)
    }

    @Test fun phantomTerminalDoesNotQueueTheSameConversationTwice() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(
            worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_waste_intake",
            activeQuests = setOf("w4_mq17"), completedQuests = emptySet(),
            completedEvents = emptySet(), completedMilestones = emptySet(), questTasksCompleted = emptyMap()
        ))
        val terminal = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
            .single { it.actionEvent == "w4_mq17_access_phantom_terminal_scene" }
        // Two callbacks can arrive before the dialogue overlay has consumed the room tap.
        compose.runOnIdle {
            s.exploration.onActionSelected(terminal)
            s.exploration.onActionSelected(terminal)
        }
        val seen = mutableListOf<String>()
        compose.runOnIdle {
            seen += s.exploration.uiState.value.activeDialogue!!.line.id
            s.exploration.advanceDialogue()
            // An underlying party/NPC tap must not restart the scripted scene.
            s.exploration.onNpcInteraction("Gh0st")
            repeat(6) {
                s.exploration.uiState.value.activeDialogue?.line?.let {
                    seen += it.id
                    s.exploration.advanceDialogue()
                }
            }
        }
        compose.waitForIdle()
        assertEquals(listOf("gh0st_w4_phantom_intro", "nova_w4_phantom_prompt"), seen)
        assertTrue("access_phantom_terminal" in s.sessionStore.state.value.questTasksCompleted["w4_mq17"].orEmpty())
        dismissNarration(s)
        compose.waitUntil(5000) {
            s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
                .any { it.name == "project phantom terminal" && it.completedInspection }
        }
        val inspection = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
            .single { it.name == "project phantom terminal" }
        assertTrue(inspection.completedInspection)
        compose.runOnIdle { s.exploration.onActionSelected(inspection) }
        assertNull(s.exploration.uiState.value.activeDialogue)
    }

    @Test fun sabotageCrateBecomesAReadOnlyInspectionAndLegacyMilestoneRestoresIt() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(
            worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_slag_river",
            activeQuests = setOf("w4_sq16"), completedQuests = emptySet(),
            completedEvents = emptySet(), completedMilestones = emptySet(),
            questTasksCompleted = emptyMap(), roomStates = emptyMap()
        ))
        act(s, "w4_sq16_destroy_crate_alpha")
        compose.waitUntil(5000) {
            s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
                .any { it.name == "ammo crate alpha" && it.completedInspection }
        }
        val completed = s.sessionStore.state.value
        assertTrue("crate_alpha" in completed.questTasksCompleted["w4_sq16"].orEmpty())
        dismissNarration(s)
        restore(s, completed.copy(completedEvents = emptySet()))
        val inspection = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>()
            .single { it.name == "ammo crate alpha" }
        assertTrue(inspection.completedInspection)
        assertNull(inspection.actionEvent)
        val xp = s.sessionStore.state.value.playerXp
        val inventory = s.sessionStore.state.value.inventory
        compose.runOnIdle { s.exploration.onActionSelected(inspection) }
        compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
        assertEquals(xp, s.sessionStore.state.value.playerXp)
        assertEquals(inventory, s.sessionStore.state.value.inventory)
    }

    @Test fun allSeventyFourFoundryRoomsHaveWorkingCardinalReturnTravel() = runWith { s ->
        val ids = s.worldDataSource.loadHubNodes().filter { it.hubId in setOf("hub_7_slag_pits", "hub_8_assembly_line") }.flatMap { it.rooms }.toSet()
        val rooms = s.worldDataSource.loadRooms().filter { it.id in ids }.associateBy { it.id }
        restore(s, s.sessionStore.state.value.copy(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_slag_landing",
            activeQuests = emptySet(), completedQuests = (16..20).map { "w4_mq$it" }.toSet() + (16..20).map { "w4_sq$it" } + "w4_sq_crucible",
            completedEvents = emptySet(), completedMilestones = setOf("ms_w4_mq17_complete"), questTasksCompleted = emptyMap(),
            roomStates = mapOf("foundry_pipe_gallery" to mapOf("assembly_route_open" to true),
                "foundry_slag_stepping_stones" to mapOf("slag_bridge_solidified" to true),
                "foundry_slag_river" to mapOf("encounter_cleared:magma_drone" to true),
                "foundry_titan_dock" to mapOf("encounter_cleared:titan_walker_boss" to true))))
        val visited = mutableSetOf<String>(); val traversed = mutableSetOf<Pair<String, String>>()
        val reverse = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        fun walk(id: String) {
            if (!visited.add(id)) return
            assertEquals(rooms.getValue(id).connections.keys, s.exploration.uiState.value.directionIndicators.keys)
            assertTrue(s.exploration.uiState.value.actions.none { it is com.example.starborn.domain.model.TravelAction })
            for ((d, dest) in rooms.getValue(id).connections) {
                go(s, d, dest); traversed += id to dest
                walk(dest)
                go(s, reverse.getValue(d), id); traversed += dest to id
            }
        }
        walk("foundry_slag_landing")
        assertEquals(74, visited.size); assertEquals(ids, visited)
        assertEquals(rooms.values.sumOf { it.connections.size }, traversed.size)
        assertEquals("foundry_slag_landing", s.sessionStore.state.value.roomId)
    }

    @Test fun slagDroneBlocksTheCrossingUntilVictoryAndKeepsItOpenAfterReload() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(
            worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_slag_river",
            activeQuests = setOf("w4_mq16"), unlockedExits = emptySet(), roomStates = emptyMap()
        ))
        compose.runOnIdle { s.exploration.travel("east") }
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals("foundry_slag_river", s.sessionStore.state.value.roomId)
        go(s, "west", "foundry_slag_landing")
        go(s, "east", "foundry_slag_river")
        compose.runOnIdle {
            s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
                outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
                enemyIds = listOf("magma_drone"), roomId = "foundry_slag_river"
            ))
        }
        val cleared = s.sessionStore.state.value
        assertEquals(true, cleared.roomStates["foundry_slag_river"]?.get("encounter_cleared:magma_drone"))
        restore(s, cleared)
        go(s, "east", "foundry_waste_intake")
        go(s, "west", "foundry_slag_river")
    }

    @Test fun springsStoryOpensPermanentWalkbackAndLegacyReloadKeepsItWithoutAnotherReward() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_pipe_gallery",
            activeQuests = setOf("w4_mq17"), completedQuests = setOf("w4_mq16"), completedMilestones = emptySet(),
            completedEvents = emptySet(), unlockedExits = emptySet(), roomStates = emptyMap(),
            questTasksCompleted = mapOf("w4_mq17" to setOf("access_phantom_terminal"))))
        compose.runOnIdle { s.exploration.travel("east") }
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals("foundry_pipe_gallery", s.sessionStore.state.value.roomId)
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }; compose.waitForIdle()
        for ((d, room) in listOf("west" to "foundry_decon_chamber", "west" to "foundry_service_airlock", "west" to "foundry_waste_intake",
            "west" to "foundry_slag_river", "west" to "foundry_slag_landing", "south" to "foundry_cooling_springs")) go(s, d, room)
        val xp = s.sessionStore.state.value.playerXp
        act(s, "w4_mq17_regroup_springs_scene"); finish(s)
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "foundry_conveyor_belt" }; dismissNarration(s)
        val transfer = s.exploration.uiState.value.roomTransition
        assertEquals(ScriptedTravelStyle.FOUNDRY, transfer?.scriptedStyle)
        assertEquals("foundry_cooling_springs", transfer?.fromRoomId)
        assertEquals(xp + 800, s.sessionStore.state.value.playerXp)
        assertEquals(true, s.sessionStore.state.value.roomStates["foundry_pipe_gallery"]?.get("assembly_route_open"))
        assertTrue("w4_mq18" in s.sessionStore.state.value.activeQuests)
        go(s, "west", "foundry_reject_bay"); go(s, "west", "foundry_pipe_gallery")
        assertEquals("hub_7_slag_pits", s.sessionStore.state.value.hubId)
        val rewarded = s.sessionStore.state.value
        // A pre-repair save contains completed story progress but no corridor state.
        val old = rewarded.copy(roomStates = rewarded.roomStates - "foundry_pipe_gallery")
        assertTrue(s.saveManager.saveGame(9, old)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "foundry_pipe_gallery" }
        assertEquals(true, s.sessionStore.state.value.roomStates["foundry_pipe_gallery"]?.get("assembly_route_open"))
        assertEquals(rewarded.playerXp, s.sessionStore.state.value.playerXp)
        assertEquals(rewarded.inventory, s.sessionStore.state.value.inventory)
        go(s, "east", "foundry_reject_bay"); go(s, "east", "foundry_conveyor_belt")
        assertEquals("hub_8_assembly_line", s.sessionStore.state.value.hubId)
        assertEquals(rewarded.questTasksCompleted, s.sessionStore.state.value.questTasksCompleted)
        assertTrue("w4_mq17" in s.sessionStore.state.value.completedQuests)
    }

    @Test fun formerDiagonalBranchesReloadWithImmediateCardinalReturns() = runWith { s ->
        for ((room, direction, target) in listOf(
            Triple("foundry_waste_intake_scale_01", "north", "foundry_waste_intake"),
            Triple("foundry_conveyor_belt_scale_01", "south", "foundry_belt_service_walk"),
            Triple("foundry_power_core_scale_01", "north", "foundry_power_core")
        )) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_4", hubId = if (room.startsWith("foundry_waste")) "hub_7_slag_pits" else "hub_8_assembly_line",
                roomId = room, activeQuests = emptySet(), completedQuests = setOf("w4_mq17"),
                completedMilestones = setOf("ms_w4_mq17_complete"), completedEvents = emptySet(), questTasksCompleted = emptyMap(),
                unlockedExits = setOf("$room:southwest")))
            val before = s.sessionStore.state.value
            assertTrue(s.saveManager.saveGame(9, before)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == room }
            go(s, direction, target)
            assertEquals(before.completedQuests, s.sessionStore.state.value.completedQuests)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
        }
    }

    @Test fun optionalSmelterKeepsItsCoolantGateAndSavedReturn() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_4", hubId = "hub_7_slag_pits", roomId = "foundry_slag_stepping_stones",
            activeQuests = emptySet(), completedQuests = setOf("w4_mq17"), completedMilestones = emptySet(),
            completedEvents = emptySet(), roomStates = emptyMap(), unlockedExits = emptySet(), questTasksCompleted = emptyMap()))
        compose.runOnIdle { s.exploration.travel("north") }; compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals("foundry_slag_stepping_stones", s.sessionStore.state.value.roomId)
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }; compose.waitForIdle()
        fun toggle() {
            val valve = s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ToggleAction>().single { it.stateKey == "slag_bridge_solidified" }
            compose.runOnIdle { s.exploration.onActionSelected(valve) }; compose.waitForIdle(); dismissNarration(s)
        }
        toggle(); go(s, "north", "foundry_subterranean_smelter")
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "foundry_subterranean_smelter" }
        go(s, "south", "foundry_slag_stepping_stones"); toggle()
        compose.runOnIdle { s.exploration.travel("north") }; compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals("foundry_slag_stepping_stones", s.sessionStore.state.value.roomId)
    }
}
