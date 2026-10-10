package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices

@Composable
internal fun DesktopRuntimeStatsContent(services: DesktopAppServices) {
    val runtime = services.exploration
    val ui by runtime.uiState.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(ui.partyStatus.members, key = { it.id }) { member ->
            DesktopMenuCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${member.name} · Level ${member.level}", style = MaterialTheme.typography.titleMedium)
                Text("${member.hpLabel.orEmpty()} · ${member.xpLabel}")
                Row {
                    TextButton(onClick = { runtime.openPartyMemberDetails(member.id) }) { Text("Stats") }
                    TextButton(onClick = { runtime.openSkillTree(member.id) }) { Text("Skill tree") }
                }
            } }
        }
        ui.partyMemberDetails?.let { details -> item {
            Text(details.name, color = Color.Cyan)
            (details.primaryStats + details.combatStats).forEach { Text("${it.label}: ${it.value}", color = Color.White) }
            details.unlockedSkills.forEach { id ->
                val skill = services.skillDefinitions[id] ?: services.skillDefinitions.values.firstOrNull { it.name.equals(id, true) }
                if (skill != null) {
                    Text(skill.name, color = Color.White)
                    Text(skill.description)
                    Text("Power: ${skill.basePower} \u00B7 Cooldown: ${skill.cooldown} turns")
                    skill.targeting?.let { Text("Target: ${it.replace('_', ' ')}") }
                    skill.usesPerBattle?.let { Text("Uses per battle: $it") }
                    skill.statusApplications.orEmpty().forEach { Text("Status: ${services.contentName(it)}") }
                } else {
                    Text(id, color = Color.White)
                }
            }
        } }
    }
}
