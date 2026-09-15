package com.example.starborn.feature.mainmenu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugScenarioCatalogTest {
    private val app = listOf(java.io.File("app"), java.io.File(".")).first {
        java.io.File(it, "src/main/assets/quests.json").exists()
    }
    private val bootstrap get() = java.io.File(app,
        "src/main/java/com/example/starborn/di/AppServices.kt").readText()

    @Test
    fun `every menu entry has a launcher or authored hub`() {
        val launches = Regex("\"([^\"]+)\" ->").findAll(bootstrap)
            .map { it.groupValues[1] }.toSet()
        val hubs = org.json.JSONArray(java.io.File(app, "src/main/assets/hubs.json").readText())
        val hubIds = (0 until hubs.length()).map { hubs.getJSONObject(it).getString("id") }.toSet()
        DebugScenarioCatalog.scenarios.forEach {
            assertTrue("Missing launcher: ${it.id}", it.id in launches || it.id in hubIds)
        }
    }

    @Test
    fun `literal seeded rooms and quest stages exist`() {
        val rooms = org.json.JSONArray(java.io.File(app, "src/main/assets/rooms.json").readText())
        val roomIds = (0 until rooms.length()).map { rooms.getJSONObject(it).getString("id") }.toSet()
        Regex("setRoom\\(\"([^\"]+)\"\\)").findAll(bootstrap).forEach {
            assertTrue("Missing seeded room: ${it.groupValues[1]}", it.groupValues[1] in roomIds)
        }
        val quests = org.json.JSONArray(java.io.File(app, "src/main/assets/quests.json").readText())
        val stages = (0 until quests.length()).associate { index ->
            val quest = quests.getJSONObject(index)
            val entries = quest.getJSONArray("stages")
            quest.getString("id") to (0 until entries.length()).map { entries.getJSONObject(it).getString("id") }
        }
        Regex("setQuestStage\\(\"([^\"]+)\", \"([^\"]+)\"\\)").findAll(bootstrap).forEach {
            assertTrue("Invalid seeded stage: ${it.groupValues[1]}/${it.groupValues[2]}",
                it.groupValues[2] in stages[it.groupValues[1]].orEmpty())
        }
    }

    @Test
    fun `literal granted items exist and recipe sandboxes have valid ingredients`() {
        val items = org.json.JSONArray(java.io.File(app, "src/main/assets/items.json").readText())
        val ids = (0 until items.length()).map { items.getJSONObject(it).getString("id") }.toSet()
        Regex("addItem\\(\"([^\"]+)\"").findAll(bootstrap).forEach {
            assertTrue("Missing granted item: ${it.groupValues[1]}", it.groupValues[1] in ids)
        }
        listOf("recipes_cooking.json", "recipes_tinkering.json").forEach { file ->
            val recipes = org.json.JSONArray(java.io.File(app, "src/main/assets/$file").readText())
            (0 until recipes.length()).forEach { index ->
                val recipe = recipes.getJSONObject(index)
                val ingredients = recipe.optJSONObject("ingredients")
                ingredients?.keys()?.forEach { assertTrue("Missing ingredient: $it", it in ids) }
                val tools = recipe.optJSONArray("tools")
                if (tools != null) (0 until tools.length()).forEach { assertTrue(tools.getString(it) in ids) }
            }
        }
    }

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
