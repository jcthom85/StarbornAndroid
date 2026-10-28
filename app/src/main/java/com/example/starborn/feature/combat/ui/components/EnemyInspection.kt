package com.example.starborn.feature.combat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.model.Enemy
import com.example.starborn.feature.combat.presentation.weaknessLabel
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.ui.titleCaseName
import com.example.starborn.ui.theme.themeColor
import java.util.Locale

@Composable
fun EnemyOverview(
    enemy: Enemy,
    state: CombatantState,
    knownWeaknesses: Set<String>,
    inspecting: Boolean,
    canInspect: Boolean,
    onInspect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A0F18).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color(0xFF5CCBE8).copy(alpha = 0.45f))
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(enemy.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                if (inspecting) Text("Paused", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7BE4FF))
            }
            Text(
                "HP ${state.hp}/${state.combatant.stats.maxHp}  •  Stability ${state.stability}/${state.combatant.stats.stability}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                if (knownWeaknesses.isEmpty()) "Weaknesses: unknown" else
                    "Known weaknesses: " + knownWeaknesses.sorted().joinToString(" • ", transform = ::weaknessLabel),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onInspect, enabled = canInspect, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Inspect")
                }
                if (inspecting) {
                    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Close overview")
                    }
                }
            }
        }
    }
}

@Composable
fun EnemyInspectionSheet(
    enemy: Enemy,
    state: CombatantState,
    knownWeaknesses: Set<String>,
    onClose: () -> Unit,
    theme: Theme? = null,
    highContrastMode: Boolean = false,
    enemyIndex: Int = 0,
    enemyCount: Int = 1,
    onPreviousEnemy: (() -> Unit)? = null,
    onNextEnemy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF08111B))
    val maxHp = state.combatant.stats.maxHp.coerceAtLeast(1)
    val maxStability = state.combatant.stats.stability.coerceAtLeast(1)
    val isBroken = state.breakTurns > 0
    val effects = statusChipsFor(state.statusEffects, state.buffs)
    val tierBadge = when (enemy.tier.lowercase(Locale.getDefault())) {
        "boss" -> "BOSS THREAT"
        "elite", "miniboss" -> "ELITE HOSTILE"
        else -> "HOSTILE UNIT"
    }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(20.dp),
            color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.95f),
            border = BorderStroke(1.25.dp, border.copy(alpha = 0.65f)),
            shadowElevation = 16.dp,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: threat classification and explicit close.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "TACTICAL SCAN · $tierBadge",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = CombatNameFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = accent
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close tactical scan",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Enemy Name + Cycle Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleCaseName(enemy.name),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (enemyCount > 1 && onPreviousEnemy != null && onNextEnemy != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(onClick = onPreviousEnemy, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = "Previous enemy",
                                    tint = accent
                                )
                            }
                            Text(
                                text = "${enemyIndex + 1}/$enemyCount",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = CombatNameFont,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                            IconButton(onClick = onNextEnemy, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Next enemy",
                                    tint = accent
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accent.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )

                // Vitals & Stability Panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HP ${state.hp}/${maxHp}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = CombatNameFont,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                            StatBar(
                                current = state.hp,
                                max = maxHp,
                                color = Color(0xFFFF5252),
                                background = Color.Black.copy(alpha = 0.55f),
                                height = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Stability ${state.stability}/${maxStability}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = CombatNameFont,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                            StatBar(
                                current = if (isBroken) 0 else state.stability,
                                max = maxStability,
                                color = if (isBroken) Color(0xFFE57373) else Color(0xFF7E57C2),
                                background = Color.Black.copy(alpha = 0.55f),
                                height = 7.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isBroken) Text("Broken · ${state.breakTurns} turns remaining",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFAB91))
                        }
                    }
                }

                // Discovered Weaknesses Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Weaknesses & Resistances",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = CombatNameFont,
                            color = accent
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (knownWeaknesses.isEmpty()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "[ ? ] Undiscovered",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            knownWeaknesses.sorted().forEach { element ->
                                Surface(
                                    color = Color(0xFFFFD54F).copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        text = weaknessLabel(element),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFE082)
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (knownWeaknesses.size < 2) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = "[ ? ]",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Statuses & Buffs Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Active Effects",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = CombatNameFont,
                            color = accent
                        )
                    )
                    if (effects.isEmpty()) {
                        Text(
                            text = "None",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            effects.forEach { chip ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, chip.tint.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = chip.tint.copy(alpha = 0.85f),
                                            contentColor = Color.White,
                                            shape = CircleShape,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = chip.icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = chip.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.92f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.22f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

