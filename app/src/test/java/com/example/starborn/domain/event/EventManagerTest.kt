package com.example.starborn.domain.event

import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.model.EventTrigger
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventManagerTest {

    @Test
    fun playCinematicExecutesFollowUpActions() {
        val sessionStore = GameSessionStore()
        var cinematicTriggered: String? = null
        var milestoneTriggered: String? = null

        val onCompleteAction = EventAction(
            type = "set_milestone",
            milestone = "ms_test"
        )
        val cinematicAction = EventAction(
            type = "play_cinematic",
            sceneId = "scene_intro",
            onComplete = listOf(onCompleteAction)
        )
        val event = GameEvent(
            id = "evt_test",
            trigger = EventTrigger(type = "custom"),
            repeatable = true,
            actions = listOf(cinematicAction)
        )

        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onPlayCinematic = { sceneId, onComplete ->
                    cinematicTriggered = sceneId
                    onComplete()
                },
                onMilestoneSet = { milestoneTriggered = it }
            )
        )

        manager.handleTrigger("custom")

        assertEquals("scene_intro", cinematicTriggered)
        assertEquals("ms_test", milestoneTriggered)
        assertTrue(sessionStore.state.value.completedMilestones.contains("ms_test"))
    }

    @Test
    fun nonRepeatableEventRunsOnce() {
        val sessionStore = GameSessionStore()
        var messageCount = 0
        val event = GameEvent(
            id = "evt_once",
            trigger = EventTrigger(type = "custom"),
            repeatable = false,
            actions = listOf(
                EventAction(
                    type = "show_message",
                    message = "Hello"
                )
            )
        )
        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onMessage = { messageCount++ }
            )
        )

        manager.handleTrigger("custom")
        manager.handleTrigger("custom")

        assertEquals(1, messageCount)
        assertTrue(sessionStore.state.value.completedEvents.contains("evt_once"))
    }

    @Test
    fun questStageConditionMatches() {
        val sessionStore = GameSessionStore()
        sessionStore.startQuest("quest_intro")
        sessionStore.setQuestStage("quest_intro", "stage_one")
        var triggered = false
        val event = GameEvent(
            id = "evt_stage_gate",
            trigger = EventTrigger(type = "custom"),
            repeatable = true,
            actions = listOf(
                EventAction(type = "show_message", message = "Stage reached")
            ),
            conditions = listOf(
                com.example.starborn.domain.model.EventCondition(
                    type = "quest_stage",
                    questId = "quest_intro",
                    stageId = "stage_one"
                )
            )
        )
        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onMessage = { triggered = true }
            )
        )

        manager.handleTrigger("custom")

        assertTrue("Event should run when quest stage matches", triggered)
    }

    @Test
    fun takeItemDelegatesToHook() {
        val sessionStore = GameSessionStore()
        var removedItemId: String? = null
        var removedQuantity: Int? = null
        val event = GameEvent(
            id = "evt_take_item",
            trigger = EventTrigger(type = "custom"),
            repeatable = true,
            actions = listOf(
                EventAction(
                    type = "take_item",
                    itemId = "quest_item",
                    quantity = 2
                )
            )
        )
        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onTakeItem = { itemId, qty ->
                    removedItemId = itemId
                    removedQuantity = qty
                    true
                }
            )
        )

        manager.handleTrigger("custom")

        assertEquals("quest_item", removedItemId)
        assertEquals(2, removedQuantity)
    }

    @Test
    fun playCinematicTriggersHook() {
        val sessionStore = GameSessionStore()
        var playedScene: String? = null
        var completed = false
        val event = GameEvent(
            id = "evt_cinematic_test",
            trigger = EventTrigger(type = "custom"),
            repeatable = true,
            actions = listOf(
                EventAction(
                    type = "play_cinematic",
                    sceneId = "scene_test",
                    onComplete = listOf(EventAction(type = "set_milestone", milestone = "ms_done"))
                )
            )
        )
        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onPlayCinematic = { sceneId, done ->
                    playedScene = sceneId
                    done()
                },
                onMilestoneSet = {
                    if (it == "ms_done") completed = true
                }
            )
        )

        manager.handleTrigger("custom")

        assertEquals("scene_test", playedScene)
        assertTrue(completed)
        assertTrue(sessionStore.state.value.completedMilestones.contains("ms_done"))
    }

    @Test
    fun audioLayerActionDelegatesToHook() {
        val sessionStore = GameSessionStore()
        var receivedSpec: AudioLayerCommandSpec? = null
        val event = GameEvent(
            id = "evt_audio_layer",
            trigger = EventTrigger(type = "custom"),
            repeatable = true,
            actions = listOf(
                EventAction(
                    type = "audio_layer",
                    audioLayer = "music",
                    audioCueId = "hero_theme",
                    audioGain = 0.5f,
                    audioFadeMs = 750L,
                    audioLoop = true
                )
            )
        )
        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onAudioLayerCommand = { receivedSpec = it }
            )
        )

        manager.handleTrigger("custom")

        val spec = receivedSpec ?: error("AudioLayerCommandSpec was not emitted")
        assertEquals("music", spec.layer)
        assertEquals("hero_theme", spec.cueId)
        assertEquals(0.5f, spec.gain)
        assertEquals(750L, spec.fadeMs)
        assertEquals(true, spec.loop)
        assertEquals(false, spec.stop)
    }

    @Test
    fun systemTutorialActionInvokesHookWithContext() {
        val sessionStore = GameSessionStore()
        var receivedScene: String? = null
        var receivedContext: String? = null
        var completionInvoked = false
        val event = GameEvent(
            id = "evt_system_tutorial",
            trigger = EventTrigger(type = "custom"),
            repeatable = true,
            actions = listOf(
                EventAction(
                    type = "system_tutorial",
                    sceneId = "scene_inventory_basics",
                    context = "inventory"
                )
            )
        )
        val manager = EventManager(
            events = listOf(event),
            sessionStore = sessionStore,
            eventHooks = EventHooks(
                onSystemTutorial = { sceneId, context, done ->
                    receivedScene = sceneId
                    receivedContext = context
                    done()
                    completionInvoked = true
                }
            )
        )

        manager.handleTrigger("custom")

        assertEquals("scene_inventory_basics", receivedScene)
        assertEquals("inventory", receivedContext)
        assertTrue("Completion callback should run", completionInvoked)
    }
}
