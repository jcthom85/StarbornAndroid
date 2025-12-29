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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    val panel: Color,
    val panelAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val danger: Color
)

@Composable
private fun rememberShopColors(highContrastMode: Boolean): ShopColors {
    val scheme = MaterialTheme.colorScheme
    return remember(highContrastMode, scheme) {
        val accent = if (highContrastMode) scheme.primary else Color(0xFF7BE4FF)
        val base = if (highContrastMode) Color(0xFF0A1018) else Color(0xFF050A12)
        val panel = base.copy(alpha = if (highContrastMode) 0.96f else 0.88f)
        val panelAlt = if (highContrastMode) Color(0xFF0E1623) else base.copy(alpha = 0.78f)
        val border = if (highContrastMode) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.18f)
        ShopColors(
            accent = accent,
            panel = panel,
            panelAlt = panelAlt,
            border = border,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.78f),
            divider = Color.White.copy(alpha = 0.10f),
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
        backgroundRes = R.drawable.market_1,
        vignetteRes = R.drawable.shop_vignette
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
                    actionLabel = "Leave"
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShopTabSelector(
                        activeTab = state.activeTab,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        largeTouchTargets = largeTouchTargets
                    )
                    CreditsPill(credits = state.credits, colors = colors)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = colors.panel,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
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
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
private fun ShopItemCard(
    item: ShopItemUi,
    onRequestQuantity: (ShopItemUi) -> Unit,
    colors: ShopColors,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val canBuy = item.canAfford && !item.locked && item.maxQuantity > 0
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.panelAlt,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                        if (item.rotating) {
                            Surface(
                                color = colors.accent.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "Fresh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    item.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "${item.price} c",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            when {
                item.locked -> Text(
                    text = item.lockedMessage ?: "Currently unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger
                )
                item.maxQuantity <= 0 -> Text(
                    text = "Not enough credits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                else -> Text(
                    text = "Max affordable: ${item.maxQuantity.coerceAtLeast(0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Button(
                onClick = { onRequestQuantity(item) },
                enabled = canBuy,
                modifier = Modifier
                    .heightIn(min = if (largeTouchTargets) 52.dp else 0.dp)
                    .widthIn(min = 140.dp)
            ) {
                Text(text = if (canBuy) "Buy" else "Unavailable")
            }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.panelAlt,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "${item.price} c",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
            Text(
                text = "In pack: ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            val reason = item.reason
            if (!item.canSell && reason != null) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger
                )
            }
            Button(
                onClick = { onRequestQuantity(item) },
                enabled = item.canSell && item.quantity > 0,
                modifier = Modifier
                    .heightIn(min = if (largeTouchTargets) 52.dp else 0.dp)
                    .widthIn(min = 140.dp)
            ) {
                Text("Sell")
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
    Row(modifier = modifier) {
        listOf(ShopTab.BUY to "Buy", ShopTab.SELL to "Sell").forEach { (tab, label) ->
            val active = activeTab == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = tabHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (active) colors.accent else colors.textSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) colors.accent else colors.border.copy(alpha = 0.4f))
                )
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
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Text(
            text = "$credits c",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
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
    val padding = if (largeTouchTargets) 18.dp else 14.dp
    val speaker = line.speaker?.takeIf { it.isNotBlank() } ?: shopName.ifBlank { "Shopkeeper" }
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.82f),
        contentColor = Color.White,
        shadowElevation = 12.dp,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (portraitRes != null) {
                    Image(
                        painter = painterResource(id = portraitRes),
                        contentDescription = speaker,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val initial = speaker.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(
                    text = speaker,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f)
            )
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
