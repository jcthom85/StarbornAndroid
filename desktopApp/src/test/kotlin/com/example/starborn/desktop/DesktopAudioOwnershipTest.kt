package com.example.starborn.desktop

import com.example.starborn.domain.audio.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopAudioOwnershipTest {
    @Test fun recreatedExplorationControllersKeepOneAudioCollector() = runBlocking {
        val directory = Files.createTempDirectory("starborn-audio-owner-").toFile()
        val audio = RecordingAudioDriver()
        val services = DesktopAppServices(directory, audio)
        try {
            val cue = services.audioRouter.commandsForUi("menu_action").filterIsInstance<AudioCommand.Play>().single().cueId
            repeat(2) { iteration ->
                withContext(Dispatchers.Main) {
                    assertTrue(services.startNewGame())
                    services.exploration.onMenuActionInvoked()
                }
                withTimeout(5000) {
                    while (audio.commands.filterIsInstance<AudioCommand.Play>().count { it.cueId == cue } < iteration + 1) delay(10)
                }
                withContext(Dispatchers.Main) { yield() }
                assertEquals(iteration + 1, audio.commands.filterIsInstance<AudioCommand.Play>().count { it.cueId == cue })
            }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
