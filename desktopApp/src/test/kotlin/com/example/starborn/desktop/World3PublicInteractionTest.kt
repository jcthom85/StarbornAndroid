package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World3PublicInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world3-public").toFile()
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

    @Test fun guestRegistryDeliversCuratorSceneThroughVisibleAction() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_skypark_dome",
            activeQuests = setOf("w3_mq13"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w3_mq12_complete"), questTasksCompleted = emptyMap()))
        assertTrue("curator" in s.exploration.uiState.value.npcs)
        compose.runOnIdle { s.exploration.onNpcInteraction("curator") }
        assertEquals("curator_w3_mq13_intro", s.exploration.uiState.value.activeDialogue?.line?.id)
        finish(s)
        assertTrue("blend_in" in s.sessionStore.state.value.questTasksCompleted["w3_mq13"].orEmpty())
        compose.runOnIdle { s.exploration.onNpcInteraction("curator") }
        assertEquals("curator_w3_guest_accepted", s.exploration.uiState.value.activeDialogue?.line?.id)
        finish(s)
        // An earlier timeline must expose the same authored scene from the registry itself.
        restore(s, s.sessionStore.state.value.copy(completedMilestones = setOf("ms_w3_mq12_complete"),
            completedEvents = emptySet(), questTasksCompleted = emptyMap()))
        act(s, "w3_mq13_review_guest_access")
        assertEquals("curator_w3_mq13_intro", s.exploration.uiState.value.activeDialogue?.line?.id)
        finish(s)
    }

    @Test fun prismRetrySaveLoadAndLensWarningLeadToOneAwardAndEscalation() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_prism_gallery",
            activeQuests = setOf("w3_mq14"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w3_containment_field_read", "ms_w3_prism_shutters_read", "ms_w3_command_tethers_traced"),
            questTasksCompleted = mapOf("w3_mq14" to setOf("enter_archive"))))
        act(s, "w3_mq14_route_prisms")
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("feed") }
        assertEquals("w3_prism_pressure_retry", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse("ms_w3_prism_solved" in s.sessionStore.state.value.completedMilestones)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.activeDialogue == null }
        dismissNarration(s)
        act(s, "w3_mq14_route_prisms")
        finish(s)
        assertTrue("solve_light_puzzle" in s.sessionStore.state.value.questTasksCompleted["w3_mq14"].orEmpty())
        dismissNarration(s)
        compose.runOnIdle { s.exploration.travel("east") }
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_archive_vault" }
        dismissNarration(s)
        val xp = s.sessionStore.state.value.playerXp
        act(s, "w3_mq14_review_lens")
        assertEquals("zeke_w3_mq14_lens_warning", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse(s.inventoryService.hasItem("the_lens"))
        val seen = mutableListOf<String>()
        finish(s, seen)
        assertTrue("thorne_w3_mq14_trap_intro" in seen)
        assertTrue("vale_w3_mq15_override" in seen)
        assertEquals(xp + 550, s.sessionStore.state.value.playerXp)
        assertEquals(1, s.inventoryService.snapshot()["the_lens"])
        assertTrue("w3_mq15" in s.sessionStore.state.value.activeQuests)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        assertEquals(xp + 550, s.sessionStore.state.value.playerXp)
        assertEquals(1, s.inventoryService.snapshot()["the_lens"])
    }

    @Test fun mikaCannotSkipTraceAndLegacyShortcutCanBeUsedTwice() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_5_lower_city", roomId = "spire_night_market",
            activeQuests = setOf("w3_sq13"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w2_mq05_complete"), questTasksCompleted = emptyMap()))
        compose.runOnIdle { s.exploration.onNpcInteraction("mika") }
        assertEquals("mika_w3_trace_hint", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse(s.exploration.uiState.value.dialogueChoices.any { it.id == "mika_w3_sq13_fix" })
        finish(s)
        val earlier = s.sessionStore.state.value.copy(roomId = "spire_night_market_scale_03",
            completedEvents = setOf("warp_spire_night_market_scale_03_to_entry"))
        repeat(2) {
            restore(s, earlier)
            act(s, "warp_spire_night_market_scale_03_to_entry")
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_night_market" }
        }
    }
    @Test fun serviceLiftMakesBothDelayedQuestReturnsPossibleAcrossSaveLoad() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_service_lift",
            activeQuests = setOf("w3_mq14", "w3_sq11", "w3_sq12"), completedQuests = setOf("w3_mq12"), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w2_mq05_complete", "ms_w3_mq12_complete"), questTasksCompleted = emptyMap()))
        val inventory = s.inventoryService.snapshot()
        repeat(2) {
            act(s, "w3_return_to_lower_city")
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_transit_plaza" }
            dismissNarration(s)
            assertEquals("hub_5_lower_city", s.sessionStore.state.value.hubId)
            compose.runOnIdle { s.exploration.travel("north") }
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_the_static" }
            assertTrue("jax" in s.exploration.uiState.value.npcs)
            assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
            assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            dismissNarration(s)
            compose.runOnIdle { s.exploration.travel("south") }
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_transit_plaza" }
            act(s, "w3_mq12_use_elevator")
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_laundry_service" }
            dismissNarration(s)
            assertEquals("hub_6_upper_city", s.sessionStore.state.value.hubId)
            compose.runOnIdle { s.exploration.travel("south") }
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_uniform_sorting" }
            compose.runOnIdle { s.exploration.travel("east") }
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_service_lift" }
            dismissNarration(s)
        }
        assertEquals(inventory, s.inventoryService.snapshot())
        assertTrue(s.sessionStore.state.value.activeQuests.containsAll(setOf("w3_mq14", "w3_sq11", "w3_sq12")))
        // A stale control callback outside its authored room cannot teleport the player.
        compose.runOnIdle { s.exploration.onActionSelected(GenericAction("elevator controls", "generic", actionEvent = "w3_mq12_use_elevator")) }
        compose.waitForIdle()
        assertEquals("spire_service_lift", s.sessionStore.state.value.roomId)
    }

    @Test fun earlyMurderEvidenceSurvivesEarlierTimelineReloadAndPaysOnce() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_vip_private_booth",
            activeQuests = setOf("w3_mq14"), completedQuests = emptySet(), completedEvents = emptySet(),
            completedMilestones = emptySet(), questTasksCompleted = emptyMap()))
        act(s, "w3_sq16_inspect_glass")
        dismissNarration(s)
        assertTrue("inspect_glass" in s.sessionStore.state.value.questTasksCompleted["w3_sq16"].orEmpty())
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        act(s, "w3_sq16_inspect_rig")
        dismissNarration(s)
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        dismissNarration(s)
        assertFalse("ms_w3_sq16_rig_inspected" in s.sessionStore.state.value.completedMilestones)
        compose.runOnIdle { s.exploration.travel("south") }
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_skyline_vip_suite" }
        act(s, "w3_sq16_start"); dismissNarration(s)
        compose.runOnIdle { s.exploration.onNpcInteraction("director_vance") }
        assertEquals("vance_initial_alibi", s.exploration.uiState.value.activeDialogue?.line?.id)
        finish(s)
        act(s, "w3_sq16_inspect_terminal"); dismissNarration(s)
        compose.runOnIdle { s.exploration.travel("north") }
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_vip_private_booth" }
        act(s, "w3_sq16_inspect_rig"); dismissNarration(s)
        compose.runOnIdle { s.exploration.travel("south") }
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "spire_skyline_vip_suite" }
        val xp = s.sessionStore.state.value.playerXp
        compose.runOnIdle { s.exploration.onNpcInteraction("director_vance") }
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("vance_confront_evidence") }
        finish(s)
        assertEquals(xp + 450, s.sessionStore.state.value.playerXp)
        assertEquals(1, s.inventoryService.snapshot()["cyano_coolant_injector"])
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        compose.runOnIdle { s.exploration.onNpcInteraction("director_vance") }; finish(s)
        assertEquals(xp + 450, s.sessionStore.state.value.playerXp)
        assertEquals(1, s.inventoryService.snapshot()["cyano_coolant_injector"])
    }

}
