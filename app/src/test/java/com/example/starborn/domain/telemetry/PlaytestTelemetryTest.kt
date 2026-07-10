package com.example.starborn.domain.telemetry

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PlaytestTelemetryTest {
    @Test
    fun recordsQuestDurationCombatRetriesAndFunScoreWithoutNarrativeText() {
        val directory = Files.createTempDirectory("starborn-telemetry").toFile()
        var wallClock = 1_000L
        var elapsed = 50L
        val telemetry = LocalPlaytestTelemetry(
            directory = directory,
            wallClockMs = { wallClock++ },
            elapsedRealtimeMs = { elapsed }
        )

        telemetry.startSession("test")
        telemetry.questStarted("w1_mq01")
        elapsed = 2_550L
        telemetry.questCompleted("w1_mq01")
        val combat = mapOf("room_id" to "workshop_yard", "enemy_ids" to listOf("faulted_loader"))
        telemetry.record("combat_started", combat)
        telemetry.record("combat_completed", combat + ("outcome" to "defeat"))
        telemetry.record("combat_started", combat)
        telemetry.scoreArc(
            FunArcScore(
                arcId = "w1_opening",
                anticipation = 4,
                agency = 5,
                mastery = 4,
                variety = 4,
                payoff = 5,
                momentum = 4,
                findingLabels = setOf("mastery", "payoff")
            )
        )

        val events = directory.resolve("fun-events.jsonl").readLines().map(::JSONObject)
        val questCompleted = events.first { it.getString("event") == "quest_completed" }
        assertEquals(2_500L, questCompleted.getLong("duration_ms"))
        val combatStarts = events.filter { it.getString("event") == "combat_started" }
        assertEquals(1, combatStarts[0].getInt("attempt"))
        assertEquals(2, combatStarts[1].getInt("attempt"))
        val score = events.first { it.getString("event") == "fun_arc_scored" }
        assertEquals(5, score.getInt("agency"))
        assertTrue(events.all { !it.toString().contains("dialogue_text") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsScoresOutsideTheFivePointScale() {
        FunArcScore(
            arcId = "invalid",
            anticipation = 0,
            agency = 1,
            mastery = 1,
            variety = 1,
            payoff = 1,
            momentum = 1
        )
    }
}
