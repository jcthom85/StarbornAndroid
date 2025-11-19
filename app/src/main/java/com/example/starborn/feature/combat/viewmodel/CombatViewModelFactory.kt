package com.example.starborn.feature.combat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices

class CombatViewModelFactory(
    private val services: AppServices,
    private val enemyIds: List<String>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CombatViewModel::class.java)) {
            return CombatViewModel(
                worldAssets = services.worldDataSource,
                combatEngine = services.combatEngine,
                statusRegistry = services.statusRegistry,
                sessionStore = services.sessionStore,
                inventoryService = services.inventoryService,
                itemCatalog = services.itemRepository,
                levelingManager = services.levelingManager,
                progressionData = services.progressionData,
                audioRouter = services.audioRouter,
                themeRepository = services.themeRepository,
                environmentThemeManager = services.environmentThemeManager,
                encounterCoordinator = services.encounterCoordinator,
                enemyIds = enemyIds
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
