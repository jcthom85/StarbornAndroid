package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.exploration.presentation.resolveRoomDescription
import org.junit.Assert.*
import org.junit.Test

class World3ReadinessRegressionTest {
    private class Harness {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val store = GameSessionStore()
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        val inventory = InventoryService(object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        })
        val events = reader.readList<GameEvent>("events.json")
        val started = mutableListOf<String>()
        val messages = mutableListOf<String>()
        val manager = EventManager(events, store, EventHooks(
            onStartDialogue = { started += it }, onMessage = { messages += it },
            onQuestTaskUpdated = { q, t -> if (q != null && t != null) store.setQuestTaskCompleted(q, t, true) },
            onQuestStageAdvanced = { q, stage -> if (q != null && stage != null) store.setQuestStage(q, stage) },
            onGiveItem = { id, n -> inventory.addItem(id, n) }, onGiveXp = store::addXp
        ))
        val dialogue = DialogueService(reader.readList<DialogueLine>("dialogue.json"),
            { isDialogueConditionMet(it, store.state.value, inventory) },
            { manager.performActions(DialogueTriggerParser.parse(it)) })
        fun act(id: String) = manager.handleTrigger("player_action", EventPayload.Action(id))
        fun requestedDialogue(): DialogueSession = requireNotNull(dialogue.startDialogueAt(started.last().removePrefix("line:")))
        fun prismReady() {
            store.startQuest("w3_mq14")
            store.setQuestTaskCompleted("w3_mq14", "enter_archive", true)
            listOf("w3_mq14_read_containment_field", "w3_mq14_read_prism_shutters", "w3_mq14_trace_command_tethers").forEach(::act)
        }
    }

    @Test fun coverStoryAllowsRecoveryAndLeavingBeforeGuestAccess() {
        for (wrong in listOf("cover_emergency", "cover_delivery")) {
            val h = Harness()
            h.store.startQuest("w3_mq13")
            var session = requireNotNull(h.dialogue.startDialogue("Museum Curator"))
            session.advance(); session.advance()
            assertEquals("w3_cover_order", session.current()?.id)
            session.choose(wrong); session.advance()
            assertEquals("w3_cover_recover", session.current()?.id)
            assertFalse("blend_in" in h.store.state.value.questTasksCompleted["w3_mq13"].orEmpty())
            session.choose("w3_cover_recovery_leave")
            assertTrue(session.isFinished())
            h.store.restore(h.store.state.value)
            session = requireNotNull(h.dialogue.startDialogue("Museum Curator"))
            session.advance(); session.advance(); session.choose("correct")
            session.advance(); session.advance()
            assertTrue(session.isFinished())
            assertTrue("blend_in" in h.store.state.value.questTasksCompleted["w3_mq13"].orEmpty())
        }
    }

    @Test fun murderRequiresPlayerDeductionAndWrongClaimsGiveNoCompletionOrRewards() {
        val h = Harness()
        listOf("w3_sq16_inspect_glass", "w3_sq16_inspect_terminal", "w3_sq16_inspect_rig").forEach(h::act)
        var session = requireNotNull(h.dialogue.startDialogue("Director Sorrell"))
        session.choose("vance_confront_evidence")
        for (wrong in listOf("case_coolant_time", "case_drink_time")) {
            session.choose(wrong); session.advance()
            assertEquals("w3_case_alibi_question", session.current()?.id)
            assertFalse("w3_sq16" in h.store.state.value.completedQuests)
            assertTrue(h.inventory.snapshot().isEmpty())
        }
        session.choose("w3_case_alibi_leave")
        assertTrue(session.isFinished())
        h.store.restore(h.store.state.value)
        session = requireNotNull(h.dialogue.startDialogue("Director Sorrell"))
        session.choose("vance_confront_evidence"); session.choose("correct"); session.advance()
        assertEquals("w3_case_physical_question", session.current()?.id)
        for (wrong in listOf("case_badge_again", "case_dispenser_clean")) {
            session.choose(wrong); session.advance()
            assertEquals("w3_case_physical_question", session.current()?.id)
            assertFalse("w3_sq16" in h.store.state.value.completedQuests)
        }
        session.choose("correct")
        session.advance()
        session.advance()
        assertEquals("vance_confession_admission", session.current()?.id)
        session.advance()
        assertEquals("vance_security_aftermath", session.current()?.id)
        session.advance()
        assertTrue(session.isFinished())
        assertTrue("w3_sq16" in h.store.state.value.completedQuests)
        assertEquals(450, h.store.state.value.playerXp)
        assertEquals(1, h.inventory.snapshot()["cyano_coolant_injector"])
        assertFalse("ms_w3_sq16_datapad_inspected" in h.store.state.value.completedMilestones)
        requireNotNull(h.dialogue.startDialogue("Director Sorrell")).advance()
        assertEquals(450, h.store.state.value.playerXp)
    }

    @Test fun ledgerReceiptsRemainBranchSpecificAcrossRestoreAndNeverRepeatRewards() {
        for (branch in listOf("w3_sq14_leak_ledger", "w3_sq14_weaponize_ledger")) {
            val h = Harness()
            h.store.setRoom("spire_service_lift")
            h.store.startQuest("w3_sq14")
            h.store.setMilestone("ms_w3_ledger_stolen")
            h.act("w3_read_ledger_receipts")
            assertTrue(h.messages.last().contains("Neither transmission"))
            h.act(branch)
            val earned = h.store.state.value
            val inventory = h.inventory.snapshot()
            val expected = if (branch == "w3_sq14_leak_ledger") "workers comparing" else "No worker-network publication"
            repeat(2) {
                h.store.restore(h.store.state.value)
                h.act("w3_read_ledger_receipts")
                assertTrue(h.messages.last().contains(expected))
                assertEquals(earned.playerXp, h.store.state.value.playerXp)
                assertEquals(inventory, h.inventory.snapshot())
            }
            h.act(if (branch == "w3_sq14_leak_ledger") "w3_sq14_weaponize_ledger" else "w3_sq14_leak_ledger")
            assertEquals(earned.playerXp, h.store.state.value.playerXp)
            assertEquals(inventory, h.inventory.snapshot())
        }
    }

    @Test fun canonicalNpcNamesOpenConversationsAndPrototypeRequiresAudit() {
        val h = Harness()
        h.store.startQuest("w3_mq13")
        val npcs = h.reader.readList<Npc>("npcs.json").associateBy { it.id }
        assertEquals("curator_w3_mq13_intro", h.dialogue.startDialogue(npcs.getValue("curator").name)?.current()?.id)
        assertEquals("lab_w3_calibration_hint", h.dialogue.startDialogue(npcs.getValue("lab_terminal").name)?.current()?.id)
        h.act("w3_sq15_test_weapon")
        assertFalse("ms_w3_prototype_trial_complete" in h.store.state.value.completedMilestones)
        h.act("w3_sq15_scan_targeting")
        val terminal = requireNotNull(h.dialogue.startDialogue(npcs.getValue("lab_terminal").name))
        terminal.advance()
        assertTrue("ms_w3_prototype_trial_complete" in h.store.state.value.completedMilestones)
    }

    @Test fun marketDialogueCannotSkipInvestigationAndLegacyGridCanStillFinish() {
        val h = Harness()
        h.store.startQuest("w3_sq13")
        assertEquals("mika_w3_trace_hint", h.dialogue.startDialogue("Mika")?.current()?.id)
        h.act("w3_sq13_fix_market_lights")
        assertFalse("ms_w3_market_grid_ready" in h.store.state.value.completedMilestones)
        h.act("w3_sq13_trace_power_theft")
        requireNotNull(h.dialogue.startDialogue("Mika")).choose("mika_w3_sq13_fix")
        assertTrue("ms_w3_market_grid_ready" in h.store.state.value.completedMilestones)
        h.act("w3_sq13_restore_market")
        val xp = h.store.state.value.playerXp
        h.act("w3_sq13_restore_market")
        assertEquals(xp, h.store.state.value.playerXp)
        assertEquals(1, h.inventory.snapshot()["neon_band"])
        val legacy = Harness()
        legacy.store.startQuest("w3_sq13")
        legacy.store.setMilestone("ms_w3_market_grid_ready")
        legacy.act("w3_sq13_restore_market")
        assertTrue("w3_sq13" in legacy.store.state.value.completedQuests)
    }

    @Test fun returnRoutesWorkRepeatedlyIncludingOldCompletedReceipts() {
        val h = Harness()
        val shortcuts = h.events.filter { it.id.startsWith("warp_spire") }
        assertEquals(10, shortcuts.size)
        shortcuts.forEach { event ->
            val destination = event.actions.single { it.type == "warp" }.roomId
            h.store.markEventCompleted(event.id) // Receipt from the formerly one-shot route.
            repeat(2) {
                h.store.setRoom("spire_vent_output_scale_03")
                h.act(requireNotNull(event.trigger.action))
                assertEquals(destination, h.store.state.value.roomId)
                assertTrue(h.manager.canHandleTrigger("player_action", EventPayload.Action(requireNotNull(event.trigger.action))))
            }
        }
    }

    @Test fun roomAndElevatorFeedbackReflectCompletedWork() {
        val h = Harness()
        val room = h.reader.readList<Room>("rooms.json").single { it.id == "spire_laundry_service" }
        val description = resolveRoomDescription(room, emptyMap(), setOf("ms_w3_guest_profile_accepted", "ms_w3_scanners_blinded"), false)!!
        assertTrue(description.contains("scanner grid blind"))
        assertFalse(description.contains("still need"))
        h.store.setMilestone("ms_w3_mq12_complete")
        h.store.setRoom("spire_transit_plaza")
        h.act("w3_mq12_use_elevator")
        assertTrue(h.messages.last().contains("service elevator"))
        assertEquals("spire_laundry_service", h.store.state.value.roomId)
    }

    @Test fun suspectGivesAlibiBeforeEvidenceAndConfessionCannotRepeat() {
        val h = Harness()
        assertEquals("vance_initial_alibi", h.dialogue.startDialogue("Director Sorrell")?.current()?.id)
        listOf("ms_w3_sq16_glass_inspected", "ms_w3_sq16_terminal_inspected", "ms_w3_sq16_rig_inspected").forEach(h.store::setMilestone)
        val vance = requireNotNull(h.dialogue.startDialogue("Director Sorrell"))
        assertEquals("director_vance_default_talk", vance.current()?.id)
        vance.choose("vance_confront_evidence")
        repeat(12) {
            if (!vance.isFinished()) {
                if (vance.choices().isEmpty()) vance.advance() else vance.choose("correct")
            }
        }
        assertTrue(vance.isFinished())
        val xp = h.store.state.value.playerXp
        requireNotNull(h.dialogue.startDialogue("Director Sorrell")).advance()
        assertEquals(xp, h.store.state.value.playerXp)
        assertEquals(1, h.inventory.snapshot()["cyano_coolant_injector"])
        assertEquals("vance_case_closed", h.dialogue.startDialogue("Director Sorrell")?.current()?.id)
    }

    @Test fun prismRequiresCluesWrongRoutesResetAndLeavingAllowsRetryAcrossRestore() {
        val h = Harness()
        h.store.startQuest("w3_mq14")
        h.act("w3_mq14_route_prisms")
        assertTrue(h.started.isEmpty())
        h.prismReady()
        h.act("w3_mq14_route_prisms")
        var controls = h.requestedDialogue()
        controls.choose("correct")
        controls.choose("return")
        assertEquals("w3_prism_return_retry", controls.current()?.id)
        controls.advance()
        assertEquals("w3_prism_route_entry", controls.current()?.id)
        assertFalse("ms_w3_prism_solved" in h.store.state.value.completedMilestones)
        controls.choose("leave")
        assertTrue(controls.isFinished())
        h.store.restore(h.store.state.value)
        h.act("w3_mq14_route_prisms")
        controls = h.requestedDialogue()
        controls.choose("correct"); controls.choose("correct"); controls.choose("cut")
        assertFalse("ms_w3_prism_solved" in h.store.state.value.completedMilestones)
        controls.advance()
        repeat(3) { controls.choose("correct") }
        assertTrue("ms_w3_prism_solved" in h.store.state.value.completedMilestones)
        assertEquals(0, h.inventory.snapshot()["the_lens"] ?: 0)
        h.act("w3_mq14_review_lens")
        val warning = h.requestedDialogue()
        assertEquals("zeke_w3_mq14_lens_warning", warning.current()?.id)
        while (!warning.isFinished()) warning.advance()
        assertTrue("w3_mq14" in h.store.state.value.completedQuests)
        assertEquals("line:thorne_w3_mq14_trap_intro", h.started.last())
        val escalation = h.requestedDialogue()
        val lines = mutableListOf<String>()
        while (!escalation.isFinished()) { lines += escalation.current()!!.id; escalation.advance() }
        assertTrue("vale_w3_mq15_override" in lines)
        val xp = h.store.state.value.playerXp
        h.act("w3_mq14_take_lens")
        assertEquals(xp, h.store.state.value.playerXp)
        assertEquals(1, h.inventory.snapshot()["the_lens"])
    }
}
