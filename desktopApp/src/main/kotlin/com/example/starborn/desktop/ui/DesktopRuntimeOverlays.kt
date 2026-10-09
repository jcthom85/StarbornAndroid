package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.viewmodel.ExplorationController
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

@Composable
internal fun DesktopRuntimeOverlays(services: DesktopAppServices, runtime: ExplorationController, ui: ExplorationUiState) {
    if (ui.showBurgQuestAstraExitDialog) AlertDialog(
        onDismissRequest = runtime::dismissBurgQuestAstraExitDialog,
        title = { Text("Explore the Astra") },
        text = { Text("Disembarking is unavailable during the BurgQuest demo. Explore the ship, simulation deck or workbench before finishing your visit.") },
        confirmButton = { TextButton(onClick = runtime::dismissBurgQuestAstraExitDialog) { Text("Continue exploring") } })
    if (ui.isMilestoneGalleryVisible) AlertDialog(
        onDismissRequest = runtime::closeMilestoneGallery,
        title = { Text("Milestone gallery") },
        text = { LazyColumn(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (ui.milestoneBands.isEmpty()) item { Text("Your journey's milestones will appear here.") }
            ui.milestoneBands.forEach { band -> item { Text(band.message) } }
        } },
        confirmButton = { TextButton(onClick = runtime::closeMilestoneGallery) { Text("Close") } })
    if (ui.isAstraNavConsoleVisible) AlertDialog(onDismissRequest = runtime::dismissAstraNavConsole,
        title = { Text("Astra navigation") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(ui.astraTransitTitle ?: ui.astraArrivalTitle ?: "Docked: ${ui.astraDockedTitle}")
                ui.astraDestinations.forEach { destination ->
                    OutlinedButton(onClick = { runtime.travelToWorldFromAstra(destination.worldId, destination.hubId, destination.roomId, destination.nodeId) }, enabled = ui.astraTransitTitle == null) { Text(destination.title) }
                }
                TextButton(onClick = runtime::disembarkAstra, enabled = ui.astraTransitTitle == null) { Text("Disembark") }
            }
        }, confirmButton = { TextButton(onClick = runtime::dismissAstraNavConsole) { Text("Close") } })
    if (ui.isSimulationDeckVisible) AlertDialog(onDismissRequest = runtime::dismissSimulationDeck,
        title = { Text("Simulation deck") }, text = {
            LazyColumn(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.starborn.feature.exploration.presentation.AstraCatalog.simulations.groupBy { it.category }.forEach { (category, programs) ->
                    item { Text(category, style = MaterialTheme.typography.labelMedium) }
                    programs.forEach { program -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { runtime.launchSimulationCombat(program.enemyIds) }) { Text(program.title) }
                            Text(program.description, style = MaterialTheme.typography.bodySmall)
                        }
                    } }
                }
            }
        }, confirmButton = { TextButton(onClick = runtime::dismissSimulationDeck) { Text("Close") } })
    if (ui.isTapeDeckVisible) AlertDialog(onDismissRequest = runtime::dismissTapeDeck,
        title = { Text("Great Frontier tapes") }, text = {
            LazyColumn(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.example.starborn.feature.exploration.presentation.AstraCatalog.films.forEach { (tape, cue, metadata) -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { runtime.playTapeTrack(tape, cue) }, enabled = ui.inventoryPreview.any { it.id == tape && it.quantity > 0 }) {
                            Text(metadata.first + if (ui.playingTapeId == tape) " \u00B7 playing" else "")
                        }
                        Text(metadata.second, style = MaterialTheme.typography.labelSmall)
                        services.itemRepository.findItem(tape)?.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                } }
                item { TextButton(onClick = runtime::stopTapeTrack) { Text("Stop") } }
            }
        }, confirmButton = { TextButton(onClick = runtime::dismissTapeDeck) { Text("Close") } })
    ui.levelUpPrompt?.let { level -> AlertDialog(onDismissRequest = runtime::dismissLevelUpPrompt,
        title = { Text("${level.characterName} · Level ${level.newLevel}") }, text = { Column {
            level.statChanges.forEach { Text("${it.label}: ${it.value}") }
            level.unlockedSkills.forEach { Text("Unlocked: ${it.name}") }
        } }, confirmButton = { TextButton(onClick = runtime::dismissLevelUpPrompt) { Text("Continue") } }) }
    ui.skillTreeOverlay?.takeUnless { ui.isMenuOverlayVisible }?.let { tree -> AlertDialog(onDismissRequest = runtime::closeSkillTreeOverlay,
        title = { Text("${tree.characterName} · ${tree.availableAp} AP available") }, text = {
            LazyColumn(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tree.branches.forEach { branch ->
                    item { Text(branch.title, style = MaterialTheme.typography.titleMedium) }
                    branch.nodes.forEach { node -> item { Column {
                        Text("${node.name} · ${node.costAp} AP${if (node.status.unlocked) " · unlocked" else ""}")
                        Text(node.description.orEmpty())
                        node.status.unmetRequirements.forEach { Text(it) }
                        Button(onClick = { runtime.unlockSkillNode(node.id) }, enabled = node.status.canPurchase) { Text("Unlock") }
                    } } }
                }
            }
        }, confirmButton = { TextButton(onClick = runtime::closeSkillTreeOverlay) { Text("Close") } }) }
    ui.questDetail?.takeUnless { ui.isMenuOverlayVisible }?.let { quest -> AlertDialog(onDismissRequest = runtime::closeQuestDetails,
        title = { Text(quest.title) }, text = { Column { Text(quest.description ?: quest.summary); quest.objectives.forEach { Text("${if (it.completed) "✓" else "○"} ${it.text}") }; quest.rewards.forEach { Text(it) } } },
        confirmButton = { TextButton(onClick = { runtime.toggleQuestTracking(quest.id) }) { Text(if (quest.tracked) "Stop tracking" else "Track quest") } },
        dismissButton = { TextButton(onClick = runtime::closeQuestDetails) { Text("Close") } }) }
}
