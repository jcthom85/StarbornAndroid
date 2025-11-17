package com.example.starborn.feature.hub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HubViewModel(
    private val worldAssets: WorldAssetDataSource,
    private val sessionStore: GameSessionStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private var hubsById: Map<String, Hub> = emptyMap()
    private var nodesByHub: Map<String, List<HubNode>> = emptyMap()

    init {
        loadHub()
    }

    private fun loadHub() {
        viewModelScope.launch(dispatchers.io) {
            val hubs = worldAssets.loadHubs()
            val nodes = worldAssets.loadHubNodes()
            val roomsById = worldAssets.loadRooms().associateBy { it.id }

            val missingAssets = worldAssets.missingHubAssets(hubs, nodes)
            if (missingAssets.isNotEmpty()) {
                Log.w(
                    "HubViewModel",
                    "Missing hub assets: ${missingAssets.joinToString()}"
                )
            }
            hubsById = hubs.associateBy { it.id }
            nodesByHub = nodes.groupBy { it.hubId }

            val session = sessionStore.state.value
            val preferredHubId = session.hubId ?: hubs.firstOrNull()?.id
            val currentHub = preferredHubId?.let { hubsById[it] } ?: hubs.firstOrNull()

            val uiNodes = currentHub?.let { hub ->
                nodesByHub[hub.id].orEmpty().map { node ->
                    HubNodeUi(
                        id = node.id,
                        title = node.title,
                        entryRoom = node.entryRoom,
                        centerX = node.position.centerX.coerceIn(0f, 1f),
                        centerY = node.position.centerY.coerceIn(0f, 1f),
                        sizeHint = node.size.firstOrNull()?.toFloat() ?: 220f,
                        discovered = node.discovered,
                        iconPath = node.iconImage,
                        subtitle = buildNodeSubtitle(node, roomsById)
                    )
                }
            }.orEmpty()

            val firstDiscovered = uiNodes.firstOrNull { it.discovered } ?: uiNodes.firstOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hub = currentHub,
                    backgroundImage = currentHub?.backgroundImage,
                    nodes = uiNodes,
                    selectedNodeId = firstDiscovered?.id
                )
            }
        }
    }

    fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    fun enterNode(nodeId: String, onEnter: (HubNodeUi) -> Unit) {
        val state = _uiState.value
        val node = state.nodes.firstOrNull { it.id == nodeId } ?: return
        val hub = state.hub ?: return

        sessionStore.setHub(hub.id)
        sessionStore.setWorld(hub.worldId)
        sessionStore.setRoom(node.entryRoom)

        _uiState.update { it.copy(selectedNodeId = nodeId) }
        onEnter(node)
    }

    private fun buildNodeSubtitle(node: HubNode, roomsById: Map<String, Room>): String? {
        val services = node.rooms.flatMap { roomId ->
            serviceTagsForRoom(roomsById[roomId])
        }.distinct()
        if (services.isNotEmpty()) {
            val label = services.joinToString(" • ")
            if (label.isNotBlank()) return label
        }
        val titles = node.rooms.mapNotNull { roomId ->
            roomsById[roomId]?.title?.takeIf { it.isNotBlank() }
        }.distinct()
        val headline = titles.take(3).joinToString(" • ")
        return headline.ifBlank { null }
    }

    private fun serviceTagsForRoom(room: Room?): List<String> {
        if (room == null) return emptyList()
        return room.actions.mapNotNull { action ->
            when ((action["type"] as? String)?.lowercase(Locale.getDefault())) {
                "shop" -> (action["name"] as? String)?.ifBlank { null } ?: "Shop"
                "tinkering" -> (action["name"] as? String)?.ifBlank { null } ?: "Tinkering"
                "cooking" -> (action["name"] as? String)?.ifBlank { null } ?: "Cooking"
                "first_aid", "firstaid" -> (action["name"] as? String)?.ifBlank { null } ?: "First Aid"
                else -> null
            }
        }.distinct()
    }
}
