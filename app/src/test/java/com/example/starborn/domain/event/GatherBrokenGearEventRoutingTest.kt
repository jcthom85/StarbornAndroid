package com.example.starborn.domain.event

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatherBrokenGearEventRoutingTest {

    @Test
    fun collectEventsOnlyFireForTheirNpcAfterDialogueClose() {
        val events = readEvents()
            .filter { it.id in setOf("evt_collect_display_from_ellie", "evt_collect_grinder_from_maddie", "evt_collect_projector_from_kasey") }
        assertEquals("Expected exactly the three gather_broken_gear collect events", 3, events.size)

        data class Result(
            val items: List<String>,
            val tasks: List<String>,
            val milestones: List<String>
        )

        fun runTrigger(type: String, npc: String, repeats: Int = 1): Result {
            val store = GameSessionStore().apply { startQuest("gather_broken_gear") }
            val givenItems = mutableListOf<String>()
            val completedTasks = mutableListOf<String>()
            val milestonesSet = mutableListOf<String>()
            val manager = EventManager(
                events = events,
                sessionStore = store,
                eventHooks = EventHooks(
                    onPlayCinematic = { _, done -> done() },
                    onGiveItem = { itemId, quantity -> givenItems += "$itemId:$quantity" },
                    onQuestTaskUpdated = { questId, taskId ->
                        if (questId == "gather_broken_gear" && !taskId.isNullOrBlank()) {
                            completedTasks += taskId
                        }
                    },
                    onMilestoneSet = { milestone -> milestonesSet += milestone }
                )
            )

            repeat(repeats.coerceAtLeast(1)) {
                manager.handleTrigger(type, EventPayload.TalkTo(npc))
            }
            return Result(items = givenItems, tasks = completedTasks, milestones = milestonesSet)
        }

        runTrigger("talk_to", "Ellie", repeats = 1).also { (items, tasks, milestones) ->
            assertTrue("Talking to Ellie should not grant the item until dialogue closes", items.isEmpty())
            assertTrue("Talking to Ellie should not complete the objective until dialogue closes", tasks.isEmpty())
            assertTrue("Talking to Ellie should not set the milestone until dialogue closes", milestones.isEmpty())
        }

        runTrigger("dialogue_closed", "Jed", repeats = 2).also { (items, tasks, milestones) ->
            assertTrue("Closing dialogue with Jed should not grant broken gear items", items.isEmpty())
            assertTrue("Closing dialogue with Jed should not complete collect objectives", tasks.isEmpty())
            assertTrue("Closing dialogue with Jed should not set broken gear milestones", milestones.isEmpty())
        }

        runTrigger("dialogue_closed", "Ellie", repeats = 2).also { (items, tasks, milestones) ->
            assertEquals(listOf("broken_arcade_display:1"), items)
            assertEquals(listOf("collect_display"), tasks)
            assertEquals(listOf("ms_ellie_display_collected"), milestones)
        }

        runTrigger("dialogue_closed", "Maddie", repeats = 2).also { (items, tasks, milestones) ->
            assertEquals(listOf("broken_coffee_grinder:1"), items)
            assertEquals(listOf("collect_grinder"), tasks)
            assertEquals(listOf("ms_maddie_grinder_collected"), milestones)
        }

        runTrigger("dialogue_closed", "Schoolteacher Kasey", repeats = 2).also { (items, tasks, milestones) ->
            assertEquals(listOf("broken_projector:1"), items)
            assertEquals(listOf("collect_projector"), tasks)
            assertEquals(listOf("ms_kasey_projector_collected"), milestones)
        }
    }
}

private fun readEvents(): List<GameEvent> {
    val file = File("src/main/assets/events.json")
    require(file.exists()) { "events.json not found" }
    val moshi = MoshiProvider.instance
    val adapter = moshi.adapter<List<GameEvent>>(Types.newParameterizedType(List::class.java, GameEvent::class.java))
    return requireNotNull(adapter.fromJson(file.readText()))
}
