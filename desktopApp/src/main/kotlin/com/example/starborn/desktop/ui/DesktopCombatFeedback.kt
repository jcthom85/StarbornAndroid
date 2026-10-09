package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.feature.combat.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

internal enum class DesktopCueKind { HIT, HEAL, STATUS, SUPPORT, BREAK, TELEGRAPH, KNOCKOUT }

internal data class DesktopCombatCue(
    val token: Long,
    val targetId: String,
    val label: String,
    val color: Color,
    val style: AttackLungeStyle? = null,
    val strong: Boolean = false,
    val kind: DesktopCueKind = DesktopCueKind.HIT,
    val amount: Int? = null,
    val critical: Boolean = false,
    val isWeakness: Boolean = false,
    val isBrokenBonus: Boolean = false,
    val isGuardBreak: Boolean = false,
    val element: String? = null
)

internal fun combatElementColor(element: String?): Color = when (element?.lowercase()) {
    "burn", "fire" -> Color(0xFFFF9B68)
    "freeze", "ice" -> Color(0xFF8DDFFF)
    "shock", "electric" -> Color(0xFFFFDE70)
    "acid" -> Color(0xFFB6ED76)
    "source" -> Color(0xFFD3A1FF)
    else -> Color(0xFFFFBB55)
}

/** Pulsing energy shockwave and atmospheric radial glow when an ally is ready to act. */
@Composable
internal fun DesktopReadyAura(
    color: Color = Color(0xFF4EE7FF),
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "desktop_ready_aura")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_pulse"
    )
    val pingAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pingAnim.snapTo(0f)
        pingAnim.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Shockwave ping
        if (pingAnim.value < 1f) {
            val pingScale = 1f + 0.35f * pingAnim.value
            val pingAlpha = (1f - pingAnim.value) * 0.8f
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = pingScale
                scaleY = pingScale * 0.45f
                alpha = pingAlpha
            }) {
                drawOval(
                    color = color,
                    topLeft = Offset(size.width * 0.1f, size.height * 0.45f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.5f),
                    style = Stroke(2.dp.toPx())
                )
            }
        }

        // Atmospheric radial glow
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
            scaleX = pulse
            scaleY = pulse * 0.45f
        }) {
            val radius = size.minDimension * 0.45f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.42f),
                        color.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.7f),
                    radius = radius * 1.2f
                ),
                topLeft = Offset(size.width * 0.1f, size.height * 0.45f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.5f)
            )
        }
    }
}

/** Floating parabolic ballistic damage text with scale bounce, rotation, sparkles, and callouts. */
@Composable
internal fun DesktopDamageBubble(
    cue: DesktopCombatCue,
    modifier: Modifier = Modifier
) {
    val progress = remember(cue.token) { Animatable(0f) }
    val scaleAnim = remember(cue.token) { Animatable(1f) }
    val isHealing = cue.kind == DesktopCueKind.HEAL
    val isMiss = cue.label.contains("MISS", ignoreCase = true)
    val isCrit = cue.critical
    val seed = (cue.token xor (cue.token ushr 16)).toInt()
    val driftX = remember(cue.token) { (((seed % 100) / 100f) - 0.5f) * if (isMiss) 20f else 48f }
    val tilt = remember(cue.token) {
        if (isHealing || isMiss) 0f
        else (((seed % 73) / 73f) - 0.5f) * if (isCrit) 14f else 8f
    }

    LaunchedEffect(cue.token) {
        progress.snapTo(0f)
        val initialScale = when {
            isCrit -> 1.75f
            isMiss -> 1.15f
            isHealing -> 1.25f
            else -> 1.38f
        }
        scaleAnim.snapTo(initialScale)
        launch {
            scaleAnim.animateTo(1.0f, tween(durationMillis = 180, easing = EaseOutBack))
        }
        launch {
            progress.animateTo(1f, tween(durationMillis = 750, easing = LinearEasing))
        }
    }

    val t = progress.value
    val v0 = when {
        isCrit -> -160f
        isMiss -> -80f
        isHealing -> -100f
        else -> -130f
    }
    val gravity = when {
        isCrit -> 220f
        isMiss -> 110f
        isHealing -> 140f
        else -> 180f
    }
    val currentY = v0 * t + 0.5f * gravity * (t * t)
    val currentX = driftX * (1f - (1f - t) * (1f - t))
    val currentAlpha = if (t < 0.45f) 1f else (1f - (t - 0.45f) / 0.55f).coerceIn(0f, 1f)

    val headline = when {
        isMiss -> "MISS!"
        cue.amount != null -> if (isHealing) "+${cue.amount}" else "${cue.amount}"
        else -> cue.label.substringBefore(" /")
    }

    val topColor = cue.color
    val bottomColor = if (isCrit) Color(0xFFFF5722) else if (isHealing) Color(0xFF00C853) else cue.color.copy(alpha = 0.8f)

    Box(
        modifier = modifier.graphicsLayer {
            translationY = currentY
            translationX = currentX
            alpha = currentAlpha
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            rotationZ = tilt
        },
        contentAlignment = Alignment.Center
    ) {
        // Sparkle flares on crit or weakness
        if ((cue.critical || cue.isWeakness || cue.isBrokenBonus) && t < 0.38f) {
            val sparkProgress = t / 0.38f
            val sparkAlpha = (1f - sparkProgress) * 0.9f
            Canvas(modifier = Modifier.size(68.dp)) {
                val sparkDist = size.minDimension * 0.45f * sparkProgress
                val sparkColor = if (cue.isWeakness) Color(0xFFFFD54F) else Color(0xFFFFCC80)
                for (i in 0 until 6) {
                    val angle = (i * (Math.PI.toFloat() / 3f) + ((seed % 100) * 0.05f))
                    val sx = center.x + cos(angle.toDouble()).toFloat() * sparkDist
                    val sy = center.y + sin(angle.toDouble()).toFloat() * sparkDist
                    drawCircle(sparkColor.copy(alpha = sparkAlpha), (3.5f * (1f - sparkProgress)).coerceAtLeast(1f), Offset(sx, sy))
                    drawCircle(Color.White.copy(alpha = sparkAlpha), (1.8f * (1f - sparkProgress)).coerceAtLeast(0.5f), Offset(sx, sy))
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val calloutText = when {
                cue.isGuardBreak -> "GUARD BREAK!"
                cue.isBrokenBonus -> "BROKEN +25%"
                cue.isWeakness -> "WEAKNESS!"
                cue.critical -> "CRITICAL!"
                else -> null
            }
            if (calloutText != null) {
                val calloutColor = when {
                    cue.isGuardBreak -> Color(0xFF64B5F6)
                    cue.isBrokenBonus -> Color(0xFFFF8A80)
                    cue.isWeakness -> Color(0xFFFFD54F)
                    cue.critical -> Color(0xFFFF8A65)
                    else -> Color.White
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, calloutColor.copy(alpha = 0.85f))
                ) {
                    Text(
                        text = calloutText,
                        color = calloutColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Box(contentAlignment = Alignment.Center) {
                val outlineColor = Color.Black.copy(alpha = 0.85f)
                val outlineOffset = 1.5.dp
                val textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (cue.critical) 28.sp else 24.sp,
                    fontStyle = if (isHealing) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = FontWeight.Black
                )

                // 8-direction outline
                listOf(
                    Offset(-1f, -1f), Offset(1f, -1f),
                    Offset(-1f, 1f), Offset(1f, 1f),
                    Offset(0f, -1.2f), Offset(0f, 1.2f),
                    Offset(-1.2f, 0f), Offset(1.2f, 0f)
                ).forEach { offset ->
                    Text(
                        text = headline,
                        style = textStyle,
                        color = outlineColor,
                        modifier = Modifier.offset(
                            x = (offset.x * outlineOffset.value).dp,
                            y = (offset.y * outlineOffset.value).dp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }

                // Main glowing text
                Text(
                    text = headline,
                    style = textStyle.copy(
                        shadow = Shadow(
                            color = bottomColor.copy(alpha = 0.9f),
                            offset = Offset(0f, 4f),
                            blurRadius = 12f
                        )
                    ),
                    color = topColor,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
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
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).desktopPointerHover().clickable(onClick = onTap),
        contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF07111A), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.widthIn(min = 300.dp, max = 480.dp).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(prompt.message, style = MaterialTheme.typography.titleLarge, color = Color.White)
                LinearProgressIndicator(progress = { remaining }, Modifier.fillMaxWidth().height(8.dp))
                Text("CLICK OR SPACEBAR", color = Color(0xFF63E6FF), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            val isReady = meter >= .999f
            Surface(Modifier.weight(1f).clickable(enabled = enabled && isReady) { onSelect(id) },
                color = if (id == actorId) Color(0xFF304335) else if (isReady) Color(0xFF13364A) else Color(0xFF12212D),
                border = if (isReady) BorderStroke(1.2.dp, Color(0xFF4EE7FF).copy(alpha = 0.75f)) else null,
                shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${member.combatant.name} - ${if (id == actorId) "ACTING" else if (isReady) "READY" else "${(meter * 100).toInt()}%"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isReady) Color(0xFF63E6FF) else Color.White)
                    LinearProgressIndicator(
                        progress = { meter },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isReady) Color(0xFF4EE7FF) else Color(0xFF2F9BE8)
                    )
                }
            }
        }
    }
}
