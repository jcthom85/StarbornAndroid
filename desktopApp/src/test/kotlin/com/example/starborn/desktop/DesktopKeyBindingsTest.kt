package com.example.starborn.desktop

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopKeyBindingsTest {
    @Test fun rejectsConflictsAndReservedKeysAndPersistsSwappedBindings() = runBlocking {
        val directory = Files.createTempDirectory("starborn-keys-").toFile()
        var store = DesktopUserSettingsStore(directory)
        try {
            assertNotNull(store.keyBindings.first().conflict(DesktopAction.INTERACT, "M"))
            assertNotNull(store.keyBindings.first().conflict(DesktopAction.INTERACT, "F11"))
            store.setKeyBinding(DesktopAction.NORTH, "F12")
            store.setKeyBinding(DesktopAction.INTERACT, "W")
            store.setKeyBinding(DesktopAction.NORTH, "E")
            store.closeAndJoin()
            store = DesktopUserSettingsStore(directory)
            val restored = store.keyBindings.first()
            assertEquals(Key.E, restored.key(DesktopAction.NORTH))
            assertEquals(Key.W, restored.key(DesktopAction.INTERACT))
            assertEquals(Key.DirectionUp, DesktopAction.NORTH.alias)
            assertTrue(runCatching { store.setKeyBinding(DesktopAction.HELP, "E") }.isFailure)
            assertEquals("H", store.keyBindings.first().label(DesktopAction.HELP))
            store.resetKeyBindings()
            assertEquals(DesktopKeyBindings(), store.keyBindings.first())
        } finally { store.closeAndJoin(); directory.deleteRecursively() }
    }
}
