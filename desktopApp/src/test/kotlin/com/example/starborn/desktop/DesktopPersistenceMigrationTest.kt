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
}
