package com.example.starborn.feature.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices
import com.example.starborn.domain.fishing.FishingService

class FishingViewModelFactory(
    private val fishingService: FishingService,
    private val zoneId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FishingViewModel::class.java)) {
            return FishingViewModel(
                fishingService = fishingService,
                zoneId = zoneId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}