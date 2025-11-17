package com.example.starborn.feature.shop

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.starborn.di.AppServices

class ShopViewModelFactory(
    private val services: AppServices,
    private val shopId: String
) : ViewModelProvider.Factory {

    constructor(context: Context, shopId: String) : this(AppServices(context), shopId)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            return ShopViewModel(
                shopId = shopId,
                shopCatalog = services.shopRepository,
                itemCatalog = services.itemRepository,
                inventoryService = services.inventoryService,
                sessionStore = services.sessionStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
