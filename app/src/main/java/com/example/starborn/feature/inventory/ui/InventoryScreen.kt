package com.example.starborn.feature.inventory.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.starborn.feature.inventory.InventoryVisualEvent
import com.example.starborn.feature.inventory.PartyMemberStatus
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.components.ItemTargetSelectionDialog
import com.example.starborn.ui.components.TargetSelectionOption
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.roundToInt
import com.example.starborn.feature.inventory.ui.components.*
import com.example.starborn.feature.inventory.ui.tabs.*

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
    val completedMilestones by viewModel.completedMilestones.collectAsStateWithLifecycle()
    val unlockedWeapons by viewModel.unlockedWeapons.collectAsStateWithLifecycle()
    val equippedWeapons by viewModel.equippedWeapons.collectAsStateWithLifecycle()
    val unlockedArmors by viewModel.unlockedArmors.collectAsStateWithLifecycle()
    val equippedArmors by viewModel.equippedArmors.collectAsStateWithLifecycle()
    val partyMembers by viewModel.partyMembers.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val healPulses = remember { mutableStateMapOf<String, InventoryHealPulse>() }

    LaunchedEffect(Unit) {
        viewModel.syncFromSession()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.visualEvents.collect { event ->
            when (event) {
                is InventoryVisualEvent.Heal -> {
                    val pulse = InventoryHealPulse(
                        id = System.nanoTime(),
                        amount = event.amount
                    )
                    healPulses[event.targetId] = pulse
                    launch {
                        delay(1000)
                        if (healPulses[event.targetId]?.id == pulse.id) {
                            healPulses.remove(event.targetId)
                        }
                    }
                }
            }
        }
    }

    InventoryScreen(
        entries = entries,
        equippedItems = equippedItems,
        completedMilestones = completedMilestones,
        unlockedWeapons = unlockedWeapons,
        equippedWeapons = equippedWeapons,
        unlockedArmors = unlockedArmors,
        equippedArmors = equippedArmors,
        partyMembers = partyMembers,
        healPulses = healPulses,
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
    completedMilestones: Set<String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    partyMembers: List<PartyMemberStatus>,
    healPulses: Map<String, InventoryHealPulse>,
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
                                partyMembers = partyMembers,
                                selectedCharacterId = selectedCharacterId,
                                healPulses = healPulses,
                                onSelectItem = { entry ->
                                    selectedSupplyEntry = entry
                                    if (entry.item.effect != null) {
                                        promptUse(entry)
                                    }
                                },
                                onSelectCharacter = { id -> selectedCharacterId = id },
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
                                        healPulses = healPulses,
                                        slots = slotOptions,
                                        selectedSlot = selectedSlot,
                                        equippedItems = loadoutEquippedItems,
                                        completedMilestones = completedMilestones,
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
                                        healPulses = healPulses,
                                        slots = slotOptions,
                                        selectedSlot = selectedSlot,
                                        equippedItems = loadoutEquippedItems,
                                        completedMilestones = completedMilestones,
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
                                        healPulses = healPulses,
                                        slots = slotOptions,
                                        selectedSlot = selectedSlot,
                                        equippedItems = loadoutEquippedItems,
                                        completedMilestones = completedMilestones,
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
