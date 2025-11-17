package com.example.starborn.feature.shop.ui

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.starborn.feature.shop.SellItemUi
import com.example.starborn.feature.shop.ShopItemUi
import com.example.starborn.feature.shop.ShopTab
import com.example.starborn.feature.shop.ShopUiState
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.feature.shop.ShopDialogueTopicUi
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
        onSmalltalk = viewModel::playSmalltalk,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets,
        voiceoverController = voiceoverController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopScreen(
    state: ShopUiState,
    snackbarHostState: SnackbarHostState,
    onBuy: (String, Int) -> Unit,
    onSell: (String, Int) -> Unit,
    onTabSelected: (ShopTab) -> Unit,
    onSmalltalk: (String) -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    voiceoverController: VoiceoverController
) {
    val context = LocalContext.current
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

    val backgroundColor = if (highContrastMode) Color(0xFF03090F) else MaterialTheme.colorScheme.background
    val dividerColor = if (highContrastMode) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
    val buttonMinHeight = if (largeTouchTargets) 52.dp else 0.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.shopName.ifBlank { "Shop" }) },
                navigationIcon = {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = buttonMinHeight)
                    ) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (highContrastMode) Color(0xFF0B1119) else MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                portraitRes?.let { resId ->
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.shopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Credits: ${state.credits}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider(color = dividerColor)

            ShopDialoguePanel(
                topics = state.smalltalkTopics,
                log = state.conversationLog,
                onSmalltalk = onSmalltalk,
                highContrastMode = highContrastMode,
                largeTouchTargets = largeTouchTargets,
                dividerColor = dividerColor
            )

            ShopTabSelector(
                activeTab = state.activeTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.fillMaxWidth(),
                highContrastMode = highContrastMode,
                largeTouchTargets = largeTouchTargets
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.unavailableMessage != null -> {
                        ShopEmptyState(message = state.unavailableMessage, highContrastMode = highContrastMode)
                    }
                    state.activeTab == ShopTab.BUY && state.itemsForSale.isEmpty() -> {
                        ShopEmptyState(message = "Nothing for sale right now.", highContrastMode = highContrastMode)
                    }
                    state.activeTab == ShopTab.SELL && state.sellInventory.isEmpty() -> {
                        ShopEmptyState(message = "You have nothing to sell.", highContrastMode = highContrastMode)
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
                                    highContrastMode = highContrastMode,
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
                                    highContrastMode = highContrastMode,
                                    largeTouchTargets = largeTouchTargets
                                )
                            }
                        }
                    }
                }
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
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = if (highContrastMode) Color(0xFF0F1319) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    val description = item.description
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${item.price} cr",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            if (item.locked) {
                Text(
                    text = item.lockedMessage ?: "Currently unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Max affordable: ${item.maxQuantity.coerceAtLeast(0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = { onRequestQuantity(item) },
                enabled = item.canAfford && !item.locked && item.maxQuantity > 0,
                modifier = Modifier.heightIn(min = if (largeTouchTargets) 52.dp else 0.dp)
            ) {
                Text(text = "Buy")
            }
        }
    }
}

@Composable
private fun SellItemCard(
    item: SellItemUi,
    onRequestQuantity: (SellItemUi) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = if (highContrastMode) Color(0xFF0F1319) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${item.price} cr",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "In stock: ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
            )
            val reason = item.reason
            if (!item.canSell && reason != null) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { onRequestQuantity(item) },
                enabled = item.canSell && item.quantity > 0,
                modifier = Modifier.heightIn(min = if (largeTouchTargets) 52.dp else 0.dp)
            ) {
                Text("Sell")
            }
        }
    }
}

@Composable
private fun ShopDialoguePanel(
    topics: List<ShopDialogueTopicUi>,
    log: List<ShopDialogueLineUi>,
    onSmalltalk: (String) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    dividerColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        tonalElevation = 2.dp,
        color = if (highContrastMode) Color(0xFF111722) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Smalltalk",
                style = MaterialTheme.typography.titleSmall,
                color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
            )
            if (topics.isEmpty()) {
                Text(
                    text = "Nothing to chat about right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highContrastMode) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    topics.forEach { topic ->
                        Button(
                            onClick = { onSmalltalk(topic.id) },
                            modifier = Modifier.heightIn(min = if (largeTouchTargets) 52.dp else 0.dp)
                        ) {
                            Text(topic.label)
                        }
                    }
                }
            }
            HorizontalDivider(color = dividerColor)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                log.asReversed().take(4).forEach { line ->
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
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
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = activeTab == ShopTab.BUY,
            onClick = { onTabSelected(ShopTab.BUY) },
            label = { Text("Buy") },
            colors = FilterChipDefaults.filterChipColors(),
            modifier = Modifier.heightIn(min = if (largeTouchTargets) 40.dp else 0.dp)
        )
        FilterChip(
            selected = activeTab == ShopTab.SELL,
            onClick = { onTabSelected(ShopTab.SELL) },
            label = { Text("Sell") },
            colors = FilterChipDefaults.filterChipColors(),
            modifier = Modifier.heightIn(min = if (largeTouchTargets) 40.dp else 0.dp)
        )
    }
}

@Composable
private fun ShopEmptyState(
    message: String,
    highContrastMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        color = if (highContrastMode) Color(0xFF131313) else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
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
                color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
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
