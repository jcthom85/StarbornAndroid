package com.example.starborn.domain.session

interface SessionSaveRepository {
    suspend fun slotInfos(): List<Pair<Int, GameSessionSlotInfo?>>
    suspend fun save(slot: Int)
    suspend fun load(slot: Int): Boolean
    suspend fun quickSave(): Boolean
    suspend fun loadQuickSave(): Boolean
    suspend fun quickSaveInfo(): GameSessionSlotInfo?
    suspend fun clearQuickSave()
    suspend fun clearAutosave()
    suspend fun clear(slot: Int)
    companion object {
        const val QUICKSAVE_SLOT = -1
        const val AUTOSAVE_SLOT = 0
        // Temporary playtest capacity; change here to restore the normal numbered-slot limit.
        const val MANUAL_SLOT_COUNT = 20
        val MANUAL_SLOTS: IntRange get() = 1..MANUAL_SLOT_COUNT
        val ALL_SLOTS: IntRange get() = QUICKSAVE_SLOT..MANUAL_SLOT_COUNT
    }
}
