package com.example.starborn.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.feature.combat.viewmodel.CombatViewModel

@Composable
fun SkillsDialog(
    player: Player,
    skills: List<Skill>,
    viewModel: CombatViewModel,
    onDismiss: () -> Unit,
    onSkillSelected: (Skill) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Skills") },
        text = {
            Column {
                for (skill in skills) {
                    val canUse = viewModel.canUseSkill(player.id, skill)
                    val cooldown = viewModel.skillCooldownRemaining(player.id, skill.id)
                    val label = if (cooldown > 0) "${skill.name} ($cooldown)" else skill.name
                    Button(
                        onClick = { if (canUse) onSkillSelected(skill) },
                        enabled = canUse
                    ) {
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}
