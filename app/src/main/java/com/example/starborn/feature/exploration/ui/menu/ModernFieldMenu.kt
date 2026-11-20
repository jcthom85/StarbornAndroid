package com.example.starborn.feature.exploration.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import com.example.starborn.feature.exploration.viewmodel.PartyMemberStatusUi
import com.example.starborn.feature.exploration.viewmodel.PartyStatusUi
import com.example.starborn.ui.background.rememberAssetPainter

internal fun MenuTab.fieldTitle(): String = when (this) {
    MenuTab.STATS -> "Party"
    MenuTab.INVENTORY -> "Items"
    else -> label()
}

@Composable
internal fun ModernFieldMenu(
    tab: MenuTab,
    detailTitle: String?,
    party: PartyStatusUi,
    selectedMemberId: String?,
    credits: String?,
    scroll: ScrollState,
    tinkerTutorial: Boolean,
    gearTutorial: Boolean,
    roomAccent: Color,
    statusMessage: String? = null,
    onSelectTab: (MenuTab) -> Unit,
    onSelectMember: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    tutorialGuide: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val highContrast = LocalFieldMenuHighContrast.current
    val largeTargets = LocalFieldMenuLargeTargets.current
    val largeFont = LocalDensity.current.fontScale >= 1.3f

    Column(
        Modifier
            .fillMaxSize()
            .background(if (highContrast) Color.Black else FieldMenuDesign.shell)
            .background(
                Brush.verticalGradient(
                    listOf(
                        roomAccent.copy(alpha = if (highContrast) 0f else 0.14f),
                        FieldMenuDesign.panel.copy(alpha = if (highContrast) 0f else 0.55f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top atmospheric accent line
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            FieldMenuDesign.cyan.copy(alpha = 0.75f),
                            FieldMenuDesign.gold.copy(alpha = 0.65f),
                            roomAccent.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Tactical Header Bar
        Surface(
            color = if (highContrast) Color.Black else FieldMenuDesign.panel.copy(alpha = 0.92f),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val compact = maxWidth < 310.dp || LocalDensity.current.fontScale > 1.25f
                val heading: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (detailTitle != null) {
                            Text(
                                text = tab.fieldTitle().uppercase(),
                                color = FieldMenuDesign.cyan.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = detailTitle ?: tab.fieldTitle(),
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                maxLines = if (compact) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            credits?.takeIf { it.isNotBlank() }?.let { badgeText ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FieldMenuDesign.gold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = FieldMenuDesign.gold,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                val actionButtonSize = if (largeTargets) 52.dp else 44.dp
                val actions: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onSave,
                            shape = RoundedCornerShape(10.dp),
                            color = FieldMenuDesign.elevatedPanel,
                            border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder),
                            modifier = Modifier.size(actionButtonSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Save, "Quick save", tint = FieldMenuDesign.textMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                        Surface(
                            onClick = { onSelectTab(MenuTab.SETTINGS) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (tab == MenuTab.SETTINGS) FieldMenuDesign.primaryActionFill else FieldMenuDesign.elevatedPanel,
                            border = BorderStroke(1.dp, if (tab == MenuTab.SETTINGS) FieldMenuDesign.gold else FieldMenuDesign.subtleBorder),
                            modifier = Modifier.size(actionButtonSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    "Settings",
                                    tint = if (tab == MenuTab.SETTINGS) FieldMenuDesign.gold else FieldMenuDesign.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onClose,
                            shape = RoundedCornerShape(10.dp),
                            color = FieldMenuDesign.elevatedPanel,
                            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f)),
                            modifier = Modifier.size(actionButtonSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, "Close field menu", tint = FieldMenuDesign.text, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        heading()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { heading() }
                        actions()
                    }
                }
            }
        }

        statusMessage?.takeIf { it.isNotBlank() }?.let {
            Surface(
                color = FieldMenuDesign.primaryActionFill.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = it,
                    color = FieldMenuDesign.text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Persistent Crew Switcher Strip (Party root + Abilities + Attributes)
        if (tab == MenuTab.STATS && party.members.size > 1) {
            com.example.starborn.ui.menu.OverflowPartyRow(
                keys = party.members.map { it.id },
                selectedKey = selectedMemberId,
                itemWidth = if (detailTitle != null || largeFont) 152.dp else 168.dp,
                background = FieldMenuDesign.deepCard,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) { index ->
                val member = party.members[index]
                val selected = member.id == selectedMemberId
                Surface(
                    onClick = { onSelectMember(member.id) },
                    modifier = Modifier.semantics { this.selected = selected },
                    color = if (selected) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel.copy(alpha = 0.6f),
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) FieldMenuDesign.gold else FieldMenuDesign.subtleBorder.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemberPortrait(
                            member = member,
                            selected = selected,
                            modifier = Modifier.size(if (detailTitle != null) 34.dp else 44.dp)
                        )
                        Column(Modifier.widthIn(min = 60.dp, max = 104.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = member.name,
                                    color = if (selected) FieldMenuDesign.gold else FieldMenuDesign.text,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "L${member.level}",
                                    color = FieldMenuDesign.cyan,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            if (detailTitle == null) {
                                Text(
                                    text = member.hpLabel ?: "HP —",
                                    color = FieldMenuDesign.textMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                member.hpProgress?.let { hp ->
                                    LinearProgressIndicator(
                                        progress = { hp.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 3.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (hp < 0.3f) FieldMenuDesign.coral else FieldMenuDesign.mint,
                                        trackColor = FieldMenuDesign.shell
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = if (highContrast) Color.White else FieldMenuDesign.cyan.copy(alpha = 0.22f))

        // Main Scrollable Viewport
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tutorialGuide()
                content()
            }
        }

        HorizontalDivider(color = if (highContrast) Color.White else FieldMenuDesign.cyan.copy(alpha = 0.20f))

        // Bottom Navigation Rail or Detail Return Bar
        if (detailTitle != null) {
            Surface(
                color = FieldMenuDesign.panel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = FieldMenuDesign.primaryActionFill,
                    border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(min = if (largeTargets) 56.dp else 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FieldMenuDesign.cyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Back to ${tab.fieldTitle()}",
                            color = FieldMenuDesign.text,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } else {
            Surface(
                color = if (highContrast) Color.Black else FieldMenuDesign.panel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(MenuTab.STATS, MenuTab.INVENTORY, MenuTab.FIELD_KIT, MenuTab.JOURNAL, MenuTab.MAP).forEach { destination ->
                        val active = destination == tab
                        val beacon = (tinkerTutorial && destination == MenuTab.FIELD_KIT) ||
                            (gearTutorial && destination == MenuTab.STATS)
                        val color = when {
                            active -> FieldMenuDesign.gold
                            beacon -> FieldMenuDesign.cyan
                            else -> FieldMenuDesign.textMuted
                        }
                        Surface(
                            onClick = { onSelectTab(destination) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { this.selected = active },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) FieldMenuDesign.elevatedPanel else Color.Transparent,
                            border = when {
                                active -> BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.65f))
                                beacon -> BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.65f))
                                else -> null
                            }
                        ) {
                            Column(
                                Modifier
                                    .heightIn(min = if (largeTargets) 74.dp else 58.dp)
                                    .padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(destination.fieldIcon(), null, tint = color, modifier = Modifier.size(21.dp))
                                    if (beacon && !active) {
                                        Box(
                                            Modifier
                                                .offset(x = 6.dp, y = (-2).dp)
                                                .size(7.dp)
                                                .background(FieldMenuDesign.cyan, CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = destination.fieldTitle(),
                                    color = if (active) FieldMenuDesign.text else color,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
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

private fun MenuTab.fieldIcon(): ImageVector = when (this) {
    MenuTab.STATS -> Icons.Default.People
    MenuTab.INVENTORY -> Icons.Default.Backpack
    MenuTab.FIELD_KIT -> Icons.Default.Build
    MenuTab.JOURNAL -> Icons.Default.Book
    MenuTab.MAP -> Icons.Default.Map
    MenuTab.SETTINGS -> Icons.Default.Settings
}

@Composable
private fun MemberPortrait(
    member: PartyMemberStatusUi,
    modifier: Modifier,
    selected: Boolean = false
) {
    val painter = rememberAssetPainter(member.portraitPath, painterResource(R.drawable.inventory_icon))
    Image(
        painter = painter,
        contentDescription = member.name,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (selected) FieldMenuDesign.gold else FieldMenuDesign.cyan.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModernPartyPage(
    member: PartyMemberStatusUi?,
    onDetails: (String) -> Unit,
    onSkills: (String) -> Unit,
    availableAp: Int = 0,
    activeMealSummary: String? = null,
    showEquipmentHint: Boolean = false,
    equipment: @Composable () -> Unit
) {
    if (member == null) {
        Text("No party members yet.", color = FieldMenuDesign.textMuted)
        return
    }
    val effectiveAp = if (availableAp > 0) availableAp else member.availableAp
    val largeTargets = LocalFieldMenuLargeTargets.current
    val largeFont = LocalDensity.current.fontScale >= 1.3f

    // 1. Crew Hero Dossier Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = FieldMenuDesign.panel,
        border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            FieldMenuDesign.primaryActionFill.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MemberPortrait(member, Modifier.size(72.dp), selected = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = FieldMenuDesign.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FieldMenuDesign.gold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = "Level ${member.level}",
                                color = FieldMenuDesign.gold,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // HP Bar + Value
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("VITALS", color = FieldMenuDesign.mint, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text(member.hpLabel ?: "HP unavailable", color = FieldMenuDesign.text, style = MaterialTheme.typography.labelSmall)
                        }
                        member.hpProgress?.let { hp ->
                            LinearProgressIndicator(
                                progress = { hp.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (hp < 0.3f) FieldMenuDesign.coral else FieldMenuDesign.mint,
                                trackColor = FieldMenuDesign.shell
                            )
                        }
                    }

                    // XP Bar + Value
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PROGRESSION", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text(member.xpLabel, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(
                            progress = { member.xpProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = FieldMenuDesign.cyan,
                            trackColor = FieldMenuDesign.shell
                        )
                    }
                }
            }

            if (!activeMealSummary.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FieldMenuDesign.cookingActionFill.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, FieldMenuDesign.ember.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Well-Fed: $activeMealSummary",
                        color = FieldMenuDesign.ember,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    // 2. Dual Tactical Cards: Abilities & Attributes (lean — no filler subtitles)
    val cardHeight = if (largeTargets) 62.dp else 54.dp
    val abilitiesCard: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            onClick = { onSkills(member.id) },
            modifier = mod.heightIn(min = cardHeight),
            shape = RoundedCornerShape(12.dp),
            color = FieldMenuDesign.primaryActionFill,
            border = BorderStroke(1.5.dp, if (effectiveAp > 0) FieldMenuDesign.gold else FieldMenuDesign.cyan.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (effectiveAp > 0) FieldMenuDesign.gold else FieldMenuDesign.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Abilities",
                        color = FieldMenuDesign.text,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (effectiveAp > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FieldMenuDesign.gold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "$effectiveAp AP",
                                color = FieldMenuDesign.gold,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                Text("›", color = FieldMenuDesign.gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    val attributesCard: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            onClick = { onDetails(member.id) },
            modifier = mod.heightIn(min = cardHeight),
            shape = RoundedCornerShape(12.dp),
            color = FieldMenuDesign.elevatedPanel,
            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = FieldMenuDesign.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Attributes",
                        color = FieldMenuDesign.text,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("›", color = FieldMenuDesign.cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (largeFont) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            abilitiesCard(Modifier.fillMaxWidth())
            attributesCard(Modifier.fillMaxWidth())
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            abilitiesCard(Modifier.weight(1f))
            attributesCard(Modifier.weight(1f))
        }
    }

    // 3. Equipment Section
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Equipment",
            color = FieldMenuDesign.text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        if (showEquipmentHint) {
            Text(
                text = "Tap slot to swap or mod",
                color = FieldMenuDesign.cyan,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
    equipment()
}

