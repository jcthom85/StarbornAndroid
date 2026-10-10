package com.example.starborn.desktop.ui

import com.example.starborn.domain.environment.EnvironmentalGeometry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

/** Presentation only: all commands and rewards are resolved by the shared combat controller. */
@Composable
fun DesktopCombatScreen(
    services: DesktopAppServices,
    enemyIds: List<String>,
    onVictory: () -> Unit,
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
    onVictory: () -> Unit,
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
    val meters by runtime.atbMeters.collectAsState()
    val banner by runtime.combatBanner.collectAsState()
    val tutorial by runtime.combatTutorial.collectAsState()
    val timedPrompt by runtime.timedPrompt.collectAsState()
    val lungeStyle by runtime.lungeStyle.collectAsState()
    val lungeToken by runtime.lungeToken.collectAsState()
    val missToken by runtime.missLungeToken.collectAsState()
    val lungeActor by runtime.lungeActorId.collectAsState()
    val missActor by runtime.missLungeActorId.collectAsState()
    var showLog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(runtime, foreground, showLog) { runtime.setBackgroundPaused(!foreground || showLog) }
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
        isBrokenBonus: Boolean = false,
        isGuardBreak: Boolean = false,
        element: String? = null
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
            isBrokenBonus = isBrokenBonus,
            isGuardBreak = isGuardBreak,
            element = element
        )
        cues.add(cue)
        scope.launch { delay(1500); cues.remove(cue) }
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
                    intents[effect.actorId] = label
                    record("${name(effect.actorId)} prepares $label")
                }
                is CombatFxEvent.Impact -> {
                    val tags = listOfNotNull(
                        "CRITICAL".takeIf { effect.critical }, "WEAKNESS".takeIf { effect.isWeakness },
                        "BROKEN BONUS".takeIf { effect.isBrokenBonus }, "GUARD BREAK".takeIf { effect.isGuardBreak },
                        effect.element?.replace('_', ' ')?.uppercase())
                    val label = "-${effect.amount}" + tags.joinToString(" / ", prefix = if (tags.isEmpty()) "" else " ")
                    val weapon = runtime.state.value?.combatants?.get(effect.sourceId)?.combatant?.weapon
                    val style = when (weapon?.weaponType) {
                        "gun" -> AttackLungeStyle.RANGED
                        "jewel" -> AttackLungeStyle.CAST
                        "glove", "sword" -> AttackLungeStyle.MELEE
                        else -> runtime.lungeStyle.value
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
                        isBrokenBonus = effect.isBrokenBonus,
                        isGuardBreak = effect.isGuardBreak,
                        element = effect.element
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
                    val label = services.contentName(effect.statusId) + if (effect.stacks > 1) " x${effect.stacks}" else ""
                    showCue(effect.targetId, label, Color(0xFFD3A1FF), AttackLungeStyle.CAST, kind = DesktopCueKind.STATUS)
                    record("${name(effect.targetId)}: $label")
                }
                is CombatFxEvent.SupportCue -> {
                    showCue(effect.actorId, effect.skillName, Color(0xFF80E7A0), AttackLungeStyle.BUFF, kind = DesktopCueKind.SUPPORT)
                    effect.targetIds.filter { it != effect.actorId }.forEach { showCue(it, effect.skillName, Color(0xFF80E7A0), AttackLungeStyle.BUFF, kind = DesktopCueKind.SUPPORT) }
                    record("${name(effect.actorId)}: ${effect.skillName}")
                }
                is CombatFxEvent.ShieldBreak -> {
                    showCue(effect.targetId, "STABILITY BROKEN", Color(0xFFFFBB55), AttackLungeStyle.CAST, true, DesktopCueKind.BREAK)
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
    LaunchedEffect(menu) { if (menu == null) { withFrameNanos { }; keyboardFocus.requestFocus() } }
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
        // CombatController already starts victory music. Keep its outcome effects visible first.
        delay(900)
        victoryPayload = com.example.starborn.navigation.CombatResultPayload(
            outcome = com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY,
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
            if (showLog) {
                showLog = false
                true
            } else {
                cancelActiveSelectionOrMenu()
            }
        } else if (event.type != KeyEventType.KeyDown || !foreground || showLog || battle.outcome != null) false else {
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
        val inspectedEnemyId = hoveredEnemyId ?: selectedTarget ?: focusedEnemies.firstOrNull()
        val context: @Composable () -> Unit = {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DesktopMenuCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        runtime.locationTitle?.let { Text(it, color = Color(0xFF91A8B3), style = MaterialTheme.typography.labelMedium) }
                        Text(runtime.encounterTitle, color = Color(0xFFFFBB55), style = MaterialTheme.typography.headlineSmall)
                        Text(instruction ?: banner?.primary ?: lastEffect ?: "Hostile Contact", color = Color(0xFF63E6FF))
                        banner?.secondary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                val focused = enemies.firstOrNull { it.combatant.id == inspectedEnemyId }
                focused?.let { target -> DesktopMenuCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(target.combatant.name, style = MaterialTheme.typography.titleMedium)
                            if (target.combatant.id == hoveredEnemyId) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF63E6FF).copy(alpha = 0.15f), border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.6f))) {
                                    Text("INSPECTED", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF63E6FF), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("HP ${target.hp}/${target.combatant.stats.maxHp}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text("Stability ${target.stability}/${target.combatant.stats.stability}", color = Color(0xFF9F79D2), fontSize = 12.sp)
                        }
                        intents[target.combatant.id]?.let { Text(it, color = Color(0xFFFFBB55), fontSize = 12.sp) }
                        val res = target.combatant.resistances
                        val affinities = listOf(
                            "PHYSICAL" to res.physical,
                            "BURN" to res.burn,
                            "FREEZE" to res.freeze,
                            "SHOCK" to res.shock,
                            "ACID" to res.acid,
                            "SOURCE" to res.source
                        ).filter { it.second != 0 }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "AFFINITY TELEMETRY",
                                color = Color(0xFF63E6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            if (affinities.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().testTag("combat-affinity-radar"),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    affinities.forEach { (name, value) ->
                                        val isWeak = value < 0
                                        val chipColor = if (isWeak) Color(0xFFFF6961) else combatElementColor(name.lowercase())
                                        val label = if (isWeak) "WEAK: $name (${value}%)" else "RESIST: $name (+${value}%)"
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = chipColor.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, chipColor.copy(alpha = 0.7f))
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = chipColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF63E6FF).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.25f)),
                                    modifier = Modifier.testTag("combat-affinity-radar")
                                ) {
                                    Text(
                                        text = "BALANCED DEFENSES",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = Color(0xFF91A8B3),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        if (target.statusEffects.isNotEmpty() || target.buffs.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                target.statusEffects.forEach { DesktopCombatStatusChip(it, services) }
                                target.buffs.forEach { DesktopCombatBuffChip(it, services) }
                            }
                        }
                    }
                } }
                DesktopCombatPartyRoster(
                    party = party,
                    services = services,
                    meters = meters,
                    actorId = actorId,
                    playerPartyDefs = runtime.playerParty,
                    onSelectActor = { id ->
                        if (targetAction == null && timedPrompt == null && tutorial?.showsModal != true) {
                            runtime.selectReadyPlayer(id)
                        }
                    }
                )
                DesktopCombatLiveFeed(history = history, onExpandLog = { showLog = true })
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
                        member.buffs.forEach { Text("${it.effect.stat.replace('_', ' ')} ${it.effect.value} (${it.remainingTurns} turns)", style = MaterialTheme.typography.bodySmall) }
                        member.statusEffects.forEach { Text("${services.contentName(it.id)} (${it.remainingTurns} turns)", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (targetAction != null) {
                        Text(instruction ?: "Choose a target", color = Color(0xFF63E6FF))
                        Text("Arrows select · Enter confirms · Esc cancels", style = MaterialTheme.typography.bodySmall, color = Color(0xFF91A8B3))
                        TextButton(onClick = { cancelActiveSelectionOrMenu() }, modifier = Modifier.desktopPointerHover()) { Text("Cancel target [Esc / Right Click]") }
                    } else if (menu != null && actorId != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (menu == "skills") "Abilities" else "Items", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { menu = null; runtime.onCombatTutorialSkillDialogDismissed() }) { Text("Back") }
                        }
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (menu == "skills") {
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
                                            targeting = skill.targeting ?: runtime.targetRequirementFor(skill).name.lowercase(),
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
                                            runtime.skillUnavailableReason(requireNotNull(actorId), skill)?.let { reason ->
                                                Text(reason, color = Color(0xFFFFBB55), style = MaterialTheme.typography.bodySmall)
                                            }
                                            val remaining = runtime.skillCooldownRemaining(requireNotNull(actorId), skill.id)
                                            if (remaining > 0) Text("Ready in $remaining turns", color = Color(0xFFFFBB55), style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "Target: ${skill.targeting?.replace('_', ' ') ?: runtime.targetRequirementFor(skill).name.lowercase()} · Cooldown: ${skill.cooldown} turns",
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
                                                Text("Select")
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
                    } else if (actor != null) {
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopRichTooltip(
                        tooltip = {
                            DesktopCombatActionTooltipContent(
                                title = "Attack",
                                shortcut = "[1]",
                                description = "Standard weapon attack on an enemy target. Fills action readiness on turn completion.",
                                details = "Consumes turn · Requires targeted enemy"
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
                                description = "Quickly consume equipped ration or snack for an instant HP or morale recovery without opening the items menu.",
                                details = "Equipped in Field Menu · Character specific"
                            )
                        }
                    ) {
                        DesktopCombatActionButton(modifier = Modifier.fillMaxWidth().testTag("combat-action-snack"), onClick = {
                            actorId?.let { id -> chooseTarget(runtime.snackTargetRequirement(id), "Choose a snack target") { runtime.useSnack(it) } }
                        }, enabled = targetAction == null && timedPrompt == null && actorId?.let(runtime::canUseSnack) == true && runtime.isCombatTutorialCommandEnabled("snack")) {
                            val label = (actorId?.let(runtime::snackLabel) ?: "Snack") + "" + actorId?.let { id -> runtime.snackCooldownRemaining(id).takeIf { it > 0 }?.let { " ($it turns)" } }.orEmpty()
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
                }) { Text("Cancel target [Esc / Right Click]") }
                        }
                    } else {
                        DesktopCombatReadyPrompt(commandAccent, false, settings)
                    }
                }
            }
        }
        }
        DesktopPortraitBackdrop(rememberDesktopAssetPainter(runtime.roomBackground, services.assetProvider), null, Modifier.fillMaxSize()) { layout ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(Modifier.offset(x = with(density) { layout.center.left.toDp() }).width(with(density) { layout.center.width.toDp() }).fillMaxHeight()) {

                DesktopVignetteOverlay(.38f, Modifier.fillMaxSize(), Color(0xFF030508))
            }
            val environmentSession by services.sessionStore.state.collectAsState()
            val environmentRoom=services.roomDefinitions[runtime.encounterRoomId]
            val environmentState=environmentRoom?.state.orEmpty().mapNotNull { (key,value) -> (value as? Boolean)?.let { key to it } }.toMap() + environmentSession.roomStates[runtime.encounterRoomId].orEmpty()
            DesktopEnvironmentalEffects(services,environmentRoom,
                EnvironmentalGeometry(with(density) { maxWidth.toPx() },with(density) { maxHeight.toPx() },
                    layout.center.left,layout.center.top,layout.center.width,layout.center.height),
                environmentState,environmentSession.completedMilestones,settings,combat=true,
                paused=!foreground || showLog || battle.outcome!=null,suppressAccents=timedPrompt!=null || tutorial?.showsModal==true)
            val battlefieldHeight = maxHeight
            val wide = hasThreePanelSpace(layout, density.density)
            val centerX = with(density) { layout.center.left.toDp() }
            val centerWidth = with(density) { layout.center.width.toDp() }
            Column(Modifier.offset(x = centerX).width(centerWidth).fillMaxHeight()
                .background(Color(if (settings.highContrastMode) 0x9505070D else 0x2505070D)).padding(start = 12.dp, end = 12.dp, bottom = 44.dp, top = if (wide) 12.dp else 64.dp)) {
DesktopBattleFormation(enemies, services, Modifier.fillMaxWidth().weight(.54f), true,
                    sprite = { id -> (runtime.enemies.firstOrNull { it.id == id }
                        ?: runtime.enemies.firstOrNull { id.startsWith(it.id) })?.portrait },
                    selectedId = selectedTarget ?: focusedEnemies.firstOrNull(), validTargets = validIds, targeting = targetAction != null,
                    meters = meters, intents = intents, feedback = feedback, cues = cues, lungeStyle = lungeStyle,
                    lungeActor = lungeActor, lungeToken = lungeToken, missActor = missActor, missToken = missToken, battleHeight = battlefieldHeight, opposingCount = enemies.size,
                    onHover = { hoveredEnemyId = it },
                    onSelect = { id ->
                        if (enemies.firstOrNull { it.combatant.id == id }?.isAlive == true && runtime.isCombatTutorialTargetEnabled(id)) {
                            runtime.focusEnemyTarget(id)
                            if (targetAction != null && id in validIds) targetAction?.invoke(id)
                        }
                    })
                if (!wide) Text(instruction ?: banner?.primary ?: "Hostile Contact", color = Color(0xFF63E6FF), style = MaterialTheme.typography.labelMedium)
DesktopBattleFormation(party, services, Modifier.fillMaxWidth().weight(.46f), false,
                    sprite = { id ->
                        val normal = runtime.playerParty.firstOrNull { it.id == id }?.combatIconPath
                        when {
                            battle.combatants[id]?.isAlive == false -> "images/characters/emotes/${id}_down.png"
                            battle.outcome is CombatOutcome.Victory -> "images/characters/emotes/${id}_cool.png"
                            missActor == id && missToken > 0 && id == lungeActor -> "images/characters/emotes/${id}_confident.png"
                            id == lungeActor -> "images/characters/emotes/${id}_angry.png"
                            else -> normal
                        }
                    },
                    selectedId = selectedTarget ?: actorId, validTargets = validIds, targeting = targetAction != null,
                    meters = meters, intents = emptyMap(), feedback = feedback, cues = cues, lungeStyle = lungeStyle,
                    lungeActor = lungeActor, lungeToken = lungeToken, missActor = missActor, missToken = missToken, battleHeight = battlefieldHeight, opposingCount = enemies.size,
                    onHover = {},
                    onSelect = { id ->
                        if (targetAction != null && id in validIds) targetAction?.invoke(id)
                        else if (targetAction == null && timedPrompt == null && tutorial?.showsModal != true) runtime.selectReadyPlayer(id)
                    })
            }
            if (wide) {
                Box(Modifier.width(with(density) { layout.left.width.toDp() }).fillMaxHeight().padding(20.dp)) {
                    Box(Modifier.align(Alignment.TopEnd).widthIn(max = 360.dp).fillMaxSize()) { context() }
                }
                Box(Modifier.offset(x = with(density) { layout.right.left.toDp() }).width(with(density) { layout.right.width.toDp() })
                    .fillMaxHeight().padding(20.dp)) {
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
                onToggleLog = { if (timedPrompt == null) showLog = !showLog },
                onCancelTarget = { cancelActiveSelectionOrMenu() }
            )
        }
        if (showLog) DesktopCombatLogDialog(
            history = history,
            onDismiss = { showLog = false },
            accentColor = commandAccent
        )
        tutorial?.let { training ->
            DesktopCombatTutorialOverlay(training, commandAccent, settings.highContrastMode,
                runtime::onCombatTutorialContinue, runtime::skipCombatTutorial)
        }
        victoryPayload?.let { payload ->
            DesktopVictoryDialog(services, payload,
                itemNameResolver = { services.itemRepository.findItem(it)?.name ?: services.contentName(it) },
                portraitById = runtime.playerParty.associate { it.id to "images/characters/emotes/${it.id}_cool.png" },
                highContrastMode = settings.highContrastMode, onContinue = onVictory, largeTouchTargets = settings.largeTouchTargets)
        } ?: run {
            (battle.outcome as? CombatOutcome.Victory)?.let { victory ->
                DesktopCombatOutcomeOverlay(victory, party, services)
            }
        }
        battle.outcome?.takeUnless { it is CombatOutcome.Victory }?.let { outcome ->
            DesktopCombatOutcomeOverlay(outcome, party, services, onContinue = {
                if (outcome is CombatOutcome.Defeat) onDefeat() else onFlee()
            })
        }
    }
}

