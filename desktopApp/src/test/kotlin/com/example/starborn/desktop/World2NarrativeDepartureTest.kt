package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.session.GameSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class World2NarrativeDepartureTest {
    @get:Rule val compose = createComposeRule()
    private fun runWith(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("world2-narrative").toFile()
        val s = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading }
            block(s)
        } finally { s.close(); dir.deleteRecursively() }
    }
    private fun act(s: DesktopAppServices, id: String) = s.exploration.onActionSelected(GenericAction(id, "generic", actionEvent = id))
    private fun dismissNarration(s: DesktopAppServices) {
        while (s.exploration.uiState.value.narrationPrompt != null) {
            val old = s.exploration.uiState.value.narrationPrompt
            s.exploration.dismissNarration()
            compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != old }
        }
    }
    @Test fun orionIsUnknownUntilHeIntroducesHimselfAndEarlierSaveResetsIdentity() = runWith { s ->
        val earlier = s.sessionStore.state.value.copy(roomId = "sector9_stasis_chamber", activeQuests = setOf("w2_mq03"),
            completedMilestones = setOf("ms_w2_stasis_awake"), completedQuests = emptySet(),
            questTasksCompleted = mapOf("w2_mq03" to setOf("align_stasis_rings")))
        s.sessionStore.restore(earlier)
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == earlier.roomId }
        assertEquals("???", s.exploration.uiState.value.npcPresenceNames["orion"])
        s.exploration.onNpcInteraction("orion")
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue != null }
        assertEquals("???", s.exploration.uiState.value.activeDialogue?.displaySpeaker)
        repeat(2) { s.exploration.advanceDialogue() }
        assertEquals("orion_w2_identity", s.exploration.uiState.value.activeDialogue?.line?.id)
        s.exploration.advanceDialogue()
        compose.waitUntil(5000) { s.exploration.uiState.value.npcPresenceNames["orion"] == "Orion" }
        assertNull(s.exploration.uiState.value.activeDialogue?.displaySpeaker)
        s.sessionStore.restore(earlier)
        compose.waitUntil(5000) { s.exploration.uiState.value.npcPresenceNames["orion"] == "???" }
    }
    @Test fun fifthBotanistScanReviewsWithZekeAtItsActualLocationAndRewardsOnce() = runWith { s ->
        // Moss is last here, rather than assuming the player finishes at Stasis Chamber.
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_crash_site", activeQuests = setOf("w2_sq01"),
            completedQuests = emptySet(), completedEvents = emptySet(), completedMilestones = emptySet(),
            questTasksCompleted = mapOf("w2_sq01" to setOf("scan_flora_1", "scan_flora_2", "scan_flora_4", "scan_flora_5"))))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_crash_site" }
        val before = s.inventoryService.snapshot()["painkillers"] ?: 0
        act(s, "w2_sq01_scan_moss")
        compose.waitUntil(5000) { s.exploration.uiState.value.narrationPrompt != null }
        assertNull(s.exploration.uiState.value.activeDialogue)
        dismissNarration(s)
        compose.waitUntil(5000) { s.exploration.uiState.value.activeDialogue?.line?.id == "zeke_w2_sq01_turnin_1" }
        assertEquals("sector9_crash_site", s.sessionStore.state.value.roomId)
        s.exploration.advanceDialogue()
        compose.waitUntil(5000) { "w2_sq01" in s.sessionStore.state.value.completedQuests }
        assertEquals(before + 2, s.inventoryService.snapshot()["painkillers"])
        act(s, "w2_sq01_scan_moss")
        assertEquals(before + 2, s.inventoryService.snapshot()["painkillers"])
    }
    @Test fun ghostRecruitmentFollowsHisChoiceAndDrillAndDoesNotRepeatOnResume() = runWith { s ->
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_canopy_ridge", activeQuests = setOf("w2_mq04"),
            completedQuests = emptySet(), completedEvents = emptySet(), completedMilestones = emptySet(),
            partyMembers = listOf("nova", "zeke", "orion"),
            questTasksCompleted = mapOf("w2_mq04" to setOf("reach_canopy_ridge", "confront_stalker"))))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_canopy_ridge" }
        s.exploration.onCombatVictory(com.example.starborn.navigation.CombatResultPayload(
            com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
            enemyIds = listOf("the_beast"), roomId = "sector9_canopy_ridge"))
        compose.waitUntil(5000) { "defeat_the_beast" in s.sessionStore.state.value.questTasksCompleted["w2_mq04"].orEmpty() }
        assertFalse("gh0st" in s.sessionStore.state.value.partyMembers)
        dismissNarration(s)
        act(s, "w2_mq04_anchor_drill")
        compose.waitUntil(5000) { s.exploration.uiState.value.cinematic != null }
        assertFalse("gh0st" in s.sessionStore.state.value.partyMembers)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(s.loadSlot(9))
        compose.waitUntil(10000) { s.exploration.uiState.value.cinematic != null }
        s.exploration.skipCinematic()
        compose.waitUntil(5000) { "w2_mq04" in s.sessionStore.state.value.completedQuests }
        assertEquals(1, s.sessionStore.state.value.partyMembers.count { it == "gh0st" })
        assertTrue("nova_link" in s.sessionStore.state.value.unlockedSkills)
        val xp = s.sessionStore.state.value.playerXp
        act(s, "w2_mq04_anchor_drill")
        assertEquals(xp, s.sessionStore.state.value.playerXp)
        assertNull(s.exploration.uiState.value.cinematic)
    }

    @Test fun firstDepartureRequiresBoardingAndBridgeConsoleAndCanStayDocked() = runWith { s ->
        s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = "sector9_hangar_bay", worldId = "world_2", hubId = "hub_4_facility",
            activeQuests = setOf("w2_mq05"), completedQuests = emptySet(), completedEvents = emptySet(),
            questTasksCompleted = mapOf("w2_mq05" to setOf("reboot_bridge_relic"))))
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "sector9_hangar_bay" }
        act(s, "w2_mq05_confirm_departure")
        assertNull(s.exploration.uiState.value.cinematic)
        act(s, "w2_mq05_launch")
        compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == "astra_cargo_bay" }
        assertEquals("sector9_hangar_bay", s.sessionStore.state.value.astraReturnRoomId)
        assertFalse("w3_mq11" in s.sessionStore.state.value.activeQuests)
        dismissNarration(s)
        s.exploration.travel("east")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_common_room" }
        s.exploration.travel("north")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_bridge" }
        act(s, "astra_nav_console")
        assertFalse(s.exploration.uiState.value.isAstraNavConsoleVisible)
        assertEquals("w2_departure_entry", s.exploration.uiState.value.activeDialogue?.line?.id)
        s.exploration.onDialogueChoiceSelected("stay")
        assertEquals("astra_bridge", s.sessionStore.state.value.roomId)
        assertFalse("w2_mq05" in s.sessionStore.state.value.completedQuests)
        assertTrue(s.saveManager.saveGame(9, s.sessionStore.state.value))
        assertTrue(s.loadSlot(9))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom?.id == "astra_bridge" }
        s.exploration.travel("south")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_common_room" }
        s.exploration.travel("west")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_cargo_bay" }
        act(s, "astra_disembark")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "sector9_hangar_bay" }
        assertFalse("w2_mq05" in s.sessionStore.state.value.completedQuests)
        act(s, "w2_mq05_launch")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_cargo_bay" }
        dismissNarration(s)
        s.exploration.travel("east")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_common_room" }
        s.exploration.travel("north")
        compose.waitUntil(5000) { s.exploration.uiState.value.currentRoom?.id == "astra_bridge" }
        act(s, "astra_nav_console")
        s.exploration.onDialogueChoiceSelected("depart")
        compose.waitUntil(5000) { s.exploration.uiState.value.cinematic != null }
        s.exploration.skipCinematic()
        compose.waitUntil(10000) { s.sessionStore.state.value.roomId == "spire_sewers_landing" }
        assertTrue("w2_mq05" in s.sessionStore.state.value.completedQuests)
        assertTrue("w3_mq11" in s.sessionStore.state.value.activeQuests)
    }
}
