package com.example.starborn.feature.exploration.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices
import com.example.starborn.domain.session.GameSaveRepository

class ExplorationViewModelFactory(
    private val services: AppServices
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(AppServices(context))

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExplorationViewModel::class.java)) {
            val saveRepository = GameSaveRepository(services)
            return ExplorationViewModel(
                worldAssets = services.worldDataSource,
                sessionStore = services.sessionStore,
                dialogueService = services.dialogueService,
                inventoryService = services.inventoryService,
                craftingService = services.craftingService,
                cinematicCoordinator = services.cinematicCoordinator,
                questRepository = services.questRepository,
                questRuntimeManager = services.questRuntimeManager,
                milestoneManager = services.milestoneManager,
                audioRouter = services.audioRouter,
                voiceoverController = services.voiceoverController,
                shopRepository = services.shopRepository,
                themeRepository = services.themeRepository,
                environmentThemeManager = services.environmentThemeManager,
                levelingManager = services.levelingManager,
                tutorialManager = services.tutorialManager,
                promptManager = services.promptManager,
                fishingService = services.fishingService,
                saveRepository = saveRepository,
                encounterCoordinator = services.encounterCoordinator,
                eventDefinitions = services.events,
                userSettingsStore = services.userSettingsStore,
                bootstrapCinematics = services.drainPendingCinematics(),
                bootstrapActions = services.drainPendingPlayerActions(),
                dialogueTriggerBinder = services::setDialogueTriggerListener
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
