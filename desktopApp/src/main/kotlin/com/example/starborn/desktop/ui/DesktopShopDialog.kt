package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.shop.ShopController
import com.example.starborn.feature.shop.ShopTab
import kotlinx.coroutines.flow.collect

@Composable
fun DesktopShopDialog(services: DesktopAppServices, shopId: String? = null, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val runtime = remember(shopId) {
        ShopController(shopId.orEmpty(), services.shopRepository, services.itemRepository,
            services.inventoryService, services.sessionStore, scope)
    }
    DisposableEffect(runtime) { onDispose { runtime.close() } }
    val ui by runtime.uiState.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(runtime) { runtime.messages.collect { message = it } }
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.width(800.dp),
        title = { Text("${ui.shopName} ? ${ui.credits} credits") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    TextButton(onClick = { runtime.switchTab(ShopTab.BUY) }) { Text("Buy") }
                    TextButton(onClick = { runtime.switchTab(ShopTab.SELL) }) { Text("Sell") }
                }
                message?.let { Text(it) }
                ui.unavailableMessage?.let { Text(it) }
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ui.activeTab == ShopTab.BUY) items(ui.itemsForSale, key = { it.id }) { item ->
                        Column {
                            Text(item.name)
                            Text(item.lockedMessage ?: item.description.orEmpty())
                            Button(onClick = { runtime.buyItem(item.id, 1) }, enabled = !item.locked && item.canAfford && item.maxQuantity > 0) {
                                Text("Buy one ? ${item.price} credits")
                            }
                        }
                    } else items(ui.sellInventory, key = { it.id }) { item ->
                        Column {
                            Text("${item.name} ?${item.quantity}")
                            item.reason?.let { Text(it) }
                            Button(onClick = { runtime.sellItem(item.id, 1) }, enabled = item.canSell) {
                                Text("Sell one ? ${item.price} credits")
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
