package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.event.*
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Worlds36SafetyTest {
    @Test fun arenaRequiresOrderedRoundsAndAllowsRetryWithoutCrossCredit() {
        val adapter=MoshiProvider.instance.adapter<List<GameEvent>>(Types.newParameterizedType(List::class.java,GameEvent::class.java))
        val store=GameSessionStore()
        val events=EventManager(requireNotNull(adapter.fromJson(File("src/main/assets/events.json").readText())),store)
        fun start(n:Int)=events.handleTrigger("player_action",EventPayload.Action("w4_crucible_start_round_$n"))
        fun win(ids:List<String>)=events.handleTrigger("encounter_victory",EventPayload.EncounterOutcome(ids,EventPayload.EncounterOutcome.Outcome.VICTORY,"foundry_crucible_arena"))
        start(3);assertFalse("ms_crucible_round3_started" in store.state.value.completedMilestones)
        start(1)
        assertTrue(events.canHandleTrigger("player_action",EventPayload.Action("w4_crucible_start_round_1")))
        win(listOf("magma_drone","magma_drone","welder_bot"))
        assertFalse("ms_crucible_round2_cleared" in store.state.value.completedMilestones)
        assertFalse("ms_crucible_round3_cleared" in store.state.value.completedMilestones)
        start(2);win(listOf("slag_golem","welder_bot"))
        assertTrue("ms_crucible_round2_cleared" in store.state.value.completedMilestones)
        assertFalse("ms_crucible_round3_cleared" in store.state.value.completedMilestones)
        start(3);win(listOf("crucible_champion","magma_drone"))
        assertTrue("ms_crucible_round3_cleared" in store.state.value.completedMilestones)
        assertFalse(events.canHandleTrigger("player_action",EventPayload.Action("w4_crucible_start_round_3")))
    }
}
