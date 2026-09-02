package com.example.starborn.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.starborn.domain.model.Item
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

private val NeonCyan = Color(0xFF63E6FF)
private val NeonAmber = Color(0xFFFF9F2E)
private val TitleWarmColor = Color(0xFFFFB86C)
private val HealthGreen = Color(0xFF00E676)
private val HealthRed = Color(0xFFFF3366)
private val ShieldBlue = Color(0xFF2979FF)
private val EnergyYellow = Color(0xFFFFD54F)
private val StabilityAmber = Color(0xFFFFB300)
private val LocalCombatPink = Color(0xFFFF007F)

data class FloatingCombatText(
    val id: Long,
    val text: String,
    val color: Color,
    val isCritical: Boolean = false,
    val targetEnemyId: String? = null
)

data class DesktopCombatant(
    val id: String,
    val name: String,
    val spritePath: String,
    var hp: Int,
    val maxHp: Int,
    var stability: Int,
    val maxStability: Int,
    var shield: Int,
    val maxShield: Int,
    val intent: String,
    val speed: Int,
    var isBroken: Boolean = false
)

data class DesktopPartyMember(
    val id: String,
    val name: String,
    val role: String,
    val spritePath: String,
    var hp: Int,
    val maxHp: Int,
    var shield: Int,
    val maxShield: Int,
    var energy: Int,
    val maxEnergy: Int,
    var isGuarding: Boolean = false
)

enum class CombatSubMenu {
    ROOT, SKILLS, ITEMS, SNACK
}

@Composable
fun DesktopCombatScreen(
    services: DesktopAppServices,
    enemyIds: List<String> = listOf("faulted_loader", "resonance_buoy"),
    onVictory: () -> Unit,
    onDefeat: () -> Unit,
    onFlee: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sessionState by services.sessionStore.state.collectAsState()
    val rooms = remember { services.worldDataSource.loadRooms() }
    val currentRoom = remember(sessionState.roomId, rooms) {
        rooms.firstOrNull { it.id == sessionState.roomId } ?: rooms.firstOrNull()
    }

    val allEnemies = remember { services.worldDataSource.loadEnemies() }
    val allSkills = remember { services.worldDataSource.loadSkills() }

    // Dynamic Party Squad based on active session partyMembers
    val allCharacters = remember { services.worldDataSource.loadCharacters() }
    val characterLookup = remember(allCharacters) { allCharacters.associateBy { it.id } }
    val activePartyIds = remember(sessionState.partyMembers) {
        if (sessionState.partyMembers.isNotEmpty()) sessionState.partyMembers else listOf("nova")
    }

    val partyMembers = remember(activePartyIds, sessionState) {
        val members = activePartyIds.mapNotNull { charId ->
            val charDef = characterLookup[charId]
            val level = sessionState.partyMemberLevels[charId] ?: sessionState.playerLevel
            val hp = sessionState.partyMemberHp[charId] ?: (100 + level * 20)
            val maxHp = 100 + level * 20
            val role = when (charId) {
                "zeke" -> "Vanguard"
                "orion" -> "Mystic"
                "gh0st" -> "Infiltrator"
                else -> "Specialist"
            }
            val sprite = when (charId) {
                "zeke" -> "images/characters/zeke_combat.png"
                "orion" -> "images/characters/orion_combat.png"
                "gh0st" -> "images/characters/gh0st_combat.png"
                else -> "images/characters/nova_combat.png"
            }
            DesktopPartyMember(
                id = charId,
                name = charDef?.name ?: charId.replaceFirstChar { it.uppercase() },
                role = role,
                spritePath = sprite,
                hp = hp,
                maxHp = maxHp,
                shield = 50 + level * 10,
                maxShield = 50 + level * 10,
                energy = 50 + level * 5,
                maxEnergy = 50 + level * 5
            )
        }
        mutableStateListOf<DesktopPartyMember>().apply {
            if (members.isNotEmpty()) addAll(members)
            else add(
                DesktopPartyMember(
                    id = "nova",
                    name = "Nova",
                    role = "Specialist",
                    spritePath = "images/characters/nova_combat.png",
                    hp = 120,
                    maxHp = 120,
                    shield = 60,
                    maxShield = 60,
                    energy = 55,
                    maxEnergy = 55
                )
            )
        }
    }

    var enemies by remember(enemyIds, allEnemies) {
        val loaded = enemyIds.mapNotNull { enemyId ->
            val def = allEnemies.firstOrNull { it.id == enemyId }
            if (def != null) {
                val intentDesc = if (def.abilities.isNotEmpty()) {
                    def.abilities.first().replace("_", " ").uppercase() + " (-${def.strength * 8} DMG)"
                } else "STRIKE (-${def.strength * 6} DMG)"
                DesktopCombatant(
                    id = def.id,
                    name = def.name,
                    spritePath = def.portrait ?: "images/enemies/heavy_loader_combat.png",
                    hp = def.hp,
                    maxHp = def.hp,
                    stability = def.stability ?: 35,
                    maxStability = def.stability ?: 35,
                    shield = if (def.tier == "elite" || def.tier == "boss") 50 else 30,
                    maxShield = if (def.tier == "elite" || def.tier == "boss") 50 else 30,
                    intent = intentDesc,
                    speed = def.speed
                )
            } else null
        }
        mutableStateOf(
            if (loaded.isNotEmpty()) loaded
            else listOf(
                DesktopCombatant(
                    id = "faulted_loader",
                    name = "Faulted Loader",
                    spritePath = "images/enemies/heavy_loader_combat.png",
                    hp = 160,
                    maxHp = 160,
                    stability = 45,
                    maxStability = 45,
                    shield = 50,
                    maxShield = 50,
                    intent = "Slide Slam (-40 DMG)",
                    speed = 8
                ),
                DesktopCombatant(
                    id = "resonance_buoy",
                    name = "Resonance Buoy",
                    spritePath = "images/enemies/resonance_buoy_combat.png",
                    hp = 110,
                    maxHp = 110,
                    stability = 30,
                    maxStability = 30,
                    shield = 20,
                    maxShield = 20,
                    intent = "Static Burst (-25 DMG)",
                    speed = 12
                )
            )
        )
    }

    var activeActorIndex by remember { mutableStateOf(0) }
    val activeActor = partyMembers.getOrElse(activeActorIndex) { partyMembers.first() }
    val activeActorName = activeActor.name.uppercase()
    val activeActorDisplayName = activeActor.name

    var selectedEnemyIndex by remember { mutableStateOf(0) }
    var currentSubMenu by remember { mutableStateOf(CombatSubMenu.ROOT) }
    val floatingTexts = remember { mutableStateListOf<FloatingCombatText>() }
    val battleLogs = remember { mutableStateListOf("◆ ENGAGED HOSTILE SECTOR ENCOUNTER // BATTLE STATIONS ACTIVE") }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var showVictoryDialog by remember { mutableStateOf(false) }
    var showDefeatDialog by remember { mutableStateOf(false) }

    // Dynamic Battle Rewards calculation
    var earnedXp by remember { mutableStateOf(100) }
    var earnedCredits by remember { mutableStateOf(50) }
    var earnedLoot by remember { mutableStateOf<List<Pair<com.example.starborn.domain.model.Item, Int>>>(emptyList()) }

    // Real Inventory consumable items from session state
    val allItemsMap = remember { services.itemRepository.allItems().associateBy { it.id } }
    val inventoryConsumables = remember(sessionState.inventory) {
        sessionState.inventory.filter { it.value > 0 }.mapNotNull { (itemId, qty) ->
            val item = allItemsMap[itemId]
            if (item != null && (item.type.equals("consumable", ignoreCase = true) || item.type.equals("snack", ignoreCase = true))) {
                item to qty
            } else null
        }
    }

    // Action Banner state
    var activeBanner by remember { mutableStateOf<Pair<String, Color>?>(null) }

    // Sprite Lunge & Recoil animations
    val playerLungeX = remember { Animatable(0f) }
    val enemyLungeX = remember { Animatable(0f) }

    fun triggerPlayerLunge() {
        coroutineScope.launch {
            playerLungeX.animateTo(32f, tween(120, easing = FastOutSlowInEasing))
            playerLungeX.animateTo(0f, tween(180, easing = LinearEasing))
        }
    }

    fun triggerEnemyLunge() {
        coroutineScope.launch {
            enemyLungeX.animateTo(-32f, tween(120, easing = FastOutSlowInEasing))
            enemyLungeX.animateTo(0f, tween(180, easing = LinearEasing))
        }
    }

    fun showBanner(text: String, accentColor: Color) {
        activeBanner = text to accentColor
        coroutineScope.launch {
            delay(1400)
            if (activeBanner?.first == text) {
                activeBanner = null
            }
        }
    }

    // Targeted Selection Prompt State
    var pendingTargetInstruction by remember { mutableStateOf<String?>(null) }
    var pendingTargetAction by remember { mutableStateOf<((targetIndex: Int) -> Unit)?>(null) }

    // Screen Shake state
    val shakeOffsetX = remember { Animatable(0f) }
    val shakeOffsetY = remember { Animatable(0f) }

    fun triggerScreenShake() {
        coroutineScope.launch {
            shakeOffsetX.snapTo(12f)
            shakeOffsetY.snapTo(-8f)
            shakeOffsetX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            shakeOffsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    fun spawnFloatingText(text: String, color: Color, isCritical: Boolean = false, targetId: String? = null) {
        val entry = FloatingCombatText(System.currentTimeMillis() + Random.nextLong(1000), text, color, isCritical, targetId)
        floatingTexts.add(entry)
        coroutineScope.launch {
            delay(1100)
            floatingTexts.remove(entry)
        }
    }

    // Battle Start Audio
    LaunchedEffect(Unit) {
        val cmds = services.audioRouter.commandsForBattle("battle_start")
        services.audioDriver.executeAll(cmds)
    }

    // Execute Active Character Attack on Specific Target Index
    fun executePlayerAttack(damage: Int, skillName: String, energyCost: Int, targetIndex: Int = selectedEnemyIndex) {
        if (isPlayerTurn && enemies.isNotEmpty() && activeActor.energy >= energyCost) {
            activeActor.energy = (activeActor.energy - energyCost).coerceAtLeast(0)
            val targetIdx = targetIndex.coerceIn(0, enemies.size - 1)
            selectedEnemyIndex = targetIdx
            val target = enemies[targetIdx]
            isPlayerTurn = false
            currentSubMenu = CombatSubMenu.ROOT
            pendingTargetInstruction = null
            pendingTargetAction = null

            triggerPlayerLunge()
            showBanner("⚡ $activeActorName // ${skillName.uppercase()}", if (activeActorIndex == 0) NeonCyan else NeonAmber)

            // SFX & Feedback
            val isCrit = Random.nextFloat() < 0.25f
            val actualDmg = if (isCrit) (damage * 1.5f).toInt() else damage

            val attackSfx = if (isCrit) "sfx_combat_crit_hit" else if (activeActorIndex == 0) "sfx_combat_blaster_fire" else "sfx_combat_heavy_wrench"
            services.audioDriver.executeAll(services.audioRouter.commandsForBattle(attackSfx))

            battleLogs.add(0, "▸ $activeActorName used [$skillName] on ${target.name} for $actualDmg damage!${if (isCrit) " (CRITICAL HIT)" else ""}")
            spawnFloatingText("-$actualDmg", if (isCrit) HealthRed else NeonCyan, isCritical = isCrit, targetId = target.id)
            if (isCrit) triggerScreenShake()

            // Damage to stability / shield / hp
            if (target.stability > 0) {
                val stabilityDmg = if (activeActorIndex == 1) 25 else 15
                target.stability = (target.stability - stabilityDmg).coerceAtLeast(0)
                if (target.stability == 0 && !target.isBroken) {
                    target.isBroken = true
                    battleLogs.add(0, "💥 SHIELD BREAK! ${target.name} is STAGGERED!")
                    spawnFloatingText("STAGGERED!", StabilityAmber, isCritical = true, targetId = target.id)
                    services.audioDriver.executeAll(services.audioRouter.commandsForBattle("sfx_combat_crit_hit"))
                }
            }

            var remaining = actualDmg
            if (target.shield > 0) {
                val shieldDmg = remaining.coerceAtMost(target.shield)
                target.shield -= shieldDmg
                remaining -= shieldDmg
            }
            target.hp = (target.hp - remaining).coerceAtLeast(0)

            // Check Defeat
            val updated = enemies.toMutableList()
            if (target.hp <= 0) {
                battleLogs.add(0, "☠ ${target.name} neutralized!")
                updated.removeAt(targetIdx)
                selectedEnemyIndex = 0
            }
            enemies = updated

            if (enemies.isEmpty()) {
                showVictoryDialog = true
                val defeatedDefs = enemyIds.mapNotNull { eid -> allEnemies.firstOrNull { it.id == eid } }
                val totalXp = if (defeatedDefs.isNotEmpty()) defeatedDefs.sumOf { it.xpReward } else 120
                val totalCredits = if (defeatedDefs.isNotEmpty()) defeatedDefs.sumOf { it.creditReward } else 65

                val lootList = mutableListOf<Pair<com.example.starborn.domain.model.Item, Int>>()
                defeatedDefs.forEach { def ->
                    def.drops.forEach { drop ->
                        if (Random.nextDouble() <= drop.chance) {
                            val item = allItemsMap[drop.id]
                            if (item != null) {
                                val qty = drop.quantity ?: drop.qtyMin ?: 1
                                lootList.add(item to qty)
                                services.inventoryService.addItem(item.id, qty)
                            }
                        }
                    }
                }
                earnedXp = totalXp
                earnedCredits = totalCredits
                earnedLoot = lootList

                battleLogs.add(0, "★ COMBAT VICTORY! Sector secured. Gained +$totalXp XP and +$totalCredits Credits.")
                services.audioDriver.executeAll(services.audioRouter.commandsForBattle("music_victory_standard"))
                services.sessionStore.addCredits(totalCredits)
                services.sessionStore.restore(services.sessionStore.state.value.copy(
                    playerXp = services.sessionStore.state.value.playerXp + totalXp,
                    inventory = services.inventoryService.snapshot()
                ))
            } else {
                coroutineScope.launch {
                    delay(800)
                    enemies.forEach { enemy ->
                        val livingParty = partyMembers.filter { it.hp > 0 }
                        if (livingParty.isNotEmpty() && !enemy.isBroken) {
                            val targetMember = livingParty.random()
                            val enemyDmg = if (enemy.id == "scrapper_guard") 35 else 22
                            val mitigatedDmg = if (targetMember.isGuarding) (enemyDmg * 0.35f).toInt() else enemyDmg

                            triggerEnemyLunge()
                            showBanner("✦ ${enemy.name.uppercase()} // ${enemy.intent.split(" ").first().uppercase()}", NeonAmber)
                            services.audioDriver.executeAll(services.audioRouter.commandsForBattle("sfx_combat_enemy_screech"))

                            battleLogs.add(0, "▸ ${enemy.name} used [${enemy.intent.split(" ").first()}] hitting ${targetMember.name} for $mitigatedDmg damage!")
                            spawnFloatingText("-$mitigatedDmg", HealthRed)

                            var rem = mitigatedDmg
                            if (targetMember.shield > 0) {
                                val sDmg = rem.coerceAtMost(targetMember.shield)
                                targetMember.shield -= sDmg
                                rem -= sDmg
                            }
                            targetMember.hp = (targetMember.hp - rem).coerceAtLeast(0)
                            delay(600)
                        } else if (enemy.isBroken) {
                            battleLogs.add(0, "▸ ${enemy.name} is recovering from STAGGER...")
                            enemy.isBroken = false
                            enemy.stability = enemy.maxStability
                            delay(400)
                        }
                    }

                    if (partyMembers.all { it.hp <= 0 }) {
                        showDefeatDialog = true
                        services.audioDriver.executeAll(services.audioRouter.commandsForBattle("music_game_over"))
                        battleLogs.add(0, "✖ CRITICAL FAILURE: Party has fallen in combat.")
                    } else {
                        // Switch active actor to next living party member
                        val livingIndices = partyMembers.indices.filter { partyMembers[it].hp > 0 }
                        if (livingIndices.isNotEmpty()) {
                            val nextIdx = livingIndices.firstOrNull { it > activeActorIndex } ?: livingIndices.first()
                            activeActorIndex = nextIdx
                        }
                        isPlayerTurn = true
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                translationX = shakeOffsetX.value
                translationY = shakeOffsetY.value
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            if (currentSubMenu != CombatSubMenu.ROOT) {
                                currentSubMenu = CombatSubMenu.ROOT
                                true
                            } else false
                        }
                        Key.Tab -> {
                            if (enemies.isNotEmpty()) {
                                selectedEnemyIndex = (selectedEnemyIndex + 1) % enemies.size
                            }
                            true
                        }
                        Key.One -> {
                            if (isPlayerTurn) {
                                when (currentSubMenu) {
                                    CombatSubMenu.ROOT -> executePlayerAttack(35, "Kinetic Pulse", 0)
                                    CombatSubMenu.SKILLS -> executePlayerAttack(60, "Plasma Overcharge", 30)
                                    CombatSubMenu.ITEMS -> {
                                        activeActor.hp = (activeActor.hp + 70).coerceAtMost(activeActor.maxHp)
                                        battleLogs.add(0, "▸ Used [Nanite Medkit] on ${activeActor.name} restoring +70 HP!")
                                        spawnFloatingText("+70 HP", HealthGreen)
                                        currentSubMenu = CombatSubMenu.ROOT
                                        isPlayerTurn = false
                                    }
                                    CombatSubMenu.SNACK -> {
                                        activeActor.energy = (activeActor.energy + 40).coerceAtMost(activeActor.maxEnergy)
                                        battleLogs.add(0, "▸ Munched on [Zeke's Smoked Jerky] (+40 Energy)!")
                                        spawnFloatingText("+40 EP", EnergyYellow)
                                        currentSubMenu = CombatSubMenu.ROOT
                                        isPlayerTurn = false
                                    }
                                }
                            }
                            true
                        }
                        Key.Two -> {
                            if (isPlayerTurn) {
                                when (currentSubMenu) {
                                    CombatSubMenu.ROOT -> currentSubMenu = CombatSubMenu.SKILLS
                                    CombatSubMenu.SKILLS -> executePlayerAttack(95, "Cosmic Railgun", 50)
                                    CombatSubMenu.ITEMS -> {
                                        activeActor.shield = (activeActor.shield + 50).coerceAtMost(activeActor.maxShield)
                                        battleLogs.add(0, "▸ Used [Shield Battery] on ${activeActor.name} restoring +50 Shield!")
                                        spawnFloatingText("+50 SHIELD", ShieldBlue)
                                        currentSubMenu = CombatSubMenu.ROOT
                                        isPlayerTurn = false
                                    }
                                    CombatSubMenu.SNACK -> {}
                                }
                            }
                            true
                        }
                        Key.Three -> {
                            if (isPlayerTurn && currentSubMenu == CombatSubMenu.ROOT) {
                                currentSubMenu = CombatSubMenu.ITEMS
                            }
                            true
                        }
                        Key.Four -> {
                            if (isPlayerTurn && currentSubMenu == CombatSubMenu.ROOT) {
                                currentSubMenu = CombatSubMenu.SNACK
                            }
                            true
                        }
                        Key.Five -> {
                            if (isPlayerTurn && currentSubMenu == CombatSubMenu.ROOT) {
                                activeActor.isGuarding = true
                                activeActor.energy = (activeActor.energy + 25).coerceAtMost(activeActor.maxEnergy)
                                battleLogs.add(0, "▸ ${activeActor.name} assumed [DEFENSIVE GUARD] (+25 Energy, -65% Damage).")
                                spawnFloatingText("GUARDING", ShieldBlue)
                                isPlayerTurn = false
                            }
                            true
                        }
                        Key.Six -> {
                            if (isPlayerTurn && currentSubMenu == CombatSubMenu.ROOT) {
                                onFlee()
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Centered Native Unstretched Artwork with Ambient Extended Side Wings & Directional Lighting Bleed
        val roomBg = currentRoom?.backgroundImage ?: "bg_combat_arena"
        val bgPainter = rememberDesktopAssetPainter(roomBg, services.assetProvider)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF02060B)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Directional Lighting Halos (Warm Amber Bleed on Left, Cool Cyan Bleed on Right)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Left Wing Warm Bleed (from lamp / crew lights)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonAmber.copy(alpha = 0.12f),
                            NeonAmber.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.40f),
                        radius = size.width * 0.45f
                    ),
                    size = size
                )

                // Right Wing Cool Bleed (from station shower / electronics)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.10f),
                            NeonCyan.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.45f),
                        radius = size.width * 0.45f
                    ),
                    size = size
                )

                // Perspective Floor Depth Lines (Extending floor plane outward from center stage)
                val horizonY = size.height * 0.58f
                val floorBottomY = size.height * 0.95f
                val centerX = size.width * 0.50f
                val stageHalfWidth = size.height * (9f / 16f) * 0.5f

                // Left Perspective Rays
                val leftStageEdge = centerX - stageHalfWidth
                drawLine(
                    color = NeonCyan.copy(alpha = 0.08f),
                    start = Offset(leftStageEdge, horizonY),
                    end = Offset(0f, floorBottomY),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = NeonCyan.copy(alpha = 0.05f),
                    start = Offset(leftStageEdge + 40f, horizonY),
                    end = Offset(size.width * 0.12f, size.height),
                    strokeWidth = 1f
                )

                // Right Perspective Rays
                val rightStageEdge = centerX + stageHalfWidth
                drawLine(
                    color = NeonAmber.copy(alpha = 0.08f),
                    start = Offset(rightStageEdge, horizonY),
                    end = Offset(size.width, floorBottomY),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = NeonAmber.copy(alpha = 0.05f),
                    start = Offset(rightStageEdge - 40f, horizonY),
                    end = Offset(size.width * 0.88f, size.height),
                    strokeWidth = 1f
                )
            }

            // Centered High-Fidelity Room Image (Native Un-stretched Aspect Ratio with Tactical HUD Viewport Calipers)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(9f / 16f)
                    .align(Alignment.Center)
            ) {
                Image(
                    painter = bgPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Soft Left & Right Edge Vignette Falloff
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xFF02060B).copy(alpha = 0.92f),
                                0.08f to Color(0xFF02060B).copy(alpha = 0.30f),
                                0.18f to Color.Transparent,
                                0.82f to Color.Transparent,
                                0.92f to Color(0xFF02060B).copy(alpha = 0.30f),
                                1.0f to Color(0xFF02060B).copy(alpha = 0.92f)
                            )
                        )
                )

                // Tactical Viewport Calipers & Edge Metering Marks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val caliperColor = NeonCyan.copy(alpha = 0.28f)
                    val bracketLen = 28f
                    val strokeW = 1.5f

                    // Top-Left Caliper
                    drawLine(caliperColor, Offset(12f, 12f), Offset(12f + bracketLen, 12f), strokeW)
                    drawLine(caliperColor, Offset(12f, 12f), Offset(12f, 12f + bracketLen), strokeW)

                    // Top-Right Caliper
                    drawLine(caliperColor, Offset(size.width - 12f, 12f), Offset(size.width - 12f - bracketLen, 12f), strokeW)
                    drawLine(caliperColor, Offset(size.width - 12f, 12f), Offset(size.width - 12f, 12f + bracketLen), strokeW)

                    // Bottom-Left Caliper
                    drawLine(caliperColor, Offset(12f, size.height - 12f), Offset(12f + bracketLen, size.height - 12f), strokeW)
                    drawLine(caliperColor, Offset(12f, size.height - 12f), Offset(12f, size.height - 12f - bracketLen), strokeW)

                    // Bottom-Right Caliper
                    drawLine(caliperColor, Offset(size.width - 12f, size.height - 12f), Offset(size.width - 12f - bracketLen, size.height - 12f), strokeW)
                    drawLine(caliperColor, Offset(size.width - 12f, size.height - 12f), Offset(size.width - 12f, size.height - 12f - bracketLen), strokeW)
                }
            }

            // Left & Right Flank Cybernetic Extension Grids & HUD Reticles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridAlpha = 0.035f
                val step = 48.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(color = NeonCyan.copy(alpha = gridAlpha), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(color = NeonCyan.copy(alpha = gridAlpha), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                // Left Tactical Wing Reticle
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.05f),
                    radius = 180f,
                    center = Offset(size.width * 0.18f, size.height * 0.45f),
                    style = Stroke(width = 1.2f)
                )
                // Right Tactical Wing Reticle
                drawCircle(
                    color = NeonAmber.copy(alpha = 0.05f),
                    radius = 180f,
                    center = Offset(size.width * 0.82f, size.height * 0.45f),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 2. Dynamic Room Weather & Ambient Dust Moters
        DesktopWeatherOverlay(currentRoom?.weather ?: "dust")

        // 3. Vignette & CRT Scanlines
        DesktopVignetteOverlay(intensity = 0.55f)
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.03f)

        // 3. Main Combat UI Deck (Left Party Flank vs Right Enemy Flank Across Arena)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Android 1:1 CombatEncounterHeader
            val headerBorder = Color(0xFF5CCBE8)
            val headerAccent = Color(0xFF7BE4FF)
            val headerPanel = Color(0xFF061018)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = headerPanel.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, headerBorder.copy(alpha = 0.46f)),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = currentRoom?.title?.uppercase() ?: "TACTICAL SECTOR",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            ),
                            color = headerAccent.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pendingTargetInstruction != null) pendingTargetInstruction!!.uppercase() else "HOSTILE CONTACT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (pendingTargetInstruction != null) Color(0xFFFFC8B8) else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        headerAccent.copy(alpha = 0.78f),
                                        headerBorder.copy(alpha = 0.30f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // MIDDLE ARENA: Left Party Flank vs Right Enemy Flank (Vertically Centered with Room Horizon)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Flank: Full 4-Hero Party Squad (Tactical 2x2 Formation: Top Backline & Bottom Frontline)
                    val partyRows = remember(partyMembers.size) {
                        if (partyMembers.size > 2) {
                            listOf(
                                partyMembers.drop(2), // Top Row: Backline (Orion, GH0ST)
                                partyMembers.take(2)  // Bottom Row: Frontline (Nova, Zeke)
                            )
                        } else listOf(partyMembers)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .graphicsLayer { translationX = playerLungeX.value }
                    ) {
                        partyRows.forEachIndexed { rowIndex, rowMembers ->
                            val isBackline = rowIndex == 0 && partyRows.size > 1
                            val xOffset = if (isBackline) 0.dp else 28.dp // Frontline steps forward towards center
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.offset(x = xOffset)
                            ) {
                                rowMembers.forEach { member ->
                                    val memberIdx = partyMembers.indexOf(member)
                                    DesktopFlankPlayerEntity(
                                        name = member.name,
                                        role = member.role,
                                        spritePath = member.spritePath,
                                        hp = member.hp,
                                        maxHp = member.maxHp,
                                        shield = member.shield,
                                        maxShield = member.maxShield,
                                        energy = member.energy,
                                        maxEnergy = member.maxEnergy,
                                        isGuarding = member.isGuarding,
                                        isReady = isPlayerTurn && activeActorIndex == memberIdx,
                                        isBackline = isBackline,
                                        cardWidth = 145.dp,
                                        spriteSize = if (isBackline) 88.dp else 102.dp,
                                        services = services
                                    )
                                }
                            }
                        }
                    }

                    // Center Floating Combat FX (Clear Open View of Room Art)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        floatingTexts.forEach { fx ->
                            Text(
                                text = fx.text,
                                color = fx.color,
                                fontSize = if (fx.isCritical) 36.sp else 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.graphicsLayer { shadowElevation = 12f }
                            )
                        }
                    }

                    // Right Flank: Enemy Entities on Stage (Tactical 2-Row Formation: Top Backline & Bottom Frontline)
                    val enemyCount = enemies.size
                    val enemyRows = remember(enemies) {
                        when {
                            enemyCount <= 2 -> listOf(enemies)
                            enemyCount <= 4 -> listOf(enemies.drop(2), enemies.take(2))
                            else -> listOf(enemies.drop(2), enemies.take(2))
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .graphicsLayer { translationX = enemyLungeX.value }
                    ) {
                        enemyRows.forEachIndexed { rowIndex, rowEnemies ->
                            val isBackline = rowIndex == 0 && enemyRows.size > 1
                            val xOffset = if (isBackline) 0.dp else (-28).dp // Frontline steps forward towards center
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.offset(x = xOffset)
                            ) {
                                rowEnemies.forEach { enemy ->
                                    val enemyIdx = enemies.indexOf(enemy)
                                    val isSelected = selectedEnemyIndex == enemyIdx
                                    DesktopFlankEnemyEntity(
                                        enemy = enemy,
                                        isSelected = isSelected,
                                        isBackline = isBackline,
                                        cardWidth = 145.dp,
                                        spriteSize = if (isBackline) 88.dp else 102.dp,
                                        services = services,
                                        onSelect = {
                                            val action = pendingTargetAction
                                            if (action != null) {
                                                action(enemyIdx)
                                            } else {
                                                selectedEnemyIndex = enemyIdx
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Android Sliding Action Banner (Center Stage Top Overlay)
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeBanner != null,
                    enter = fadeIn(tween(140)) + slideInVertically(tween(180)) { -it },
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                ) {
                    activeBanner?.let { (text, color) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070E18).copy(alpha = 0.94f))
                                .border(BorderStroke(1.5.dp, color), RoundedCornerShape(8.dp))
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = text,
                                color = color,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // BOTTOM BAR: Centered Tactical Command Palette Deck (Matching Android 1:1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val paletteBase = Color(0xFF0F1118)
                val borderColor = Color.White.copy(alpha = 0.5f)
                val accentColor = when (activeActor.id) {
                    "nova" -> Color(0xFF63E6FF)
                    "zeke" -> Color(0xFFFF9F2E)
                    "orion" -> Color(0xFFBA68C8)
                    else -> Color(0xFF00E676)
                }

                Box(
                    modifier = Modifier
                        .widthIn(max = 540.dp)
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = paletteBase.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 10.dp,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.25.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Actor Header (Portrait + Name + HP + ATB Bar)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFF1C1F24))
                                        .clickable {
                                            if (isPlayerTurn) activeActorIndex = (activeActorIndex + 1) % partyMembers.size
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberDesktopAssetPainter(activeActor.spritePath, services.assetProvider),
                                        contentDescription = activeActorDisplayName,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .padding(4.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = activeActorDisplayName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${activeActor.hp}/${activeActor.maxHp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.74f)
                                    )
                                    LinearProgressIndicator(
                                        progress = { if (isPlayerTurn) 1f else 0.45f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .padding(top = 2.dp),
                                        color = accentColor,
                                        trackColor = Color(0xFF1B1F29)
                                    )
                                }
                            }

                            // 5 Command Buttons matching Android CommandPalette OR Targeted Selection Badge
                            if (pendingTargetInstruction != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0A0C10).copy(alpha = 0.90f))
                                        .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(accentColor)
                                        )
                                        Text(
                                            text = pendingTargetInstruction!!.uppercase(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            pendingTargetInstruction = null
                                            pendingTargetAction = null
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                val commands = listOf(
                                    Triple("Attack", Icons.Rounded.Whatshot) {
                                        val basicAttackName = if (activeActorIndex == 0) "Attack" else "Strike"
                                        pendingTargetInstruction = "Choose an enemy for $basicAttackName"
                                        pendingTargetAction = { targetIdx ->
                                            executePlayerAttack(if (activeActorIndex == 0) 35 else 42, basicAttackName, 0, targetIdx)
                                        }
                                    },
                                    Triple("Abilities", Icons.Rounded.AutoAwesome) {
                                        currentSubMenu = CombatSubMenu.SKILLS
                                    },
                                    Triple("Items", Icons.Rounded.Inventory2) {
                                        currentSubMenu = CombatSubMenu.ITEMS
                                    },
                                    Triple("Snack", Icons.Rounded.Restaurant) {
                                        currentSubMenu = CombatSubMenu.SNACK
                                    },
                                    Triple("Retreat", Icons.AutoMirrored.Rounded.ExitToApp) {
                                        onFlee()
                                    }
                                )

                                val rows = listOf(commands.take(3), commands.drop(3))
                                rows.forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        chunk.forEach { (label, icon, action) ->
                                            DesktopCombatCommandButton(
                                                label = label,
                                                icon = icon,
                                                enabled = isPlayerTurn,
                                                onClick = action,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Command Palette Corner Accent Markers (Android Canvas Match)
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        val strokeWidth = 3.dp.toPx()
                        val markerLength = 32.dp.toPx()
                        val y = strokeWidth / 2f
                        val tint = accentColor.copy(alpha = 0.9f)
                        drawLine(
                            color = tint,
                            start = Offset(0f, y),
                            end = Offset(markerLength, y),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = tint,
                            start = Offset(size.width - markerLength, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Abilities / Skills Dialog Modal (Matching Android SkillsDialog with Character-Specific Skill Decks)
        if (currentSubMenu == CombatSubMenu.SKILLS) {
            val abilities = when (activeActor.id) {
                "nova" -> listOf(
                    Triple("Plasma Overcharge", "Supercharge blaster cell with ionized plasma to deal massive thermal burst damage.", 30 to 60),
                    Triple("Cosmic Railgun", "Fire high-velocity kinetic round piercing heavy defensive plating.", 50 to 95),
                    Triple("EMP Disruptor", "Release electromagnetic blast disabling enemy barriers and staggering stability.", 40 to 45)
                )
                "zeke" -> listOf(
                    Triple("Hydraulic Kick", "High-torque mechanical kick rupturing enemy armor stability.", 25 to 55),
                    Triple("Overload Slam", "Channel generator surge into an earth-shattering ground slam.", 45 to 85),
                    Triple("Kinetic Barrier", "Fortify shielding with reactive magnetic plates.", 20 to 0)
                )
                "orion" -> listOf(
                    Triple("Void Siphon", "Channel stellar resonance to drain vital energy from target.", 35 to 65),
                    Triple("Cosmic Flare", "Ignite ambient stellar dust creating blinding burst damage.", 55 to 105),
                    Triple("Astral Ward", "Weave protective ward bolstering squad defenses.", 30 to 0)
                )
                "gh0st" -> listOf(
                    Triple("Headshot", "Precision armor-piercing strike exploiting mechanical vulnerabilities.", 35 to 80),
                    Triple("Phase Strike", "Blink through rift space striking behind enemy defenses.", 45 to 90),
                    Triple("Smoke Screen", "Deploy tactical aerosol cloaking signature.", 25 to 0)
                )
                else -> listOf(
                    Triple("Kinetic Pulse", "Discharge standard tactical beam.", 20 to 40)
                )
            }

            AlertDialog(
                onDismissRequest = { currentSubMenu = CombatSubMenu.ROOT },
                containerColor = Color(0xFF061018).copy(alpha = 0.96f),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.88f),
                shape = RoundedCornerShape(18.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF7BE4FF)
                            )
                            Text(
                                text = "$activeActorDisplayName Abilities",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF7BE4FF).copy(alpha = 0.80f), Color.Transparent)
                                    )
                                )
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        abilities.forEach { (name, desc, costAndDmg) ->
                            val (cost, dmg) = costAndDmg
                            val canUse = activeActor.energy >= cost
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(1.dp, Color(0xFF5CCBE8).copy(alpha = 0.34f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            lineHeight = 14.sp
                                        )
                                        Text(
                                            text = "Cost: $cost EP  •  Power: $dmg DMG",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF7BE4FF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = {
                                            currentSubMenu = CombatSubMenu.ROOT
                                            pendingTargetInstruction = "Choose an enemy for $name"
                                            pendingTargetAction = { targetIdx ->
                                                executePlayerAttack(dmg, name, cost, targetIdx)
                                            }
                                        },
                                        enabled = canUse,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7BE4FF), contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Select", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { currentSubMenu = CombatSubMenu.ROOT }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color(0xFF7BE4FF))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Close", color = Color.White)
                    }
                },
                modifier = Modifier.border(1.5.dp, Color(0xFF5CCBE8).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            )
        }

        // Combat Items Dialog Modal (Matching Android CombatItemsDialog)
        if (currentSubMenu == CombatSubMenu.ITEMS) {
            val consumableItems = remember(inventoryConsumables) {
                if (inventoryConsumables.isNotEmpty()) inventoryConsumables else {
                    listOfNotNull(
                        allItemsMap["nanite_medkit"]?.let { it to 2 } ?: (Item(id = "nanite_medkit", name = "Nanite Medkit", type = "consumable", description = "Field medical injector") to 2),
                        allItemsMap["shield_cell"]?.let { it to 1 } ?: (Item(id = "shield_cell", name = "Shield Cell", type = "consumable", description = "Auxiliary battery capacitor") to 1)
                    )
                }
            }

            AlertDialog(
                onDismissRequest = { currentSubMenu = CombatSubMenu.ROOT },
                containerColor = Color(0xFF061018).copy(alpha = 0.96f),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.88f),
                shape = RoundedCornerShape(18.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF7BE4FF)
                            )
                            Text(
                                text = "Items",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF7BE4FF).copy(alpha = 0.80f), Color.Transparent)
                                    )
                                )
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        consumableItems.forEach { (item, qty) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(1.dp, Color(0xFF5CCBE8).copy(alpha = 0.34f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = item.description ?: "Consumable field support item",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "Quantity: x$qty",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF7BE4FF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = {
                                            currentSubMenu = CombatSubMenu.ROOT
                                            if (item.id.contains("shield", ignoreCase = true)) {
                                                activeActor.shield = (activeActor.shield + 50).coerceAtMost(activeActor.maxShield)
                                                battleLogs.add(0, "▸ Used [${item.name}] on ${activeActor.name} restoring +50 Shield!")
                                                spawnFloatingText("+50 SHIELD", ShieldBlue)
                                            } else {
                                                activeActor.hp = (activeActor.hp + 70).coerceAtMost(activeActor.maxHp)
                                                battleLogs.add(0, "▸ Used [${item.name}] on ${activeActor.name} restoring +70 HP!")
                                                spawnFloatingText("+70 HP", HealthGreen)
                                            }
                                            isPlayerTurn = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7BE4FF), contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Use", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { currentSubMenu = CombatSubMenu.ROOT }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color(0xFF7BE4FF))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Close", color = Color.White)
                    }
                },
                modifier = Modifier.border(1.5.dp, Color(0xFF5CCBE8).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            )
        }

        // Crew Snack Confirmation Dialog
        if (currentSubMenu == CombatSubMenu.SNACK) {
            AlertDialog(
                onDismissRequest = { currentSubMenu = CombatSubMenu.ROOT },
                containerColor = Color(0xFF061018).copy(alpha = 0.96f),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.88f),
                shape = RoundedCornerShape(18.dp),
                title = {
                    Text("Crew Snack", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                },
                text = {
                    Text("Consume [Zeke's Smoked Jerky] to revitalize the squad and restore +40 Energy Points (EP)?", color = Color.White.copy(alpha = 0.88f))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentSubMenu = CombatSubMenu.ROOT
                            activeActor.energy = (activeActor.energy + 40).coerceAtMost(activeActor.maxEnergy)
                            battleLogs.add(0, "▸ Munched on [Zeke's Smoked Jerky] (+40 Energy)!")
                            spawnFloatingText("+40 EP", EnergyYellow)
                            isPlayerTurn = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F2E), contentColor = Color.Black)
                    ) {
                        Text("Eat Snack", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { currentSubMenu = CombatSubMenu.ROOT }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                modifier = Modifier.border(1.5.dp, Color(0xFFFF9F2E).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            )
        }

        // Two-Stage Victory Dialog Modal (Matching Android VictorySpoilsDialog 1:1)
        if (showVictoryDialog) {
            val panelColor = Color(0xFF21130D).copy(alpha = 0.96f)
            val cardColor = Color(0xFF171A24).copy(alpha = 0.92f)
            val accentColor = Color(0xFFFF922B)
            val borderColor = Color(0xFFFF922B)

            var victoryStage by remember { mutableStateOf(0) } // 0 = SPOILS, 1 = LEVEL_UPS

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = panelColor,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 18.dp,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.74f)),
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(accentColor.copy(alpha = 0.16f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.66f)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (victoryStage == 0) Icons.Filled.EmojiEvents else Icons.Outlined.School,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (victoryStage == 0) "BATTLE REWARDS" else "PROGRESSION",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Text(
                                    text = if (victoryStage == 0) "Spoils Recovered" else "Level Up!",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(accentColor.copy(alpha = 0.48f))
                        )

                        // Stage Content
                        if (victoryStage == 0) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = cardColor.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.54f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                            Text(text = "Experience", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f))
                                            Text(text = "+$earnedXp XP", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        }
                                    }
                                    Surface(
                                        color = cardColor.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.54f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                            Text(text = "Credits", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f))
                                            Text(text = "+$earnedCredits CR", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        }
                                    }
                                }

                                Text(text = "Loot Recovered", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                if (earnedLoot.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        earnedLoot.forEach { (item, qty) ->
                                            Surface(
                                                color = cardColor.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = item.name, color = Color.White)
                                                    Text(text = "×$qty", color = accentColor, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = cardColor.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No salvageable components found.",
                                            color = Color.White.copy(alpha = 0.50f),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                partyMembers.forEach { member ->
                                    val memberLevel = sessionState.partyMemberLevels[member.id] ?: sessionState.playerLevel
                                    val isLead = member.id == "nova"
                                    Surface(
                                        color = cardColor.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.54f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Image(
                                                painter = rememberDesktopAssetPainter(member.spritePath, services.assetProvider),
                                                contentDescription = member.name,
                                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "${member.name} — Level $memberLevel ${if (isLead) "Specialist" else member.role}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Max HP: ${member.maxHp}  •  XP: ${sessionState.playerXp}  •  Vitals Stable",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF63E6FF)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (victoryStage == 0) {
                                    victoryStage = 1
                                } else {
                                    onVictory()
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (victoryStage == 0) "Next" else "Continue",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Defeat Dialog Modal
        if (showDefeatDialog) {
            AlertDialog(
                onDismissRequest = onDefeat,
                title = {
                    Text(text = "CRITICAL FAILURE // VITAL SIGNS LOST", color = HealthRed, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                },
                text = {
                    Text(text = "Nova sustained critical hull trauma and was extracted to stasis emergency recovery.", color = FieldMenuDesign.text)
                },
                confirmButton = {
                    Button(onClick = onDefeat, colors = ButtonDefaults.buttonColors(containerColor = HealthRed)) {
                        Text(text = "RETURN TO TITLE MENU", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = FieldMenuDesign.shell,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.5.dp, HealthRed, RoundedCornerShape(16.dp))
            )
        }
    }
}



@Composable
private fun DesktopFlankPlayerEntity(
    name: String,
    role: String,
    spritePath: String,
    hp: Int,
    maxHp: Int,
    shield: Int,
    maxShield: Int,
    energy: Int,
    maxEnergy: Int,
    isGuarding: Boolean,
    isReady: Boolean,
    isBackline: Boolean = false,
    cardWidth: androidx.compose.ui.unit.Dp = 145.dp,
    spriteSize: androidx.compose.ui.unit.Dp = 100.dp,
    services: DesktopAppServices
) {
    val sprite = rememberDesktopAssetPainter(spritePath, services.assetProvider)

    // Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "player_breathing_$name")
    val breathWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathWave"
    )
    val breathScaleY = 1f + 0.015f * sin(breathWave)

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraPulse"
    )

    val depthScale = if (isBackline) 0.88f else 1.0f
    val depthAlpha = if (isBackline) 0.88f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = depthScale
            scaleY = depthScale
            alpha = depthAlpha
        }
    ) {
        // Ground Stage Entity with Shadow & Ready Aura
        Box(
            modifier = Modifier.size(spriteSize + 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Drop Shadow
            Canvas(modifier = Modifier.fillMaxWidth().height(18.dp).align(Alignment.BottomCenter)) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.60f),
                    topLeft = Offset(size.width * 0.15f, 0f),
                    size = Size(size.width * 0.70f, size.height)
                )
            }

            // Active Actor Pulsing Ground Ring
            if (isReady) {
                Canvas(modifier = Modifier.size(spriteSize * 1.05f).align(Alignment.BottomCenter)) {
                    drawOval(
                        color = NeonCyan.copy(alpha = auraPulse),
                        style = Stroke(width = 2.5.dp.toPx()),
                        topLeft = Offset(0f, size.height * 0.60f),
                        size = Size(size.width, size.height * 0.35f)
                    )
                }
            }

            // Character Sprite
            Image(
                painter = sprite,
                contentDescription = name,
                modifier = Modifier
                    .size(spriteSize)
                    .graphicsLayer {
                        scaleY = breathScaleY
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Compact Character Status Card (Neatly docked beneath the hero)
        Box(
            modifier = Modifier
                .width(cardWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF040A12).copy(alpha = 0.90f))
                .border(BorderStroke(1.2.dp, if (isReady) NeonCyan else Color(0xFF1E2A3A)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name.uppercase(),
                        color = if (isReady) NeonCyan else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    if (isGuarding) {
                        Text(text = "🛡 GUARD", color = ShieldBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // HP Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "HP", color = HealthGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "$hp/$maxHp", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                LinearProgressIndicator(
                    progress = { hp.toFloat() / maxHp },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = HealthGreen,
                    trackColor = Color(0xFF102018)
                )

                // Shield Bar (if active)
                if (maxShield > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "SHD", color = ShieldBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = "$shield/$maxShield", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                    LinearProgressIndicator(
                        progress = { shield.toFloat() / maxShield },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = ShieldBlue,
                        trackColor = Color(0xFF0F1828)
                    )
                }

                // Energy Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "EP", color = EnergyYellow, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "$energy/$maxEnergy", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                LinearProgressIndicator(
                    progress = { energy.toFloat() / maxEnergy },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = EnergyYellow,
                    trackColor = Color(0xFF221E10)
                )
            }
        }
    }
}

@Composable
private fun DesktopFlankEnemyEntity(
    enemy: DesktopCombatant,
    isSelected: Boolean,
    isBackline: Boolean = false,
    cardWidth: androidx.compose.ui.unit.Dp = 150.dp,
    spriteSize: androidx.compose.ui.unit.Dp = 100.dp,
    services: DesktopAppServices,
    onSelect: () -> Unit
) {
    val sprite = rememberDesktopAssetPainter(enemy.spritePath, services.assetProvider)

    // Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "enemy_breathing_${enemy.id}")
    val breathWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "enemyBreathWave"
    )
    val breathScaleY = 1f + 0.02f * sin(breathWave)

    val depthScale = if (isBackline) 0.88f else 1.0f
    val depthAlpha = if (isBackline) 0.88f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onSelect)
            .graphicsLayer {
                scaleX = depthScale
                scaleY = depthScale
                alpha = depthAlpha
            }
    ) {
        // Ground Stage Enemy Sprite with Drop Shadow & Circular Target Reticle
        Box(
            modifier = Modifier.size(spriteSize + 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Drop Shadow
            Canvas(modifier = Modifier.fillMaxWidth().height(18.dp).align(Alignment.BottomCenter)) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.60f),
                    topLeft = Offset(size.width * 0.15f, 0f),
                    size = Size(size.width * 0.70f, size.height)
                )
            }

            // Target Reticle Ring (when selected)
            if (isSelected) {
                Canvas(modifier = Modifier.size(spriteSize * 0.95f).align(Alignment.BottomCenter)) {
                    drawOval(
                        color = NeonAmber,
                        style = Stroke(width = 2.dp.toPx()),
                        topLeft = Offset(0f, size.height * 0.55f),
                        size = Size(size.width, size.height * 0.40f)
                    )
                }
            }

            // Enemy Sprite
            Image(
                painter = sprite,
                contentDescription = enemy.name,
                modifier = Modifier
                    .size(spriteSize)
                    .graphicsLayer {
                        scaleY = breathScaleY
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Compact Enemy Status Card (Neatly docked beneath the enemy)
        Box(
            modifier = Modifier
                .width(cardWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0x33FF9F2E) else Color(0xFF040A12).copy(alpha = 0.90f))
                .border(
                    BorderStroke(1.5.dp, if (isSelected) NeonAmber else Color(0xFF1E2A3A)),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = enemy.name.uppercase(),
                        color = if (isSelected) NeonAmber else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Text(text = "[TARGET]", color = NeonAmber, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }

                // Health Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "HP", color = HealthRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "${enemy.hp}/${enemy.maxHp}", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                LinearProgressIndicator(
                    progress = { enemy.hp.toFloat() / enemy.maxHp },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = HealthRed,
                    trackColor = Color(0xFF241018)
                )

                // Shield Bar (if has shield)
                if (enemy.maxShield > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "SHD", color = ShieldBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(text = "${enemy.shield}/${enemy.maxShield}", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                    LinearProgressIndicator(
                        progress = { enemy.shield.toFloat() / enemy.maxShield },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = ShieldBlue,
                        trackColor = Color(0xFF0F1828)
                    )
                }

                // Stability / Break Gauge
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = if (enemy.isBroken) "⚡ STAGGER" else "STABILITY", color = StabilityAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "${enemy.stability}/${enemy.maxStability}", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                LinearProgressIndicator(
                    progress = { enemy.stability.toFloat() / enemy.maxStability },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = StabilityAmber,
                    trackColor = Color(0xFF24180A)
                )

                // Hostile Action Intent with Icon
                val intentIcon = when {
                    enemy.intent.contains("SLAM", ignoreCase = true) || enemy.intent.contains("GNAW", ignoreCase = true) -> "⚔️"
                    enemy.intent.contains("EMP", ignoreCase = true) || enemy.intent.contains("STATIC", ignoreCase = true) -> "⚡"
                    enemy.intent.contains("SHIELD", ignoreCase = true) || enemy.intent.contains("GUARD", ignoreCase = true) -> "🛡️"
                    else -> "💥"
                }
                Text(
                    text = "$intentIcon ${enemy.intent}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DesktopCombatCommandButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    cooldownRemaining: Int = 0,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (enabled) Color(0xFF1E2534) else Color(0xFF1B1F29)
    Box(
        modifier = modifier
            .widthIn(min = 88.dp)
            .heightIn(min = 54.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = background,
            tonalElevation = if (enabled) 4.dp else 0.dp,
            border = BorderStroke(1.dp, if (enabled) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (cooldownRemaining > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cooldownRemaining.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                )
            }
        }
    }
}

