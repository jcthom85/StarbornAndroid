package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.combat.ActiveBuff
import com.example.starborn.domain.combat.StatusEffect
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos

data class StatusChip(
    val icon: ImageVector,
    val tint: Color,
    val turns: Int,
    val value: Int? = null
)

@Composable
fun EnemyStatusRail(
    statuses: List<StatusEffect>,
    buffs: List<ActiveBuff>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    val entries = statusChipsFor(statuses, buffs)
    if (entries.isEmpty()) return
    val visible = entries.take(maxVisible.coerceAtLeast(1))
    val overflow = entries.size - visible.size
    Surface(
        color = Color(0xFF070A10).copy(alpha = 0.74f),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visible.forEach { chip ->
                EnemyStatusPip(chip)
            }
            if (overflow > 0) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.height(22.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$overflow",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnemyStatusPip(chip: StatusChip) {
    Box(modifier = Modifier.size(24.dp)) {
        Surface(
            color = chip.tint.copy(alpha = 0.88f),
            contentColor = Color.White,
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
            modifier = Modifier.matchParentSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = chip.icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Surface(
            color = Color(0xFF05070B).copy(alpha = 0.96f),
            contentColor = Color.White,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.28f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 2.dp)
                .height(12.dp)
                .widthIn(min = 12.dp)
        ) {
            Text(
                text = statusChipLabel(chip),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun StatusBadges(statuses: List<StatusEffect>, buffs: List<ActiveBuff>) {
    val entries = statusChipsFor(statuses, buffs)
    if (entries.isEmpty()) return
    LazyRow(
        modifier = Modifier.widthIn(max = 1200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(entries) { chip ->
            Surface(
                color = chip.tint.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.height(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = chip.icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = statusChipLabel(chip),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

fun statusChipsFor(statuses: List<StatusEffect>, buffs: List<ActiveBuff>): List<StatusChip> =
    buildList {
        statuses.forEach { statusEffect ->
            add(
                StatusChip(
                    icon = iconForStatus(statusEffect.id),
                    tint = colorForStatus(statusEffect.id),
                    turns = statusEffect.remainingTurns
                )
            )
        }
        buffs.forEach { buff ->
            val isPositive = buff.effect.value >= 0
            val stat = buff.effect.stat.lowercase(Locale.getDefault())
            add(
                StatusChip(
                    icon = iconForStat(stat),
                    tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    turns = buff.remainingTurns,
                    value = buff.effect.value
                )
            )
        }
    }

fun statusChipLabel(chip: StatusChip): String {
    val value = chip.value
    return if (value != null) {
        val magnitude = abs(value)
        val sign = when {
            value > 0 -> "+"
            value < 0 -> "-"
            else -> ""
        }
        if (magnitude > 9) "${sign}9+" else "$sign$magnitude"
    } else {
        val turns = chip.turns.coerceAtLeast(0)
        if (turns > 9) "9+" else turns.toString()
    }
}

fun iconForStatus(statusId: String): ImageVector {
    return when (statusId.lowercase(Locale.getDefault())) {
        "burn", "meltdown" -> Icons.Rounded.Whatshot
        "shock", "static" -> Icons.Rounded.Bolt
        "freeze", "frozen" -> Icons.Rounded.AcUnit
        "wet", "water" -> Icons.Rounded.WaterDrop
        "acid", "poison", "erosion", "bleed" -> Icons.Rounded.WaterDrop
        "stun", "stagger" -> Icons.Rounded.Star
        "blind" -> Icons.Rounded.VisibilityOff
        "shield", "guard", "invulnerable" -> Icons.Rounded.Shield
        "weak", "brittle", "exposed", "discord" -> Icons.Rounded.BrokenImage
        "regen", "heal" -> Icons.Rounded.AutoAwesome
        "jammed", "silence" -> Icons.Rounded.VisibilityOff
        "overdrive", "charged" -> Icons.Rounded.Bolt
        else -> Icons.Rounded.Warning
    }
}

fun iconForStat(stat: String): ImageVector {
    return when (stat) {
        "strength", "str", "atk" -> Icons.Rounded.Restaurant
        "vitality", "vit", "def" -> Icons.Rounded.Shield
        "agility", "agi", "spd", "speed" -> Icons.Rounded.AutoAwesome
        "focus", "int", "psi" -> Icons.Rounded.Bolt
        else -> Icons.Rounded.AutoAwesome
    }
}

fun colorForStatus(statusId: String): Color {
    return when (statusId.lowercase(Locale.getDefault())) {
        "burn", "meltdown" -> Color(0xFFFF5722)
        "shock", "static", "overdrive" -> Color(0xFFFFC107)
        "freeze", "frozen" -> Color(0xFF03A9F4)
        "acid", "poison", "erosion" -> Color(0xFF8BC34A)
        "bleed" -> Color(0xFFE91E63)
        "stun", "stagger" -> Color(0xFF9C27B0)
        "blind" -> Color(0xFF607D8B)
        "shield", "guard", "invulnerable", "regen" -> Color(0xFF4CAF50)
        "weak", "brittle", "exposed" -> Color(0xFF795548)
        else -> Color(0xFF9E9E9E)
    }
}

@Composable
fun ShieldFieldOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF42A5F5)
) {
    val transition = rememberInfiniteTransition(label = "shield_field")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = -1.25f,
        targetValue = -1.25f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                -1.25f at 0
                -1.25f at 340
                2.25f at 1880
                2.25f at 2600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_sweep"
    )
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val maskBlend = BlendMode.SrcAtop
        val intensity = 0.65f + 0.35f * pulse
        val center = Offset(size.width * 0.5f, size.height * 0.35f)
        val radius = size.maxDimension * 0.9f
        val base = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.20f * intensity),
                color.copy(alpha = 0.10f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
        drawRect(brush = base, blendMode = maskBlend)

        val bandWidth = size.minDimension * 0.55f
        val x = size.width * sweep
        val shimmer = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.08f + 0.10f * pulse),
                Color.Transparent
            ),
            start = Offset(x - bandWidth, 0f),
            end = Offset(x + bandWidth, size.height)
        )
        drawRect(brush = shimmer, blendMode = maskBlend)
    }
}

@Composable
fun BrokenFieldOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFB388FF)
) {
    val transition = rememberInfiniteTransition(label = "broken_field")
    val pulse by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "broken_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "broken_sweep"
    )
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val maskBlend = BlendMode.SrcAtop
        val intensity = 0.6f + 0.4f * pulse
        val center = Offset(size.width * 0.5f, size.height * 0.6f)
        val radius = size.maxDimension * 0.95f
        val base = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.16f * intensity),
                color.copy(alpha = 0.08f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
        drawRect(brush = base, blendMode = maskBlend)

        val bandWidth = size.minDimension * 0.45f
        val looped = (sin(sweep) + 1f) * 0.5f
        val x = (size.width + bandWidth) * looped - bandWidth
        val fissure = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.22f + 0.18f * pulse),
                Color.Transparent
            ),
            start = Offset(x - bandWidth, 0f),
            end = Offset(x + bandWidth, size.height)
        )
        drawRect(brush = fissure, blendMode = maskBlend)

        val crackStroke = size.minDimension * 0.024f
        val crackColor = Color(0xFFF3E5F5).copy(alpha = 0.48f + 0.28f * pulse)
        val crackHighlight = Color.White.copy(alpha = 0.28f + 0.2f * pulse)

        val shardCenter = Offset(size.width * 0.46f, size.height * 0.44f)
        val mainStart = Offset(size.width * 0.12f, size.height * 0.08f)
        val mainEnd = Offset(size.width * 0.9f, size.height * 0.86f)
        drawLine(color = crackColor, start = mainStart, end = shardCenter, strokeWidth = crackStroke, blendMode = maskBlend)
        drawLine(color = crackColor, start = shardCenter, end = mainEnd, strokeWidth = crackStroke, blendMode = maskBlend)
        drawLine(color = crackHighlight, start = mainStart, end = shardCenter, strokeWidth = crackStroke * 0.55f, blendMode = maskBlend)
        drawLine(color = crackHighlight, start = shardCenter, end = mainEnd, strokeWidth = crackStroke * 0.55f, blendMode = maskBlend)

        val branchStroke = crackStroke * 0.7f
        val branches = listOf(
            shardCenter to shardCenter + Offset(size.width * 0.26f, size.height * -0.14f),
            shardCenter to shardCenter + Offset(size.width * -0.18f, size.height * 0.26f),
            shardCenter to shardCenter + Offset(size.width * 0.12f, size.height * 0.3f)
        )
        branches.forEach { (start, end) ->
            drawLine(color = crackColor, start = start, end = end, strokeWidth = branchStroke, blendMode = maskBlend)
            drawLine(color = crackHighlight, start = start, end = end, strokeWidth = branchStroke * 0.55f, blendMode = maskBlend)
        }

        val burstRadius = size.minDimension * 0.08f
        val burstCount = 7
        repeat(burstCount) { idx ->
            val angle = (idx / burstCount.toFloat()) * 360f + 12f
            val length = burstRadius * (0.7f + 0.5f * (idx % 2))
            val rad = Math.toRadians(angle.toDouble())
            val end = Offset(
                shardCenter.x + (cos(rad) * length).toFloat(),
                shardCenter.y + (sin(rad) * length).toFloat()
            )
            drawLine(color = crackHighlight, start = shardCenter, end = end, strokeWidth = crackStroke * 0.45f, blendMode = maskBlend)
        }

        val glintRadius = size.minDimension * 0.18f
        val glintBrush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.12f + 0.08f * pulse),
                Color.Transparent,
                Color.Transparent
            ),
            center = shardCenter + Offset(glintRadius * 0.2f, -glintRadius * 0.2f),
            radius = glintRadius
        )
        drawCircle(
            brush = glintBrush,
            radius = glintRadius,
            center = shardCenter + Offset(glintRadius * 0.2f, -glintRadius * 0.2f),
            blendMode = maskBlend
        )
    }
}
