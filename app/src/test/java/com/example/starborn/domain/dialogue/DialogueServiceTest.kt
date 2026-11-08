package com.example.starborn.domain.dialogue

import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.DialogueOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import com.example.starborn.core.MoshiProvider
import com.squareup.moshi.Types
import java.io.File
import org.junit.Test

class DialogueServiceTest {

    @Test fun responseMenusHaveAFinalExitWhileLinearDialogueCannotBeCanceled() {
        var triggers = 0
        val root = DialogueLine("question", "Guide", "What do you need?", trigger = "give_xp:99",
            options = listOf(DialogueOption("answer", "Tell me more.", next = "reply")))
        val reply = DialogueLine("reply", "Guide", "Here is the answer.")
        val session = DialogueSession(mapOf(root.id to root, reply.id to reply), root,
            DialogueConditionEvaluator { true }, DialogueTriggerHandler { triggers++ })
        assertEquals(DialogueSession.RESPONSE_EXIT_ID, session.choices().last().id)
        session.choose(DialogueSession.RESPONSE_EXIT_ID)
        assertTrue(session.isFinished())
        assertEquals(0, triggers)
        val linear = DialogueSession(mapOf(reply.id to reply), reply, DialogueConditionEvaluator { true }, DialogueTriggerHandler { triggers++ })
        assertTrue(linear.choices().isEmpty())
        assertEquals(reply, linear.choose(DialogueSession.RESPONSE_EXIT_ID))
        assertEquals(0, triggers)
    }

    @Test
    fun `every authored dialogue graph has a route to an ending`() {
        val assets = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }
        val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
        val lines = requireNotNull(MoshiProvider.instance.adapter<List<DialogueLine>>(type)
            .fromJson(File(assets, "dialogue.json").readText()))
        val terminating = mutableSetOf<String>()
        do {
            val previous = terminating.size
            lines.forEach { line ->
                val targets = line.options?.takeIf { it.isNotEmpty() }?.map { it.next } ?: listOf(line.next)
                if (targets.any { it == null || it in terminating }) terminating += line.id
            }
        } while (previous != terminating.size)
        assertEquals("Dialogue graphs without an exit: ${lines.filter { it.id !in terminating }.map { it.id }}",
            lines.size, terminating.size)
    }

    @Test
    fun `resident looping conversations can leave before and after receiving gifts`() {
        val assets = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.exists() }
        val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
        val lines = requireNotNull(MoshiProvider.instance.adapter<List<DialogueLine>>(type)
            .fromJson(File(assets, "dialogue.json").readText()))
        for (who in listOf("evelyn", "robert", "cheno", "chris", "andrew", "creator_jason", "oliver", "meeko")) {
            val root = lines.single { it.id == "${who}_default_talk" }
            for (giftReceived in listOf(false, true)) {
                val session = DialogueSession(lines.associateBy { it.id }, root,
                    DialogueConditionEvaluator { condition -> condition.isNullOrBlank() ||
                        (!giftReceived && condition.startsWith("milestone_not_set:")) }, DialogueTriggerHandler { })
                assertTrue("${who} goodbye must remain unconditional", session.choices().any { it.id == "${who}_leave" })
                session.choose("${who}_leave")
                assertTrue("${who} conversation should close", session.isFinished())
            }
        }
    }

    @Test
    fun `Charlie can leave at every trust stage and earns his gift only once`() {
        val assets = listOf(File("src/main/assets"), File("app/src/main/assets"))
            .first { it.exists() }
        val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
        val lines = MoshiProvider.instance.adapter<List<DialogueLine>>(type)
            .fromJson(File(assets, "dialogue.json").readText())!!
        val milestones = mutableSetOf<String>()
        var gifts = 0
        var xp = 0
        val service = DialogueService(lines, DialogueConditionEvaluator { condition ->
            condition.isNullOrBlank() || condition.split(',').all {
                val (kind, value) = it.split(':', limit = 2)
                when (kind) {
                    "milestone" -> value in milestones
                    "milestone_not_set" -> value !in milestones
                    else -> error("Unexpected Charlie condition: $it")
                }
            }
        }, DialogueTriggerHandler { trigger ->
            trigger.split(',').forEach {
                val (kind, value) = it.split(':', limit = 2)
                when (kind) {
                    "set_milestone" -> milestones.add(value)
                    "give_item" -> { assertEquals("charlies_comfort_stone", value); gifts++ }
                    "give_xp" -> xp += value.toInt()
                    else -> error("Unexpected Charlie trigger: $it")
                }
            }
        })
        for (stage in 0..3) {
            val leaving = service.startDialogue("Charlie")!!
            assertTrue(leaving.choices().any { it.id == "charlie_opt_leave" })
            leaving.choose("charlie_opt_leave")
            assertTrue(leaving.isFinished())
            assertEquals(stage, milestones.size)
            if (stage < 3) {
                val session = service.startDialogue("Charlie")!!
                session.choose("charlie_opt_step_${stage + 1}")
                session.advance()
                assertEquals("charlie_default_talk", session.current()?.id)
            }
        }
        repeat(2) {
            for (option in listOf("charlie_opt_play", "charlie_opt_cuddle")) {
                val session = service.startDialogue("Charlie")!!
                assertTrue(session.choices().none { it.id.startsWith("charlie_opt_step_") })
                session.choose(option)
                session.advance()
                session.choose("charlie_opt_leave")
                assertTrue(session.isFinished())
            }
        }
        assertEquals(1, gifts)
        assertEquals(120, xp)
    }

    @Test
    fun `scripted comms enter explicitly without replacing NPC greetings or quest dialogue`() {
        val radio = DialogueLine(id = "w1_comms_exit", speaker = "Zeke", text = "Follow the red strobes.",
            portrait = "images/npcs/comms_radio.png", scriptedOnly = true)
        val normal = DialogueLine(id = "zeke_default_talk", speaker = "Zeke", text = "Hello")
        val service = DialogueService(listOf(radio, normal), DialogueConditionEvaluator { true }, DialogueTriggerHandler {})
        assertEquals(normal, service.startDialogue("Zeke")?.current())
        val session = service.startDialogueAt(radio.id)
        assertEquals(radio, session?.current())
        session?.advance()
        assertTrue(session?.isFinished() == true)
        assertEquals(normal, service.startDialogue("Zeke")?.current())
    }

    @Test
    fun `eligible active quest dialogue precedes older optional offers`() {
        val lines = listOf(
            DialogueLine(id = "offer", speaker = "Zeke", text = "Optional quest", condition = "quest_not_started:side"),
            DialogueLine(id = "blocked", speaker = "Zeke", text = "Later", condition = "quest_active:later"),
            DialogueLine(id = "progress", speaker = "Zeke", text = "Continue", condition = "quest_active:current"),
            DialogueLine(id = "fallback", speaker = "Zeke", text = "Hello")
        )
        var active = true
        val service = DialogueService(lines, DialogueConditionEvaluator {
            when (it) {
                "quest_active:later" -> false
                "quest_active:current" -> active
                else -> true
            }
        }, DialogueTriggerHandler { })
        assertEquals("progress", service.startDialogue("Zeke")?.current()?.id)
        active = false
        assertEquals("offer", service.startDialogue("Zeke")?.current()?.id)
    }

    @Test
    fun `choices gate progression until selected`() {
        val lines = listOf(
            DialogueLine(
                id = "start",
                speaker = "NPC",
                text = "Choose your path",
                options = listOf(
                    DialogueOption(
                        id = "opt_a",
                        text = "Option A",
                        next = "after_a"
                    )
                )
            ),
            DialogueLine(
                id = "after_a",
                speaker = "NPC",
                text = "You chose wisely."
            )
        )

        val service = DialogueService(
            dialogueLines = lines,
            conditionEvaluator = DialogueConditionEvaluator { true },
            triggerHandler = DialogueTriggerHandler { }
        )

        val session = service.startDialogue("npc")
        assertNotNull(session)

        val initial = session!!.current()
        assertEquals("start", initial?.id)
        assertTrue(session.choices().isNotEmpty())

        // advance should no-op while choices remain
        val stillStart = session.advance()
        assertEquals("start", stillStart?.id)

        val next = session.choose("opt_a")
        assertEquals("after_a", next?.id)
        assertTrue(session.choices().isEmpty())
    }
}
