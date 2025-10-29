package com.example.starborn.feature.exploration.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices

class ExplorationViewModelFactory(
    private val services: AppServices
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(AppServices(context))

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExplorationViewModel::class.java)) {
            return ExplorationViewModel(
                worldAssets = services.worldDataSource,
                sessionStore = services.sessionStore,
                dialogueService = services.dialogueService,
                inventoryService = services.inventoryService,
                craftingService = services.craftingService,
                questRepository = services.questRepository,
                eventDefinitions = services.events
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
