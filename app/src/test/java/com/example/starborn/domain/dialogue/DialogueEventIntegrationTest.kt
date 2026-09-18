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

    @Test
    fun dialogueAudioTriggersDispatchToAudioLayerCommandHook() {
        val sessionStore = GameSessionStore()
        var receivedLayer: String? = null
        var receivedCue: String? = null
        var receivedContext: String? = null

        val eventManager = EventManager(
            events = emptyList(),
            sessionStore = sessionStore,
            eventHooks = com.example.starborn.domain.event.EventHooks(
                onAudioLayerCommand = { spec ->
                    receivedLayer = spec.layer
                    receivedCue = spec.cueId
                    receivedContext = spec.context
                }
            )
        )

        val actions = DialogueTriggerParser.parse("music_persist:gf_01_unpayable_debt")
        eventManager.performActions(actions)

        org.junit.Assert.assertEquals("music", receivedLayer)
        org.junit.Assert.assertEquals("gf_01_unpayable_debt", receivedCue)
        org.junit.Assert.assertEquals("persist", receivedContext)
    }
}
