package com.example.starborn.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRouterTest {

    @Test
    fun commandsForRoomSwitchesMusicAndAmbientLayers() {
        val bindings = AudioBindings(
            music = mapOf(
                "hub_alpha" to "theme_alpha",
                "hub_bravo" to "theme_bravo"
            ),
            ambience = mapOf(
                "room_one" to "wind_gentle",
                "room_two" to "wind_fierce"
            )
        )
        val router = AudioRouter(bindings)

        val initialCommands = router.commandsForRoom(
            hubId = "hub_alpha",
            roomId = "room_one"
        )
        assertTrue(initialCommands.any { it is AudioCommand.Play && it.cueId == "theme_alpha" })
        assertTrue(initialCommands.any { it is AudioCommand.Play && it.cueId == "wind_gentle" })

        val redundantCommands = router.commandsForRoom(
            hubId = "hub_alpha",
            roomId = "room_one"
        )
        assertTrue("No new commands should be issued when nothing changes", redundantCommands.isEmpty())

        val switchedCommands = router.commandsForRoom(
            hubId = "hub_bravo",
            roomId = "room_two"
        )
        assertTrue(switchedCommands.any { it is AudioCommand.Stop && it.cueId == "theme_alpha" })
        assertTrue(switchedCommands.any { it is AudioCommand.Stop && it.cueId == "wind_gentle" })
        assertTrue(switchedCommands.any { it is AudioCommand.Play && it.cueId == "theme_bravo" })
        assertTrue(switchedCommands.any { it is AudioCommand.Play && it.cueId == "wind_fierce" })
    }

    @Test
    fun commandsForLayerOverrideStopsMusicImmediately() {
        val bindings = AudioBindings(
            music = mapOf("hub_alpha" to "theme_alpha")
        )
        val router = AudioRouter(bindings)
        router.commandsForRoom(hubId = "hub_alpha", roomId = null)

        val stopCommands = router.commandsForLayerOverride(
            layer = AudioCueType.MUSIC,
            stop = true
        )

        assertEquals(1, stopCommands.size)
        val stop = stopCommands.first() as AudioCommand.Stop
        assertEquals(AudioCueType.MUSIC, stop.type)
        assertEquals("theme_alpha", stop.cueId)
    }
}
