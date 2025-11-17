package com.example.starborn.feature.crafting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices

class CookingViewModelFactory(
    private val services: AppServices
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(AppServices(context))

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CookingViewModel::class.java)) {
            return CookingViewModel(
                craftingService = services.craftingService,
                inventoryService = services.inventoryService,
                tutorialManager = services.tutorialManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
