package com.example.starborn.feature.hub.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.starborn.feature.exploration.ui.DialogueOverlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.ui.MenuOverlay
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.ui.components.*
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import kotlinx.coroutines.launch

/** Same field-menu pages and controllers, hosted without an exploration scene or room entry. */
@Composable
internal fun HubFieldMenu(
    viewModel: ExplorationViewModel,
    craftingViewModel: CraftingViewModel,
    initialTab: MenuTab?,
    questId: String?,
    onDismiss: () -> Unit,
    onReturnToTitle: () -> Unit,
    onLoad: suspend (Int) -> Boolean,
    onLoaded: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    var replaceMeal by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    var replacing by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf<String?>(null) }
    var slots by remember { mutableStateOf(emptyList<SaveSlotSummary>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var modMove by remember { mutableStateOf<com.example.starborn.domain.inventory.ModTransferRequest?>(null) }
    LaunchedEffect(Unit) {
        viewModel.setNoticePresentationBlocked("hub-menu", true)
        viewModel.openMenuOverlay(initialTab, showTutorial = false)
        questId?.let(viewModel::openQuestDetails)
    }
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.closeMenuOverlay()
            viewModel.setNoticePresentationBlocked("hub-menu", false)
        }
    }
    LaunchedEffect(craftingViewModel) { craftingViewModel.craftResults.collect { viewModel.onTinkeringCrafted(it.itemId) } }
    fun close() { viewModel.closeMenuOverlay(); onDismiss() }
    BackHandler { if (mode != null && !busy) mode = null else if (mode == null) close() }
    fun use(item: InventoryPreviewItemUi, replacement: Boolean) {
        replacing = replacement
        if (item.effect?.target?.lowercase() in listOf("party", "all_allies") || ui.partyStatus.members.isEmpty()) {
            viewModel.useInventoryItem(item.id, null, replacement)
        } else target = item
    }
    fun saveDialog(nextMode: String) { scope.launch { slots = viewModel.fetchSaveSlots(); error = null; mode = nextMode } }
    if (ui.isLoading) { Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }; return }
    MenuOverlay(
        selectedTab = ui.menuTab, onSelectTab = viewModel::selectMenuTab, onClose = ::close,
        onOpenRegionMap = ::close,
        onOpenInventory = { viewModel.selectMenuTab(MenuTab.INVENTORY) },
        onOpenJournal = { viewModel.selectMenuTab(MenuTab.JOURNAL) },
        onOpenMapLegend = viewModel::openMapLegend,
        onOpenFieldKit = { viewModel.selectMenuTab(MenuTab.FIELD_KIT) },
        settings = ui.settings,
        onMusicVolumeChange = viewModel::updateMusicVolume,
        onSfxVolumeChange = viewModel::updateSfxVolume,
        onVoiceVolumeChange = viewModel::updateVoiceVolume,
        onToggleTutorials = viewModel::updateTutorialsEnabled,
        onToggleVignette = viewModel::setVignetteEnabled,
        onToggleModernFieldMenu = viewModel::setModernFieldMenu,
        onToggleHighContrast = viewModel::setHighContrastMode,
        onToggleLargeTouchTargets = viewModel::setLargeTouchTargets,
        onToggleScreenshakeDisabled = viewModel::setScreenshakeDisabled,
        onToggleFlashesDisabled = viewModel::setFlashesDisabled,
        onToggleHapticsDisabled = viewModel::setHapticsDisabled,
        onQuickSave = { viewModel.quickSave() }, onSaveGame = { saveDialog("save") }, onLoadGame = { saveDialog("load") },
        onReturnToTitle = { viewModel.quickSaveAndReturnToTitle(onReturnToTitle) },
        partyStatus = ui.partyStatus, onShowSkillTree = viewModel::openSkillTree,
        onShowDetails = viewModel::openPartyMemberDetails, statusMessage = ui.menuFeedback ?: ui.statusMessage,
        trackedQuest = ui.questLogActive.firstOrNull { it.id == ui.trackedQuestId },
        activeQuests = ui.questLogActive, completedQuests = ui.questLogCompleted,
        minimap = null, fullMap = null, theme = ui.theme, isCurrentRoomDark = false,
        onMenuAction = viewModel::onMenuActionInvoked, creditsLabel = ui.progressionSummary.creditsLabel,
        inventoryItems = ui.inventoryPreview, equippedItems = ui.equippedItems, completedMilestones = ui.completedMilestones,
        unlockedWeapons = ui.unlockedWeapons, equippedWeapons = ui.equippedWeapons,
        unlockedArmors = ui.unlockedArmors, equippedArmors = ui.equippedArmors,
        onEquipItem = viewModel::equipInventoryItem,
        onEquipMod = { slot, item, character ->
            val result = viewModel.requestEquipInventoryMod(slot, item, character)
            if (result is com.example.starborn.domain.inventory.ModEquipResult.TransferRequired && item != null) {
                val owner = character ?: result.sources.firstOrNull()?.characterId
                if (owner != null) modMove = com.example.starborn.domain.inventory.ModTransferRequest(
                    com.example.starborn.domain.inventory.ModLocation(owner, slot), item, result.sources)
            }
        },
        onEquipWeapon = viewModel::equipWeapon, resolveWeaponItem = viewModel::weaponItem,
        onEquipArmor = viewModel::equipArmor, resolveArmorItem = viewModel::armorItem,
        onUseInventoryItem = { item -> if (viewModel.isPreparedMeal(item.id) && ui.activeMeal != null) replaceMeal = item else use(item, false) },
        onShowQuestDetails = viewModel::openQuestDetails, questDetail = ui.questDetail,
        onCloseQuestDetails = viewModel::closeQuestDetails, onToggleQuestTracking = viewModel::toggleQuestTracking,
        partyMemberDetails = ui.partyMemberDetails, onClosePartyMemberDetails = viewModel::closePartyMemberDetails,
        skillTreeOverlay = ui.skillTreeOverlay, onCloseSkillTree = viewModel::closeSkillTreeOverlay,
        onUnlockSkill = viewModel::unlockSkillNode, onResetSkillBuild = viewModel::resetPrototypeBuild,
        onCloseMapLegend = viewModel::closeMapLegend, craftingViewModel = craftingViewModel,
        onTinkerTutorialStep = viewModel::onTinkerTutorialStep,
        isTinkeringTutorialActive = ui.isTinkeringTutorialActive, isGearTutorialActive = ui.isGearTutorialActive
    )
    ui.activeDialogue?.let { dialogue ->
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)).clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}) {
            DialogueOverlay(dialogue, ui.dialogueChoices, viewModel::advanceDialogue, viewModel::onDialogueChoiceSelected,
                viewModel::onDialogueVoiceRequested, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
    modMove?.let { request ->
        com.example.starborn.feature.crafting.ui.ModTransferDialog(request,
            ui.inventoryPreview.firstOrNull { it.id == request.itemId }?.name ?: request.itemId,
            { modMove = null }) { source -> modMove = null; viewModel.requestEquipInventoryMod(request.target.slotId, request.itemId, request.target.characterId, source) }
    }
    replaceMeal?.let { item -> AlertDialog(onDismissRequest = { replaceMeal = null },
        title = { Text("Replace your meal?") }, text = { Text("Eating ${item.name} replaces your active meal.") },
        confirmButton = { TextButton(onClick = { replaceMeal = null; use(item, true) }) { Text("Replace meal") } },
        dismissButton = { TextButton(onClick = { replaceMeal = null }) { Text("Keep current meal") } }) }
    target?.let { item -> ItemTargetSelectionDialog(itemName = item.name,
        targets = ui.partyStatus.members.map { TargetSelectionOption(it.id, it.name, it.hpLabel) },
        onSelect = { id -> viewModel.useInventoryItem(item.id, id, replacing); target = null },
        onDismiss = { target = null }, backgroundColor = FieldMenuDesign.panel, borderColor = FieldMenuDesign.border,
        textColor = FieldMenuDesign.text, accentColor = FieldMenuDesign.cyan) }
    mode?.let { selectedMode -> SaveLoadDialog(mode = selectedMode, slots = slots,
        onSave = { slot -> scope.launch {
            busy = true
            try { if (viewModel.saveGame(slot)) mode = null else error = "Save failed. Try again." }
            finally { busy = false }
        } },
        onLoad = { slot -> scope.launch {
            busy = true
            try { if (onLoad(slot)) { mode = null; close(); onLoaded() } else error = "Load failed. Try again." }
            finally { busy = false }
        } },
        onDelete = { slot -> scope.launch { viewModel.deleteGame(slot); slots = viewModel.fetchSaveSlots() } },
        onDismiss = { if (!busy) mode = null }, accentColor = FieldMenuDesign.cyan, panelColor = FieldMenuDesign.panel,
        borderColor = FieldMenuDesign.border, textColor = FieldMenuDesign.text, modern = ui.settings.modernFieldMenu,
        largeTargets = ui.settings.largeTouchTargets, highContrast = ui.settings.highContrastMode,
        confirmLoad = true, busy = busy, errorMessage = error) }
}
