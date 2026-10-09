package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.*

@Composable
internal fun DesktopMenuPartyStrip(services: DesktopAppServices, members: List<PartyMemberStatusUi>,
    selectedId: String?, largeTargets: Boolean, onSelect: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 800.dp && !largeTargets
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        members.forEach { member ->
            Surface(onClick = { onSelect(member.id) }, shape = RoundedCornerShape(12.dp),
                color = if (selectedId == member.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                border = BorderStroke(1.dp, if (selectedId == member.id) FieldMenuDesign.gold else FieldMenuDesign.border.copy(alpha = .4f))) {
                Row(Modifier.width(if (largeTargets) 240.dp else if (compact) 150.dp else 210.dp).padding(if (compact) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    member.portraitPath?.let {
                        Image(rememberDesktopAssetPainter(it, services.assetProvider), member.name,
                            Modifier.size(if (largeTargets) 64.dp else if (compact) 38.dp else 56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(member.name, color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
                        if (!compact) Text("Level ${member.level} · ${member.hpLabel.orEmpty()}", color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
                        member.hpProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp), color = FieldMenuDesign.gold) }
                    }
                }
            }
        }
    }
}
}

@Composable
internal fun DesktopMenuPartyContent(services: DesktopAppServices, memberId: String?) {
    val ui by services.exploration.uiState.collectAsState()
    val member = ui.partyStatus.members.firstOrNull { it.id == memberId }
    if (member == null) { Text("No party members yet."); return }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        val portrait: @Composable (Modifier) -> Unit = { modifier ->
            Surface(modifier, color = FieldMenuDesign.elevatedPanel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = .28f))) {
                val stats: @Composable () -> Unit = {
                    Text(member.name, style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge, color = FieldMenuDesign.text, fontWeight = FontWeight.SemiBold)
                    Text("Level ${member.level} / ${member.hpLabel.orEmpty()}", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelLarge)
                    member.hpProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Color(0xFF8DD7BD)) }
                    if (wide) Text(member.xpLabel, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
                    if (wide) LinearProgressIndicator(progress = { member.xpProgress.coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = FieldMenuDesign.cyan)
                }
                val actions: @Composable () -> Unit = {
                    OutlinedButton(onClick = { services.exploration.openPartyMemberDetails(member.id) }, modifier = Modifier.fillMaxWidth()) { Text("Attributes") }
                    Button(onClick = { services.exploration.openSkillTree(member.id) }, modifier = Modifier.fillMaxWidth()) { Text("Skills") }
                }
                if (wide) Column(Modifier.background(Brush.verticalGradient(listOf(FieldMenuDesign.gold.copy(alpha = .10f), Color.Transparent)))
                    .padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    member.portraitPath?.let { Image(rememberDesktopAssetPainter(it, services.assetProvider), member.name, Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit) }
                    stats(); actions()
                } else Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    member.portraitPath?.let { Image(rememberDesktopAssetPainter(it, services.assetProvider), member.name, Modifier.size(56.dp), contentScale = ContentScale.Fit) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { stats() }
                    Column(Modifier.width(118.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
                }
            }
        }
        val gear: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DesktopMenuSection("Equipment")
                if (member.id.equals("ollie", true)) Text("This party member does not use equipment.", color = FieldMenuDesign.textMuted)
                else Box(Modifier.weight(1f)) { DesktopRuntimeGearContent(services, member.id) }
            }
        }
        if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            portrait(Modifier.width(280.dp).fillMaxHeight())
            gear(Modifier.weight(1f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            portrait(Modifier.fillMaxWidth().heightIn(max = 145.dp))
            gear(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
internal fun DesktopMenuAttributesContent(services: DesktopAppServices, details: PartyMemberDetailsUi) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Level ${details.level} · ${details.hpLabel.orEmpty()} · ${details.xpLabel}", color = FieldMenuDesign.gold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("Attributes" to details.primaryStats, "Combat" to details.combatStats).forEach { (title, stats) ->
                Surface(Modifier.weight(1f), color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .16f))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        stats.forEach { stat -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stat.label); Text(stat.value, color = FieldMenuDesign.gold) } }
                    }
                }
            }
        } }
        item { Text("Unlocked skills", style = MaterialTheme.typography.titleMedium) }
        items(details.unlockedSkills) { id -> services.skillDefinitions[id]?.let { skill ->
            Surface(Modifier.fillMaxWidth(), color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .16f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(skill.name, color = FieldMenuDesign.gold); Text(skill.description)
                    Text("Power: ${skill.basePower} · Cooldown: ${skill.cooldown} turns", style = MaterialTheme.typography.bodySmall)
                    skill.targeting?.let { Text("Target: ${it.replace('_', ' ')}") }
                    skill.usesPerBattle?.let { Text("Uses per battle: $it") }
                    skill.statusApplications.orEmpty().forEach { Text("Status: ${services.contentName(it)}") }
                }
            }
        } }
    }
}

