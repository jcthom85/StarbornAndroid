package com.example.starborn.feature.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.di.AppServices
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSaveRepository.Companion.AUTOSAVE_SLOT
import com.example.starborn.domain.session.GameSaveRepository.Companion.QUICKSAVE_SLOT
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaveSlotSummary(
    val slot: Int,
    val state: GameSessionState?,
    val title: String,
    val subtitle: String,
    val isEmpty: Boolean,
    val isAutosave: Boolean = false,
    val isQuickSave: Boolean = false,
    val savedAtMillis: Long? = null
)

class MainMenuViewModel(
    private val services: AppServices
) : ViewModel() {

    private val _slots = MutableStateFlow<List<SaveSlotSummary>>(emptyList())
    val slots: StateFlow<List<SaveSlotSummary>> = _slots.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val mainMenuTheme: Theme? = services.themeRepository.getTheme(MAIN_MENU_THEME_ID)
        ?: services.themeRepository.getTheme(DEFAULT_THEME_ID)

    init {
        refreshSlots()
    }

    fun refreshSlots() {
        viewModelScope.launch {
            val quicksave = runCatching { services.quickSaveInfo() }.getOrNull().toQuickSaveSummary()
            val autosave = runCatching { services.autosaveInfo() }.getOrNull().toAutosaveSummary()
            val summaries = (1..MAX_SLOTS).map { slot ->
                val info = runCatching { services.slotInfo(slot) }.getOrNull()
                info.toSummary(slot)
            }
            _slots.value = buildList {
                add(quicksave)
                add(autosave)
                addAll(summaries)
            }
        }
    }

    suspend fun loadSlot(slot: Int): Boolean {
        val success = when (slot) {
            AUTOSAVE_SLOT -> runCatching { services.loadAutosave() }.getOrElse { false }
            QUICKSAVE_SLOT -> runCatching { services.loadQuickSave() }.getOrElse { false }
            else -> runCatching { services.loadSlot(slot) }.getOrElse { false }
        }
        if (success) {
            services.syncInventoryFromSession()
        } else {
            val message = when (slot) {
                AUTOSAVE_SLOT -> "Unable to load autosave."
                QUICKSAVE_SLOT -> "Unable to load quicksave."
                else -> "Unable to load slot $slot."
            }
            emitMessage(message)
        }
        return success
    }

    fun startNewGame(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val success = services.startNewGame()
            if (success) {
                services.syncInventoryFromSession()
                onComplete?.invoke()
            } else {
                emitMessage("Failed to start new game. Check assets and logs.")
            }
        }
    }

    fun startNewGameWithFullInventory(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val success = services.startNewGame(debugFullInventory = true)
            if (success) {
                services.syncInventoryFromSession()
                onComplete?.invoke()
            } else {
                emitMessage("Failed to start debug new game.")
            }
        }
    }

    suspend fun saveSlot(slot: Int) {
        when (slot) {
            AUTOSAVE_SLOT -> emitMessage("Autosave is managed automatically.")
            QUICKSAVE_SLOT -> runCatching { services.quickSave() }
                .onSuccess {
                    emitMessage("Quicksave complete.")
                    refreshSlots()
                }
                .onFailure { emitMessage("Failed to quicksave.") }
            else -> runCatching { services.saveSlot(slot) }
                .onSuccess {
                    emitMessage("Saved game to slot $slot.")
                    refreshSlots()
                }
                .onFailure { emitMessage("Failed to save slot $slot.") }
        }
    }

    suspend fun deleteSlot(slot: Int) {
        val action = when (slot) {
            AUTOSAVE_SLOT -> runCatching { services.clearAutosave() }
            QUICKSAVE_SLOT -> runCatching { services.clearQuickSave() }
            else -> runCatching { services.clearSlot(slot) }
        }
        action
            .onSuccess {
                val message = when (slot) {
                    AUTOSAVE_SLOT -> "Cleared autosave."
                    QUICKSAVE_SLOT -> "Cleared quicksave."
                    else -> "Cleared slot $slot."
                }
                emitMessage(message)
                refreshSlots()
            }
            .onFailure {
                val message = when (slot) {
                    AUTOSAVE_SLOT -> "Unable to clear autosave."
                    QUICKSAVE_SLOT -> "Unable to clear quicksave."
                    else -> "Unable to clear slot $slot."
                }
                emitMessage(message)
            }
    }

    suspend fun reloadSlotFromAssets(slot: Int) {
        val success = runCatching { services.resetSlotFromAssets(slot) }.getOrElse { false }
        emitMessage(
            if (success) "Reloaded slot $slot from assets."
            else "Unable to reload slot $slot."
        )
        refreshSlots()
    }

    private fun GameSessionSlotInfo?.toQuickSaveSummary(): SaveSlotSummary {
        val state = this?.state ?: return SaveSlotSummary(
            slot = QUICKSAVE_SLOT,
            state = null,
            title = "Quicksave",
            subtitle = "No quicksave data",
            isEmpty = true,
            isQuickSave = true,
            savedAtMillis = null
        )
        if (state.isEmpty()) {
            return SaveSlotSummary(
                slot = QUICKSAVE_SLOT,
                state = null,
                title = "Quicksave",
                subtitle = "No quicksave data",
                isEmpty = true,
                isQuickSave = true,
                savedAtMillis = null
            )
        }
        val savedAt = this.savedAtMillis
        val location = buildList {
            state.roomId?.let { add("Room $it") }
            state.hubId?.let { add("Hub $it") }
        }.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Quicksave"
        val details = "Level ${state.playerLevel} · ${state.playerCredits} credits"
        val subtitle = listOfNotNull(details, formatTimestamp(savedAt)).joinToString(" • ")
        return SaveSlotSummary(
            slot = QUICKSAVE_SLOT,
            state = state,
            title = location,
            subtitle = subtitle,
            isEmpty = false,
            isQuickSave = true,
            savedAtMillis = savedAt
        )
    }

    private fun GameSessionSlotInfo?.toSummary(slot: Int): SaveSlotSummary {
        val state = this?.state
        val savedAt = this?.savedAtMillis
        if (state == null || state.isEmpty()) {
            return SaveSlotSummary(
                slot = slot,
                state = null,
                title = "Empty Slot",
                subtitle = "No data",
                isEmpty = true,
                savedAtMillis = null
            )
        }
        val location = buildList {
            state.roomId?.let { add("Room $it") }
            state.hubId?.let { add("Hub $it") }
        }.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Unknown Location"
        val summary = "Level ${state.playerLevel} · ${state.playerCredits} credits"
        val subtitle = listOfNotNull(summary, formatTimestamp(savedAt)).joinToString(" • ")
        return SaveSlotSummary(
            slot = slot,
            state = state,
            title = location,
            subtitle = subtitle,
            isEmpty = false,
            savedAtMillis = savedAt
        )
    }

    private fun GameSessionSlotInfo?.toAutosaveSummary(): SaveSlotSummary {
        val state = this?.state ?: return SaveSlotSummary(
            slot = AUTOSAVE_SLOT,
            state = null,
            title = "Autosave",
            subtitle = "No autosave data",
            isEmpty = true,
            isAutosave = true,
            savedAtMillis = null
        )
        val savedAt = this.savedAtMillis
        return if (state.isEmpty()) {
            SaveSlotSummary(
                slot = AUTOSAVE_SLOT,
                state = null,
                title = "Autosave",
                subtitle = "No autosave data",
                isEmpty = true,
                isAutosave = true,
                savedAtMillis = null
            )
        } else {
            val location = buildList {
                state.roomId?.let { add("Room $it") }
                state.hubId?.let { add("Hub $it") }
            }.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Autosave"
            val details = "Level ${state.playerLevel} · ${state.playerCredits} credits"
            val subtitle = listOfNotNull(details, formatTimestamp(savedAt)).joinToString(" • ")
            SaveSlotSummary(
                slot = AUTOSAVE_SLOT,
                state = state,
                title = location,
                subtitle = subtitle,
                isEmpty = false,
                isAutosave = true,
                savedAtMillis = savedAt
            )
        }
    }

    private fun GameSessionState.isEmpty(): Boolean {
        return worldId == null && hubId == null && roomId == null && inventory.isEmpty() && activeQuests.isEmpty()
    }

    private fun formatTimestamp(millis: Long?): String? {
        val value = millis ?: return null
        if (value <= 0L) return null
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("MMM d • HH:mm")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(Instant.ofEpochMilli(value))
        }.getOrNull()
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val MAX_SLOTS = 3
        private const val MAIN_MENU_THEME_ID = "starborn"
        private const val DEFAULT_THEME_ID = "default"
    }
}
