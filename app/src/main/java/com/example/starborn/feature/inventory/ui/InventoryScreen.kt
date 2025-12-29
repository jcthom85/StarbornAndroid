package com.example.starborn.feature.inventory.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.feature.inventory.InventoryViewModel
import com.example.starborn.feature.inventory.PartyMemberStatus
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.components.ItemTargetSelectionDialog
import com.example.starborn.ui.components.TargetSelectionOption
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
private val EQUIP_SLOTS = GearRules.equipSlots

enum class InventoryTab { SUPPLIES, GEAR, KEY_ITEMS }

data class InventoryLaunchOptions(
    val initialTab: InventoryTab? = null,
    val focusSlot: String? = null,
    val initialCharacterId: String? = null
)

private data class WeaponOption(
    val id: String,
    val item: Item
)

private data class ArmorOption(
    val id: String,
    val item: Item
)

private fun Item.categoryKey(): String {
    categoryOverride?.let { return it.lowercase(Locale.getDefault()) }
    if (equipment != null) return CATEGORY_EQUIPMENT
    val normalized = type.lowercase(Locale.getDefault())
    if (GearRules.isWeaponType(normalized)) return CATEGORY_EQUIPMENT
    val idLower = id.lowercase(Locale.getDefault())
    val nameLower = name.lowercase(Locale.getDefault())
    val looksBrokenComponent = idLower.startsWith("broken_") || nameLower.contains("broken ")
    return when (normalized) {
        "consumable", "medicine", "food", "drink", "tonic" -> CATEGORY_CONSUMABLES
        "weapon", "armor", "shield", "accessory", "gear", "snack" -> CATEGORY_EQUIPMENT
        "material", "ingredient", "component", "resource" -> CATEGORY_CRAFTING
        // Treat broken quest components as crafting parts so they appear under Supplies -> Crafting.
        "misc" -> if (looksBrokenComponent) CATEGORY_CRAFTING else CATEGORY_OTHER
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
    focusSlot: String? = null,
    initialCharacterId: String? = null
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val equippedItems by viewModel.equippedItems.collectAsStateWithLifecycle()
    val unlockedWeapons by viewModel.unlockedWeapons.collectAsStateWithLifecycle()
    val equippedWeapons by viewModel.equippedWeapons.collectAsStateWithLifecycle()
    val unlockedArmors by viewModel.unlockedArmors.collectAsStateWithLifecycle()
    val equippedArmors by viewModel.equippedArmors.collectAsStateWithLifecycle()
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
        unlockedWeapons = unlockedWeapons,
        equippedWeapons = equippedWeapons,
        unlockedArmors = unlockedArmors,
        equippedArmors = equippedArmors,
        partyMembers = partyMembers,
        snackbarHostState = snackbarHostState,
        onUseItem = viewModel::useItem,
        onEquipItem = viewModel::equipItem,
        onEquipMod = viewModel::equipMod,
        resolveWeaponItem = viewModel::weaponItem,
        onEquipWeapon = viewModel::equipWeapon,
        resolveArmorItem = viewModel::armorItem,
        onEquipArmor = viewModel::equipArmor,
        onRefreshInventory = viewModel::syncFromSession,
        onBack = onBack,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets,
        theme = theme,
        credits = credits,
        initialTab = initialTab,
        focusSlot = focusSlot,
        initialCharacterId = initialCharacterId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryScreen(
    entries: List<InventoryEntry>,
    equippedItems: Map<String, String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    partyMembers: List<PartyMemberStatus>,
    snackbarHostState: SnackbarHostState,
    onUseItem: (String, String?) -> Unit,
    onEquipItem: (String, String?, String?) -> Unit,
    onEquipMod: (String, String?, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipWeapon: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    onRefreshInventory: () -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme?,
    credits: Int,
    initialTab: InventoryTab?,
    focusSlot: String?,
    initialCharacterId: String?
) {
    val normalizedFocusSlot = remember(focusSlot) { focusSlot?.lowercase(Locale.getDefault()) }
    val normalizedInitialCharacterId = remember(initialCharacterId) { initialCharacterId?.lowercase(Locale.getDefault()) }
    val resolvedInitialTab = remember(initialTab, normalizedFocusSlot) {
        initialTab ?: if (normalizedFocusSlot != null) InventoryTab.GEAR else InventoryTab.SUPPLIES
    }
    var activeTab by remember(resolvedInitialTab) { mutableStateOf(resolvedInitialTab) }
    val scope = rememberCoroutineScope()
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
    var selectedCharacterId by remember(partyMembers, normalizedInitialCharacterId) {
        mutableStateOf(
            partyMembers.firstOrNull { member ->
                normalizedInitialCharacterId != null && member.id.equals(normalizedInitialCharacterId, ignoreCase = true)
            }?.id ?: partyMembers.firstOrNull()?.id
        )
    }
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
    var pendingModSlot by remember { mutableStateOf<String?>(null) }
    var showModDialog by remember { mutableStateOf(false) }
    var showWeaponDialog by remember { mutableStateOf(false) }
    var showArmorDialog by remember { mutableStateOf(false) }
    var selectedWeaponOptionId by remember { mutableStateOf<String?>(null) }
    var selectedArmorOptionId by remember { mutableStateOf<String?>(null) }
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
    val modEntries = remember(entries) {
        entries.filter { entry -> isModItem(entry.item) }
    }
    val inventoryEquippableEntries = remember(entries) {
        entries.mapNotNull { entry ->
            val item = entry.item
            val normalizedType = item.type.lowercase(Locale.getDefault())
            val slotCandidate = item.equipment?.slot?.lowercase(Locale.getDefault()) ?: when {
                EQUIP_SLOTS.contains(normalizedType) -> normalizedType
                GearRules.isWeaponType(normalizedType) -> "weapon"
                else -> null
            }
            if (slotCandidate == null) return@mapNotNull null
            if (item.equipment != null) return@mapNotNull entry
            val weaponType = normalizedType.takeIf { GearRules.isWeaponType(it) }
            entry.copy(item = item.copy(equipment = Equipment(slot = slotCandidate, weaponType = weaponType)))
        }
    }
    val characterEquippedItems = remember(equippedItems, selectedCharacterId) {
        equippedForCharacter(equippedItems, selectedCharacterId)
    }
    val equippedWeaponId = remember(equippedWeapons, selectedCharacterId) {
        selectedCharacterId?.let { id ->
            equippedWeapons[id.lowercase(Locale.getDefault())]
        }
    }
    val equippedWeaponItem = remember(equippedWeaponId) { equippedWeaponId?.let(resolveWeaponItem) }
    val equippedWeaponName = remember(equippedWeaponItem, equippedWeaponId) {
        when {
            equippedWeaponItem != null -> equippedWeaponItem.name
            !equippedWeaponId.isNullOrBlank() -> equippedWeaponId.humanizeId()
            else -> "No weapon equipped"
        }
    }
    val weaponOptionIds = remember(unlockedWeapons, equippedWeaponId) {
        if (equippedWeaponId.isNullOrBlank()) unlockedWeapons
        else unlockedWeapons + equippedWeaponId
    }
    val weaponOptions = remember(weaponOptionIds, selectedCharacterId, resolveWeaponItem) {
        buildWeaponOptions(
            unlockedWeapons = weaponOptionIds,
            characterId = selectedCharacterId,
            resolveWeaponItem = resolveWeaponItem
        )
    }
    val equippedArmorId = remember(equippedArmors, selectedCharacterId) {
        selectedCharacterId?.let { id ->
            equippedArmors[id.lowercase(Locale.getDefault())]
        }
    }
    val equippedArmorItem = remember(equippedArmorId) { equippedArmorId?.let(resolveArmorItem) }
    val equippedArmorName = remember(equippedArmorItem, equippedArmorId) {
        when {
            equippedArmorItem != null -> equippedArmorItem.name
            !equippedArmorId.isNullOrBlank() -> equippedArmorId.humanizeId()
            else -> "No armor equipped"
        }
    }
    val armorOptionIds = remember(unlockedArmors, equippedArmorId) {
        if (equippedArmorId.isNullOrBlank()) unlockedArmors
        else unlockedArmors + equippedArmorId
    }
    val armorOptions = remember(armorOptionIds, selectedCharacterId, resolveArmorItem) {
        buildArmorOptions(
            unlockedArmors = armorOptionIds,
            characterId = selectedCharacterId,
            resolveArmorItem = resolveArmorItem
        )
    }
    val fallbackEquippedEntries = remember(inventoryEquippableEntries, characterEquippedItems, selectedCharacterId) {
        val existingIds = inventoryEquippableEntries.map { it.item.id }.toSet()
        characterEquippedItems.mapNotNull { (slotId, itemId) ->
            if (itemId.isNullOrBlank() || existingIds.contains(itemId)) return@mapNotNull null
            val normalizedSlot = slotId.lowercase(Locale.getDefault())
            if (!EQUIP_SLOTS.contains(normalizedSlot)) return@mapNotNull null
            val readableName = itemId.split('_', ' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
                }.ifBlank { itemId }
            InventoryEntry(
                item = Item(
                    id = itemId,
                    name = readableName,
                    type = "equipment",
                    equipment = Equipment(
                        slot = normalizedSlot,
                        weaponType = if (normalizedSlot == "weapon") GearRules.allowedWeaponTypeFor(selectedCharacterId) else null
                    )
                ),
                quantity = 1
            )
        }
    }
    val equippableEntries = remember(inventoryEquippableEntries, fallbackEquippedEntries) {
        (inventoryEquippableEntries + fallbackEquippedEntries).distinctBy { it.item.id }
    }
    val slotOptions = remember { (EQUIP_SLOTS + "weapon").distinct() }
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
    val equippedItemNamesWithWeapon = remember(equippedItemNames, equippedWeaponName, equippedArmorName) {
        var updated = equippedItemNames
        if (!equippedWeaponName.isNullOrBlank()) {
            updated = updated + ("weapon" to equippedWeaponName)
        }
        if (!equippedArmorName.isNullOrBlank()) {
            updated = updated + ("armor" to equippedArmorName)
        }
        updated
    }
    val loadoutEquippedItems = remember(characterEquippedItems, equippedWeaponId, equippedArmorId) {
        var updated = characterEquippedItems
        if (!equippedWeaponId.isNullOrBlank()) {
            updated = updated + ("weapon" to equippedWeaponId)
        }
        if (!equippedArmorId.isNullOrBlank()) {
            updated = updated + ("armor" to equippedArmorId)
        }
        updated
    }
    var selectedEquipEntry by remember(selectedSlot, equippableEntries) {
        mutableStateOf(
            equippableEntries.firstOrNull { it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true) }
                ?: equippableEntries.firstOrNull()
        )
    }
    LaunchedEffect(selectedSlot, selectedCharacterId, equippableEntries, activeTab, characterEquippedItems) {
        if (activeTab == InventoryTab.GEAR) {
            val normalizedSlot = selectedSlot.lowercase(Locale.getDefault())
            if (normalizedSlot == "weapon" || normalizedSlot == "armor") {
                selectedEquipEntry = null
                return@LaunchedEffect
            }
            val equippedId = characterEquippedItems[normalizedSlot]
            val filteredOptions = filterGearOptions(
                entries = equippableEntries,
                slotId = selectedSlot,
                characterId = selectedCharacterId
            )
            selectedEquipEntry = filteredOptions.firstOrNull { it.item.id == equippedId }
                ?: filteredOptions.firstOrNull()
                ?: equippableEntries.firstOrNull { it.item.equipment?.slot.equals(selectedSlot, ignoreCase = true) }
                ?: equippableEntries.firstOrNull()
        }
    }
    LaunchedEffect(showWeaponDialog, weaponOptions, equippedWeaponId) {
        if (showWeaponDialog) {
            val initialSelection = weaponOptions.firstOrNull { option ->
                option.id.equals(equippedWeaponId, ignoreCase = true)
            } ?: weaponOptions.firstOrNull()
            selectedWeaponOptionId = initialSelection?.id
        }
    }
    LaunchedEffect(showArmorDialog, armorOptions, equippedArmorId) {
        if (showArmorDialog) {
            val initialSelection = armorOptions.firstOrNull { option ->
                option.id.equals(equippedArmorId, ignoreCase = true)
            } ?: armorOptions.firstOrNull()
            selectedArmorOptionId = initialSelection?.id
        }
    }
    LaunchedEffect(selectedCharacterId) {
        if (selectedCharacterId == null) {
            showWeaponDialog = false
            showArmorDialog = false
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
                            InventoryTab.GEAR -> {
                                val normalizedSlot = selectedSlot.lowercase(Locale.getDefault())
                                if (normalizedSlot == "weapon") {
                                    WeaponGearTabContent(
                                        partyMembers = partyMembers,
                                        selectedCharacterId = selectedCharacterId,
                                        slots = slotOptions,
                                        selectedSlot = selectedSlot,
                                        equippedItems = loadoutEquippedItems,
                                        equippedItemNames = equippedItemNamesWithWeapon,
                                        equippedWeaponItem = equippedWeaponItem,
                                        equippedWeaponName = equippedWeaponName,
                                        equippedWeaponSummary = equippedWeaponItem?.let { weaponSummaryLine(it) },
                                        hasUnlockedWeapons = weaponOptions.isNotEmpty() && selectedCharacterId != null,
                                        accentColor = accentColor,
                                        borderColor = borderColor,
                                        foregroundColor = foregroundColor,
                                        largeTouchTargets = largeTouchTargets,
                                        onSelectCharacter = { id -> selectedCharacterId = id },
                                        onSelectSlot = { slot ->
                                            val normalized = slot.lowercase(Locale.getDefault())
                                            selectedSlot = normalized
                                        },
                                        onSelectModSlot = { slot ->
                                            if (selectedCharacterId != null) {
                                                pendingModSlot = slot
                                                showModDialog = true
                                            }
                                        },
                                        onOpenWeaponPicker = {
                                            if (selectedCharacterId != null) {
                                                showWeaponDialog = true
                                            }
                                        }
                                    )
                                } else if (normalizedSlot == "armor") {
                                    ArmorGearTabContent(
                                        partyMembers = partyMembers,
                                        selectedCharacterId = selectedCharacterId,
                                        slots = slotOptions,
                                        selectedSlot = selectedSlot,
                                        equippedItems = loadoutEquippedItems,
                                        equippedItemNames = equippedItemNamesWithWeapon,
                                        equippedArmorItem = equippedArmorItem,
                                        equippedArmorName = equippedArmorName,
                                        equippedArmorSummary = equippedArmorItem?.let { armorSummaryLine(it) },
                                        hasUnlockedArmors = armorOptions.isNotEmpty() && selectedCharacterId != null,
                                        accentColor = accentColor,
                                        borderColor = borderColor,
                                        foregroundColor = foregroundColor,
                                        largeTouchTargets = largeTouchTargets,
                                        onSelectCharacter = { id -> selectedCharacterId = id },
                                        onSelectSlot = { slot ->
                                            val normalized = slot.lowercase(Locale.getDefault())
                                            selectedSlot = normalized
                                        },
                                        onSelectModSlot = { slot ->
                                            if (selectedCharacterId != null) {
                                                pendingModSlot = slot
                                                showModDialog = true
                                            }
                                        },
                                        onOpenArmorPicker = {
                                            if (selectedCharacterId != null) {
                                                showArmorDialog = true
                                            }
                                        }
                                    )
                                } else {
                                    val equippedId = characterEquippedItems[normalizedSlot]
                                    val equippedEntry = equippableEntries.firstOrNull { it.item.id == equippedId }
                                    val gearOptions = remember(equippableEntries, selectedSlot, selectedCharacterId) {
                                        filterGearOptions(
                                            entries = equippableEntries,
                                            slotId = selectedSlot,
                                            characterId = selectedCharacterId
                                        )
                                    }
                                    GearTabContent(
                                        partyMembers = partyMembers,
                                        selectedCharacterId = selectedCharacterId,
                                        slots = slotOptions,
                                        selectedSlot = selectedSlot,
                                        equippedItems = loadoutEquippedItems,
                                        availableItems = gearOptions,
                                        selectedEntry = selectedEquipEntry,
                                        equippedEntry = equippedEntry,
                                        equippedItemName = equippedItemNamesWithWeapon[normalizedSlot],
                                        equippedItemNames = equippedItemNamesWithWeapon,
                                        accentColor = accentColor,
                                        borderColor = borderColor,
                                        foregroundColor = foregroundColor,
                                        highContrastMode = highContrastMode,
                                        largeTouchTargets = largeTouchTargets,
                                        onSelectCharacter = { id ->
                                            selectedCharacterId = id
                                        },
                                        onSelectSlot = { slot ->
                                            val normalized = slot.lowercase(Locale.getDefault())
                                            selectedSlot = normalized
                                        },
                                        onSelectEntry = { entry -> selectedEquipEntry = entry },
                                        onEquip = if (selectedCharacterId != null) {
                                            { selectedCharacterId?.let { onEquipItem(selectedSlot, selectedEquipEntry?.item?.id, it) } }
                                        } else null,
                                        onUnequip = if (selectedCharacterId != null) {
                                            { onEquipItem(selectedSlot, null, selectedCharacterId!!) }
                                        } else null,
                                        onSelectModSlot = { slot ->
                                            if (selectedCharacterId != null) {
                                                pendingModSlot = slot
                                                showModDialog = true
                                            }
                                        }
                                    )
                                }
                            }
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
    if (showModDialog && pendingModSlot != null && selectedCharacterId != null) {
        val slotLabel = slotDisplayName(pendingModSlot!!)
        val currentModId = characterEquippedItems[pendingModSlot!!]
        ModPickerDialog(
            slotLabel = slotLabel,
            mods = modEntries,
            selectedModId = currentModId,
            onSelect = { entry ->
                onEquipMod(pendingModSlot!!, entry.item.id, selectedCharacterId)
                pendingModSlot = null
                showModDialog = false
            },
            onRemove = {
                onEquipMod(pendingModSlot!!, null, selectedCharacterId)
                pendingModSlot = null
                showModDialog = false
            },
            onDismiss = {
                pendingModSlot = null
                showModDialog = false
            },
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets
        )
    }
    val weaponDialogCharacterId = selectedCharacterId
    if (showWeaponDialog && weaponDialogCharacterId != null) {
        val characterName = partyMembers.firstOrNull { it.id == weaponDialogCharacterId }?.name
            ?: weaponDialogCharacterId.humanizeId()
        WeaponPickerDialog(
            characterName = characterName,
            weapons = weaponOptions,
            equippedWeaponId = equippedWeaponId,
            selectedWeaponId = selectedWeaponOptionId,
            onSelectWeapon = { selectedWeaponOptionId = it },
            onEquipWeapon = { weaponId ->
                onEquipWeapon(weaponDialogCharacterId, weaponId)
            },
            onDismiss = { showWeaponDialog = false },
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets
        )
    }
    val armorDialogCharacterId = selectedCharacterId
    if (showArmorDialog && armorDialogCharacterId != null) {
        val characterName = partyMembers.firstOrNull { it.id == armorDialogCharacterId }?.name
            ?: armorDialogCharacterId.humanizeId()
        ArmorPickerDialog(
            characterName = characterName,
            armors = armorOptions,
            equippedArmorId = equippedArmorId,
            selectedArmorId = selectedArmorOptionId,
            onSelectArmor = { selectedArmorOptionId = it },
            onEquipArmor = { armorId ->
                onEquipArmor(armorDialogCharacterId, armorId)
            },
            onDismiss = { showArmorDialog = false },
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets
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
private fun GearTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    slots: List<String>,
    selectedSlot: String,
    equippedItems: Map<String, String>,
    availableItems: List<InventoryEntry>,
    selectedEntry: InventoryEntry?,
    equippedEntry: InventoryEntry?,
    equippedItemName: String?,
    equippedItemNames: Map<String, String?>,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    onSelectCharacter: (String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectEntry: (InventoryEntry) -> Unit,
    onEquip: (() -> Unit)?,
    onUnequip: (() -> Unit)?,
    onSelectModSlot: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
        GearWorkshop(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            slots = slots,
            selectedSlot = selectedSlot,
            onSelectSlot = onSelectSlot,
            equippedItemName = equippedItemName,
            equippedItemNames = equippedItemNames,
            equippedItems = equippedItems,
            availableItems = availableItems,
            selectedEntry = selectedEntry,
            equippedEntry = equippedEntry,
            onSelectEntry = onSelectEntry,
            onEquip = onEquip,
            onUnequip = onUnequip,
            onSelectModSlot = onSelectModSlot,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
    }
}

@Composable
private fun WeaponGearTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    slots: List<String>,
    selectedSlot: String,
    equippedItems: Map<String, String>,
    equippedItemNames: Map<String, String?>,
    equippedWeaponItem: Item?,
    equippedWeaponName: String?,
    equippedWeaponSummary: String?,
    hasUnlockedWeapons: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onSelectCharacter: (String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectModSlot: (String) -> Unit,
    onOpenWeaponPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GearLoadoutGrid(
                slots = slots,
                selectedSlot = selectedSlot,
                equippedItemNames = equippedItemNames,
                equippedItems = equippedItems,
                onSelectSlot = onSelectSlot,
                onSelectModSlot = onSelectModSlot,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
            WeaponSlotPanel(
                equippedWeaponItem = equippedWeaponItem,
                equippedWeaponName = equippedWeaponName,
                equippedWeaponSummary = equippedWeaponSummary,
                hasUnlockedWeapons = hasUnlockedWeapons,
                onOpenWeaponPicker = onOpenWeaponPicker,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
        }
    }
}

@Composable
private fun ArmorGearTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    slots: List<String>,
    selectedSlot: String,
    equippedItems: Map<String, String>,
    equippedItemNames: Map<String, String?>,
    equippedArmorItem: Item?,
    equippedArmorName: String?,
    equippedArmorSummary: String?,
    hasUnlockedArmors: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onSelectCharacter: (String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectModSlot: (String) -> Unit,
    onOpenArmorPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GearLoadoutGrid(
                slots = slots,
                selectedSlot = selectedSlot,
                equippedItemNames = equippedItemNames,
                equippedItems = equippedItems,
                onSelectSlot = onSelectSlot,
                onSelectModSlot = onSelectModSlot,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
            ArmorSlotPanel(
                equippedArmorItem = equippedArmorItem,
                equippedArmorName = equippedArmorName,
                equippedArmorSummary = equippedArmorSummary,
                hasUnlockedArmors = hasUnlockedArmors,
                onOpenArmorPicker = onOpenArmorPicker,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
        }
    }
}

@Composable
private fun WeaponSlotPanel(
    equippedWeaponItem: Item?,
    equippedWeaponName: String?,
    equippedWeaponSummary: String?,
    hasUnlockedWeapons: Boolean,
    onOpenWeaponPicker: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val weaponLabel = equippedWeaponName?.takeIf { it.isNotBlank() } ?: "No weapon equipped"
    val description = equippedWeaponItem?.description?.takeIf { it.isNotBlank() }
        ?: if (equippedWeaponItem == null) "No weapon equipped yet." else "No description available."
    val iconRes = remember(equippedWeaponItem?.id) {
        equippedWeaponItem?.let { itemIconRes(it) } ?: slotIconRes("weapon")
    }
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Weapon Loadout", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 88.dp else 76.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weaponLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    equippedWeaponSummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }
            }
            if (!hasUnlockedWeapons) {
                Text(
                    text = "No weapons unlocked yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenWeaponPicker,
                    enabled = hasUnlockedWeapons,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Choose Weapon")
                }
            }
        }
    }
}

@Composable
private fun ArmorSlotPanel(
    equippedArmorItem: Item?,
    equippedArmorName: String?,
    equippedArmorSummary: String?,
    hasUnlockedArmors: Boolean,
    onOpenArmorPicker: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val armorLabel = equippedArmorName?.takeIf { it.isNotBlank() } ?: "No armor equipped"
    val description = equippedArmorItem?.description?.takeIf { it.isNotBlank() }
        ?: if (equippedArmorItem == null) "No armor equipped yet." else "No description available."
    val iconRes = remember(equippedArmorItem?.id) {
        equippedArmorItem?.let { itemIconRes(it) } ?: slotIconRes("armor")
    }
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Armor Loadout", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 88.dp else 76.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = armorLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    equippedArmorSummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }
            }
            if (!hasUnlockedArmors) {
                Text(
                    text = "No armor unlocked yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenArmorPicker,
                    enabled = hasUnlockedArmors,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Choose Armor")
                }
            }
        }
    }
}

@Composable
private fun PartyMemberSidebar(
    modifier: Modifier,
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    onSelectCharacter: (String) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (partyMembers.isEmpty()) {
             Box(contentAlignment = Alignment.Center) {
                 Text("No Party", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
             }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(partyMembers, key = { it.id }) { member ->
                    PartyMemberItem(
                        member = member,
                        isSelected = member.id == selectedCharacterId,
                        onClick = { onSelectCharacter(member.id) },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PartyMemberItem(
    member: PartyMemberStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val background = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    val border = if (isSelected) accentColor else Color.Transparent
    val portraitPainter = rememberAssetPainter(member.portraitPath, R.drawable.main_menu_background)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = portraitPainter,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GearWorkshop(
    modifier: Modifier,
    slots: List<String>,
    selectedSlot: String,
    onSelectSlot: (String) -> Unit,
    equippedItemName: String?,
    equippedItemNames: Map<String, String?>,
    equippedItems: Map<String, String>,
    availableItems: List<InventoryEntry>,
    selectedEntry: InventoryEntry?,
    equippedEntry: InventoryEntry?,
    onSelectEntry: (InventoryEntry) -> Unit,
    onEquip: (() -> Unit)?,
    onUnequip: (() -> Unit)?,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GearLoadoutGrid(
            slots = slots,
            selectedSlot = selectedSlot,
            equippedItemNames = equippedItemNames,
            equippedItems = equippedItems,
            onSelectSlot = onSelectSlot,
            onSelectModSlot = onSelectModSlot,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets
        )

        ComparisonPanel(
            slotLabel = slotDisplayName(selectedSlot),
            selectedEntry = selectedEntry,
            equippedEntry = equippedEntry,
            equippedItemName = equippedItemName,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets,
            onEquip = onEquip,
            onUnequip = onUnequip
        )

        InventoryItemsColumn(
            modifier = Modifier.weight(1f),
            items = availableItems,
            selectedItem = selectedEntry,
            onSelectItem = onSelectEntry,
            accentColor = accentColor,
            borderColor = borderColor,
            equippedItemId = equippedEntry?.item?.id,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets,
            statSummary = { entry -> primaryStatSummary(entry.item) }
        )
    }
}

@Composable
private fun GearLoadoutGrid(
    slots: List<String>,
    selectedSlot: String,
    equippedItemNames: Map<String, String?>,
    equippedItems: Map<String, String>,
    onSelectSlot: (String) -> Unit,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val preferredOrder = listOf("weapon", "armor", "accessory", "snack")
    val orderedSlots = remember(slots) {
        val normalized = slots.map { it.lowercase(Locale.getDefault()) }
        val ordered = preferredOrder.filter { normalized.contains(it) }
        ordered + normalized.filterNot { preferredOrder.contains(it) }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        orderedSlots.forEach { slot ->
            LoadoutSlotCard(
                slot = slot,
                isSelected = slot.equals(selectedSlot, ignoreCase = true),
                equippedItemName = equippedItemNames[slot],
                equippedItems = equippedItems,
                equippedItemNames = equippedItemNames,
                onClick = { onSelectSlot(slot) },
                onSelectModSlot = onSelectModSlot,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadoutSlotCard(
    slot: String,
    isSelected: Boolean,
    equippedItemName: String?,
    equippedItems: Map<String, String>,
    equippedItemNames: Map<String, String?>,
    onClick: () -> Unit,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    modifier: Modifier
) {
    val background = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.28f)
    val border = if (isSelected) accentColor else borderColor.copy(alpha = 0.5f)
    val slotLabel = slotDisplayName(slot)
    val name = equippedItemName?.takeIf { it.isNotBlank() } ?: "Empty"
    val cardHeight = if (largeTouchTargets) 104.dp else 96.dp
    val normalizedSlot = slot.lowercase(Locale.getDefault())
    val supportsMods = normalizedSlot == "weapon" || normalizedSlot == "armor"
    val modSlots = if (normalizedSlot == "weapon") {
        listOf("weapon_mod1", "weapon_mod2")
    } else if (normalizedSlot == "armor") {
        listOf("armor_mod1", "armor_mod2")
    } else {
        emptyList()
    }
    val baseEquippedId = equippedItems[normalizedSlot]
    val modsLocked = supportsMods && baseEquippedId.isNullOrBlank()

    Surface(
        modifier = modifier
            .heightIn(min = cardHeight)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(slotIconRes(slot)),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slotLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = foregroundColor.copy(alpha = if (equippedItemName.isNullOrBlank()) 0.6f else 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
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
            if (supportsMods) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (modsLocked) 0.6f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mods",
                        style = MaterialTheme.typography.labelSmall,
                        color = foregroundColor.copy(alpha = 0.7f)
                    )
                    modSlots.forEach { modSlot ->
                        val modName = equippedItemNames[modSlot]?.takeIf { it.isNotBlank() } ?: "Empty"
                        ModSlotChip(
                            label = if (modSlot.endsWith("1")) "M1" else "M2",
                            name = modName,
                            enabled = !modsLocked,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            foregroundColor = foregroundColor,
                            onClick = { onSelectModSlot(modSlot) }
                        )
                    }
                }
                if (modsLocked) {
                    Text(
                        text = "Equip ${slotLabel.lowercase(Locale.getDefault())} to unlock mods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModSlotChip(
    label: String,
    name: String,
    enabled: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(12.dp))
            .let { base -> if (enabled) base.clickable { onClick() } else base },
        color = Color.Black.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = foregroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModSlotsPanel(
    baseSlot: String,
    baseEquippedId: String?,
    equippedItemNames: Map<String, String?>,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val normalizedBase = baseSlot.lowercase(Locale.getDefault())
    val modSlots = if (normalizedBase == "weapon") {
        listOf("weapon_mod1", "weapon_mod2")
    } else {
        listOf("armor_mod1", "armor_mod2")
    }
    val isLocked = baseEquippedId.isNullOrBlank()
    val panelAlpha = if (isLocked) 0.5f else 1f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .alpha(panelAlpha),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeading(label = "Mods", accentColor = accentColor)
            if (isLocked) {
                Text(
                    text = "Equip a ${slotDisplayName(baseSlot).lowercase(Locale.getDefault())} to install mods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.7f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                modSlots.forEach { slot ->
                    val modName = equippedItemNames[slot]?.takeIf { it.isNotBlank() } ?: "Empty Slot"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (largeTouchTargets) 68.dp else 60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .let { base ->
                                if (!isLocked) base.clickable { onSelectModSlot(slot) } else base
                            },
                        color = Color.Black.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = slotDisplayName(slot).replace("Weapon ", "").replace("Armor ", ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = foregroundColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = modName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = foregroundColor,
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

@Composable
private fun ModPickerDialog(
    slotLabel: String,
    mods: List<InventoryEntry>,
    selectedModId: String?,
    onSelect: (InventoryEntry) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val selectedName = mods.firstOrNull { it.item.id == selectedModId }?.item?.name
        ?: selectedModId?.humanizeId()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            color = Color(0xFF0B0F16),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Mod",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = foregroundColor
                        )
                        Text(
                            text = slotLabel.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                Text(
                    text = "Current: ${selectedName ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRemove,
                        enabled = !selectedModId.isNullOrBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.18f),
                            contentColor = accentColor,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Remove Mod")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp)
                    ) {
                        Text("Cancel")
                    }
                }
                if (mods.isEmpty()) {
                    Text(
                        text = "No mods in inventory yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = foregroundColor.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mods, key = { it.item.id }) { entry ->
                            InventoryItemRow(
                                entry = entry,
                                selected = entry.item.id == selectedModId,
                                isEquipped = entry.item.id == selectedModId,
                                accentColor = accentColor,
                                onClick = { onSelect(entry) },
                                largeTouchTargets = largeTouchTargets,
                                statSummary = primaryStatSummary(entry.item)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponPickerDialog(
    characterName: String,
    weapons: List<WeaponOption>,
    equippedWeaponId: String?,
    selectedWeaponId: String?,
    onSelectWeapon: (String) -> Unit,
    onEquipWeapon: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050B12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weapon Loadout",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = foregroundColor
                        )
                        Text(
                            text = characterName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                if (weapons.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No weapons unlocked yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = foregroundColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val selectedOption = weapons.firstOrNull { it.id.equals(selectedWeaponId, ignoreCase = true) }
                        ?: weapons.firstOrNull()
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val isWide = maxWidth > 720.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                WeaponOptionsColumn(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight(),
                                    weapons = weapons,
                                    selectedWeaponId = selectedOption?.id,
                                    equippedWeaponId = equippedWeaponId,
                                    onSelectWeapon = onSelectWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                WeaponDetailPanel(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight(),
                                    option = selectedOption,
                                    equippedWeaponId = equippedWeaponId,
                                    onEquipWeapon = onEquipWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                WeaponDetailPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    option = selectedOption,
                                    equippedWeaponId = equippedWeaponId,
                                    onEquipWeapon = onEquipWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                WeaponOptionsColumn(
                                    modifier = Modifier.weight(1f),
                                    weapons = weapons,
                                    selectedWeaponId = selectedOption?.id,
                                    equippedWeaponId = equippedWeaponId,
                                    onSelectWeapon = onSelectWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
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
private fun ArmorPickerDialog(
    characterName: String,
    armors: List<ArmorOption>,
    equippedArmorId: String?,
    selectedArmorId: String?,
    onSelectArmor: (String) -> Unit,
    onEquipArmor: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050B12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Armor Loadout",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = foregroundColor
                        )
                        Text(
                            text = characterName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                if (armors.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No armor unlocked yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = foregroundColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val selectedOption = armors.firstOrNull { it.id.equals(selectedArmorId, ignoreCase = true) }
                        ?: armors.firstOrNull()
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val isWide = maxWidth > 720.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ArmorOptionsColumn(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight(),
                                    armors = armors,
                                    selectedArmorId = selectedOption?.id,
                                    equippedArmorId = equippedArmorId,
                                    onSelectArmor = onSelectArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                ArmorDetailPanel(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight(),
                                    option = selectedOption,
                                    equippedArmorId = equippedArmorId,
                                    onEquipArmor = onEquipArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ArmorDetailPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    option = selectedOption,
                                    equippedArmorId = equippedArmorId,
                                    onEquipArmor = onEquipArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                ArmorOptionsColumn(
                                    modifier = Modifier.weight(1f),
                                    armors = armors,
                                    selectedArmorId = selectedOption?.id,
                                    equippedArmorId = equippedArmorId,
                                    onSelectArmor = onSelectArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
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
private fun WeaponOptionsColumn(
    modifier: Modifier,
    weapons: List<WeaponOption>,
    selectedWeaponId: String?,
    equippedWeaponId: String?,
    onSelectWeapon: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(weapons, key = { it.id }) { option ->
                WeaponOptionRow(
                    option = option,
                    selected = option.id.equals(selectedWeaponId, ignoreCase = true),
                    isEquipped = option.id.equals(equippedWeaponId, ignoreCase = true),
                    accentColor = accentColor,
                    borderColor = borderColor,
                    foregroundColor = foregroundColor,
                    largeTouchTargets = largeTouchTargets,
                    onClick = { onSelectWeapon(option.id) }
                )
            }
        }
    }
}

@Composable
private fun ArmorOptionsColumn(
    modifier: Modifier,
    armors: List<ArmorOption>,
    selectedArmorId: String?,
    equippedArmorId: String?,
    onSelectArmor: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(armors, key = { it.id }) { option ->
                ArmorOptionRow(
                    option = option,
                    selected = option.id.equals(selectedArmorId, ignoreCase = true),
                    isEquipped = option.id.equals(equippedArmorId, ignoreCase = true),
                    accentColor = accentColor,
                    borderColor = borderColor,
                    foregroundColor = foregroundColor,
                    largeTouchTargets = largeTouchTargets,
                    onClick = { onSelectArmor(option.id) }
                )
            }
        }
    }
}

@Composable
private fun WeaponOptionRow(
    option: WeaponOption,
    selected: Boolean,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val outline = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
    val summary = weaponSummaryLine(option.item)
    val iconRes = remember(option.id) { itemIconRes(option.item) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 80.dp else 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isEquipped) {
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "EQUIPPED",
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
private fun ArmorOptionRow(
    option: ArmorOption,
    selected: Boolean,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val outline = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
    val summary = armorSummaryLine(option.item)
    val iconRes = remember(option.id) { itemIconRes(option.item) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 80.dp else 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isEquipped) {
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "EQUIPPED",
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
private fun WeaponDetailPanel(
    modifier: Modifier,
    option: WeaponOption?,
    equippedWeaponId: String?,
    onEquipWeapon: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val weapon = option?.item
    val isEquipped = option?.id?.equals(equippedWeaponId, ignoreCase = true) == true
    val detailRows = weapon?.let { weaponDetailRows(it) }.orEmpty()
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp
    val iconRes = remember(option?.id) {
        weapon?.let { itemIconRes(it) } ?: slotIconRes("weapon")
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Weapon Details", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 96.dp else 84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weapon?.name ?: "Select a weapon",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = weapon?.description?.takeIf { it.isNotBlank() } ?: "Choose a weapon to see details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (detailRows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detailRows.forEach { (label, value) ->
                        StatRow(label, value, accentColor)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { option?.let { onEquipWeapon(it.id) } },
                    enabled = option != null && !isEquipped,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(if (isEquipped) "Equipped" else "Equip Weapon")
                }
            }
        }
    }
}

@Composable
private fun ArmorDetailPanel(
    modifier: Modifier,
    option: ArmorOption?,
    equippedArmorId: String?,
    onEquipArmor: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val armor = option?.item
    val isEquipped = option?.id?.equals(equippedArmorId, ignoreCase = true) == true
    val detailRows = armor?.let { armorDetailRows(it) }.orEmpty()
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp
    val iconRes = remember(option?.id) {
        armor?.let { itemIconRes(it) } ?: slotIconRes("armor")
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Armor Details", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 96.dp else 84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = armor?.name ?: "Select armor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = armor?.description?.takeIf { it.isNotBlank() } ?: "Choose armor to see details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (detailRows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detailRows.forEach { (label, value) ->
                        StatRow(label, value, accentColor)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { option?.let { onEquipArmor(it.id) } },
                    enabled = option != null && !isEquipped,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(if (isEquipped) "Equipped" else "Equip Armor")
                }
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
                equipment.accuracy?.let { StatRow("Accuracy", formatSignedDecimal(it), accentColor) }
                equipment.critRate?.let { StatRow("Crit Rate", formatSignedDecimal(it), accentColor) }
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
    labels.addAll(listOf("Damage", "Defense", "HP Bonus", "Accuracy", "Crit Rate", "Weapon Type"))
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
    equipment.accuracy?.let { stats += "Accuracy" to StatValue(formatSignedDecimal(it), it.roundToInt()) }
    equipment.critRate?.let { stats += "Crit Rate" to StatValue(formatSignedDecimal(it), it.roundToInt()) }
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
    equipment.accuracy?.let { return "ACC ${formatSignedDecimal(it)}" }
    equipment.critRate?.let { return "CRIT ${formatSignedDecimal(it)}" }
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
        normalizedType == "mod" || item.equipment?.slot?.equals("mod", true) == true ->
            R.drawable.item_icon_material
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
        normalizedType in setOf("weapon", "armor", "accessory", "gear", "snack") ||
            GearRules.isWeaponType(normalizedType) || item.equipment != null -> {
            val slot = item.equipment?.slot?.lowercase(Locale.getDefault())
                ?: normalizedType.takeIf { EQUIP_SLOTS.contains(it) }
                ?: if (GearRules.isWeaponType(normalizedType)) "weapon" else null
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

private fun formatSignedDecimal(value: Double): String {
    val formatted = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
    return if (value >= 0) "+$formatted" else formatted
}

private fun formatMultiplier(value: Double): String {
    val formatted = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
    return "x$formatted"
}

private fun formatPercent(value: Double): String {
    val percent = if (value <= 1.0) value * 100.0 else value
    val rounded = percent.roundToInt()
    return "$rounded%"
}

private fun slotIconRes(slot: String): Int {
    return when (slot.lowercase(Locale.getDefault())) {
        "weapon" -> R.drawable.item_icon_sword
        "armor" -> R.drawable.item_icon_armor
        "accessory" -> R.drawable.item_icon_accessory
        "snack" -> R.drawable.item_icon_food
        else -> R.drawable.item_icon_generic
    }
}

private fun isModItem(item: Item): Boolean {
    val normalizedType = item.type.lowercase(Locale.getDefault())
    return normalizedType == "mod" || item.equipment?.slot?.equals("mod", true) == true
}

private fun filterGearOptions(
    entries: List<InventoryEntry>,
    slotId: String?,
    characterId: String?
): List<InventoryEntry> {
    val normalizedSlot = slotId?.trim()?.lowercase(Locale.getDefault()) ?: return emptyList()
    return entries.filter { entry ->
        GearRules.matchesSlot(
            equipment = entry.item.equipment,
            slotId = normalizedSlot,
            characterId = characterId,
            itemTypeHint = entry.item.type
        )
    }
}

private fun buildWeaponOptions(
    unlockedWeapons: Set<String>,
    characterId: String?,
    resolveWeaponItem: (String) -> Item?
): List<WeaponOption> {
    val expectedType = GearRules.allowedWeaponTypeFor(characterId)
    val options = unlockedWeapons.mapNotNull { rawId ->
        val item = resolveWeaponItem(rawId) ?: return@mapNotNull null
        val weaponType = weaponTypeFor(item)
        if (expectedType != null && weaponType != null && weaponType != expectedType) return@mapNotNull null
        if (expectedType != null && weaponType == null && item.equipment?.slot?.equals("weapon", true) != true) {
            return@mapNotNull null
        }
        WeaponOption(id = item.id, item = item)
    }
    return options
        .distinctBy { it.id }
        .sortedBy { it.item.name.lowercase(Locale.getDefault()) }
}

private fun buildArmorOptions(
    unlockedArmors: Set<String>,
    characterId: String?,
    resolveArmorItem: (String) -> Item?
): List<ArmorOption> {
    val expectedType = GearRules.allowedArmorTypeFor(characterId)
    val options = unlockedArmors.mapNotNull { rawId ->
        val item = resolveArmorItem(rawId) ?: return@mapNotNull null
        if (!item.isArmorItem()) return@mapNotNull null
        val armorType = item.type.trim().lowercase(Locale.getDefault())
        if (expectedType != null) {
            if (!GearRules.isArmorType(armorType)) return@mapNotNull null
            if (armorType != expectedType) return@mapNotNull null
        }
        ArmorOption(id = item.id, item = item)
    }
    return options
        .distinctBy { it.id }
        .sortedBy { it.item.name.lowercase(Locale.getDefault()) }
}

private fun weaponTypeFor(item: Item): String? {
    val weaponType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
    if (!weaponType.isNullOrBlank()) return weaponType
    val normalizedType = item.type.trim().lowercase(Locale.getDefault())
    return normalizedType.takeIf { GearRules.isWeaponType(it) }
}

private fun Item.isArmorItem(): Boolean {
    val normalizedType = type.trim().lowercase(Locale.getDefault())
    return normalizedType == "armor" || equipment?.slot?.equals("armor", ignoreCase = true) == true
}

private fun weaponSummaryLine(item: Item): String? {
    val equipment = item.equipment ?: return null
    val parts = mutableListOf<String>()
    formatDamageLabel(equipment)?.let { parts += "DMG $it" }
    equipment.attackStyle?.let { parts += slotDisplayName(it) }
    equipment.attackElement?.let { parts += slotDisplayName(it) }
    return parts.joinToString(" | ").takeIf { it.isNotBlank() }
}

private fun armorSummaryLine(item: Item): String? {
    val equipment = item.equipment ?: return null
    val parts = mutableListOf<String>()
    equipment.defense?.let { parts += "DEF ${formatSigned(it)}" }
    equipment.hpBonus?.let { parts += "HP ${formatSigned(it)}" }
    equipment.accuracy?.let { parts += "ACC ${formatSignedDecimal(it)}" }
    equipment.critRate?.let { parts += "CRIT ${formatSignedDecimal(it)}" }
    return parts.joinToString(" | ").takeIf { it.isNotBlank() }
}

private fun weaponDetailRows(item: Item): List<Pair<String, String>> {
    val equipment = item.equipment ?: return emptyList()
    val rows = mutableListOf<Pair<String, String>>()
    formatDamageLabel(equipment)?.let { rows += "Damage" to it }
    equipment.attackStyle?.let { rows += "Style" to slotDisplayName(it) }
    equipment.attackPowerMultiplier?.let { rows += "Power" to formatMultiplier(it) }
    equipment.attackChargeTurns?.let { turns ->
        rows += "Charge" to "$turns turn${if (turns == 1) "" else "s"}"
    }
    equipment.attackSplashMultiplier?.let { rows += "Splash" to formatPercent(it) }
    equipment.attackElement?.let { rows += "Element" to slotDisplayName(it) }
    equipment.statusOnHit?.let { status ->
        val chanceLabel = equipment.statusChance?.let { formatPercent(it) }
        val label = chanceLabel?.let { "${slotDisplayName(status)} ($it)" } ?: slotDisplayName(status)
        rows += "Status" to label
    }
    equipment.accuracy?.let { rows += "Accuracy" to formatSignedDecimal(it) }
    equipment.critRate?.let { rows += "Crit Rate" to formatSignedDecimal(it) }
    return rows
}

private fun armorDetailRows(item: Item): List<Pair<String, String>> {
    val equipment = item.equipment ?: return emptyList()
    val rows = mutableListOf<Pair<String, String>>()
    equipment.defense?.let { rows += "Defense" to formatSigned(it) }
    equipment.hpBonus?.let { rows += "HP Bonus" to formatSigned(it) }
    equipment.accuracy?.let { rows += "Accuracy" to formatSignedDecimal(it) }
    equipment.critRate?.let { rows += "Crit Rate" to formatSignedDecimal(it) }
    equipment.statMods?.forEach { (stat, value) ->
        rows += slotDisplayName(stat) to formatSigned(value)
    }
    return rows
}

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
    return scoped
}

private fun slotDisplayName(raw: String): String =
    raw.split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
        .ifBlank { raw }

private fun String.humanizeId(): String =
    split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
