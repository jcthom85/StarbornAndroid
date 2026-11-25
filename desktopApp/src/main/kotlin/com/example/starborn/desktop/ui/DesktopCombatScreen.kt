package com.example.starborn.desktop.ui

import com.example.starborn.domain.environment.EnvironmentalGeometry

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.combat.*
import com.example.starborn.feature.combat.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import com.example.starborn.data.local.UserSettings
import com.example.starborn.feature.combat.presentation.*

internal fun resistanceValueForElement(profile: ResistanceProfile, element: String): Int =
    when (element.trim().lowercase()) {
        "burn", "fire" -> profile.burn
        "freeze", "ice" -> profile.freeze
        "shock", "electric", "lightning" -> profile.shock
        "acid", "erosion", "corrosion" -> profile.acid
        "source", "harmonic" -> profile.source
        "physical", "kinetic" -> profile.physical
        else -> 0
    }

/** Presentation only: all commands and rewards are resolved by the shared combat controller. */
@Composable
fun DesktopCombatScreen(
    services: DesktopAppServices,
    enemyIds: List<String>,
    onVictory: (com.example.starborn.navigation.CombatResultPayload) -> Unit,
    onDefeat: () -> Unit,
    onFlee: () -> Unit
) {
    MaterialTheme(typography = desktopStarbornTypography(services)) {
        DesktopCombatContent(services, enemyIds, onVictory, onDefeat, onFlee)
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun DesktopCombatContent(
    services: DesktopAppServices,
    enemyIds: List<String>,
    onVictory: (com.example.starborn.navigation.CombatResultPayload) -> Unit,
    onDefeat: () -> Unit,
    onFlee: () -> Unit
) {
    val initialSettings by produceState<UserSettings?>(null, services) { value = services.userSettingsStore.settings.first() }
    val loadedSettings = initialSettings ?: return
    val settings by services.userSettingsStore.settings.collectAsState(initial = loadedSettings)
    val scope = rememberCoroutineScope()
    val runtime = remember(services, enemyIds) {
        CombatController(services.worldDataSource, services.combatEngine, services.statusRegistry,
            services.sessionStore, services.inventoryService, services.itemRepository,
            services.levelingManager, services.progressionData, services.audioRouter,
            services.themeRepository, services.environmentThemeManager, services.encounterCoordinator,
            enemyIds, tutorialsEnabled = loadedSettings.tutorialsEnabled, parentScope = scope, telemetry = services.playtestTelemetry)
    }
    DisposableEffect(runtime) { onDispose { runtime.close() } }
    val foreground = LocalDesktopForeground.current
    val state by runtime.state.collectAsState()
    val actorId by runtime.awaitingAction.collectAsState()
    val inventory by runtime.inventory.collectAsState()
    val focusedEnemies by runtime.selectedEnemies.collectAsState()
    val weaknessKnowledge by runtime.weaknessKnowledge.collectAsState()
    val meters by runtime.atbMeters.collectAsState()
    val banner by runtime.combatBanner.collectAsState()
    val tutorial by runtime.combatTutorial.collectAsState()
    val buildHint by runtime.buildHint.collectAsState()
    val timedPrompt by runtime.timedPrompt.collectAsState()
    val lungeStyle by runtime.lungeStyle.collectAsState()
    val lungeToken by runtime.lungeToken.collectAsState()
    val missToken by runtime.missLungeToken.collectAsState()
    val lungeActor by runtime.lungeActorId.collectAsState()
    val missActor by runtime.missLungeActorId.collectAsState()
    var showLog by remember { mutableStateOf(false) }
    var showPauseSettings by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(runtime, foreground, showLog, showPauseSettings) {
        runtime.setBackgroundPaused(!foreground || showLog || showPauseSettings)
    }
    var targetRequirement by remember { mutableStateOf(TargetRequirement.NONE) }
    var targetAction by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var instruction by remember { mutableStateOf<String?>(null) }
    var lastEffect by remember { mutableStateOf<String?>(null) }
    var hoveredEnemyId by remember { mutableStateOf<String?>(null) }
    val intents = remember { mutableStateMapOf<String, String>() }
    val feedback = remember { mutableStateMapOf<String, String>() }
    val cues = remember(runtime) { mutableStateListOf<DesktopCombatCue>() }
    var cueToken by remember(runtime) { mutableLongStateOf(0) }
    val history = remember(runtime) { mutableStateListOf<String>() }
    val probedElementsByEnemyType = remember(runtime) { mutableStateMapOf<String, Set<String>>() }
    val playerIdSet = remember(runtime.playerParty) { runtime.playerParty.map { it.id.lowercase() }.toSet() }
    val stageShakeX = remember { Animatable(0f) }
    val stageShakeY = remember { Animatable(0f) }
    fun triggerStageShake(heavy: Boolean) {
        if (settings.disableScreenshake) return
        val amp = if (heavy) 8.5f else 4.8f
        scope.launch {
            stageShakeX.snapTo(amp)
            stageShakeX.animateTo(-amp * 0.55f, tween(45, easing = LinearEasing))
            stageShakeX.animateTo(amp * 0.28f, tween(45, easing = LinearEasing))
            stageShakeX.animateTo(0f, tween(55, easing = FastOutSlowInEasing))
        }
        scope.launch {
            stageShakeY.snapTo(-amp * 0.42f)
            stageShakeY.animateTo(amp * 0.22f, tween(55, easing = LinearEasing))
            stageShakeY.animateTo(0f, tween(75, easing = FastOutSlowInEasing))
        }
    }
    fun name(id: String) = runtime.state.value?.combatants?.get(id)?.combatant?.name ?: services.contentName(id)
    fun record(message: String) { history.add(message); if (history.size > 60) history.removeAt(0) }
    fun showCue(
        target: String,
        label: String,
        color: Color,
        style: AttackLungeStyle? = null,
        strong: Boolean = false,
        kind: DesktopCueKind = DesktopCueKind.HIT,
        amount: Int? = null,
        critical: Boolean = false,
        isWeakness: Boolean = false,
        isVantage: Boolean = false,
        isBrokenBonus: Boolean = false,
        isGuardBreak: Boolean = false,
        element: String? = null,
        sourceId: String? = null,
        statusId: String? = null,
        heroFxStyle: DesktopAttackFxStyle? = null
    ) {
        val cue = DesktopCombatCue(
            token = ++cueToken,
            targetId = target,
            label = label,
            color = color,
            style = style,
            strong = strong,
            kind = kind,
            amount = amount,
            critical = critical,
            isWeakness = isWeakness,
            isVantage = isVantage,
            isBrokenBonus = isBrokenBonus,
            isGuardBreak = isGuardBreak,
            element = element,
            sourceId = sourceId,
            statusId = statusId,
            heroFxStyle = heroFxStyle
        )
        cues.add(cue)
        scope.launch { delay(if (cue.isGuardBreak) 1900L else 1500L); cues.remove(cue) }
    }
    val keyboardFocus = remember { FocusRequester() }
    var keyboardTarget by remember { mutableStateOf(0) }
    LaunchedEffect(targetAction) { keyboardTarget = 0 }

    LaunchedEffect(runtime) {
        runtime.fxEvents.collect { effect ->
            when (effect) {
                is CombatFxEvent.Audio -> services.audioDriver.executeAll(effect.commands)
                is CombatFxEvent.Telegraph -> {
                    val targets = effect.targetIds.joinToString { name(it) }
                    val label = effect.skillName + if (targets.isNotBlank()) " -> $targets" else ""
                    effect.targetIds.forEach { showCue(it, effect.skillName, Color(0xFFFFBB55), kind = DesktopCueKind.TELEGRAPH) }
                    if (runtime.enemies.firstOrNull { it.id == runtime.enemyTypeId(effect.actorId) }?.authored == null)
                        intents[effect.actorId] = label
                    record("${name(effect.actorId)} prepares $label")
                }
                is CombatFxEvent.Impact -> {
                    val targetState = runtime.state.value?.combatants?.get(effect.targetId)
                    val targetProfile = targetState?.combatant?.resistances
                    val hitElement = effect.element
                    if (hitElement != null && targetState?.combatant?.side == CombatSide.ENEMY) {
                        val enemyType = runtime.enemyTypeId(effect.targetId)
                        if (enemyType != null) {
                            val normalizedEl = hitElement.trim().lowercase()
                            probedElementsByEnemyType[enemyType] = probedElementsByEnemyType[enemyType].orEmpty() + normalizedEl
                        }
                    }
                    val isVantageHit = !effect.isWeakness && hitElement != null && targetProfile != null &&
                        ElementalAffinityRules.tierForValue(resistanceValueForElement(targetProfile, hitElement)) == AffinityTier.VANTAGE
                    val tags = listOfNotNull(
                        "CRITICAL".takeIf { effect.critical },
                        "WEAKNESS".takeIf { effect.isWeakness },
                        "VANTAGE".takeIf { isVantageHit },
                        "BROKEN BONUS".takeIf { effect.isBrokenBonus },
                        "GUARD BREAK".takeIf { effect.isGuardBreak },
                        effect.element?.replace('_', ' ')?.uppercase()
                    )
                    val label = "-${effect.amount}" + tags.joinToString(" / ", prefix = if (tags.isEmpty()) "" else " ")
                    val weapon = runtime.state.value?.combatants?.get(effect.sourceId)?.combatant?.weapon
                    val style = when (weapon?.weaponType) {
                        "gun" -> AttackLungeStyle.RANGED
                        "jewel" -> AttackLungeStyle.CAST
                        "glove", "sword" -> AttackLungeStyle.MELEE
                        else -> runtime.lungeStyle.value
                    }
                    val heroFxStyle = if (effect.showAttackFx) {
                        resolveDesktopAttackFxStyle(effect.sourceId, runtime.state.value, playerIdSet)
                    } else null
                    if (effect.critical || effect.isGuardBreak) {
                        triggerStageShake(effect.isGuardBreak)
                    }
                    showCue(
                        target = effect.targetId,
                        label = label,
                        color = combatElementColor(effect.element),
                        style = style.takeIf { effect.showAttackFx },
                        strong = effect.critical || effect.isGuardBreak,
                        kind = DesktopCueKind.HIT,
                        amount = effect.amount,
                        critical = effect.critical,
                        isWeakness = effect.isWeakness,
                        isVantage = isVantageHit,
                        isBrokenBonus = effect.isBrokenBonus,
                        isGuardBreak = effect.isGuardBreak,
                        element = effect.element,
                        sourceId = effect.sourceId,
                        heroFxStyle = heroFxStyle
                    )
                    intents.remove(effect.sourceId)
                    record("${name(effect.targetId)}: $label")
                    lastEffect = "${name(effect.targetId)}: $label"
                }
                is CombatFxEvent.Heal -> {
                    showCue(
                        target = effect.targetId,
                        label = "+${effect.amount} HP",
                        color = Color(0xFF80E7A0),
                        style = AttackLungeStyle.BUFF,
                        kind = DesktopCueKind.HEAL,
                        amount = effect.amount
                    )
                    record("${name(effect.targetId)} recovers ${effect.amount} HP")
                }
                is CombatFxEvent.StatusApplied -> {
                    val label = effect.displayLabel ?: services.contentName(effect.statusId)
                    showCue(effect.targetId, label, Color(0xFFD3A1FF), AttackLungeStyle.CAST, kind = DesktopCueKind.STATUS, statusId = effect.statusId)
                    record("${name(effect.targetId)}: $label")
                }
                is CombatFxEvent.SupportCue -> {
                    showCue(effect.actorId, effect.skillName, Color(0xFF80E7A0), AttackLungeStyle.BUFF, kind = DesktopCueKind.SUPPORT)
                    effect.targetIds.filter { it != effect.actorId }.forEach { showCue(it, effect.skillName, Color(0xFF80E7A0), AttackLungeStyle.BUFF, kind = DesktopCueKind.SUPPORT) }
                    record("${name(effect.actorId)}: ${effect.skillName}")
                }
                is CombatFxEvent.ShieldBreak -> {
                    triggerStageShake(true)
                    showCue(effect.targetId, "STABILITY BREAK!", Color(0xFF00E5FF), AttackLungeStyle.CAST, true, DesktopCueKind.BREAK)
                    record("${name(effect.targetId)}: stability broken")
                }
                is CombatFxEvent.Knockout -> {
                    showCue(effect.targetId, "DEFEATED", Color(0xFFBECAD0), kind = DesktopCueKind.KNOCKOUT)
                    feedback[effect.targetId] = "DEFEATED"
                    intents.remove(effect.targetId)
                    record("${name(effect.targetId)} defeated")
                }
                is CombatFxEvent.TurnQueued -> {
                    showCue(effect.actorId, "ACTING", Color(0xFF63E6FF))
                }
                is CombatFxEvent.CombatOutcomeFx -> {
                    lastEffect = effect.outcome.name.lowercase().replaceFirstChar { it.uppercase() }
                    record(lastEffect.orEmpty())
                }
            }
        }
    }
    LaunchedEffect(runtime) { runtime.onScreenReady() }
    // Completion acknowledgment cannot depend on an animation being visible or the window staying focused.
    LaunchedEffect(lungeToken) { if (lungeToken > 0) { delay(360); runtime.onLungeFinished(lungeToken) } }
    LaunchedEffect(missToken) {
        if (missToken > 0) {
            missActor?.let { showCue(it, "MISS", Color(0xFFCDD6DC)); record("${name(it)}: miss") }
            delay(360); runtime.onMissLungeFinished(missToken)
        }
    }
    LaunchedEffect(actorId) { menu = null; targetAction = null; instruction = null }

    fun chooseTarget(requirement: TargetRequirement, label: String, action: (String?) -> Unit) {
        menu = null
        if (requirement == TargetRequirement.NONE) { action(null); return }
        targetRequirement = requirement
        instruction = label
        targetAction = { target -> targetAction = null; instruction = null; action(target) }
    }
    fun attack() {
        if (actorId != null && timedPrompt == null && targetAction == null && runtime.onCombatTutorialCommand("attack")) {
            chooseTarget(TargetRequirement.ENEMY, "Choose an enemy for Attack") { runtime.playerAttack(it) }
        }
    }
    val battle = state ?: return
    LaunchedEffect(battle.outcome, timedPrompt) {
        if (battle.outcome != null || timedPrompt != null) showPauseSettings = false
    }
    LaunchedEffect(menu, showPauseSettings, showLog) {
        if (menu == null && !showPauseSettings && !showLog) {
            withFrameNanos { }
            keyboardFocus.requestFocus()
        }
    }
    val party = battle.combatants.values.filter { it.combatant.side != CombatSide.ENEMY }
    val enemies = battle.combatants.values.filter { it.combatant.side == CombatSide.ENEMY }
    val actor = battle.combatants[actorId]

    // PC Ergonomics: Auto-select ready player character when turn meter is full
    LaunchedEffect(actorId, meters, timedPrompt, targetAction, menu, battle.outcome) {
        if (actorId == null && timedPrompt == null && targetAction == null && menu == null && battle.outcome == null && tutorial?.showsModal != true) {
            val readyMember = party.firstOrNull { (meters[it.combatant.id] ?: 0f) >= 0.999f && it.isAlive }
            if (readyMember != null) {
                runtime.selectReadyPlayer(readyMember.combatant.id)
            }
        }
    }

    var victoryPayload by remember(runtime) { mutableStateOf<com.example.starborn.navigation.CombatResultPayload?>(null) }
    LaunchedEffect(runtime, battle.outcome) {
        val outcome = battle.outcome as? CombatOutcome.Victory ?: return@LaunchedEffect
        val summaries = runtime.consumeLevelUpSummaries()
        // Let the victory pose settle before presenting rewards and victory stinger.
        delay(2000)
        runtime.playVictoryMusic()
        victoryPayload = com.example.starborn.navigation.CombatResultPayload(
            outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
            enemyIds = runtime.encounterEnemyIds, roomId = runtime.encounterRoomId,
            sourcePartyId = runtime.encounterSourcePartyId,
            rewardXp = outcome.rewards.xp, rewardAp = outcome.rewards.ap,
            rewardCredits = outcome.rewards.credits,
            rewardItems = outcome.rewards.drops.groupBy { it.itemId }.mapValues { (_, drops) -> drops.sumOf { it.quantity } },
            levelUps = summaries)
    }
    val targetCandidates = when (targetRequirement) {
        TargetRequirement.ENEMY -> enemies.filter { it.isAlive }
        TargetRequirement.ALLY -> party.filter { it.isAlive }
        TargetRequirement.ANY -> party.filter { it.isAlive } + enemies.filter { it.isAlive }
        else -> emptyList()
    }.filter { runtime.isCombatTutorialTargetEnabled(it.combatant.id) }
    fun openSkills() { if (actor != null && timedPrompt == null && targetAction == null && runtime.onCombatTutorialCommand("skills")) menu = "skills" }
    fun openItems() { if (actor != null && timedPrompt == null && targetAction == null && runtime.isCombatTutorialCommandEnabled("items")) menu = "items" }

    fun cancelActiveSelectionOrMenu(): Boolean {
        if (menu != null || targetAction != null) {
            if (menu != null) runtime.onCombatTutorialSkillDialogDismissed()
            if (targetAction != null) runtime.onCombatTutorialTargetCancelled()
            menu = null
            targetAction = null
            instruction = null
            return true
        }
        return false
    }

    Box(Modifier.fillMaxSize().focusRequester(keyboardFocus).pointerInput(menu, targetAction) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                    if (cancelActiveSelectionOrMenu()) {
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }.onPreviewKeyEvent { event ->
        if (battle.outcome != null) false else if (event.type == KeyEventType.KeyDown && (event.key == Key.Escape || (showLog && event.key == Key.L))) {
            when {
                showPauseSettings -> {
                    showPauseSettings = false
                    true
                }
                showLog -> {
                    showLog = false
                    true
                }
                cancelActiveSelectionOrMenu() -> true
                event.key == Key.Escape && timedPrompt == null && tutorial?.showsModal != true -> {
                    showPauseSettings = true
                    true
                }
                else -> false
            }
        } else if (event.type != KeyEventType.KeyDown || !foreground || showLog || showPauseSettings || battle.outcome != null) false else {
            if (menu == "skills") {
                val numKeyIndex = when (event.key) {
                    Key.One -> 0; Key.Two -> 1; Key.Three -> 2; Key.Four -> 3
                    Key.Five -> 4; Key.Six -> 5; Key.Seven -> 6; Key.Eight -> 7; Key.Nine -> 8
                    else -> -1
                }
                val skills = runtime.activePlayerSkills()
                if (numKeyIndex in skills.indices) {
                    val skill = skills[numKeyIndex]
                    if (runtime.canUseSkill(requireNotNull(actorId), skill) && (tutorial?.step != CombatTutorialStep.CHOOSE_HYDRAULIC_KICK || skill.id == tutorial?.expectedSkillId)) {
                        if (runtime.onCombatTutorialSkillSelected(skill.id)) {
                            chooseTarget(runtime.targetRequirementFor(skill), "Choose a target for ${skill.name}") { target ->
                                runtime.useSkill(skill, target?.let(::listOf))
                            }
                        }
                    }
                    true
                } else if (event.key == Key.Backspace) {
                    cancelActiveSelectionOrMenu()
                } else false
            } else if (menu == "items") {
                val numKeyIndex = when (event.key) {
                    Key.One -> 0; Key.Two -> 1; Key.Three -> 2; Key.Four -> 3
                    Key.Five -> 4; Key.Six -> 5; Key.Seven -> 6; Key.Eight -> 7; Key.Nine -> 8
                    else -> -1
                }
                val usable = inventory.filter(CombatItemPresentation::isUsable)
                if (numKeyIndex in usable.indices) {
                    val entry = usable[numKeyIndex]
                    val requirement = CombatItemPresentation.targetRequirement(entry)
                    chooseTarget(requirement, "Choose a target for ${entry.item.name}") { runtime.useItem(entry, it) }
                    true
                } else if (event.key == Key.Backspace) {
                    cancelActiveSelectionOrMenu()
                } else false
            } else if (menu != null) {
                if (event.key == Key.Backspace) { cancelActiveSelectionOrMenu(); true } else false
            } else when (event.key) {
                Key.Spacebar -> {
                    if (timedPrompt != null) {
                        runtime.registerTimedPromptTap()
                        true
                    } else if (targetAction != null && targetCandidates.isNotEmpty()) {
                        targetAction?.invoke(targetCandidates[keyboardTarget.coerceIn(0, targetCandidates.lastIndex)].combatant.id)
                        true
                    } else false
                }
                Key.Escape -> cancelActiveSelectionOrMenu()
                Key.G -> {
                    if (actorId != null && targetAction == null && timedPrompt == null && tutorial == null) runtime.guard()
                    true
                }
                Key.Four -> {
                    if (actorId != null && timedPrompt == null && targetAction == null && runtime.canUseSnack(requireNotNull(actorId)) && runtime.isCombatTutorialCommandEnabled("snack")) {
                        chooseTarget(runtime.snackTargetRequirement(requireNotNull(actorId)), "Choose a snack target") { runtime.useSnack(it) }
                    }; true
                }
                Key.L -> { if (timedPrompt == null) showLog = true; true }
                Key.R -> { if (actor != null && timedPrompt == null && targetAction == null && runtime.isCombatTutorialCommandEnabled("retreat")) runtime.attemptRetreat(); true }
                Key.One -> { attack(); true }
                Key.Two -> { openSkills(); true }
                Key.Three -> { openItems(); true }
                Key.DirectionLeft, Key.DirectionUp, Key.DirectionRight, Key.DirectionDown -> {
                    if (targetAction != null && targetCandidates.isNotEmpty()) {
                        val delta = if (event.key == Key.DirectionLeft || event.key == Key.DirectionUp) -1 else 1
                        keyboardTarget = Math.floorMod(keyboardTarget + delta, targetCandidates.size)
                        true
                    } else false
                }
                Key.Enter -> {
                    if (targetAction != null && targetCandidates.isNotEmpty()) {
                        targetAction?.invoke(targetCandidates[keyboardTarget.coerceIn(0, targetCandidates.lastIndex)].combatant.id)
                        true
                    } else false
                }
                Key.Tab -> {
                    if (targetAction != null && targetCandidates.isNotEmpty()) {
                        val delta = if (event.isShiftPressed) -1 else 1
                        keyboardTarget = Math.floorMod(keyboardTarget + delta, targetCandidates.size)
                        true
                    } else {
                        val ready = party.filter { (meters[it.combatant.id] ?: 0f) >= 1f && it.isAlive }
                        if (timedPrompt == null && targetAction == null && ready.isNotEmpty()) {
                            val next = (ready.indexOfFirst { it.combatant.id == actorId } + 1) % ready.size
                            runtime.selectReadyPlayer(ready[next].combatant.id); true
                        } else false
                    }
                }
                else -> false
            }
        }
    }.focusable()) {
        val selectedTarget = if (targetAction != null) targetCandidates.getOrNull(keyboardTarget)?.combatant?.id else null
        val validIds = if (targetAction != null) targetCandidates.map { it.combatant.id }.toSet() else emptySet()
        val inspectedEnemyId = selectedTarget ?: hoveredEnemyId ?: focusedEnemies.firstOrNull()
        val context: @Composable () -> Unit = {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DesktopMenuCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        runtime.locationTitle?.let { Text(it, color = Color(0xFF91A8B3), style = MaterialTheme.typography.labelMedium) }
                        Text(runtime.encounterTitle, color = Color(0xFFFFBB55), style = MaterialTheme.typography.headlineSmall)
                        banner?.primary?.let { Text(it, color = Color(0xFF63E6FF)) }
                        banner?.secondary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                tutorial?.takeUnless { it.showsModal }?.let { training ->
                    DesktopCombatTutorialOverlay(training, Color(0xFF7BE4FF), settings.highContrastMode,
                        runtime::onCombatTutorialContinue, runtime::skipCombatTutorial)
                }
                val focused = enemies.firstOrNull { it.combatant.id == inspectedEnemyId }
                focused?.let { target ->
                    val enemyTypeId = runtime.enemyTypeId(target.combatant.id)
                    val enemyDef = runtime.enemies.firstOrNull { it.id == enemyTypeId }
                    val tierNorm = enemyDef?.tier?.lowercase().orEmpty()
                    val (tierLabel, tierColor) = when (tierNorm) {
                        "boss" -> "BOSS THREAT" to Color(0xFFFF8A80)
                        "elite", "miniboss" -> "ELITE HOSTILE" to Color(0xFFFFD54F)
                        else -> "HOSTILE" to Color(0xFF63E6FF)
                    }
                    val thumbPath = enemyDef?.portrait?.takeIf { it.isNotBlank() }
                        ?: enemyDef?.sprite?.firstOrNull()?.takeIf { it.isNotBlank() }
                        ?: "images/enemies/${enemyTypeId}_combat.png"
                    val maxHp = target.combatant.stats.maxHp.coerceAtLeast(1)
                    val maxStability = target.combatant.stats.stability.coerceAtLeast(1)
                    val isBroken = target.isAlive && target.breakTurns > 0
                    DesktopMenuCard(Modifier.fillMaxWidth().testTag("combat-enemy-inspection")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF101924),
                                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.55f))
                                ) {
                                    Image(
                                        painter = rememberDesktopAssetPainter(thumbPath, services.assetProvider),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        tierLabel,
                                        color = tierColor,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        target.combatant.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (target.combatant.id == hoveredEnemyId || target.combatant.id == selectedTarget) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF63E6FF).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            if (target.combatant.id == selectedTarget) "TARGET LOCK" else "INSPECTED",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = Color(0xFF63E6FF),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("HP ${target.hp}/$maxHp", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    LinearProgressIndicator(
                                        progress = { (target.hp.toFloat() / maxHp).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                                        color = Color(0xFFFF5252),
                                        trackColor = Color(0xCC070B12)
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Stability ${target.stability}/$maxStability", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    LinearProgressIndicator(
                                        progress = { (if (isBroken) 0f else target.stability.toFloat() / maxStability).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                                        color = if (isBroken) Color(0xFFE57373) else Color(0xFF7E57C2),
                                        trackColor = Color(0xCC070B12)
                                    )
                                }
                            }
                            if (isBroken) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE57373).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFFFF8A80).copy(alpha = 0.65f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "STABILITY BROKEN · ${target.breakTurns} ${if (target.breakTurns == 1) "turn" else "turns"} remaining (+25% damage)",
                                        color = Color(0xFFFFAB91),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            intents[target.combatant.id]?.let { Text(it, color = Color(0xFFFFBB55), fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                            val discovered = (weaknessKnowledge.knownEnemyWeaknesses[enemyTypeId].orEmpty() +
                                probedElementsByEnemyType[enemyTypeId].orEmpty()).sorted()
                            val weakElements = discovered.filter {
                                ElementalAffinityRules.tierForValue(resistanceValueForElement(target.combatant.resistances, it)) == AffinityTier.WEAKNESS
                            }
                            val vantageElements = discovered.filter {
                                ElementalAffinityRules.tierForValue(resistanceValueForElement(target.combatant.resistances, it)) == AffinityTier.VANTAGE
                            }
                            val resistElements = discovered.filter {
                                ElementalAffinityRules.tierForValue(resistanceValueForElement(target.combatant.resistances, it)) == AffinityTier.RESIST
                            }
                            val immuneElements = discovered.filter {
                                ElementalAffinityRules.tierForValue(resistanceValueForElement(target.combatant.resistances, it)) == AffinityTier.IMMUNE
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("combat-affinity-radar")) {
                                if (weakElements.isEmpty() && vantageElements.isEmpty() && resistElements.isEmpty() && immuneElements.isEmpty()) {
                                    Text("Weaknesses", color = Color(0xFF63E6FF), fontSize = 10.sp)
                                    Text("Not discovered", color = Color(0xFF91A8B3), fontSize = 12.sp)
                                } else {
                                    if (weakElements.isNotEmpty()) {
                                        Text("Weaknesses (2× Stability · -1 CD)", color = Color(0xFF63E6FF), fontSize = 10.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            weakElements.forEach { element ->
                                                Surface(shape = RoundedCornerShape(4.dp), color = combatElementColor(element).copy(alpha = .16f)) {
                                                    Text(com.example.starborn.feature.combat.presentation.weaknessLabel(element),
                                                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = combatElementColor(element), fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (vantageElements.isNotEmpty()) {
                                        Text("Vantage (1.5× Stability · -1 CD)", color = Color(0xFFFFC857), fontSize = 10.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            vantageElements.forEach { element ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = combatElementColor(element).copy(alpha = .16f),
                                                    border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.55f))
                                                ) {
                                                    Text(
                                                        com.example.starborn.feature.combat.presentation.weaknessLabel(element),
                                                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        color = combatElementColor(element),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (resistElements.isNotEmpty()) {
                                        Text("Resists (Reduced Damage)", color = Color(0xFF90A4AE), fontSize = 10.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            resistElements.forEach { element ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF90A4AE).copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, Color(0xFF90A4AE).copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        com.example.starborn.feature.combat.presentation.weaknessLabel(element),
                                                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        color = Color(0xFFCFD8DC),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (immuneElements.isNotEmpty()) {
                                        Text("Immunities (No Effect)", color = Color(0xFFEF9A9A), fontSize = 10.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            immuneElements.forEach { element ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFEF9A9A).copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, Color(0xFFEF9A9A).copy(alpha = 0.45f))
                                                ) {
                                                    Text(
                                                        com.example.starborn.feature.combat.presentation.weaknessLabel(element),
                                                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        color = Color(0xFFFFCDD2),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (target.statusEffects.isNotEmpty() || target.buffs.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    target.statusEffects.forEach { DesktopCombatStatusChip(it, services) }
                                    target.buffs.forEach { DesktopCombatBuffChip(it, services) }
                                }
                            }
                            enemyDef?.flavor?.takeIf { it.isNotBlank() }?.let { flavor ->
                                Text(
                                    flavor,
                                    color = Color(0xFF9EB4C4),
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, lineHeight = 16.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                val victoryEmotesActive = battle.outcome is CombatOutcome.Victory && runtime.encounterEnemyIds != listOf("ascended_vale")
                DesktopCombatPartyRoster(
                    party = party,
                    services = services,
                    meters = meters,
                    actorId = actorId,
                    executingId = lungeActor.takeIf { lungeToken > 0 },
                    playerPartyDefs = runtime.playerParty,
                    cues = cues,
                    victoryEmotes = victoryEmotesActive,
                    lungeActor = lungeActor,
                    lungeToken = lungeToken,
                    missActor = missActor,
                    missToken = missToken,
                    authoredState = battle,
                    onSelectActor = { id ->
                        if (targetAction == null && timedPrompt == null && tutorial?.showsModal != true) {
                            runtime.selectReadyPlayer(id)
                        }
                    }
                )
                }
                DesktopCombatLiveFeed(history = history.toList(), onExpandLog = { showLog = true },
                    modifier = Modifier.height(150.dp).testTag("combat-recent-actions"))
            }
        }
        val accentValues = runtime.theme?.accent
        val commandAccent = if (accentValues != null && accentValues.size >= 3) Color(accentValues[0], accentValues[1], accentValues[2], accentValues.getOrElse(3) { 1f }) else Color(0xFFFFBB55)
        val commandColors = MaterialTheme.colorScheme.copy(primary = commandAccent, onPrimary = Color(0xFF050A10))
        val commands: @Composable () -> Unit = {
            MaterialTheme(colorScheme = commandColors) {
            if (actor == null && menu == null && targetAction == null) {
                DesktopCombatReadyPrompt(commandAccent, party.any { it.isAlive && (meters[it.combatant.id] ?: 0f) >= .999f }, settings)
            } else DesktopMenuCard(Modifier.fillMaxWidth().then(if (menu != null) Modifier.fillMaxHeight() else Modifier)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    actor?.let { member ->
                        runtime.playerParty.firstOrNull { it.id == member.combatant.id }?.miniIconPath?.let { path ->
                            Image(rememberDesktopAssetPainter(path, services.assetProvider), null, Modifier.size(64.dp), contentScale = ContentScale.Fit)
                        }
                        Text(member.combatant.name, style = MaterialTheme.typography.headlineSmall, color = Color(0xFFFFBB55))
                        Text("HP ${member.hp}/${member.combatant.stats.maxHp} / Momentum ${member.momentum}", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { (meters[member.combatant.id] ?: 0f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        member.weaponCharge?.let { Text("Charging: ${it.remainingTurns} turns", color = Color(0xFFFFDE70), style = MaterialTheme.typography.bodySmall) }
                        member.buffs.forEach { Text(com.example.starborn.domain.combat.CombatClarity.compactBuffLabel(it), style = MaterialTheme.typography.bodySmall) }
                        member.statusEffects.forEach { Text("${services.contentName(it.id)} (${it.remainingTurns} turns)", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (targetAction != null) {
                        Text(instruction ?: "Choose a target", color = Color(0xFF63E6FF))
                        Text("Selected: " + (targetCandidates.getOrNull(keyboardTarget)?.combatant?.name ?: "No valid target"),
                            color = Color(0xFFFFBB55), modifier = Modifier.testTag("combat-selected-target"))
                        Text("Arrows select  |  Enter confirms  |  Esc cancels", style = MaterialTheme.typography.bodySmall, color = Color(0xFF91A8B3))
                        TextButton(onClick = { cancelActiveSelectionOrMenu() }, modifier = Modifier.desktopPointerHover()) { Text("Cancel") }
                    } else if (menu != null && actorId != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (menu == "skills") "Abilities" else "Items", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { menu = null; runtime.onCombatTutorialSkillDialogDismissed() }) { Text("Back") }
                        }
                    val menuListState = rememberLazyListState()
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(state = menuListState, modifier = Modifier.fillMaxSize().padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (menu == "skills") {
                            item { Text("Cooldowns count this character's turns.", style = MaterialTheme.typography.bodySmall) }
                            if (runtime.activePlayerSkills().isEmpty()) item { Text("No abilities available") }
                            itemsIndexed(runtime.activePlayerSkills(), key = { _, it -> it.id }) { index, skill ->
                                val canUse = runtime.canUseSkill(requireNotNull(actorId), skill) && (tutorial?.step != CombatTutorialStep.CHOOSE_HYDRAULIC_KICK || skill.id == tutorial?.expectedSkillId)
                                val keyGlyph = if (index < 9) "[${index + 1}]" else ""
                                DesktopRichTooltip(
                                    tooltip = {
                                        DesktopSkillTooltipContent(
                                            skillName = skill.name,
                                            description = skill.description,
                                            cooldown = skill.cooldown,
                                            targeting = runtime.skillTargetScopeLabel(skill),
                                            keyGlyph = keyGlyph.takeIf { it.isNotBlank() },
                                            accent = if (canUse) Color(0xFF63E6FF) else Color(0xFF7E8F9B)
                                        )
                                    },
                                    accent = if (canUse) Color(0xFF63E6FF) else Color(0xFF7E8F9B),
                                    maxWidth = 320.dp
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF09141B),
                                        border = BorderStroke(1.dp, if (canUse) Color(0xFF2D4454) else Color(0xFF16232D)),
                                        modifier = Modifier.fillMaxWidth().then(if (canUse) Modifier.desktopPointerHover() else Modifier)
                                    ) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(skill.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (canUse) Color.White else Color(0xFF7E8F9B))
                                                if (keyGlyph.isNotBlank() && canUse) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                                    ) {
                                                        Text(keyGlyph, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(skill.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                                            val elements = skill.combatTags.orEmpty().filter { it in setOf("physical", "burn", "freeze", "shock", "acid", "source") }
                                            if (elements.isNotEmpty()) Text("Element: " + elements.joinToString { com.example.starborn.feature.combat.presentation.weaknessLabel(it) },
                                                color = combatElementColor(elements.first()), style = MaterialTheme.typography.labelMedium)
                                            runtime.skillUnavailableReason(requireNotNull(actorId), skill)?.takeUnless { it == "On cooldown" }?.let { reason ->
                                                Text(reason, color = Color(0xFFFFBB55), style = MaterialTheme.typography.bodySmall)
                                            }
                                            val remaining = runtime.skillCooldownRemaining(requireNotNull(actorId), skill.id)
                                            if (remaining > 0) Text("Ready in ${com.example.starborn.domain.combat.CombatClarity.turns(remaining)}", color = Color(0xFFFFBB55), style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "Target: ${runtime.skillTargetScopeLabel(skill)}  |  Cooldown: ${com.example.starborn.domain.combat.CombatClarity.skillCooldownLabel(skill.cooldown, 0, actor?.momentum ?: 0)}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall
                                            )
                                            Button(
                                                onClick = {
                                                    if (runtime.onCombatTutorialSkillSelected(skill.id)) {
                                                        chooseTarget(runtime.targetRequirementFor(skill), "Choose a target for ${skill.name}") { target ->
                                                            runtime.useSkill(skill, target?.let(::listOf))
                                                        }
                                                    }
                                                },
                                                enabled = canUse,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.align(Alignment.End).desktopPointerHover(canUse)
                                            ) {
                                                Text(if (runtime.targetRequirementFor(skill) == TargetRequirement.NONE) "Use: ${runtime.skillTargetScopeLabel(skill)}" else "Choose target")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val usable = inventory.filter(CombatItemPresentation::isUsable)
                            if (usable.isEmpty()) item { Text("No usable items") }
                            itemsIndexed(usable, key = { _, it -> it.item.id }) { index, entry ->
                                val keyGlyph = if (index < 9) "[${index + 1}]" else ""
                                DesktopRichTooltip(
                                    tooltip = {
                                        DesktopItemTooltipContent(
                                            item = entry.item,
                                            fallbackName = entry.item.name,
                                            quantity = entry.quantity,
                                            services = services
                                        )
                                    },
                                    maxWidth = 300.dp
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF09141B),
                                        border = BorderStroke(1.dp, Color(0xFF2D4454)),
                                        modifier = Modifier.fillMaxWidth().desktopPointerHover()
                                    ) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(entry.item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                                if (keyGlyph.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                                    ) {
                                                        Text(keyGlyph, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            entry.item.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f)) }
                                            com.example.starborn.feature.exploration.presentation.ItemDetails.lines(entry.item, services::contentName).forEach {
                                                Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF91A8B3))
                                            }
                                            Button(
                                                onClick = {
                                                    val requirement = CombatItemPresentation.targetRequirement(entry)
                                                    chooseTarget(requirement, "Choose a target for ${entry.item.name}") { runtime.useItem(entry, it) }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.align(Alignment.End).desktopPointerHover()
                                            ) {
                                                Text("${entry.item.name} ×${entry.quantity}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(menuListState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                        style = ScrollbarStyle(16.dp, 5.dp, RoundedCornerShape(3.dp), 150, Color(0xFF63E6FF).copy(alpha = 0.28f), Color(0xFF63E6FF).copy(alpha = 0.72f))
                    )
                    }
                    } else if (actor != null) {
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopRichTooltip(
                        tooltip = {
                            DesktopCombatActionTooltipContent(
                                title = "Attack",
                                shortcut = "[1]",
                                description = "Attack with the equipped weapon. Builds Momentum for Overcharge; spends one action.",
                                details = "Consumes turn  |  Requires targeted enemy"
                            )
                        }
                    ) {
                        DesktopCombatActionButton(modifier = Modifier.fillMaxWidth().testTag("combat-action-attack").heightIn(min = if (settings.largeTouchTargets) 56.dp else 48.dp), onClick = ::attack, icon = Icons.Rounded.FlashOn, enabled = actor != null && targetAction == null && timedPrompt == null && runtime.isCombatTutorialCommandEnabled("attack")) {
                            Row(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString("Attack"), AnnotatedString("Attack [1]")))
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Attack", fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text("[1]", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    DesktopRichTooltip(
                        tooltip = {
                            DesktopCombatActionTooltipContent(
                                title = "Abilities",
                                shortcut = "[2]",
                                description = "Deploy specialized character abilities, elemental attacks, and support protocols.",
                                details = "Select from available class skills"
                            )
                        }
                    ) {
                        DesktopCombatActionButton(modifier = Modifier.fillMaxWidth().testTag("combat-action-skills").heightIn(min = if (settings.largeTouchTargets) 56.dp else 48.dp), onClick = ::openSkills, icon = Icons.Rounded.AutoAwesome,
                            enabled = actor != null && targetAction == null && timedPrompt == null && runtime.isCombatTutorialCommandEnabled("skills")) {
                            Row(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString("Abilities"), AnnotatedString("Abilities [2]")))
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Abilities", fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text("[2]", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    DesktopRichTooltip(
                        tooltip = {
                            DesktopCombatActionTooltipContent(
                                title = "Guard",
                                shortcut = "[G]",
                                description = "Brace against incoming strikes to reduce damage taken and fortify stability until your next turn.",
                                details = "Consumes turn  |  Self defense"
                            )
                        }
                    ) {
                        DesktopCombatActionButton(
                            modifier = Modifier.fillMaxWidth().testTag("combat-action-guard").heightIn(min = if (settings.largeTouchTargets) 52.dp else 44.dp),
                            onClick = { runtime.guard() },
                            icon = Icons.Rounded.Shield,
                            enabled = actor != null && targetAction == null && timedPrompt == null && tutorial == null
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString("Guard"), AnnotatedString("Guard [G]")))
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Guard", fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text("[G]", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    DesktopRichTooltip(
                        tooltip = {
                            DesktopCombatActionTooltipContent(
                                title = "Items",
                                shortcut = "[3]",
                                description = "Use field consumables, medical injectors, or combat ordnance from inventory.",
                                details = "Consumes 1 cargo unit per use"
                            )
                        }
                    ) {
                        DesktopCombatActionButton(modifier = Modifier.fillMaxWidth().testTag("combat-action-items").heightIn(min = if (settings.largeTouchTargets) 56.dp else 48.dp), onClick = ::openItems, icon = Icons.Rounded.Inventory2, enabled = actor != null && targetAction == null && timedPrompt == null && runtime.isCombatTutorialCommandEnabled("items")) {
                            Row(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString("Items"), AnnotatedString("Items [3]")))
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Items", fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text("[3]", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    DesktopRichTooltip(
                        tooltip = {
                            val snackName = actorId?.let(runtime::snackLabel) ?: "Snack"
                            DesktopCombatActionTooltipContent(
                                title = snackName,
                                shortcut = "[4]",
                                description = actorId?.let(runtime::snackPreview) ?: "Select a ready character to view their snack ability.",
                                details = "Equipped in Field Menu  |  Character specific"
                            )
                        }
                    ) {
                        DesktopCombatActionButton(modifier = Modifier.fillMaxWidth().testTag("combat-action-snack"), onClick = {
                            actorId?.let { id -> chooseTarget(runtime.snackTargetRequirement(id), "Choose a snack target") { runtime.useSnack(it) } }
                        }, enabled = targetAction == null && timedPrompt == null && actorId?.let(runtime::canUseSnack) == true && runtime.isCombatTutorialCommandEnabled("snack")) {
                            val label = (actorId?.let(runtime::snackLabel) ?: "Snack") + "" + actorId?.let { id -> runtime.snackCooldownRemaining(id).takeIf { it > 0 }?.let { " (ready in $it turns)" } }.orEmpty()
                            Row(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString(label), AnnotatedString("$label [4]")))
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text("[4]", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    actorId?.let { id ->
                        Text(runtime.snackPreview(id), style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().testTag("combat-snack-preview"))
                    }
                    DesktopRichTooltip(
                        tooltip = {
                            DesktopCombatActionTooltipContent(
                                title = "Retreat",
                                shortcut = "[R]",
                                description = "Attempt to disengage from active hostiles and escape back to the previous safe area.",
                                details = "May fail against certain high-threat encounters",
                                accent = Color(0xFFFF887F)
                            )
                        },
                        accent = Color(0xFFFF887F)
                    ) {
                        DesktopCombatActionButton(modifier = Modifier.fillMaxWidth().testTag("combat-action-retreat"), onClick = { runtime.attemptRetreat() }, icon = Icons.Rounded.ExitToApp, enabled = actor != null && targetAction == null && timedPrompt == null && runtime.isCombatTutorialCommandEnabled("retreat")) {
                            Row(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString("Retreat"), AnnotatedString("Retreat [R]")))
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Retreat", fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text("[R]", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                if (targetAction != null) TextButton(modifier = Modifier.fillMaxWidth().desktopPointerHover(), onClick = {
                    cancelActiveSelectionOrMenu()
                }) { Text("Cancel") }
                        }
                    } else {
                        DesktopCombatReadyPrompt(commandAccent, false, settings)
                    }
                }
            }
        }
        }
        DesktopPortraitBackdrop(rememberDesktopAssetPainter(runtime.roomBackground, services.assetProvider), null, Modifier.fillMaxSize()) { backdropLayout ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            val originalWide = hasThreePanelSpace(backdropLayout, density.density)
            val formationCount = battle.authored.formationCapacity.takeIf { it > 0 } ?: enemies.size
            val isCompositeBoss = enemies.isNotEmpty() && enemies.all { member ->
                runtime.enemies.firstOrNull { it.id == runtime.enemyTypeId(member.combatant.id) }?.composite != null
            }
            val needsLargerStage = isCompositeBoss || formationCount > 3 || formationCount > 1 && runtime.enemies.any { it.combatSize == "massive" }
            val layout = if (originalWide && needsLargerStage) {
                val stageWidth = maxOf(backdropLayout.center.width, with(density) { (maxWidth - 480.dp).toPx() })
                val side = (with(density) { maxWidth.toPx() } - stageWidth) / 2f
                val h = with(density) { maxHeight.toPx() }
                PortraitBackdropLayout(
                    androidx.compose.ui.geometry.Rect(side, 0f, side + stageWidth, h),
                    androidx.compose.ui.geometry.Rect(0f, 0f, side, h),
                    androidx.compose.ui.geometry.Rect(side + stageWidth, 0f, with(density) { maxWidth.toPx() }, h))
            } else backdropLayout
            val battlefieldHeight = maxHeight
            val wide = originalWide
            val centerX = if (wide) with(density) { layout.center.left.toDp() } else 0.dp
            val centerWidth = if (wide) with(density) { layout.center.width.toDp() } else maxWidth
            if (!wide || needsLargerStage) {
                Image(rememberDesktopAssetPainter(runtime.roomBackground, services.assetProvider), null,
                    Modifier.offset(x = centerX).width(centerWidth).fillMaxHeight(), contentScale = ContentScale.Crop)
                DesktopVignetteOverlay(.3f, Modifier.fillMaxSize(), Color(0xFF030508))
            }
            Box(Modifier.offset(x = with(density) { layout.center.left.toDp() }).width(with(density) { layout.center.width.toDp() }).fillMaxHeight()) {
                DesktopVignetteOverlay(.38f, Modifier.fillMaxSize(), Color(0xFF030508))
                DesktopBattleStageBackdrop(
                    accentColor = commandAccent,
                    borderColor = Color(0xFF5CCBE8),
                    panelColor = Color(0xFF061018),
                    highContrastMode = settings.highContrastMode,
                    animate = !settings.disableFlashes && !settings.disableScreenshake,
                    modifier = Modifier.fillMaxSize()
                )
            }
            val environmentSession by services.sessionStore.state.collectAsState()
            val environmentRoom=services.roomDefinitions[runtime.encounterRoomId]
            val environmentState=environmentRoom?.state.orEmpty().mapNotNull { (key,value) -> (value as? Boolean)?.let { key to it } }.toMap() + environmentSession.roomStates[runtime.encounterRoomId].orEmpty()
            DesktopEnvironmentalEffects(services,environmentRoom,
                EnvironmentalGeometry(with(density) { maxWidth.toPx() },with(density) { maxHeight.toPx() },
                    layout.center.left,layout.center.top,layout.center.width,layout.center.height),
                environmentState,environmentSession.completedMilestones,settings,combat=true,
                paused=!foreground || showLog || showPauseSettings || battle.outcome!=null,suppressAccents=timedPrompt!=null || tutorial?.showsModal==true)
            Column(Modifier.offset(x = centerX).width(centerWidth).fillMaxHeight()
                .graphicsLayer {
                    translationX = stageShakeX.value
                    translationY = stageShakeY.value
                }
                .background(Color(if (settings.highContrastMode) 0x9505070D else 0x2505070D)).padding(start = 12.dp, end = 12.dp, bottom = 44.dp, top = if (wide) 12.dp else 64.dp)) {
DesktopBattleFormation(enemies, services, Modifier.fillMaxWidth().weight(if (wide) .58f else .64f), true,
                    sprite = { id -> runtime.enemies.firstOrNull { it.id == runtime.enemyTypeId(id) }?.let { enemy ->
                        enemy.sprite.firstOrNull()?.takeIf { it.isNotBlank() } ?: enemy.portrait
                    } },
                    enemySize = { id -> runtime.enemies.firstOrNull { it.id == runtime.enemyTypeId(id) }?.combatSize ?: "medium" },
                    enemyWidth = { id -> runtime.enemies.firstOrNull { it.id == runtime.enemyTypeId(id) }?.combatWidth ?: 1f },
                    composite = { id -> runtime.enemies.firstOrNull { it.id == runtime.enemyTypeId(id) }?.composite },
                    selectedId = if (targetAction != null) selectedTarget else focusedEnemies.firstOrNull(), validTargets = validIds, targeting = targetAction != null,
                    meters = meters, intents = intents, feedback = feedback, cues = cues, lungeStyle = lungeStyle, authoredState = battle,
                    lungeActor = lungeActor, lungeToken = lungeToken, missActor = missActor, missToken = missToken, battleHeight = battlefieldHeight, opposingCount = enemies.size,
                    onHover = { hoveredEnemyId = it },
                    onSelect = { id ->
                        if (enemies.firstOrNull { it.combatant.id == id }?.isAlive == true && runtime.isCombatTutorialTargetEnabled(id)) {
                            runtime.focusEnemyTarget(id)
                            if (targetAction != null && id in validIds) targetAction?.invoke(id)
                        }
                    })
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    if (com.example.starborn.ui.combat.hasAuthoredPreparation(battle))
                        com.example.starborn.ui.combat.AuthoredEncounterPanel(battle, runtime.enemies,
                            runtime::focusEnemyTarget, compact = true, skillName = runtime::skillDisplayName)
                    else com.example.starborn.ui.combat.AuthoredEncounterStatus(battle)
                }
                if (!wide && targetAction != null) Text(instruction ?: "Choose a target", color = Color(0xFF63E6FF), style = MaterialTheme.typography.labelMedium)
                val victoryEmotesActive = battle.outcome is CombatOutcome.Victory && runtime.encounterEnemyIds != listOf("ascended_vale")
DesktopBattleFormation(party, services, Modifier.fillMaxWidth().weight(if (wide) .42f else .36f), false,
                    choosingId = actorId, executingId = lungeActor.takeIf { lungeToken > 0 }, authoredState = battle,
                    sprite = { id ->
                        val normal = runtime.playerParty.firstOrNull { it.id == id }?.let { def ->
                            def.combatIconPath.takeIf { it.isNotBlank() } ?: def.miniIconPath
                        }
                        val memberCues = cues.filter { it.targetId == id }
                        val hasThreatPreparation = battle.authored.preparations.values.any { id in it.targetIds }
                        resolveDesktopPartyCombatEmote(
                            id = id,
                            state = battle.combatants[id],
                            defaultPath = normal,
                            victoryEmotes = victoryEmotesActive,
                            isMissLunge = missActor == id && missToken > 0L && id == lungeActor,
                            isLunging = lungeActor == id && lungeToken > 0L,
                            hasDamageCue = memberCues.any { it.kind == DesktopCueKind.HIT || it.kind == DesktopCueKind.BREAK },
                            hasSupportCue = memberCues.any { it.kind == DesktopCueKind.HEAL || it.kind == DesktopCueKind.SUPPORT },
                            hasTelegraphCue = hasThreatPreparation || memberCues.any { it.kind == DesktopCueKind.TELEGRAPH },
                            isActing = id == actorId,
                            assetProvider = services.assetProvider
                        )
                    },
                    selectedId = if (targetAction != null) selectedTarget else actorId, validTargets = validIds, targeting = targetAction != null,
                    meters = meters, intents = emptyMap(), feedback = feedback, cues = cues, lungeStyle = lungeStyle,
                    lungeActor = lungeActor, lungeToken = lungeToken, missActor = missActor, missToken = missToken, battleHeight = battlefieldHeight, opposingCount = enemies.size,
                    onHover = {},
                    onSelect = { id ->
                        if (targetAction != null && id in validIds) targetAction?.invoke(id)
                        else if (targetAction == null && timedPrompt == null && tutorial?.showsModal != true) runtime.selectReadyPlayer(id)
                    })
            }
            if (wide) {
                Box(Modifier.width(with(density) { layout.left.width.toDp() }).fillMaxHeight().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 64.dp)) {
                    Box(Modifier.align(Alignment.TopEnd).widthIn(max = 360.dp).fillMaxSize()) { context() }
                }
                Box(Modifier.offset(x = with(density) { layout.right.left.toDp() }).width(with(density) { layout.right.width.toDp() })
                    .fillMaxHeight().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 64.dp)) {
                    Box(Modifier.align(Alignment.TopStart).widthIn(max = 340.dp).fillMaxSize()) { commands() }
                }
            } else {
                var compactCommands by remember { mutableStateOf(false) }
                var compactContext by remember { mutableStateOf(false) }
                LaunchedEffect(actorId, menu, targetAction != null) { if (actorId != null) { compactCommands = true; compactContext = false } }
                Row(Modifier.align(Alignment.TopCenter).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { compactContext = !compactContext; compactCommands = false }) { Text("Battle") }
                    OutlinedButton(onClick = { compactCommands = !compactCommands; compactContext = false }) { Text("Commands") }
                }
                if (compactCommands || compactContext) {
                    Box(Modifier.align(Alignment.CenterEnd).padding(top = 64.dp, bottom = 12.dp, end = 12.dp)
                        .width(280.dp).fillMaxHeight(.85f)) {
                        if (compactContext) context() else commands()
                    }
                }
            }
            timedPrompt?.let { prompt ->
                Box(Modifier.offset(x = centerX).width(centerWidth).fillMaxHeight()) { DesktopTimedCombatPrompt(prompt, runtime::registerTimedPromptTap) }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("combat-bottom-key-legend"),
            contentAlignment = Alignment.BottomCenter
        ) {
            DesktopCombatBottomKeyLegend(
                targeting = targetAction != null,
                menu = menu,
                timedPrompt = timedPrompt != null,
                actorAvailable = actor != null && targetAction == null && timedPrompt == null,
                onToggleLog = { if (timedPrompt == null && !showPauseSettings) showLog = !showLog },
                onCancelTarget = { cancelActiveSelectionOrMenu() },
                onTogglePause = if (timedPrompt == null && tutorial?.showsModal != true && battle.outcome == null) {
                    { showPauseSettings = !showPauseSettings }
                } else null
            )
        }
        if (showLog) DesktopCombatLogDialog(
            history = history.toList(),
            onDismiss = { showLog = false },
            accentColor = commandAccent
        )
        if (showPauseSettings) DesktopCombatPauseDialog(
            services = services,
            userSettings = settings,
            currentRoomTitle = runtime.locationTitle,
            accentColor = commandAccent,
            onResume = { showPauseSettings = false }
        )
        tutorial?.takeIf { it.showsModal }?.let { training ->
            DesktopCombatTutorialOverlay(training, commandAccent, settings.highContrastMode,
                runtime::onCombatTutorialContinue, runtime::skipCombatTutorial)
        }
        buildHint?.let { hint ->
            AlertDialog(onDismissRequest = runtime::dismissBuildHint,
                    modifier = Modifier.informationAcknowledgement(runtime::dismissBuildHint),
                title = { Text(hint.context ?: "Nova's build") }, text = { Text(hint.message) },
                confirmButton = { TextButton(onClick = runtime::dismissBuildHint) { Text("Got it") } })
        }
        victoryPayload?.let { payload ->
            DesktopVictoryDialog(services, payload,
                itemNameResolver = { services.itemRepository.findItem(it)?.name ?: services.contentName(it) },
                portraitById = runtime.playerParty.associate { it.id to "images/characters/emotes/${it.id}_cool.png" },
                highContrastMode = settings.highContrastMode, onContinue = { onVictory(payload) }, largeTouchTargets = settings.largeTouchTargets)
        } ?: run {
            (battle.outcome as? CombatOutcome.Victory)?.let { victory ->
                DesktopCombatOutcomeOverlay(victory, party, services)
            }
        }
        battle.outcome?.takeUnless { it is CombatOutcome.Victory }?.let { outcome ->
            LaunchedEffect(outcome) {
                kotlinx.coroutines.delay(if (outcome is CombatOutcome.Defeat) 750L else 600L)
                if (outcome is CombatOutcome.Defeat) onDefeat() else onFlee()
            }
            DesktopCombatOutcomeOverlay(outcome, party, services, onContinue = if (outcome is CombatOutcome.Defeat) onDefeat else onFlee)
        }
    }
}

