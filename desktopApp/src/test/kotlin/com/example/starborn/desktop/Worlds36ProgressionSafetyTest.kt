package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.domain.dialogue.*
import com.example.starborn.domain.event.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.*
import com.example.starborn.navigation.CombatResultPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.io.File

/** Regression coverage for the pre-playtest controller failures. */
class Worlds36ProgressionSafetyTest {
    @get:Rule val compose = createComposeRule()
    private val observations = linkedMapOf<String,Any?>()
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private fun services(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("worlds36-readiness").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            block(s)
        } finally { s.close(); dir.deleteRecursively() }
    }
    @Test fun recordCurrentRuntimeReadiness() {
        services { s ->
            s.sessionStore.restore(s.sessionStore.state.value.copy(roomId="spire_zekes_apartment", activeQuests=setOf("w3_mq11"), completedQuests=emptySet(),
                questStageById=mapOf("w3_mq11" to "find_safehouse"), completedMilestones=emptySet(), completedEvents=emptySet(), questTasksCompleted=emptyMap()))
            compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id=="spire_zekes_apartment" }
            while(s.exploration.uiState.value.narrationPrompt!=null) { s.exploration.dismissNarration(); compose.waitForIdle() }
            observations["w3_safehouse"] = mapOf("visible_npcs" to s.exploration.uiState.value.npcs,
                "active_dialogue" to s.exploration.uiState.value.activeDialogue?.line?.id, "tasks" to s.sessionStore.state.value.questTasksCompleted["w3_mq11"],
                "actions" to s.exploration.uiState.value.actions.map{it.name})
            s.exploration.onNpcInteraction("zeke")
            compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue?.line?.id == "zeke_w3_mq11_safehouse_intro" }
            for ((room,npc) in listOf("foundry_lava_tube_bypass" to "Rylan", "foundry_pipe_gallery" to "Cameron", "orbital_customs_lounge" to "Charles")) {
                s.sessionStore.restore(s.sessionStore.state.value.copy(roomId=room, activeQuests=emptySet(), completedQuests=emptySet(), completedMilestones=emptySet(), completedEvents=emptySet()))
                compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id==room }
                s.exploration.onNpcInteraction(npc)
                compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue!=null }
                val root=s.exploration.uiState.value.activeDialogue!!.line
                val lines=reader.readList<DialogueLine>("dialogue.json").associateBy{it.id}
                val seen=mutableSetOf<String>()
                fun terminal(id:String?):Boolean {
                    if(id==null)return true
                    if(!seen.add(id))return false
                    val line=lines[id]?:return false
                    val opts=line.options.orEmpty().filter { isDialogueConditionMet(it.condition,s.sessionStore.state.value,s.inventoryService) }
                    return if(opts.isNotEmpty())opts.any { terminal(it.next) } else terminal(line.next)
                }
                observations["dialogue_$npc"]=mapOf("visible_npcs" to s.exploration.uiState.value.npcs,"root" to root.id,"has_terminal_path_in_current_state" to terminal(root.id))
            }
            for ((quest,room,enemies,task) in listOf(
                listOf("w3_mq15","spire_landing_pad_roof","administrator_boss,aero_drone","defeat_administrator"),
                listOf("w4_mq20","foundry_titan_dock","titan_walker_boss","defeat_titan_walker"),
                listOf("w6_mq30","source_center","ascended_vale","defeat_vale")
            )) {
                val ids=enemies.split(',')
                val targetEnemy=ids.first()
                s.sessionStore.restore(s.sessionStore.state.value.copy(roomId=room, activeQuests=emptySet(), completedQuests=emptySet(), completedEvents=emptySet(),
                    completedMilestones=emptySet(), roomStates=emptyMap(),questTasksCompleted=emptyMap(),questStageById=emptyMap()))
                compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id==room }
                s.exploration.onCombatVictory(CombatResultPayload(CombatResultPayload.Outcome.VICTORY,enemyIds=ids,roomId=room))
                compose.waitUntil(5000) { targetEnemy !in s.exploration.uiState.value.enemies }
                s.sessionStore.startQuest(quest)
                if(quest=="w6_mq30") s.sessionStore.setQuestTaskCompleted(quest,"confront_vale",true)
                assertTrue(s.saveManager.saveGame(9,s.sessionStore.state.value));assertTrue(s.loadSlot(9))
                compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
                observations["early_clear_$quest"]=mapOf("enemy_still_available" to (targetEnemy in s.exploration.uiState.value.enemies),
                    "objective_credited_after_accept_and_reload" to (task in s.sessionStore.state.value.questTasksCompleted[quest].orEmpty()),
                    "clear_receipts" to s.sessionStore.state.value.roomStates[room])
            }
            // Clearing both available Firewall sources early must not strand the later objective.
            s.sessionStore.restore(s.sessionStore.state.value.copy(roomId="deep_firewall_gamma",activeQuests=emptySet(),completedQuests=emptySet(),
                completedEvents=emptySet(),completedMilestones=emptySet(),roomStates=emptyMap(),questTasksCompleted=emptyMap(),questStageById=emptyMap()))
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id=="deep_firewall_gamma" }
            for (r in listOf("deep_firewall_gamma","deep_server_farm_scale_02")) {
                s.sessionStore.setRoom(r)
                compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id==r }
                s.exploration.onCombatVictory(CombatResultPayload(CombatResultPayload.Outcome.VICTORY,enemyIds=listOf("firewall_construct"),roomId=r))
                compose.waitUntil(5000) { "firewall_construct" !in s.exploration.uiState.value.enemies }
            }
            s.sessionStore.startQuest("w5_mq23")
            s.sessionStore.setQuestTaskCompleted("w5_mq23","firewall_gamma",true)
            assertTrue(s.saveManager.saveGame(9,s.sessionStore.state.value));assertTrue(s.loadSlot(9))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            observations["early_clear_both_firewall_sources"]=mapOf("remaining_current_enemies" to s.exploration.uiState.value.enemies,
                "credited_after_accept_and_reload" to ("defeat_construct" in s.sessionStore.state.value.questTasksCompleted["w5_mq23"].orEmpty()),
                "clear_receipts" to s.sessionStore.state.value.roomStates.filterKeys { it in setOf("deep_firewall_gamma","deep_server_farm_scale_02") })
            // Exercise public movement along the authored graph, rather than infer reachability from connections alone.
            val rooms=reader.readList<Room>("rooms.json").associateBy{it.id}
            fun path(start:String,target:String):List<Pair<String,String>>? {
                val q=java.util.ArrayDeque<Pair<String,List<Pair<String,String>>>>();val seen=mutableSetOf(start)
                q.add(start to emptyList())
                while(q.isNotEmpty()) { val (id,p)=q.removeFirst();if(id==target)return p
                    rooms[id]?.connections?.forEach { (direction,next) -> if(seen.add(next))q.add(next to (p+(direction to next))) }
                }
                return null
            }
            for ((start,target,quest) in listOf(Triple("spire_archive_vault","spire_landing_pad_roof","w3_mq14"),
                Triple("foundry_conveyor_belt","foundry_titan_dock","w4_mq18"),Triple("orbital_executive_dock","deep_firewall_gamma","w5_mq21"),
                Triple("source_campfire","source_new_world","w6_mq26"))) {
                s.sessionStore.restore(s.sessionStore.state.value.copy(roomId=start,activeQuests=setOf(quest),completedQuests=emptySet(),
                    completedMilestones=emptySet(),completedEvents=emptySet(),roomStates=emptyMap(),questTasksCompleted=emptyMap(),questStageById=emptyMap()))
                compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id==start }
                val trace=mutableListOf<String>();var blocked:String?=null
                for ((direction,next) in requireNotNull(path(start,target))) {
                    while(s.exploration.uiState.value.narrationPrompt!=null) { s.exploration.dismissNarration();compose.waitForIdle() }
                    val before=s.sessionStore.state.value.roomId
                    s.exploration.travel(direction);compose.waitForIdle()
                    if(s.sessionStore.state.value.roomId!=next) { blocked="$before -> $direction -> $next";break }
                    trace+=next
                }
                observations["public_early_path_$quest"]=mapOf("start" to start,"target" to target,"trace" to trace,"blocked" to (blocked?:"none"))
            }
            s.sessionStore.restore(s.sessionStore.state.value.copy(roomId="source_center",activeQuests=setOf("w6_mq26"),completedQuests=emptySet(),completedMilestones=emptySet(),roomStates=emptyMap()))
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id=="source_center" }
            s.exploration.travel("east")
            compose.waitForIdle()
            assertEquals("source_center", s.sessionStore.state.value.roomId)
            observations["pre_ending_east_from_center"]=s.sessionStore.state.value.roomId
            // Actual production predicate/menu + trigger execution with insufficient quantity.
            s.inventoryService.restore(mapOf("composite_plate" to 1,"hydraulic_fluid" to 1))
            s.sessionStore.restore(s.sessionStore.state.value.copy(roomId="foundry_pipe_gallery",completedMilestones=setOf("ms_cameron_pipe_favor_asked"),inventory=s.inventoryService.snapshot()))
            compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id=="foundry_pipe_gallery" }
            s.exploration.onNpcInteraction("Cameron")
            compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue!=null }
            val offered=s.exploration.uiState.value.dialogueChoices.any { it.id=="cameron_opt_favor_turnin" }
            if(offered) { s.exploration.onDialogueChoiceSelected("cameron_opt_favor_turnin");s.exploration.advanceDialogue() }
            observations["cameron_one_plate"]=mapOf("turnin_offered" to offered,"reward_granted" to s.inventoryService.hasItem("camerons_pipe_wrench"),
                "completed_milestone" to ("ms_cameron_pipe_favor_completed" in s.sessionStore.state.value.completedMilestones))
        }
        services { s ->
            s.inventoryService.restore(emptyMap())
            s.sessionStore.restore(s.sessionStore.state.value.copy(roomId="spire_the_static", activeQuests=setOf("w3_sq11"),
                completedQuests=emptySet(),completedMilestones=emptySet(),questTasksCompleted=emptyMap(),inventory=emptyMap(),
                roomStates=mapOf("spire_exec_lounge_scale_02" to mapOf("encounter_cleared:sentinel_mki" to true))))
            compose.waitUntil(10000) { s.inventoryService.hasItem("neon_sign_core") }
            assertEquals(1,s.inventoryService.snapshot()["neon_sign_core"])
            assertTrue(s.saveManager.saveGame(9,s.sessionStore.state.value));assertTrue(s.loadSlot(9))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            assertEquals(1,s.inventoryService.snapshot()["neon_sign_core"])
        }
        val store=GameSessionStore()
        val manager=EventManager(reader.readList<GameEvent>("events.json"),store)
        manager.handleTrigger("player_action",EventPayload.Action("w4_crucible_start_round_1"))
        observations["crucible_round1_retry_available"]=manager.canHandleTrigger("player_action",EventPayload.Action("w4_crucible_start_round_1"))
        manager.handleTrigger("encounter_victory",EventPayload.EncounterOutcome(listOf("magma_drone","magma_drone","welder_bot"),EventPayload.EncounterOutcome.Outcome.VICTORY,"foundry_crucible_arena"))
        observations["crucible_after_round1"]=store.state.value.completedMilestones
        assertTrue((observations.getValue("w3_safehouse") as Map<*,*>)["visible_npcs"].toString().contains("zeke"))
        for (npc in listOf("Rylan","Cameron","Charles")) assertEquals(true,
            (observations.getValue("dialogue_$npc") as Map<*,*>)["has_terminal_path_in_current_state"])
        for (q in listOf("w3_mq15","w4_mq20","w6_mq30")) assertEquals(true,
            (observations.getValue("early_clear_$q") as Map<*,*>)["objective_credited_after_accept_and_reload"])
        assertEquals(true,(observations.getValue("early_clear_both_firewall_sources") as Map<*,*>)["credited_after_accept_and_reload"])
        assertEquals(true, observations["crucible_round1_retry_available"])
        assertEquals(setOf("ms_crucible_round1_started","ms_crucible_round1_cleared"), observations["crucible_after_round1"])
        assertEquals(false,(observations.getValue("cameron_one_plate") as Map<*,*>)["turnin_offered"])
        assertEquals(false,(observations.getValue("cameron_one_plate") as Map<*,*>)["reward_granted"])
        val out=File("build/reports/worlds36-readiness/observations.json").apply { parentFile.mkdirs() }
        out.writeText(org.json.JSONObject(observations).toString(2)+"\n")
    }
}
