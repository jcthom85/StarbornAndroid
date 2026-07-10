package com.example.starborn.feature.mainmenu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugScenarioCatalogTest {
    @Test
    fun `scenario ids are unique and searchable fields are populated`() {
        val scenarios = DebugScenarioCatalog.scenarios

        assertEquals(scenarios.size, scenarios.map { it.id }.distinct().size)
        scenarios.forEach { scenario ->
            assertTrue(scenario.id.isNotBlank())
            assertTrue(scenario.title.isNotBlank())
            assertTrue(scenario.description.isNotBlank())
            assertTrue(scenario.worldLabel.isNotBlank())
            assertNotNull(DebugScenarioCatalog.find(scenario.id))
        }
    }

    @Test
    fun `catalog covers every authored hub and each scenario category`() {
        val scenarios = DebugScenarioCatalog.scenarios
        val hubIds = scenarios
            .filter { it.destination == DebugScenarioDestination.HUB && it.id.startsWith("hub_") }
            .map { it.id }
            .toSet()

        assertEquals((1..12).map { "hub_${it}_" }.size, hubIds.size)
        (1..12).forEach { number ->
            assertTrue(hubIds.any { it.startsWith("hub_${number}_") })
        }
        DebugScenarioCategory.entries.forEach { category ->
            assertTrue(scenarios.any { it.category == category })
        }
        listOf("node_progression_w1", "node_progression_w2", "astra_access", "astra_home").forEach { id ->
            val scenario = DebugScenarioCatalog.find(id)
            assertNotNull(scenario)
            assertEquals(DebugScenarioDestination.HUB, scenario?.destination)
        }
        val sourceGate = DebugScenarioCatalog.find("w2_source_gate")
        assertNotNull(sourceGate)
        assertEquals(DebugScenarioDestination.EXPLORATION, sourceGate?.destination)
        listOf(
            "fun_w3_corporate_espionage",
            "fun_w4_quality_control",
            "fun_w5_ghost_shell",
            "fun_w6_hr_record"
        ).forEach { id ->
            assertNotNull(DebugScenarioCatalog.find(id))
        }
    }
}
