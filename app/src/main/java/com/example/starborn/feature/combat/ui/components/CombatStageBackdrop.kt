package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.viewmodel.CombatTutorialState
import com.example.starborn.feature.combat.viewmodel.CombatTutorialStep
import com.example.starborn.ui.theme.themeColor
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CombatEncounterHeader(
    locationTitle: String?,
    statusText: String,
    targetMode: Boolean,
    onCancelTarget: (() -> Unit)?,
    theme: Theme?,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = panel.copy(alpha = if (highContrastMode) 0.94f else 0.70f),
        border = BorderStroke(1.dp, border.copy(alpha = if (highContrastMode) 0.72f else 0.46f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = locationTitle?.uppercase(Locale.getDefault()) ?: "CURRENT AREA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        ),
                        color = accent.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (targetMode) Color(0xFFFFC8B8) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (targetMode && onCancelTarget != null) {
                    TextButton(onClick = onCancelTarget) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF7E78),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
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
                                border.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CombatTutorialOverlay(
    tutorial: CombatTutorialState,
    theme: Theme?,
    highContrastMode: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    val title = when (tutorial.step) {
        CombatTutorialStep.BRIEF -> "Guard Break Training"
        CombatTutorialStep.BLOCKED_EXPLANATION -> "Direct Hit: Blocked"
        CombatTutorialStep.SUCCESS -> "Guard Broken"
        else -> "Training"
    }
    val message = when (tutorial.step) {
        CombatTutorialStep.BRIEF ->
            "That trainer eats direct hits. First, test the shield, then break its guard with Hydraulic Kick."
        CombatTutorialStep.SELECT_NOVA_ATTACK -> "Tap Nova when her action is ready."
        CombatTutorialStep.CHOOSE_ATTACK -> "Choose Attack. First, test the shield."
        CombatTutorialStep.TARGET_BASIC_ATTACK -> "Choose the Acoustic Bulwark."
        CombatTutorialStep.AWAIT_BASIC_RESULT -> "Watch how the shield handles a direct hit."
        CombatTutorialStep.BLOCKED_EXPLANATION ->
            "The shield reduced the attack to zero. Guard Break strips protection before you commit damage."
        CombatTutorialStep.SELECT_NOVA_SKILL -> "Nova is ready again. Tap Nova to break the guard."
        CombatTutorialStep.CHOOSE_SKILLS -> "Open Abilities to find a guard-breaking move."
        CombatTutorialStep.CHOOSE_HYDRAULIC_KICK -> "Use Hydraulic Kick."
        CombatTutorialStep.TARGET_HYDRAULIC_KICK -> "Choose an enemy for Hydraulic Kick."
        CombatTutorialStep.AWAIT_SHIELD_BREAK -> "Watch the guard break."
        CombatTutorialStep.SUCCESS ->
            "Hydraulic Kick stripped the shield. Now finish the fight."
    }
    if (tutorial.showsModal) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
                border = BorderStroke(1.dp, border.copy(alpha = 0.72f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onSkip) {
                            Text("Skip Training", color = Color.White.copy(alpha = 0.72f))
                        }
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(alpha = 0.92f),
                                contentColor = Color(0xFF041018)
                            )
                        ) {
                            Text(
                                text = when (tutorial.step) {
                                    CombatTutorialStep.BRIEF -> "Start Training"
                                    CombatTutorialStep.BLOCKED_EXPLANATION -> "Break The Guard"
                                    CombatTutorialStep.SUCCESS -> "Finish The Fight"
                                    else -> "Continue"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 190.dp),
                shape = RoundedCornerShape(12.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.96f else 0.88f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.62f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BattleStageBackdrop(
    accentColor: Color,
    borderColor: Color,
    panelColor: Color,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val motion = rememberInfiniteTransition(label = "battle_stage_backdrop")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battle_stage_phase"
    )
    Canvas(modifier = modifier) {
        val railAlpha = if (highContrastMode) 0.34f else 0.22f
        val centerY = size.height * 0.47f
        val laneHeight = size.height * 0.18f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    panelColor.copy(alpha = if (highContrastMode) 0.42f else 0.26f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, centerY - laneHeight / 2f),
            size = Size(size.width, laneHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )
        val pulse = 0.55f + 0.45f * sin(phase * 2f * PI).toFloat()
        drawLine(
            color = accentColor.copy(alpha = 0.18f + 0.10f * pulse),
            start = Offset(size.width * 0.08f, centerY),
            end = Offset(size.width * 0.92f, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.12f, size.height * 0.20f),
            end = Offset(size.width * 0.88f, size.height * 0.20f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.12f, size.height * 0.76f),
            end = Offset(size.width * 0.88f, size.height * 0.76f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        val tickCount = 7
        repeat(tickCount) { index ->
            val x = size.width * (0.16f + index * 0.68f / (tickCount - 1).coerceAtLeast(1))
            val alpha = 0.08f + 0.08f * ((phase + index * 0.13f) % 1f)
            drawLine(
                color = accentColor.copy(alpha = alpha),
                start = Offset(x, centerY - laneHeight * 0.28f),
                end = Offset(x, centerY + laneHeight * 0.28f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
