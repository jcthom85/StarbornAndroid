package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.combat.*
import com.example.starborn.feature.combat.viewmodel.AttackLungeStyle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.Clip

class DesktopElevationParityTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun combatTransitionOverlayCompletesEnterQuickCloseAndQuickOpenModes() {
        val dir = Files.createTempDirectory("desktop-transition-parity").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        var mode by mutableStateOf(TransitionMode.ENTER)
        val completions = AtomicInteger(0)
        try {
            compose.setContent {
                DesktopStarbornTheme(services) {
                    Box(Modifier.size(1280.dp, 800.dp)) {
                        DesktopCombatTransitionOverlay(
                            visible = true,
                            mode = mode,
                            mainText = if (mode == TransitionMode.ENTER) "HOSTILE CONTACT" else "",
                            subText = "",
                            holdUntilReady = false,
                            suppressFlashes = false,
                            onFinished = { completions.incrementAndGet() }
                        )
                    }
                }
            }

            compose.onNodeWithTag("desktop-combat-transition").assertExists()
            compose.onNodeWithText("HOSTILE CONTACT").assertExists()
            compose.waitUntil(5_000) { completions.get() >= 1 }

            compose.runOnIdle { mode = TransitionMode.QUICK_CLOSE }
            compose.waitUntil(3_000) { completions.get() >= 2 }

            compose.runOnIdle { mode = TransitionMode.QUICK_OPEN }
            compose.waitUntil(3_000) { completions.get() >= 3 }
        } finally {
            compose.runOnIdle { services.close() }
            dir.deleteRecursively()
        }
    }

    @Test
    fun ascendedSilenceOrdersLinkedTargetsLeftToRightAndScalesStage() {
        val dir = Files.createTempDirectory("desktop-silence-parity").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            val enemyDefs = services.worldDataSource.loadEnemies().associateBy { it.id }
            val ids = listOf("ascended_god", "silence_voice_anchor", "silence_memory_anchor")
            val members = ids.map { id ->
                val def = requireNotNull(enemyDefs[id]) { "Missing $id in enemies.json" }
                CombatantState(
                    Combatant(id, def.name, CombatSide.ENEMY, StatBlock(def.hp, 12, 10, 10, 10, 5, 10, 120)),
                    def.hp,
                    120
                )
            }
            val state = CombatState(emptyList(), 0, members.associateBy { it.combatant.id })
            var wide by mutableStateOf(true)

            compose.setContent {
                DesktopStarbornTheme(services) {
                    Box(Modifier.size(if (wide) 1024.dp else 390.dp, 520.dp)) {
                        DesktopBattleFormation(
                            members = members,
                            services = services,
                            modifier = Modifier.size(if (wide) 1024.dp else 390.dp, 520.dp),
                            enemySide = true,
                            sprite = { id -> enemyDefs[id]?.sprite?.firstOrNull() ?: enemyDefs[id]?.portrait },
                            selectedId = "ascended_god",
                            validTargets = ids.toSet(),
                            targeting = false,
                            meters = emptyMap(),
                            intents = emptyMap(),
                            feedback = emptyMap(),
                            cues = emptyList(),
                            lungeStyle = AttackLungeStyle.CAST,
                            lungeActor = null,
                            lungeToken = 0L,
                            missActor = null,
                            missToken = 0L,
                            authoredState = state,
                            enemySize = { id -> enemyDefs[id]?.combatSize ?: "massive" },
                            enemyWidth = { id -> enemyDefs[id]?.combatWidth ?: 1f },
                            composite = { id -> enemyDefs[id]?.composite },
                            onSelect = {}
                        )
                    }
                }
            }

            for (isWide in listOf(true, false)) {
                compose.runOnIdle { wide = isWide }
                compose.waitForIdle()
                val voiceBounds = compose.onNodeWithTag("combat-linked-target-silence_voice_anchor").fetchSemanticsNode().boundsInRoot
                val godBounds = compose.onNodeWithTag("combat-linked-target-ascended_god").fetchSemanticsNode().boundsInRoot
                val memoryBounds = compose.onNodeWithTag("combat-linked-target-silence_memory_anchor").fetchSemanticsNode().boundsInRoot
                assertTrue(
                    "Expected Voice Anchor left of Ascended: Silence left of Memory Anchor (wide=$isWide): $voiceBounds, $godBounds, $memoryBounds",
                    voiceBounds.left < godBounds.left && godBounds.left < memoryBounds.left
                )
                val godFigure = compose.onNodeWithTag("combat-figure-ascended_god", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertTrue("Expected commanding Silence figure height (got ${godFigure.height})", godFigure.height >= 250f)
            }
        } finally {
            compose.runOnIdle { services.close() }
            dir.deleteRecursively()
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun vantageRadarAndEscapePauseOverlayWorkInDesktopCombat() {
        val dir = Files.createTempDirectory("desktop-vantage-pause").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            services.startNewGame()
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }

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
                    }
                }
            }

            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("combat-affinity-radar").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Not discovered").assertExists()

            // The Source Beast has Burn (-100 Weakness) and Shock (-50 Vantage)
            compose.runOnIdle {
                services.sessionStore.discoverEnemyWeakness("the_beast", "burn")
                services.sessionStore.discoverEnemyWeakness("the_beast", "shock")
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Vantage (1.5× Stability · -1 CD)").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Weaknesses (2× Stability · -1 CD)").assertExists()
            compose.onNodeWithText("Burn").assertExists()
            compose.onNodeWithText("Shock").assertExists()

            // Verify Pause / Settings button and [Esc] toggle
            compose.onNodeWithTag("combat-pause-button").assertExists()
            compose.onRoot().performKeyInput { pressKey(Key.Escape) }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("combat-pause-dialog").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("COMBAT PAUSED").assertExists()
            compose.onNodeWithTag("combat-pause-resume").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("combat-pause-dialog").fetchSemanticsNodes().isEmpty()
            }
        } finally {
            compose.runOnIdle { services.close() }
            dir.deleteRecursively()
        }
    }

    @Test
    fun partyCombatEmotePriorityAndAudioDriverMusicClock() {
        val dir = Files.createTempDirectory("desktop-emotes-audio").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            val stats = StatBlock(100, 12, 10, 10, 10, 5, 10, 100)
            val baseMember = CombatantState(Combatant("nova", "Nova", CombatSide.PLAYER, stats), 100, 100)
            val defaultIcon = "images/characters/nova_combat_icon.png"

            assertEquals(
                "images/characters/emotes/nova_down.png",
                resolveDesktopPartyCombatEmote("nova", baseMember.copy(hp = 0), defaultIcon, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_cool.png",
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, victoryEmotes = true, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_confident.png",
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, isMissLunge = true, isLunging = true, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_angry.png",
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, isLunging = true, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_gasping.png",
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, hasDamageCue = true, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_happy.png",
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, hasSupportCue = true, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_surprised.png",
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, hasTelegraphCue = true, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_worried.png",
                resolveDesktopPartyCombatEmote("nova", baseMember.copy(hp = 25), defaultIcon, assetProvider = services.assetProvider)
            )
            assertEquals(
                "images/characters/emotes/nova_confident.png",
                resolveDesktopPartyCombatEmote("nova", baseMember.copy(momentum = 2), defaultIcon, assetProvider = services.assetProvider)
            )
            assertEquals(
                defaultIcon,
                resolveDesktopPartyCombatEmote("nova", baseMember, defaultIcon, assetProvider = services.assetProvider)
            )
            // Unknown guest ally falls back cleanly to defaultIcon rather than missing path
            assertEquals(
                defaultIcon,
                resolveDesktopPartyCombatEmote("guest_ally", baseMember.copy(hp = 0), defaultIcon, assetProvider = services.assetProvider)
            )

            // Verify DesktopAudioDriver cue resolution & loop-wrapped music clock
            val positionUs = AtomicLong(45_250_000L) // 3.25s into second 42s loop
            val running = AtomicBoolean(true)
            val opened = AtomicBoolean(true)
            val mockClip = Proxy.newProxyInstance(
                Clip::class.java.classLoader,
                arrayOf(Clip::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "open" -> { opened.set(true); Unit }
                    "isOpen" -> opened.get()
                    "isRunning", "isActive" -> running.get()
                    "start", "loop" -> { running.set(true); Unit }
                    "stop" -> { running.set(false); Unit }
                    "close" -> { running.set(false); opened.set(false); Unit }
                    "getMicrosecondLength" -> 42_000_000L
                    "getMicrosecondPosition" -> positionUs.get()
                    "setMicrosecondPosition" -> { positionUs.set((args?.getOrNull(0) as? Long) ?: 0L); Unit }
                    "getFrameLength" -> 44100 * 42
                    "getFramePosition" -> 0
                    "isControlSupported" -> false
                    else -> when (method.returnType) {
                        java.lang.Boolean.TYPE -> false
                        java.lang.Integer.TYPE -> 0
                        java.lang.Long.TYPE -> 0L
                        java.lang.Float.TYPE -> 0f
                        else -> null
                    }
                }
            } as Clip

            val driver = DesktopAudioDriver(services.assetProvider, prewarmDefaults = false) { mockClip }
            try {
                assertTrue(driver.hasCue("sfx_combat_transition_slam"))
                assertTrue(driver.hasCue("music_arcade_harmonic_pulse"))

                driver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_arcade_harmonic_pulse", loop = true, fadeMs = 0L))
                val deadline = System.currentTimeMillis() + 5_000L
                while (driver.currentMusicPositionMs("music_arcade_harmonic_pulse") == null && System.currentTimeMillis() < deadline) {
                    Thread.sleep(25)
                }
                assertEquals(0L, driver.currentMusicPositionMs("music_arcade_harmonic_pulse"))

                // Simulate 45.25s elapsed across a 42s looping Clip (3.25s into loop 2)
                positionUs.set(45_250_000L)
                assertEquals(3_250L, driver.currentMusicPositionMs("music_arcade_harmonic_pulse"))

                driver.setMusicPausedForOverlay(true, "music_arcade_harmonic_pulse")
                val pauseDeadline = System.currentTimeMillis() + 2_000L
                while (running.get() && System.currentTimeMillis() < pauseDeadline) {
                    Thread.sleep(20)
                }
                assertFalse(running.get())

                driver.setMusicPausedForOverlay(false, "music_arcade_harmonic_pulse")
                val resumeDeadline = System.currentTimeMillis() + 2_000L
                while (!running.get() && System.currentTimeMillis() < resumeDeadline) {
                    Thread.sleep(20)
                }
                assertTrue(running.get())

                driver.restartCurrentMusicFromStart("music_arcade_harmonic_pulse")
                val restartDeadline = System.currentTimeMillis() + 2_000L
                while (driver.currentMusicPositionMs("music_arcade_harmonic_pulse") != 0L && System.currentTimeMillis() < restartDeadline) {
                    Thread.sleep(20)
                }
                assertEquals(0L, driver.currentMusicPositionMs("music_arcade_harmonic_pulse"))
            } finally {
                driver.release()
            }
        } finally {
            compose.runOnIdle { services.close() }
            dir.deleteRecursively()
        }
    }
}
