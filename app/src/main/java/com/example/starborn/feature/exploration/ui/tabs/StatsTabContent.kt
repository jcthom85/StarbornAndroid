package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.ui.ThemedMenuButton
import com.example.starborn.feature.exploration.viewmodel.PartyMemberStatusUi
import com.example.starborn.feature.exploration.viewmodel.PartyStatusUi
import com.example.starborn.ui.background.rememberAssetPainter

@Composable
fun StatsTabContent(
    partyStatus: PartyStatusUi,
    accentColor: Color,
    borderColor: Color,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    MenuSectionCard(
        title = "Party Status",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        PartyStatusPanel(
            partyStatus = partyStatus,
            accentColor = accentColor,
            onShowSkillTree = onShowSkillTree,
            onShowDetails = onShowDetails
        )
    }
}

@Composable
private fun PartyStatusPanel(
    partyStatus: PartyStatusUi,
    accentColor: Color,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    if (partyStatus.members.isEmpty()) {
        Text(
            text = "No party members yet.",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        partyStatus.members.forEach { member ->
            PartyMemberCard(
                member = member,
                accentColor = accentColor,
                onShowSkillTree = onShowSkillTree,
                onShowDetails = onShowDetails
            )
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun PartyMemberCard(
    member: PartyMemberStatusUi,
    accentColor: Color,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    val portraitPainter = rememberAssetPainter(
        imagePath = member.portraitPath,
        fallback = painterResource(R.drawable.inventory_icon)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.22f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = portraitPainter,
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = member.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Lv ${member.level}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            ThemedMenuButton(
                label = "Details",
                accentColor = accentColor,
                modifier = Modifier
                    .widthIn(min = 96.dp, max = 120.dp)
                    .heightIn(min = 40.dp),
                onClick = { onShowDetails(member.id) }
            )
        }

        val bars = buildList {
            member.hpProgress?.let { add(Triple(member.hpLabel ?: "HP", it, Color(0xFFFF5252))) }
            add(Triple("XP", member.xpProgress, Color(0xFF7C4DFF)))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bars.forEach { (label, progress, color) ->
                MiniStatBar(
                    label = label,
                    progress = progress,
                    accentColor = color
                )
            }
        }
    }
}

@Composable
private fun MiniStatBar(
    label: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall
        )
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accentColor,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}
