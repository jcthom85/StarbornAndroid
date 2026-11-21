package com.example.starborn.feature.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.di.AppServices
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicPresentation
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import com.example.starborn.feature.exploration.viewmodel.helpers.CinematicPlaybackManager
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.SessionSaveRepository
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



class MainMenuViewModel(
    private val services: AppServices
) : ViewModel() {

    private val _slots = MutableStateFlow<List<SaveSlotSummary>>(emptyList())
    val slots: StateFlow<List<SaveSlotSummary>> = _slots.asStateFlow()

    private val _newGamePlusUnlocked = MutableStateFlow(false)
    val newGamePlusUnlocked: StateFlow<Boolean> = _newGamePlusUnlocked.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val mainMenuTheme: Theme? = services.themeRepository.getTheme(MAIN_MENU_THEME_ID)
        ?: services.themeRepository.getTheme(DEFAULT_THEME_ID)
    private val worldsById = services.worldDataSource.loadWorlds().associateBy { it.id }
    private val roomsById = services.worldDataSource.loadRooms().associateBy { it.id }
    private val hubsById = services.worldDataSource.loadHubs().associateBy { it.id }
    private val charactersById = services.worldDataSource.loadCharacters().associateBy { it.id }

    init {
        refreshSlots()
    }

    fun refreshSlots() {
        viewModelScope.launch {
            val quicksave = runCatching { services.quickSaveInfo() }.getOrNull().toQuickSaveSummary()
            val autosave = runCatching { services.autosaveInfo() }.getOrNull().toAutosaveSummary()
            val summaries = SessionSaveRepository.MANUAL_SLOTS.map { slot ->
                val info = runCatching { services.slotInfo(slot) }.getOrNull()
                info.toSummary(slot)
            }
            _slots.value = buildList {
                add(quicksave)
                add(autosave)
                addAll(summaries)
            }
            _newGamePlusUnlocked.value = runCatching {
                services.isNewGamePlusUnlocked()
            }.getOrDefault(false)
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

    fun startNewGame(
        onComplete: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = services.startNewGame()
            if (success) {
                services.syncInventoryFromSession()
                onComplete?.invoke()
            } else {
                emitMessage("Failed to start new game. Check assets and logs.")
                onFailure?.invoke()
            }
        }
    }

    fun startNewGamePlus(
        onComplete: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = services.startNewGamePlus()
            if (success) {
                services.syncInventoryFromSession()
                onComplete?.invoke()
            } else {
                emitMessage("Failed to start Master Protocol. Check save state.")
                onFailure?.invoke()
            }
        }
    }

    fun startDebugScenario(
        scenario: DebugScenario,
        onComplete: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = services.startDebugScenario(scenario.id, allowInGameLaunch = true)
            if (success) {
                services.syncInventoryFromSession()
                onComplete?.invoke()
            } else {
                emitMessage(services.debugScenarioError ?: "Failed to start ${scenario.title}.")
                onFailure?.invoke()
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
        val location = saveLocationTitle(state, fallback = "Quicksave")
        val details = saveDetails(state)
        val subtitle = listOfNotNull(details, formatTimestamp(savedAt)).joinToString(" - ")
        return SaveSlotSummary(
            slot = QUICKSAVE_SLOT,
            state = state,
            title = location,
            subtitle = subtitle,
            isEmpty = false,
            isQuickSave = true,
            savedAtMillis = savedAt,
            partyPortraits = partyPortraitsFor(state)
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
        val location = saveLocationTitle(state, fallback = "Unknown Location")
        val summary = saveDetails(state)
        val subtitle = listOfNotNull(summary, formatTimestamp(savedAt)).joinToString(" - ")
        return SaveSlotSummary(
            slot = slot,
            state = state,
            title = location,
            subtitle = subtitle,
            isEmpty = false,
            savedAtMillis = savedAt,
            partyPortraits = partyPortraitsFor(state)
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
            val location = saveLocationTitle(state, fallback = "Autosave")
            val details = saveDetails(state)
            val subtitle = listOfNotNull(details, formatTimestamp(savedAt)).joinToString(" - ")
            SaveSlotSummary(
                slot = AUTOSAVE_SLOT,
                state = state,
                title = location,
                subtitle = subtitle,
                isEmpty = false,
                isAutosave = true,
                savedAtMillis = savedAt,
                partyPortraits = partyPortraitsFor(state)
            )
        }
    }

    private fun GameSessionState.isEmpty(): Boolean {
        return worldId == null && hubId == null && roomId == null && inventory.isEmpty() && activeQuests.isEmpty()
    }

    private fun saveLocationTitle(state: GameSessionState, fallback: String): String {
        val room = state.roomId?.let { roomId ->
            roomsById[roomId]?.title?.takeIf { it.isNotBlank() } ?: roomId.readableSaveLabel()
        }
        val hub = state.hubId?.let { hubId ->
            hubsById[hubId]?.title?.takeIf { it.isNotBlank() } ?: hubId.readableSaveLabel()
        }
        return listOfNotNull(room, hub)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" - ")
            ?: fallback
    }

    private fun partyPortraitsFor(state: GameSessionState): List<String> {
        val partyIds = state.partyMembers.ifEmpty { listOfNotNull(state.playerId) }
        return partyIds.mapNotNull { id ->
            charactersById[id]?.miniIconPath?.takeIf { it.isNotBlank() }
        }
    }

    private fun saveDetails(state: GameSessionState): String {
        val world = state.worldId?.let { worldId ->
            worldsById[worldId]?.title?.takeIf { it.isNotBlank() } ?: worldId.readableSaveLabel()
        }
        return listOfNotNull(
            world,
            "Level ${state.playerLevel}",
            "${state.playerCredits} credits"
        ).joinToString(" - ")
    }

    private fun String.readableSaveLabel(): String =
        replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
            .ifBlank { this }

    private fun formatTimestamp(millis: Long?): String? {
        val value = millis ?: return null
        if (value <= 0L) return null
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("MMM d - HH:mm")
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

    fun screeningRoomEntries(): List<ScreeningRoomEntry> {
        val allIllustrated = services.cinematicService.allScenes()
            .filter { it.presentation == CinematicPresentation.ILLUSTRATED }
            .associateBy { it.id }

        val curatedOrder = listOf(
            "World 1 — Mining Colony" to listOf(
                "intro_prologue",
                "new_game_fade_in",
                "scene_cutter_surge",
                "scene_relic_sync",
                "scene_w1_lift_separation",
                "scene_launch_crash"
            ),
            "World 2 — The Wilds" to listOf(
                "scene_w2_orion_awakening",
                "scene_anchor_drill",
                "scene_w2_astra_departure"
            ),
            "World 3 — The Spire" to listOf(
                "scene_w3_lens_heist",
                "scene_w3_foundry_flight"
            ),
            "World 4 — The Foundry" to listOf(
                "scene_w4_anvil_forged",
                "scene_w4_rylos_spared",
                "scene_w4_outer_shield"
            ),
            "World 5 — The Orbital Ring" to listOf(
                "scene_w5_anchor_stasis",
                "scene_w5_soloist_breach",
                "scene_w5_cross_the_tear"
            ),
            "World 6 — The Source" to listOf(
                "scene_w6_song_fire_reunion",
                "scene_w6_jed_echo_lesson",
                "scene_w6_silence_rises",
                "scene_w6_final_note",
                "scene_w6_epilogue_credits"
            )
        )

        val entries = mutableListOf<ScreeningRoomEntry>()
        val seen = mutableSetOf<String>()
        for ((worldLabel, ids) in curatedOrder) {
            for (id in ids) {
                val scene = allIllustrated[id] ?: continue
                seen += id
                entries += scene.toScreeningEntry(worldLabel)
            }
        }
        allIllustrated.values
            .filter { it.id !in seen }
            .sortedBy { it.id }
            .forEach { scene ->
                entries += scene.toScreeningEntry("Other")
            }
        return entries
    }

    private fun CinematicScene.toScreeningEntry(worldLabel: String): ScreeningRoomEntry {
        val durationSec = steps.sumOf { it.durationSeconds ?: 4.0 }
        val imgCount = steps.mapNotNull { it.imagePath }.distinct().size
        return ScreeningRoomEntry(
            worldLabel = worldLabel,
            scene = this,
            durationSeconds = durationSec,
            imageCount = imgCount
        )
    }

    fun buildScreeningCinematicState(scene: CinematicScene, stepIndex: Int): CinematicUiState {
        val playback = CinematicPlaybackState(
            scene = scene,
            stepIndex = stepIndex.coerceIn(0, (scene.steps.size - 1).coerceAtLeast(0))
        )
        return CinematicPlaybackManager.toUiState(
            playback = playback,
            resolveEmotePortrait = { speaker, emote ->
                val normalized = speaker.trim().lowercase(Locale.US)
                val cleanEmote = emote?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
                if (cleanEmote != null) {
                    "images/characters/emotes/${normalized}_${cleanEmote}.png"
                } else {
                    null
                }
            },
            resolvePortraitKey = { speaker ->
                val normalized = speaker.trim().lowercase(Locale.US)
                "images/characters/${normalized}_portrait.png"
            }
        )
    }

    companion object {
        private const val MAIN_MENU_THEME_ID = "starborn"
        private const val DEFAULT_THEME_ID = "default"
    }
}

data class ScreeningRoomEntry(
    val worldLabel: String,
    val scene: CinematicScene,
    val durationSeconds: Double,
    val imageCount: Int
)
