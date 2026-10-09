package com.example.starborn.feature.hub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.session.GameSessionStore

class HubViewModel(worldAssets: WorldAssetDataSource, questRepository: QuestRepository,
    sessionStore: GameSessionStore, dispatchers: DispatcherProvider = DefaultDispatcherProvider) : ViewModel() {
    val runtime = HubController(worldAssets, questRepository, sessionStore, dispatchers, viewModelScope)
    val uiState get() = runtime.uiState
    fun finishNodeReveal(nodeId: String) = runtime.finishNodeReveal(nodeId)
    fun selectNode(nodeId: String) = runtime.selectNode(nodeId)
    fun enterNode(nodeId: String, onEnter: (HubNodeUi) -> Unit) = runtime.enterNode(nodeId, onEnter)
    fun dismissLockedPrompt() = runtime.dismissLockedPrompt()
    override fun onCleared() { runtime.close() }
}
