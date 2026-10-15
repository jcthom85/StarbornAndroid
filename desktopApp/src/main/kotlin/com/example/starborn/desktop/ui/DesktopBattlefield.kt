package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.feature.combat.viewmodel.AttackLungeStyle
import com.example.starborn.data.local.UserSettings

/** Sprites occupy the scene; status stays immediately below the corresponding target. */
@Composable
internal fun DesktopBattleFormation(
    members: List<CombatantState>, services: DesktopAppServices, modifier: Modifier,
    enemySide: Boolean, sprite: (String) -> String?, selectedId: String?,
    validTargets: Set<String>, targeting: Boolean, meters: Map<String, Float>,
    intents: Map<String, String>, feedback: Map<String, String>,
    cues: List<DesktopCombatCue>, lungeStyle: AttackLungeStyle,
    lungeActor: String?, lungeToken: Long, missActor: String?, missToken: Long,
    battleHeight: androidx.compose.ui.unit.Dp = 800.dp, opposingCount: Int = 0,
    choosingId: String? = null, executingId: String? = null,
    enemySize: (String) -> String = { "medium" },
    enemyWidth: (String) -> Float = { 1f },
    onHover: (String?) -> Unit = {},
    onSelect: (String) -> Unit
) {
    val settings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    BoxWithConstraints(modifier) {
        val placements = planBattleFormation(
            members.map { FormationMember(it.combatant.id, enemySize(it.combatant.id), enemyWidth(it.combatant.id)) },
            maxWidth.value, maxHeight.value, enemySide)
        placements.forEach { placement ->
            val member = members.first { it.combatant.id == placement.id }
            val id = member.combatant.id
            key(id) {
                Box(Modifier.offset(x = placement.x.dp, y = placement.y.dp).width(placement.width.dp)) {
                    DesktopBattleFigure(member, sprite(id), services, enemySide,
                        selectedId == id, id in validTargets, targeting, meters[id], intents[id], feedback[id],
                        placement.spriteHeight.dp, placement.width.dp,
                        if (lungeActor == id) lungeToken else 0L, if (missActor == id) missToken else 0L,
                        cues = cues.filter { it.targetId == id }, lungeStyle = lungeStyle, settings = settings,
                        onHover = onHover, onClick = { onSelect(id) },
                        actionState = if (enemySide) null else combatActionState(member.isAlive, id == executingId, id == choosingId, (meters[id] ?: 0f) >= .999f))
                }
            }
        }

    }
}

@Composable
private fun DesktopBattleFigure(
    state: CombatantState, sprite: String?, services: DesktopAppServices, enemySide: Boolean,
    selected: Boolean, validTarget: Boolean, targeting: Boolean, meter: Float?, intent: String?,
    feedback: String?, figureHeight: androidx.compose.ui.unit.Dp, cardWidth: androidx.compose.ui.unit.Dp, lungeToken: Long, missToken: Long,
    cues: List<DesktopCombatCue>, lungeStyle: AttackLungeStyle, settings: UserSettings,
    onHover: (String?) -> Unit, onClick: () -> Unit, actionState: String?
) {
    val motion = remember { Animatable(0f) }
    val recoil = remember { Animatable(0f) }
    val feedbackRise = remember { Animatable(0f) }
    val hp by animateFloatAsState(state.hp.toFloat() / state.combatant.stats.maxHp.coerceAtLeast(1), tween(350))
    val readiness = (meter ?: 0f).coerceIn(0f, 1f)
    val direction = if (enemySide) 1f else -1f
    var previousHp by remember(state.combatant.id) { mutableIntStateOf(state.hp) }
    val idle = rememberInfiniteTransition(label = "combat idle")
    val wave by idle.animateFloat(0f, 6.28318f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "breathing")
    val phase = (state.combatant.id.hashCode() % 100) / 100f * 6.28318f
    val breath = sin(wave + phase)
    val shielded = state.statusEffects.any { it.id.lowercase() in setOf("invulnerable", "shield", "guard", "defend") }
    val lifeAlpha by animateFloatAsState(if (state.isAlive) 1f else if (enemySide) 0f else .65f, tween(850), label = "knockout")
    val isBroken = enemySide && state.isAlive && state.breakTurns > 0
    val displayedStability = remember(state.combatant.id) { Animatable(state.stability.toFloat()) }
    LaunchedEffect(state.stability, state.breakTurns) {
        if (isBroken) displayedStability.snapTo(0f)
        else displayedStability.animateTo(state.stability.toFloat(), tween(420, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(lungeToken) {
        if (lungeToken > 0) {
            val distance = when (lungeStyle) { AttackLungeStyle.MELEE -> 22f; AttackLungeStyle.RANGED -> 8f; else -> 0f }
            motion.animateTo(distance * direction, tween(120)); motion.animateTo(0f, tween(220))
        }
    }
    LaunchedEffect(missToken) {
        if (missToken > 0) { recoil.animateTo(-18f * direction, tween(120)); recoil.animateTo(0f, tween(220)) }
    }
    LaunchedEffect(state.hp) {
        val damaged = state.hp < previousHp
        previousHp = state.hp
        if (damaged && !settings.disableScreenshake) {
            recoil.snapTo(0f)
            recoil.animateTo(-7f * direction, tween(70)); recoil.animateTo(0f, tween(130))
        }
    }
    LaunchedEffect(feedback) { feedbackRise.snapTo(0f); if (feedback != null) feedbackRise.animateTo(-18f, tween(850)) }
    val accent = when {
        selected -> Color(0xFFFFBB55)
        targeting && validTarget -> Color(0xFF63E6FF)
        else -> Color(0xFF425566)
    }
    val status: @Composable () -> Unit = {
Column(Modifier.height(if (enemySide) 56.dp else 68.dp).fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.combatant.name, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(progress = { readiness }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFF2F9BE8), trackColor = Color(0xCC070B12))
            LinearProgressIndicator(progress = { hp.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFFF4D59), trackColor = Color(0xCC070B12))
            if (enemySide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isBroken) Icons.Rounded.FlashOn else Icons.Rounded.Bolt,
                    contentDescription = if (isBroken) "Broken: takes 25% more direct damage" else "Stability (Stagger)",
                    tint = if (isBroken) Color(0xFFFF8A80) else Color(0xFFB39DDB).copy(alpha = .9f), modifier = Modifier.size(12.dp))
                LinearProgressIndicator(progress = { displayedStability.value.div(state.combatant.stats.stability.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(5.dp), color = if (isBroken) Color(0xFFE57373) else Color(0xFF7E57C2), trackColor = Color(0xCC070B12))
            }
            if (!enemySide) Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index -> Box(Modifier.weight(1f).fillMaxHeight().background(if (state.momentum > index) Color(0xFF63E6FF) else Color.White.copy(alpha = .12f))) }
            }
            val notice = when {
                state.isAlive && enemySide && state.breakTurns > 0 -> "STABILITY BROKEN"
                intent != null -> intent
                state.weaponCharge != null -> "Charging ${state.weaponCharge?.remainingTurns}"
                state.statusEffects.isNotEmpty() || state.buffs.isNotEmpty() ->
                    "${state.statusEffects.size + state.buffs.size} effects"
                else -> null
            }
            notice?.let { Text(it, color = Color(0xFFFFBB55), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (if (targeting) selected && validTarget else !enemySide && actionState != null) {
                val label = if (targeting) {
                    if (selected) "SELECTED TARGET" else "AVAILABLE TARGET"
                } else actionState.orEmpty()
                Text(label, color = accent, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    LaunchedEffect(isHovered) {
        onHover(if (isHovered) state.combatant.id else null)
    }

    Column(Modifier.width(cardWidth).testTag("combat-figure-${state.combatant.id}").desktopPointerHover(!targeting || validTarget)
        .hoverable(interactionSource)
        .clickable(interactionSource = interactionSource, indication = null, enabled = !targeting || validTarget, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val spritePainter = rememberCombatSpritePainter(sprite, services.assetProvider)
        val intrinsic = spritePainter.intrinsicSize
        val aspect = if (intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.height > 0f) intrinsic.width / intrinsic.height else 1f
        val visibleWidth = minOf(cardWidth, figureHeight * aspect)
        val visibleHeight = minOf(figureHeight, cardWidth / aspect.coerceAtLeast(.1f))
        Box(Modifier.fillMaxWidth().height(figureHeight).testTag("combat-sprite-${state.combatant.id}"), contentAlignment = Alignment.BottomCenter) {
            val isReady = !enemySide && state.isAlive && readiness >= 0.999f
            if (isReady) {
                DesktopReadyAura(modifier = Modifier.fillMaxWidth().height(figureHeight))
            }
            Canvas(Modifier.align(Alignment.BottomCenter).width(visibleWidth).height(visibleHeight)) {
                drawOval(Color.Black.copy(alpha = .40f * lifeAlpha), Offset(size.width * .2f, size.height * .9f), androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .08f))
                val isTargetedOrSelected = state.isAlive && (selected || targeting && validTarget || isHovered)
                if (isTargetedOrSelected) {
                    val ringColor = if (selected) Color(0xFFFFBB55) else if (isHovered) Color(0xFF63E6FF) else accent
                    val ringWidth = minOf(size.width * .6f, size.height * .8f)
                    drawOval(ringColor.copy(alpha = .85f * lifeAlpha),
                        Offset((size.width-ringWidth)/2, size.height * .88f),
                        androidx.compose.ui.geometry.Size(ringWidth, size.height * .10f), style = Stroke(2.dp.toPx()))

                    val bracketColor = ringColor.copy(alpha = (if (selected) 0.95f else if (isHovered) 0.55f else 0.25f) * lifeAlpha)
                    val strokePx = 2.dp.toPx()
                    val len = minOf(size.width, size.height) * 0.16f
                    val pad = ((size.width - minOf(size.width, size.height)) / 2f + 4.dp.toPx())
                    val top = 4.dp.toPx()
                    val bottom = size.height - top
                    val right = size.width - pad
                    drawLine(bracketColor, Offset(pad, top), Offset(pad + len, top), strokePx)
                    drawLine(bracketColor, Offset(pad, top), Offset(pad, top + len), strokePx)
                    drawLine(bracketColor, Offset(right, top), Offset(right - len, top), strokePx)
                    drawLine(bracketColor, Offset(right, top), Offset(right, top + len), strokePx)
                    drawLine(bracketColor, Offset(pad, bottom), Offset(pad + len, bottom), strokePx)
                    drawLine(bracketColor, Offset(pad, bottom), Offset(pad, bottom - len), strokePx)
                    drawLine(bracketColor, Offset(right, bottom), Offset(right - len, bottom), strokePx)
                    drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - len), strokePx)
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).width(visibleWidth).height(visibleHeight).graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    val daze = sin(wave * 2f + phase)
                    val animate = state.isAlive && !settings.disableScreenshake
                    translationY = motion.value + recoil.value + when {
                        animate && isBroken -> 6f + daze * 3.5f
                        animate -> breath * (if (hp < .25f) 2.8f else 1.5f)
                        else -> 0f
                    }
                    rotationZ = if (isBroken && animate) breath * 4.2f else 0f
                    scaleX = if (isBroken) 1f + if (animate) .015f * daze else 0f
                        else 1f + if (animate) breath * .006f else 0f
                    scaleY = if (isBroken) .96f - (if (animate) .01f * daze else 0f) else scaleX
                    alpha = lifeAlpha * if (targeting && !validTarget) .45f else 1f
                }) {
                Image(spritePainter, state.combatant.name, Modifier.matchParentSize(),
                    alignment = Alignment.BottomCenter, contentScale = ContentScale.Fit)
                if (isBroken) com.example.starborn.shared.combat.StabilityBrokenField(
                    Modifier.matchParentSize().testTag("combat-broken-field-${state.combatant.id}"), flashesDisabled = settings.disableFlashes)
            }
            if (shielded && state.isAlive) Canvas(Modifier.align(Alignment.Center).size(figureHeight)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension * .47f
                val shield = Color(0xFF7FE6FF).copy(alpha = if (settings.disableFlashes) .22f else .3f + .12f * breath)
                drawCircle(shield.copy(alpha = .08f), radius, center)
                repeat(6) { index ->
                    val a = index * Math.PI / 3; val b = (index + 1) * Math.PI / 3
                    drawLine(shield, center + Offset(cos(a).toFloat(), sin(a).toFloat()) * radius,
                        center + Offset(cos(b).toFloat(), sin(b).toFloat()) * radius, 2.dp.toPx())
                }
            }
            cues.forEach { cue -> key(cue.token) { DesktopCombatHit(cue, settings.disableFlashes, Modifier.fillMaxSize()) } }
            cues.forEach { cue ->
                key(cue.token) {
                    if (cue.kind == DesktopCueKind.HIT || cue.kind == DesktopCueKind.HEAL) {
                        DesktopDamageBubble(cue, Modifier.align(Alignment.Center))
                    } else if (cue.kind == DesktopCueKind.BREAK) {
                        // The shared mobile burst includes its floating badge.
                    } else {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                            color = Color(0xF007111A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(cue.label, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = cue.color, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            feedback?.let { label ->
                Surface(Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = feedbackRise.value },
                    shape = RoundedCornerShape(8.dp), color = Color(0xEE07111A)) {
                    Text(label, Modifier.padding(8.dp), color = if (label.startsWith("+")) Color(0xFF80E7A0) else Color(0xFFFFBB55),
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        status()
    }
}
