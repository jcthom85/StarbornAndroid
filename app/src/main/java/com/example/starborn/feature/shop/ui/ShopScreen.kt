package com.example.starborn.feature.shop.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.R
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
import com.example.starborn.feature.shop.SellItemUi
import com.example.starborn.feature.shop.ShopItemUi
import com.example.starborn.feature.shop.ShopTab
import com.example.starborn.feature.shop.ShopUiState
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.feature.shop.ShopDialogueLineUi
import com.example.starborn.feature.shop.ShopViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopRoute(
    viewModel: ShopViewModel,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    voiceoverController: VoiceoverController
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    ShopScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBuy = viewModel::buyItem,
        onSell = viewModel::sellItem,
        onTabSelected = viewModel::switchTab,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets,
        voiceoverController = voiceoverController
    )
}

private data class ShopColors(
    val accent: Color,
    val accentAlt: Color,
    val panel: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val danger: Color
)

@Composable
private fun rememberShopColors(highContrastMode: Boolean): ShopColors {
    val scheme = MaterialTheme.colorScheme
    return remember(highContrastMode, scheme) {
        val accent = if (highContrastMode) scheme.primary else Color(0xFFFFC857)
        val accentAlt = if (highContrastMode) scheme.secondary else Color(0xFF5EE6F2)
        val base = if (highContrastMode) Color(0xFF0A1018) else Color(0xFF060A0D)
        val panel = base.copy(alpha = if (highContrastMode) 0.96f else 0.84f)
        val card = if (highContrastMode) Color(0xFF121A28) else Color(0xFF12100D).copy(alpha = 0.80f)
        val border = if (highContrastMode) Color.White.copy(alpha = 0.32f) else Color(0xFFFFD17A).copy(alpha = 0.22f)
        ShopColors(
            accent = accent,
            accentAlt = accentAlt,
            panel = panel,
            card = card,
            border = border,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.76f),
            danger = if (highContrastMode) Color(0xFFFF6B6B) else scheme.error
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopScreen(
    state: ShopUiState,
    snackbarHostState: SnackbarHostState,
    onBuy: (String, Int) -> Unit,
    onSell: (String, Int) -> Unit,
    onTabSelected: (ShopTab) -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    voiceoverController: VoiceoverController
) {
    val context = LocalContext.current
    val colors = rememberShopColors(highContrastMode)
    var lastVoiceLineId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.conversationLog) {
        val voiceEntry = state.conversationLog.lastOrNull { !it.voiceCue.isNullOrBlank() }
        val entryId = voiceEntry?.id
        val cue = voiceEntry?.voiceCue
        if (entryId != null && cue != null) {
            if (lastVoiceLineId == null) {
                lastVoiceLineId = entryId
            } else if (entryId != lastVoiceLineId) {
                lastVoiceLineId = entryId
                voiceoverController.enqueue(cue)
            }
        }
    }
    val portraitRes = remember(state.portraitPath) {
        resolvePortraitResource(state.portraitPath, context)
    }
    var pendingPurchase by remember { mutableStateOf<ShopItemUi?>(null) }
    var pendingSale by remember { mutableStateOf<SellItemUi?>(null) }

    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.shop_contraband_background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                StationHeader(
                    title = state.shopName.ifBlank { "Shop" },
                    iconRes = R.drawable.shop_icon,
                    onBack = onBack,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets,
                    actionContentDescription = "Close Shop"
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShopControlPanel(
                    shopName = state.shopName,
                    activeTab = state.activeTab,
                    credits = state.credits,
                    onTabSelected = onTabSelected,
                    colors = colors,
                    largeTouchTargets = largeTouchTargets
                )

                ShopInventoryPanel(
                    activeTab = state.activeTab,
                    itemCount = if (state.activeTab == ShopTab.BUY) state.itemsForSale.size else state.sellInventory.size,
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        state.unavailableMessage != null -> {
                            ShopEmptyState(message = state.unavailableMessage, colors = colors)
                        }
                        state.activeTab == ShopTab.BUY && state.itemsForSale.isEmpty() -> {
                            ShopEmptyState(message = "Nothing for sale right now.", colors = colors)
                        }
                        state.activeTab == ShopTab.SELL && state.sellInventory.isEmpty() -> {
                            ShopEmptyState(message = "You have nothing to sell.", colors = colors)
                        }
                        state.activeTab == ShopTab.BUY -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(state.itemsForSale, key = { it.id }) { item ->
                                    ShopItemCard(
                                        item = item,
                                        onRequestQuantity = { pendingPurchase = it },
                                        colors = colors,
                                        largeTouchTargets = largeTouchTargets
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(state.sellInventory, key = { it.id }) { item ->
                                    SellItemCard(
                                        item = item,
                                        onRequestQuantity = { pendingSale = it },
                                        colors = colors,
                                        largeTouchTargets = largeTouchTargets
                                    )
                                }
                            }
                        }
                    }
                }

                ShopDialogueBar(
                    lines = state.conversationLog,
                    shopName = state.shopName,
                    portraitRes = portraitRes,
                    colors = colors,
                    largeTouchTargets = largeTouchTargets,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    pendingPurchase?.let { target ->
        QuantityPickerDialog(
            title = "Buy ${target.name}",
            maxQuantity = target.maxQuantity,
            confirmLabel = "Buy",
            onConfirm = { quantity ->
                onBuy(target.id, quantity)
                pendingPurchase = null
            },
            onDismiss = { pendingPurchase = null },
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
    }
    pendingSale?.let { target ->
        QuantityPickerDialog(
            title = "Sell ${target.name}",
            maxQuantity = target.quantity,
            confirmLabel = "Sell",
            onConfirm = { quantity ->
                onSell(target.id, quantity)
                pendingSale = null
            },
            onDismiss = { pendingSale = null },
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
    }
}

@Composable
private fun ShopControlPanel(
    shopName: String,
    activeTab: ShopTab,
    credits: Int,
    onTabSelected: (ShopTab) -> Unit,
    colors: ShopColors,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.panel.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.75f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.13f),
                            colors.accentAlt.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "BLACK-MARKET LEDGER",
                        color = colors.accent.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = shopName.ifBlank { "Shop" },
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            shadow = Shadow(
                                color = colors.accent.copy(alpha = 0.35f),
                                blurRadius = 12f
                            )
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CreditsPill(credits = credits, colors = colors)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShopTabSelector(
                    activeTab = activeTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.weight(1f),
                    colors = colors,
                    largeTouchTargets = largeTouchTargets
                )
            }
        }
    }
}

@Composable
private fun ShopInventoryPanel(
    activeTab: ShopTab,
    itemCount: Int,
    colors: ShopColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val title = if (activeTab == ShopTab.BUY) "Available Stock" else "Buyback Manifest"
    Surface(
        modifier = modifier,
        color = colors.panel.copy(alpha = 0.82f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.72f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.accentAlt.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.16f)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = colors.accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemCount LISTED",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = 0.55f),
                                colors.accentAlt.copy(alpha = 0.24f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItemUi,
    onRequestQuantity: (ShopItemUi) -> Unit,
    colors: ShopColors,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val canBuy = item.canAfford && !item.locked && item.maxQuantity > 0
    val availabilityText = when {
        item.locked -> item.lockedMessage ?: "Currently unavailable"
        item.maxQuantity <= 0 -> "Not enough credits"
        else -> "Limit ${item.maxQuantity.coerceAtLeast(0)}"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (canBuy) colors.accent.copy(alpha = 0.32f) else colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = if (canBuy) 0.11f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.rotating) {
                            Surface(
                                color = colors.accentAlt.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, colors.accentAlt.copy(alpha = 0.38f))
                            ) {
                                Text(
                                    text = "FRESH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accentAlt,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    item.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                PriceChip(price = item.price, colors = colors)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = availabilityText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.locked) colors.danger else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onRequestQuantity(item) },
                    enabled = canBuy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color(0xFF181008),
                        disabledContainerColor = Color.White.copy(alpha = 0.10f),
                        disabledContentColor = colors.textSecondary
                    ),
                    modifier = Modifier
                        .heightIn(min = if (largeTouchTargets) 52.dp else 42.dp)
                        .widthIn(min = 104.dp)
                ) {
                    Text(text = "Buy", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PriceChip(
    price: Int,
    colors: ShopColors,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = price.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "CRED",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SellItemCard(
    item: SellItemUi,
    onRequestQuantity: (SellItemUi) -> Unit,
    colors: ShopColors,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val canSell = item.canSell && item.quantity > 0
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (canSell) colors.accentAlt.copy(alpha = 0.30f) else colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.accentAlt.copy(alpha = if (canSell) 0.10f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                PriceChip(price = item.price, colors = colors)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "In pack: ${item.quantity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    val reason = item.reason
                    if (!item.canSell && reason != null) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(
                    onClick = { onRequestQuantity(item) },
                    enabled = canSell,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentAlt,
                        contentColor = Color(0xFF061014),
                        disabledContainerColor = Color.White.copy(alpha = 0.10f),
                        disabledContentColor = colors.textSecondary
                    ),
                    modifier = Modifier
                        .heightIn(min = if (largeTouchTargets) 52.dp else 42.dp)
                        .widthIn(min = 104.dp)
                ) {
                    Text("Sell", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShopTabSelector(
    activeTab: ShopTab,
    onTabSelected: (ShopTab) -> Unit,
    modifier: Modifier = Modifier,
    colors: ShopColors,
    largeTouchTargets: Boolean
) {
    val tabHeight = if (largeTouchTargets) 54.dp else 44.dp
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(ShopTab.BUY to "Buy", ShopTab.SELL to "Sell").forEach { (tab, label) ->
                val active = activeTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = tabHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelected(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) colors.accent.copy(alpha = 0.18f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (active) colors.accent.copy(alpha = 0.50f) else Color.Transparent
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) colors.accent else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditsPill(
    credits: Int,
    colors: ShopColors,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.46f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = credits.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "CREDITS",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent.copy(alpha = 0.86f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShopDialogueBar(
    lines: List<ShopDialogueLineUi>,
    shopName: String,
    portraitRes: Int?,
    colors: ShopColors,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val line = lines.lastOrNull() ?: return
    val padding = if (largeTouchTargets) 14.dp else 12.dp
    val speaker = line.speaker?.takeIf { it.isNotBlank() } ?: shopName.ifBlank { "Shopkeeper" }
    Surface(
        modifier = modifier,
        color = colors.panel.copy(alpha = 0.86f),
        contentColor = Color.White,
        shadowElevation = 9.dp,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.13f),
                            colors.accentAlt.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (portraitRes != null) {
                Image(
                    painter = painterResource(id = portraitRes),
                    contentDescription = speaker,
                    modifier = Modifier
                        .size(if (largeTouchTargets) 56.dp else 48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.24f)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(if (largeTouchTargets) 52.dp else 46.dp)
                        .clip(CircleShape),
                    color = colors.accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.36f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initial = speaker.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = speaker,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShopEmptyState(
    message: String,
    colors: ShopColors
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun QuantityPickerDialog(
    title: String,
    maxQuantity: Int,
    confirmLabel: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val cappedMax = maxQuantity.coerceAtLeast(0)
    var quantity by remember(cappedMax) {
        mutableStateOf(if (cappedMax > 0) 1 else 0)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = quantity > 0 && cappedMax > 0,
                onClick = { onConfirm(quantity) },
                modifier = Modifier.heightIn(min = if (largeTouchTargets) 48.dp else 0.dp)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = if (largeTouchTargets) 48.dp else 0.dp)
            ) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (cappedMax > 0) "Select quantity (max $cappedMax)" else "You cannot perform this action right now.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = quantity > 1,
                        onClick = { quantity = (quantity - 1).coerceAtLeast(1) },
                        modifier = if (largeTouchTargets) Modifier.size(56.dp) else Modifier
                    ) {
                        Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        enabled = quantity < cappedMax,
                        onClick = { quantity = (quantity + 1).coerceAtMost(cappedMax) },
                        modifier = if (largeTouchTargets) Modifier.size(56.dp) else Modifier
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase")
                    }
                }
            }
        }
    )
}

private fun resolvePortraitResource(path: String?, context: Context): Int? {
    if (path.isNullOrBlank()) return null
    val base = path.substringBeforeLast('.', missingDelimiterValue = path)
    val afterImages = base.substringAfter("images/", base)
    val candidates = buildList {
        add(afterImages.substringAfterLast('/'))
        add(afterImages.replace('/', '_'))
        add(base.substringAfterLast('/'))
        add(afterImages.replace('-', '_').replace('/', '_'))
    }.map { candidate ->
        candidate
            .replace('-', '_')
            .filter { it.isLowerCase() || it.isDigit() || it == '_' }
    }.filter { it.isNotBlank() }

    for (name in candidates.distinct()) {
        val normalized = if (name.first().isDigit()) "portrait_$name" else name
        val id = context.resources.getIdentifier(normalized, "drawable", context.packageName)
        if (id != 0) return id
    }
    return null
}
