package com.example.starborn.desktop.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.feature.combat.presentation.CombatTutorialCopy
import com.example.starborn.feature.combat.viewmodel.CombatTutorialState

/** Android's tutorial framing, sized for a desktop battlefield. Shared copy and gating remain canonical. */
@Composable
internal fun DesktopCombatTutorialOverlay(
    tutorial: CombatTutorialState,
    accent: Color,
    highContrast: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val panel = if (highContrast) Color.Black else Color(0xF0061018)
    val border = if (highContrast) Color.White else accent.copy(alpha = .72f)
    if (tutorial.showsModal) {
        Dialog(onDismissRequest = {}, properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false
        )) {
            Surface(Modifier.widthIn(max = 540.dp).fillMaxWidth().padding(20.dp),
                shape = RoundedCornerShape(16.dp), color = panel,
                border = BorderStroke(1.dp, border), shadowElevation = 12.dp) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(CombatTutorialCopy.title(tutorial), style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(CombatTutorialCopy.message(tutorial).adaptInputVocabularyForDesktop(), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .88f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (tutorial.canSkip) TextButton(onClick = onSkip, modifier = Modifier.desktopPointerHover()) { Text("Skip Training", color = Color.White.copy(alpha = .72f)) }
                        Button(onClick = onContinue, modifier = Modifier.desktopPointerHover(), colors = ButtonDefaults.buttonColors(
                            containerColor = accent, contentColor = Color(0xFF041018))) {
                            Text(CombatTutorialCopy.continueLabel(tutorial), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 480.dp).padding(horizontal = 20.dp),
                shape = RoundedCornerShape(999.dp), color = panel,
                border = BorderStroke(1.2.dp, border), shadowElevation = 10.dp) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(999.dp)))
                    Text(CombatTutorialCopy.message(tutorial).adaptInputVocabularyForDesktop(), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
internal fun DesktopCombatActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.ArrowForward,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier.desktopPointerHover(enabled).heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) .6f else .15f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0B1820), contentColor = Color.White,
            disabledContainerColor = Color(0xFF081017), disabledContentColor = Color(0xFF60717B)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .3f))
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/** Android-parity sci-fi styled outcome banner for Victory, Defeat, and Retreat. */
@Composable
internal fun DesktopCombatOutcomeOverlay(
    outcome: CombatOutcome,
    party: List<CombatantState>,
    services: DesktopAppServices,
    onContinue: (() -> Unit)? = null
) {
    val isVictory = outcome is CombatOutcome.Victory
    val isRetreat = outcome is CombatOutcome.Retreat
    val accentColor = when {
        isVictory -> Color(0xFFFF922B)
        isRetreat -> Color(0xFF7CD8FF)
        else -> Color(0xFFE65D5D)
    }
    val eyebrow = when {
        isVictory -> "Combat Result"
        isRetreat -> "Tactical Exit"
        else -> "Party Status"
    }
    val title = when {
        isVictory -> "Victory"
        isRetreat -> "Retreated"
        else -> "Party defeated"
    }
    val subtitle = when {
        isVictory -> "Hostile contact resolved"
        isRetreat -> "Disengaged from combat"
        else -> "The party collapses in defeat"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isVictory) 0.62f else 0.52f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF15100D).copy(alpha = 0.94f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.72f)),
            shadowElevation = 18.dp,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.66f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVictory) Icons.Filled.EmojiEvents else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.2.dp)
                        .background(accentColor.copy(alpha = 0.44f))
                )

                if (!isRetreat) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        party.take(4).forEach { member ->
                            val emoteSuffix = if (isVictory) "cool" else "down"
                            val emotePath = "images/characters/emotes/${member.combatant.id}_${emoteSuffix}.png"
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.36f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isVictory) 96.dp else 84.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = rememberDesktopAssetPainter(emotePath, services.assetProvider),
                                        contentDescription = member.combatant.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = if (isVictory) 1.15f else 1f
                                                scaleY = if (isVictory) 1.15f else 1f
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f))
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                )

                if (onContinue != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.desktopPointerHover(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF041018)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isVictory) "Collect Spoils" else "Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Persistent Party Roster Panel for Desktop Wide-Screen layout. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopCombatPartyRoster(
    party: List<CombatantState>,
    services: DesktopAppServices,
    meters: Map<String, Float>,
    actorId: String?,
    playerPartyDefs: List<com.example.starborn.domain.model.Player>,
    onSelectActor: (String) -> Unit
) {
    DesktopMenuCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PARTY STATUS",
                    color = Color(0xFF91A8B3),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                val readyCount = party.count { it.isAlive && (meters[it.combatant.id] ?: 0f) >= 0.999f }
                if (readyCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF63E6FF).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.5f))
                    ) {
                        Text(
                            "[Tab] Cycle Ready",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color(0xFF63E6FF),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            party.forEach { member ->
                val id = member.combatant.id
                val isActing = id == actorId
                val meter = (meters[id] ?: 0f).coerceIn(0f, 1f)
                val isReady = member.isAlive && meter >= 0.999f
                val playerDef = playerPartyDefs.firstOrNull { it.id == id }
                val iconPath = playerDef?.miniIconPath ?: playerDef?.combatIconPath

                val cardBorder = when {
                    isActing -> BorderStroke(1.2.dp, Color(0xFFFFBB55))
                    isReady -> BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.6f))
                    else -> BorderStroke(1.dp, Color(0xFF22323D).copy(alpha = 0.5f))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActing) Color(0xFF13222B) else Color(0xFF09141B),
                    border = cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isReady && !isActing) Modifier.desktopPointerHover() else Modifier)
                        .clickable(enabled = isReady && !isActing) { onSelectActor(id) }
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Portrait / Mini-Icon
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isActing) Color(0xFFFFBB55).copy(alpha = 0.6f) else Color(0xFF1E313D)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            if (iconPath != null) {
                                Image(
                                    rememberDesktopAssetPainter(iconPath, services.assetProvider),
                                    contentDescription = member.combatant.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        // Info & Bars
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    member.combatant.name,
                                    color = if (member.isAlive) Color.White else Color(0xFF88959E),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                when {
                                    isActing -> Text("ACTING", color = Color(0xFFFFBB55), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    isReady -> Text("READY", color = Color(0xFF63E6FF), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    !member.isAlive -> Text("DOWN", color = Color(0xFFE65D5D), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    else -> Text("${(meter * 100).toInt()}%", color = Color(0xFF819AA8), style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // HP Bar + exact numbers
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { (member.hp.toFloat() / member.combatant.stats.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                    modifier = Modifier.weight(1f).height(4.dp),
                                    color = if (member.hp < member.combatant.stats.maxHp * 0.25f) Color(0xFFFF4D59) else Color(0xFF55E588),
                                    trackColor = Color(0x99070B12)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${member.hp}/${member.combatant.stats.maxHp}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }

                            // ATB & Momentum Row
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { meter },
                                    modifier = Modifier.weight(1f).height(3.dp),
                                    color = Color(0xFF2F9BE8),
                                    trackColor = Color(0x99070B12)
                                )
                                Spacer(Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(3) { pipIdx ->
                                        Box(
                                            Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (member.momentum > pipIdx) Color(0xFF63E6FF) else Color.White.copy(alpha = 0.15f),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            // Active Statuses & Buffs
                            if (member.statusEffects.isNotEmpty() || member.buffs.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    member.statusEffects.forEach { DesktopCombatStatusChip(it, services) }
                                    member.buffs.forEach { DesktopCombatBuffChip(it, services) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

