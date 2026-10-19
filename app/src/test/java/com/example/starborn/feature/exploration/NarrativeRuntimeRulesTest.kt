package com.example.starborn.feature.exploration

import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeRuntimeRulesTest {
    @Test
    fun authoredBunkPanelIsHiddenUntilLitAndAfterOpening() {
        val assets = listOf(java.io.File("app/src/main/assets"), java.io.File("src/main/assets"))
            .first { it.isDirectory }
        val rooms = org.json.JSONArray(java.io.File(assets, "rooms.json").readText())
        val room = (0 until rooms.length()).map { rooms.getJSONObject(it) }
            .first { it.getString("id") == "pit_nova_bunk" }
        val actions = room.getJSONArray("actions")
        val panel = (0 until actions.length()).map { actions.getJSONObject(it) }
            .first { it.optString("action_event") == "w1_mq01_inspect_safety_fault" }
        val action = panel.keys().asSequence().associateWith { panel.get(it) }
        val initial = mapOf("light_on" to false, "conduit_isolated" to false)
        val revealed = setOf("ms_w1_mq01_fault_visible")
        assertFalse(room.getString("description_dark").contains("door control panel", true))
        assertFalse(room.getString("description_dark").contains("scorched conduit", true))
        assertTrue(room.getString("description_dark").contains("bunk light"))
        assertFalse(narrativeActionVisible(action, initial, emptyMap(), emptySet()))
        assertTrue(narrativeActionVisible(action, initial, mapOf("light_on" to true), revealed))
        assertFalse(narrativeActionVisible(action, initial, mapOf("light_on" to false), revealed))
        assertFalse(narrativeActionVisible(action, initial,
            mapOf("light_on" to true, "conduit_isolated" to true), revealed))
    }

    @Test
    fun bunkPossessionsStayDiscoverableAfterOpeningTheDoorButRemainHiddenInTheDark() {
        val assets = listOf(java.io.File("app/src/main/assets"), java.io.File("src/main/assets")).first { it.isDirectory }
        val rooms = org.json.JSONArray(java.io.File(assets, "rooms.json").readText())
        val room = (0 until rooms.length()).map { rooms.getJSONObject(it) }.first { it.getString("id") == "pit_nova_bunk" }
        val actions = room.getJSONArray("actions")
        for (name in listOf("cargo netting", "rusty locker")) {
            val data = (0 until actions.length()).map { actions.getJSONObject(it) }.single { it.getString("name") == name }
            val action = data.keys().asSequence().associateWith { data.get(it) }
            val initial = mapOf("light_on" to false, "conduit_isolated" to false)
            assertFalse(narrativeActionVisible(action, initial, emptyMap(), emptySet()))
            assertTrue(narrativeActionVisible(action, initial, mapOf("light_on" to true), emptySet()))
            assertTrue(narrativeActionVisible(action, initial, mapOf("light_on" to true, "conduit_isolated" to true), emptySet()))
            assertFalse(narrativeActionVisible(action, initial, mapOf("light_on" to false, "conduit_isolated" to true), emptySet()))
            val variants = room.getJSONArray("description_variants")
            for (index in 0 until variants.length()) assertTrue(variants.getJSONObject(index).getString("description").contains(name, ignoreCase = true))
        }
    }

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
