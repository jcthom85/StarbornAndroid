package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.starborn.R
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Enemy
import kotlinx.coroutines.delay
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.ui.titleCaseName
import com.example.starborn.feature.combat.ui.animations.CombatSide
import com.example.starborn.feature.combat.ui.animations.LungeAxis
import com.example.starborn.feature.combat.ui.animations.Lungeable
import com.example.starborn.feature.combat.viewmodel.AttackLungeStyle
import com.example.starborn.feature.enemy.EnemyPresentationTier
import com.example.starborn.feature.enemy.combatEnemySpriteScale
import com.example.starborn.feature.enemy.enemyPresentationTier
import com.example.starborn.ui.background.rememberAssetPainter
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

data class CompositeEnemyEntry(
    val enemy: com.example.starborn.domain.model.Enemy,
    val combatantId: String,
    val layout: com.example.starborn.domain.model.CompositePart
)

private val HUD_BAR_WIDTH = 156.dp
private val TargetRippleColor = Color(0xFF3FE4FF)
private val EnemyShadowDrop = 0.dp

@Composable
fun PartyRoster(
    party: List<Player>,
    combatState: CombatState,
    activeId: String?,
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    supportFx: List<SupportFxUi>,
    telegraphFx: List<TelegraphFxUi>,
    onMemberTap: ((String) -> Unit)? = null,
    allowNonReadySelection: Boolean = false,
    victoryEmotes: Boolean = false,
    atbMeters: Map<String, Float> = emptyMap(),
    lungeActorId: String?,
    lungeToken: Long,
    lungeStyle: AttackLungeStyle,
    onLungeFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (party.isEmpty()) return
    val rows = remember(party) { party.chunked(2) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { rowMembers ->
            key(rowMembers.joinToString(separator = "|") { member -> member.id }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    rowMembers.forEach { member ->
                        key(member.id) {
                            val memberState = combatState.combatants[member.id]
                            val maxHp = memberState?.combatant?.stats?.maxHp ?: member.hp.coerceAtLeast(1)
                            val currentHp = memberState?.hp ?: maxHp
                            val isActive = member.id == activeId
                            val supportHighlights = supportFx.filter { member.id in it.targetIds }
                            val telegraphHighlights = telegraphFx.filter { member.id in it.targetIds }
                            val cardShape = RoundedCornerShape(22.dp)
                            val isAlive = memberState?.isAlive != false
                            val memberLungeToken = if (member.id == lungeActorId) lungeToken else null
                            val isMissLunge = memberLungeToken != null && lungeStyle == AttackLungeStyle.MISS
                            val portraitPath = when {
                                !isAlive -> "images/characters/emotes/${member.id}_down.png"
                                isMissLunge -> "images/characters/emotes/${member.id}_confident.png"
                                memberLungeToken != null -> "images/characters/emotes/${member.id}_angry.png"
                                victoryEmotes -> "images/characters/emotes/${member.id}_cool.png"
                                else -> member.combatIconPath.takeIf { it.isNotBlank() } ?: member.miniIconPath
                            }
                            val portraitPainter = rememberAssetPainter(portraitPath, painterResource(R.drawable.main_menu_background))
                            val portraitFrameSize = 114.dp
                            val atbProgress = atbMeters[member.id] ?: 0f
                            val readyToAct = atbProgress >= 0.999f

                            val baseModifier = Modifier.widthIn(min = 150.dp)
                            val canTap = onMemberTap != null && isAlive && (readyToAct || allowNonReadySelection)
                            val interactiveModifier = if (canTap) {
                                baseModifier.clickable { onMemberTap(member.id) }
                            } else {
                                baseModifier
                            }
                            val damageFlash = rememberDamageFlash(member.id, combatState.log)
                            val hitPulse = rememberHitPulse(member.id, combatState.log)
                            val hitRecoilY = rememberHitRecoil(
                                targetId = member.id,
                                log = combatState.log,
                                directionSign = 1f
                            )
                            val suppressLunge = damageFlash > 0f
                            Box(
                                modifier = interactiveModifier.padding(horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(portraitFrameSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (readyToAct) {
                                            ReadyAura(
                                                color = Color(0xFF4EE7FF),
                                                modifier = Modifier.matchParentSize()
                                            )
                                        }
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .border(
                                                        width = 2.dp,
                                                        color = Color.White.copy(alpha = 0.55f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1C1F24))
                                                .graphicsLayer {
                                                    compositingStrategy = CompositingStrategy.Offscreen
                                                    val pulse = 1f + 0.06f * hitPulse
                                                    scaleX = pulse
                                                    scaleY = pulse
                                                    translationY = hitRecoilY
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val lungeDistance = when {
                                                suppressLunge -> 0.dp
                                                isMissLunge -> 18.dp
                                                lungeStyle == AttackLungeStyle.RANGED -> 12.dp
                                                lungeStyle == AttackLungeStyle.BUFF -> 16.dp
                                                lungeStyle == AttackLungeStyle.CAST -> 8.dp
                                                lungeStyle == AttackLungeStyle.ITEM -> 6.dp
                                                lungeStyle == AttackLungeStyle.SNACK -> 8.dp
                                                else -> 24.dp
                                            }
                                            val lungeAxis = if (isMissLunge || lungeStyle == AttackLungeStyle.ITEM) LungeAxis.X else LungeAxis.Y
                                            val lungeDirectionSign = when {
                                                isMissLunge -> -1f
                                                lungeStyle == AttackLungeStyle.RANGED -> -1f
                                                else -> 1f
                                            }
                                            Lungeable(
                                                side = CombatSide.PLAYER,
                                                triggerToken = memberLungeToken,
                                                distance = lungeDistance,
                                                axis = lungeAxis,
                                                directionSign = lungeDirectionSign,
                                                modifier = Modifier.matchParentSize(),
                                                onFinished = { memberLungeToken?.let(onLungeFinished) }
                                            ) {
                                                Image(
                                                    painter = portraitPainter,
                                                    contentDescription = member.name,
                                                    modifier = Modifier.matchParentSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            val shielded = memberState?.statusEffects.orEmpty().any { status ->
                                                isShieldVisualStatus(status.id)
                                            }
                                            if (shielded && isAlive) {
                                                ShieldFieldOverlay(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clip(CircleShape)
                                                )
                                            }
                                            if (damageFlash > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.18f * damageFlash))
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = titleCaseName(member.name),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = CombatNameFont,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White
                                    )
                                    AtbBar(
                                        progress = atbProgress,
                                        modifier = Modifier.width(HUD_BAR_WIDTH)
                                    )
                                    StatBar(
                                        current = currentHp,
                                        max = maxHp,
                                        color = Color(0xFFFF5252),
                                        background = Color.Black.copy(alpha = 0.5f),
                                        height = 12.dp,
                                        modifier = Modifier.width(HUD_BAR_WIDTH)
                                    )
                                }
                                val statuses = memberState?.statusEffects.orEmpty()
                                val buffs = memberState?.buffs.orEmpty()
                                if (statuses.isNotEmpty() || buffs.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .zIndex(0.5f),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            StatusBadges(statuses = statuses, buffs = buffs)
                                        }
                                    }
                                }
                                CombatFxOverlay(
                                    damageFx = damageFx.filter { it.targetId == member.id },
                                    attackFx = attackFx.filter { it.targetId == member.id },
                                    healFx = healFx.filter { it.targetId == member.id },
                                    statusFx = statusFx.filter { it.targetId == member.id },
                                    shieldBreakFx = shieldBreakFx.filter { it.targetId == member.id },
                                    showKnockout = knockoutFx.any { it.targetId == member.id },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp)
                                        .size(portraitFrameSize)
                                )
                                CombatFxOverlay(
                                    damageFx = emptyList(),
                                    attackFx = emptyList(),
                                    healFx = emptyList(),
                                    statusFx = emptyList(),
                                    shieldBreakFx = emptyList(),
                                    showKnockout = false,
                                    shape = cardShape,
                                    supportFx = supportHighlights,
                                    telegraphFx = telegraphHighlights,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                        }
                    }
                    if (rowMembers.size == 1 && party.size > 1) {
                        Spacer(modifier = Modifier.width(150.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnemyRoster(
    enemies: List<Enemy>,
    combatantIds: List<String>,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    labelBorderColor: Color,
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    telegraphFx: List<TelegraphFxUi>,
    delayedDeathTargets: Map<String, Long>,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit,
    atbMeters: Map<String, Float> = emptyMap(),
    lungeActorId: String?,
    lungeToken: Long,
    missLungeActorId: String?,
    missLungeToken: Long,
    onLungeFinished: (Long) -> Unit,
    onMissLungeFinished: (Long) -> Unit,
    showTargetPrompt: Boolean = false,
    lungeStyle: AttackLungeStyle = AttackLungeStyle.MELEE
) {
    if (enemies.isEmpty()) return
    val compositeGroup = enemies.firstOrNull()?.composite?.group
    val useCompositeLayout = compositeGroup != null &&
        enemies.all { it.composite?.group == compositeGroup }
    if (useCompositeLayout) {
        CompositeEnemyRoster(
            enemies = enemies,
            combatantIds = combatantIds,
            combatState = combatState,
            activeId = activeId,
            selectedEnemyIds = selectedEnemyIds,
            labelBorderColor = labelBorderColor,
            damageFx = damageFx,
            attackFx = attackFx,
            healFx = healFx,
            statusFx = statusFx,
            shieldBreakFx = shieldBreakFx,
            knockoutFx = knockoutFx,
            telegraphFx = telegraphFx,
            delayedDeathTargets = delayedDeathTargets,
            onEnemyTap = onEnemyTap,
            onEnemyLongPress = onEnemyLongPress,
            atbMeters = atbMeters,
            lungeActorId = lungeActorId,
            lungeToken = lungeToken,
            missLungeActorId = missLungeActorId,
            missLungeToken = missLungeToken,
            onLungeFinished = onLungeFinished,
            onMissLungeFinished = onMissLungeFinished,
            showTargetPrompt = showTargetPrompt,
            lungeStyle = lungeStyle
        )
        return
    }
    val density = LocalDensity.current
    val totalEnemies = enemies.size
    val rowCounts = when {
        totalEnemies <= 3 -> listOf(totalEnemies)
        totalEnemies == 4 -> listOf(2, 2)
        else -> listOf(2, 3)
    }
    val indexedEnemies = enemies.mapIndexed { index, enemy -> index to enemy }
    var cursor = 0
    val rows = rowCounts.mapNotNull { count ->
        if (cursor >= indexedEnemies.size) return@mapNotNull null
        val end = (cursor + count).coerceAtMost(indexedEnemies.size)
        val slice = indexedEnemies.subList(cursor, end)
        cursor = end
        slice
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (rows.size > 1) 360.dp else 240.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boxWidth = this.maxWidth
        val rowSpacing = 14.dp
        val columnSpacing = 12.dp
        val maxColumnsForSizing = 3
        val baseCardSize = 200.dp
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEachIndexed { rowIndex, row ->
                val columns = row.size.coerceAtLeast(1)
                val rowWidth = boxWidth
                val maxCardSizeForLayout =
                    (rowWidth - columnSpacing * (maxColumnsForSizing - 1)) / maxColumnsForSizing
                val cardSize = baseCardSize.coerceAtMost(maxCardSizeForLayout)
                val portraitSize = cardSize * 0.88f
                val barWidth = cardSize * 0.72f
                val innerPadding = if (cardSize < 150.dp) 6.dp else 10.dp
                val rowOffset = if (rowIndex == 0 && rows.size > 1) (-12).dp else 0.dp
                Row(
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset(y = rowOffset)
                ) {
                    row.forEach { (index, enemy) ->
                        val combatantId = combatantIds.getOrElse(index) { enemy.id }
                        val enemyState = combatState.combatants[combatantId]
                        val maxHp = enemyState?.combatant?.stats?.maxHp ?: enemy.hp.coerceAtLeast(1)
                        val currentHp = enemyState?.hp ?: maxHp
                        val maxStability = enemyState?.combatant?.stats?.stability ?: 100
                        val currentStability = enemyState?.stability ?: maxStability
                        val isActive = activeId == combatantId
                        val isSelected = combatantId in selectedEnemyIds
                        val isAlive = enemyState?.isAlive != false
                        val telegraphHighlights = telegraphFx.filter { combatantId in it.targetIds }
                        val isVisible = isAlive || delayedDeathTargets.containsKey(combatantId)
                        val isElite = isEliteTier(enemy.tier)
                        val isBoss = isBossTier(enemy.tier)
                        val flash = rememberDamageFlash(combatantId, combatState.log)
                        val hitPulse = rememberHitPulse(combatantId, combatState.log)
                        val hitRecoilY = rememberHitRecoil(
                            targetId = combatantId,
                            log = combatState.log,
                            directionSign = -1f
                        )
                        val damageShake = rememberDamageShake(combatantId, combatState.log)
                        val selectionGlow = remember { Animatable(0f) }
                        LaunchedEffect(isSelected) {
                            if (isSelected) {
                                selectionGlow.snapTo(0.6f)
                                selectionGlow.animateTo(0f, tween(durationMillis = 500))
                            } else {
                                selectionGlow.snapTo(0f)
                            }
                        }
                        val spritePath = enemy.sprite.firstOrNull()?.takeIf { it.isNotBlank() }
                            ?: enemy.portrait?.takeIf { it.isNotBlank() }
                            ?: "images/enemies/${enemy.id}_combat.png"
                        val painter = rememberAssetPainter(
                            spritePath,
                            painterResource(R.drawable.main_menu_background)
                        )
                        val shape = RoundedCornerShape(28.dp)
                        val atbProgress = atbMeters[combatantId] ?: 0f
                        val spriteScale = combatEnemySpriteScale(enemy.tier)
                        val rippleScale = 1f + (spriteScale - 1f) * 0.5f
                        val rippleSize = portraitSize * rippleScale
                        val labelSpacer = if (isElite) 8.dp else 4.dp
                        val enemyLungeToken = if (combatantId == lungeActorId) lungeToken else null
                        val enemyMissToken = if (combatantId == missLungeActorId) missLungeToken else null
                        val enemyInteractionSource = remember(combatantId) { MutableInteractionSource() }
                        val cardModifier = Modifier
                            .width(cardSize)
                            .graphicsLayer {
                                translationX = damageShake * with(density) { 6.dp.toPx() }
                            }
                            .zIndex(
                                when {
                                    isActive -> 3f
                                    isSelected -> 2.5f
                                    rowIndex == rows.lastIndex -> 2f
                                    else -> 1f
                                }
                            )
                            .combinedClickable(
                                interactionSource = enemyInteractionSource,
                                indication = null,
                                onClick = { if (isAlive) onEnemyTap(combatantId) },
                                onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
                            )

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(220)) +
                                scaleIn(initialScale = 0.9f, animationSpec = tween(260)),
                            exit = fadeOut(animationSpec = tween(360)) +
                                scaleOut(targetScale = 0.65f, animationSpec = tween(420)),
                            modifier = cardModifier,
                            label = "enemy_card_$combatantId"
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EnemyStatusLabel(
                                    name = enemy.name,
                                    currentHp = currentHp,
                                    maxHp = maxHp,
                                    currentStability = currentStability,
                                    maxStability = maxStability,
                                    breakTurns = enemyState?.breakTurns ?: 0,
                                    atbProgress = atbProgress,
                                    isAlive = isAlive,
                                    isActive = isActive,
                                    isSelected = isSelected,
                                    accentColor = labelBorderColor,
                                    barWidth = barWidth
                                )
                                Spacer(modifier = Modifier.height(labelSpacer))
                                Box(
                                    modifier = Modifier
                                        .size(cardSize)
                                        .graphicsLayer {
                                            val glowScale = 1f + selectionGlow.value * 0.08f
                                            val hitScale = 1f + 0.04f * hitPulse
                                            val combined = glowScale * hitScale
                                            scaleX = combined
                                            scaleY = combined
                                        }
                                ) {
                                    EnemyShadow(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(bottom = innerPadding)
                                            .offset(y = EnemyShadowDrop)
                                            .size(portraitSize),
                                        widthFraction = 0.72f,
                                        heightFraction = 0.16f,
                                        alpha = 0.44f
                                    )
                                    if (flash > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(shape)
                                                .background(Color.Black.copy(alpha = 0.18f * flash))
                                        )
                                    }
                                    SelectionRipple(
                                        isSelected = isSelected,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(bottom = innerPadding)
                                            .size(rippleSize)
                                    )
                                    val idleTransition = rememberInfiniteTransition(label = "enemy_idle_$combatantId")
                                    val idlePhase = remember(combatantId) { Random.nextFloat() * (2f * Math.PI).toFloat() }
                                    val idleWave by idleTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = (2f * Math.PI).toFloat(),
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 2600, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "enemy_idle_wave"
                                    )
                                    val enemyBroken = (enemyState?.breakTurns ?: 0) > 0
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(bottom = innerPadding)
                                            .size(portraitSize)
                                            .graphicsLayer {
                                                compositingStrategy = CompositingStrategy.Offscreen
                                                transformOrigin = TransformOrigin(0.5f, 1f)
                                                if (enemyBroken) {
                                                    scaleX = spriteScale
                                                    scaleY = spriteScale
                                                    translationY = hitRecoilY
                                                } else {
                                                    val idleBreath = sin(idleWave + idlePhase)
                                                    val idleBob = idleBreath * 2.2f
                                                    val idlePuff = 1f + 0.012f * idleBreath
                                                    scaleX = spriteScale * (1f + 0.006f * idleBreath)
                                                    scaleY = spriteScale * idlePuff
                                                    translationY = hitRecoilY + idleBob
                                                }
                                            }
                                    ) {
                                        Lungeable(
                                            side = CombatSide.ENEMY,
                                            triggerToken = enemyMissToken,
                                            distance = 18.dp,
                                            axis = LungeAxis.X,
                                            directionSign = 1f,
                                            modifier = Modifier.matchParentSize(),
                                            onFinished = { enemyMissToken?.let(onMissLungeFinished) }
                                        ) {
                                            val dist = when (lungeStyle) {
                                                AttackLungeStyle.RANGED -> 12.dp
                                                AttackLungeStyle.BUFF -> 16.dp
                                                AttackLungeStyle.CAST -> 8.dp
                                                AttackLungeStyle.ITEM -> 6.dp
                                                AttackLungeStyle.SNACK -> 8.dp
                                                else -> 24.dp
                                            }
                                            val sign = when {
                                                lungeStyle == AttackLungeStyle.RANGED || lungeStyle == AttackLungeStyle.BUFF || lungeStyle == AttackLungeStyle.SNACK -> -1f
                                                else -> 1f
                                            }
                                            Lungeable(
                                                side = CombatSide.ENEMY,
                                                triggerToken = enemyLungeToken,
                                                distance = dist,
                                                axis = if (lungeStyle == AttackLungeStyle.ITEM) LungeAxis.X else LungeAxis.Y,
                                                directionSign = sign,
                                                modifier = Modifier.matchParentSize(),
                                                onFinished = { enemyLungeToken?.let(onLungeFinished) }
                                            ) {
                                                Image(
                                                    painter = painter,
                                                    contentDescription = enemy.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.matchParentSize()
                                                )
                                            }
                                        }
                                        val enemyShielded = enemyState?.statusEffects.orEmpty().any { status ->
                                            isShieldVisualStatus(status.id)
                                        }
                                        val enemyBroken = (enemyState?.breakTurns ?: 0) > 0
                                        if (enemyShielded && isAlive) {
                                            ShieldFieldOverlay(modifier = Modifier.matchParentSize())
                                        }
                                        if (enemyBroken && isAlive) {
                                            BrokenFieldOverlay(modifier = Modifier.matchParentSize())
                                        }
                                    }
                                    CombatFxOverlay(
                                        damageFx = damageFx.filter { it.targetId == combatantId },
                                        attackFx = attackFx.filter { it.targetId == combatantId },
                                        healFx = healFx.filter { it.targetId == combatantId },
                                        statusFx = statusFx.filter { it.targetId == combatantId },
                                        shieldBreakFx = shieldBreakFx.filter { it.targetId == combatantId },
                                        showKnockout = knockoutFx.any { it.targetId == combatantId },
                                        shape = shape,
                                        telegraphFx = telegraphHighlights,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    EnemyStatusRail(
                                        statuses = enemyState?.statusEffects.orEmpty(),
                                        buffs = enemyState?.buffs.orEmpty(),
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .zIndex(6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompositeEnemyRoster(
    enemies: List<Enemy>,
    combatantIds: List<String>,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    labelBorderColor: Color,
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    telegraphFx: List<TelegraphFxUi>,
    delayedDeathTargets: Map<String, Long>,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit,
    atbMeters: Map<String, Float> = emptyMap(),
    lungeActorId: String?,
    lungeToken: Long,
    missLungeActorId: String?,
    missLungeToken: Long,
    onLungeFinished: (Long) -> Unit,
    onMissLungeFinished: (Long) -> Unit,
    showTargetPrompt: Boolean = false,
    lungeStyle: AttackLungeStyle = AttackLungeStyle.MELEE
) {
    if (enemies.isEmpty()) return
    val entries = enemies.mapIndexedNotNull { index, enemy ->
        val layout = enemy.composite ?: return@mapIndexedNotNull null
        val combatantId = combatantIds.getOrElse(index) { enemy.id }
        CompositeEnemyEntry(enemy = enemy, combatantId = combatantId, layout = layout)
    }
    if (entries.size != enemies.size) return
    val compositeGroup = entries.firstOrNull()?.layout?.group
    val underSpriteIds = if (compositeGroup == "driller") {
        setOf("driller_drill_arm", "driller_fire_arm")
    } else {
        emptySet()
    }
    val groupOffsetSource = entries.firstOrNull { entry ->
        entry.layout.role?.equals("core", ignoreCase = true) == true
    } ?: entries.firstOrNull()
    val underSpriteEntries = entries.filter { it.enemy.id in underSpriteIds }
    val topRowEntriesBase = entries.filterNot { it.enemy.id in underSpriteIds }
    val topRowEntries = if (compositeGroup == "driller") {
        topRowEntriesBase.sortedBy { it.layout.offsetX }
    } else {
        topRowEntriesBase
    }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boxWidth = maxWidth
        val paddingFactor = 0.18f
        val extentFactorX = entries.maxOf { entry ->
            abs(entry.layout.offsetX) + (entry.layout.widthScale / 2f)
        }
        val widthLimit = boxWidth * 0.95f
        val sizeLimit = widthLimit / (extentFactorX * 2f + paddingFactor)
        val baseSize = minOf(minOf(220.dp, sizeLimit) * 1.08f, sizeLimit)
        val baseValue = baseSize.value
        val groupOffsetX = groupOffsetSource?.layout?.groupOffsetX ?: 0f
        val groupOffsetY = groupOffsetSource?.layout?.groupOffsetY ?: 0f
        val groupOffsetXValue = baseValue * groupOffsetX
        val groupOffsetYValue = baseValue * groupOffsetY
        val groupOffsetXDp = baseSize * groupOffsetX
        val groupOffsetYDp = baseSize * groupOffsetY
        var minX = 0f
        var maxX = 0f
        var minY = 0f
        var maxY = 0f
        entries.forEach { entry ->
            val width = baseValue * entry.layout.widthScale
            val height = baseValue * entry.layout.heightScale
            val labelReserve = if (entry.enemy.id in underSpriteIds) {
                baseValue * 0.44f
            } else {
                0f
            }
            val centerX = baseValue * entry.layout.offsetX + groupOffsetXValue
            val centerY = baseValue * entry.layout.offsetY + groupOffsetYValue
            minX = min(minX, centerX - width / 2f)
            maxX = max(maxX, centerX + width / 2f)
            minY = min(minY, centerY - height / 2f)
            maxY = max(maxY, centerY + height / 2f + labelReserve)
        }
        val padding = baseValue * paddingFactor
        val halfWidth = max(abs(minX), abs(maxX)) + padding
        val halfHeight = max(abs(minY), abs(maxY)) + padding
        val compositeWidth = (halfWidth * 2f).dp
        val compositeHeight = (halfHeight * 2f).dp
        val underLabelAnchorY = underSpriteEntries.maxOfOrNull { entry ->
            val partHeight = baseSize * entry.layout.heightScale
            val partOffsetY = baseSize * entry.layout.offsetY + groupOffsetYDp
            partOffsetY + partHeight / 2f
        } ?: 0.dp
        val underLabelOffsetY = underLabelAnchorY + 42.dp
        val topLabelOffset = 76.dp
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (topRowEntries.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxWidth()
                        .offset(x = groupOffsetXDp, y = topLabelOffset + groupOffsetYDp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    items(topRowEntries) { entry ->
                        CompositePartStatus(
                            entry = entry,
                            combatState = combatState,
                            activeId = activeId,
                            selectedEnemyIds = selectedEnemyIds,
                            atbMeters = atbMeters,
                            labelBorderColor = labelBorderColor,
                            onEnemyTap = onEnemyTap,
                            onEnemyLongPress = onEnemyLongPress
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.size(compositeWidth, compositeHeight),
                contentAlignment = Alignment.Center
            ) {
                entries.forEach { entry ->
                    val enemy = entry.enemy
                    val combatantId = entry.combatantId
                    val enemyState = combatState.combatants[combatantId]
                    val isActive = activeId == combatantId
                    val isSelected = combatantId in selectedEnemyIds
                    val telegraphHighlights = telegraphFx.filter { combatantId in it.targetIds }
                    val isAlive = enemyState?.isAlive != false
                    val isVisible = isAlive || delayedDeathTargets.containsKey(combatantId)
                    val flash = rememberDamageFlash(combatantId, combatState.log)
                    val hitPulse = rememberHitPulse(combatantId, combatState.log)
                    val hitRecoilY = rememberHitRecoil(
                        targetId = combatantId,
                        log = combatState.log,
                        directionSign = -1f
                    )
                    val damageShake = rememberDamageShake(combatantId, combatState.log)
                    val selectionGlow = remember { Animatable(0f) }
                    LaunchedEffect(isSelected) {
                        if (isSelected) {
                            selectionGlow.snapTo(0.6f)
                            selectionGlow.animateTo(0f, tween(durationMillis = 500))
                        } else {
                            selectionGlow.snapTo(0f)
                        }
                    }
                    val spritePath = enemy.sprite.firstOrNull()?.takeIf { it.isNotBlank() }
                        ?: enemy.portrait?.takeIf { it.isNotBlank() }
                        ?: "images/enemies/${enemy.id}_combat.png"
                    val painter = rememberAssetPainter(
                        spritePath,
                        painterResource(R.drawable.main_menu_background)
                    )
                    val shape = RoundedCornerShape(24.dp)
                    val enemyLungeToken = if (combatantId == lungeActorId) lungeToken else null
                    val enemyMissToken = if (combatantId == missLungeActorId) missLungeToken else null
                    val partWidth = baseSize * entry.layout.widthScale
                    val partHeight = baseSize * entry.layout.heightScale
                    val partOffsetX = baseSize * entry.layout.offsetX
                    val partOffsetY = baseSize * entry.layout.offsetY + groupOffsetYDp
                    val enemyInteractionSource = remember(combatantId) { MutableInteractionSource() }
                    val partModifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = partOffsetX + groupOffsetXDp, y = partOffsetY)
                        .size(width = partWidth, height = partHeight)
                        .graphicsLayer {
                            translationX = damageShake * with(density) { 6.dp.toPx() }
                        }
                        .zIndex(
                            when {
                                isActive -> entry.layout.z + 0.4f
                                isSelected -> entry.layout.z + 0.2f
                                else -> entry.layout.z
                            }
                        )
                        .combinedClickable(
                            interactionSource = enemyInteractionSource,
                            indication = null,
                            onClick = { if (isAlive) onEnemyTap(combatantId) },
                            onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
                        )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
                        exit = fadeOut(animationSpec = tween(360)) +
                            scaleOut(targetScale = 0.65f, animationSpec = tween(420)),
                        modifier = partModifier,
                        label = "enemy_part_$combatantId"
                    ) {
                        val idleTransition = rememberInfiniteTransition(label = "enemy_idle_part_$combatantId")
                        val idlePhase = remember(combatantId) { Random.nextFloat() * (2f * Math.PI).toFloat() }
                        val idleWave by idleTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = (2f * Math.PI).toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2600, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "enemy_idle_wave"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    val glowScale = 1f + selectionGlow.value * 0.08f
                                    val hitScale = 1f + 0.04f * hitPulse
                                    val combined = glowScale * hitScale
                                    val isBroken = (enemyState?.breakTurns ?: 0) > 0
                                    if (isBroken) {
                                        scaleX = combined
                                        scaleY = combined
                                        translationY = hitRecoilY
                                    } else {
                                        val idleBreath = sin(idleWave + idlePhase)
                                        val idleBob = idleBreath * 2.0f
                                        val idlePuff = 1f + 0.01f * idleBreath
                                        scaleX = combined * (1f + 0.006f * idleBreath)
                                        scaleY = combined * idlePuff
                                        translationY = hitRecoilY + idleBob
                                    }
                                }
                        ) {
                            if (entry.layout.role?.equals("core", ignoreCase = true) != false) {
                                EnemyShadow(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(y = EnemyShadowDrop),
                                    widthFraction = 0.78f,
                                    heightFraction = 0.18f,
                                    alpha = 0.44f
                                )
                            }
                            if (flash > 0f) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(shape)
                                        .background(Color.Black.copy(alpha = 0.18f * flash))
                                )
                            }
                            SelectionRipple(
                                isSelected = isSelected,
                                modifier = Modifier.matchParentSize()
                            )
                            Lungeable(
                                side = CombatSide.ENEMY,
                                triggerToken = enemyMissToken,
                                distance = 18.dp,
                                axis = LungeAxis.X,
                                directionSign = 1f,
                                modifier = Modifier.matchParentSize(),
                                onFinished = { enemyMissToken?.let(onMissLungeFinished) }
                            ) {
                                val dist = when (lungeStyle) {
                                    AttackLungeStyle.RANGED -> 12.dp
                                    AttackLungeStyle.BUFF -> 16.dp
                                    AttackLungeStyle.CAST -> 8.dp
                                    AttackLungeStyle.ITEM -> 6.dp
                                    AttackLungeStyle.SNACK -> 8.dp
                                    else -> 24.dp
                                }
                                val sign = when {
                                    lungeStyle == AttackLungeStyle.RANGED || lungeStyle == AttackLungeStyle.BUFF || lungeStyle == AttackLungeStyle.SNACK -> -1f
                                    else -> 1f
                                }
                                Lungeable(
                                    side = CombatSide.ENEMY,
                                    triggerToken = enemyLungeToken,
                                    distance = dist,
                                    axis = if (lungeStyle == AttackLungeStyle.ITEM) LungeAxis.X else LungeAxis.Y,
                                    directionSign = sign,
                                    modifier = Modifier.matchParentSize(),
                                    onFinished = { enemyLungeToken?.let(onLungeFinished) }
                                ) {
                                    Image(
                                        painter = painter,
                                        contentDescription = enemy.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }
                            val enemyShielded = enemyState?.statusEffects.orEmpty().any { status ->
                                isShieldVisualStatus(status.id)
                            }
                            val enemyBroken = (enemyState?.breakTurns ?: 0) > 0
                            if (enemyShielded && isAlive) {
                                ShieldFieldOverlay(modifier = Modifier.matchParentSize())
                            }
                            if (enemyBroken && isAlive) {
                                BrokenFieldOverlay(modifier = Modifier.matchParentSize())
                            }
                            CombatFxOverlay(
                                damageFx = damageFx.filter { it.targetId == combatantId },
                                attackFx = attackFx.filter { it.targetId == combatantId },
                                healFx = healFx.filter { it.targetId == combatantId },
                                statusFx = statusFx.filter { it.targetId == combatantId },
                                shieldBreakFx = shieldBreakFx.filter { it.targetId == combatantId },
                                showKnockout = knockoutFx.any { it.targetId == combatantId },
                                shape = shape,
                                telegraphFx = telegraphHighlights,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
                underSpriteEntries.forEach { entry ->
                    val enemy = entry.enemy
                    val combatantId = entry.combatantId
                    val enemyState = combatState.combatants[combatantId]
                    val isAlive = enemyState?.isAlive != false
                    val isVisible = isAlive || delayedDeathTargets.containsKey(combatantId)
                    val partOffsetX = baseSize * entry.layout.offsetX + groupOffsetXDp
                    val labelOffsetY = underLabelOffsetY
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(180)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(220)),
                        exit = fadeOut(animationSpec = tween(240)) +
                            scaleOut(targetScale = 0.9f, animationSpec = tween(260)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = partOffsetX, y = labelOffsetY)
                            .zIndex(entry.layout.z + 1.2f),
                        label = "enemy_part_label_$combatantId"
                    ) {
                        CompositePartStatus(
                            entry = entry,
                            combatState = combatState,
                            activeId = activeId,
                            selectedEnemyIds = selectedEnemyIds,
                            atbMeters = atbMeters,
                            labelBorderColor = labelBorderColor,
                            onEnemyTap = onEnemyTap,
                            onEnemyLongPress = onEnemyLongPress
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompositePartStatus(
    entry: CompositeEnemyEntry,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    atbMeters: Map<String, Float>,
    labelBorderColor: Color,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit
) {
    val combatantId = entry.combatantId
    val enemyState = combatState.combatants[combatantId]
    val maxHp = enemyState?.combatant?.stats?.maxHp ?: entry.enemy.hp.coerceAtLeast(1)
    val currentHp = enemyState?.hp ?: maxHp
    val maxStability = enemyState?.combatant?.stats?.stability ?: 100
    val currentStability = enemyState?.stability ?: maxStability
    val isAlive = enemyState?.isAlive != false
    val isActive = combatantId == activeId
    val isSelected = combatantId in selectedEnemyIds
    val atbProgress = atbMeters[combatantId] ?: 0f
    Box(contentAlignment = Alignment.Center) {
        EnemyStatusLabel(
            name = entry.enemy.name,
            currentHp = currentHp,
            maxHp = maxHp,
            currentStability = currentStability,
            maxStability = maxStability,
            breakTurns = enemyState?.breakTurns ?: 0,
            atbProgress = atbProgress,
            isAlive = isAlive,
            isActive = isActive,
            isSelected = isSelected,
            accentColor = labelBorderColor,
            barWidth = 90.dp,
            onClick = { if (isAlive) onEnemyTap(combatantId) },
            onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
        )
        EnemyStatusRail(
            statuses = enemyState?.statusEffects.orEmpty(),
            buffs = enemyState?.buffs.orEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 18.dp)
                .zIndex(2f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnemyStatusLabel(
    name: String,
    currentHp: Int,
    maxHp: Int,
    currentStability: Int,
    maxStability: Int,
    breakTurns: Int,
    atbProgress: Float,
    isAlive: Boolean,
    isActive: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    barWidth: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val baseAccent = accentColor.copy(alpha = 1f)
    val borderColor = when {
        isActive -> baseAccent
        isSelected -> baseAccent.copy(alpha = 0.7f)
        else -> baseAccent.copy(alpha = 0.4f)
    }
    val isBroken = breakTurns > 0
    val displayedStability = remember { Animatable(currentStability.toFloat()) }
    LaunchedEffect(currentStability, maxStability, breakTurns) {
        val target = if (isBroken) 0f else currentStability.toFloat()
        if (isBroken) {
            displayedStability.snapTo(0f)
        } else {
            val start = displayedStability.value
            val clampedTarget = target.coerceIn(0f, maxStability.toFloat())
            if (start != clampedTarget) {
                displayedStability.animateTo(
                    clampedTarget,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                )
            }
        }
    }
    val interactionModifier = if (onClick != null || onLongClick != null) {
        modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
    } else {
        modifier
    }
    Surface(
        color = Color(0xFF0C0F14).copy(alpha = if (isSelected) 0.72f else 0.5f),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = interactionModifier
            .height(66.dp)
            .alpha(if (isAlive) 1f else 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleCaseName(name),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AtbBar(
                progress = atbProgress,
                modifier = Modifier.width(barWidth),
                color = if (isActive) Color(0xFFFFD36E) else Color(0xFFFF9F43)
            )
            StatBar(
                current = currentHp,
                max = maxHp,
                color = Color(0xFFFF5252),
                background = Color.Black.copy(alpha = 0.5f),
                height = 6.dp,
                modifier = Modifier.width(barWidth)
            )
            Row(
                modifier = Modifier.width(barWidth),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (isBroken) Color(0xFFFF8A80) else Color(0xFFB39DDB).copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp)
                )
                StatBar(
                    current = displayedStability.value.roundToInt(),
                    max = maxStability,
                    color = if (isBroken) Color(0xFFE57373) else Color(0xFF7E57C2),
                    background = Color.Black.copy(alpha = 0.45f),
                    height = 5.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

fun isEliteTier(tier: String): Boolean {
    return enemyPresentationTier(tier) == EnemyPresentationTier.ELITE
}

fun isBossTier(tier: String): Boolean {
    return enemyPresentationTier(tier) == EnemyPresentationTier.BOSS
}

@Composable
fun rememberDamageFlash(
    targetId: String,
    log: List<CombatLogEntry>
): Float {
    val lastDamage = log.lastOrNull { entry ->
        entry is CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(lastDamage) {
        if (lastDamage != null) {
            anim.snapTo(1f)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 350))
        }
    }
    return anim.value
}

@Composable
fun rememberDamageShake(
    targetId: String,
    log: List<CombatLogEntry>
): Float {
    val damageIndex = log.indexOfLast {
        it is CombatLogEntry.Damage &&
            it.targetId == targetId &&
            !(it.amount == 0 && it.element == "miss")
    }
    val lastIndex = remember { mutableStateOf(-1) }
    val anim = remember { Animatable(0f) }
    val direction = remember { mutableStateOf(1f) }
    LaunchedEffect(damageIndex) {
        if (damageIndex >= 0 && damageIndex > lastIndex.value) {
            lastIndex.value = damageIndex
            direction.value = if (Random.nextBoolean()) 1f else -1f
            anim.snapTo(1f)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 320, easing = EaseOutBack))
        }
    }
    return anim.value * direction.value
}

@Composable
fun rememberHitRecoil(
    targetId: String,
    log: List<CombatLogEntry>,
    directionSign: Float
): Float {
    val lastHit = log.lastOrNull { entry ->
        entry is CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    } as? CombatLogEntry.Damage
    val hitIndex = log.indexOfLast { entry ->
        entry is CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    }
    val lastIndex = remember { mutableStateOf(-1) }
    val anim = remember { Animatable(0f) }
    val density = LocalDensity.current
    val critical = lastHit?.critical == true
    val distance = if (critical) 12.dp else 8.dp
    val holdMs = if (critical) 90L else 60L
    LaunchedEffect(hitIndex) {
        if (hitIndex >= 0 && hitIndex > lastIndex.value) {
            lastIndex.value = hitIndex
            anim.snapTo(1f)
            delay(holdMs)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
        }
    }
    val distancePx = with(density) { distance.toPx() }
    return anim.value * distancePx * directionSign
}

@Composable
fun rememberHitPulse(
    targetId: String,
    log: List<CombatLogEntry>
): Float {
    val lastHit = log.lastOrNull { entry ->
        entry is CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    } as? CombatLogEntry.Damage
    val hitIndex = log.indexOfLast { entry ->
        entry is CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    }
    val lastIndex = remember { mutableStateOf(-1) }
    val anim = remember { Animatable(0f) }
    val critical = lastHit?.critical == true
    val holdMs = if (critical) 90L else 60L
    val returnMs = if (critical) 240 else 200
    LaunchedEffect(hitIndex) {
        if (hitIndex >= 0 && hitIndex > lastIndex.value) {
            lastIndex.value = hitIndex
            anim.snapTo(1f)
            delay(holdMs)
            anim.animateTo(0f, animationSpec = tween(durationMillis = returnMs, easing = FastOutSlowInEasing))
        }
    }
    return anim.value
}

fun isShieldVisualStatus(statusId: String): Boolean {
    val normalized = statusId.trim().lowercase(Locale.getDefault())
    return normalized == "invulnerable" ||
        normalized == "shield" ||
        normalized == "guard" ||
        normalized == "defend"
}

@Composable
fun EnemyShadow(
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.62f,
    heightFraction: Float = 0.18f,
    liftFraction: Float = 0f,
    dropFraction: Float = 0f,
    alpha: Float = 0.35f
) {
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val shadowWidth = size.width * widthFraction
        val shadowHeight = size.height * heightFraction
        val topLeft = Offset(
            (size.width - shadowWidth) / 2f,
            size.height - shadowHeight - size.height * liftFraction + size.height * dropFraction
        )
        drawOval(
            color = Color.Black.copy(alpha = alpha * 0.45f),
            topLeft = topLeft,
            size = Size(shadowWidth, shadowHeight)
        )
        drawOval(
            color = Color.Black.copy(alpha = alpha),
            topLeft = topLeft + Offset(shadowWidth * 0.1f, shadowHeight * 0.12f),
            size = Size(shadowWidth * 0.8f, shadowHeight * 0.7f)
        )
    }
}

@Composable
fun SelectionRipple(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    color: Color = TargetRippleColor
) {
    val rippleAnim = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(
                1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        } else {
            rippleAnim.snapTo(0f)
        }
    }
    val progress = rippleAnim.value
    if (progress <= 0f) return
    Canvas(modifier = modifier) {
        val minDimension = size.minDimension
        if (minDimension <= 0f) return@Canvas
        val t = progress.coerceIn(0f, 1f)
        val center = Offset(size.width / 2f, size.height * 0.82f)
        val radius = minDimension * (0.16f + 0.34f * t)
        val stroke = minDimension * (0.024f - 0.012f * t)
        val alpha = (1f - t).coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = 0.65f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = color.copy(alpha = 0.35f * alpha),
            radius = radius * 0.65f,
            center = center,
            style = Stroke(width = stroke * 0.6f)
        )
    }
}

@Composable
fun StatBar(
    current: Int,
    max: Int,
    color: Color,
    background: Color,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val ratio = if (max <= 0) 0f else current.coerceAtLeast(0).coerceAtMost(max).toFloat() / max.toFloat()
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
    ) {
        val clampedRatio = ratio.coerceIn(0f, 1f)
        if (clampedRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedRatio)
                    .align(Alignment.CenterStart)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val corner = CornerRadius(size.height, size.height)
                    val bodyBrush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.6f),
                            color.copy(alpha = 0.85f),
                            color
                        )
                    )
                    drawRoundRect(
                        brush = bodyBrush,
                        size = size,
                        cornerRadius = corner
                    )
                    val tipWidth = size.width.coerceAtMost(size.height * 1.5f)
                    if (tipWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0f)
                                )
                            ),
                            topLeft = Offset(size.width - tipWidth, 0f),
                            size = Size(tipWidth, size.height),
                            cornerRadius = corner
                        )
                    }
                }
            }
        }
    }
}
