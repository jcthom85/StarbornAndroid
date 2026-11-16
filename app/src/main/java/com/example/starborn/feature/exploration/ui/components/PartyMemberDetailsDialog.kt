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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.R
import com.example.starborn.feature.exploration.viewmodel.CharacterStatValueUi
import com.example.starborn.feature.exploration.viewmodel.PartyMemberDetailsUi
import com.example.starborn.ui.background.rememberAssetPainter

private fun statRoleSubtitle(label: String): String = when (label.trim().lowercase()) {
    "strength", "str" -> "Physical & melee damage scaling"
    "vitality", "vit" -> "Max HP ceiling & physical resilience"
    "agility", "agi" -> "Turn speed, ATB fill & evasion"
    "focus", "foc" -> "Tech/Source power & status potency"
    "luck", "lck" -> "Critical hit chance & bonus yields"
    "attack", "atk" -> "Base weapon & strike damage"
    "defense", "def" -> "Mitigates incoming physical & energy hits"
    "speed", "spd" -> "Determines combat turn frequency"
    "accuracy", "acc" -> "Hit reliability (caps at 100%, offsets blind)"
    "crit", "crit chance", "crit rate" -> "Chance to deal 1.5× critical damage"
    "evasion", "eva" -> "Chance to dodge incoming enemy attacks"
    "stability" -> "Resists stagger & Stability Break in combat"
    "status resistance", "status resist", "resist" -> "Reduces hostile debuff application chance"
    else -> "Core character combat parameter"
}

private fun statDetailedBreakdown(label: String, value: String): String = when (label.trim().lowercase()) {
    "strength", "str" -> "Strength ($value) directly increases basic weapon strikes and physical skill damage."
    "vitality", "vit" -> "Vitality ($value) raises Maximum HP and reduces damage taken from heavy physical blows."
    "agility", "agi" -> "Agility ($value) accelerates ATB gauge recovery and improves evasion against single-target attacks."
    "focus", "foc" -> "Focus ($value) amplifies Source/Tech abilities, healing output, and elemental status application."
    "luck", "lck" -> "Luck ($value) boosts critical strike probability, clean fishing hook windows, and salvage luck."
    else -> "$label ($value): ${statRoleSubtitle(label)}. Modified by equipped weapons, armor, mods, learned skills, and active Well-Fed meals."
}

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
@OptIn(ExperimentalLayoutApi::class)
fun PartyMemberDetailsContent(
    details: PartyMemberDetailsUi,
    accentColor: Color,
    borderColor: Color,
    onClose: () -> Unit = {},
    showClose: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (LocalModernFieldMenu.current && !showClose) {
        var inspectedStat by rememberSaveable(details.id) {
            mutableStateOf(details.primaryStats.firstOrNull()?.label)
        }
        val allStats = remember(details.primaryStats, details.combatStats) {
            details.primaryStats + details.combatStats
        }
        val activeStat = allStats.firstOrNull { it.label == inspectedStat } ?: details.primaryStats.firstOrNull()

        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Vitals & Progression Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = FieldMenuDesign.panel,
                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(FieldMenuDesign.primaryActionFill.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAssetPainter(details.portraitPath, painterResource(R.drawable.inventory_icon)),
                        contentDescription = details.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${details.name} - Level ${details.level}",
                            color = FieldMenuDesign.text,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = details.hpLabel ?: "HP unavailable",
                                color = FieldMenuDesign.mint,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            details.hpProgress?.let { hp ->
                                LinearProgressIndicator(
                                    progress = { hp.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (hp < 0.3f) FieldMenuDesign.coral else FieldMenuDesign.mint,
                                    trackColor = FieldMenuDesign.shell
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = details.xpLabel,
                                color = FieldMenuDesign.textMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress = { details.xpProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = FieldMenuDesign.cyan,
                                trackColor = FieldMenuDesign.shell
                            )
                        }
                    }
                }
            }

            ModernStatSection(
                title = "Base attributes",
                subtitle = "Tap any stat to inspect its combat role",
                stats = details.primaryStats,
                accent = accentColor,
                selectedLabel = activeStat?.label,
                onSelectStat = { inspectedStat = it }
            )

            ModernStatSection(
                title = "Base combat stats",
                subtitle = "Derived offense, defense & resistance telemetry",
                stats = details.combatStats,
                accent = FieldMenuDesign.gold,
                selectedLabel = activeStat?.label,
                onSelectStat = { inspectedStat = it }
            )

            if (details.unlockedSkills.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = FieldMenuDesign.panel,
                    border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unlocked Techniques & Skills",
                            color = FieldMenuDesign.cyan,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            details.unlockedSkills.forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FieldMenuDesign.primaryActionFill,
                                    border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = FieldMenuDesign.gold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = skill,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = FieldMenuDesign.text
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Stat Inspector Footer
            activeStat?.let { stat ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = FieldMenuDesign.elevatedPanel,
                    border = BorderStroke(1.5.dp, FieldMenuDesign.gold.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "◈ ${stat.label.uppercase()}",
                                color = FieldMenuDesign.gold,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = stat.value,
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = statDetailedBreakdown(stat.label, stat.value),
                            color = FieldMenuDesign.textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        return
    }
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (LocalModernFieldMenu.current) FieldMenuDesign.panel else Color(0xFF071018),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (LocalModernFieldMenu.current) 0.12f else 0.65f))
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (LocalModernFieldMenu.current && !showClose) Modifier else Modifier.heightIn(max = 580.dp).verticalScroll(scrollState))
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
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "HEALTH (HP)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFF6B6B)
                                    )
                                    Text(
                                        text = details.hpLabel ?: "HP unavailable",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                if (details.hpProgress != null) LinearProgressIndicator(
                                    progress = { details.hpProgress?.coerceIn(0f, 1f) ?: 0f },
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
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    progress = { details.xpProgress.coerceIn(0f, 1f) },
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
                        title = "BASE ATTRIBUTES",
                        stats = details.primaryStats,
                        accentColor = accentColor,
                        borderColor = borderColor
                    )
                }

                // Combat Stats Grid
                if (details.combatStats.isNotEmpty()) {
                    StatSectionCard(
                        title = "BASE COMBAT STATS",
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
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stat.label,
                                        modifier = Modifier.weight(1f),
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

@Composable
private fun ModernStatSection(
    title: String,
    subtitle: String,
    stats: List<CharacterStatValueUi>,
    accent: Color,
    selectedLabel: String?,
    onSelectStat: (String) -> Unit
) {
    if (stats.isEmpty()) return
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FieldMenuDesign.panel,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = accent,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            val columns = if (largeFont) 1 else 2
            stats.chunked(columns).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowStats.forEach { stat ->
                        val selected = stat.label == selectedLabel
                        Surface(
                            onClick = { onSelectStat(stat.label) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) FieldMenuDesign.elevatedPanel else FieldMenuDesign.deepCard,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) FieldMenuDesign.gold else FieldMenuDesign.subtleBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stat.label,
                                        modifier = Modifier.weight(1f),
                                        color = if (selected) FieldMenuDesign.gold else FieldMenuDesign.text,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stat.value,
                                        color = FieldMenuDesign.text,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = statRoleSubtitle(stat.label),
                                    color = FieldMenuDesign.textMuted,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (rowStats.size < columns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
