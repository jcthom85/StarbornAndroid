package com.example.starborn.feature.mainmenu

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices

class MainMenuViewModelFactory(
    private val services: AppServices
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(AppServices(context))

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainMenuViewModel::class.java)) {
            return MainMenuViewModel(services) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
