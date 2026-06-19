package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.starborn.R
import com.example.starborn.feature.exploration.viewmodel.CharacterStatValueUi
import com.example.starborn.feature.exploration.viewmodel.PartyMemberDetailsUi
import com.example.starborn.ui.background.rememberAssetPainter

@Composable
fun PartyMemberDetailsDialog(
    details: PartyMemberDetailsUi,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val onSurface = MaterialTheme.colorScheme.onSurface
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
        title = {
            Text(
                text = "${details.name} • Lv ${details.level}",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val portraitPath = details.portraitPath
                    if (!portraitPath.isNullOrBlank()) {
                        Image(
                            painter = rememberAssetPainter(portraitPath, painterResource(R.drawable.inventory_icon)),
                            contentDescription = details.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, onSurface.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = details.xpLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface
                        )
                        details.hpLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                StatSection(title = "Attributes", stats = details.primaryStats)
                StatSection(title = "Combat Stats", stats = details.combatStats)
                if (details.unlockedSkills.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Unlocked Skills",
                            style = MaterialTheme.typography.titleSmall,
                            color = onSurface
                        )
                        details.unlockedSkills.forEach { skill ->
                            Text(
                                text = "• $skill",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun StatSection(
    title: String,
    stats: List<CharacterStatValueUi>
) {
    if (stats.isEmpty()) return
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            stats.forEach { stat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface
                    )
                }
            }
        }
    }
}
