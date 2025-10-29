package com.example.starborn.feature.combat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource

class CombatViewModelFactory(
    private val context: Context,
    private val enemyId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CombatViewModel::class.java)) {
            val assetReader = AssetJsonReader(context, MoshiProvider.instance)
            val assetDataSource = WorldAssetDataSource(assetReader)
            return CombatViewModel(assetDataSource, enemyId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
