package com.example.starborn

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.starborn.di.AppServices
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.ui.DialogueOverlay
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DialogueReviewPlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var services: AppServices
    private lateinit var vm: ExplorationViewModel
    private val state get() = services.sessionStore.state.value

    @Test fun pamFamilyAndOneTimeGift() = inRoom(1, "hub_1_homestead", "pit_mess") {
        talk("Pam", "pam_default_talk")
        choose("Who’s in those photos behind the till?", "pam_family_reply")
        capture("w1-pam")
        advance("pam_default_talk")
        choose("Got anything warm to take into the deep shafts?", "pam_biscuits_reply")
        advance("pam_default_talk")
        assertEquals(1, quantity("gagas_skillet_biscuits"))
        close("Thanks, Pam. I'll let you get back to it.")
        talk("Pam", "pam_default_talk")
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "pam_opt_biscuits" })
        close("Thanks, Pam. I'll let you get back to it.")
    }

    @Test fun kaelenFirstVisitPatrolAndRepeatAdvice() = inRoom(2, "hub_3_sector9", "sector9_beach_cove") {
        talk("Scout Kaelen", "kaelen_intro")
        choose("I'm Nova. We crashed recently in an escape pod.", "kaelen_who_reply")
        advance("kaelen_supplies")
        advance(null)
        compose.runOnIdle { services.sessionStore.setMilestone("ms_w2_cutter_repaired") }
        talk("Scout Kaelen", "kaelen_repeat_default_talk")
        choose("I found your scout patrol's beacon in the Burned Glade. I recovered their Thermal Cutter.", "kaelen_patrol_acknowledged")
        capture("w2-kaelen")
        advance("kaelen_repeat_default_talk")
        assertEquals(1, quantity("battery_pack"))
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "kaelen_opt_patrol" })
        choose("Remind me about the coastal hazards.", "kaelen_repeat_advice")
        advance(null)
        talk("Scout Kaelen", "kaelen_repeat_default_talk")
        choose("How are you holding up out here on your own?", "kaelen_favor_offer")
        advance("kaelen_repeat_default_talk")
        close("Let's leave it there for now.")
        compose.runOnIdle {
            services.inventoryService.addItem("herb", 1)
            services.inventoryService.addItem("beast_meat", 1)
        }
        talk("Scout Kaelen", "kaelen_repeat_default_talk")
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "kaelen_opt_favor_turnin" })
        close("Let's leave it there for now.")
        compose.runOnIdle { services.inventoryService.addItem("herb", 1) }
        talk("Scout Kaelen", "kaelen_repeat_default_talk")
        choose("I've gathered fresh herbs and beast meat from the wilds.", "kaelen_favor_turnin")
        advance("kaelen_repeat_default_talk")
        assertEquals(1, quantity("precision_sight"))
        assertEquals(0, quantity("herb"))
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "kaelen_opt_favor_turnin" })
        close("Let's leave it there for now.")
    }

    @Test fun gardenTopicsAndGoodbyes() = inRoom(3, "hub_6_upper_city", "spire_skypark_dome") {
        talk("Evelyn", "evelyn_default_talk")
        choose("Do the plants really respond to jazz?", "evelyn_music_reply")
        advance("evelyn_default_talk")
        choose("Your orchids look exceptionally vibrant.", "evelyn_favor_reply")
        advance("evelyn_default_talk")
        assertTrue("ms_evelyn_bloom_received" in state.completedMilestones)
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "evelyn_opt_favor_offer" })
        capture("w3-evelyn")
        close("I'll let you get back to it. Take care.")
        talk("Robert", "robert_default_talk")
        choose("You built a company out of specialized mops?", "robert_business_reply")
        advance("robert_default_talk")
        choose("Your industrial equipment is seriously impressive.", "robert_gift_reply")
        advance("robert_default_talk")
        assertTrue("ms_robert_core_received" in state.completedMilestones)
        close("I'll let you get back to it. Take care.")
    }

    @Test fun chenoRequiresBothSetsOfParts() = inRoom(
        3, "hub_5_lower_city", "spire_rigging_deck", mapOf("heavy_gear" to 2, "wiring_bundle" to 1)
    ) {
        talk("Cheno", "cheno_default_talk")
        choose("Need an extra pair of hands on the rigging?", "cheno_favor_offer")
        advance("cheno_default_talk")
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "cheno_opt_favor_turnin" })
        assertEquals(2, quantity("heavy_gear"))
        close("I'll see you around. Take care.")
        compose.runOnIdle { services.inventoryService.addItem("wiring_bundle", 1) }
        talk("Cheno", "cheno_default_talk")
        choose("I brought the heavy gears and wiring bundles for the center truss.", "cheno_favor_turnin")
        advance("cheno_default_talk")
        assertEquals(1, quantity("cheno_carabiner"))
        assertEquals(0, quantity("heavy_gear"))
        assertEquals(0, quantity("wiring_bundle"))
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "cheno_opt_favor_turnin" })
        close("I'll see you around. Take care.")
    }

    @Test fun sorrellWrongEvidenceExitAndCompletedRevisit() = inRoom(3, "hub_6_upper_city", "spire_skyline_vip_suite") {
        talk("Director Sorrell", "vance_initial_alibi")
        advance(null)
        compose.runOnIdle {
            listOf("glass", "terminal", "rig").forEach { services.sessionStore.setMilestone("ms_w3_sq16_${it}_inspected") }
        }
        talk("Director Sorrell", "director_vance_default_talk")
        choose("Let's check your alibi against the records.", "w3_case_alibi_question")
        choose("His comms unit vented coolant at 21:12.", "w3_case_remote_rebuttal")
        capture("w3-sorrell-rebuttal")
        advance("w3_case_alibi_question")
        close("Review the evidence before continuing.")
        assertFalse("w3_sq16" in state.completedQuests)
        talk("Director Sorrell", "director_vance_default_talk")
        choose("Let's check your alibi against the records.", "w3_case_alibi_question")
        choose("His badge entered the Syndicate Booth at 21:14.", "vance_confession_1")
        advance("w3_case_physical_question")
        choose("The badge timestamp repeats his name.", "w3_case_same_record_rebuttal")
        advance("w3_case_physical_question")
        choose("Coolant in the glass matches the missing core in his comms rig.", "vance_confession_2")
        advance("vance_confession_3")
        advance("vance_confession_admission")
        advance("vance_security_aftermath")
        advance(null)
        assertTrue("w3_sq16" in state.completedQuests)
        val xp = state.playerXp
        runBlocking {
            services.saveSlot(1)
            assertTrue(services.loadSlot(1))
        }
        assertEquals(1, quantity("cyano_coolant_injector"))
        talk("Director Sorrell", "vance_case_closed")
        advance(null)
        assertEquals(xp, state.playerXp)
        assertEquals(1, quantity("cyano_coolant_injector"))
    }

    @Test fun rylanFavorAndTaliaAcknowledgment() = inRoom(
        4, "hub_7_slag_pits", "foundry_lava_tube_bypass", mapOf("heavy_gear" to 1, "hydraulic_fluid" to 1)
    ) {
        talk("Rylan", "forge_hand_rylan_default_talk")
        choose("Need any help around the welder bench?", "rylan_favor_offer")
        advance("forge_hand_rylan_default_talk")
        choose("I brought the heavy gear and hydraulic fluid you needed.", "rylan_favor_turnin")
        advance("forge_hand_rylan_default_talk")
        assertTrue("heat_liner" in state.unlockedArmors)
        assertEquals(0, quantity("heavy_gear"))
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "rylan_opt_worker" })
        close("I should get going.")
        compose.runOnIdle { services.sessionStore.completeQuest("w4_sq17") }
        talk("Rylan", "forge_hand_rylan_default_talk")
        choose("I found Talia's worker tablet near the cooling pool.", "rylan_worker_acknowledged")
        capture("w4-rylan")
        advance("forge_hand_rylan_default_talk")
        assertEquals(1, quantity("composite_plate"))
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id in setOf("rylan_opt_worker", "rylan_opt_favor_turnin") })
        close("I should get going.")
    }

    @Test fun charlesRequiresPublicQueueEvenWithMaterials() = inRoom(
        5, "hub_9_orbital_ring", "orbital_customs_lounge", mapOf("circuit_board" to 2, "focusing_lens" to 1)
    ) {
        talk("Charles", "analyst_charles_default_talk")
        choose("Why help someone breaking through Dominion customs?", "charles_ethics_reply")
        advance("analyst_charles_default_talk")
        choose("Could your analytics help tune my tactical heuristics?", "charles_favor_offer")
        advance("analyst_charles_default_talk")
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id == "charles_opt_favor_turnin" })
        choose("What do you need me to check at the gate?", "charles_favor_status")
        capture("w5-charles")
        advance("analyst_charles_default_talk")
        close("I should get going.")
        roomAction("w5_charles_verify_public_queue")
        assertTrue("ms_charles_public_queue_verified" in state.completedMilestones)
        talk("Charles", "analyst_charles_default_talk")
        choose("Here are the boards and lens for the tap.", "charles_favor_turnin")
        advance("analyst_charles_default_talk")
        assertEquals(1, quantity("charles_heuristic_slate"))
        assertEquals(0, quantity("circuit_board"))
        assertEquals(0, quantity("focusing_lens"))
        assertFalse(vm.uiState.value.dialogueChoices.any { it.id.startsWith("charles_opt_favor_") })
        close("I should get going.")
    }

    @Test fun memoryRefusalCanBeLeftAndReopened() = inRoom(6, "hub_12_singularity", "source_memory_stair") {
        compose.runOnIdle { services.sessionStore.startQuest("w6_mq29") }
        val refusals = mapOf(
            "jed" to "Let go of the latch. “I miss him. Leave him as he was.”",
            "astra" to "Keep the scarred Astra and the crew who flew her.",
            "foundry" to "“We go back with you.” Let Gh0st turn away."
        )
        for ((memory, response) in refusals) {
            val scene = "w6_mq29_refuse_${memory}_revision"
            val receipt = "ms_w6_refused_${memory}_revision"
            repeat(2) { attempt ->
                roomAction("${scene}_scene")
                awaitLine("${scene}_line_1")
                for (line in 2..4) advance("${scene}_line_$line")
                if (attempt == 0) {
                    close("Step away for now.")
                    assertFalse(receipt in state.completedMilestones)
                } else {
                    if (memory == "jed") capture("w6-jeds-door")
                    close(response)
                    assertTrue(receipt in state.completedMilestones)
                }
            }
            roomAction("${scene}_scene")
            awaitLine(null)
        }
    }

    private fun inRoom(world: Int, hub: String, room: String, inventory: Map<String, Int> = emptyMap(), block: () -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.runOnUiThread {
            services = AppServices(IsolatedProgressionContext(context), isTestSession = true)
            services.sessionStore.restore(GameSessionState(worldId = "world_$world", hubId = hub, roomId = room,
                playerId = "nova", partyMembers = listOf("nova"), inventory = inventory))
            services.inventoryService.restore(inventory)
            vm = ExplorationViewModelFactory(services).create(ExplorationViewModel::class.java)
        }
        try {
            compose.waitUntil(15_000) { vm.uiState.value.currentRoom?.id == room && !vm.uiState.value.isLoading }
            compose.setContent {
                val ui by vm.uiState.collectAsState()
                StarbornTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        ui.activeDialogue?.let { dialogue ->
                            DialogueOverlay(dialogue, ui.dialogueChoices, vm::advanceDialogue, vm::onDialogueChoiceSelected,
                                onPlayVoice = {}, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
            block()
        } finally {
            compose.runOnUiThread { vm.runtime.close(); services.release() }
        }
    }

    private fun quantity(id: String) = services.inventoryService.snapshot()[id] ?: 0

    private fun roomAction(event: String) {
        compose.runOnIdle {
            val action = vm.uiState.value.actions.filterIsInstance<GenericAction>().single { it.actionEvent == event }
            vm.onActionSelected(action)
        }
    }

    private fun talk(name: String, id: String) {
        compose.runOnIdle { vm.onNpcInteraction(name) }
        awaitLine(id)
    }

    private fun advance(next: String?) {
        reveal()
        compose.onNodeWithTag("dialogue-advance").performTouchInput { click(Offset(12f, 12f)) }
        awaitLine(next)
    }

    private fun choose(label: String, next: String?) {
        reveal()
        compose.onNodeWithText(label.removeSurrounding("[", "]")).performScrollTo().assertIsDisplayed()
            .performTouchInput { click() }
        awaitLine(next)
    }

    private fun close(label: String) = choose(label, null)

    private fun reveal() {
        val surface = compose.onNodeWithTag("dialogue-advance")
        if (surface.isDisplayed()) surface.performTouchInput { click(Offset(12f, 12f)) }
    }

    private fun awaitLine(id: String?) {
        compose.waitUntil(5_000) { vm.uiState.value.activeDialogue?.line?.id == id }
    }

    private fun capture(name: String) {
        reveal()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.cacheDir, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
