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
import java.io.File
import com.example.starborn.domain.fishing.FishingData
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.model.FirstAidRecipe

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
        val firstAid = readList("src/main/assets/recipes_firstaid.json", FirstAidRecipe::class.java)
        val fishing = readObject("src/main/assets/recipes_fishing.json", FishingData::class.java)
        assertTrue(tinkering.isNotEmpty())
        assertTrue(firstAid.isNotEmpty())
        assertTrue(fishing.zones.containsKey("sector9_stream"))
        assertTrue(fishing.zones.containsKey("spire_runoff"))
        assertTrue(fishing.zones.containsKey("orbital_false_tide"))
        assertTrue(fishing.rods.any { it.id == "reinforced_resin_rod" })
        assertTrue(fishing.rods.any { it.id == "harmonic_carbon_rod" })
        assertTrue(fishing.lures.any { it.id == "harmonic_spool_lure" })
        assertTrue(fishing.rods.isNotEmpty())
        assertTrue(fishing.lures.isNotEmpty())
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

private fun <T> readObject(path: String, clazz: Class<T>): T {
    val file = File(path)
    require(file.exists()) { "$path not found" }
    val moshi = MoshiProvider.instance
    val adapter = moshi.adapter(clazz)
    return requireNotNull(adapter.fromJson(file.readText()))
}
