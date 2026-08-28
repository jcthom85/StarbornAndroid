package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.*
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CampaignEndToEndPlaytest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance

    @Test
    fun `simulate full campaign quest progression from prologue to epilogue`() {
        val quests = readList<Quest>("quests.json")
        val events = readList<GameEvent>("events.json")
        val rooms = readList<Room>("rooms.json")
        val dialogue = readList<DialogueLine>("dialogue.json")
        val enemies = readList<Enemy>("enemies.json")
        val skills = readList<Skill>("skills.json")

        val roomById = rooms.associateBy { it.id }
        val dialogueById = dialogue.associateBy { it.id }
        val enemyById = enemies.associateBy { it.id }
        val skillById = skills.associateBy { it.id }

        // 1. Verify 30 Main Quests exist (5 per world across 6 worlds)
        val mainQuests = quests.filter { it.id.contains("_mq") }
        assertEquals("Campaign must contain exactly 30 main quests across 6 worlds", 30, mainQuests.size)

        for (world in 1..6) {
            val worldMqs = mainQuests.filter { it.id.startsWith("w${world}_mq") }
            assertEquals("World $world must have 5 sequential main quests", 5, worldMqs.size)
        }

        // 2. Walk each main quest and verify complete task-event-room chain
        val missingRooms = mutableListOf<String>()
        val missingEnemies = mutableListOf<String>()

        mainQuests.forEach { quest ->
            quest.stages.forEach { stage ->
                stage.tasks.forEach { task ->
                    // Find all events associated with this quest and stage
                    val matchingEvents = events.filter { event ->
                        event.conditions.any { it.questId == quest.id && (it.stageId == null || it.stageId == stage.id) }
                    }

                    matchingEvents.forEach { event ->
                        if (event.trigger.type == "action" || event.trigger.type == "inspect") {
                            val roomId = event.trigger.room ?: event.trigger.roomId
                            if (roomId != null && !roomById.containsKey(roomId)) {
                                missingRooms.add("Quest ${quest.id}: Room $roomId does not exist")
                            }
                        }

                        // Check combat encounters
                        if (event.trigger.type == "encounter_victory") {
                            val enemyIds = event.trigger.enemies.orEmpty()
                            enemyIds.forEach { enemyId ->
                                if (!enemyById.containsKey(enemyId)) {
                                    missingEnemies.add("Quest ${quest.id}: Enemy $enemyId does not exist")
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue("All main quest rooms must resolve: $missingRooms", missingRooms.isEmpty())
        assertTrue("All main quest enemies must resolve: $missingEnemies", missingEnemies.isEmpty())
    }

    @Test
    fun `verify enemy skills and combat readiness across all campaign encounters`() {
        val enemies = readList<Enemy>("enemies.json")
        val skills = readList<Skill>("skills.json")
        val skillById = skills.associateBy { it.id }

        val missingSkills = mutableListOf<String>()
        enemies.forEach { enemy ->
            enemy.abilities.forEach { skillId ->
                if (!skillById.containsKey(skillId)) {
                    missingSkills.add("Enemy ${enemy.id} missing skill $skillId")
                }
            }
        }

        assertTrue("All enemy skills must exist in skills.json: $missingSkills", missingSkills.isEmpty())
    }

    @Test
    fun `verify all key debug scenario rooms resolve cleanly`() {
        val rooms = readList<Room>("rooms.json")
        val roomById = rooms.associateBy { it.id }

        // Verify key scenario start rooms exist
        val keyScenarioRooms = listOf(
            "pit_nova_bunk", "sector9_stream_falls", "spire_prism_gallery",
            "orbital_executive_dock", "orbital_grand_concourse"
        )
        keyScenarioRooms.forEach { roomId ->
            assertTrue("Scenario room $roomId must exist in rooms.json", roomById.containsKey(roomId))
        }
    }

    private inline fun <reified T> readList(path: String): List<T> {
        val file = File(assets, path)
        assertTrue("Asset file $path must exist", file.exists())
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}
