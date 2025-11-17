package com.example.starborn.feature.hub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.session.GameSessionStore

class HubViewModelFactory(
    private val worldAssets: WorldAssetDataSource,
    private val sessionStore: GameSessionStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HubViewModel(worldAssets, sessionStore) as T
    }
}
