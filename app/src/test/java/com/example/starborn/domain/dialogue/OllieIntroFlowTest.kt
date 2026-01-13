package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OllieIntroFlowTest {

    @Test
    fun ollieIntroHappensInTwoPhases() {
        val sessionStore = GameSessionStore()
        val eventManager = EventManager(
            events = loadEvents(),
            sessionStore = sessionStore,
            eventHooks = EventHooks()
        )
        val dialogueService = DialogueService(
            loadDialogue(),
            DialogueConditionEvaluator { condition ->
                conditionMet(condition, sessionStore.state.value)
            },
            DialogueTriggerHandler { trigger ->
                val actions = DialogueTriggerParser.parse(trigger)
                eventManager.performActions(actions)
            }
        )

        // Enter Main Road triggers the intro (cinematic fallback starts Ollie's intro dialogue).
        eventManager.handleTrigger("enter_room", com.example.starborn.domain.event.EventPayload.EnterRoom("town_8"))

        val introSession = dialogueService.startDialogue("Ollie")
        assertEquals("ollie_intro_1", introSession?.current()?.id)

        introSession?.advance() // move to second line
        introSession?.advance() // fire triggers on second line

        val afterIntro = sessionStore.state.value
        assertTrue(afterIntro.completedMilestones.contains("ms_ollie_met"))
        assertTrue(afterIntro.activeQuests.contains("talk_to_jed"))
        assertFalse(afterIntro.partyMembers.contains("ollie"))

        val followUpSession = dialogueService.startDialogue("Ollie")
        assertEquals("ollie_quest_prompt", followUpSession?.current()?.id)

        followUpSession?.advance()
        followUpSession?.advance()

        val afterRecruit = sessionStore.state.value
        assertTrue(afterRecruit.completedMilestones.contains("ms_ollie_recruited"))
        assertTrue(afterRecruit.partyMembers.contains("ollie"))
    }

    private fun conditionMet(raw: String?, state: GameSessionState): Boolean {
        if (raw.isNullOrBlank()) return true
        val tokens = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        for (token in tokens) {
            val parts = token.split(':', limit = 2)
            val type = parts[0].trim().lowercase()
            val value = parts.getOrNull(1)?.trim().orEmpty()
            val met = when (type) {
                "milestone" -> value in state.completedMilestones
                "milestone_not_set" -> value !in state.completedMilestones
                else -> true
            }
            if (!met) return false
        }
        return true
    }

    private fun loadDialogue(): List<DialogueLine> {
        val file = File("src/main/assets/dialogue.json")
        val moshi = MoshiProvider.instance
        val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
        val adapter = moshi.adapter<List<DialogueLine>>(type)
        return requireNotNull(adapter.fromJson(file.readText()))
    }

    private fun loadEvents() = run {
        val file = File("src/main/assets/events.json")
        val moshi = MoshiProvider.instance
        val type = Types.newParameterizedType(List::class.java, com.example.starborn.domain.model.GameEvent::class.java)
        val adapter = moshi.adapter<List<com.example.starborn.domain.model.GameEvent>>(type)
        requireNotNull(adapter.fromJson(file.readText()))
    }
}
