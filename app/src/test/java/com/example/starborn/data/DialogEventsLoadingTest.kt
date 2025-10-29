package com.example.starborn.data

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.FirstAidRecipe

@RunWith(RobolectricTestRunner::class)
class DialogEventsLoadingTest {

    @Test
    fun dialogueJsonLoadsAndStartsSession() {
        val store = GameSessionStore()
        val dialogue = readDialogue()
        assertTrue("Dialogue list should not be empty", dialogue.isNotEmpty())

        val service = DialogueService(
            dialogue,
            DialogueConditionEvaluator { true },
            DialogueTriggerHandler { trigger ->
                // basic trigger handling should update quest state
                if (trigger.startsWith("start_quest:")) {
                    val questId = trigger.substringAfter(':')
                    store.startQuest(questId)
                }
            }
        )

        val session = service.startDialogue("Jed")
        assertNotNull("Expected dialogue to start for Jed", session)
        assertNotNull("Expected first dialogue line", session?.current())
    }

    @Test
    fun eventsJsonLoadsAndRunsSimpleTrigger() {
        val store = GameSessionStore()
        val events = readEvents()
        assertTrue("Event list should not be empty", events.isNotEmpty())

        val manager = EventManager(
            events = events,
            sessionStore = store,
            eventHooks = EventHooks()
        )

        manager.handleTrigger("talk_to", EventPayload.TalkTo("jed"))
        // verifying that quest state remains internally consistent
        val state = store.state.value
        assertEquals(state.completedQuests.intersect(state.activeQuests).isEmpty(), true)
    }

    @Test
    fun craftingRecipesLoad() {
        val tinkering = readList("src/main/assets/recipes_tinkering.json", TinkeringRecipe::class.java)
        val cooking = readList("src/main/assets/recipes_cooking.json", CookingRecipe::class.java)
        val firstAid = readList("src/main/assets/recipes_firstaid.json", FirstAidRecipe::class.java)
        assertTrue(tinkering.isNotEmpty())
        assertTrue(cooking.isNotEmpty())
        assertTrue(firstAid.isNotEmpty())
    }
}

private fun readDialogue(): List<DialogueLine> {
    val file = File("src/main/assets/dialogue.json")
    require(file.exists()) { "dialogue.json not found" }
    val moshi = MoshiProvider.instance
    val adapter = moshi.adapter<List<DialogueLine>>(Types.newParameterizedType(List::class.java, DialogueLine::class.java))
    return requireNotNull(adapter.fromJson(file.readText()))
}

private fun readEvents(): List<GameEvent> {
    val file = File("src/main/assets/events.json")
    require(file.exists()) { "events.json not found" }
    val moshi = MoshiProvider.instance
    val adapter = moshi.adapter<List<GameEvent>>(Types.newParameterizedType(List::class.java, GameEvent::class.java))
    return requireNotNull(adapter.fromJson(file.readText()))
}

private fun <T> readList(path: String, clazz: Class<T>): List<T> {
    val file = File(path)
    require(file.exists()) { "$path not found" }
    val moshi = MoshiProvider.instance
    val adapter = moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, clazz))
    return requireNotNull(adapter.fromJson(file.readText()))
}
