package com.example.starborn.feature.exploration

import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeRuntimeRulesTest {
    @Test
    fun stateAndMilestoneVisibilityHandlesMissingFalseTrueAndPersistedValues() {
        val action = mapOf<String, Any?>(
            "show_when_state" to "hunter_confronted",
            "hide_when_state" to "beast_defeated",
            "show_when_milestone" to "ms_w2_mq03_complete"
        )

        assertFalse(narrativeActionVisible(action, emptyMap(), emptyMap(), emptySet()))
        assertFalse(
            narrativeActionVisible(
                action,
                mapOf("hunter_confronted" to false, "beast_defeated" to false),
                emptyMap(),
                setOf("ms_w2_mq03_complete")
            )
        )
        assertTrue(
            narrativeActionVisible(
                action,
                mapOf("hunter_confronted" to false, "beast_defeated" to false),
                mapOf("hunter_confronted" to true),
                setOf("ms_w2_mq03_complete")
            )
        )
        assertFalse(
            narrativeActionVisible(
                action,
                mapOf("hunter_confronted" to true, "beast_defeated" to false),
                mapOf("beast_defeated" to true),
                setOf("ms_w2_mq03_complete")
            )
        )
    }

    @Test
    fun milestoneNegationHidesExhaustedActions() {
        val action = mapOf<String, Any?>(
            "name" to "tool case",
            "type" to "generic",
            "requires_milestone_not_set" to "ms_w1_jed_bunk_tools_looted"
        )

        assertTrue(narrativeActionVisible(action, emptyMap(), emptyMap(), emptySet()))
        assertFalse(narrativeActionVisible(action, emptyMap(), emptyMap(), setOf("ms_w1_jed_bunk_tools_looted")))
    }
}
