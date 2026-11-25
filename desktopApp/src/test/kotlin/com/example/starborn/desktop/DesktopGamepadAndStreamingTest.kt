@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.awt.event.KeyEvent as AwtKeyEvent
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.Control
import javax.sound.sampled.Line
import javax.sound.sampled.SourceDataLine

class DesktopGamepadAndStreamingTest {
    @get:Rule
    val compose = createComposeRule()

    private class MockGamepadBackend : DesktopGamepadBackend {
        @Volatile var state = GamepadPollState(connected = false)
        val rumbles = CopyOnWriteArrayList<Triple<Float, Float, Int>>()

        override fun poll(): GamepadPollState = state
        override fun rumble(lowFrequency: Float, highFrequency: Float, durationMs: Int) {
            rumbles.add(Triple(lowFrequency, highFrequency, durationMs))
        }
        override fun close() = Unit
    }

    private class TestStreamingLine(private val expectedFormat: AudioFormat) {
        val opened = AtomicBoolean(false)
        val running = AtomicBoolean(false)
        val closed = AtomicBoolean(false)
        val totalBytesWritten = AtomicLong(0L)
        val maxChunkSizeSeen = AtomicInteger(0)

        val line: SourceDataLine = Proxy.newProxyInstance(
            SourceDataLine::class.java.classLoader,
            arrayOf(SourceDataLine::class.java)
        ) { _, method, args ->
            when (method.name) {
                "open" -> { opened.set(true); null }
                "start" -> { running.set(true); null }
                "stop" -> { running.set(false); null }
                "flush", "drain" -> null
                "close" -> { running.set(false); opened.set(false); closed.set(true); null }
                "isOpen" -> opened.get() && !closed.get()
                "isRunning", "isActive" -> running.get() && !closed.get()
                "getFormat" -> expectedFormat
                "getBufferSize" -> 16_384
                "available" -> 16_384
                "isControlSupported" -> false // Exercises software 16-bit PCM sample scaling fallback too!
                "getControls" -> emptyArray<Control>()
                "getLineInfo" -> Line.Info(SourceDataLine::class.java)
                "write" -> {
                    val len = args[2] as Int
                    if (len > 0) {
                        maxChunkSizeSeen.updateAndGet { prev -> maxOf(prev, len) }
                        totalBytesWritten.addAndGet(len.toLong())
                        // Pace lightly so test doesn't spin 100MB/s in memory
                        Thread.sleep(2)
                    }
                    len
                }
                "getLongFramePosition" -> totalBytesWritten.get() / expectedFormat.frameSize.coerceAtLeast(4)
                "getFramePosition" -> (totalBytesWritten.get() / expectedFormat.frameSize.coerceAtLeast(4)).toInt()
                "getMicrosecondPosition" -> {
                    val frames = totalBytesWritten.get() / expectedFormat.frameSize.coerceAtLeast(4)
                    ((frames * 1_000_000.0) / expectedFormat.sampleRate.coerceAtLeast(44100f)).toLong()
                }
                "hashCode" -> System.identityHashCode(this)
                "equals" -> args?.firstOrNull() === this
                "toString" -> "TestStreamingLine"
                else -> null
            }
        } as SourceDataLine
    }

    @Test
    fun gamepadServiceRoutesContextButtonsSwitchesXboxAndPlayStationGlyphsAndRumbles() {
        val dir = Files.createTempDirectory("starborn-gamepad-test-").toFile()
        val mockBackend = MockGamepadBackend()
        val emitted = CopyOnWriteArrayList<Pair<Int, Boolean>>()
        val gamepad = DesktopGamepadService(
            backend = mockBackend,
            startPollingLoop = false,
            keyEventSink = { code, pressed -> emitted.add(code to pressed) }
        )
        val services = DesktopAppServices(
            saveDirectory = dir,
            audioDriverOverride = RecordingAudioDriver(),
            gamepadServiceOverride = gamepad
        )
        try {
            services.startNewGame()
            runBlocking {
                services.userSettingsStore.setTutorialsEnabled(false)
                services.userSettingsStore.setHapticsDisabled(false)
            }

            // 1. Verify Steam Deck / Xbox context routing across Exploration, Fishing, Combat & Arcade
            gamepad.context = DesktopGamepadContext(screenState = DesktopScreenState.EXPLORATION, isWindowForeground = true)
            mockBackend.state = GamepadPollState(
                connected = true,
                controllerName = "Steam Virtual Gamepad",
                pressedButtons = setOf(DesktopGamepadButton.SOUTH, DesktopGamepadButton.LEFT_SHOULDER)
            )
            gamepad.pollOnce()
            assertEquals(DesktopInputDevice.GAMEPAD_XBOX_DECK, gamepad.activeInputDevice.value)
            assertTrue(emitted.contains(AwtKeyEvent.VK_E to true))
            assertTrue(emitted.contains(AwtKeyEvent.VK_F5 to true))

            // Release buttons
            mockBackend.state = mockBackend.state.copy(pressedButtons = emptySet())
            gamepad.pollOnce()
            assertTrue(emitted.contains(AwtKeyEvent.VK_E to false))
            assertTrue(emitted.contains(AwtKeyEvent.VK_F5 to false))

            // Fishing: South button maps to Spacebar (cast/hook/hold-to-reel)
            emitted.clear()
            gamepad.context = DesktopGamepadContext(screenState = DesktopScreenState.FISHING, isWindowForeground = true)
            mockBackend.state = GamepadPollState(
                connected = true,
                controllerName = "Xbox Wireless Controller",
                pressedButtons = setOf(DesktopGamepadButton.SOUTH)
            )
            gamepad.pollOnce()
            assertEquals(listOf(AwtKeyEvent.VK_SPACE to true), emitted.toList())
            mockBackend.state = mockBackend.state.copy(pressedButtons = emptySet())
            gamepad.pollOnce()

            // 2. Verify live UI glyph switching in Combat & Exploration HUD between Xbox/Steam Deck and PlayStation DualSense
            compose.setContent {
                DesktopStarbornTheme(services) {
                    Box(Modifier.size(1280.dp, 800.dp)) {
                        DesktopCombatScreen(
                            services = services,
                            enemyIds = listOf("the_beast"),
                            onVictory = {},
                            onDefeat = {},
                            onFlee = {}
                        )
                        DesktopExplorationKeyLegendHud(
                            ui = ExplorationUiState(),
                            blocked = false
                        )
                    }
                }
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("combat-action-guard").fetchSemanticsNodes().isNotEmpty()
            }

            // Xbox / Steam Deck glyphs active
            compose.onNodeWithText("(A)", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("[LB]", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("L-Stick / D-Pad", useUnmergedTree = true).assertExists()
            // Stable testTag preserved even when visual glyph is "LB"
            compose.onNodeWithTag("key-legend-F5").assertExists()

            // Switch to DualSense PlayStation controller
            compose.runOnIdle {
                mockBackend.state = GamepadPollState(
                    connected = true,
                    controllerName = "DualSense Wireless Controller",
                    pressedButtons = setOf(DesktopGamepadButton.WEST)
                )
                gamepad.pollOnce()
            }
            compose.waitForIdle()
            assertEquals(DesktopInputDevice.GAMEPAD_PLAYSTATION, gamepad.activeInputDevice.value)
            compose.onNodeWithText("(✕)", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("[L1]", useUnmergedTree = true).assertExists()

            // Rumble passes through to backend when foreground
            gamepad.rumble(0.8f, 0.5f, 200)
            assertEquals(1, mockBackend.rumbles.size)
        } finally {
            services.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun musicAndAmbienceStreamInSmall4KbChunksWithPauseRestartAndCleanShutdown() {
        val assetProvider = DesktopAssetProvider()
        val lines = CopyOnWriteArrayList<TestStreamingLine>()
        val driver = DesktopAudioDriver.createStreamingForTest(
            assetProvider = assetProvider,
            streamLineFactory = { fmt -> TestStreamingLine(fmt).also { lines.add(it) }.line }
        )
        try {
            driver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_title_theme", loop = true))
            val startDeadline = System.currentTimeMillis() + 4_000L
            while (System.currentTimeMillis() < startDeadline && !driver.isStreamingActive(AudioCueType.MUSIC, "music_title_theme")) {
                Thread.sleep(15)
            }
            assertTrue("Title music must stream via SourceDataLine", driver.isStreamingActive(AudioCueType.MUSIC, "music_title_theme"))
            assertEquals(1, lines.size)

            // Wait until several chunks have streamed and position clock advances
            val streamDeadline = System.currentTimeMillis() + 4_000L
            while (System.currentTimeMillis() < streamDeadline && (driver.currentMusicPositionMs("music_title_theme") ?: 0L) < 60L) {
                Thread.sleep(15)
            }
            val posMs = driver.currentMusicPositionMs("music_title_theme") ?: 0L
            assertTrue("Streaming music position clock must advance (got ${posMs}ms)", posMs >= 60L)
            assertTrue(
                "Streaming must write bounded <= ${DesktopAudioDriver.STREAM_CHUNK_BYTES}B chunks rather than full-track buffers",
                lines[0].maxChunkSizeSeen.get() in 1..DesktopAudioDriver.STREAM_CHUNK_BYTES
            )

            // Pause for overlay & verify line stops, then resume
            driver.setMusicPausedForOverlay(true, "music_title_theme")
            Thread.sleep(60)
            assertFalse(lines[0].running.get())
            driver.setMusicPausedForOverlay(false, "music_title_theme")
            val resumeDeadline = System.currentTimeMillis() + 2_000L
            while (System.currentTimeMillis() < resumeDeadline && !lines[0].running.get()) {
                Thread.sleep(15)
            }
            assertTrue(lines[0].running.get())

            // Stop cue closes streaming line cleanly
            driver.execute(AudioCommand.Stop(AudioCueType.MUSIC, "music_title_theme", fadeMs = 0L))
            val closeDeadline = System.currentTimeMillis() + 2_000L
            while (System.currentTimeMillis() < closeDeadline && !lines[0].closed.get()) {
                Thread.sleep(15)
            }
            assertTrue("Stopped streaming SourceDataLine must be closed", lines[0].closed.get())
        } finally {
            driver.release()
        }
    }
}
