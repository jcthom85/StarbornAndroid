package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.ui.arcade.DesktopArcadeKeys
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.shop.*
import kotlinx.coroutines.delay
import java.awt.event.KeyEvent
import java.util.Locale

private enum class ShopCategoryFilter(val label: String) {
    ALL("All"),
    WEAPONS("Weapons"),
    ARMOR("Armor & Mods"),
    CONSUMABLES("Consumables"),
    MATERIALS("Materials")
}

@Composable
fun DesktopShopDialog(
    services: DesktopAppServices,
    shopId: String? = null,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val runtime = remember(shopId) {
        ShopController(
            shopId = shopId.orEmpty(),
            shopCatalog = services.shopRepository,
            itemCatalog = services.itemRepository,
            inventoryService = services.inventoryService,
            sessionStore = services.sessionStore,
            parentScope = scope
        )
    }
    DisposableEffect(runtime) {
        onDispose { runtime.close() }
    }

    val ui by runtime.uiState.collectAsState()
    val session by services.sessionStore.state.collectAsState()
    val fonts = LocalStarbornFonts.current

    var selectedCategory by remember { mutableStateOf(ShopCategoryFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var activeMessage by remember { mutableStateOf<String?>(null) }
    var showChatTab by remember { mutableStateOf(false) }

    val cue: (String) -> Unit = { cueName ->
        services.audioDriver.executeAll(services.audioRouter.commandsForUi(cueName))
    }

    LaunchedEffect(runtime) {
        runtime.messages.collect { msg ->
            activeMessage = msg
            cue("confirm")
        }
    }

    LaunchedEffect(activeMessage) {
        if (activeMessage != null) {
            delay(4000)
            activeMessage = null
        }
    }

    // Keyboard navigation
    DesktopArcadeKeys { keyCode, pressed ->
        if (pressed) {
            when (keyCode) {
                KeyEvent.VK_1 -> {
                    showChatTab = false
                    runtime.switchTab(ShopTab.BUY)
                    cue("action_inspect")
                }
                KeyEvent.VK_2 -> {
                    showChatTab = false
                    runtime.switchTab(ShopTab.SELL)
                    cue("action_inspect")
                }
                KeyEvent.VK_3 -> {
                    if (ui.smalltalkTopics.isNotEmpty()) {
                        showChatTab = true
                        cue("action_inspect")
                    }
                }
                KeyEvent.VK_ESCAPE -> {
                    onDismiss()
                }
            }
        }
    }

    val catalogMap = remember(services) {
        services.itemRepository.allItems().associateBy { it.id }
    }

    val activeCharacter = session.partyMembers.firstOrNull() ?: "nova"

    fun matchesCategory(filter: ShopCategoryFilter, item: Item?): Boolean {
        if (filter == ShopCategoryFilter.ALL || item == null) return true
        val type = item.type.lowercase(Locale.ROOT)
        val cat = item.categoryOverride?.lowercase(Locale.ROOT) ?: type
        return when (filter) {
            ShopCategoryFilter.WEAPONS -> type == "weapon" || cat == "weapon"
            ShopCategoryFilter.ARMOR -> type in listOf("armor", "mod", "accessory") || cat in listOf("armor", "mod", "accessory")
            ShopCategoryFilter.CONSUMABLES -> type in listOf("consumable", "ingredient", "food", "potion") || cat in listOf("consumable", "ingredient")
            ShopCategoryFilter.MATERIALS -> type in listOf("component", "material", "scrap", "ore", "junk") || cat in listOf("component", "material")
            else -> true
        }
    }

    fun resolveEquippedItem(candidate: Item): Item? {
        val slot = when {
            candidate.type == "weapon" || candidate.equipment?.slot == "weapon" -> "weapon"
            candidate.type == "armor" || candidate.equipment?.slot == "armor" -> "armor"
            candidate.equipment?.slot != null -> candidate.equipment?.slot
            else -> null
        } ?: return null

        val equippedId = when (slot) {
            "weapon" -> session.equippedWeapons[activeCharacter]
            "armor" -> session.equippedArmors[activeCharacter]
            else -> session.equippedItems["$activeCharacter:$slot"] ?: session.equippedItems[slot]
        } ?: return null

        return catalogMap[equippedId]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(920.dp)
                .height(650.dp)
                .testTag("desktop-shop-dialog"),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF07111D),
            border = BorderStroke(1.5.dp, Color(0xFF1E3A52)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header: Dealer Info + Credits Balance + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Dealer Avatar & Shop Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val portraitPainter = rememberDesktopAssetPainter(
                            ui.portraitPath,
                            services.assetProvider,
                            fallbackColor = Color(0xFF10283B)
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10283B),
                            border = BorderStroke(1.5.dp, Color(0xFF63E6FF)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            if (!ui.portraitPath.isNullOrBlank()) {
                                Image(
                                    painter = portraitPainter,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🏪", fontSize = 20.sp)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = ui.shopName.ifBlank { "Vendor Terminal" },
                                color = Color.White,
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = ui.conversationLog.lastOrNull()?.text ?: "Sector Commerce & Logistics Terminal",
                                color = Color(0xFF8FB0C4),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Credits Balance & Close Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x22FFD54F),
                            border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🪙 %,d CREDITS".format(ui.credits),
                                    color = Color(0xFFFFD54F),
                                    fontFamily = fonts.orbitron,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        DesktopKeyBadge(
                            keyGlyph = "Esc",
                            label = "Close",
                            onClick = onDismiss
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF162D42))

                // Navigation Row: Tabs (Buy / Sell / Chat) + Category Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Primary Tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TabButton(
                            label = "BUY [1]",
                            count = ui.itemsForSale.size,
                            isSelected = !showChatTab && ui.activeTab == ShopTab.BUY,
                            onClick = {
                                showChatTab = false
                                runtime.switchTab(ShopTab.BUY)
                                cue("action_inspect")
                            }
                        )

                        TabButton(
                            label = "SELL [2]",
                            count = ui.sellInventory.size,
                            isSelected = !showChatTab && ui.activeTab == ShopTab.SELL,
                            onClick = {
                                showChatTab = false
                                runtime.switchTab(ShopTab.SELL)
                                cue("action_inspect")
                            }
                        )

                        if (ui.smalltalkTopics.isNotEmpty()) {
                            TabButton(
                                label = "CHAT [3]",
                                count = ui.smalltalkTopics.size,
                                isSelected = showChatTab,
                                onClick = {
                                    showChatTab = true
                                    cue("action_inspect")
                                }
                            )
                        }
                    }

                    // Category Filter Chips (when in Buy or Sell view)
                    if (!showChatTab) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ShopCategoryFilter.entries.forEach { filter ->
                                val selected = selectedCategory == filter
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (selected) Color(0xFF13344D) else Color(0xFF0A1926),
                                    border = BorderStroke(1.dp, if (selected) Color(0xFF63E6FF) else Color(0xFF1C364C)),
                                    modifier = Modifier
                                        .desktopPointerHover()
                                        .clickable {
                                            selectedCategory = filter
                                            cue("action_inspect")
                                        }
                                ) {
                                    Text(
                                        text = filter.label,
                                        color = if (selected) Color(0xFF63E6FF) else Color(0xFF8FB0C4),
                                        fontFamily = fonts.orbitron,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error / Unavailable Notice
                if (ui.unavailableMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF261808),
                        border = BorderStroke(1.dp, Color(0xFFFF9800)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ui.unavailableMessage ?: "",
                            color = Color(0xFFFFE0B2),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Main Content View (Buy, Sell, or Chat)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        showChatTab -> {
                            DealerChatPane(
                                dialogueLines = ui.conversationLog,
                                topics = ui.smalltalkTopics,
                                onSelectTopic = { topicId ->
                                    runtime.playSmalltalk(topicId)
                                    cue("confirm")
                                }
                            )
                        }
                        ui.activeTab == ShopTab.BUY -> {
                            val filteredBuyItems = ui.itemsForSale.filter { itemUi ->
                                val realItem = catalogMap[itemUi.id]
                                matchesCategory(selectedCategory, realItem) &&
                                    (searchQuery.isBlank() || itemUi.name.contains(searchQuery, ignoreCase = true))
                            }

                            if (filteredBuyItems.isEmpty()) {
                                EmptyCatalogNotice("No wares available in this category.")
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredBuyItems, key = { it.id }) { itemUi ->
                                        val realItem = catalogMap[itemUi.id]
                                        val equipped = realItem?.let(::resolveEquippedItem)

                                        ShopBuyItemCard(
                                            itemUi = itemUi,
                                            realItem = realItem,
                                            equippedItem = equipped,
                                            services = services,
                                            onBuyOne = {
                                                runtime.buyItem(itemUi.id, 1)
                                            },
                                            onBuyMax = {
                                                if (itemUi.maxQuantity > 1) {
                                                    runtime.buyItem(itemUi.id, itemUi.maxQuantity)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            val filteredSellItems = ui.sellInventory.filter { sellUi ->
                                val realItem = catalogMap[sellUi.id]
                                matchesCategory(selectedCategory, realItem) &&
                                    (searchQuery.isBlank() || sellUi.name.contains(searchQuery, ignoreCase = true))
                            }

                            if (filteredSellItems.isEmpty()) {
                                EmptyCatalogNotice("No inventory items found to sell.")
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredSellItems, key = { it.id }) { sellUi ->
                                        val realItem = catalogMap[sellUi.id]

                                        ShopSellItemCard(
                                            sellUi = sellUi,
                                            realItem = realItem,
                                            services = services,
                                            onSellOne = {
                                                runtime.sellItem(sellUi.id, 1)
                                            },
                                            onSellAll = {
                                                if (sellUi.quantity > 1) {
                                                    runtime.sellItem(sellUi.id, sellUi.quantity)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Transaction Toast & Key Legend Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Transaction feedback message
                    AnimatedVisibility(
                        visible = activeMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3300FF9D),
                            border = BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "✓ ${activeMessage.orEmpty()}",
                                color = Color(0xFF00FF9D),
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (activeMessage == null) {
                        Spacer(Modifier.width(1.dp))
                    }

                    // Key Guide
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopKeyBadge("1", "Buy")
                        DesktopKeyBadge("2", "Sell")
                        if (ui.smalltalkTopics.isNotEmpty()) {
                            DesktopKeyBadge("3", "Chat")
                        }
                        DesktopKeyBadge("Esc", "Exit")
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF133652) else Color(0xFF0A1926),
        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF63E6FF) else Color(0xFF1B354A)),
        modifier = Modifier
            .desktopPointerHover()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF8FB0C4),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = if (isSelected) Color(0xFF63E6FF).copy(alpha = 0.25f) else Color(0xFF152A3B)
            ) {
                Text(
                    text = "$count",
                    color = if (isSelected) Color(0xFF63E6FF) else Color(0xFF6F90A6),
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun ShopBuyItemCard(
    itemUi: ShopItemUi,
    realItem: Item?,
    equippedItem: Item?,
    services: DesktopAppServices,
    onBuyOne: () -> Unit,
    onBuyMax: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val isEquipment = realItem?.equipment != null || realItem?.type in listOf("weapon", "armor", "mod", "accessory")

    val cardContent = @Composable {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (itemUi.locked) Color(0xFF0A121A) else Color(0xFF0D1E2D),
            border = BorderStroke(1.dp, if (itemUi.locked) Color(0xFF1A2B3A) else Color(0xFF1E3C56)),
            modifier = Modifier
                .fillMaxWidth()
                .desktopPointerHover(!itemUi.locked)
                .testTag("shop-item-${itemUi.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Item Identity & Description
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = itemUi.name,
                            color = if (itemUi.locked) Color(0xFF7897AC) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )

                        if (itemUi.rotating) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0x33FFD54F),
                                border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "★ ROTATING",
                                    color = Color(0xFFFFD54F),
                                    fontFamily = fonts.orbitron,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        realItem?.categoryOverride?.let { cat ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0x2263E6FF)
                            ) {
                                Text(
                                    text = cat.uppercase(),
                                    color = Color(0xFF63E6FF),
                                    fontFamily = fonts.orbitron,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (itemUi.locked) {
                        Text(
                            text = itemUi.lockedMessage ?: "Milestone Locked",
                            color = Color(0xFFFF9800),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = itemUi.description ?: "Standard issue equipment.",
                            color = Color(0xFF90B1C4),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Price & Buy Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Price tag
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (itemUi.canAfford) Color(0x2263E6FF) else Color(0x22FF5252),
                        border = BorderStroke(1.dp, if (itemUi.canAfford) Color(0xFF63E6FF).copy(alpha = 0.5f) else Color(0xFFFF5252).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "%,d CR".format(itemUi.price),
                            color = if (itemUi.canAfford) Color(0xFF63E6FF) else Color(0xFFFF5252),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Buy 1
                    Button(
                        onClick = onBuyOne,
                        enabled = !itemUi.locked && itemUi.canAfford && itemUi.maxQuantity > 0,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B4E6B),
                            disabledContainerColor = Color(0xFF132330),
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFF4C6678)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).testTag("buy-button-${itemUi.id}")
                    ) {
                        Text(
                            text = "Buy 1",
                            fontFamily = fonts.orbitron,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Buy Max (if player can afford multiple)
                    if (itemUi.maxQuantity > 1) {
                        OutlinedButton(
                            onClick = onBuyMax,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("buy-max-button-${itemUi.id}")
                        ) {
                            Text(
                                text = "Max (${itemUi.maxQuantity})",
                                color = Color(0xFF63E6FF),
                                fontFamily = fonts.orbitron,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Rich Tooltip: Equipment comparison if gear, else item details
    if (isEquipment && realItem != null) {
        val slotLabel = realItem.equipment?.slot ?: realItem.type
        DesktopRichTooltip(
            tooltip = {
                DesktopEquipmentCompareTooltipContent(
                    candidate = realItem,
                    equipped = equippedItem,
                    slotLabel = slotLabel,
                    services = services
                )
            },
            maxWidth = 360.dp,
            content = cardContent
        )
    } else {
        DesktopRichTooltip(
            tooltip = {
                DesktopItemTooltipContent(
                    item = realItem,
                    fallbackName = itemUi.name,
                    quantity = null,
                    services = services
                )
            },
            maxWidth = 320.dp,
            content = cardContent
        )
    }
}

@Composable
private fun ShopSellItemCard(
    sellUi: SellItemUi,
    realItem: Item?,
    services: DesktopAppServices,
    onSellOne: () -> Unit,
    onSellAll: () -> Unit
) {
    val fonts = LocalStarbornFonts.current

    DesktopRichTooltip(
        tooltip = {
            DesktopItemTooltipContent(
                item = realItem,
                fallbackName = sellUi.name,
                quantity = sellUi.quantity,
                services = services
            )
        },
        maxWidth = 320.dp
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (sellUi.canSell) Color(0xFF0D1E2D) else Color(0xFF0A121A),
            border = BorderStroke(1.dp, if (sellUi.canSell) Color(0xFF1E3C56) else Color(0xFF1A2B3A)),
            modifier = Modifier
                .fillMaxWidth()
                .desktopPointerHover(sellUi.canSell)
                .testTag("sell-item-${sellUi.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Item Identity & Cargo Count
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sellUi.name,
                            color = if (sellUi.canSell) Color.White else Color(0xFF7897AC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF16324A)
                        ) {
                            Text(
                                text = "×${sellUi.quantity} IN CARGO",
                                color = Color(0xFF63E6FF),
                                fontFamily = fonts.orbitron,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!sellUi.canSell) {
                        Text(
                            text = sellUi.reason ?: "Unsellable",
                            color = Color(0xFFFF9800),
                            fontSize = 11.5.sp
                        )
                    } else {
                        Text(
                            text = sellUi.description ?: "Inventory cargo.",
                            color = Color(0xFF90B1C4),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Price & Sell Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Resale value per unit
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0x22FFD54F),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+%,d CR".format(sellUi.price),
                            color = Color(0xFFFFD54F),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Sell 1
                    Button(
                        onClick = onSellOne,
                        enabled = sellUi.canSell,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C4F3C),
                            disabledContainerColor = Color(0xFF132330),
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFF4C6678)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).testTag("sell-button-${sellUi.id}")
                    ) {
                        Text(
                            text = "Sell 1",
                            fontFamily = fonts.orbitron,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sell All
                    if (sellUi.quantity > 1) {
                        val totalEarnings = sellUi.price * sellUi.quantity
                        OutlinedButton(
                            onClick = onSellAll,
                            enabled = sellUi.canSell,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("sell-all-button-${sellUi.id}")
                        ) {
                            Text(
                                text = "All (+%,d CR)".format(totalEarnings),
                                color = Color(0xFF00FF9D),
                                fontFamily = fonts.orbitron,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DealerChatPane(
    dialogueLines: List<ShopDialogueLineUi>,
    topics: List<ShopDialogueTopicUi>,
    onSelectTopic: (String) -> Unit
) {
    val fonts = LocalStarbornFonts.current

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Conversation Log
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF091420),
            border = BorderStroke(1.dp, Color(0xFF173046)),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dialogueLines, key = { it.id }) { line ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = line.speaker ?: "Dealer",
                            color = Color(0xFF63E6FF),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F2233),
                            border = BorderStroke(1.dp, Color(0xFF1E3A52))
                        ) {
                            Text(
                                text = line.text,
                                color = Color(0xFFE0ECF4),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Smalltalk Topic Prompt Buttons
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "INQUIRIES & RUMORS",
                color = Color(0xFF8FB0C4),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            topics.forEach { topic ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0D1E2D),
                    border = BorderStroke(1.dp, Color(0xFF1E3C56)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .desktopPointerHover()
                        .clickable { onSelectTopic(topic.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💬", fontSize = 14.sp)
                        Text(
                            text = topic.label,
                            color = Color(0xFF63E6FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogNotice(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("📦", fontSize = 28.sp)
            Text(
                text = message,
                color = Color(0xFF7897AC),
                fontSize = 13.sp
            )
        }
    }
}
