package com.example.starborn.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import com.example.starborn.ui.theme.themeColor
import java.util.Locale

@Composable
fun SkillsDialog(
    player: Player,
    skills: List<Skill>,
    viewModel: CombatViewModel,
    onDismiss: () -> Unit,
    onSkillSelected: (Skill) -> Unit,
    allowedSkillIds: Set<String>? = null,
    highlightedSkillId: String? = null,
    theme: Theme? = null,
    highContrastMode: Boolean = false
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    var detailSkill by remember { mutableStateOf<Skill?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accent
                    )
                    Text(
                        text = "Abilities",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accent.copy(alpha = 0.80f), Color.Transparent)
                            )
                        )
                )
            }
        },
        text = {
            if (skills.isEmpty()) {
                Text(
                    text = "No abilities available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(skills, key = { it.id }) { skill ->
                        val allowedByGuide = allowedSkillIds == null || skill.id in allowedSkillIds
                        val canUse = allowedByGuide && viewModel.canUseSkill(player.id, skill)
                        val cooldown = viewModel.skillCooldownRemaining(player.id, skill.id)
                        val highlighted = skill.id == highlightedSkillId
                        AbilityRow(
                            skill = skill,
                            cooldownRemaining = cooldown,
                            canUse = canUse,
                            highlighted = highlighted,
                            accent = accent,
                            border = border,
                            highContrastMode = highContrastMode,
                            onDetails = { detailSkill = skill },
                            onUse = { onSkillSelected(skill) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Close", color = Color.White)
            }
        }
    )

    detailSkill?.let { skill ->
        AbilityDetailsDialog(
            skill = skill,
            targetRequirement = viewModel.targetRequirementFor(skill),
            cooldownRemaining = viewModel.skillCooldownRemaining(player.id, skill.id),
            accent = accent,
            border = border,
            panel = panel,
            highContrastMode = highContrastMode,
            onDismiss = { detailSkill = null }
        )
    }
}

@Composable
private fun AbilityRow(
    skill: Skill,
    cooldownRemaining: Int,
    canUse: Boolean,
    highlighted: Boolean,
    accent: Color,
    border: Color,
    highContrastMode: Boolean,
    onDetails: () -> Unit,
    onUse: () -> Unit
) {
    val rowBorder = if (highlighted) accent else border.copy(alpha = 0.34f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) {
            accent.copy(alpha = if (highContrastMode) 0.18f else 0.12f)
        } else {
            Color.White.copy(alpha = if (highContrastMode) 0.10f else 0.07f)
        },
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, rowBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (canUse) Color.White else Color.White.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = abilitySummary(skill, cooldownRemaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cooldownRemaining > 0) {
                        Color(0xFFFFC857)
                    } else {
                        accent.copy(alpha = 0.88f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDetails) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details for ${skill.name}",
                    tint = accent
                )
            }
            Button(
                onClick = onUse,
                enabled = canUse
            ) {
                Text(if (cooldownRemaining > 0) cooldownRemaining.toString() else "Use")
            }
        }
    }
}

@Composable
private fun AbilityDetailsDialog(
    skill: Skill,
    targetRequirement: TargetRequirement,
    cooldownRemaining: Int,
    accent: Color,
    border: Color,
    panel: Color,
    highContrastMode: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panel.copy(alpha = if (highContrastMode) 1f else 0.98f),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = accent
            )
        },
        title = {
            Text(
                text = skill.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = skill.description.ifBlank { "No description available." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = if (highContrastMode) 0.10f else 0.06f),
                        border = BorderStroke(1.dp, border.copy(alpha = 0.34f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AbilityDetailLine("Type", skill.type.toDisplayLabel())
                            AbilityDetailLine("Target", targetRequirement.toDisplayLabel(skill))
                            AbilityDetailLine(
                                "Power",
                                skill.basePower.takeIf { it > 0 }?.toString() ?: "Utility"
                            )
                            AbilityDetailLine(
                                "Scaling",
                                skill.scaling?.toDisplayLabel() ?: "None"
                            )
                            AbilityDetailLine(
                                "Cooldown",
                                when {
                                    cooldownRemaining > 0 -> "$cooldownRemaining turns remaining"
                                    skill.cooldown > 0 -> "${skill.cooldown} turns"
                                    else -> "None"
                                }
                            )
                            skill.usesPerBattle?.let { uses ->
                                AbilityDetailLine("Battle limit", "$uses use${if (uses == 1) "" else "s"}")
                            }
                        }
                    }
                }
                if (!skill.statusApplications.isNullOrEmpty()) {
                    item {
                        AbilityDetailSection(
                            title = "Applies",
                            values = skill.statusApplications.orEmpty(),
                            accent = accent
                        )
                    }
                }
                if (!skill.combatTags.isNullOrEmpty()) {
                    item {
                        AbilityDetailSection(
                            title = "Combat traits",
                            values = skill.combatTags.orEmpty(),
                            accent = accent
                        )
                    }
                }
                if (!skill.conditions.isNullOrEmpty()) {
                    item {
                        AbilityDetailSection(
                            title = "Requirements",
                            values = skill.conditions.orEmpty(),
                            accent = Color(0xFFFFC857)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back to Abilities", color = Color.White)
            }
        }
    )
}

@Composable
private fun AbilityDetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.62f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun AbilityDetailSection(title: String, values: List<String>, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
        Text(
            text = values.joinToString("  |  ") { it.toDisplayLabel() },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.86f)
        )
    }
}

private fun abilitySummary(skill: Skill, cooldownRemaining: Int): String {
    val parts = buildList {
        if (skill.basePower > 0) add("${skill.basePower} power")
        skill.scaling?.takeIf { it.isNotBlank() }?.let { add(it.toDisplayLabel()) }
        when {
            cooldownRemaining > 0 -> add("$cooldownRemaining turn cooldown")
            skill.cooldown > 0 -> add("${skill.cooldown} turn cooldown")
            else -> add("No cooldown")
        }
    }
    return parts.joinToString("  |  ")
}

private fun TargetRequirement.toDisplayLabel(skill: Skill): String = when (this) {
    TargetRequirement.ENEMY -> "One enemy"
    TargetRequirement.ALLY -> "One ally"
    TargetRequirement.ANY -> "Any combatant"
    TargetRequirement.NONE -> when {
        skill.combatTags.orEmpty().any { it.equals("aoe", ignoreCase = true) } -> "Group"
        skill.combatTags.orEmpty().any { it.equals("support", ignoreCase = true) } -> "Self or group"
        else -> "Automatic"
    }
}

private fun String.toDisplayLabel(): String =
    trim()
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
