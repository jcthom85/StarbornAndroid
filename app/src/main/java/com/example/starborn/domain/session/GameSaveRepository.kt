package com.example.starborn.domain.session

import com.example.starborn.di.AppServices

class GameSaveRepository(private val services: AppServices) : SessionSaveRepository {

    private val slotIndices = 1..3

    override suspend fun slotInfos(): List<Pair<Int, GameSessionSlotInfo?>> {
        return slotIndices.map { slot ->
            slot to services.slotInfo(slot)
        }
    }

    override suspend fun save(slot: Int) {
        services.saveSlot(slot)
    }

    override suspend fun load(slot: Int): Boolean {
        return services.loadSlot(slot)
    }

    override suspend fun quickSave(): Boolean = services.quickSave()

    override suspend fun loadQuickSave(): Boolean = services.loadQuickSave()

    override suspend fun quickSaveInfo(): GameSessionSlotInfo? = services.quickSaveInfo()

    override suspend fun clearQuickSave() {
        services.clearQuickSave()
    }

    override suspend fun clearAutosave() {
        services.clearAutosave()
    }

    override suspend fun clear(slot: Int) {
        services.clearSlot(slot)
    }

    companion object {
        const val QUICKSAVE_SLOT = -1
        const val AUTOSAVE_SLOT = 0
    }
}
