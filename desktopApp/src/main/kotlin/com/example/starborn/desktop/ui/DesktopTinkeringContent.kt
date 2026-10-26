package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.crafting.*
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@Composable
fun DesktopTinkeringContent(services: DesktopAppServices) {
    val scope = rememberCoroutineScope()
    val catalog = remember(services) { services.itemRepository.allItems().associateBy { it.id } }
    val runtime = remember(services) { CraftingController(services.craftingService, services.inventoryService, services.sessionStore, scope) }
    val state by runtime.uiState.collectAsState()
    val resultFocus = remember { BringIntoViewRequester() }
    var detailSize by remember { mutableStateOf(IntSize.Zero) }
    var page by rememberSaveable { mutableStateOf("Workbench") }
    var socket by rememberSaveable { mutableIntStateOf(-1) }
    var query by rememberSaveable { mutableStateOf("") }
    var scrapId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingScrap by remember { mutableStateOf<TinkeringItemChoice?>(null) }
    var recipeId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(runtime) { runtime.craftResults.collect {
        services.exploration.onTinkeringCrafted(it.itemId)
        services.audioDriver.executeAll(services.audioRouter.commandsForUi("tinkering_ratchet"))
    } }
    LaunchedEffect(runtime) { runtime.messages.collect { message ->
        if (message.startsWith("Scrapped ")) services.audioDriver.executeAll(services.audioRouter.commandsForUi("tinkering_ratchet"))
    } }
    pendingScrap?.let { item ->
        AlertDialog(onDismissRequest = { pendingScrap = null },
            title = { Text("Scrap ${item.name}?") },
            text = { Text("Consumes one item. You receive:\n${item.salvage.joinToString("\n")}") },
            confirmButton = { TextButton(onClick = { pendingScrap = null; runtime.scrap(item.id) }) { Text("Scrap one") } },
            dismissButton = { TextButton(onClick = { pendingScrap = null }) { Text("Cancel") } })
    }
    LaunchedEffect(state.tutorialStep) {
        state.tutorialStep?.let { step ->
            services.exploration.onTinkerTutorialStep(step)
            page = "Workbench"
            socket = when (step) {
                TinkeringTutorialStep.SLOT_BASE -> -1
                TinkeringTutorialStep.SLOT_COMPONENT -> 0
                else -> 0
            }
        }
    }
    LaunchedEffect(state.tutorialStep, state.bench.preview?.recipeId, detailSize) {
        if (state.tutorialStep == TinkeringTutorialStep.SYNTHESIZE && state.bench.preview != null) {
            withFrameNanos { }; withFrameNanos { }
            resultFocus.bringIntoView()
        }
    }
    DisposableEffect(runtime) { onDispose { runtime.close(); services.exploration.onTinkeringClosed() } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Workbench", "Schematics", "Scrap").forEach { tab -> FilterChip(page == tab, onClick = { page = tab; query = "" }, modifier = Modifier.desktopPointerHover(), label = { Text(tab) }) }
        }
        state.lastMessage?.let { Text(it, color = FieldMenuDesign.cyan) }
        state.tutorialStep?.let { step ->
            Surface(color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .6f))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tutorial", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelMedium)
                        Text(if (step == TinkeringTutorialStep.COMPLETE) "Complete" else "Step ${step.ordinal + 1} of 3", style = MaterialTheme.typography.labelSmall, color = FieldMenuDesign.textMuted)
                    }
                    if (step == TinkeringTutorialStep.SLOT_BASE) Text("Repair the Cryo-Inductor", style = MaterialTheme.typography.titleSmall)
                    Text(tinkeringTutorialInstruction(step), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        when (page) {
            "Workbench" -> DesktopStationPanes(Modifier.weight(1f), stackBelow = 500.dp, primary = { paneModifier ->
                Column(paneModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (socket == -1) "Base items" else "Compatible components", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Find a part") }, singleLine = true)
                    val options = if (socket == -1) state.baseItemIds else state.componentOptions[state.bench.mainItemId].orEmpty()
                    val materials = state.inventory.filter { it.quantity > 0 && it.id in options && it.name.contains(query, true) }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (materials.isEmpty()) item { Text(if (socket != -1 && state.bench.mainItemId == null) "Choose a base item first." else "No matching materials in your inventory. Check Schematics for requirements.", color = FieldMenuDesign.textMuted) }
                        items(materials, key = { it.id }) { item ->
                            OutlinedButton(onClick = { if (socket == -1) runtime.selectMain(item.id) else runtime.selectComponent(socket, item.id) }, modifier = Modifier.fillMaxWidth().desktopPointerHover()) {
                                DesktopMenuItemArt(services, catalog[item.id], Modifier.size(40.dp))
                                Text(item.name, Modifier.weight(1f).padding(start = 10.dp)); Text("x${item.quantity}")
                            }
                        }
                    }
                }
            }, detail = { paneModifier ->
                Column(paneModifier.fillMaxHeight().onSizeChanged { detailSize = it }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesktopMenuSection("Assembly")
                    Text("Base item + compatible components -> Result", style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
                    (-1..if (state.tutorialStep != null) 0 else 1).forEach { target ->
                        if (target >= 0) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HorizontalDivider(Modifier.weight(1f), color = FieldMenuDesign.cyan.copy(alpha = .16f))
                            Text("+", color = FieldMenuDesign.cyan)
                            HorizontalDivider(Modifier.weight(1f), color = FieldMenuDesign.cyan.copy(alpha = .16f))
                        }
                        val guidedSocket = state.tutorialStep == TinkeringTutorialStep.SLOT_BASE && target == -1 || state.tutorialStep == TinkeringTutorialStep.SLOT_COMPONENT && target == 0
                        val name = if (target == -1) state.bench.mainItemName else state.bench.componentNames.getOrNull(target)
                        Surface(onClick = { socket = target }, color = FieldMenuDesign.panel, shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.desktopPointerHover(),
                            border = BorderStroke(1.dp, if (guidedSocket) FieldMenuDesign.gold else if (socket == target && state.tutorialStep != TinkeringTutorialStep.SYNTHESIZE) FieldMenuDesign.cyan else FieldMenuDesign.cyan.copy(alpha = .18f))) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                val itemId = if (target == -1) state.bench.mainItemId else state.bench.componentIds.getOrNull(target)
                                DesktopMenuItemArt(services, catalog[itemId], Modifier.size(56.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (target == -1) "Base item" else "Component ${target + 1}", color = if (socket == target) FieldMenuDesign.gold else FieldMenuDesign.textMuted)
                                    Text(name?.takeIf { it.isNotBlank() } ?: "Choose a part")
                                }
                                if (!name.isNullOrBlank()) TextButton(onClick = { if (target == -1) runtime.selectMain(null) else runtime.selectComponent(target, null) }, modifier = Modifier.desktopPointerHover()) { Text("Remove") }
                            }
                        }
                    }
                    state.bench.preview?.let { preview ->
                        DesktopMenuCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.bringIntoViewRequester(resultFocus).testTag("tinker-tutorial-result"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Result", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelSmall)
                                    Text(preview.name, style = MaterialTheme.typography.titleLarge, color = FieldMenuDesign.gold)
                                    Text(preview.resultSummary)
                                }
                                DesktopModComparison(preview.equipmentComparison)
                            }
                        }
                        if (preview.requiresSchematic && !preview.learned) Text(preview.discoveryHint ?: "Learn this schematic from Inventory before building.")
                    }
                    if (!state.bench.canCraftSelection && state.bench.preview == null) {
                        Text(if (state.bench.mainItemId == null) "Choose a base item to start." else "Add compatible components to discover a result.", color = FieldMenuDesign.gold)
                    }
                    state.bench.requirements.forEach { Text("${it.label}: ${it.available} / ${it.required}") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = runtime::craftFromBench, enabled = state.bench.canCraftSelection, modifier = Modifier.testTag("tinker-build").desktopPointerHover(state.bench.canCraftSelection),
                            border = if (state.tutorialStep == TinkeringTutorialStep.SYNTHESIZE) BorderStroke(2.dp, FieldMenuDesign.cyan) else null) { Text("Synthesize") }
                        TextButton(onClick = runtime::clearBench, modifier = Modifier.desktopPointerHover()) { Text("Clear") }
                    }
                }
            })
            "Schematics" -> {
                var showAllClues by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Find a schematic") }, singleLine = true)
                val matchingLocked = state.lockedRecipes.filter { it.name.contains(query, true) }
                val relevantLocked = matchingLocked.filter { it.isRelevantDiscovery }
                val visibleLocked = if (query.isNotBlank() || showAllClues || relevantLocked.isEmpty()) matchingLocked else relevantLocked
                val hiddenClueCount = (matchingLocked.size - visibleLocked.size).coerceAtLeast(0)
                val recipes = state.learnedRecipes.filter { it.name.contains(query, true) } + visibleLocked
                val selected = recipes.firstOrNull { it.id == recipeId } ?: recipes.firstOrNull()
                DesktopStationPanes(Modifier.weight(1f), stackBelow = 500.dp, primary = { paneModifier ->
                    LazyColumn(paneModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.schematicChoices, key = { "found-${it.id}" }) { item -> OutlinedButton(onClick = { runtime.learnFoundSchematic(item.id) }) { Text("Learn ${item.name}") } }
                        if (recipes.isEmpty()) item { Text("No matching schematics.") }
                        items(recipes, key = { it.id }) { recipe ->
                            Surface(onClick = { recipeId = recipe.id }, color = if (recipe.id == selected?.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel, shape = RoundedCornerShape(4.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(recipe.name); Text(if (recipe.learned) "Learned" else "Undiscovered", color = FieldMenuDesign.textMuted) }
                            }
                        }
                        if (query.isBlank() && (hiddenClueCount > 0 || (showAllClues && relevantLocked.isNotEmpty() && relevantLocked.size < matchingLocked.size))) {
                            item(key = "toggle-clues") {
                                OutlinedButton(onClick = { showAllClues = !showAllClues }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (showAllClues) "Show only clues for owned base parts (${relevantLocked.size})"
                                        else "Show all ${matchingLocked.size} discovery clues (+$hiddenClueCount locked)")
                                }
                            }
                        }
                    }
                }, detail = { paneModifier ->
                    selected?.let { recipe -> Column(paneModifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesktopMenuSection(recipe.name)
                        if (recipe.learned) {
                            DesktopMenuItemArt(services, catalog[recipe.resultId], Modifier.size(96.dp))
                            recipe.description?.let { Text(it) }; Text(recipe.resultSummary)
                            DesktopModComparison(recipe.equipmentComparison)
                            recipe.ingredients.forEach { Text("${it.label}: ${it.available} / ${it.required}") }
                            Button(onClick = { runtime.craft(recipe.id) }, enabled = recipe.canCraft) { Text("Synthesize") }
                        } else Text(recipe.discoveryHint ?: "Find its schematic")
                    } }
                })
            }
            else -> {
                val selected = state.scrapChoices.firstOrNull { it.id == scrapId } ?: state.scrapChoices.firstOrNull()
                DesktopStationPanes(Modifier.weight(1f), stackBelow = 500.dp, primary = { paneModifier ->
                    LazyColumn(paneModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.scrapChoices.isEmpty()) item { Text("No equipment available to scrap.", color = FieldMenuDesign.textMuted) }
                        items(state.scrapChoices, key = { it.id }) { item ->
                            Surface(onClick = { scrapId = item.id }, modifier = Modifier.fillMaxWidth(), color = if (selected?.id == item.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (selected?.id == item.id) FieldMenuDesign.gold else FieldMenuDesign.border)) {
                                Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DesktopMenuItemArt(services, catalog[item.id], Modifier.size(48.dp))
                                    Text(item.name, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }, detail = { paneModifier ->
                    selected?.let { item -> DesktopMenuCard(paneModifier.fillMaxHeight()) {
                        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DesktopMenuSection(item.name)
                            Text(services.craftingService.copySummary(item.id), style = MaterialTheme.typography.labelSmall)
                            DesktopMenuScrollPane(Modifier.weight(1f).fillMaxWidth()) {
                                DesktopMenuItemArt(services, catalog[item.id], Modifier.size(64.dp))
                                catalog[item.id]?.description?.let { Text(it) }
                            }
                            HorizontalDivider()
                            Text("Salvage", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.titleMedium)
                            item.salvage.forEach { Text(it) }
                            Button(onClick = { pendingScrap = item }, modifier = Modifier.fillMaxWidth()) { Text("Scrap") }
                        }
                    } }
                })
            }
        }

    }
}
