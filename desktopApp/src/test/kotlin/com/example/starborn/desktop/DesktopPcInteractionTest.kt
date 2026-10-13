package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.feature.exploration.presentation.InlineActionTarget
import com.example.starborn.feature.exploration.viewmodel.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class DesktopPcInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedQuicksaveDoesNotClaimProgressWasSaved() {
        compose.setContent { DesktopStarbornTheme {
            Box(Modifier.width(300.dp)) { DesktopExplorationKeyLegendHud(ExplorationUiState(), blocked = false, onQuickSave = { false }) }
        } }
        compose.onNodeWithTag("key-legend-F5").performClick()
        compose.onNodeWithText("Save failed").assertExists()
        compose.onNodeWithText("Saved!").assertDoesNotExist()
    }

    @Test fun rebindingDialogRejectsConflictsAndAppliesTheChosenKey() {
        val directory = Files.createTempDirectory("starborn-rebind-ui-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            compose.setContent { DesktopStarbornTheme(services) {
                Column(Modifier.size(640.dp, 600.dp).verticalScroll(rememberScrollState())) { DesktopKeyboardSettings(services) }
            } }
            compose.onNodeWithTag("bind-INTERACT").performScrollTo().performClick()
            compose.onNodeWithTag("key-binding-capture").performKeyInput { pressKey(Key.M) }
            compose.onNodeWithText("M is already assigned to open field map.").assertExists()
            assertEquals("E", runBlocking { services.userSettingsStore.keyBindings.first() }.label(DesktopAction.INTERACT))
            compose.onNodeWithTag("key-binding-capture").performKeyInput { pressKey(Key.F7) }
            compose.waitUntil(5000) { runBlocking { services.userSettingsStore.keyBindings.first() }.label(DesktopAction.INTERACT) == "F7" }
            compose.onNodeWithTag("bind-INTERACT").assertTextContains("F7")
            compose.onNodeWithTag("key-binding-capture").assertDoesNotExist()
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun roomChooserSupportsNumbersAndRespectsLockedActions() {
        val directory = Files.createTempDirectory("starborn-action-picker-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        val locked = GenericAction("sealed console", "inspect")
        val available = GenericAction("locker", "inspect")
        var selected: InlineActionTarget? = null
        try {
            val ui = ExplorationUiState(actions = listOf(locked, available),
                actionHints = mapOf(locked.actionKey() to ActionHintUi(true, "Requires a key")))
            compose.setContent { DesktopStarbornTheme(services) {
                DesktopRoomInteractionDialog(services, ui, onDismiss = {}, onSelect = { selected = it })
            } }
            compose.onNodeWithTag("room-interaction-0").assertIsNotEnabled()
            compose.onNodeWithTag("room-interaction-1").performKeyInput { pressKey(Key.One) }
            assertNull(selected)
            compose.onNodeWithTag("room-interaction-1").performKeyInput { pressKey(Key.Two) }
            assertEquals(InlineActionTarget.Room(available), selected)
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun remappedActionKeyAndQuickLoadWorkInExploration() {
        val directory = Files.createTempDirectory("starborn-pc-exploration-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setEnvironmentalEffectsQuality(com.example.starborn.data.local.EnvironmentalEffectsQuality.OFF)
                services.userSettingsStore.setKeyBinding(DesktopAction.ACTIONS, "K")
            }
            assertTrue(services.startDebugScenario("tut_npc_dialogue"))
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(1920.dp, 1080.dp)) { DesktopExplorationScreen(services, {}, {}, {}, {}, {}, {}) }
            } }
            compose.waitUntil(10000) { services.exploration.uiState.value.currentRoom?.id == "pit_jed_bunk" }
            compose.onNodeWithTag("key-legend-K").assertIsDisplayed()
            compose.onAllNodes(isRoot())[0].performKeyInput { pressKey(Key.K) }
            compose.onNodeWithText("Room interactions").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            compose.onAllNodes(isRoot())[0].performKeyInput { pressKey(Key.F9) }
            compose.onNodeWithText("No quicksave available").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            assertTrue(services.saveManager.saveGame(-1, services.sessionStore.state.value.copy(playerCredits = 111)))
            compose.onAllNodes(isRoot())[0].performKeyInput { pressKey(Key.F9) }
            compose.onNodeWithText("Load quicksave?").assertIsDisplayed()
            compose.onNodeWithText("Load quicksave").performClick()
            compose.waitUntil(10000) {
                compose.mainClock.advanceTimeBy(100)
                services.sessionStore.state.value.playerCredits == 111
            }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
