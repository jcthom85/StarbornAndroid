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
    companion object { const val QUICKSAVE_SLOT = -1; const val AUTOSAVE_SLOT = 0 }
}
