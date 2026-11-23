package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.TuningPuzzle
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.exploration.presentation.resolveRoomDescription
import com.example.starborn.shared.puzzle.ChronoCoilTrace
import com.example.starborn.shared.puzzle.SignalDial
import org.junit.Assert.*
import org.junit.Test

class PuzzlePolishVerificationTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val dialogue = reader.readList<DialogueLine>("dialogue.json").associateBy { it.id }
    private val events = reader.readList<GameEvent>("events.json")
    private val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
    private val puzzles = reader.readList<TuningPuzzle>("tuning_puzzles.json").associateBy { it.id }

    @Test fun salonStasisAndSourceGateAllowInPuzzleClueReviewWithoutExiting() {
        val checks = listOf(
            "w3_case_alibi_question" to "review_evidence",
            "w3_case_physical_question" to "review_evidence",
            "w2_stasis_1" to "review_diagram",
            "w2_stasis_2" to "review_diagram",
            "w2_stasis_3" to "review_diagram",
            "w2_gate_1" to "review_map",
            "w2_gate_2" to "review_map"
        )
        checks.forEach { (nodeId, optId) ->
            val node = dialogue.getValue(nodeId)
            val reviewOpt = node.options.orEmpty().single { it.id == optId }
            val reviewNode = dialogue.getValue(requireNotNull(reviewOpt.next))
            assertEquals("$nodeId review loops back to decision", nodeId, reviewNode.next)
            assertTrue(reviewNode.text.isNotBlank())
        }
    }

    @Test fun worldFiveFirewallsFormOneConnectedCoolantBusWithDynamicRoomConsequences() {
        // Dynamic room descriptions show downstream load shifts while keeping inline clickable links
        val betaAfterAlpha = requireNotNull(resolveRoomDescription(
            rooms.getValue("deep_firewall_beta"), emptyMap(), setOf("ms_w5_firewall_alpha_down"), false
        ))
        assertTrue(betaAfterAlpha.contains("firewall node beta", ignoreCase = true))
        assertTrue(betaAfterAlpha.contains("Alpha's bled cipher", ignoreCase = true))

        val gammaAfterBeta = requireNotNull(resolveRoomDescription(
            rooms.getValue("deep_firewall_gamma"), emptyMap(), setOf("ms_w5_firewall_beta_down"), false
        ))
        assertTrue(gammaAfterBeta.contains("firewall node gamma", ignoreCase = true))
        assertTrue(gammaAfterBeta.contains("capacitor", ignoreCase = true))

        // Early inspection of Beta before Alpha explains why Alpha must be shunted first
        val dialogues = mutableListOf<String>()
        val messages = mutableListOf<String>()
        val store = GameSessionStore().apply {
            startQuest("w5_mq23")
            setQuestTaskCompleted("w5_mq23", "navigate_maze", true)
        }
        val manager = EventManager(events, store, EventHooks(
            onStartDialogue = { dialogues += it },
            onMessage = { msg -> messages += msg }
        ))
        manager.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_beta_console"))
        assertTrue(dialogues.isEmpty())
        assertTrue(messages.any { it.contains("Alpha", ignoreCase = true) })

        // Alpha console opens routing prompt with bus map review and correct trigger
        manager.handleTrigger("player_action", EventPayload.Action("w5_mq23_firewall_alpha_console"))
        assertEquals(listOf("line:w5_firewall_alpha_prompt"), dialogues)

        listOf("alpha", "beta", "gamma").forEach { node ->
            val prompt = dialogue.getValue("w5_firewall_${node}_prompt")
            val mapOpt = prompt.options.orEmpty().single { it.id == "review_map" }
            assertEquals("w5_firewall_${node}_prompt", dialogue.getValue(requireNotNull(mapOpt.next)).next)
            val correct = prompt.options.orEmpty().single { it.id == "correct" }
            assertEquals("player_action:w5_mq23_firewall_$node", correct.trigger)
        }
    }

    @Test fun chronoCoilUsesCoupledTraceAndEveryTuningPuzzleHasCustomPresentation() {
        assertTrue("All tuning puzzles must have bespoke presentations",
            puzzles.values.all { it.presentation.isNotBlank() && it.presentation != "sliders" })

        val chrono = puzzles.getValue("w3_chrono_flux_tune")
        assertEquals("chrono_trace", chrono.presentation)

        fun buildTrace(damp: Float, drift: Float, dil: Float) = ChronoCoilTrace(
            chrono.sliders.map { s ->
                val v = when (s.id) {
                    "damping" -> damp
                    "pitch" -> drift
                    else -> dil
                }
                SignalDial(s.id, v, s.min, s.max, s.target, s.tolerance)
            }
        )

        // Damping off-target pulls visible carrier offset even when drift slider is on-target
        val dampingPull = buildTrace(damp = 85f, drift = 108f, dil = 240f)
        assertFalse(dampingPull.carrierStable)
        assertTrue("Off-target damping must shift effectiveDriftOffset", dampingPull.effectiveDriftOffset > 0.25f)

        // Drift off-target injects jitter even when damping slider is on-target
        val driftJitter = buildTrace(damp = 45f, drift = 160f, dil = 240f)
        assertFalse(driftJitter.carrierStable)
        assertTrue("Off-target drift must add coupled jitter", driftJitter.jitter > 0.25f)

        // All aligned locks cleanly
        val locked = buildTrace(damp = 45f, drift = 108f, dil = 240f)
        assertTrue(locked.ready)
        assertEquals(0f, locked.effectiveDriftOffset, 0.001f)
        assertEquals(0f, locked.jitter, 0.001f)
        assertEquals(0f, locked.phaseOffset, 0.001f)
    }

    @Test fun magmaRegulatorIsShortEnvironmentalSequencePreservingThermalAegisReward() {
        assertFalse("Duplicate slider puzzle removed", "w4_magma_conduit_tune" in puzzles)
        val action = rooms.getValue("foundry_cooling_springs").actions.single { it["name"] == "magma regulator" }
        assertEquals("w4_magma_regulator_inspect", action["action_event"])

        val step1 = dialogue.getValue("w4_magma_regulator_prompt")
        val step2 = dialogue.getValue(requireNotNull(step1.options.orEmpty().single { it.id == "bleed" }.next))
        assertEquals("player_action:w4_relic_magma_solved", step2.options.orEmpty().single { it.id == "seat_seal" }.trigger)

        val granted = mutableListOf<String>()
        val store = GameSessionStore()
        val manager = EventManager(events, store, EventHooks(onGiveItem = { id, _ -> granted += id }))
        manager.handleTrigger("player_action", EventPayload.Action("w4_relic_magma_solved"))
        assertEquals(listOf("relic_mod_thermal_aegis"), granted)
        assertTrue("ms_relic_w4_magma_solved" in store.state.value.completedMilestones)
    }
}
