package com.example.starborn.feature.inventory.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.feature.inventory.InventoryViewModel
import com.example.starborn.feature.inventory.PartyMemberStatus
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.components.ItemTargetSelectionDialog
import com.example.starborn.ui.components.TargetSelectionOption
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.roundToInt

private const val CATEGORY_ALL = "all"
private const val CATEGORY_CONSUMABLES = "consumables"
private const val CATEGORY_EQUIPMENT = "equipment"
private const val CATEGORY_CRAFTING = "crafting"
private const val CATEGORY_KEY_ITEMS = "key_items"
private const val CATEGORY_OTHER = "other"

enum class InventoryTab { SUPPLIES, GEAR, KEY_ITEMS }

data class InventoryLaunchOptions(
    val initialTab: InventoryTab? = null,
    val focusSlot: String? = null
)

private fun Item.categoryKey(): String {
    categoryOverride?.let { return it.lowercase(Locale.getDefault()) }
    if (equipment != null) return CATEGORY_EQUIPMENT
    val normalized = type.lowercase(Locale.getDefault())
    return when (normalized) {
        "consumable", "medicine", "food", "drink", "tonic" -> CATEGORY_CONSUMABLES
        "weapon", "armor", "shield", "accessory", "gear", "snack" -> CATEGORY_EQUIPMENT
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
    val partyMembers by viewModel.partyMembers.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.syncFromSession()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    InventoryScreen(
        entries = entries,
        equippedItems = equippedItems,
        partyMembers = partyMembers,
        snackbarHostState = snackbarHostState,
        onUseItem = viewModel::useItem,
        onEquipItem = viewModel::equipItem,
        onRefreshInventory = viewModel::syncFromSession,
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
    partyMembers: List<PartyMemberStatus>,
    snackbarHostState: SnackbarHostState,
    onUseItem: (String, String?) -> Unit,
    onEquipItem: (String, String?, String) -> Unit,
    onRefreshInventory: () -> Unit,
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
        initialTab ?: if (normalizedFocusSlot != null) InventoryTab.GEAR else InventoryTab.SUPPLIES
    }
    var activeTab by remember(resolvedInitialTab) { mutableStateOf(resolvedInitialTab) }
    val suppliesEntries = remember(entries) {
        entries.filter {
            when (it.item.categoryKey()) {
                CATEGORY_EQUIPMENT, CATEGORY_KEY_ITEMS -> false
                else -> true
            }
        }
    }
    val keyItemEntries = remember(entries) {
        entries.filter { it.item.categoryKey() == CATEGORY_KEY_ITEMS }
    }
    var selectedCategory by remember(suppliesEntries) { mutableStateOf(CATEGORY_ALL) }
    var selectedCharacterId by remember(partyMembers) { mutableStateOf(partyMembers.firstOrNull()?.id) }
    LaunchedEffect(partyMembers) {
        val ids = partyMembers.map { it.id }
        if (selectedCharacterId !in ids) {
            selectedCharacterId = ids.firstOrNull()
        }
    }
    val categories = remember(suppliesEntries) {
        val keys = suppliesEntries.map { it.item.categoryKey() }.toSet()
        buildList {
            add(CATEGORY_ALL)
            listOf(
                CATEGORY_CONSUMABLES,
                CATEGORY_CRAFTING,
                CATEGORY_OTHER
            ).forEach { key -> if (key in keys) add(key) }
        }
    }
    if (selectedCategory !in categories) {
        selectedCategory = CATEGORY_ALL
    }
    val filteredSupplies = remember(suppliesEntries, selectedCategory) {
        when (selectedCategory) {
            CATEGORY_ALL -> suppliesEntries
            else -> suppliesEntries.filter { it.item.categoryKey() == selectedCategory }
        }
    }
    var selectedSupplyEntry by remember { mutableStateOf(filteredSupplies.firstOrNull()) }
    var pendingTargetItem by remember { mutableStateOf<InventoryEntry?>(null) }
    var showTargetDialog by remember { mutableStateOf(false) }
    LaunchedEffect(filteredSupplies, activeTab) {
        if (activeTab == InventoryTab.SUPPLIES) {
            selectedSupplyEntry = when {
                filteredSupplies.isEmpty() -> null
                selectedSupplyEntry == null -> filteredSupplies.first()
                filteredSupplies.none { it.item.id == selectedSupplyEntry?.item?.id } -> filteredSupplies.first()
                else -> selectedSupplyEntry
            }
        }
    }
    var selectedKeyEntry by remember { mutableStateOf(keyItemEntries.firstOrNull()) }
    LaunchedEffect(keyItemEntries, activeTab) {
        if (activeTab == InventoryTab.KEY_ITEMS) {
            selectedKeyEntry = when {
                keyItemEntries.isEmpty() -> null
                selectedKeyEntry == null -> keyItemEntries.first()
                keyItemEntries.none { it.item.id == selectedKeyEntry?.item?.id } -> keyItemEntries.first()
                else -> selectedKeyEntry
            }
        }
    }

    val entryById = remember(entries) { entries.associateBy { it.item.id } }
    val equippableEntries = remember(entries) { entries.filter { it.item.equipment != null } }
    val characterEquippedItems = remember(equippedItems, selectedCharacterId) {
        equippedForCharacter(equippedItems, selectedCharacterId)
    }
    val baseSlots = remember(equippableEntries) {
        equippableEntries
            .mapNotNull { it.item.equipment?.slot?.lowercase(Locale.getDefault()) }
            .distinct()
    }
    val slotOptions = remember(baseSlots, characterEquippedItems, normalizedFocusSlot) {
        (baseSlots + characterEquippedItems.keys.map { it.lowercase(Locale.getDefault()) } + listOfNotNull(normalizedFocusSlot))
            .distinct()
            .ifEmpty { listOf("weapon", "armor", "accessory", "snack") }
    }
    var selectedSlot by remember(slotOptions, normalizedFocusSlot) {
        mutableStateOf(
            normalizedFocusSlot?.takeIf { slotOptions.contains(it) }
                ?: slotOptions.firstOrNull()
                ?: "weapon"
        )
    }
    LaunchedEffect(selectedCharacterId, slotOptions) {
        if (selectedSlot !in slotOptions && slotOptions.isNotEmpty()) {
            selectedSlot = slotOptions.first()
        }
    }
    val equippedItemNames = remember(characterEquippedItems, entryById) {
        characterEquippedItems.mapValues { (_, itemId) ->
            entryById[itemId]?.item?.name ?: itemId
                .takeIf { it.isNotBlank() }
                ?.split('_', ' ')
                ?.filter { it.isNotBlank() }
                ?.joinToString(" ") { part ->
                    part.replaceFirstChar { c ->
                        if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
                    }
                }.orEmpty()
        }
    }
    var selectedEquipEntry by remember(selectedSlot, equippableEntries) {
        mutableStateOf(
            equippableEntries.firstOrNull { it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true) }
        )
    }
    LaunchedEffect(selectedSlot, equippableEntries, activeTab, characterEquippedItems) {
        if (activeTab == InventoryTab.GEAR) {
            val normalizedSlot = selectedSlot.lowercase(Locale.getDefault())
            val equippedId = characterEquippedItems[normalizedSlot]
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

                    if (entries.isEmpty()) {
                        InventoryEmptyState(
                            accentColor = accentColor,
                            borderColor = borderColor,
                            foregroundColor = foregroundColor,
                            onRefreshInventory = onRefreshInventory,
                            largeTouchTargets = largeTouchTargets
                        )
                    } else {
                        val promptUse: (InventoryEntry) -> Unit = inner@{ entry ->
                            val effect = entry.item.effect ?: return@inner
                            val targetMode = effect.target?.lowercase(Locale.getDefault()) ?: "any"
                            when {
                                targetMode == "party" -> onUseItem(entry.item.id, null)
                                partyMembers.isEmpty() -> onUseItem(entry.item.id, null)
                                else -> {
                                    pendingTargetItem = entry
                                    showTargetDialog = true
                                }
                            }
                        }

                        when (activeTab) {
                            InventoryTab.SUPPLIES -> SuppliesTabContent(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onSelectCategory = { selectedCategory = it },
                                items = filteredSupplies,
                                selectedItem = selectedSupplyEntry,
                                onSelectItem = { entry ->
                                    selectedSupplyEntry = entry
                                    if (entry.item.effect != null) {
                                        promptUse(entry)
                                    }
                                },
                                onUseItem = { entry ->
                                    promptUse(entry)
                                },
                                credits = credits,
                                accentColor = accentColor,
                                borderColor = borderColor,
                                foregroundColor = foregroundColor,
                                highContrastMode = highContrastMode,
                                largeTouchTargets = largeTouchTargets
                        )
                        InventoryTab.GEAR -> EquipmentTabContent(
                            partyMembers = partyMembers,
                            selectedCharacterId = selectedCharacterId,
                            onSelectCharacter = { selectedCharacterId = it },
                            slots = slotOptions,
                            selectedSlot = selectedSlot,
                            onSelectSlot = { selectedSlot = it },
                            equippableEntries = equippableEntries,
                            selectedEntry = selectedEquipEntry,
                            onSelectEntry = { selectedEquipEntry = it },
                            equippedItems = characterEquippedItems,
                            equippedItemNames = equippedItemNames,
                            onEquipItem = { slot, itemId ->
                                selectedCharacterId?.let { charId ->
                                    onEquipItem(slot, itemId, charId)
                                }
                            },
                            accentColor = accentColor,
                            borderColor = borderColor,
                            foregroundColor = foregroundColor,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                            )
                            InventoryTab.KEY_ITEMS -> KeyItemsTabContent(
                                items = keyItemEntries,
                                selectedItem = selectedKeyEntry,
                                onSelectItem = { selectedKeyEntry = it },
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
    if (showTargetDialog && pendingTargetItem != null) {
        val targets = partyMembers.map {
            TargetSelectionOption(
                id = it.id,
                name = it.name,
                detail = "HP ${it.hp}/${it.maxHp}"
            )
        }
        ItemTargetSelectionDialog(
            itemName = pendingTargetItem!!.item.name,
            targets = targets,
            onSelect = { targetId ->
                onUseItem(pendingTargetItem!!.item.id, targetId)
                pendingTargetItem = null
                showTargetDialog = false
            },
            onDismiss = {
                pendingTargetItem = null
                showTargetDialog = false
            },
            backgroundColor = panelColor.copy(alpha = 0.8f),
            borderColor = borderColor,
            textColor = foregroundColor,
            accentColor = accentColor
        )
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
            label = "Supplies",
            selected = selectedTab == InventoryTab.SUPPLIES,
            onClick = { onSelectTab(InventoryTab.SUPPLIES) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
        InventoryTabButton(
            label = "Gear",
            selected = selectedTab == InventoryTab.GEAR,
            onClick = { onSelectTab(InventoryTab.GEAR) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
        InventoryTabButton(
            label = "Key Items",
            selected = selectedTab == InventoryTab.KEY_ITEMS,
            onClick = { onSelectTab(InventoryTab.KEY_ITEMS) },
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
private fun InventoryEmptyState(
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    onRefreshInventory: () -> Unit,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Inventory not loaded",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = foregroundColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sync from your save to see gear and supplies.",
                style = MaterialTheme.typography.bodyMedium,
                color = foregroundColor.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            val minHeight = if (largeTouchTargets) 56.dp else 48.dp
            Button(
                onClick = onRefreshInventory,
                modifier = Modifier
                    .heightIn(min = minHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color(0xFF010308)
                )
            ) {
                Text("Reload Inventory")
            }
        }
    }
}

@Composable
private fun SuppliesTabContent(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    onSelectItem: (InventoryEntry) -> Unit,
    onUseItem: (InventoryEntry) -> Unit,
    credits: Int,
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
            credits = credits,
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
            onPrimaryAction = selectedItem?.let { entry -> { onUseItem(entry) } }
        )
    }
}

@Composable
private fun KeyItemsTabContent(
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    onSelectItem: (InventoryEntry) -> Unit,
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
        InventoryItemsColumn(
            modifier = Modifier
                .weight(0.6f)
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
            largeTouchTargets = largeTouchTargets
        )
    }
}

@Composable
private fun EquipmentTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    onSelectCharacter: (String) -> Unit,
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
    val normalizedSlot = selectedSlot.lowercase(Locale.getDefault())
    val equippedItemId = equippedItems[normalizedSlot]
    val equippedEntry = equippableEntries.firstOrNull { it.item.id == equippedItemId }
    val slotEntries = equippableEntries.filter {
        it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (partyMembers.isNotEmpty()) {
            PartyPickerRow(
                members = partyMembers,
                selectedId = selectedCharacterId,
                onSelect = onSelectCharacter,
                accentColor = accentColor,
                borderColor = borderColor,
                largeTouchTargets = largeTouchTargets
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoadoutColumn(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                slots = slots,
                selectedSlot = selectedSlot,
                onSelectSlot = onSelectSlot,
                equippedItemNames = equippedItemNames,
                accentColor = accentColor,
                borderColor = borderColor,
                largeTouchTargets = largeTouchTargets
            )
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InventoryItemsColumn(
                    modifier = Modifier.weight(1f),
                    items = slotEntries,
                    selectedItem = selectedEntry,
                    onSelectItem = onSelectEntry,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    equippedItemId = equippedItemId,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets,
                    statSummary = { entry -> primaryStatSummary(entry.item) }
                )
                ComparisonPanel(
                    slotLabel = selectedSlot,
                    selectedEntry = selectedEntry,
                    equippedEntry = equippedEntry,
                    equippedItemName = equippedItemNames[normalizedSlot],
                    accentColor = accentColor,
                    borderColor = borderColor,
                    foregroundColor = foregroundColor,
                    largeTouchTargets = largeTouchTargets,
                    onEquip = selectedEntry?.let { entry ->
                        if (entry.item.id != equippedItemId) {
                            { onEquipItem(normalizedSlot, entry.item.id) }
                        } else null
                    },
                    onUnequip = equippedItemId?.let { { onEquipItem(normalizedSlot, null) } }
                )
            }
        }
    }
}

@Composable
private fun PartyPickerRow(
    members: List<PartyMemberStatus>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        members.forEach { member ->
            val selected = member.id == selectedId
            Surface(
                onClick = { onSelect(member.id) },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (selected) accentColor else borderColor.copy(alpha = 0.6f)),
                color = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                modifier = Modifier
                    .heightIn(min = if (largeTouchTargets) 44.dp else 36.dp)
            ) {
                Text(
                    text = member.name,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun InventoryCategoryColumn(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    credits: Int,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val creditsLabel = remember(credits) { formatter.format(credits.coerceAtLeast(0)) }

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
            SectionHeading(label = "Wallet", accentColor = accentColor)
            WalletSummaryCard(
                creditsLabel = creditsLabel,
                accentColor = accentColor,
                borderColor = borderColor
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f))
            Text(
                text = "Supplies",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
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
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WalletSummaryCard(
    creditsLabel: String,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "WALLET",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor.copy(alpha = 0.85f)
            )
            Text(
                text = "$creditsLabel c",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LoadoutColumn(
    modifier: Modifier,
    slots: List<String>,
    selectedSlot: String,
    onSelectSlot: (String) -> Unit,
    equippedItemNames: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            SectionHeading(label = "Loadout Overview", accentColor = accentColor)
            Text(
                text = "Tap a slot to focus matching gear.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                slots.forEach { slot ->
                    val normalized = slot.lowercase(Locale.getDefault())
                    val equippedLabel = equippedItemNames[normalized]
                    LoadoutCard(
                        slotName = slot,
                        equippedItemName = equippedLabel,
                        selected = slot.equals(selectedSlot, ignoreCase = true),
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onClick = { onSelectSlot(slot) },
                        largeTouchTargets = largeTouchTargets
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadoutCard(
    slotName: String,
    equippedItemName: String?,
    selected: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    largeTouchTargets: Boolean
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val border = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
    val slotLabel = slotName.uppercase(Locale.getDefault())
    val equippedLabel = equippedItemName?.takeIf { it.isNotBlank() } ?: "Empty"
    val equippedColor = if (equippedItemName.isNullOrBlank()) {
        Color.White.copy(alpha = 0.5f)
    } else {
        Color.White
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 76.dp else 64.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(width = if (selected) 1.5.dp else 1.dp, color = border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 46.dp)
                    .heightIn(min = 46.dp),
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = slotLabel.take(3),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = equippedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = equippedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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
    largeTouchTargets: Boolean,
    statSummary: (InventoryEntry) -> String? = { null }
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = if (highContrastMode) 0.35f else 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        val listState = rememberLazyListState()
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
                state = listState,
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
                        largeTouchTargets = largeTouchTargets,
                        statSummary = statSummary(entry)
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
    largeTouchTargets: Boolean,
    statSummary: String? = null
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
                statSummary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
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
private fun ComparisonPanel(
    slotLabel: String,
    selectedEntry: InventoryEntry?,
    equippedEntry: InventoryEntry?,
    equippedItemName: String?,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onEquip: (() -> Unit)?,
    onUnequip: (() -> Unit)?
) {
    val equippedName = equippedEntry?.item?.name ?: equippedItemName
    val selectedName = selectedEntry?.item?.name
    val comparisonRows = remember(selectedEntry, equippedEntry) {
        buildComparisonRows(equippedEntry?.item, selectedEntry?.item)
    }
    val buttonMinHeight = if (largeTouchTargets) 56.dp else 48.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeading(label = "Item Picker & Comparison", accentColor = accentColor)
            Text(
                text = slotLabel.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = foregroundColor.copy(alpha = 0.85f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ComparisonPill(
                    title = "Equipped",
                    value = equippedName ?: "Empty",
                    borderColor = borderColor,
                    accentColor = accentColor,
                    highlight = false,
                    valueColor = foregroundColor,
                    modifier = Modifier.weight(1f)
                )
                ComparisonPill(
                    title = "Selected",
                    value = selectedName ?: "Choose an item",
                    borderColor = borderColor,
                    accentColor = accentColor,
                    highlight = selectedEntry != null,
                    valueColor = foregroundColor,
                    modifier = Modifier.weight(1f)
                )
            }
            selectedEntry?.item?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.8f)
                )
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.35f))
            if (selectedEntry == null) {
                Text(
                    text = "Select an item on the right to compare with your current loadout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor.copy(alpha = 0.75f)
                )
            } else if (comparisonRows.isEmpty()) {
                Text(
                    text = "No comparable stats for this item type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor.copy(alpha = 0.75f)
                )
            } else {
                comparisonRows.forEach { row ->
                    ComparisonStatRow(row = row, accentColor = accentColor, textColor = foregroundColor)
                }
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onEquip?.invoke() },
                    enabled = onEquip != null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonMinHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF010308),
                        disabledContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(text = "Equip to ${slotLabel.uppercase(Locale.getDefault())}")
                }
                if (onUnequip != null) {
                    OutlinedButton(
                        onClick = onUnequip,
                        modifier = Modifier
                            .weight(0.6f)
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text("Unequip")
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonPill(
    title: String,
    value: String,
    borderColor: Color,
    accentColor: Color,
    highlight: Boolean,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (highlight) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(1.dp, if (highlight) accentColor else borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComparisonStatRow(
    row: StatComparisonRow,
    accentColor: Color,
    textColor: Color
) {
    val deltaColor = when {
        row.delta == null || row.delta == 0 -> textColor.copy(alpha = 0.7f)
        row.delta > 0 -> accentColor
        else -> Color(0xFFFF6B6B)
    }
    val deltaLabel = row.delta?.takeIf { it != 0 }?.let { formatSigned(it) } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = row.equippedValue ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.8f),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.9f)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = row.selectedValue ?: "—",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
            Text(
                text = deltaLabel,
                style = MaterialTheme.typography.labelSmall,
                color = deltaColor
            )
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

private data class StatComparisonRow(
    val label: String,
    val equippedValue: String?,
    val selectedValue: String?,
    val delta: Int?
)

private data class StatValue(
    val display: String,
    val numeric: Int?
)

private fun buildComparisonRows(
    equippedItem: Item?,
    selectedItem: Item?
): List<StatComparisonRow> {
    val equippedStats = equipmentStats(equippedItem?.equipment)
    val selectedStats = equipmentStats(selectedItem?.equipment)
    if (equippedStats.isEmpty() && selectedStats.isEmpty()) return emptyList()

    val labels = linkedSetOf<String>()
    labels.addAll(listOf("Damage", "Defense", "HP Bonus", "Weapon Type"))
    labels.addAll(selectedStats.keys)
    labels.addAll(equippedStats.keys)

    return labels.mapNotNull { label ->
        val equipped = equippedStats[label]
        val candidate = selectedStats[label]
        if (equipped == null && candidate == null) return@mapNotNull null
        val delta = when {
            candidate?.numeric != null || equipped?.numeric != null ->
                (candidate?.numeric ?: 0) - (equipped?.numeric ?: 0)
            else -> null
        }
        StatComparisonRow(
            label = label,
            equippedValue = equipped?.display,
            selectedValue = candidate?.display,
            delta = delta
        )
    }
}

private fun equipmentStats(equipment: Equipment?): Map<String, StatValue> {
    if (equipment == null) return emptyMap()
    val stats = mutableListOf<Pair<String, StatValue>>()
    formatDamageLabel(equipment)?.let { damage ->
        stats += "Damage" to StatValue(damage, averageDamageValue(equipment))
    }
    equipment.defense?.let { stats += "Defense" to StatValue(it.toString(), it) }
    equipment.hpBonus?.let { stats += "HP Bonus" to StatValue("+$it", it) }
    equipment.weaponType?.let { stats += "Weapon Type" to StatValue(it, null) }
    equipment.statMods?.forEach { (stat, value) ->
        val label = stat.uppercase(Locale.getDefault())
        stats += label to StatValue(formatSigned(value), value)
    }
    return stats.toMap()
}

private fun formatDamageLabel(equipment: Equipment): String? {
    val damageMin = equipment.damageMin
    val damageMax = equipment.damageMax
    return when {
        damageMin == null && damageMax == null -> null
        damageMin != null && damageMax != null && damageMin != damageMax -> "${damageMin}–${damageMax}"
        damageMin != null -> damageMin.toString()
        else -> damageMax.toString()
    }
}

private fun averageDamageValue(equipment: Equipment): Int? {
    val values = listOfNotNull(equipment.damageMin, equipment.damageMax)
    if (values.isEmpty()) return null
    return values.average().roundToInt()
}

private fun primaryStatSummary(item: Item): String? {
    val equipment = item.equipment ?: return null
    formatDamageLabel(equipment)?.let { return "DMG $it" }
    equipment.defense?.let { return "DEF ${it}" }
    equipment.hpBonus?.let { return "HP ${formatSigned(it)}" }
    equipment.statMods?.entries?.firstOrNull()?.let { (stat, value) ->
        return "${stat.uppercase(Locale.getDefault())} ${formatSigned(value)}"
    }
    return null
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
        normalizedType in setOf("weapon", "armor", "accessory", "gear", "snack") || item.equipment != null -> {
            val slot = item.equipment?.slot?.lowercase(Locale.getDefault())
            when {
                slot == "snack" -> R.drawable.item_icon_food
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

private fun equippedForCharacter(
    equippedItems: Map<String, String>,
    characterId: String?
): Map<String, String> {
    val normalizedId = characterId?.lowercase(Locale.getDefault())
    val scoped = equippedItems.mapNotNull { (key, value) ->
        val owner = key.substringBefore(':', missingDelimiterValue = "")
        val slot = key.substringAfter(':', missingDelimiterValue = "")
        if (owner.isNotBlank() && slot.isNotBlank() && normalizedId != null && owner.equals(normalizedId, ignoreCase = true)) {
            slot to value
        } else null
    }.toMap()
    if (scoped.isNotEmpty()) return scoped
    return equippedItems.filterKeys { !it.contains(':') }
}
