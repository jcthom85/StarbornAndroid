package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.domain.movement.EnemyPartyRuntimeState
import com.example.starborn.navigation.CombatResultPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopExplorationReturnTest {
    @get:Rule val compose = createComposeRule()
    private fun withRoom(room: String, block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-exploration-return-").toFile()
        val s = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            runBlocking { s.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(s.startDebugScenario("tut_npc_dialogue"))
            s.sessionStore.restore(s.sessionStore.state.value.copy(roomId = room, completedEvents = emptySet(),
                roomStates = emptyMap(), unlockedExits = emptySet(), activeQuests = setOf("w1_mq01")))
            compose.waitUntil(10000) { s.exploration.uiState.value.currentRoom?.id == room }
            block(s)
        } finally { s.close(); directory.deleteRecursively() }
    }
    private fun victory(s: DesktopAppServices, room: String, enemies: List<String>, source: String? = null) {
        s.exploration.onCombatVictory(CombatResultPayload(CombatResultPayload.Outcome.VICTORY,
            enemyIds = enemies, roomId = room, sourcePartyId = source))
    }

    @Test fun loaderVictoryClearsEnemyOpensNorthAndSurvivesDiskReload() = withRoom("workshop_yard") { s ->
        assertTrue("faulted_loader" in s.exploration.uiState.value.enemies)
        s.exploration.travel("north")
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertNull(s.exploration.uiState.value.cinematic)
        assertNull(s.exploration.uiState.value.narrationPrompt)
        s.exploration.dismissBlockedPrompt()
        s.exploration.travel("north")
        compose.waitUntil(5000) { s.exploration.uiState.value.blockedPrompt != null }
        assertNull(s.exploration.uiState.value.cinematic)
        s.exploration.dismissBlockedPrompt()
        victory(s, "workshop_yard", listOf("faulted_loader"))
        compose.waitUntil(5000) { "faulted_loader" !in s.exploration.uiState.value.enemies }
        assertFalse("north" in s.exploration.uiState.value.blockedDirections)
        assertTrue(s.sessionStore.state.value.roomStates.getValue("workshop_yard")["loader_cleared"] == true)
        assertTrue(s.saveManager.saveGame(1, s.sessionStore.state.value))
        assertTrue(s.loadSlot(1))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom != null }
        assertFalse("faulted_loader" in s.exploration.uiState.value.enemies)
        s.exploration.travel("north")
        compose.waitUntil(5000) { s.sessionStore.state.value.roomId == "workshop_floor" }
    }

    @Test fun defeatingOneStaticFormationPreservesOtherEnemiesAndRetreatClearsNone() = withRoom("foundry_conveyor_belt") { s ->
        val original = s.exploration.uiState.value.enemies
        assertTrue(original.containsAll(listOf("welder_bot", "magma_drone", "phantom_prototype")))
        s.exploration.onCombatRetreat(listOf("welder_bot", "magma_drone"))
        assertEquals(original, s.exploration.uiState.value.enemies)
        victory(s, "foundry_conveyor_belt", listOf("welder_bot", "magma_drone"))
        compose.waitUntil(5000) { "welder_bot" !in s.exploration.uiState.value.enemies }
        assertEquals(listOf("phantom_prototype"), s.exploration.uiState.value.enemies)
        victory(s, "foundry_conveyor_belt", listOf("welder_bot", "magma_drone"))
        assertEquals(listOf("phantom_prototype"), s.exploration.uiState.value.enemies)
        assertTrue(s.saveManager.saveGame(1, s.sessionStore.state.value))
        assertTrue(s.loadSlot(1))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom != null }
        assertEquals(listOf("phantom_prototype"), s.exploration.uiState.value.enemies)
    }

    @Test fun previouslyStuckLoaderSaveRecoversWithoutAnotherFightOrReward() = withRoom("workshop_yard") { s ->
        val old = s.sessionStore.state.value.copy(roomStates = mapOf("workshop_yard" to mapOf("loader_cleared" to true)),
            completedEvents = setOf("w1_mq01_faulted_loader_victory"))
        assertTrue(s.saveManager.saveGame(2, old))
        assertTrue(s.loadSlot(2))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom != null }
        assertFalse("faulted_loader" in s.exploration.uiState.value.enemies)
        assertFalse("north" in s.exploration.uiState.value.blockedDirections)
        assertEquals(old.playerAp, s.sessionStore.state.value.playerAp)
        assertEquals(old.playerCredits, s.sessionStore.state.value.playerCredits)
    }

    @Test fun movingPartyVictoryPersistsItsIdentityAndPreservesTheStaticFormation() = withRoom("debug_enemy_party_sizes") { s ->
        val party = s.worldDataSource.loadEnemyMovement().parties.first { it.id == "debug_patrol_3" }
        val state = s.sessionStore.state.value.copy(enemyPartyStates = mapOf(
            party.id to EnemyPartyRuntimeState("debug_enemy_party_sizes", moveRemainingMs = 60000)))
        assertTrue(s.saveManager.saveGame(3, state))
        assertTrue(s.loadSlot(3))
        compose.waitUntil(10000) { !s.exploration.uiState.value.isLoading && s.exploration.uiState.value.currentRoom != null }
        victory(s, "debug_enemy_party_sizes", party.enemies, party.id)
        compose.waitUntil(5000) { s.sessionStore.state.value.enemyPartyStates[party.id]?.defeated == true }
        assertTrue("dominion_dampener" in s.exploration.uiState.value.enemies)
        compose.waitUntil(5000) { s.exploration.uiState.value.visualEnemyParties.none { it.id == "moving_${party.id}" } }
    }

    @Test @OptIn(ExperimentalTestApi::class)
    fun oneKeyboardPressAcknowledgesOnlyTheTopQueuedInformationCard() = withRoom("pit_nova_bunk") { s ->
        var acknowledged = 0
        compose.setContent { DesktopStarbornTheme(s) {
            Box(Modifier.size(1280.dp, 800.dp)) { DesktopExplorationScreen(s, {}, {}, {}, {}, {}, {}) }
        } }
        compose.waitForIdle()
        compose.runOnIdle {
            listOf("first", "second").forEach { id ->
                s.promptManager.enqueue(com.example.starborn.domain.prompt.TutorialPrompt(
                    com.example.starborn.domain.tutorial.TutorialEntry(id, "test", "Information $id"), { acknowledged++ }))
            }
        }
        compose.onNodeWithText("Information first").assertIsDisplayed()
        compose.onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Enter) }
        compose.waitUntil(5000) { s.promptManager.state.value.current?.id == "second" }
        compose.runOnIdle { assertEquals(1, acknowledged) }
        compose.onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Escape) }
        compose.waitUntil(5000) { s.promptManager.state.value.current == null }
        compose.runOnIdle { assertEquals(2, acknowledged) }
    }

    @Test fun roomActivitiesRequireAChoiceAndShortcutLegendIsAbsent() = withRoom("pit_nova_bunk") { s ->
        s.sessionStore.updatePartyVitals(mapOf("nova" to 20))
        compose.setContent { DesktopStarbornTheme(s) {
            Box(Modifier.size(1280.dp, 800.dp)) { DesktopExplorationScreen(s, {}, {}, {}, {}, {}, {}) }
        } }
        compose.onNodeWithTag("hud-key-legend").assertDoesNotExist()
        compose.onNodeWithText("Actions").performClick()
        compose.onNodeWithText("Rest at sleeping shelf", substring = true).performClick()
        compose.onNodeWithText("A place to rest and recover the party's HP.").assertIsDisplayed()
        assertEquals(20, s.sessionStore.state.value.partyMemberHp["nova"])
        compose.onNodeWithText("Back").performClick()
        assertEquals(20, s.sessionStore.state.value.partyMemberHp["nova"])
        val rest = s.exploration.uiState.value.actions.filterIsInstance<RestStopAction>().single()
        compose.runOnIdle { s.exploration.inspectRoomAction(rest) }
        compose.onNodeWithText("Rest").performClick()
        compose.waitUntil(5000) { s.sessionStore.state.value.partyMemberHp.getValue("nova") > 20 }
        compose.onNodeWithText("A Quiet Rest").assertIsDisplayed()
    }
}
