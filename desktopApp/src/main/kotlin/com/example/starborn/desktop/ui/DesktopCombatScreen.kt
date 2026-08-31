package com.example.starborn.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val NeonCyan = Color(0xFF63E6FF)
private val NeonAmber = Color(0xFFFF9F2E)
private val HealthGreen = Color(0xFF00E676)
private val HealthRed = Color(0xFFFF3366)
private val ShieldBlue = Color(0xFF2979FF)
private val EnergyYellow = Color(0xFFFFD54F)
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
    var hp: Int,
    val maxHp: Int,
    var shield: Int,
    val maxShield: Int,
    val intent: String,
    val speed: Int,
    val isBoss: Boolean = false
)

enum class CombatSubMenu {
    ROOT, SKILLS, ITEMS
}

@Composable
fun DesktopCombatScreen(
    services: DesktopAppServices,
    enemyIds: List<String> = listOf("scrapper_guard", "scrapper_drone"),
    onVictory: () -> Unit,
    onDefeat: () -> Unit,
    onFlee: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var playerHp by remember { mutableStateOf(240) }
    val playerMaxHp = 240
    var playerShield by remember { mutableStateOf(100) }
    val playerMaxShield = 100
    var playerEnergy by remember { mutableStateOf(60) }
    val playerMaxEnergy = 80
    var isGuarding by remember { mutableStateOf(false) }

    var enemies by remember {
        mutableStateOf(
            listOf(
                DesktopCombatant("scrapper_guard", "Scrapper Vanguard", 160, 160, 50, 50, "Heavy Strike (-40 DMG)", 8),
                DesktopCombatant("scrapper_drone", "Automaton Scout", 110, 110, 20, 20, "Pulse Laser (-25 DMG)", 12)
            )
        )
    }

    var selectedEnemyIndex by remember { mutableStateOf(0) }
    var currentSubMenu by remember { mutableStateOf(CombatSubMenu.ROOT) }
    val floatingTexts = remember { mutableStateListOf<FloatingCombatText>() }
    val battleLogs = remember { mutableStateListOf("◆ ENGAGED HOSTILE SECTOR ENCOUNTER // BATTLE STATIONS ACTIVE") }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var showVictoryDialog by remember { mutableStateOf(false) }
    var showDefeatDialog by remember { mutableStateOf(false) }

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

    // Execute Player Attack
    val executePlayerAttack: (Int, String, Int) -> Unit = { damage, skillName, energyCost ->
        if (isPlayerTurn && enemies.isNotEmpty() && playerEnergy >= energyCost) {
            playerEnergy = (playerEnergy - energyCost).coerceAtLeast(0)
            val targetIdx = selectedEnemyIndex.coerceIn(0, enemies.size - 1)
            val target = enemies[targetIdx]
            isPlayerTurn = false
            currentSubMenu = CombatSubMenu.ROOT

            val isCrit = Random.nextFloat() < 0.25f
            val actualDmg = if (isCrit) (damage * 1.5f).toInt() else damage

            battleLogs.add(0, "▸ Nova used [$skillName] on ${target.name} for $actualDmg damage!${if (isCrit) " (CRITICAL HIT)" else ""}")
            spawnFloatingText("-$actualDmg", if (isCrit) HealthRed else NeonCyan, isCritical = isCrit, targetId = target.id)
            if (isCrit) triggerScreenShake()

            // Apply damage to Shield then HP
            var remaining = actualDmg
            if (target.shield > 0) {
                val shieldDmg = remaining.coerceAtMost(target.shield)
                target.shield -= shieldDmg
                remaining -= shieldDmg
            }
            target.hp = (target.hp - remaining).coerceAtLeast(0)

            // Check Enemy Defeat
            val updated = enemies.toMutableList()
            if (target.hp <= 0) {
                battleLogs.add(0, "☠ ${target.name} was neutralized!")
                updated.removeAt(targetIdx)
                selectedEnemyIndex = 0
            }
            enemies = updated

            if (enemies.isEmpty()) {
                showVictoryDialog = true
                battleLogs.add(0, "★ COMBAT VICTORY! Sector secured. Gained +120 XP and +65 Credits.")
                services.sessionStore.addCredits(65)
                services.sessionStore.restore(services.sessionStore.state.value.copy(playerXp = services.sessionStore.state.value.playerXp + 120))
            } else {
                // Enemy Counter-Turn Loop
                coroutineScope.launch {
                    delay(900)
                    enemies.forEach { enemy ->
                        if (playerHp > 0) {
                            val enemyDmg = if (enemy.id == "scrapper_guard") 35 else 22
                            val mitigatedDmg = if (isGuarding) (enemyDmg * 0.35f).toInt() else enemyDmg

                            battleLogs.add(0, "▸ ${enemy.name} used [${enemy.intent.split(" ").first()}] hitting Nova for $mitigatedDmg damage!")
                            spawnFloatingText("-$mitigatedDmg", HealthRed)

                            var rem = mitigatedDmg
                            if (playerShield > 0) {
                                val sDmg = rem.coerceAtMost(playerShield)
                                playerShield -= sDmg
                                rem -= sDmg
                            }
                            playerHp = (playerHp - rem).coerceAtLeast(0)
                            delay(600)
                        }
                    }

                    isGuarding = false
                    playerEnergy = (playerEnergy + 20).coerceAtMost(playerMaxEnergy)

                    if (playerHp <= 0) {
                        showDefeatDialog = true
                        battleLogs.add(0, "✖ CRITICAL FAILURE: Nova has fallen in combat.")
                    } else {
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
                                        playerHp = (playerHp + 70).coerceAtMost(playerMaxHp)
                                        battleLogs.add(0, "▸ Used [Nanite Medkit] restoring +70 HP!")
                                        spawnFloatingText("+70 HP", HealthGreen)
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
                                        playerShield = (playerShield + 50).coerceAtMost(playerMaxShield)
                                        battleLogs.add(0, "▸ Used [Shield Battery] restoring +50 Shield!")
                                        spawnFloatingText("+50 SHIELD", ShieldBlue)
                                        currentSubMenu = CombatSubMenu.ROOT
                                        isPlayerTurn = false
                                    }
                                }
                            }
                            true
                        }
                        Key.Three -> {
                            if (isPlayerTurn) {
                                when (currentSubMenu) {
                                    CombatSubMenu.ROOT -> currentSubMenu = CombatSubMenu.ITEMS
                                    CombatSubMenu.SKILLS -> currentSubMenu = CombatSubMenu.ROOT
                                    CombatSubMenu.ITEMS -> currentSubMenu = CombatSubMenu.ROOT
                                }
                            }
                            true
                        }
                        Key.Four -> {
                            if (isPlayerTurn && currentSubMenu == CombatSubMenu.ROOT) {
                                isGuarding = true
                                playerEnergy = (playerEnergy + 25).coerceAtMost(playerMaxEnergy)
                                battleLogs.add(0, "▸ Nova assumed [DEFENSIVE GUARD] position (+25 Energy, -65% Damage).")
                                spawnFloatingText("GUARDING", ShieldBlue)
                                isPlayerTurn = false
                            }
                            true
                        }
                        Key.Five -> {
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
        // 1. Panoramic Combat Arena Backdrop
        val bgPainter = rememberDesktopAssetPainter("bg_combat_arena", services.assetProvider)
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. CRT Scanline & Cinematic Vignette Overlays
        DesktopVignetteOverlay(intensity = 0.75f)
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.06f)

        // 3. Main Combat UI Viewport
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Turn Timeline Track (Matching Android initiative order)
            DesktopCombatTurnTrack(
                isPlayerTurn = isPlayerTurn,
                enemies = enemies,
                selectedEnemyIndex = selectedEnemyIndex
            )

            // Middle Section: Combat Arena (Enemy Formations & Player Stage)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Nova Player Vitals Card
                DesktopPlayerCombatCard(
                    playerHp = playerHp,
                    playerMaxHp = playerMaxHp,
                    playerShield = playerShield,
                    playerMaxShield = playerMaxShield,
                    playerEnergy = playerEnergy,
                    playerMaxEnergy = playerMaxEnergy,
                    isGuarding = isGuarding,
                    isPlayerTurn = isPlayerTurn,
                    services = services
                )

                // Center Floating FX Canvas
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
                            fontSize = if (fx.isCritical) 32.sp else 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.graphicsLayer {
                                shadowElevation = 12f
                            }
                        )
                    }
                }

                // Right: Enemy Formation Cards
                Column(
                    modifier = Modifier.width(360.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    enemies.forEachIndexed { idx, enemy ->
                        val isSelected = selectedEnemyIndex == idx
                        DesktopEnemyCombatCard(
                            enemy = enemy,
                            isSelected = isSelected,
                            onSelect = { selectedEnemyIndex = idx }
                        )
                    }
                }
            }

            // Bottom Section: Tactical Action Command Deck & Live Battle Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Command Action Deck
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                        .background(FieldMenuDesign.panel.copy(alpha = 0.95f))
                        .border(BorderStroke(1.2.dp, FieldMenuDesign.border.copy(alpha = 0.5f)), RoundedCornerShape(FieldMenuDesign.cardRadius))
                        .padding(18.dp)
                ) {
                    when (currentSubMenu) {
                        CombatSubMenu.ROOT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isPlayerTurn) "TACTICAL COMBAT DECK // SELECT COMMAND" else "ENEMY TURN IN PROGRESS...",
                                    color = if (isPlayerTurn) NeonCyan else NeonAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DesktopCombatActionButton("[1] ATTACK", NeonCyan, isPlayerTurn) { executePlayerAttack(35, "Kinetic Pulse", 0) }
                                    DesktopCombatActionButton("[2] SKILLS", EnergyYellow, isPlayerTurn) { currentSubMenu = CombatSubMenu.SKILLS }
                                    DesktopCombatActionButton("[3] ITEMS", HealthGreen, isPlayerTurn) { currentSubMenu = CombatSubMenu.ITEMS }
                                    DesktopCombatActionButton("[4] GUARD", ShieldBlue, isPlayerTurn) {
                                        isGuarding = true
                                        playerEnergy = (playerEnergy + 25).coerceAtMost(playerMaxEnergy)
                                        battleLogs.add(0, "▸ Nova assumed [DEFENSIVE GUARD] (+25 Energy, -65% Damage).")
                                        spawnFloatingText("GUARDING", ShieldBlue)
                                        isPlayerTurn = false
                                    }
                                    DesktopCombatActionButton("[5] FLEE", LocalCombatPink, isPlayerTurn) { onFlee() }
                                }
                            }
                        }
                        CombatSubMenu.SKILLS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "SELECT ADVANCED SKILL", color = EnergyYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "[ESC] BACK", color = FieldMenuDesign.textMuted, fontSize = 11.sp, modifier = Modifier.clickable { currentSubMenu = CombatSubMenu.ROOT })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DesktopCombatActionButton("PLASMA OVERCHARGE [30 EP]", EnergyYellow, playerEnergy >= 30) { executePlayerAttack(60, "Plasma Overcharge", 30) }
                                    DesktopCombatActionButton("COSMIC RAILGUN [50 EP]", NeonAmber, playerEnergy >= 50) { executePlayerAttack(95, "Cosmic Railgun", 50) }
                                }
                            }
                        }
                        CombatSubMenu.ITEMS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "SELECT COMBAT ITEM", color = HealthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "[ESC] BACK", color = FieldMenuDesign.textMuted, fontSize = 11.sp, modifier = Modifier.clickable { currentSubMenu = CombatSubMenu.ROOT })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DesktopCombatActionButton("NANITE MEDKIT (+70 HP)", HealthGreen, true) {
                                        playerHp = (playerHp + 70).coerceAtMost(playerMaxHp)
                                        battleLogs.add(0, "▸ Used [Nanite Medkit] restoring +70 HP!")
                                        spawnFloatingText("+70 HP", HealthGreen)
                                        currentSubMenu = CombatSubMenu.ROOT
                                        isPlayerTurn = false
                                    }
                                    DesktopCombatActionButton("SHIELD BATTERY (+50 SHIELD)", ShieldBlue, true) {
                                        playerShield = (playerShield + 50).coerceAtMost(playerMaxShield)
                                        battleLogs.add(0, "▸ Used [Shield Battery] restoring +50 Shield!")
                                        spawnFloatingText("+50 SHIELD", ShieldBlue)
                                        currentSubMenu = CombatSubMenu.ROOT
                                        isPlayerTurn = false
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Live Battle Log Panel (matching Android CombatLogPanel)
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .height(115.dp)
                        .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                        .background(Color(0xDD060A12))
                        .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.3f)), RoundedCornerShape(FieldMenuDesign.cardRadius))
                        .padding(12.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(battleLogs) { log ->
                            Text(text = log, color = FieldMenuDesign.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Victory Dialog Modal
        if (showVictoryDialog) {
            AlertDialog(
                onDismissRequest = onVictory,
                title = {
                    Text(text = "SECTOR SECURED // COMBAT VICTORY", color = NeonAmber, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "All hostile signatures eliminated from the combat perimeter.", color = FieldMenuDesign.text, fontSize = 13.sp)
                        Text(text = "Rewards Awarded: +120 XP  •  +65 Credits  •  1x Tech Scrap", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    Button(onClick = onVictory, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                        Text(text = "CONTINUE EXPEDITION", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = FieldMenuDesign.shell,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp))
            )
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
private fun DesktopCombatTurnTrack(
    isPlayerTurn: Boolean,
    enemies: List<DesktopCombatant>,
    selectedEnemyIndex: Int
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF061018).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "INITIATIVE TURN TRACK:", color = FieldMenuDesign.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                // Player Turn Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPlayerTurn) FieldMenuDesign.cyan.copy(alpha = 0.25f) else Color(0xFF101724))
                        .border(BorderStroke(1.dp, if (isPlayerTurn) FieldMenuDesign.cyan else Color(0xFF222C3E)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "NOVA (YOU)", color = if (isPlayerTurn) FieldMenuDesign.cyan else FieldMenuDesign.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Enemy Turn Chips
                enemies.forEachIndexed { idx, enemy ->
                    val isTargeted = selectedEnemyIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isPlayerTurn && isTargeted) LocalCombatPink.copy(alpha = 0.25f) else Color(0xFF101724))
                            .border(BorderStroke(1.dp, if (isTargeted) NeonAmber else Color(0xFF222C3E)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = enemy.name.uppercase(), color = if (isTargeted) NeonAmber else FieldMenuDesign.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(text = "[TAB] SWITCH TARGET", color = FieldMenuDesign.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DesktopPlayerCombatCard(
    playerHp: Int,
    playerMaxHp: Int,
    playerShield: Int,
    playerMaxShield: Int,
    playerEnergy: Int,
    playerMaxEnergy: Int,
    isGuarding: Boolean,
    isPlayerTurn: Boolean,
    services: DesktopAppServices
) {
    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Box(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
            .background(FieldMenuDesign.panel.copy(alpha = 0.95f))
            .border(BorderStroke(1.5.dp, if (isPlayerTurn) FieldMenuDesign.cyan else FieldMenuDesign.border.copy(alpha = 0.35f)), RoundedCornerShape(FieldMenuDesign.cardRadius))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = portrait,
                    contentDescription = "Nova",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(1.5.dp, FieldMenuDesign.cyan), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column {
                    Text(text = "NOVA // SPECIALIST", color = FieldMenuDesign.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (isGuarding) {
                        Text(text = "🛡 GUARD ACTIVE (-65%)", color = ShieldBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = if (isPlayerTurn) "READY FOR COMMAND" else "STANDBY", color = if (isPlayerTurn) HealthGreen else FieldMenuDesign.textMuted, fontSize = 11.sp)
                    }
                }
            }

            // Health Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "HEALTH", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$playerHp / $playerMaxHp", color = FieldMenuDesign.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { playerHp.toFloat() / playerMaxHp },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = HealthGreen,
                    trackColor = Color(0xFF102018)
                )
            }

            // Shield Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "SHIELD", color = ShieldBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$playerShield / $playerMaxShield", color = FieldMenuDesign.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { playerShield.toFloat() / playerMaxShield },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = ShieldBlue,
                    trackColor = Color(0xFF0F1828)
                )
            }

            // Energy Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "ENERGY (EP)", color = EnergyYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$playerEnergy / $playerMaxEnergy", color = FieldMenuDesign.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { playerEnergy.toFloat() / playerMaxEnergy },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = EnergyYellow,
                    trackColor = Color(0xFF221E10)
                )
            }
        }
    }
}

@Composable
private fun DesktopEnemyCombatCard(
    enemy: DesktopCombatant,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
            .background(if (isSelected) Color(0x33FF9F2E) else FieldMenuDesign.panel.copy(alpha = 0.90f))
            .border(
                BorderStroke(1.5.dp, if (isSelected) NeonAmber else FieldMenuDesign.border.copy(alpha = 0.25f)),
                RoundedCornerShape(FieldMenuDesign.cardRadius)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = enemy.name.uppercase(), color = if (isSelected) NeonAmber else FieldMenuDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (isSelected) {
                    Text(text = "[TARGET LOCK]", color = NeonAmber, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }

            // Health
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "HP: ${enemy.hp}/${enemy.maxHp}", color = HealthRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "SHIELD: ${enemy.shield}/${enemy.maxShield}", color = ShieldBlue, fontSize = 11.sp)
            }
            LinearProgressIndicator(
                progress = { enemy.hp.toFloat() / enemy.maxHp },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
                color = HealthRed,
                trackColor = Color(0xFF241018)
            )

            // Intent Forecast
            Text(text = "INTENT: ${enemy.intent}", color = FieldMenuDesign.textMuted, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
private fun DesktopCombatActionButton(
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
            .background(if (enabled) accentColor.copy(alpha = 0.18f) else Color(0xFF101520))
            .border(BorderStroke(1.dp, if (enabled) accentColor else Color(0xFF1E2838)), RoundedCornerShape(FieldMenuDesign.controlRadius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) accentColor else FieldMenuDesign.textMuted.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
