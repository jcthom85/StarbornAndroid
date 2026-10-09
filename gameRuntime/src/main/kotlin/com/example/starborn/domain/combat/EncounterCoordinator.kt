package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Drop
import java.util.concurrent.atomic.AtomicReference

class EncounterCoordinator {
    private val pendingDescriptor = AtomicReference<EncounterDescriptor?>(null)
    private val activeSourcePartyId = AtomicReference<String?>(null)

    fun setPendingEncounter(descriptor: EncounterDescriptor) {
        pendingDescriptor.set(descriptor)
    }

    fun consumePendingEncounter(): EncounterDescriptor? {
        val descriptor = pendingDescriptor.getAndSet(null)
        activeSourcePartyId.set(descriptor?.sourcePartyId)
        return descriptor
    }

    fun currentSourcePartyId(): String? = activeSourcePartyId.get()

    fun clear() {
        pendingDescriptor.set(null)
        activeSourcePartyId.set(null)
    }
}

data class EncounterDescriptor(
    val enemies: List<EncounterEnemyInstance>,
    val sourcePartyId: String? = null
)

data class EncounterEnemyInstance(
    val enemyId: String,
    val hp: Int? = null,
    val vitality: Int? = null,
    val stability: Int? = null,
    val overrideDrops: List<Drop>? = null,
    val extraDrops: List<Drop> = emptyList()
)
