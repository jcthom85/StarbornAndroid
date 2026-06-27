package com.example.starborn.feature.hub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.node.NodeProgressionEvaluator
import com.example.starborn.domain.node.NodeVisibility
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HubViewModel(
    private val worldAssets: WorldAssetDataSource,
    private val questRepository: QuestRepository,
    private val sessionStore: GameSessionStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private var hubsById: Map<String, Hub> = emptyMap()
    private var nodesByHub: Map<String, List<HubNode>> = emptyMap()
    private var nodeDescriptionsById: Map<String, String> = emptyMap()
    private var lockedPreviewsById: Map<String, String> = emptyMap()
    private val nodeProgression = NodeProgressionEvaluator()

    init {
        loadHub()
        observeSession()
    }

    private fun loadHub() {
        viewModelScope.launch(dispatchers.io) {
            val hubs = worldAssets.loadHubs()
            val nodes = worldAssets.loadHubNodes()
            nodeDescriptionsById = worldAssets.loadHubNodeDescriptions()
            lockedPreviewsById = worldAssets.loadHubNodeLockedPreviews()

            val missingAssets = worldAssets.missingHubAssets(hubs, nodes)
            if (missingAssets.isNotEmpty()) {
                Log.w(
                    "HubViewModel",
                    "Missing hub assets: ${missingAssets.joinToString()}"
                )
            }
            hubsById = hubs.associateBy { it.id }
            nodesByHub = nodes.groupBy { it.hubId }

            val preSession = sessionStore.state.value
            nodes.firstOrNull { preSession.roomId in it.rooms }?.let { sessionStore.visitNode(it.id) }
            val session = sessionStore.state.value
            val preferredHubId = session.hubId ?: hubs.firstOrNull()?.id
            val currentHub = preferredHubId?.let { hubsById[it] } ?: hubs.firstOrNull()
            val uiNodes = buildUiNodes(currentHub, session)
            val firstDiscovered = uiNodes.firstOrNull { it.discovered } ?: uiNodes.firstOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hub = currentHub,
                    backgroundImage = currentHub?.backgroundImage,
                    nodes = uiNodes,
                    selectedNodeId = firstDiscovered?.id,
                    trackedQuest = buildTrackedQuest(session)
                )
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch(dispatchers.io) {
            sessionStore.state.drop(1).collect { session ->
                if (hubsById.isEmpty() || nodesByHub.isEmpty()) return@collect
                val preferredHubId = session.hubId ?: _uiState.value.hub?.id ?: hubsById.values.firstOrNull()?.id
                val currentHub = preferredHubId?.let { hubsById[it] } ?: hubsById.values.firstOrNull()
                val uiNodes = buildUiNodes(currentHub, session)
                val currentSelection = _uiState.value.selectedNodeId
                val selectedNodeId = currentSelection
                    ?.takeIf { selected -> uiNodes.any { it.id == selected } }
                    ?: uiNodes.firstOrNull { it.discovered }?.id
                    ?: uiNodes.firstOrNull()?.id

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hub = currentHub,
                        backgroundImage = currentHub?.backgroundImage,
                        nodes = uiNodes,
                        selectedNodeId = selectedNodeId,
                        trackedQuest = buildTrackedQuest(session)
                    )
                }
            }
        }
    }

    private fun buildUiNodes(currentHub: Hub?, session: GameSessionState): List<HubNodeUi> {
        return currentHub?.let { hub ->
            nodesByHub[hub.id].orEmpty().mapNotNull { node ->
                val availability = nodeProgression.evaluate(node, session)
                if (availability.visibility == NodeVisibility.HIDDEN) return@mapNotNull null
                HubNodeUi(
                    id = node.id,
                    title = node.title,
                    entryRoom = node.entryRoom,
                    centerX = node.position.centerX.coerceIn(0f, 1f),
                    centerY = node.position.centerY.coerceIn(0f, 1f),
                    sizeHint = node.size.firstOrNull()?.toFloat() ?: 220f,
                    discovered = availability.visited,
                    iconPath = node.iconImage,
                    description = nodeDescriptionsById[node.id],
                    lockedPreview = lockedPreviewsById[node.id],
                    unlocked = availability.unlocked,
                    visited = availability.visited,
                    completed = availability.completed,
                    canEnter = availability.canEnterFromHub,
                    lockReason = availability.unmetRequirement
                )
            }.toMutableList().apply {
                if (hub.id != ASTRA_HUB_ID && ASTRA_UNLOCK_MILESTONE in session.completedMilestones) {
                    add(astraAccessNode())
                } else if (hub.id == ASTRA_HUB_ID && session.astraReturnHubId != null) {
                    add(astraDisembarkNode())
                }
            }
        }.orEmpty()
    }

    fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    fun enterNode(nodeId: String, onEnter: (HubNodeUi) -> Unit) {
        val state = _uiState.value
        val node = state.nodes.firstOrNull { it.id == nodeId } ?: return
        val hub = state.hub ?: return

        if (!node.canEnter) {
            _uiState.update {
                it.copy(
                    selectedNodeId = nodeId,
                    statusMessage = null,
                    lockedPrompt = HubLockedPrompt(
                        title = node.title,
                        message = node.lockReason ?: "Reach this location through exploration first."
                    )
                )
            }
            return
        }

        if (node.special == SPECIAL_ASTRA) {
            val session = sessionStore.state.value
            sessionStore.setAstraReturnLocation(session.worldId, session.hubId, session.roomId)
            sessionStore.setWorld(ASTRA_WORLD_ID)
            sessionStore.setHub(ASTRA_HUB_ID)
            sessionStore.setRoom(ASTRA_BRIDGE_ROOM_ID)
            sessionStore.visitNode(ASTRA_BRIDGE_NODE_ID)
            onEnter(node)
            return
        }

        if (node.special == SPECIAL_DISEMBARK) {
            val session = sessionStore.state.value
            sessionStore.setWorld(session.astraReturnWorldId)
            sessionStore.setHub(session.astraReturnHubId)
            sessionStore.setRoom(session.astraReturnRoomId)
            sessionStore.clearAstraReturnLocation()
            onEnter(node)
            return
        }

        sessionStore.setHub(hub.id)
        sessionStore.setWorld(hub.worldId)
        sessionStore.setRoom(node.entryRoom)
        sessionStore.visitNode(node.id)

        _uiState.update { it.copy(selectedNodeId = nodeId, statusMessage = null) }
        onEnter(node)
    }

    fun dismissLockedPrompt() {
        _uiState.update { it.copy(lockedPrompt = null) }
    }

    private fun buildTrackedQuest(session: GameSessionState): HubQuestUi? {
        val questId = session.trackedQuestId
            ?.takeIf { it.isNotBlank() }
            ?: session.activeQuests.firstOrNull()
            ?: return null
        if (!session.activeQuests.contains(questId)) return null
        val quest = questRepository.questById(questId) ?: return null
        val stage = activeStage(quest, session)
        val completedTasks = session.questTasksCompleted[quest.id].orEmpty()
        val objective = stage?.tasks
            ?.firstOrNull { task -> !completedTasks.contains(task.id) }
            ?.text
            ?: stage?.description.takeIf { !it.isNullOrBlank() }
            ?: quest.summary.takeIf { it.isNotBlank() }
        return HubQuestUi(
            id = quest.id,
            title = quest.title,
            objective = objective,
            stageTitle = stage?.title?.takeIf { it.isNotBlank() }
        )
    }

    private fun activeStage(quest: Quest, session: GameSessionState) =
        session.questStageById[quest.id]
            ?.let { stageId -> quest.stages.firstOrNull { it.id.equals(stageId, ignoreCase = true) } }
            ?: quest.stages.firstOrNull()

    private fun astraAccessNode() = HubNodeUi(
        id = ASTRA_ACCESS_ID,
        title = "The Astra",
        entryRoom = ASTRA_BRIDGE_ROOM_ID,
        centerX = 0.5f,
        centerY = 0.88f,
        sizeHint = 240f,
        discovered = true,
        iconPath = "images/nodes/world_2/hangar_bay.png",
        description = "A battered ship with warm lights in the windows and enough scars to count as history.",
        visited = true,
        special = SPECIAL_ASTRA
    )

    private fun astraDisembarkNode() = HubNodeUi(
        id = ASTRA_DISEMBARK_ID,
        title = "Disembark",
        entryRoom = "",
        centerX = 0.12f,
        centerY = 0.88f,
        sizeHint = 180f,
        discovered = true,
        description = "The ramp waits open, pointing back toward the trouble you left behind.",
        visited = true,
        special = SPECIAL_DISEMBARK
    )

    companion object {
        const val ASTRA_HUB_ID = "hub_astra"
        const val ASTRA_WORLD_ID = "world_astra"
        const val ASTRA_BRIDGE_NODE_ID = "astra_bridge_node"
        const val ASTRA_BRIDGE_ROOM_ID = "astra_bridge"
        const val ASTRA_ACCESS_ID = "astra_access"
        const val ASTRA_DISEMBARK_ID = "astra_disembark"
        const val ASTRA_UNLOCK_MILESTONE = "ms_w2_mq05_complete"
        const val SPECIAL_ASTRA = "astra"
        const val SPECIAL_DISEMBARK = "disembark"
    }
}
