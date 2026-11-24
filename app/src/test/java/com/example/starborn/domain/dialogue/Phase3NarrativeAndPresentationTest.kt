package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.Enemy
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase3NarrativeAndPresentationTest {

    private fun resolveAssetsDir(): File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }

    private fun resolveWorldAssetsDir(): File =
        listOf(File("../world_assets/src/main/assets"), File("world_assets/src/main/assets")).first { it.exists() }

    private inline fun <reified T> readJsonList(fileName: String): List<T> {
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        return requireNotNull(
            MoshiProvider.instance.adapter<List<T>>(type)
                .fromJson(File(resolveAssetsDir(), fileName).readText())
        )
    }

    @Test
    fun `cameo dialogues stay diegetic and preserve rewards and exits`() {
        val lines = readJsonList<DialogueLine>("dialogue.json").associateBy { it.id }

        val jasonIds = listOf(
            "creator_jason_default_talk",
            "jason_what_reply",
            "jason_galaxy_reply",
            "jason_finish_reply",
            "jason_passion_reply",
            "jason_resonator_reply"
        )
        val charlesIds = listOf(
            "analyst_charles_default_talk",
            "charles_mmo_reply",
            "charles_critique_reply",
            "charles_frontier_reply",
            "charles_ethics_reply"
        )
        val combinedCopy = (jasonIds + charlesIds)
            .mapNotNull { lines[it] }
            .flatMap { line -> listOf(line.text) + line.options.orEmpty().map { it.text } }
            .joinToString("\n")
            .lowercase()

        val bannedMetaPhrases = listOf(
            "simulated galaxy",
            "try a build",
            "play it again",
            "playing a game",
            "raid group"
        )
        for (phrase in bannedMetaPhrases) {
            assertFalse("Cameo dialogue should not break fourth wall with '$phrase'", combinedCopy.contains(phrase))
        }

        assertTrue(
            lines.getValue("jason_resonator_reply").trigger.orEmpty().contains("give_item:creators_harmonic_resonator")
        )
        assertTrue(
            lines.getValue("charles_favor_turnin").trigger.orEmpty().contains("give_item:charles_heuristic_slate")
        )
    }

    @Test
    fun `dialogue catalog has zero raw System speakers and planning and aftermath stay scripted`() {
        val lines = readJsonList<DialogueLine>("dialogue.json")
        val byId = lines.associateBy { it.id }

        val systemLines = lines.filter { it.speaker.equals("System", ignoreCase = true) }.map { it.id }
        assertTrue("No dialogue node should expose raw speaker 'System': $systemLines", systemLines.isEmpty())

        val planning = byId.getValue("w3_mq12_planning_table_dialogue")
        assertEquals("Nova", planning.speaker)
        assertTrue(planning.scriptedOnly)

        val aftermath = byId.getValue("vance_security_aftermath")
        assertEquals("Dr. Lyra", aftermath.speaker)
        assertTrue(aftermath.scriptedOnly)
    }

    @Test
    fun `world 5 airlock hack chains into Vale compliance greeting without hijacking Vale dialogue`() {
        val lines = readJsonList<DialogueLine>("dialogue.json")
        var w5Mq21Active = true
        var w5Mq22Active = false
        val completedTasks = mutableSetOf("force_dock")

        val service = DialogueService(
            dialogueLines = lines,
            conditionEvaluator = DialogueConditionEvaluator { raw ->
                raw.isNullOrBlank() || raw.split(',').all { token ->
                    val (kind, value) = token.trim().split(':', limit = 2)
                    when (kind) {
                        "quest_active" -> (value == "w5_mq21" && w5Mq21Active) || (value == "w5_mq22" && w5Mq22Active)
                        "quest_task_done" -> value.substringAfter(':') in completedTasks
                        "quest_task_not_done" -> value.substringAfter(':') !in completedTasks
                        else -> false
                    }
                }
            },
            triggerHandler = DialogueTriggerHandler { trigger ->
                if (trigger.contains("player_action:w5_mq21_hack_airlock")) {
                    completedTasks += "hack_airlock"
                    w5Mq21Active = false
                    w5Mq22Active = true
                }
            }
        )

        val session = service.startDialogue("Gh0st")
        assertNotNull(session)
        assertEquals("gh0st_w5_mq21_airlock", session?.current()?.id)

        val secondBeat = session?.advance()
        assertNotNull("Expected Vale compliance PA greeting after airlock override", secondBeat)
        assertEquals("vale_w5_compliance_greeting", secondBeat?.id)
        assertEquals("Vale", secondBeat?.speaker)

        assertNull(session?.advance())
        assertTrue(session?.isFinished() == true)
        assertNull("Scripted PA greeting must not become a standalone Vale talk entry", service.startDialogue("Vale"))
    }

    @Test
    fun `silence composite formation aligns anchors left to right around towering core`() {
        val enemies = readJsonList<Enemy>("enemies.json").associateBy { it.id }

        val core = enemies.getValue("ascended_god")
        val voice = enemies.getValue("silence_voice_anchor")
        val memory = enemies.getValue("silence_memory_anchor")

        val ordered = listOf(core, voice, memory).sortedBy { it.composite?.offsetX ?: 0f }.map { it.id }
        assertEquals(listOf("silence_voice_anchor", "ascended_god", "silence_memory_anchor"), ordered)
        assertTrue("Silence core should have commanding scale", (core.composite?.widthScale ?: 0f) >= 1.6f)
    }

    @Test
    fun `all playable party members have full reactive combat emote set`() {
        val emotesDir = File(resolveWorldAssetsDir(), "images/characters/emotes")
        val members = listOf("nova", "zeke", "orion", "gh0st")
        val requiredEmotes = listOf("down", "cool", "confident", "angry", "gasping", "happy", "surprised", "worried")

        for (member in members) {
            for (emote in requiredEmotes) {
                val file = File(emotesDir, "${member}_${emote}.png")
                assertTrue("Missing reactive combat emote: ${file.path}", file.exists() && file.length() > 1024L)
            }
        }
    }
}
