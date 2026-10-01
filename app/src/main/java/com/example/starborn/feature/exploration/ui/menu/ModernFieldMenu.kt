package com.example.starborn.feature.exploration.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    credits: String,
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
    Column(
        Modifier.fillMaxSize().background(if (highContrast) Color.Black else FieldMenuDesign.shell)
            .background(Brush.verticalGradient(listOf(
                roomAccent.copy(alpha = if (highContrast) 0f else .10f),
                roomAccent.copy(alpha = if (highContrast) 0f else .025f),
                Color.Transparent
            )))
            .statusBarsPadding().navigationBarsPadding()
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            val compact = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.2f
            val heading: @Composable () -> Unit = {
                Column {
                    Text(detailTitle ?: tab.fieldTitle(), color = FieldMenuDesign.text,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = if (compact) 2 else 1, overflow = TextOverflow.Ellipsis)
                    Text(credits, color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelMedium)
                }
            }
            val actions: @Composable () -> Unit = {
                IconButton(onClick = onSave, modifier = Modifier.size(if (largeTargets) 56.dp else 48.dp)) {
                    Icon(Icons.Default.Save, "Quick save", tint = FieldMenuDesign.textMuted)
                }
                IconButton(onClick = { onSelectTab(MenuTab.SETTINGS) }, modifier = Modifier.size(if (largeTargets) 56.dp else 48.dp)) {
                    Icon(Icons.Default.Settings, "Settings", tint = if (tab == MenuTab.SETTINGS) FieldMenuDesign.gold else FieldMenuDesign.textMuted)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(if (largeTargets) 56.dp else 48.dp)) {
                    Icon(Icons.Default.Close, "Close field menu", tint = FieldMenuDesign.text)
                }
            }
            if (compact) {
                Column {
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
        statusMessage?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = FieldMenuDesign.text, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        tutorialGuide()
        if (detailTitle == null && tab in listOf(MenuTab.STATS, MenuTab.INVENTORY) && party.members.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                party.members.forEach { member ->
                    val selected = member.id == selectedMemberId
                    Surface(
                        onClick = { onSelectMember(member.id); onSelectTab(MenuTab.STATS) },
                        modifier = Modifier.semantics { this.selected = selected },
                        color = if (selected) FieldMenuDesign.elevatedPanel else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MemberPortrait(member, Modifier.size(44.dp))
                            Column(Modifier.widthIn(min = 70.dp, max = 115.dp)) {
                                Text(member.name, color = if (selected) FieldMenuDesign.gold else FieldMenuDesign.text,
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Lv ${member.level}",
                                    color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(member.hpLabel ?: "HP unavailable", color = FieldMenuDesign.textMuted,
                                    style = MaterialTheme.typography.labelSmall)
                                member.hpProgress?.let { hp ->
                                    LinearProgressIndicator(progress = { hp.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp),
                                        color = if (hp < .3f) Color(0xFFFF887F) else Color(0xFF8DD7BD),
                                        trackColor = FieldMenuDesign.panel)
                                }
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = if (highContrast) Color.White else roomAccent.copy(alpha = .18f))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 760.dp).fillMaxWidth().verticalScroll(scroll).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { content() }
        }
        HorizontalDivider(color = if (highContrast) Color.White else roomAccent.copy(alpha = .14f))
        if (detailTitle != null) {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("Back to ${tab.fieldTitle()}")
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                listOf(MenuTab.STATS, MenuTab.INVENTORY, MenuTab.FIELD_KIT, MenuTab.JOURNAL, MenuTab.MAP).forEach { destination ->
                    val beacon = (tinkerTutorial && destination == MenuTab.FIELD_KIT) ||
                        (gearTutorial && destination == MenuTab.STATS)
                    val color = when {
                        destination == tab -> FieldMenuDesign.gold
                        beacon -> FieldMenuDesign.cyan
                        else -> FieldMenuDesign.textMuted
                    }
                    Surface(onClick = { onSelectTab(destination) },
                        modifier = Modifier.weight(1f).semantics { this.selected = destination == tab }, shape = RoundedCornerShape(10.dp),
                        color = if (destination == tab) FieldMenuDesign.elevatedPanel else Color.Transparent) {
                        Column(Modifier.heightIn(min = if (largeTargets) 80.dp else 64.dp).padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(destination.fieldIcon(), null, tint = color, modifier = Modifier.size(22.dp))
                            Text(destination.fieldTitle(), color = color, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (beacon && destination != tab) Text("•", color = FieldMenuDesign.cyan, fontSize = 10.sp)
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
private fun MemberPortrait(member: PartyMemberStatusUi, modifier: Modifier) {
    val painter = rememberAssetPainter(member.portraitPath, painterResource(R.drawable.inventory_icon))
    Image(painter, member.name, contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(10.dp)))
}

@Composable
internal fun ModernPartyPage(
    member: PartyMemberStatusUi?,
    onDetails: (String) -> Unit,
    onSkills: (String) -> Unit,
    equipment: @Composable () -> Unit
) {
    if (member == null) {
        Text("No party members yet.", color = FieldMenuDesign.textMuted)
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MemberPortrait(member, Modifier.size(88.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(member.name, style = MaterialTheme.typography.headlineSmall, color = FieldMenuDesign.text)
            Text("Level ${member.level}", color = FieldMenuDesign.gold)
            Text(member.hpLabel ?: "HP unavailable", color = FieldMenuDesign.textMuted)
            Text(member.xpLabel, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onDetails(member.id) }, modifier = Modifier.weight(1f).heightIn(min = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp)) { Text("Attributes") }
        Button(onClick = { onSkills(member.id) }, modifier = Modifier.weight(1f).heightIn(min = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp)) { Text("Skills") }
    }
    Text("Equipment", color = FieldMenuDesign.text, style = MaterialTheme.typography.titleMedium)
    equipment()
}
