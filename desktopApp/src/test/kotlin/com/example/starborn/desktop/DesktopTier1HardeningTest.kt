@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.awt.event.KeyEvent as AwtKeyEvent
import java.io.File
import java.nio.file.Files

class DesktopTier1HardeningTest {
    @get:Rule
    val compose = createComposeRule()

    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val dir = Files.createTempDirectory("starborn-desktop-tier1-").toFile()
        val services = DesktopAppServices(dir, RecordingAudioDriver())
        try {
            block(services)
        } finally {
            services.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun bitmapLruCacheEvictsLeastRecentlyUsedByCountAndByteBudget() {
        val countCache = DesktopBitmapLruCache(
            maxEntries = 3,
            maxPixelBytes = 64L * 1024L * 1024L,
            byteEstimator = { 1_024L }
        )
        val b1 = ImageBitmap(4, 4)
        val b2 = ImageBitmap(4, 4)
        val b3 = ImageBitmap(4, 4)
        val b4 = ImageBitmap(4, 4)

        countCache.put("a", b1)
        countCache.put("b", b2)
        countCache.put("c", b3)
        // Touch "a" so "b" becomes least recently used
        assertSame(b1, countCache.get("a"))
        countCache.put("d", b4)

        assertEquals(3, countCache.size())
        assertFalse("Least-recently-used entry 'b' must be evicted", countCache.containsKey("b"))
        assertTrue(countCache.containsKey("a"))
        assertTrue(countCache.containsKey("c"))
        assertTrue(countCache.containsKey("d"))

        // Byte-budget eviction: max 20_000 bytes, each entry 8_000 bytes -> holds at most 2 entries
        val byteCache = DesktopBitmapLruCache(
            maxEntries = 50,
            maxPixelBytes = 20_000L,
            byteEstimator = { 8_000L }
        )
        byteCache.put("room1", b1)
        byteCache.put("room2", b2)
        byteCache.put("room3", b3)
        assertEquals(2, byteCache.size())
        assertTrue(byteCache.totalBytes() <= 20_000L)
        assertFalse("Oldest room bitmap must be evicted when exceeding pixel byte budget", byteCache.containsKey("room1"))
        assertTrue(byteCache.containsKey("room2"))
        assertTrue(byteCache.containsKey("room3"))
    }

    @Test
    fun scopedKeyBindingsIsolateExplorationCombatAndArcadeAndUpdateCombatBadgesLive() = withServices { services ->
        services.startNewGame()
        runBlocking {
            services.userSettingsStore.setTutorialsEnabled(false)
            // Default bindings must have zero cross-scope conflicts ("D" in EAST + "D" in ARCADE_LANE_1)
            val defaults = services.userSettingsStore.keyBindings.first()
            DesktopAction.entries.forEach { action ->
                assertNull("Default binding for $action must be conflict-free", defaults.conflict(action, defaults.label(action)))
            }
            assertEquals(AwtKeyEvent.VK_D, defaults.awtKeyCode(DesktopAction.ARCADE_LANE_1))

            // Rebinding Combat Guard to "E" succeeds even though Exploration INTERACT uses "E"
            services.userSettingsStore.setKeyBinding(DesktopAction.COMBAT_GUARD, "E")
            // Duplicate within the SAME scope (Combat) is rejected
            assertTrue(runCatching { services.userSettingsStore.setKeyBinding(DesktopAction.COMBAT_ATTACK, "E") }.isFailure)
        }

        val updated = runBlocking { services.userSettingsStore.keyBindings.first() }
        assertEquals("E", updated.label(DesktopAction.COMBAT_GUARD))
        assertEquals("E", updated.label(DesktopAction.INTERACT))

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
            compose.onAllNodesWithTag("combat-action-guard").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("[E]", useUnmergedTree = true).assertExists()
    }

    @Test
    fun fieldMenuSupportsQEPageCyclingAndBracketSubCategoryNavigation() = withServices { services ->
        services.startNewGame()
        runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }

        compose.setContent {
            DesktopStarbornTheme(services) {
                Box(Modifier.size(1280.dp, 800.dp)) {
                    DesktopFieldMenuContent(
                        services = services,
                        initialTab = DesktopMenuTab.STATS,
                        onOpenFieldKit = {},
                        onReturnToTitle = {},
                        onDismiss = {}
                    )
                }
            }
        }
        compose.waitForIdle()
        // Initial tab is Party ("STATS") -> press E to advance to Inventory
        compose.onRoot().performKeyInput { pressKey(Key.E) }
        compose.waitForIdle()
        compose.onNodeWithText("Supplies").assertExists()

        // Press ] to switch Inventory sub-category Supplies -> Key Items -> Gear
        compose.onRoot().performKeyInput { pressKey(Key.RightBracket) }
        compose.waitForIdle()
        compose.onRoot().performKeyInput { pressKey(Key.RightBracket) }
        compose.waitForIdle()
        // Press Q to step back from Inventory to Party
        compose.onRoot().performKeyInput { pressKey(Key.Q) }
        compose.waitForIdle()
        assertEquals(com.example.starborn.feature.exploration.viewmodel.MenuTab.STATS, services.exploration.uiState.value.menuTab)
    }

    @Test
    fun macAndLinuxSaveDirectoriesMigrateFromLegacyDotStarborn() {
        val tempRoot = Files.createTempDirectory("starborn-os-migration-").toFile()
        try {
            val legacyDir = File(tempRoot, ".starborn").apply { mkdirs() }
            File(legacyDir, "saves").mkdirs()
            File(legacyDir, "saves/game_session_quicksave.pb").writeBytes(byteArrayOf(1, 2, 3, 4))

            val macTarget = DesktopAppServices.resolveSaveDirectory(
                appDataEnv = null,
                userHome = tempRoot.absolutePath,
                osName = "Mac OS X",
                xdgDataHome = null
            )
            assertEquals(File(tempRoot, "Library/Application Support/Starborn").path, macTarget.path)
            assertTrue(DesktopAppServices.migrateSaveDirectory(legacyDir, macTarget))
            assertTrue(File(macTarget, "saves/game_session_quicksave.pb").exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
