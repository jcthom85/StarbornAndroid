package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

enum class ShopTab {
    BUY, SELL
}

@Composable
fun DesktopShopDialog(
    services: DesktopAppServices,
    shopId: String? = "scrapper_shop",
    onDismiss: () -> Unit
) {
    val allShops = remember { services.shopRepository.allShops().associateBy { it.id } }
    val currentShop = (if (shopId != null) allShops[shopId] else null) ?: allShops.values.firstOrNull() ?: ShopDefinition(
        id = "station_market",
        name = "Astra Outpost Merchant",
        greeting = "Welcome, traveler. Best scrap and munitions in the quadrant."
    )

    val allItems = remember { services.itemRepository.allItems().associateBy { it.id } }
    val sessionState by services.sessionStore.state.collectAsState()
    var activeTab by remember { mutableStateOf(ShopTab.BUY) }
    var transactionMessage by remember { mutableStateOf<String?>(null) }

    val shopItems = remember(currentShop) {
        val directItems = currentShop.sells.items.mapNotNull { allItems[it] }
        if (directItems.isNotEmpty()) directItems else allItems.values.take(8)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(920.dp)
                .height(600.dp),
            shape = RoundedCornerShape(FieldMenuDesign.shellRadius),
            color = FieldMenuDesign.shell,
            border = BorderStroke(1.5.dp, FieldMenuDesign.cyan.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val merchantPortrait = rememberDesktopAssetPainter(currentShop.portrait ?: "dr_aris", services.assetProvider)
                        Image(
                            painter = merchantPortrait,
                            contentDescription = currentShop.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(1.5.dp, FieldMenuDesign.cyan), CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Column {
                            Text(
                                text = currentShop.name.uppercase(),
                                color = FieldMenuDesign.cyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = currentShop.greeting ?: "Exchange goods and military salvage.",
                                color = FieldMenuDesign.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CREDITS: ${sessionState.playerCredits} CR",
                            color = FieldMenuDesign.gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = FieldMenuDesign.text)
                        }
                    }
                }

                // Buy / Sell Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopShopTabButton("BUY WAREHOUSE GOODS", activeTab == ShopTab.BUY) {
                        activeTab = ShopTab.BUY
                        transactionMessage = null
                    }
                    DesktopShopTabButton("SELL CARGO SALVAGE", activeTab == ShopTab.SELL) {
                        activeTab = ShopTab.SELL
                        transactionMessage = null
                    }
                }

                // Main Market Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                        .background(FieldMenuDesign.panel)
                        .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.35f)), RoundedCornerShape(FieldMenuDesign.cardRadius))
                        .padding(16.dp)
                ) {
                    when (activeTab) {
                        ShopTab.BUY -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(shopItems) { item ->
                                    val price = item.buyPrice ?: (if (item.value > 0) item.value else 40)
                                    val canAfford = sessionState.playerCredits >= price

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FieldMenuDesign.elevatedPanel)
                                            .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = item.name, color = FieldMenuDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text(text = item.description.orEmpty(), color = FieldMenuDesign.textMuted, fontSize = 11.sp, maxLines = 1)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "$price CR",
                                                    color = if (canAfford) FieldMenuDesign.gold else Color(0xFFFF5252),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )

                                                Button(
                                                    onClick = {
                                                        if (canAfford) {
                                                            services.sessionStore.addCredits(-price)
                                                            val inv = sessionState.inventory.toMutableMap()
                                                            inv[item.id] = (inv[item.id] ?: 0) + 1
                                                            services.sessionStore.setInventory(inv)
                                                            transactionMessage = "✓ PURCHASED ${item.name} (-$price CR)"
                                                        }
                                                    },
                                                    enabled = canAfford,
                                                    colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.cyan),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Text(text = "BUY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ShopTab.SELL -> {
                            val sellableEntries = sessionState.inventory.entries.toList()
                            if (sellableEntries.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = "— Cargo hold contains no salvage items to sell. —", color = FieldMenuDesign.textMuted, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(sellableEntries) { entry ->
                                        val item = allItems[entry.key]
                                        val itemName = item?.name ?: entry.key.replace("_", " ").uppercase()
                                        val baseVal = item?.value?.takeIf { it > 0 } ?: 30
                                        val sellPrice = (baseVal * 0.5f).toInt().coerceAtLeast(10)
                                        val qty = entry.value

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(FieldMenuDesign.elevatedPanel)
                                                .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(text = itemName, color = FieldMenuDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "x$qty", color = FieldMenuDesign.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text(text = item?.description.orEmpty(), color = FieldMenuDesign.textMuted, fontSize = 11.sp, maxLines = 1)
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "+$sellPrice CR",
                                                        color = Color(0xFF00E676),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )

                                                    Button(
                                                        onClick = {
                                                            services.sessionStore.addCredits(sellPrice)
                                                            val inv = sessionState.inventory.toMutableMap()
                                                            val current = inv[entry.key] ?: 1
                                                            if (current <= 1) inv.remove(entry.key) else inv[entry.key] = current - 1
                                                            services.sessionStore.setInventory(inv)
                                                            transactionMessage = "✓ SOLD 1x $itemName (+$sellPrice CR)"
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(text = "SELL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Status Message
                if (transactionMessage != null) {
                    Text(
                        text = transactionMessage!!,
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopShopTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
            .background(if (isSelected) FieldMenuDesign.cyan.copy(alpha = 0.2f) else FieldMenuDesign.elevatedPanel)
            .border(
                BorderStroke(1.dp, if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.border.copy(alpha = 0.25f)),
                RoundedCornerShape(FieldMenuDesign.controlRadius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
