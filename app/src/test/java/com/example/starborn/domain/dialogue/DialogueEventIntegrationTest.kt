package com.example.starborn.domain.dialogue

import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueEventIntegrationTest {

    @Test
    fun dialogueTriggersUpdateSessionStateThroughEventManager() {
        val sessionStore = GameSessionStore()
        val eventManager = EventManager(events = emptyList(), sessionStore = sessionStore)

        val actions = DialogueTriggerParser.parse("start_quest:gather_broken_gear,set_milestone:ms_intro")
        eventManager.performActions(actions)

        val state = sessionStore.state.value
        assertTrue(state.activeQuests.contains("gather_broken_gear"))
        assertTrue(state.completedMilestones.contains("ms_intro"))
    }
}
