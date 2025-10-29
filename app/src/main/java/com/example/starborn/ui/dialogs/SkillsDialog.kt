package com.example.starborn.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill

@Composable
fun SkillsDialog(
    player: Player,
    skills: List<Skill>,
    onDismiss: () -> Unit,
    onSkillSelected: (Skill) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Skills") },
        text = {
            Column {
                for (skill in skills) {
                    Button(onClick = { onSkillSelected(skill) }) {
                        Text(text = skill.name)
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
