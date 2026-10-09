package com.example.starborn.domain.session

import com.example.starborn.domain.inventory.InventoryService

/** Synchronous publication prevents saves and immediate event conditions seeing stale inventory. */
class SessionInventoryBridge(
    private val session: GameSessionStore,
    private val inventory: InventoryService
) : AutoCloseable {
    private val listener: (Map<String, Int>) -> Unit = session::setInventory

    init { inventory.addSnapshotListener(listener) }

    fun restore(state: GameSessionState) {
        inventory.restore(state.inventory)
        session.restore(state.copy(inventory = inventory.snapshot()))
    }

    fun snapshot(): GameSessionState = session.state.value.copy(inventory = inventory.snapshot())

    override fun close() { inventory.removeSnapshotListener(listener) }
}
