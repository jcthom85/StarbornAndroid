package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File

class DesktopPersistenceMigrationTest {
    @Test fun legacySaveImportsWithoutDestroyingOriginalAndProtocolTakesPrecedence() {
        val root = Files.createTempDirectory("starborn-migration").toFile()
        try {
            val saves = File(root, "saves").apply { mkdirs() }
            val legacy = File(saves, "slot_1.json")
            val state = GameSessionState(roomId = "pit_jed_bunk", playerCredits = 231,
                inventory = mapOf("medkit" to 2))
            val json = MoshiProvider.instance.adapter(GameSessionState::class.java).toJson(state)
            legacy.writeText(json)
            val manager = DesktopSaveManager(root)
            val loaded = manager.loadGame(1)
            assertEquals(manager.lastError.value, 231, loaded?.playerCredits)
            assertEquals(mapOf("medkit" to 2), loaded?.inventory)
            assertEquals(json, legacy.readText())
            assertEquals(json, File(saves, "slot_1.json.pre-migration.bak").readText())
            assertTrue(manager.saveGame(1, requireNotNull(loaded).copy(playerCredits = 400)))
            assertEquals(400, DesktopSaveManager(root).loadGame(1)?.playerCredits)
        } finally { root.deleteRecursively() }
    }

    @Test fun explicitDeletionDoesNotReimportLegacySlot() {
        val root = Files.createTempDirectory("starborn-delete").toFile()
        try {
            val saves = File(root, "saves").apply { mkdirs() }
            File(saves, "slot_2.json").writeText(MoshiProvider.instance.adapter(GameSessionState::class.java)
                .toJson(GameSessionState(roomId = "pit_nova_bunk")))
            val manager = DesktopSaveManager(root)
            assertNotNull(manager.loadGame(2))
            assertTrue(manager.deleteSlot(2))
            assertFalse(manager.hasSave(2))
            assertNull(manager.loadGame(2))
        } finally { root.deleteRecursively() }
    }

    @Test fun malformedLegacySaveRemainsUntouchedAndReportsFailure() {
        val root = Files.createTempDirectory("starborn-corrupt").toFile()
        try {
            val saves = File(root, "saves").apply { mkdirs() }
            val legacy = File(saves, "slot_3.json").apply { writeText("{broken") }
            val manager = DesktopSaveManager(root)
            assertNull(manager.loadGame(3))
            assertNotNull(manager.lastError.value)
            assertEquals("{broken", legacy.readText())
            assertFalse(File(saves, "game_session_slot3.pb").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun saveDirectoryResolvesToAppDataOnWindowsAndUserHomeFallback() {
        val fakeAppData = "C:\\Users\\MockPlayer\\AppData\\Roaming"
        val fakeHome = "C:\\Users\\MockPlayer"

        val windowsResolved = DesktopAppServices.resolveSaveDirectory(appDataEnv = fakeAppData, userHome = fakeHome)
        assertEquals(File(fakeAppData, "Starborn").path, windowsResolved.path)

        val fallbackResolved = DesktopAppServices.resolveSaveDirectory(appDataEnv = null, userHome = fakeHome)
        assertEquals(File(fakeHome, ".starborn").path, fallbackResolved.path)

        val blankAppDataResolved = DesktopAppServices.resolveSaveDirectory(appDataEnv = "   ", userHome = fakeHome)
        assertEquals(File(fakeHome, ".starborn").path, blankAppDataResolved.path)
    }

    @Test fun legacySaveDirectoryMigratesToTargetWithoutDataLoss() {
        val tempRoot = Files.createTempDirectory("starborn-dir-migration-").toFile()
        try {
            val legacyDir = File(tempRoot, ".starborn").apply { mkdirs() }
            val targetDir = File(tempRoot, "AppData/Roaming/Starborn")

            // Seed legacy saves and settings
            val legacySavesDir = File(legacyDir, "saves").apply { mkdirs() }
            val legacySaveFile = File(legacySavesDir, "slot_0.json").apply {
                writeText("""{"roomId":"pit_command_deck","playerCredits":999}""")
            }
            val legacySettings = File(legacyDir, "user_settings.preferences_pb").apply {
                writeText("mock_proto_bytes")
            }

            // Run migration
            val result = DesktopAppServices.migrateSaveDirectory(source = legacyDir, target = targetDir)
            assertTrue("Expected migration to succeed", result)

            // Verify files exist in targetDir
            val targetSaveFile = File(targetDir, "saves/slot_0.json")
            val targetSettings = File(targetDir, "user_settings.preferences_pb")
            assertTrue("Target save file must exist", targetSaveFile.exists())
            assertTrue("Target settings file must exist", targetSettings.exists())
            assertEquals(legacySaveFile.readText(), targetSaveFile.readText())
            assertEquals(legacySettings.readText(), targetSettings.readText())

            // Verify original legacy files were preserved
            assertTrue("Original legacy save file must still exist", legacySaveFile.exists())
            assertTrue("Original legacy settings file must still exist", legacySettings.exists())

            // Running again on initialized target should be a safe no-op (returning false, not overwriting)
            val secondRun = DesktopAppServices.migrateSaveDirectory(source = legacyDir, target = targetDir)
            assertFalse("Second run should not re-migrate into populated directory", secondRun)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test fun windowBoundsEnforcesMinimumsAndPersistsState() = kotlinx.coroutines.runBlocking {
        val tempRoot = Files.createTempDirectory("starborn-window-bounds-").toFile()
        try {
            val store = DesktopUserSettingsStore(tempRoot)
            val defaults = store.initialWindowBounds()
            assertEquals(1280, defaults.width)
            assertEquals(800, defaults.height)
            assertFalse(defaults.isMaximized)

            // Attempt saving sub-minimum bounds (below 1024x700)
            store.saveWindowBounds(width = 800, height = 600, x = 150, y = 150, isMaximized = false)
            val constrained = store.initialWindowBounds()
            assertEquals("Width must remain unmutated when below 1024", 1280, constrained.width)
            assertEquals("Height must remain unmutated when below 700", 800, constrained.height)

            // Save valid bounds
            store.saveWindowBounds(width = 1600, height = 900, x = 200, y = 200, isMaximized = true)
            val updated = store.initialWindowBounds()
            assertTrue("Maximized state must be remembered", updated.isMaximized)
            store.close()
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}

