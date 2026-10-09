package com.example.starborn.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.ShopGate
import com.example.starborn.domain.model.ShopDialogueLine
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.shop.ShopCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class ShopViewModel(
    shopId: String,
    shopCatalog: ShopCatalog,
    itemCatalog: ItemCatalog,
    inventoryService: InventoryService,
    sessionStore: GameSessionStore
) : ViewModel() {
    val runtime = ShopController(
        shopId = shopId,
        shopCatalog = shopCatalog,
        itemCatalog = itemCatalog,
        inventoryService = inventoryService,
        sessionStore = sessionStore,
        parentScope = viewModelScope
    )
    val uiState: StateFlow<ShopUiState> get() = runtime.uiState
    val messages: SharedFlow<String> get() = runtime.messages
    fun buyItem(itemId: String, quantity: Int) = runtime.buyItem(itemId, quantity)
    fun playSmalltalk(topicId: String) = runtime.playSmalltalk(topicId)
    fun sellItem(itemId: String, quantity: Int) = runtime.sellItem(itemId, quantity)
    fun switchTab(tab: ShopTab) = runtime.switchTab(tab)
    override fun onCleared() { runtime.close(); super.onCleared() }
}
