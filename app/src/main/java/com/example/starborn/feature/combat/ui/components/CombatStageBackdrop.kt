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
    val isLoader = tutorial.tutorialType == com.example.starborn.feature.combat.viewmodel.CombatTutorialType.LOADER_WEAKNESS
    val title = when (tutorial.step) {
        CombatTutorialStep.BRIEF -> "Combat Tutorial"
        CombatTutorialStep.BLOCKED_EXPLANATION -> "Direct Hit: Blocked"
        CombatTutorialStep.SUCCESS -> if (isLoader) "Stability Broken!" else "Guard Broken"
        else -> "Combat Tutorial"
    }
    val message = when (tutorial.step) {
        CombatTutorialStep.BRIEF ->
            if (isLoader)
                "When Nova is ready, tap her to choose an action. Start with a standard Attack and select the Faulted Loader."
            else
                "That trainer eats direct hits. First, test the shield, then break its guard with Hydraulic Kick."
        CombatTutorialStep.SELECT_NOVA_ATTACK -> "Tap Nova when her action is ready."
        CombatTutorialStep.CHOOSE_ATTACK -> "Choose Attack. First, test the shield."
        CombatTutorialStep.TARGET_BASIC_ATTACK -> if (isLoader) "Choose the Faulted Loader." else "Choose the Shield Trainer."
        CombatTutorialStep.AWAIT_BASIC_RESULT -> if (isLoader) "Watch the standard Attack connect." else "Watch how the shield handles a direct hit."
        CombatTutorialStep.BLOCKED_EXPLANATION ->
            if (isLoader)
                "Standard Attacks are always available. The loader's cracked relay is weak to Shock—use Arc Tether to damage its Stability and accelerate cooldowns."
            else
                "The shield reduced the attack to zero. Guard Break strips protection before you commit damage."
        CombatTutorialStep.SELECT_NOVA_SKILL ->
            if (isLoader) "Nova is ready. Tap Nova to choose an action." else "Nova is ready again. Tap Nova to break the guard."
        CombatTutorialStep.CHOOSE_SKILLS ->
            if (isLoader) "Select Abilities." else "Open Abilities to find a guard-breaking move."
        CombatTutorialStep.CHOOSE_HYDRAULIC_KICK ->
            if (isLoader) "Select Arc Tether." else "Use Hydraulic Kick."
        CombatTutorialStep.TARGET_HYDRAULIC_KICK ->
            if (isLoader) "Target the Faulted Loader." else "Choose an enemy for Hydraulic Kick."
        CombatTutorialStep.AWAIT_SHIELD_BREAK ->
            if (isLoader) "Watch the Shock pulse break its stability." else "Watch the guard break."
        CombatTutorialStep.SUCCESS ->
            if (isLoader)
                "Stability broken! Broken targets take 25% more direct damage. Arc Tether is on cooldown—finish it with Attacks."
            else
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
                        if (tutorial.canSkip) {
                            TextButton(onClick = onSkip) {
                                Text("Skip Training", color = Color.White.copy(alpha = 0.72f))
                            }
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
                                    CombatTutorialStep.BRIEF -> if (isLoader) "Engage" else "Start Training"
                                    CombatTutorialStep.BLOCKED_EXPLANATION -> if (isLoader) "Exploit Weakness" else "Break The Guard"
                                    CombatTutorialStep.SUCCESS -> if (isLoader) "Finish Combat" else "Finish The Fight"
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
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(min = 240.dp, max = 480.dp)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(999.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.92f),
                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.85f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accent, RoundedCornerShape(999.dp))
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
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
