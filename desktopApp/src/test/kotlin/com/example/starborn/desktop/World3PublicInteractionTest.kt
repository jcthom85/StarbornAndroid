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
        compose.waitUntil(10000) {
            val ui = s.exploration.uiState.value
            val restored = s.sessionStore.state.value
            !ui.isLoading && ui.currentRoom?.id == state.roomId &&
                ui.completedMilestones == restored.completedMilestones && ui.completedQuests == restored.completedQuests
        }
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

    @Test fun leavingAnNpcDoesNotDiscardAnAlreadyQueuedScriptedInteraction() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city",
            roomId = "spire_prism_gallery", activeQuests = setOf("w3_mq14"), completedQuests = emptySet(),
            completedEvents = emptySet(), completedMilestones = setOf("ms_w3_containment_field_read",
                "ms_w3_prism_shutters_read", "ms_w3_command_tethers_traced"),
            questTasksCompleted = mapOf("w3_mq14" to setOf("enter_archive"))))
        compose.runOnIdle { s.exploration.onNpcInteraction("Evelyn") }
        act(s, "w3_mq14_route_prisms")
        assertTrue(s.exploration.uiState.value.activeDialogue!!.canDismiss)
        compose.runOnIdle { s.exploration.dismissDialogue() }
        assertEquals("w3_prism_route_entry", s.exploration.uiState.value.activeDialogue?.line?.id)
        assertFalse(s.exploration.uiState.value.activeDialogue!!.canDismiss)
        assertFalse("ms_w3_prism_solved" in s.sessionStore.state.value.completedMilestones)
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("leave") }
        assertNull(s.exploration.uiState.value.activeDialogue)
    }

    @Test fun worldSixCompanionConversationsCanBeLeftWithoutChangingRescueProgress() = runWith { s ->
        for ((npc, task, line) in listOf(
            Triple("Zeke", "rescue_zeke", "zeke_w6_nightmare_release"),
            Triple("Gh0st", "rescue_gh0st", "gh0st_w6_nightmare_release"),
            Triple("Orion", "rescue_orion", "orion_w6_nightmare_release"))) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_6", hubId = "hub_11_event_horizon",
                roomId = "source_campfire", activeQuests = setOf("w6_mq26"), completedQuests = emptySet(),
                completedMilestones = emptySet(), completedEvents = emptySet(),
                questTasksCompleted = mapOf("w6_mq26" to setOf(task))))
            compose.runOnIdle { s.exploration.onNpcInteraction(npc) }
            assertEquals(line, s.exploration.uiState.value.activeDialogue?.line?.id)
            assertTrue(s.exploration.uiState.value.activeDialogue!!.canDismiss)
            val before = s.sessionStore.state.value
            compose.runOnIdle { s.exploration.dismissDialogue() }
            assertNull(s.exploration.uiState.value.activeDialogue)
            assertEquals(before.questTasksCompleted, s.sessionStore.state.value.questTasksCompleted)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
        }
    }

    @Test fun leavingNpcConversationsDoesNotExecuteUnfinishedGiftTriggers() = runWith { s ->
        for ((npc, giftChoice, item) in listOf(
            Triple("Evelyn", "evelyn_opt_favor_offer", "mimis_jazz_bloom"),
            Triple("Robert", "robert_opt_gift", "roberts_buffer_core"))) {
            restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city",
                roomId = "spire_skypark_dome", activeQuests = emptySet(),
                completedQuests = (11..15).map { "w3_mq$it" }.toSet(), completedMilestones = emptySet()))
            compose.runOnIdle { s.exploration.onNpcInteraction(npc) }
            assertTrue(s.exploration.uiState.value.activeDialogue!!.canDismiss)
            val before = s.sessionStore.state.value
            compose.runOnIdle { s.exploration.onDialogueChoiceSelected(giftChoice) }
            compose.runOnIdle { s.exploration.dismissDialogue() }
            assertNull(s.exploration.uiState.value.activeDialogue)
            assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertEquals(before.completedMilestones, s.sessionStore.state.value.completedMilestones)
            assertEquals(0, s.inventoryService.snapshot()[item] ?: 0)
            compose.runOnIdle { s.exploration.onNpcInteraction(npc) }
            compose.runOnIdle { s.exploration.onDialogueChoiceSelected(giftChoice) }
            compose.runOnIdle { s.exploration.advanceDialogue() }
            assertEquals(1, s.inventoryService.snapshot()[item])
            compose.runOnIdle { s.exploration.dismissDialogue() }
            assertNull(s.exploration.uiState.value.activeDialogue)
        }
    }

    @Test fun fixedDismissalWorksAcrossLaterWorldResidents() = runWith { s ->
        for ((npc, room, hub) in listOf(
            Triple("Cheno", "spire_rigging_deck", "hub_5_lower_city"),
            Triple("Chris", "spire_old_subway_car", "hub_5_lower_city"),
            Triple("Andrew", "spire_old_subway_car", "hub_5_lower_city"),
            Triple("Jason", "spire_hidden_studio", "hub_5_lower_city"),
            Triple("Meeko", "spire_hidden_studio", "hub_5_lower_city"),
            Triple("Rylan", "foundry_lava_tube_bypass", "hub_7_slag_pits"),
            Triple("Cameron", "foundry_pipe_gallery", "hub_7_slag_pits"),
            Triple("Charles", "orbital_customs_lounge", "hub_9_orbital_ring"),
            Triple("Oliver", "astra_quarters", "hub_astra"))) {
            restore(s, s.sessionStore.state.value.copy(roomId = room, hubId = hub,
                activeQuests = emptySet(), completedQuests = emptySet(), completedMilestones = emptySet(), completedEvents = emptySet()))
            compose.runOnIdle { s.exploration.onNpcInteraction(npc) }
            assertNotNull("$npc should open", s.exploration.uiState.value.activeDialogue)
            assertTrue("$npc should expose a persistent Leave control", s.exploration.uiState.value.activeDialogue!!.canDismiss)
            compose.runOnIdle { s.exploration.dismissDialogue() }
            assertNull("$npc should close", s.exploration.uiState.value.activeDialogue)
            assertTrue(s.exploration.uiState.value.dialogueChoices.isEmpty())
        }
    }

    @Test fun coverStoryRecoversAfterWrongAnswerLeaveAndSaveReload() = runWith { s ->
        restore(s, s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city", roomId = "spire_skypark_dome",
            activeQuests = setOf("w3_mq13"), completedQuests = setOf("w3_mq12"), completedEvents = emptySet(),
            completedMilestones = setOf("ms_w3_mq12_complete"), questTasksCompleted = emptyMap()))
        act(s, "w3_mq13_review_guest_access")
        repeat(2) { compose.runOnIdle { s.exploration.advanceDialogue() }; compose.waitForIdle() }
        assertEquals("w3_cover_order", s.exploration.uiState.value.activeDialogue?.line?.id)
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("cover_emergency") }
        compose.runOnIdle { s.exploration.advanceDialogue() }
        assertFalse("blend_in" in s.sessionStore.state.value.questTasksCompleted["w3_mq13"].orEmpty())
        compose.runOnIdle { s.exploration.onDialogueChoiceSelected("w3_cover_recovery_leave") }
        assertNull(s.exploration.uiState.value.activeDialogue)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
        dismissNarration(s)
        act(s, "w3_mq13_review_guest_access"); finish(s)
        assertTrue("blend_in" in s.sessionStore.state.value.questTasksCompleted["w3_mq13"].orEmpty())
    }

    @Test fun localQuestConsequencesStayVisibleWithoutPayingAgain() = runWith { s ->
        val base = s.sessionStore.state.value.copy(worldId = "world_3", hubId = "hub_6_upper_city",
            roomId = "spire_service_lift", activeQuests = setOf("w3_sq14"), completedQuests = setOf("w3_mq12"),
            completedMilestones = setOf("ms_w3_mq12_complete", "ms_w3_ledger_stolen"), completedEvents = emptySet())
        for (branch in listOf("w3_sq14_leak_ledger", "w3_sq14_weaponize_ledger")) {
            restore(s, base)
            act(s, branch); dismissNarration(s)
            compose.waitUntil(5000) { "w3_sq14" in s.sessionStore.state.value.completedQuests }
            val before = s.sessionStore.state.value
            compose.waitUntil(5000) { s.exploration.uiState.value.completedMilestones == before.completedMilestones }
            assertFalse("branch=$branch milestones=${before.completedMilestones} actions=${s.exploration.uiState.value.actions.map { it.name }}",
                s.exploration.uiState.value.actions.any { it.name in setOf("worker network uplink", "private dead drop") })
            assertTrue(s.saveManager.saveGame(9, before))
            assertTrue(compose.runOnIdle { s.loadSlot(9) })
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            dismissNarration(s)
            repeat(2) {
                act(s, "w3_read_ledger_receipts")
                compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
                val message = s.exploration.uiState.value.narrationPrompt!!.message
                assertTrue(message.contains(if (branch == "w3_sq14_leak_ledger") "workers comparing" else "No worker-network publication"))
                dismissNarration(s)
            }
            assertEquals(before.inventory, s.sessionStore.state.value.inventory)
            assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
        }
        for ((room, milestone, name) in listOf(
            Triple("spire_night_market", "ms_w3_market_lit", "open stalls"),
            Triple("spire_drone_test_alcove", "ms_w3_prototype_trial_complete", "trial replay"))) {
            restore(s, base.copy(roomId = room, completedMilestones = base.completedMilestones))
            assertFalse(s.exploration.uiState.value.actions.any { it.name == name })
            restore(s, base.copy(roomId = room, completedMilestones = base.completedMilestones + milestone))
            val action = s.exploration.uiState.value.actions.single { it.name == name }
            compose.runOnIdle { s.exploration.onActionSelected(action) }
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
            assertTrue(s.exploration.uiState.value.narrationPrompt!!.message.isNotBlank())
            dismissNarration(s)
        }
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
