package com.example.starborn.domain.dialogue

import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.DialogueOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueServiceTest {

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
