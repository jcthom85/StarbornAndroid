package com.example.starborn.domain.session

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import org.junit.Assert.*
import org.junit.Test

class SessionInventoryBridgeTest {
    private fun inventory() = InventoryService(object : ItemCatalog {
        override fun load() {}
        override fun findItem(idOrAlias: String) = Item(id = idOrAlias, name = idOrAlias, type = "consumable")
    })

    @Test fun immediateSnapshotsIncludeAddedAndConsumedSupplies() {
        val inventory = inventory()
        val session = GameSessionStore()
        SessionInventoryBridge(session, inventory).use { bridge ->
            inventory.addItem("medkit", 2)
            assertEquals(2, session.state.value.inventory["medkit"])
            inventory.removeItem("medkit")
            assertEquals(1, bridge.snapshot().inventory["medkit"])
            inventory.removeItem("medkit")
            assertFalse(session.state.value.inventory.containsKey("medkit"))
        }
    }

    @Test fun restoringAnotherSessionCannotPublishPreviousInventoryLater() {
        val inventory = inventory()
        val session = GameSessionStore()
        SessionInventoryBridge(session, inventory).use { bridge ->
            inventory.addItem("old_item", 10)
            bridge.restore(GameSessionState(roomId = "next_room", inventory = mapOf("new_item" to 3)))
            assertEquals("next_room", session.state.value.roomId)
            assertEquals(mapOf("new_item" to 3), session.state.value.inventory)
            assertEquals(session.state.value.inventory, inventory.snapshot())
        }
    }

    @Test fun disposedBridgeStopsUpdatingSession() {
        val inventory = inventory()
        val session = GameSessionStore()
        val bridge = SessionInventoryBridge(session, inventory)
        bridge.close()
        inventory.addItem("medkit")
        assertTrue(session.state.value.inventory.isEmpty())
    }
}
