package com.example.starborn.feature.inventory.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.feature.inventory.InventoryViewModel
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

private const val CATEGORY_ALL = "all"
private const val CATEGORY_CONSUMABLES = "consumables"
private const val CATEGORY_EQUIPMENT = "equipment"
private const val CATEGORY_CRAFTING = "crafting"
private const val CATEGORY_KEY_ITEMS = "key_items"
private const val CATEGORY_OTHER = "other"

enum class InventoryTab { ITEMS, EQUIPMENT }

data class InventoryLaunchOptions(
    val initialTab: InventoryTab? = null,
    val focusSlot: String? = null
)

private fun Item.categoryKey(): String {
    val normalized = type.lowercase(Locale.getDefault())
    return when (normalized) {
        "consumable", "medicine", "food", "drink", "tonic" -> CATEGORY_CONSUMABLES
        "weapon", "armor", "shield", "accessory", "gear" -> CATEGORY_EQUIPMENT
        "material", "ingredient", "component", "resource" -> CATEGORY_CRAFTING
        "key", "key_item", "quest" -> CATEGORY_KEY_ITEMS
        else -> CATEGORY_OTHER
    }
}

private fun categoryLabel(key: String): String = when (key) {
    CATEGORY_ALL -> "All"
    CATEGORY_CONSUMABLES -> "Consumables"
    CATEGORY_EQUIPMENT -> "Equipment"
    CATEGORY_CRAFTING -> "Crafting"
    CATEGORY_KEY_ITEMS -> "Key Items"
    CATEGORY_OTHER -> "Other"
    else -> key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRoute(
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme? = null,
    credits: Int,
    initialTab: InventoryTab? = null,
    focusSlot: String? = null
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val equippedItems by viewModel.equippedItems.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    InventoryScreen(
        entries = entries,
        equippedItems = equippedItems,
        snackbarHostState = snackbarHostState,
        onUseItem = viewModel::useItem,
        onEquipItem = viewModel::equipItem,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets,
        theme = theme,
        credits = credits,
        initialTab = initialTab,
        focusSlot = focusSlot
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryScreen(
    entries: List<InventoryEntry>,
    equippedItems: Map<String, String>,
    snackbarHostState: SnackbarHostState,
    onUseItem: (String) -> Unit,
    onEquipItem: (String, String?) -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme?,
    credits: Int,
    initialTab: InventoryTab?,
    focusSlot: String?
) {
    val normalizedFocusSlot = remember(focusSlot) { focusSlot?.lowercase(Locale.getDefault()) }
    val resolvedInitialTab = remember(initialTab, normalizedFocusSlot) {
        initialTab ?: if (normalizedFocusSlot != null) InventoryTab.EQUIPMENT else InventoryTab.ITEMS
    }
    var activeTab by remember(resolvedInitialTab) { mutableStateOf(resolvedInitialTab) }
    var selectedCategory by remember(entries) { mutableStateOf(CATEGORY_ALL) }
    val categories = remember(entries) {
        val keys = entries.map { it.item.categoryKey() }.toSet()
        buildList {
            add(CATEGORY_ALL)
            listOf(
                CATEGORY_CONSUMABLES,
                CATEGORY_EQUIPMENT,
                CATEGORY_CRAFTING,
                CATEGORY_KEY_ITEMS,
                CATEGORY_OTHER
            ).forEach { key -> if (key in keys) add(key) }
        }
    }
    if (selectedCategory !in categories) {
        selectedCategory = CATEGORY_ALL
    }
    val filteredItems = remember(entries, selectedCategory) {
        when (selectedCategory) {
            CATEGORY_ALL -> entries
            else -> entries.filter { it.item.categoryKey() == selectedCategory }
        }
    }
    var selectedEntry by remember { mutableStateOf(filteredItems.firstOrNull()) }
    LaunchedEffect(filteredItems, activeTab) {
        if (activeTab == InventoryTab.ITEMS) {
            selectedEntry = when {
                filteredItems.isEmpty() -> null
                selectedEntry == null -> filteredItems.first()
                filteredItems.none { it.item.id == selectedEntry?.item?.id } -> filteredItems.first()
                else -> selectedEntry
            }
        }
    }

    val entryById = remember(entries) { entries.associateBy { it.item.id } }
    val equippableEntries = remember(entries) { entries.filter { it.item.equipment != null } }
    val baseSlots = remember(equippableEntries) {
        equippableEntries
            .mapNotNull { it.item.equipment?.slot?.lowercase(Locale.getDefault()) }
            .distinct()
    }
    val slotOptions = remember(baseSlots, equippedItems, normalizedFocusSlot) {
        (baseSlots + equippedItems.keys.map { it.lowercase(Locale.getDefault()) } + listOfNotNull(normalizedFocusSlot))
            .distinct()
            .ifEmpty { listOf("weapon", "armor", "accessory") }
    }
    var selectedSlot by remember(slotOptions, normalizedFocusSlot) {
        mutableStateOf(
            normalizedFocusSlot?.takeIf { slotOptions.contains(it) }
                ?: slotOptions.firstOrNull()
                ?: "weapon"
        )
    }
    val equippedItemNames = remember(equippedItems, entryById) {
        equippedItems.mapValues { (_, itemId) ->
            entryById[itemId]?.item?.name ?: itemId
        }
    }
    var selectedEquipEntry by remember(selectedSlot, equippableEntries) {
        mutableStateOf(
            equippableEntries.firstOrNull { it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true) }
        )
    }
    LaunchedEffect(selectedSlot, equippableEntries, activeTab, equippedItems) {
        if (activeTab == InventoryTab.EQUIPMENT) {
            val normalizedSlot = selectedSlot.lowercase(Locale.getDefault())
            val equippedId = equippedItems[normalizedSlot]
            selectedEquipEntry = equippableEntries.firstOrNull { it.item.id == equippedId }
                ?: equippableEntries.firstOrNull {
                    it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true)
                }
        }
    }

    val backgroundPainter = rememberRoomBackgroundPainter(theme?.backgroundImage)
    val panelColor = themeColor(theme?.bg, Color(0xFF050A10)).copy(alpha = if (highContrastMode) 0.96f else 0.9f)
    val borderColor = themeColor(theme?.border, Color.White.copy(alpha = 0.45f))
    val accentColor = themeColor(theme?.accent, Color(0xFF7CD8FF))
    val foregroundColor = themeColor(theme?.fg, Color.White)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF010308))
        ) {
            BackgroundImage(painter = backgroundPainter)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xE6000000),
                                Color(0xCC00030A)
                            )
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                color = panelColor,
                border = BorderStroke(1.5.dp, borderColor),
                shape = RoundedCornerShape(36.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    InventoryHeader(
                        credits = credits,
                        totalItems = entries.sumOf { it.quantity },
                        onBack = onBack,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        foregroundColor = foregroundColor,
                        largeTouchTargets = largeTouchTargets
                    )
                    InventoryTabRow(
                        selectedTab = activeTab,
                        onSelectTab = { activeTab = it },
                        accentColor = accentColor,
                        borderColor = borderColor,
                        largeTouchTargets = largeTouchTargets
                    )
                    when (activeTab) {
                        InventoryTab.ITEMS -> ItemsTabContent(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { selectedCategory = it },
                            items = filteredItems,
                            selectedItem = selectedEntry,
                            onSelectItem = { selectedEntry = it },
                            onUseItem = onUseItem,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            foregroundColor = foregroundColor,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                        )
                        InventoryTab.EQUIPMENT -> EquipmentTabContent(
                            slots = slotOptions,
                            selectedSlot = selectedSlot,
                            onSelectSlot = { selectedSlot = it },
                            equippableEntries = equippableEntries,
                            selectedEntry = selectedEquipEntry,
                            onSelectEntry = { selectedEquipEntry = it },
                            equippedItems = equippedItems,
                            equippedItemNames = equippedItemNames,
                            onEquipItem = onEquipItem,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            foregroundColor = foregroundColor,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundImage(painter: Painter) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = 0.35f
    )
}

@Composable
private fun InventoryHeader(
    credits: Int,
    totalItems: Int,
    onBack: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val creditsLabel = remember(credits) { formatter.format(credits.coerceAtLeast(0)) }
    val itemsLabel = remember(totalItems) { formatter.format(totalItems.coerceAtLeast(0)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .heightIn(min = if (largeTouchTargets) 56.dp else 48.dp)
                .widthIn(min = 120.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable { onBack() },
            color = Color.Transparent,
            border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = foregroundColor
                )
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = foregroundColor
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Field Inventory",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = foregroundColor
            )
            Text(
                text = "Pack contents synced with crew manifest.",
                style = MaterialTheme.typography.bodySmall,
                color = foregroundColor.copy(alpha = 0.75f)
            )
        }
        InventorySummaryPill(
            label = "Credits",
            value = "$creditsLabel c",
            accentColor = accentColor,
            borderColor = borderColor
        )
        InventorySummaryPill(
            label = "Items",
            value = itemsLabel,
            accentColor = accentColor,
            borderColor = borderColor
        )
    }
}

@Composable
private fun InventorySummaryPill(
    label: String,
    value: String,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 60.dp)
            .widthIn(min = 120.dp),
        color = accentColor.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.9f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun InventoryTabRow(
    selectedTab: InventoryTab,
    onSelectTab: (InventoryTab) -> Unit,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InventoryTabButton(
            label = "Items",
            selected = selectedTab == InventoryTab.ITEMS,
            onClick = { onSelectTab(InventoryTab.ITEMS) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
        InventoryTabButton(
            label = "Equipment",
            selected = selectedTab == InventoryTab.EQUIPMENT,
            onClick = { onSelectTab(InventoryTab.EQUIPMENT) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InventoryTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    val textColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
    Surface(
        modifier = modifier
            .heightIn(min = if (largeTouchTargets) 58.dp else 48.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun ItemsTabContent(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    onSelectItem: (InventoryEntry) -> Unit,
    onUseItem: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InventoryCategoryColumn(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = onSelectCategory,
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets
        )
        InventoryItemsColumn(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            items = items,
            selectedItem = selectedItem,
            onSelectItem = onSelectItem,
            accentColor = accentColor,
            borderColor = borderColor,
            equippedItemId = null,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
        InventoryDetailPanel(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            entry = selectedItem,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets,
            primaryActionLabel = selectedItem?.let { "Use Item" },
            primaryActionEnabled = selectedItem?.item?.effect != null,
            onPrimaryAction = selectedItem?.let { entry -> { onUseItem(entry.item.id) } }
        )
    }
}

@Composable
private fun EquipmentTabContent(
    slots: List<String>,
    selectedSlot: String,
    onSelectSlot: (String) -> Unit,
    equippableEntries: List<InventoryEntry>,
    selectedEntry: InventoryEntry?,
    onSelectEntry: (InventoryEntry) -> Unit,
    equippedItems: Map<String, String>,
    equippedItemNames: Map<String, String>,
    onEquipItem: (String, String?) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SlotColumn(
            slots = slots,
            selectedSlot = selectedSlot,
            onSelectSlot = onSelectSlot,
            equippedItemNames = equippedItemNames,
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets
        )
        val normalizedSlot = selectedSlot.lowercase(Locale.getDefault())
        val equippedItemId = equippedItems[normalizedSlot]
        val slotEntries = equippableEntries.filter {
            it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true)
        }
        InventoryItemsColumn(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            items = slotEntries,
            selectedItem = selectedEntry,
            onSelectItem = onSelectEntry,
            accentColor = accentColor,
            borderColor = borderColor,
            equippedItemId = equippedItemId,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
        InventoryDetailPanel(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            entry = selectedEntry,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets,
            equippedItemName = equippedItemNames[normalizedSlot],
            primaryActionLabel = selectedEntry?.let { "Equip to ${selectedSlot.uppercase(Locale.getDefault())}" },
            primaryActionEnabled = selectedEntry != null && selectedEntry.item.id != equippedItemId,
            onPrimaryAction = selectedEntry?.let { entry ->
                { onEquipItem(normalizedSlot, entry.item.id) }
            },
            secondaryActionLabel = equippedItemId?.let { "Unequip" },
            secondaryActionEnabled = equippedItemId != null,
            onSecondaryAction = equippedItemId?.let { { onEquipItem(normalizedSlot, null) } }
        )
    }
}

@Composable
private fun InventoryCategoryColumn(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { key ->
                val selected = key == selectedCategory
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onSelectCategory(key) },
                    color = if (selected) accentColor.copy(alpha = 0.8f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selected) accentColor else borderColor.copy(alpha = 0.6f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryLabel(key),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) Color(0xFF020409) else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotColumn(
    slots: List<String>,
    selectedSlot: String,
    onSelectSlot: (String) -> Unit,
    equippedItemNames: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            slots.forEach { slot ->
                val selected = slot.equals(selectedSlot, ignoreCase = true)
                val equippedLabel = equippedItemNames[slot.lowercase(Locale.getDefault())]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onSelectSlot(slot) },
                    color = if (selected) accentColor.copy(alpha = 0.8f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selected) accentColor else borderColor.copy(alpha = 0.6f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = slot.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected) Color(0xFF020409) else Color.White.copy(alpha = 0.85f)
                            )
                            equippedLabel?.takeIf { it.isNotBlank() }?.let { label ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) Color(0xFF010308) else Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemsColumn(
    modifier: Modifier,
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    onSelectItem: (InventoryEntry) -> Unit,
    accentColor: Color,
    borderColor: Color,
    equippedItemId: String?,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = if (highContrastMode) 0.35f else 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (items.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "No items in this category yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                items(items, key = { it.item.id }) { entry ->
                    InventoryItemRow(
                        entry = entry,
                        selected = entry.item.id == selectedItem?.item?.id,
                        isEquipped = equippedItemId?.equals(entry.item.id, ignoreCase = true) == true,
                        accentColor = accentColor,
                        onClick = { onSelectItem(entry) },
                        largeTouchTargets = largeTouchTargets
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryItemRow(
    entry: InventoryEntry,
    selected: Boolean,
    isEquipped: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    largeTouchTargets: Boolean
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val borderColor = if (selected) accentColor else Color.White.copy(alpha = 0.25f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 76.dp else 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconRes = remember(entry.item.id) { itemIconRes(entry.item) }
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.item.description?.takeIf { it.isNotBlank() } ?: entry.item.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isEquipped) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "x${entry.quantity}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                entry.item.rarity?.let {
                    Text(
                        text = it.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryDetailPanel(
    modifier: Modifier,
    entry: InventoryEntry?,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    equippedItemName: String? = null,
    primaryActionLabel: String? = null,
    primaryActionEnabled: Boolean = true,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionEnabled: Boolean = true,
    onSecondaryAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (entry == null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Select an item to inspect its details.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = entry.item.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = foregroundColor
                )
                Text(
                    text = "Quantity: ${entry.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                entry.item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                equippedItemName?.takeIf { it.isNotBlank() }?.let { label ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                    SectionHeading("Currently Equipped", accentColor)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                StatRow("Type", entry.item.type, accentColor)
                StatRow("Value", "${entry.item.value} c", accentColor)
                entry.item.rarity?.let { StatRow("Rarity", it, accentColor) }
                entry.item.buyPrice?.let { StatRow("Purchase", "${it} c", accentColor) }

                entry.item.equipment?.let { equipment ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                    SectionHeading("Equipment Stats", accentColor)
                    equipment.weaponType?.let { StatRow("Weapon Type", it, accentColor) }
                    val damageMin = equipment.damageMin
                    val damageMax = equipment.damageMax
                    if (damageMin != null || damageMax != null) {
                        val label = when {
                            damageMin != null && damageMax != null && damageMin != damageMax ->
                                "${damageMin}–${damageMax}"
                            damageMin != null -> damageMin.toString()
                            else -> damageMax.toString()
                        }
                        StatRow("Damage", label, accentColor)
                    }
                    equipment.defense?.let { StatRow("Defense", it.toString(), accentColor) }
                    equipment.hpBonus?.let { StatRow("HP Bonus", "+$it", accentColor) }
                    equipment.statMods?.forEach { (stat, value) ->
                        StatRow(stat.uppercase(Locale.getDefault()), formatSigned(value), accentColor)
                    }
                }

                entry.item.effect?.let { effect ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                    SectionHeading("Item Effect", accentColor)
                    EffectRows(effect, accentColor)
                }

                Spacer(modifier = Modifier.weight(1f))
                val buttonMinHeight = if (largeTouchTargets) 56.dp else 48.dp
                if (entry != null && primaryActionLabel != null && onPrimaryAction != null) {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = primaryActionEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = buttonMinHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF010308),
                            disabledContainerColor = Color.White.copy(alpha = 0.1f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(primaryActionLabel)
                    }
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        enabled = secondaryActionEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    label: String,
    accentColor: Color
) {
    Text(
        text = label.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = accentColor
    )
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor
        )
    }
}

@Composable
private fun EffectRows(effect: ItemEffect, accentColor: Color) {
    effect.restoreHp?.takeIf { it > 0 }?.let { StatRow("Restore HP", "+$it", accentColor) }
    effect.restoreRp?.takeIf { it > 0 }?.let { StatRow("Restore RP", "+$it", accentColor) }
    effect.damage?.takeIf { it > 0 }?.let { StatRow("Damage", "$it", accentColor) }
    effect.learnSchematic?.let { StatRow("Schematic", it, accentColor) }
    effect.singleBuff?.let { buff ->
        StatRow("Buff", "${buff.stat}+${buff.value}", accentColor)
    }
    effect.buffs?.forEach { buff ->
        StatRow("Buff", "${buff.stat}+${buff.value}", accentColor)
    }
}

@DrawableRes
private fun itemIconRes(item: Item?): Int {
    if (item == null) return R.drawable.item_icon_generic
    val normalizedType = item.type.lowercase(Locale.getDefault())
    val name = item.name.lowercase(Locale.getDefault())
    return when {
        normalizedType.contains("food") || listOf("stew", "salad", "ramen", "cake").any { name.contains(it) } ->
            R.drawable.item_icon_food
        normalizedType in setOf("consumable", "medicine", "tonic", "drink") ->
            R.drawable.item_icon_consumable
        normalizedType.contains("fish") ->
            R.drawable.item_icon_fish
        normalizedType.contains("ingredient") || normalizedType.contains("material") ->
            R.drawable.item_icon_ingredient
        normalizedType.contains("fishing") ->
            R.drawable.item_icon_fishing
        normalizedType.contains("lure") ->
            R.drawable.item_icon_lure
        normalizedType in setOf("weapon", "armor", "accessory", "gear") || item.equipment != null -> {
            val slot = item.equipment?.slot?.lowercase(Locale.getDefault())
            when {
                slot == "armor" && name.contains("glove") -> R.drawable.item_icon_gloves
                slot == "armor" && name.contains("pendant") -> R.drawable.item_icon_pendant
                slot == "armor" -> R.drawable.item_icon_armor
                slot == "accessory" -> R.drawable.item_icon_accessory
                slot == "weapon" && (name.contains("gun") || name.contains("pistol") || name.contains("rifle")) ->
                    R.drawable.item_icon_gun
                slot == "weapon" -> R.drawable.item_icon_sword
                else -> R.drawable.item_icon_generic
            }
        }
        else -> R.drawable.item_icon_generic
    }
}

private fun formatSigned(value: Int): String =
    if (value >= 0) "+$value" else value.toString()
