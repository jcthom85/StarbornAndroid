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
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onSelect: (String) -> Unit
) {
    val settings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    BoxWithConstraints(modifier) {
        val compactCrew = !enemySide && members.size > 2 && (battleHeight < 640.dp || opposingCount > 3 && battleHeight < 800.dp)
        val portraitSize = if (compactCrew) 48.dp else if (battleHeight < 640.dp) 64.dp else 80.dp
        val formations = when {
            !enemySide -> members.chunked(if (maxWidth >= 600.dp) 4 else 2)
            members.size <= 3 -> listOf(members)
            members.size == 4 -> members.chunked(2)
            members.size == 5 -> listOf(members.take(2), members.drop(2))
            else -> members.chunked(3)
        }
        val enemyCardWidth = minOf(200.dp, (maxWidth - 24.dp) / 3)
        val cardWidth = if (enemySide) enemyCardWidth else if (compactCrew) 88.dp else 140.dp
        val requestedSize = if (enemySide) enemyCardWidth * .88f else portraitSize
        val summaryHeight = members.maxOfOrNull { member ->
            76 + (if (member.isAlive && enemySide && member.stability <= 0) 16 else 0) +
                (if (intents[member.combatant.id] != null) 15 else 0) +
                (if (member.weaponCharge != null) 15 else 0) +
                (if (member.statusEffects.isNotEmpty()) 14 else 0) +
                (if (member.buffs.isNotEmpty()) 14 else 0)
        } ?: 76
        val rowSpace = (maxHeight - 14.dp * (formations.size - 1).coerceAtLeast(0)) / formations.size.coerceAtLeast(1)
        val figureHeight = minOf(requestedSize, (rowSpace - summaryHeight.dp).coerceAtLeast(24.dp))
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp, if (enemySide) Alignment.CenterVertically else Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally) {
            formations.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                    row.forEach { member -> key(member.combatant.id) {
                        val id = member.combatant.id
                        DesktopBattleFigure(member, sprite(id), services, enemySide,
                            selectedId == id, id in validTargets, targeting, meters[id], intents[id], feedback[id], figureHeight, cardWidth,
                            if (lungeActor == id) lungeToken else 0L, if (missActor == id) missToken else 0L,
                            cues = cues.filter { it.targetId == id }, lungeStyle = lungeStyle, settings = settings,
                            onClick = { onSelect(id) })
                    } }
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
    cues: List<DesktopCombatCue>, lungeStyle: AttackLungeStyle, settings: UserSettings, onClick: () -> Unit
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
Column(Modifier.widthIn(max = if (enemySide) 220.dp else 180.dp).fillMaxWidth().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.combatant.name, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            LinearProgressIndicator(progress = { readiness }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Color(0xFF2F9BE8), trackColor = Color(0xCC070B12))
            LinearProgressIndicator(progress = { hp.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Color(0xFFFF4D59), trackColor = Color(0xCC070B12))
            if (enemySide) LinearProgressIndicator(progress = { state.stability.toFloat().div(state.combatant.stats.stability.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFF9F79D2), trackColor = Color(0xCC070B12))
            if (!enemySide) Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index -> Box(Modifier.weight(1f).fillMaxHeight().background(if (state.momentum > index) Color(0xFF63E6FF) else Color.White.copy(alpha = .12f))) }
            }
            if (state.isAlive && enemySide && state.stability <= 0) Text("BROKEN", color = Color(0xFFFFBB55), style = MaterialTheme.typography.labelSmall)
            intent?.let { Text(it, color = Color(0xFFFFBB55), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            state.weaponCharge?.let { Text("Charging ${it.remainingTurns}", color = Color(0xFFFFDE70), fontSize = 10.sp) }
            if (state.statusEffects.isNotEmpty()) Text(state.statusEffects.joinToString { services.contentName(it.id) }, color = Color(0xFFD3A1FF), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (state.buffs.isNotEmpty()) Text(state.buffs.joinToString { services.contentName(it.effect.stat) }, color = Color(0xFF80E7A0), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (if (targeting) validTarget else selected || !enemySide && readiness >= .999f) Text(if (targeting) "TARGET" else if (enemySide) "FOCUSED" else "READY", color = accent, style = MaterialTheme.typography.labelSmall)
        }
    }
    Column(Modifier.width(cardWidth).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, enabled = !targeting || validTarget, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (enemySide) status()
        Box(Modifier.fillMaxWidth().height(figureHeight), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxSize()) {
                drawOval(Color.Black.copy(alpha = .40f * lifeAlpha), Offset(size.width * .2f, size.height * .9f), androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .08f))
                if (state.isAlive && (selected || targeting && validTarget)) {
                    val ringWidth = minOf(size.width * .6f, size.height * .8f)
                    drawOval(accent.copy(alpha = .85f * lifeAlpha),
                        Offset((size.width-ringWidth)/2, size.height * .88f),
                        androidx.compose.ui.geometry.Size(ringWidth, size.height * .10f), style = Stroke(2.dp.toPx()))
                }
            }
            Image(rememberDesktopAssetPainter(sprite, services.assetProvider), state.combatant.name,
                Modifier.size(figureHeight).graphicsLayer {
                    translationY = motion.value + recoil.value + if (state.isAlive && !settings.disableScreenshake) breath * (if (hp < .25f) 2.8f else 1.5f) else 0f
                    rotationZ = if (enemySide && state.isAlive && state.stability <= 0 && !settings.disableScreenshake) breath * 2.5f else 0f
                    scaleX = 1f + if (state.isAlive && !settings.disableScreenshake) breath * .006f else 0f
                    scaleY = scaleX
                    alpha = lifeAlpha * if (targeting && !validTarget) .45f else 1f
                }, contentScale = ContentScale.Fit)
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
            Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                cues.takeLast(4).forEach { cue -> key(cue.token) {
                    Surface(color = Color(0xF007111A), shape = RoundedCornerShape(6.dp)) {
                        Text(cue.label, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = cue.color, style = MaterialTheme.typography.labelMedium)
                    }
                } }
            }
            feedback?.let { label ->
                Surface(Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = feedbackRise.value },
                    shape = RoundedCornerShape(8.dp), color = Color(0xEE07111A)) {
                    Text(label, Modifier.padding(8.dp), color = if (label.startsWith("+")) Color(0xFF80E7A0) else Color(0xFFFFBB55),
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (!enemySide) status()
    }
}
