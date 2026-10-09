package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.feature.combat.viewmodel.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

internal enum class DesktopCueKind { HIT, HEAL, STATUS, SUPPORT, BREAK, TELEGRAPH, KNOCKOUT }

internal data class DesktopCombatCue(
    val token: Long, val targetId: String, val label: String, val color: Color,
    val style: AttackLungeStyle? = null, val strong: Boolean = false, val kind: DesktopCueKind = DesktopCueKind.HIT
)

internal fun combatElementColor(element: String?): Color = when (element?.lowercase()) {
    "burn", "fire" -> Color(0xFFFF9B68)
    "freeze", "ice" -> Color(0xFF8DDFFF)
    "shock", "electric" -> Color(0xFFFFDE70)
    "acid" -> Color(0xFFB6ED76)
    "source" -> Color(0xFFD3A1FF)
    else -> Color(0xFFFFBB55)
}

/** Local, event-specific hits; concurrent damage, status and support cues are kept separately. */
@Composable
internal fun DesktopCombatHit(cue: DesktopCombatCue, flashesDisabled: Boolean, modifier: Modifier = Modifier) {
    val phase = remember(cue.token) { Animatable(0f) }
    LaunchedEffect(cue.token) { phase.animateTo(1f, tween(600)) }
    if (cue.kind == DesktopCueKind.HIT && cue.style == null) return
    Canvas(modifier) {
        val t = phase.value
        val color = cue.color.copy(alpha = (1f - t) * if (flashesDisabled) .25f else .8f)
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension * (.1f + .3f * t)
        val stroke = if (cue.strong) 4.dp.toPx() else 2.dp.toPx()
        when (cue.kind) {
            DesktopCueKind.HEAL -> {
                repeat(7) { index ->
                    val x = size.width * ((index + 1) / 8f)
                    val y = size.height * (1f - t) + (index % 3) * 5.dp.toPx()
                    drawLine(color, Offset(x - 4.dp.toPx(), y), Offset(x + 4.dp.toPx(), y), stroke)
                    drawLine(color, Offset(x, y - 4.dp.toPx()), Offset(x, y + 4.dp.toPx()), stroke)
                }
            }
            DesktopCueKind.STATUS -> repeat(8) { index ->
                val angle = index * Math.PI / 4 + t * Math.PI
                drawCircle(color, 3.dp.toPx(), center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius)
            }
            DesktopCueKind.SUPPORT -> {
                drawOval(color, Offset(size.width * .1f, size.height * .75f), androidx.compose.ui.geometry.Size(size.width * .8f, size.height * .15f), style = Stroke(stroke))
                repeat(5) { index -> val x = size.width * (index + 1) / 6
                    drawLine(color, Offset(x, size.height * .8f), Offset(x, size.height * (.8f - t * .6f)), stroke)
                }
            }
            DesktopCueKind.BREAK -> repeat(12) { index ->
                val angle = index * Math.PI / 6
                val vector = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(color, center + vector * radius, center + vector * (radius + 12.dp.toPx()), stroke)
            }
            DesktopCueKind.TELEGRAPH -> {
                drawCircle(color, size.minDimension * .38f, center, style = Stroke(stroke))
                drawLine(color, center - Offset(0f, 12.dp.toPx()), center + Offset(0f, 3.dp.toPx()), stroke * 2)
                drawCircle(color, 2.dp.toPx(), center + Offset(0f, 10.dp.toPx()))
            }
            DesktopCueKind.KNOCKOUT -> repeat(12) { index ->
                val angle = index * Math.PI / 6 + t
                drawCircle(color, (1f - t) * 4.dp.toPx(), center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius)
            }
            DesktopCueKind.HIT -> when (cue.style) {
            AttackLungeStyle.MELEE -> {
                drawLine(color, center - Offset(radius, radius), center + Offset(radius, radius), stroke)
                drawLine(color, center - Offset(-radius, radius), center + Offset(-radius, radius), stroke)
            }
            AttackLungeStyle.RANGED -> {
                drawCircle(color, radius, center, style = Stroke(stroke))
                repeat(8) { index ->
                    val angle = index * Math.PI / 4
                    val vector = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(color, center + vector * radius, center + vector * (radius + 14.dp.toPx()), stroke)
                }
            }
            else -> {
                drawCircle(color, radius, center, style = Stroke(stroke))
                drawCircle(color.copy(alpha = color.alpha * .5f), radius * .7f, center, style = Stroke(stroke))
            }
            }
        }
    }
}

@Composable
internal fun DesktopTimedCombatPrompt(prompt: CombatController.TimedPromptState, onTap: () -> Unit) {
    var remaining by remember(prompt.id) { mutableFloatStateOf(1f) }
    LaunchedEffect(prompt.id) {
        do {
            val elapsed = System.nanoTime() / 1_000_000 - prompt.startedAt
            remaining = (1f - elapsed.toFloat() / prompt.durationMillis.coerceAtLeast(1)).coerceIn(0f, 1f)
            delay(16)
        } while (remaining > 0)
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(onClick = onTap),
        contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF07111A), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
            Column(Modifier.widthIn(min = 300.dp, max = 480.dp).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(prompt.message, style = MaterialTheme.typography.titleLarge, color = Color.White)
                LinearProgressIndicator(progress = { remaining }, Modifier.fillMaxWidth().height(8.dp))
                Text("Tap", color = Color.White)
            }
        }
    }
}

@Composable
internal fun DesktopCombatReadiness(members: List<CombatantState>, meters: Map<String, Float>, actorId: String?,
    onSelect: (String) -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().background(Color(0xE807111A)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        members.filter { it.isAlive && it.combatant.side != com.example.starborn.domain.combat.CombatSide.ENEMY }.forEach { member ->
            val id = member.combatant.id
            val meter = (meters[id] ?: 0f).coerceIn(0f, 1f)
            Surface(Modifier.weight(1f).clickable(enabled = enabled && meter >= .999f) { onSelect(id) },
                color = if (id == actorId) Color(0xFF304335) else Color(0xFF12212D),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${member.combatant.name} - ${if (id == actorId) "ACTING" else if (meter >= .999f) "READY" else "${(meter * 100).toInt()}%"}",
                        style = MaterialTheme.typography.labelMedium, color = Color.White)
                    LinearProgressIndicator(progress = { meter }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
