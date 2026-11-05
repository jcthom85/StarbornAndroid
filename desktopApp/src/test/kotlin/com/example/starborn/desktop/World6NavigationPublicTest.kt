package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World6NavigationPublicTest {
    @get:Rule val compose = createComposeRule()

    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world6-navigation").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(compose.runOnIdle { services.startDebugScenario("tut_npc_dialogue") })
            compose.waitUntil(10000) { !services.exploration.uiState.value.isLoading }
            block(services)
        } finally { compose.runOnIdle { services.close() }; dir.deleteRecursively() }
    }

    private fun restore(s: DesktopAppServices, state: GameSessionState) {
        compose.runOnIdle { s.sessionStore.restore(state) }
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == state.roomId }
        dismissNarration(s)
    }

    private fun dismissNarration(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            compose.runOnIdle { s.exploration.dismissNarration() }
            compose.waitForIdle()
        }
    }

    private fun act(s: DesktopAppServices, event: String) {
        val action = s.exploration.uiState.value.actions.filterIsInstance<GenericAction>().single { it.actionEvent == event }
        compose.runOnIdle { s.exploration.onActionSelected(action) }
        compose.waitForIdle()
    }

    private fun finish(s: DesktopAppServices, seen: MutableList<String> = mutableListOf()) {
        repeat(25) {
            val line = s.exploration.uiState.value.activeDialogue?.line ?: return
            seen += line.id
            val choices = s.exploration.uiState.value.dialogueChoices
            if (choices.isEmpty()) compose.runOnIdle { s.exploration.advanceDialogue() }
            else compose.runOnIdle { s.exploration.onDialogueChoiceSelected(choices.single { it.id == "correct" }.id) }
            compose.waitForIdle()
        }
        error("World 6 dialogue did not finish")
    }

    private fun go(s: DesktopAppServices, direction: String, expected: String) {
        assertTrue(direction in setOf("north", "south", "east", "west"))
        dismissNarration(s)
        compose.runOnIdle { s.exploration.travel(direction) }
        compose.waitUntil(5000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == expected }
        dismissNarration(s)
        assertEquals(expected, s.sessionStore.state.value.roomId)
    }

    private val clearance = mapOf(
        "source_memory_bridge" to mapOf("bridge_built" to true),
        "source_memory_threshold" to mapOf("crossing_open" to true),
        "source_memory_stair" to mapOf("ascent_open" to true),
        "source_spire_arena" to mapOf("center_path_open" to true),
        "source_center" to mapOf("ending_complete" to true))

    private fun blocked(s: DesktopAppServices, direction: String, clue: String) {
        val before = s.sessionStore.state.value
        compose.runOnIdle { s.exploration.travel(direction) }
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertEquals(before.roomId, s.sessionStore.state.value.roomId)
        assertTrue(s.exploration.uiState.value.blockedPrompt!!.message.contains(clue, true))
        compose.runOnIdle { s.exploration.dismissBlockedPrompt() }
        assertEquals(before.playerXp, s.sessionStore.state.value.playerXp)
    }

    private fun victory(s: DesktopAppServices, enemy: String, vararg group: String) {
        assertTrue(enemy in s.exploration.uiState.value.enemies)
        val room = requireNotNull(s.sessionStore.state.value.roomId)
        compose.runOnIdle { s.exploration.engageEnemy(enemy) }
        assertTrue(s.sessionStore.state.value.pendingBattleJson.isNotBlank())
        // Controller transition fixture; earned campaign tests play the actual battles.
        compose.runOnIdle { s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
            com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
            enemyIds = group.toList(), roomId = room)) }
        compose.waitForIdle(); dismissNarration(s)
    }

    private fun reload(s: DesktopAppServices, state: GameSessionState = s.sessionStore.state.value) {
        assertTrue(s.saveManager.saveGame(9, state)); assertTrue(compose.runOnIdle { s.loadSlot(9) })
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == state.roomId }
        dismissNarration(s)
    }

    @Test fun allSourceRoomsHaveWorkingCompassTravelAndImmediateReturns() = runWith { s ->
        val ids = s.worldDataSource.loadHubNodes().filter { it.hubId in setOf("hub_11_event_horizon", "hub_12_singularity") }.flatMap { it.rooms }.toSet()
        val rooms = s.worldDataSource.loadRooms().filter { it.id in ids }.associateBy { it.id }
        restore(s, s.sessionStore.state.value.copy(worldId = "world_6", hubId = "hub_11_event_horizon", roomId = "source_campfire",
            activeQuests = emptySet(), completedQuests = (26..30).map { "w6_mq$it" }.toSet(),
            completedEvents = emptySet(), completedMilestones = setOf("ms_game_complete", "ms_w6_mq26_complete"),
            questTasksCompleted = emptyMap(), roomStates = clearance))
        val seen = mutableSetOf<String>(); val edges = mutableSetOf<Pair<String,String>>()
        val reverse = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        fun walk(id: String) {
            if (!seen.add(id)) return
            assertEquals(rooms.getValue(id).connections.keys, s.exploration.uiState.value.directionIndicators.keys)
            assertTrue(s.exploration.uiState.value.actions.none { it is com.example.starborn.domain.model.TravelAction })
            for ((d, to) in rooms.getValue(id).connections) {
                go(s,d,to); edges += id to to; walk(to); go(s,reverse.getValue(d),id); edges += to to id
            }
        }
        walk("source_campfire")
        assertEquals(65,seen.size); assertEquals(ids,seen); assertEquals(128,edges.size)
    }

    @Test fun formerDiagonalSavesUseNewSwipeRoutesWithoutLosingNightmareReturns() = runWith { s ->
        for ((room, route) in listOf(
            "source_zeke_review_loop" to listOf("east" to "source_zeke_nightmare", "east" to "source_campfire"),
            "source_zeke_nightmare" to listOf("east" to "source_campfire"),
            "source_orion_nightmare" to listOf("west" to "source_campfire_node_scale_01", "west" to "source_campfire"))) {
            restore(s,s.sessionStore.state.value.copy(worldId="world_6",hubId="hub_11_event_horizon",roomId=room,
                activeQuests=setOf("w6_mq26"),completedQuests=emptySet(),completedEvents=emptySet(),completedMilestones=emptySet(),
                roomStates=emptyMap(),questTasksCompleted=emptyMap(),visitedNodes=emptySet(),unlockedExits=setOf("$room:southeast","$room:southwest")))
            val before=s.sessionStore.state.value; reload(s)
            for ((d,to) in route) go(s,d,to)
            go(s,"north","source_gh0st_nightmare"); go(s,"south","source_campfire")
            go(s,"west","source_zeke_nightmare"); go(s,"east","source_campfire")
            go(s,"east","source_campfire_node_scale_01"); go(s,"east","source_orion_nightmare")
            go(s,"west","source_campfire_node_scale_01"); go(s,"west","source_campfire")
            assertEquals(before.playerXp,s.sessionStore.state.value.playerXp)
            assertEquals(before.inventory,s.sessionStore.state.value.inventory)
            assertEquals(before.questTasksCompleted,s.sessionStore.state.value.questTasksCompleted)
        }
    }

    @Test fun bridgeAscentAndDefendersOpenOnlyThroughTheirStoryActionsAndKeepRestAccess() = runWith { s ->
        restore(s,s.sessionStore.state.value.copy(worldId="world_6",hubId="hub_11_event_horizon",roomId="source_memory_bridge",
            activeQuests=setOf("w6_mq28"),completedQuests=setOf("w6_mq26","w6_mq27"),completedEvents=emptySet(),
            completedMilestones=emptySet(),roomStates=emptyMap(),questTasksCompleted=emptyMap(),visitedNodes=emptySet()))
        blocked(s,"east","anchors")
        for (anchor in listOf("zeke","ghost","orion","nova")) { act(s,"w6_mq28_anchor_$anchor"); dismissNarration(s) }
        blocked(s,"east","unfinished bridge")
        act(s,"w6_mq28_build_bridge"); dismissNarration(s)
        go(s,"east","source_memory_bridge_span"); go(s,"east","source_memory_threshold")
        blocked(s,"east","promise")
        go(s,"west","source_memory_bridge_span"); act(s,"w6_mq28_final_banter"); dismissNarration(s)
        go(s,"east","source_memory_threshold"); blocked(s,"east","threshold")
        act(s,"w6_mq28_reach_singularity")
        compose.waitUntil(5000) { s.sessionStore.state.value.roomId=="source_memory_stair" }; dismissNarration(s)
        go(s,"west","source_memory_threshold"); go(s,"east","source_memory_stair")
        blocked(s,"east","Refuse")
        for (revision in listOf("jed","astra","foundry")) { act(s,"w6_mq29_refuse_${revision}_revision"); dismissNarration(s) }
        blocked(s,"east","memory stair")
        act(s,"w6_mq29_climb_stair"); dismissNarration(s)
        go(s,"east","source_memory_fragments")
        victory(s,"source_shadow","source_shadow","distorted_sentinel","glitch_hound")
        go(s,"east","source_spire_thought"); go(s,"east","source_spire_archive"); go(s,"east","source_spire_arena")
        blocked(s,"east","Memory Leak")
        victory(s,"memory_leak","memory_leak","nightmare_guard")
        go(s,"east","source_center")
        assertTrue("w6_mq29" in s.sessionStore.state.value.completedQuests)
        assertTrue("w6_mq30" in s.sessionStore.state.value.activeQuests)
        blocked(s,"east","white horizon")
        val after=s.sessionStore.state.value
        reload(s,after.copy(roomStates=after.roomStates-clearance.keys))
        for (to in listOf("source_spire_arena","source_spire_archive","source_spire_thought","source_memory_fragments","source_memory_stair","source_memory_threshold","source_memory_bridge_span")) go(s,"west",to)
        val rest=s.exploration.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.RestStopAction>().single()
        compose.runOnIdle { s.exploration.onActionSelected(rest) }; compose.waitForIdle(); dismissNarration(s)
        assertEquals("source_memory_bridge_span",s.sessionStore.state.value.roomId)
        go(s,"east","source_memory_threshold"); go(s,"east","source_memory_stair"); go(s,"east","source_memory_fragments")
        go(s,"east","source_spire_thought"); go(s,"east","source_spire_archive"); go(s,"east","source_spire_arena"); go(s,"east","source_center")
        assertEquals(after.playerXp,s.sessionStore.state.value.playerXp)
        assertEquals(after.inventory,s.sessionStore.state.value.inventory)
    }

    @Test fun legacyLocationsRetainEscapeAndReturnWithoutReceivingStoryCredit() = runWith { s ->
        for ((room,west) in listOf("source_memory_bridge_span" to "source_memory_bridge", "source_memory_stair" to "source_memory_threshold",
            "source_memory_fragments" to "source_memory_stair", "source_center" to "source_spire_arena", "source_new_world" to "source_center")) {
            restore(s,s.sessionStore.state.value.copy(worldId="world_6",hubId=if(room in setOf("source_memory_bridge_span")) "hub_11_event_horizon" else "hub_12_singularity",roomId=room,
                activeQuests=setOf("w6_mq26"),completedQuests=emptySet(),completedEvents=emptySet(),completedMilestones=emptySet(),
                roomStates=emptyMap(),questTasksCompleted=emptyMap(),visitedNodes=emptySet(),unlockedExits=emptySet()))
            val before=s.sessionStore.state.value; reload(s)
            go(s,"west",west); go(s,"east",room)
            assertEquals(before.playerXp,s.sessionStore.state.value.playerXp)
            assertEquals(before.inventory,s.sessionStore.state.value.inventory)
            assertEquals(before.completedQuests,s.sessionStore.state.value.completedQuests)
            assertEquals(before.questTasksCompleted,s.sessionStore.state.value.questTasksCompleted)
        }
    }
    @Test fun namedMemoryReturnsRemainRepeatableAfterReloadAndOldCompletionReceipts() = runWith { s ->
        val rooms=s.worldDataSource.loadRooms().filter { it.id.startsWith("source_") }.associateBy { it.id }
        val shortcuts=rooms.values.flatMap { room -> room.actions.mapNotNull { raw ->
            (raw["action_event"] as? String)?.takeIf { it.startsWith("warp_source_") }?.let { Triple(room.id,it,raw["name"] as String) }
        } }
        assertEquals(9,shortcuts.size)
        fun walkTo(target:String) {
            val pending=java.util.ArrayDeque<Pair<String,List<Pair<String,String>>>>(); val seen=mutableSetOf<String>()
            pending.add(requireNotNull(s.sessionStore.state.value.roomId) to emptyList())
            while(pending.isNotEmpty()) {
                val (id,path)=pending.removeFirst()
                if(!seen.add(id))continue
                if(id==target) { path.forEach { (d,to) -> go(s,d,to) };return }
                for((d,to) in rooms.getValue(id).connections) pending.add(to to (path+(d to to)))
            }
            error("Missing walking return to $target")
        }
        for((id,event,label) in shortcuts) {
            restore(s,s.sessionStore.state.value.copy(worldId="world_6",hubId=if(id.startsWith("source_memory_stair") || id.startsWith("source_spire_thought") || id.startsWith("source_center")) "hub_12_singularity" else "hub_11_event_horizon",roomId=id,
                activeQuests=emptySet(),completedQuests=(26..30).map { "w6_mq$it" }.toSet(),completedEvents=setOf(event,"w6_salvage_astral_thread"),
                completedMilestones=setOf("ms_game_complete"),roomStates=clearance,questTasksCompleted=emptyMap(),visitedNodes=emptySet()))
            val before=s.sessionStore.state.value
            reload(s)
            repeat(2) {
                val ui=s.exploration.uiState.value;val room=requireNotNull(ui.currentRoom)
                val action=ui.actions.filterIsInstance<GenericAction>().single { it.actionEvent==event }
                val plan=requireNotNull(com.example.starborn.feature.exploration.presentation.buildInlineActionPlan(room.description,ui.actions,ui.actionHints,room))
                assertTrue("$label must have a tappable description link",plan.segments.any { (it.target as? com.example.starborn.feature.exploration.presentation.InlineActionTarget.Room)?.action==action })
                act(s,event);dismissNarration(s)
                assertNotEquals(id,s.sessionStore.state.value.roomId)
                walkTo(id)
                assertEquals(before.playerXp,s.sessionStore.state.value.playerXp)
                assertEquals(before.inventory,s.sessionStore.state.value.inventory)
                reload(s)
            }
        }
    }

}
