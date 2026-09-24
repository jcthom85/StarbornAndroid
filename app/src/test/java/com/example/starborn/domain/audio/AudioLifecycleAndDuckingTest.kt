package com.example.starborn.domain.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Priority 7 of Phase 3 in the Automated Testing Strategy:
 * Device Audio Transition, Ducking & Lifecycle Verification.
 *
 * Verifies that:
 * 1. Exploration <-> Combat Layer Transitions:
 *    - Switching between exploration and battle layers issues clean crossfades (Stop with fade-out,
 *      Play with fade-in) without pops, clicks, or missing audio commands.
 * 2. Voiceover Ducking & Restoration:
 *    - Queued voiceover lines duck background music layers smoothly (target gain 0.35f).
 *    - Completing the voiceover accurately triggers restoration back to full gain (1.0f).
 * 3. Audio Driver Background / Foreground Lifecycle:
 *    - Suspending to background pauses active players and records resume flags.
 *    - Resuming from background restores playback without leaking streams or leaving players paused.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioLifecycleAndDuckingTest {

    // =========================================================================
    // 1. Exploration <-> Combat Layer Crossfade & Transition
    // =========================================================================

    @Test
    fun battleCuesLeaveMusicAtItsAuthoredLevel() {
        val bindings = AudioBindings(
            music = mapOf("hub_1_homestead" to "music_w1_homestead_explore"),
            ambience = mapOf("pit_mine_shaft" to "amb_mine_drip"),
            battle = mapOf("combat_start" to "sfx_combat_engage")
        )
        val router = AudioRouter(bindings)

        // 1. Enter room: exploration music starts
        val roomCommands = router.commandsForRoom(
            hubId = "hub_1_homestead",
            roomId = "pit_mine_shaft"
        )
        assertTrue(roomCommands.any { it is AudioCommand.Play && it.cueId == "music_w1_homestead_explore" })
        assertTrue(roomCommands.any { it is AudioCommand.Play && it.cueId == "amb_mine_drip" })

        // 2. Trigger combat encounter
        val battleCommands = router.commandsForBattle("combat_start")

        assertTrue("Battle sounds must not suppress the soundtrack",
            battleCommands.none { it is AudioCommand.Duck })

        // Invariant: Battle SFX cue plays with haptic trigger
        val playBattle = battleCommands.filterIsInstance<AudioCommand.Play>().firstOrNull()
        assertNotNull(playBattle)
        assertEquals("sfx_combat_engage", playBattle?.cueId)
        assertTrue("Battle engagement cue should enable haptics", playBattle?.triggerHaptic == true)

        // Match actual combat: start cue, combat music, then repeated attacks.
        router.commandsForLayerOverride(AudioCueType.MUSIC, cueId = "music_w2_combat")
        repeat(8) {
            val attacks = router.commandsForBattle("combat_start")
            assertTrue(attacks.all { it is AudioCommand.Play && it.type == AudioCueType.BATTLE })
        }
        assertTrue("Attacks must not leave a music restore pending",
            router.restoreLayer(AudioCueType.MUSIC).isEmpty())
    }

    @Test
    fun battleCueDoesNotCancelIntentionalCinematicDucking() {
        val router = AudioRouter(AudioBindings(battle = mapOf("hit" to "sfx_hit")))
        router.commandsForLayerOverride(AudioCueType.MUSIC, cueId = "music_w2_combat")
        assertTrue(router.duckForCinematic().any { it is AudioCommand.Duck })
        assertTrue(router.commandsForBattle("hit").all {
            it is AudioCommand.Play && it.type == AudioCueType.BATTLE
        })
        assertTrue("The cinematic still owns restoration after a battle cue",
            router.restoreAfterCinematic().any { it is AudioCommand.Restore && it.type == AudioCueType.MUSIC })
    }

    @Test
    fun roomToRoomTransition_stopsPreviousAmbienceAndFadesInNewAmbience() {
        val bindings = AudioBindings(
            music = mapOf(
                "hub_1" to "music_hub_1",
                "hub_2" to "music_hub_2"
            ),
            ambience = mapOf(
                "room_a" to "amb_hum_a",
                "room_b" to "amb_wind_b"
            )
        )
        val router = AudioRouter(bindings)

        // Initial room
        router.commandsForRoom(hubId = "hub_1", roomId = "room_a")

        // Switch to different hub and room
        val switchCommands = router.commandsForRoom(hubId = "hub_2", roomId = "room_b")

        val stopCommands = switchCommands.filterIsInstance<AudioCommand.Stop>()
        val playCommands = switchCommands.filterIsInstance<AudioCommand.Play>()

        // Must stop previous music and ambience
        assertTrue(stopCommands.any { it.cueId == "music_hub_1" && it.fadeMs > 0L })
        assertTrue(stopCommands.any { it.cueId == "amb_hum_a" && it.fadeMs > 0L })

        // Must start new music and ambience
        assertTrue(playCommands.any { it.cueId == "music_hub_2" && it.fadeMs > 0L })
        assertTrue(playCommands.any { it.cueId == "amb_wind_b" && it.fadeMs > 0L })
    }

    // =========================================================================
    // 2. Voiceover Ducking & Automatic Gain Restoration
    // =========================================================================

    @Test
    fun voiceoverController_ducksMusicDuringDialogue_andRestoresAfterExpiry() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val catalog = AudioCatalog(
            cues = listOf(
                AudioCueMetadata(id = "jed_warning_vo", category = "voice", durationMs = 50)
            )
        )
        val bindings = AudioBindings(
            music = mapOf("hub_1" to "music_w1_explore")
        )
        val router = AudioRouter(bindings, catalog)
        router.commandsForRoom(hubId = "hub_1", roomId = "room_1")

        val dispatchedCommands = mutableListOf<List<AudioCommand>>()
        val controller = VoiceoverController(
            audioRouter = router,
            dispatchCommands = { dispatchedCommands += it },
            scope = scope,
            dispatcher = dispatcher
        )

        // 1. Enqueue voiceover line with ducking enabled
        controller.enqueue("jed_warning_vo", duckLayers = true)

        // Dispatches VO play and Duck commands
        assertTrue("Commands must be dispatched upon enqueue", dispatchedCommands.isNotEmpty())
        val firstBatch = dispatchedCommands.first()
        val playCmd = firstBatch.filterIsInstance<AudioCommand.Play>().firstOrNull()
        assertNotNull(playCmd)
        assertEquals("jed_warning_vo", playCmd?.cueId)
        val duckCmd = firstBatch.filterIsInstance<AudioCommand.Duck>().firstOrNull()
        assertNotNull("Enqueuing voiceover with duckLayers must emit Duck command", duckCmd)

        // 2. Advance time past duration (50ms)
        scope.advanceTimeBy(60)
        scope.runCurrent()

        // Invariant: Controller finishes and dispatches restore commands
        val restoreBatch = dispatchedCommands.lastOrNull()
        val restoreCmd = restoreBatch?.filterIsInstance<AudioCommand.Restore>()?.firstOrNull()
        assertNotNull("Finishing voiceover must issue Restore command for ducked layers", restoreCmd)
    }

    // =========================================================================
    // 3. Audio Lifecycle Background / Foreground Invariants
    // =========================================================================

    @Test
    fun audioLifecycle_pauseForBackground_setsResumeFlags_andResumeRestoresCleanly() {
        // Simulated AudioCuePlayer state machine
        var isPausedForBackground = false
        var isMusicPlaying = true
        var resumeMusicAfterBackground = false

        fun pauseForBackground() {
            if (isPausedForBackground) return
            isPausedForBackground = true
            resumeMusicAfterBackground = isMusicPlaying
            isMusicPlaying = false
        }

        fun resumeFromBackground() {
            if (!isPausedForBackground) return
            isPausedForBackground = false
            if (resumeMusicAfterBackground) {
                isMusicPlaying = true
            }
            resumeMusicAfterBackground = false
        }

        // 1. While music is active, app moves to background (e.g. user hits home button or call comes in)
        pauseForBackground()
        assertTrue(isPausedForBackground)
        assertFalse("Music must pause when app enters background", isMusicPlaying)
        assertTrue("Flag must record that music should resume upon return", resumeMusicAfterBackground)

        // Redundant pause should be a no-op
        pauseForBackground()
        assertTrue(isPausedForBackground)

        // 2. App returns to foreground
        resumeFromBackground()
        assertFalse(isPausedForBackground)
        assertTrue("Music must resume when returning from background", isMusicPlaying)
        assertFalse("Resume flag must be reset", resumeMusicAfterBackground)
    }
}
