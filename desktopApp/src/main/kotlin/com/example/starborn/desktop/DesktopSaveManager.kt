package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.session.GameSessionState
import com.squareup.moshi.JsonClass
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class DesktopSaveSlotInfo(
    val slotIndex: Int,
    val timestamp: Long,
    val formattedDate: String,
    val roomId: String?,
    val roomTitle: String?,
    val playerLevel: Int,
    val credits: Int,
    val activeQuestsCount: Int,
    val completedQuestsCount: Int
)

class DesktopSaveManager(
    private val saveDirectory: File
) {
    private val savesDir = File(saveDirectory, "saves").apply { mkdirs() }
    private val moshi = MoshiProvider.instance
    private val sessionAdapter = moshi.adapter(GameSessionState::class.java)
    private val metadataAdapter = moshi.adapter(DesktopSaveSlotInfo::class.java)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun saveGame(slotIndex: Int, state: GameSessionState, roomTitle: String? = null): Boolean {
        return try {
            val file = getSlotFile(slotIndex)
            val metaFile = getMetadataFile(slotIndex)
            val json = sessionAdapter.toJson(state)
            file.writeText(json)

            val meta = DesktopSaveSlotInfo(
                slotIndex = slotIndex,
                timestamp = System.currentTimeMillis(),
                formattedDate = dateFormat.format(Date()),
                roomId = state.roomId,
                roomTitle = roomTitle ?: state.roomId ?: "Unknown Sector",
                playerLevel = state.playerLevel,
                credits = state.playerCredits,
                activeQuestsCount = state.activeQuests.size,
                completedQuestsCount = state.completedQuests.size
            )
            metaFile.writeText(metadataAdapter.toJson(meta))
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun loadGame(slotIndex: Int): GameSessionState? {
        return try {
            val file = getSlotFile(slotIndex)
            if (!file.exists()) return null
            val json = file.readText()
            sessionAdapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    fun getSlotMetadata(slotIndex: Int): DesktopSaveSlotInfo? {
        return try {
            val metaFile = getMetadataFile(slotIndex)
            if (!metaFile.exists()) return null
            metadataAdapter.fromJson(metaFile.readText())
        } catch (_: Throwable) {
            null
        }
    }

    fun deleteSlot(slotIndex: Int): Boolean {
        val file = getSlotFile(slotIndex)
        val metaFile = getMetadataFile(slotIndex)
        return file.delete() && metaFile.delete()
    }

    fun hasSave(slotIndex: Int): Boolean {
        return getSlotFile(slotIndex).exists()
    }

    private fun getSlotFile(slotIndex: Int): File {
        val name = if (slotIndex == 0) "autosave.json" else "slot_$slotIndex.json"
        return File(savesDir, name)
    }

    private fun getMetadataFile(slotIndex: Int): File {
        val name = if (slotIndex == 0) "autosave.meta.json" else "slot_$slotIndex.meta.json"
        return File(savesDir, name)
    }
}
