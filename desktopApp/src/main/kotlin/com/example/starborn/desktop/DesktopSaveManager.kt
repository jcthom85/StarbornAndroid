package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

/** Desktop adapter over the same protocol, atomic writes and backups used by Android. */
class DesktopSaveManager(
    saveDirectory: File,
    private val roomTitleFor: (String?) -> String? = { it }
) {
    private val savesDir = File(saveDirectory, "saves").apply { mkdirs() }
    private val persistence = GameSessionPersistence(savesDir)
    private val legacyAdapter = MoshiProvider.instance.adapter(GameSessionState::class.java)
    private val titles = mutableMapOf<String, String>()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private fun validateSlot(slot: Int) = require(slot in -1..3) { "Invalid save slot: $slot" }
    private fun newFile(slot: Int) = File(savesDir, when (slot) {
        -1 -> "game_session_quicksave.pb"
        0 -> "game_session_autosave.pb"
        else -> "game_session_slot$slot.pb"
    })
    private fun legacyFile(slot: Int) = File(savesDir, if (slot == 0) "autosave.json" else "slot_$slot.json")

    private suspend fun write(slot: Int, state: GameSessionState) {
        when (slot) {
            -1 -> persistence.writeQuickSave(state)
            0 -> persistence.writeAutosave(state)
            else -> persistence.writeSlot(slot, state)
        }
    }

    private suspend fun read(slot: Int): GameSessionSlotInfo? = when (slot) {
        -1 -> persistence.quickSaveInfo()
        0 -> persistence.autosaveInfo()
        else -> persistence.slotInfo(slot)
    }

    private suspend fun migrateLegacy(slot: Int) {
        if (slot < 0 || newFile(slot).exists()) return
        val legacy = legacyFile(slot)
        if (!legacy.exists()) return
        val original = legacy.readText()
        val imported = requireNotNull(legacyAdapter.fromJson(original)) { "Empty legacy save" }
            .migrateOpeningNarrativeState()
        val backup = File(savesDir, legacy.name + ".pre-migration.bak")
        if (!backup.exists()) legacy.copyTo(backup)
        write(slot, imported)
        check(read(slot)?.state == (imported.battleCheckpoint ?: imported)) { "Save migration verification failed" }
        // The original JSON is deliberately retained. A protocol save takes precedence.
    }

    @Synchronized
    fun saveGame(slotIndex: Int, state: GameSessionState, roomTitle: String? = null): Boolean = attempt(false) {
        validateSlot(slotIndex)
        migrateLegacy(slotIndex)
        write(slotIndex, state)
        if (roomTitle != null && state.roomId != null) titles[requireNotNull(state.roomId)] = roomTitle
        true
    }

    @Synchronized
    fun loadGame(slotIndex: Int): GameSessionState? = attempt(null) {
        validateSlot(slotIndex)
        val existed = newFile(slotIndex).exists() || legacyFile(slotIndex).exists()
        migrateLegacy(slotIndex)
        val info = read(slotIndex)
        check(!existed || info != null) { "Save is unreadable; retained files are available for recovery" }
        info?.state?.migrateOpeningNarrativeState()
    }

    @Synchronized
    fun getSlotMetadata(slotIndex: Int): DesktopSaveSlotInfo? = attempt(null) {
        validateSlot(slotIndex)
        migrateLegacy(slotIndex)
        read(slotIndex)?.let { info ->
            val state = info.state
            val timestamp = info.savedAtMillis ?: 0L
            DesktopSaveSlotInfo(slotIndex, timestamp,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp)),
                state.roomId, titles[state.roomId] ?: roomTitleFor(state.roomId), state.playerLevel,
                state.playerCredits, state.activeQuests.size, state.completedQuests.size)
        }
    }

    @Synchronized
    fun slotInfo(slotIndex: Int): GameSessionSlotInfo? = attempt(null) {
        validateSlot(slotIndex)
        migrateLegacy(slotIndex)
        read(slotIndex)
    }

    @Synchronized
    fun deleteSlot(slotIndex: Int): Boolean = attempt(false) {
        validateSlot(slotIndex)
        // Remove legacy files too so explicitly deleted slots are not re-imported.
        when (slotIndex) {
            -1 -> persistence.clearQuickSave()
            0 -> persistence.clearAutosave()
            else -> persistence.clearSlot(slotIndex)
        }
        legacyFile(slotIndex).takeIf { it.exists() }?.let { check(it.delete()) }
        File(savesDir, legacyFile(slotIndex).nameWithoutExtension + ".meta.json").takeIf { it.exists() }?.delete()
        true
    }

    fun hasSave(slotIndex: Int): Boolean {
        validateSlot(slotIndex)
        return newFile(slotIndex).exists() || (slotIndex >= 0 && legacyFile(slotIndex).exists())
    }

    private fun <T> attempt(fallback: T, action: suspend () -> T): T = try {
        val value = runBlocking(Dispatchers.IO) { action() }
        _lastError.value = null
        value
    } catch (error: Exception) {
        _lastError.value = error.message ?: error.javaClass.simpleName
        System.err.println("Starborn save error: ${_lastError.value}")
        fallback
    }
}
