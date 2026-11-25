package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.presentation.AstraArcadeStatusUi
import com.example.starborn.feature.exploration.presentation.AstraCatalog
import com.example.starborn.feature.exploration.presentation.canPresentLevelUp
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.ExplorationController
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopRuntimeOverlays(
    services: DesktopAppServices,
    runtime: ExplorationController,
    ui: ExplorationUiState,
    externalModal: Boolean = false
) {
    if (ui.showBurgQuestAstraExitDialog) {
        DesktopConsoleModal(
            eyebrow = "Astra Flight Deck",
            title = "Explore the Astra",
            onDismiss = runtime::dismissBurgQuestAstraExitDialog,
            accent = FieldMenuDesign.gold,
            maxWidth = 520.dp,
            testTag = "astra-burgquest-exit-modal",
            footer = {
                Button(
                    onClick = runtime::dismissBurgQuestAstraExitDialog,
                    modifier = Modifier.desktopPointerHover(),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold, contentColor = Color(0xFF1A1408))
                ) {
                    Text("Continue exploring", fontWeight = FontWeight.Bold)
                }
            }
        ) {
            Text(
                "Disembarking is unavailable during the BurgQuest demo. Explore the ship, simulation deck or workbench before finishing your visit.",
                color = FieldMenuDesign.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (ui.isAstraRecreationVisible) {
        val entries: List<AstraArcadeStatusUi> = if (ui.astraArcadeEntries.isNotEmpty()) {
            ui.astraArcadeEntries
        } else {
            ui.astraArcades.map { arcade ->
                AstraArcadeStatusUi(
                    id = arcade.id,
                    title = arcade.title,
                    worldHint = arcade.worldHint,
                    discovered = true,
                    installed = true,
                    highScore = 0,
                    claimedTiers = emptySet()
                )
            }
        }
        val installedCount = entries.count { it.installed }
        val totalCount = entries.size.coerceAtLeast(1)
        DesktopConsoleModal(
            eyebrow = "Astra Lounge · Recreation",
            title = "Recreation",
            onDismiss = runtime::closeAstraRecreation,
            accent = FieldMenuDesign.cyan,
            maxWidth = 680.dp,
            testTag = "astra-recreation-console",
            headerTrailing = {
                if (ui.astraPrizesAvailable) {
                    Surface(
                        color = FieldMenuDesign.gold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "Tokens: ${ui.astraArcadeTokens} · $installedCount/$totalCount",
                            color = FieldMenuDesign.gold,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            },
            footer = {
                TextButton(onClick = runtime::closeAstraRecreation, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            DesktopConsoleLazyList(accent = FieldMenuDesign.cyan) {
                item(key = "rec-actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = runtime::openRecreationFilms,
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp).desktopPointerHover(),
                            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.5f))
                        ) {
                            Text("Great Frontier films", fontWeight = FontWeight.SemiBold)
                        }
                        if (ui.astraPrizesAvailable) {
                            OutlinedButton(
                                onClick = runtime::openRecreationPrizes,
                                modifier = Modifier.weight(1f).heightIn(min = 46.dp).desktopPointerHover(),
                                border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.65f))
                            ) {
                                Text("Arcade prizes", color = FieldMenuDesign.gold, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item(key = "rec-heading") {
                    Text(
                        "Restored cabinets ($installedCount/$totalCount)",
                        color = FieldMenuDesign.cyan,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (entries.isEmpty()) {
                    item(key = "rec-empty") {
                        Text(
                            "Restore a cabinet at the workbench to add a game here.",
                            color = FieldMenuDesign.textMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                itemsIndexed(entries, key = { index, game -> "${game.id}_$index" }) { index, game ->
                    when {
                        game.installed -> {
                            Surface(
                                onClick = { runtime.openRecreationArcade(game.id) },
                                color = Color(0xFF0E2030),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth().desktopPointerHover()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            game.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = FieldMenuDesign.text
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "HI ${game.highScore}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FieldMenuDesign.textMuted
                                            )
                                            listOf(
                                                "BRONZE" to Color(0xFFCD7F32),
                                                "SILVER" to Color(0xFFC0C8D4),
                                                "GOLD" to Color(0xFFFFD54F)
                                            ).forEach { (tier, tint) ->
                                                val earned = tier in game.claimedTiers
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (earned) tint.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                                    border = BorderStroke(1.dp, if (earned) tint.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f))
                                                ) {
                                                    Text(
                                                        text = tier.first().toString(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (earned) tint else Color.White.copy(alpha = 0.28f),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = { runtime.openRecreationArcade(game.id) },
                                        modifier = Modifier.desktopPointerHover(),
                                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.cyan, contentColor = Color(0xFF041018))
                                    ) {
                                        Text("Play", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        game.discovered -> {
                            Surface(
                                color = Color(0xFF1A1710),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.38f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        game.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = FieldMenuDesign.gold
                                    )
                                    Text(
                                        "Logic board recovered — repair core in Menu → Tinker to install.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC7B89A)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "Cabinet 0${index + 1} — Undiscovered",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = FieldMenuDesign.text.copy(alpha = 0.58f)
                                    )
                                    if (game.worldHint.isNotBlank()) {
                                        Text(
                                            "Signal trace: ${game.worldHint}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FieldMenuDesign.textMuted.copy(alpha = 0.72f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ui.astraFacility?.let { facility ->
        val showCargoTabs = facility.title == "Cargo Manifest" || facility.title == "Relic Array"
        DesktopConsoleModal(
            eyebrow = "Astra Shipboard Terminal",
            title = facility.title,
            onDismiss = runtime::closeAstraFacility,
            accent = FieldMenuDesign.cyan,
            maxWidth = 680.dp,
            testTag = "astra-facility-console",
            footer = {
                TextButton(onClick = runtime::closeAstraFacility, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            if (showCargoTabs) {
                DesktopCargoRecordTabs(
                    selected = if (facility.title == "Relic Array") "relics" else "cargo",
                    onSelect = runtime::selectCargoRecord
                )
            }
            DesktopConsoleLazyList(accent = FieldMenuDesign.cyan) {
                if (facility.entries.isEmpty()) {
                    item(key = "facility-empty") {
                        Text("No entries recorded yet.", color = FieldMenuDesign.textMuted)
                    }
                }
                items(facility.entries, key = { it.title }) { entry ->
                    Surface(
                        color = FieldMenuDesign.panel,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (entry.isNew) FieldMenuDesign.gold.copy(alpha = 0.6f) else FieldMenuDesign.cyan.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.title + if (entry.isNew) " - New" else "",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (entry.isNew) FieldMenuDesign.gold else FieldMenuDesign.text,
                                    modifier = Modifier.weight(1f)
                                )
                                if (entry.isNew) {
                                    Surface(
                                        color = FieldMenuDesign.gold.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            "NEW",
                                            color = FieldMenuDesign.gold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(entry.body, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (ui.isCatchLogVisible) {
        val records = ui.fishingJournal.flatMap { it.species }.groupBy { it.itemId }
        val caughtCount = records.values.count { list -> list.any { it.caught } }
        val totalSpecies = records.size
        DesktopConsoleModal(
            eyebrow = "Specimen Archive",
            title = "Astra Catch Log",
            onDismiss = runtime::closeCatchLog,
            accent = FieldMenuDesign.cyan,
            maxWidth = 680.dp,
            testTag = "astra-catch-log-console",
            headerTrailing = {
                Surface(
                    color = FieldMenuDesign.cyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f))
                ) {
                    Text(
                        "$caughtCount/$totalSpecies species recorded",
                        color = FieldMenuDesign.cyan,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            },
            footer = {
                TextButton(onClick = runtime::closeCatchLog, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            DesktopCargoRecordTabs("catch", runtime::selectCargoRecord)
            Text(
                "Cargo Bay specimen archive. Records remain after cooking or selling.",
                color = FieldMenuDesign.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
            DesktopConsoleLazyList(accent = FieldMenuDesign.cyan) {
                if (ui.fishingJournal.isEmpty()) {
                    item(key = "catch-empty") {
                        Text("Your catches will appear here after fishing.", color = FieldMenuDesign.textMuted)
                    }
                }
                items(ui.fishingJournal, key = { it.zoneId }) { zone ->
                    Surface(
                        color = FieldMenuDesign.panel,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                zone.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FieldMenuDesign.gold
                            )
                            zone.species.forEach { fish ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (!fish.caught) "Undiscovered species"
                                        else fish.name + if (fish.clean) " - Clean catch" else " - Caught",
                                        color = if (fish.caught) FieldMenuDesign.text else FieldMenuDesign.textMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (fish.caught && fish.clean) {
                                        Surface(
                                            color = Color(0xFF80E7A0).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, Color(0xFF80E7A0).copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                "CLEAN",
                                                color = Color(0xFF80E7A0),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (ui.isMilestoneGalleryVisible) {
        DesktopConsoleModal(
            eyebrow = "Campaign Chronicle",
            title = "Milestone gallery",
            onDismiss = runtime::closeMilestoneGallery,
            accent = FieldMenuDesign.gold,
            maxWidth = 660.dp,
            testTag = "astra-milestone-gallery-console",
            footer = {
                TextButton(onClick = runtime::closeMilestoneGallery, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            DesktopConsoleLazyList(accent = FieldMenuDesign.gold) {
                if (ui.milestoneBands.isEmpty()) {
                    item(key = "milestones-empty") {
                        Text("Your journey's milestones will appear here.", color = FieldMenuDesign.textMuted)
                    }
                }
                itemsIndexed(ui.milestoneBands) { index, band ->
                    Surface(
                        color = FieldMenuDesign.panel,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = FieldMenuDesign.gold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    "%02d".format(index + 1),
                                    color = FieldMenuDesign.gold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Text(band.message, color = FieldMenuDesign.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (ui.isAstraNavConsoleVisible) {
        DesktopConsoleModal(
            eyebrow = "Astra Helm · Navigation",
            title = "Astra navigation",
            onDismiss = runtime::dismissAstraNavConsole,
            accent = FieldMenuDesign.cyan,
            maxWidth = 680.dp,
            testTag = "astra-nav-console",
            footer = {
                TextButton(
                    onClick = runtime::disembarkAstra,
                    enabled = ui.astraTransitTitle == null,
                    modifier = Modifier.desktopPointerHover(ui.astraTransitTitle == null)
                ) {
                    Text("Disembark", color = FieldMenuDesign.gold, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = runtime::dismissAstraNavConsole, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            Surface(
                color = FieldMenuDesign.panel,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        ui.astraTransitTitle ?: ui.astraArrivalTitle ?: "Docked: ${ui.astraDockedTitle}",
                        color = FieldMenuDesign.gold,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (ui.astraRouteHint.isNotBlank()) {
                        Text(ui.astraRouteHint, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            DesktopConsoleLazyList(accent = FieldMenuDesign.cyan, maxHeight = 360.dp) {
                items(ui.astraDestinations, key = { "${it.worldId}:${it.hubId}:${it.roomId}" }) { destination ->
                    OutlinedButton(
                        onClick = {
                            runtime.travelToWorldFromAstra(
                                destination.worldId,
                                destination.hubId,
                                destination.roomId,
                                destination.nodeId
                            )
                        },
                        enabled = ui.astraTransitTitle == null,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.42f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0B1924),
                            contentColor = FieldMenuDesign.text
                        ),
                        modifier = Modifier.fillMaxWidth().desktopPointerHover(ui.astraTransitTitle == null)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(destination.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = FieldMenuDesign.cyan)
                            Text(destination.desc, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
                        }
                    }
                }
            }
        }
    }

    if (ui.isSimulationDeckVisible) {
        DesktopConsoleModal(
            eyebrow = "Combat Holodeck",
            title = "Simulation deck",
            onDismiss = runtime::dismissSimulationDeck,
            accent = FieldMenuDesign.cyan,
            maxWidth = 700.dp,
            testTag = "astra-simulation-console",
            footer = {
                TextButton(onClick = runtime::dismissSimulationDeck, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            DesktopConsoleLazyList(accent = FieldMenuDesign.cyan) {
                item(key = "sim-rules") {
                    Text(AstraCatalog.simulationRules, color = FieldMenuDesign.text, style = MaterialTheme.typography.bodyMedium)
                }
                item(key = "sim-hint") {
                    Text(
                        "More programs unlock as the crew completes campaign encounters.",
                        color = FieldMenuDesign.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ui.availableSimulations.groupBy { it.category }.forEach { (category, programs) ->
                    item(key = "sim-cat-$category") {
                        Text(
                            category.uppercase(),
                            color = FieldMenuDesign.gold,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        )
                    }
                    items(programs, key = { it.title }) { program ->
                        Surface(
                            color = FieldMenuDesign.panel,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { runtime.launchSimulationCombat(program.enemyIds) },
                                    modifier = Modifier.desktopPointerHover(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FieldMenuDesign.cyan.copy(alpha = 0.22f),
                                        contentColor = FieldMenuDesign.text
                                    ),
                                    border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.6f))
                                ) {
                                    Text(program.title, fontWeight = FontWeight.Bold)
                                }
                                Text(program.description, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (ui.isTapeDeckVisible) {
        DesktopConsoleModal(
            eyebrow = "Holotape Archive",
            title = "Great Frontier tapes",
            onDismiss = runtime::dismissTapeDeck,
            accent = FieldMenuDesign.gold,
            maxWidth = 700.dp,
            testTag = "astra-tape-deck-console",
            footer = {
                if (ui.playingTapeId != null) {
                    OutlinedButton(onClick = runtime::stopTapeTrack, modifier = Modifier.desktopPointerHover()) {
                        Text("Stop")
                    }
                }
                TextButton(onClick = runtime::dismissTapeDeck, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            DesktopConsoleLazyList(accent = FieldMenuDesign.gold) {
                items(AstraCatalog.films, key = { it.first }) { (tape, cue, metadata) ->
                    val owned = ui.inventoryPreview.any { it.id == tape && it.quantity > 0 }
                    val isPlaying = ui.playingTapeId == tape
                    Surface(
                        color = FieldMenuDesign.panel,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isPlaying -> FieldMenuDesign.gold.copy(alpha = 0.75f)
                                owned -> FieldMenuDesign.cyan.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.10f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { runtime.playTapeTrack(tape, cue) },
                                enabled = owned,
                                modifier = Modifier.desktopPointerHover(owned),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) FieldMenuDesign.gold else FieldMenuDesign.cyan.copy(alpha = 0.22f),
                                    contentColor = if (isPlaying) Color(0xFF1A1408) else FieldMenuDesign.text
                                )
                            ) {
                                Text(
                                    metadata.first + if (isPlaying) " \u00B7 playing" else "",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(metadata.second, style = MaterialTheme.typography.labelSmall, color = FieldMenuDesign.gold)
                            services.itemRepository.findItem(tape)?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
                            }
                        }
                    }
                }
                item(key = "tape-stop") {
                    TextButton(onClick = runtime::stopTapeTrack, modifier = Modifier.desktopPointerHover()) {
                        Text("Stop")
                    }
                }
            }
        }
    }

    ui.levelUpPrompt?.takeIf { canPresentLevelUp(ui, externalModal) }?.let { level ->
        val portraitPath = "images/characters/emotes/${level.characterId}_cool.png"
        DesktopConsoleModal(
            eyebrow = "Progression",
            title = "${level.characterName} · Level ${level.newLevel}",
            onDismiss = runtime::dismissLevelUpPrompt,
            accent = FieldMenuDesign.gold,
            maxWidth = 540.dp,
            testTag = "exploration-level-up-modal",
            footer = {
                Button(
                    onClick = runtime::dismissLevelUpPrompt,
                    modifier = Modifier.desktopPointerHover(),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.gold, contentColor = Color(0xFF1A1408))
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().informationAcknowledgement(runtime::dismissLevelUpPrompt),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B0F15))
                        .border(1.dp, FieldMenuDesign.gold.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberDesktopAssetPainter(portraitPath, services.assetProvider),
                        contentDescription = level.characterName,
                        modifier = Modifier.matchParentSize().padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    level.statChanges.forEach { stat ->
                        Text("${stat.label}: ${stat.value}", color = FieldMenuDesign.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (level.unlockedSkills.isNotEmpty()) {
                HorizontalDivider(color = FieldMenuDesign.gold.copy(alpha = 0.25f))
                Text("Unlocked Abilities", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    level.unlockedSkills.forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = FieldMenuDesign.gold.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.55f))
                        ) {
                            Text(
                                "Unlocked: ${skill.name}",
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    ui.skillTreeOverlay?.takeUnless { ui.isMenuOverlayVisible }?.let { tree ->
        DesktopFieldMenuTheme(services) {
            DesktopConsoleModal(
                eyebrow = "Skill Network · ${tree.availableAp} AP Available",
                title = "${tree.characterName} · ${tree.availableAp} AP available",
                onDismiss = runtime::closeSkillTreeOverlay,
                accent = FieldMenuDesign.gold,
                maxWidth = 1180.dp,
                maxHeightFraction = 0.90f,
                testTag = "standalone-skill-tree-console",
                footer = {
                    TextButton(onClick = runtime::closeSkillTreeOverlay, modifier = Modifier.desktopPointerHover()) {
                        Text("Close")
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 620.dp)) {
                    DesktopMenuSkillsContent(services, tree)
                }
            }
        }
    }

    ui.questDetail?.takeUnless { ui.isMenuOverlayVisible }?.let { quest ->
        DesktopConsoleModal(
            eyebrow = if (quest.tracked) "Tracked Mission" else "Mission Dossier",
            title = quest.title,
            onDismiss = runtime::closeQuestDetails,
            accent = FieldMenuDesign.gold,
            maxWidth = 640.dp,
            testTag = "standalone-quest-detail-console",
            footer = {
                OutlinedButton(
                    onClick = { runtime.toggleQuestTracking(quest.id) },
                    modifier = Modifier.desktopPointerHover(),
                    border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.6f))
                ) {
                    Text(if (quest.tracked) "Stop tracking" else "Track quest", color = FieldMenuDesign.gold)
                }
                TextButton(onClick = runtime::closeQuestDetails, modifier = Modifier.desktopPointerHover()) {
                    Text("Close")
                }
            }
        ) {
            DesktopMenuScrollPane(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                Text(quest.description ?: quest.summary, color = FieldMenuDesign.text, style = MaterialTheme.typography.bodyMedium)
                if (quest.objectives.isNotEmpty()) {
                    HorizontalDivider(color = FieldMenuDesign.gold.copy(alpha = 0.22f))
                    Text("Objectives", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    quest.objectives.forEach { obj ->
                        Text(
                            "${if (obj.completed) "✓" else "○"} ${obj.text}",
                            color = if (obj.completed) Color(0xFF80E7A0) else FieldMenuDesign.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (quest.rewards.isNotEmpty()) {
                    HorizontalDivider(color = FieldMenuDesign.gold.copy(alpha = 0.22f))
                    Text("Rewards", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quest.rewards.forEach { reward ->
                            Surface(
                                color = FieldMenuDesign.cyan.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.4f))
                            ) {
                                Text(reward, color = FieldMenuDesign.text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopCargoRecordTabs(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("cargo" to "Supplies", "catch" to "Catch Log", "relics" to "Relics").forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                modifier = Modifier.desktopPointerHover(),
                label = { Text(label) }
            )
        }
    }
}
