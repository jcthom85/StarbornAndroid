package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopRoomEntryRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dialogueDefaultCallbacksPlayHumanAndMachineVoices() {
        val directory = Files.createTempDirectory("desktop-voices").toFile()
        val audio = RecordingAudioDriver()
        val services = DesktopAppServices(directory, audio)
        var line by mutableStateOf(DialogueLine("human", "Jed", "Check the conduit before leaving."))
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme(services) {
                DesktopAuthoredDialogueOverlay(services, DialogueUi(line, null, null), emptyList(), {}, {}, {})
            } }
            compose.mainClock.advanceTimeBy(150)
            compose.waitForIdle()
            assertTrue(audio.commands.filterIsInstance<AudioCommand.Play>().any {
                it.type == AudioCueType.VOICE && it.cueId.startsWith("voice_murmur_male_")
            })
            compose.runOnIdle { line = DialogueLine("machine", "Sentinel-3", "The channel is clear.") }
            compose.mainClock.advanceTimeBy(150)
            compose.waitForIdle()
            assertTrue(audio.commands.filterIsInstance<AudioCommand.Play>().any {
                it.type == AudioCueType.VOICE && it.cueId.startsWith("voice_murmur_machine_")
            })
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun npcRowDoesNotShiftWhilePreviousRoomDescriptionFinishesAnimating() {
        val directory = Files.createTempDirectory("desktop-room-layout").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        val room = services.roomDefinitions.getValue("pit_jed_bunk")
        var current by mutableStateOf(room.copy(description = "A long corridor runs through the colony. ".repeat(35)))
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme(services) { DesktopExplorationTheme(services) {
                Box(Modifier.size(1000.dp, 640.dp)) {
                    val ui = ExplorationUiState(isLoading = false, forceBlackScreen = false, currentRoom = current, npcs = listOf("Jed"))
                    DesktopExplorationPanels(services, ui, portraitBackdropLayout(Size(1000f, 640f), Size(1088f, 1920f)),
                        current.description, null, false, false) {}
                }
            } } }
            compose.mainClock.advanceTimeBy(400)
            compose.runOnIdle { current = room.copy(id = "new_room", description = "A quiet bunk beside the corridor.") }
            compose.mainClock.advanceTimeByFrame()
            val first = compose.onNodeWithTag("npc-presence").fetchSemanticsNode().boundsInRoot
            compose.mainClock.advanceTimeBy(400)
            val settled = compose.onNodeWithTag("npc-presence").fetchSemanticsNode().boundsInRoot
            assertEquals(first.top, settled.top, .5f)
            assertEquals(first.left, settled.left, .5f)
        } finally { services.close(); directory.deleteRecursively() }
    }
}
