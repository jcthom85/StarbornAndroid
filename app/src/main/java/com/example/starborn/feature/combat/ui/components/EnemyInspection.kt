package com.example.starborn.feature.combat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.model.Enemy
import com.example.starborn.feature.combat.presentation.weaknessLabel
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.ui.titleCaseName
import com.example.starborn.ui.background.rememberAssetPainter
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

@OptIn(ExperimentalLayoutApi::class)
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
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    val maxHp = state.combatant.stats.maxHp.coerceAtLeast(1)
    val maxStability = state.combatant.stats.stability.coerceAtLeast(1)
    val isBroken = state.breakTurns > 0
    val effects = statusChipsFor(state.statusEffects, state.buffs)
    val tier = enemy.tier.lowercase(Locale.getDefault())
    val (tierLabel, tierColor) = when (tier) {
        "boss" -> "BOSS THREAT" to Color(0xFFFF8A80)
        "elite", "miniboss" -> "ELITE HOSTILE" to Color(0xFFFFD54F)
        else -> "HOSTILE" to accent
    }
    val spritePath = enemy.portrait?.takeIf { it.isNotBlank() }
        ?: enemy.sprite.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "images/enemies/${enemy.id}_combat.png"
    val enemyPainter = rememberAssetPainter(spritePath, painterResource(R.drawable.main_menu_background))
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.45f to Color(0xFF02070E).copy(alpha = if (highContrastMode) 0.42f else 0.16f),
                    1.0f to Color(0xFF02070E).copy(alpha = if (highContrastMode) 0.84f else 0.64f)
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(16.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.92f),
                border = BorderStroke(1.25.dp, border.copy(alpha = if (highContrastMode) 0.85f else 0.60f)),
                shadowElevation = 12.dp,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 14.dp, top = 16.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: unit thumbnail, classification eyebrow + name, and target cycling.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(11.dp),
                            color = Color(0xFF141C28),
                            border = BorderStroke(1.dp, tierColor.copy(alpha = 0.55f))
                        ) {
                            Image(
                                painter = enemyPainter,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val eyebrow = if (enemyCount > 1) "$tierLabel · ${enemyIndex + 1}/$enemyCount" else tierLabel
                            Text(
                                text = eyebrow,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.7.sp
                                ),
                                color = tierColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = titleCaseName(enemy.name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (enemyCount > 1 && onPreviousEnemy != null && onNextEnemy != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF1E2534),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, border.copy(alpha = 0.45f))
                                ) {
                                    IconButton(
                                        onClick = onPreviousEnemy,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronLeft,
                                            contentDescription = "Previous enemy",
                                            tint = accent
                                        )
                                    }
                                }
                                Surface(
                                    color = Color(0xFF1E2534),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, border.copy(alpha = 0.45f))
                                ) {
                                    IconButton(
                                        onClick = onNextEnemy,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = "Next enemy",
                                            tint = accent
                                        )
                                    }
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
                                    colors = listOf(
                                        accent.copy(alpha = 0.78f),
                                        border.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // 2-Column Vitals Telemetry Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF121A26).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, border.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "HP ${state.hp}/${maxHp}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = CombatNameFont,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    StatBar(
                                        current = state.hp,
                                        max = maxHp,
                                        color = Color(0xFFFF5252),
                                        background = Color.Black.copy(alpha = 0.55f),
                                        height = 7.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Stability ${state.stability}/${maxStability}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = CombatNameFont,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    StatBar(
                                        current = if (isBroken) 0 else state.stability,
                                        max = maxStability,
                                        color = if (isBroken) Color(0xFFE57373) else Color(0xFF7E57C2),
                                        background = Color.Black.copy(alpha = 0.55f),
                                        height = 7.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            if (isBroken) {
                                Text(
                                    text = "Broken · ${state.breakTurns} turns remaining",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFFFAB91)
                                )
                            }
                        }
                    }

                    // Weaknesses & Active Effects Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = if (highContrastMode) 0.08f else 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, border.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Weaknesses",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = accent.copy(alpha = 0.92f)
                            )
                            if (knownWeaknesses.isEmpty()) {
                                Text(
                                    text = "Undiscovered — strike elements or Scan to reveal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB8C9D4)
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    knownWeaknesses.sorted().forEach { element ->
                                        val tint = weaknessTintColor(element)
                                        Surface(
                                            color = tint.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, tint.copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                text = weaknessLabel(element),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = tint
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                            if (effects.isEmpty()) {
                                Text(
                                    text = "Active effects: None",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB8C9D4)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Active Effects",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = CombatNameFont,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = accent.copy(alpha = 0.92f)
                                    )
                                    effects.forEach { chip ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = chip.tint.copy(alpha = 0.85f),
                                                contentColor = Color.White,
                                                shape = CircleShape,
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = chip.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
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

                    enemy.flavor.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 16.sp
                            ),
                            color = Color(0xFFB8C9D4),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    // Single tactical Close action matching CommandPalette / SkillsDialog style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (enemyCount > 1) "Tap another enemy on field to switch" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = Color(0xFF1E2534),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, border.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clickable(onClick = onClose)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = CombatNameFont,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
            CommandPaletteMarkers(
                color = accent,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

internal fun weaknessTintColor(element: String): Color =
    when (element.trim().lowercase(Locale.getDefault())) {
        "burn", "fire" -> Color(0xFFFF8A65)
        "shock", "lightning" -> Color(0xFF7BE4FF)
        "freeze", "ice", "cryo" -> Color(0xFF80DEEA)
        "acid", "poison", "corrosion" -> Color(0xFFAED581)
        "source", "harmonic", "void" -> Color(0xFFCE93D8)
        else -> Color(0xFFFFD54F)
    }

