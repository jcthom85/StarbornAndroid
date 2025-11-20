package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Drop
import java.util.concurrent.atomic.AtomicReference

class EncounterCoordinator {
    private val pendingDescriptor = AtomicReference<EncounterDescriptor?>(null)

    fun setPendingEncounter(descriptor: EncounterDescriptor) {
        pendingDescriptor.set(descriptor)
    }

    fun consumePendingEncounter(): EncounterDescriptor? {
        return pendingDescriptor.getAndSet(null)
    }

    fun clear() {
        pendingDescriptor.set(null)
    }
}

data class EncounterDescriptor(
    val enemies: List<EncounterEnemyInstance>
)

data class EncounterEnemyInstance(
    val enemyId: String,
    val overrideDrops: List<Drop>? = null,
    val extraDrops: List<Drop> = emptyList()
)
