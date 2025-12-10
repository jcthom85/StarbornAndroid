package com.example.starborn.domain.session

import com.example.starborn.di.AppServices

class GameSaveRepository(private val services: AppServices) {

    private val slotIndices = 1..3

    suspend fun slotInfos(): List<Pair<Int, GameSessionSlotInfo?>> {
        return slotIndices.map { slot ->
            slot to services.slotInfo(slot)
        }
    }

    suspend fun save(slot: Int) {
        services.saveSlot(slot)
    }

    suspend fun load(slot: Int): Boolean {
        return services.loadSlot(slot)
    }

    suspend fun quickSave(): Boolean = services.quickSave()

    suspend fun loadQuickSave(): Boolean = services.loadQuickSave()

    suspend fun quickSaveInfo(): GameSessionSlotInfo? = services.quickSaveInfo()

    suspend fun clearQuickSave() {
        services.clearQuickSave()
    }

    suspend fun clearAutosave() {
        services.clearAutosave()
    }

    suspend fun clear(slot: Int) {
        services.clearSlot(slot)
    }

    companion object {
        const val QUICKSAVE_SLOT = -1
        const val AUTOSAVE_SLOT = 0
    }
}
