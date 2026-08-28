package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.R
import com.example.starborn.feature.exploration.viewmodel.CharacterStatValueUi
import com.example.starborn.feature.exploration.viewmodel.PartyMemberDetailsUi
import com.example.starborn.ui.background.rememberAssetPainter

@Composable
fun PartyMemberDetailsDialog(
    details: PartyMemberDetailsUi,
    accentColor: Color = Color(0xFF7FE6FF),
    borderColor: Color = Color(0x3D7FE6FF),
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PartyMemberDetailsContent(
            details = details,
            accentColor = accentColor,
            borderColor = borderColor,
            onClose = onDismiss,
            showClose = true,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun PartyMemberDetailsContent(
    details: PartyMemberDetailsUi,
    accentColor: Color,
    borderColor: Color,
    onClose: () -> Unit = {},
    showClose: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF071018),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f))
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Name, Level Badge, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = details.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFC857).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Lv ${details.level}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFC857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (showClose) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Hero Card: Portrait + HP / XP Bars
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF040A10).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val portraitPath = details.portraitPath
                        Image(
                            painter = rememberAssetPainter(portraitPath, painterResource(R.drawable.inventory_icon)),
                            contentDescription = details.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // HP Bar
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "HEALTH (HP)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFF6B6B)
                                    )
                                    Text(
                                        text = details.hpLabel ?: "500 / 500",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFFF5252),
                                    trackColor = Color.White.copy(alpha = 0.12f)
                                )
                            }

                            // XP Bar
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "EXPERIENCE (XP)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFF9D7BFF)
                                    )
                                    Text(
                                        text = details.xpLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { 0.25f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF7C4DFF),
                                    trackColor = Color.White.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }

                // Primary Attributes Grid
                if (details.primaryStats.isNotEmpty()) {
                    StatSectionCard(
                        title = "PRIMARY ATTRIBUTES",
                        stats = details.primaryStats,
                        accentColor = accentColor,
                        borderColor = borderColor
                    )
                }

                // Combat Stats Grid
                if (details.combatStats.isNotEmpty()) {
                    StatSectionCard(
                        title = "COMBAT STATS",
                        stats = details.combatStats,
                        accentColor = Color(0xFFFFC857),
                        borderColor = borderColor
                    )
                }

                // Unlocked Skills List
                if (details.unlockedSkills.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF040A10).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "UNLOCKED TECHNIQUES & SKILLS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = accentColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                details.unlockedSkills.forEach { skill ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFC857),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = skill,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                color = Color.White
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

@Composable
private fun StatSectionCard(
    title: String,
    stats: List<CharacterStatValueUi>,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF040A10).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = accentColor
            )
            // 2-column stats row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                stats.chunked(2).forEach { rowStats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowStats.forEach { stat ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.18f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stat.label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        text = stat.value,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        if (rowStats.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
