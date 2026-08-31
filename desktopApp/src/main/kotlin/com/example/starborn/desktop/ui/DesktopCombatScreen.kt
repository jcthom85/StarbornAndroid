package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.combat.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NeonCyan = Color(0xFF00F5D4)
private val NeonPink = Color(0xFFFF007F)
private val NeonAmber = Color(0xFFFFB703)
private val DeepSpaceDark = Color(0xFF060913)
private val PanelDark = Color(0xFF0A0F1E)
private val PanelBorder = Color(0xFF1B283E)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFF8FA1B7)
private val HealthGreen = Color(0xFF00E676)
private val HealthRed = Color(0xFFFF3366)
private val ShieldBlue = Color(0xFF2979FF)

data class FloatingCombatText(
    val id: Long,
    val text: String,
    val color: Color,
    val isCritical: Boolean = false
)

enum class CombatActionMenu {
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

    // 1. Initialize Combat Encounter State
    var playerHp by remember { mutableStateOf(240) }
    val playerMaxHp = 240
    var playerShield by remember { mutableStateOf(100) }
    val playerMaxShield = 100
    var playerEnergy by remember { mutableStateOf(60) }
    val playerMaxEnergy = 80

    // Enemy States
    var enemies by remember {
        mutableStateOf(
            listOf(
                DesktopEnemyState("scrapper_guard", "Scrapper Vanguard", 160, 160, 50, 50, "Heavy Strike"),
                DesktopEnemyState("scrapper_drone", "Automaton Scout", 110, 110, 20, 20, "Pulse Laser")
            )
        )
    }

    var selectedEnemyIndex by remember { mutableStateOf(0) }
    var actionMenu by remember { mutableStateOf(CombatActionMenu.ROOT) }
    var combatLog by remember { mutableStateOf(listOf("Tactical encounter initialized. Commander holds initiative.")) }
    val floatingTexts = remember { mutableStateListOf<FloatingCombatText>() }
    var isPlayerTurn by remember { mutableStateOf(true) }

    // Start Combat BGM
    LaunchedEffect(Unit) {
        val cmds = services.audioRouter.commandsForBattle("battle_start")
        services.audioDriver.executeAll(cmds)
    }

    // Execute Player Attack
    val executePlayerAttack: (Int, String) -> Unit = { damage, skillName ->
        if (isPlayerTurn && enemies.isNotEmpty()) {
            val targetIndex = selectedEnemyIndex.coerceIn(0, enemies.size - 1)
            val target = enemies[targetIndex]

            // Calculate damage on target
            val shieldDmg = minOf(target.shield, damage)
            val remainDmg = damage - shieldDmg
            val newShield = (target.shield - shieldDmg).coerceAtLeast(0)
            val newHp = (target.hp - remainDmg).coerceAtLeast(0)

            floatingTexts.add(
                FloatingCombatText(
                    id = System.currentTimeMillis(),
                    text = "-$damage",
                    color = if (skillName == "Plasma Burst") NeonCyan else NeonAmber,
                    isCritical = damage > 70
                )
            )

            combatLog = combatLog + "Nova executed [$skillName] on ${target.name} for $damage DMG!"

            if (newHp <= 0) {
                combatLog = combatLog + "${target.name} was neutralized!"
                enemies = enemies.filterIndexed { idx, _ -> idx != targetIndex }
                selectedEnemyIndex = 0
            } else {
                enemies = enemies.mapIndexed { idx, e ->
                    if (idx == targetIndex) e.copy(hp = newHp, shield = newShield) else e
                }
            }

            // Check Victory
            if (enemies.isEmpty()) {
                combatLog = combatLog + ">> VICTORY: All hostiles eliminated!"
                coroutineScope.launch {
                    delay(1500)
                    onVictory()
                }
            } else {
                // Enemy Turn Counterattack
                isPlayerTurn = false
                coroutineScope.launch {
                    delay(900)
                    enemies.forEach { enemy ->
                        val enemyDmg = (20..35).random()
                        val pShieldDmg = minOf(playerShield, enemyDmg)
                        val pRemainDmg = enemyDmg - pShieldDmg
                        playerShield = (playerShield - pShieldDmg).coerceAtLeast(0)
                        playerHp = (playerHp - pRemainDmg).coerceAtLeast(0)

                        floatingTexts.add(
                            FloatingCombatText(
                                id = System.currentTimeMillis() + (1..100).random(),
                                text = "-$enemyDmg",
                                color = HealthRed
                            )
                        )
                        combatLog = combatLog + "${enemy.name} used [${enemy.intent}] for $enemyDmg DMG on Nova!"
                        delay(600)
                    }

                    if (playerHp <= 0) {
                        combatLog = combatLog + ">> DEFEAT: Hull integrity compromised!"
                        delay(1500)
                        onDefeat()
                    } else {
                        // Restore energy & return to player turn
                        playerEnergy = minOf(playerMaxEnergy, playerEnergy + 15)
                        isPlayerTurn = true
                        actionMenu = CombatActionMenu.ROOT
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Tab, Key.DirectionRight -> {
                            if (enemies.isNotEmpty()) {
                                selectedEnemyIndex = (selectedEnemyIndex + 1) % enemies.size
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            if (enemies.isNotEmpty()) {
                                selectedEnemyIndex = if (selectedEnemyIndex - 1 < 0) enemies.size - 1 else selectedEnemyIndex - 1
                            }
                            true
                        }
                        Key.One -> {
                            if (actionMenu == CombatActionMenu.ROOT) {
                                executePlayerAttack(45, "Kinetic Strike")
                            } else if (actionMenu == CombatActionMenu.SKILLS) {
                                if (playerEnergy >= 25) {
                                    playerEnergy -= 25
                                    executePlayerAttack(85, "Plasma Burst")
                                }
                            }
                            true
                        }
                        Key.Two -> {
                            if (actionMenu == CombatActionMenu.ROOT) {
                                actionMenu = CombatActionMenu.SKILLS
                            } else if (actionMenu == CombatActionMenu.SKILLS) {
                                if (playerEnergy >= 40) {
                                    playerEnergy -= 40
                                    executePlayerAttack(130, "Supercharge Railgun")
                                }
                            }
                            true
                        }
                        Key.Three -> {
                            if (actionMenu == CombatActionMenu.ROOT) {
                                // Defend / Shield Restore
                                playerShield = minOf(playerMaxShield, playerShield + 35)
                                combatLog = combatLog + "Nova raised Defense Matrix (+35 Shield)!"
                                isPlayerTurn = false
                                coroutineScope.launch {
                                    delay(800)
                                    isPlayerTurn = true
                                }
                            }
                            true
                        }
                        Key.Four -> {
                            // Nano-Injector Item
                            playerHp = minOf(playerMaxHp, playerHp + 60)
                            combatLog = combatLog + "Nova injected Nano-Repair (+60 HP)!"
                            true
                        }
                        Key.Five, Key.Escape -> {
                            if (actionMenu != CombatActionMenu.ROOT) {
                                actionMenu = CombatActionMenu.ROOT
                            } else {
                                onFlee()
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Battle Backdrop Artwork
        val backdrop = rememberDesktopAssetPainter("images/rooms/combat_arena.webp", services.assetProvider)
        Image(
            painter = backdrop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark atmospheric gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD060913),
                            Color(0x77060913),
                            Color(0xF0060913)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Timeline Bar: Turn Order Sequence
            DesktopCombatTimelineBar(enemies, isPlayerTurn)

            // Main Battle Stage: Left (Enemies) vs Right (Player Crew)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Flank: Enemy Party
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "HOSTILE TARGETS [TAB: CYCLE]",
                        color = HealthRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    enemies.forEachIndexed { index, enemy ->
                        DesktopEnemyCard(
                            enemy = enemy,
                            isSelected = selectedEnemyIndex == index,
                            services = services,
                            onClick = { selectedEnemyIndex = index }
                        )
                    }
                }

                // Center Combat Action / Floating Damage Display
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    floatingTexts.takeLast(3).forEach { ft ->
                        Text(
                            text = ft.text,
                            color = ft.color,
                            fontSize = if (ft.isCritical) 32.sp else 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // Right Flank: Player Crew Battlers
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "VANGUARD SQUAD",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    DesktopCrewBattlerCard(
                        name = "Nova (Commander)",
                        role = "Vanguard Operative",
                        hp = playerHp,
                        maxHp = playerMaxHp,
                        shield = playerShield,
                        maxShield = playerMaxShield,
                        energy = playerEnergy,
                        maxEnergy = playerMaxEnergy,
                        isActiveTurn = isPlayerTurn,
                        services = services
                    )
                }
            }

            // Bottom Console: Tactical Hotkey Console & Combat Directives
            DesktopTacticalCommandConsole(
                actionMenu = actionMenu,
                isPlayerTurn = isPlayerTurn,
                playerEnergy = playerEnergy,
                combatLog = combatLog,
                onAttack = { executePlayerAttack(45, "Kinetic Strike") },
                onOpenSkills = { actionMenu = CombatActionMenu.SKILLS },
                onSkill1 = {
                    if (playerEnergy >= 25) {
                        playerEnergy -= 25
                        executePlayerAttack(85, "Plasma Burst")
                    }
                },
                onSkill2 = {
                    if (playerEnergy >= 40) {
                        playerEnergy -= 40
                        executePlayerAttack(130, "Supercharge Railgun")
                    }
                },
                onDefend = {
                    playerShield = minOf(playerMaxShield, playerShield + 35)
                    combatLog = combatLog + "Nova raised Defense Matrix (+35 Shield)!"
                    isPlayerTurn = false
                    coroutineScope.launch {
                        delay(800)
                        isPlayerTurn = true
                    }
                },
                onItem = {
                    playerHp = minOf(playerMaxHp, playerHp + 60)
                    combatLog = combatLog + "Nova injected Nano-Repair (+60 HP)!"
                },
                onBack = { actionMenu = CombatActionMenu.ROOT },
                onFlee = onFlee
            )
        }
    }
}

data class DesktopEnemyState(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val shield: Int,
    val maxShield: Int,
    val intent: String
)

@Composable
private fun DesktopCombatTimelineBar(enemies: List<DesktopEnemyState>, isPlayerTurn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF0050812))
            .border(BorderStroke(1.dp, PanelBorder))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TURN TIMELINE:",
                color = NeonAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Timeline badges
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPlayerTurn) NeonCyan.copy(alpha = 0.25f) else Color(0xFF141F32))
                    .border(BorderStroke(1.dp, if (isPlayerTurn) NeonCyan else PanelBorder), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "1. NOVA (ACTIVE)",
                    color = if (isPlayerTurn) NeonCyan else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            enemies.forEachIndexed { idx, enemy ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isPlayerTurn && idx == 0) HealthRed.copy(alpha = 0.25f) else Color(0xFF141F32))
                        .border(BorderStroke(1.dp, if (!isPlayerTurn && idx == 0) HealthRed else PanelBorder), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${idx + 2}. ${enemy.name.uppercase()}",
                        color = if (!isPlayerTurn && idx == 0) HealthRed else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            text = "TURN-BASED TACTICAL SIMULATION",
            color = TextMuted.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DesktopEnemyCard(
    enemy: DesktopEnemyState,
    isSelected: Boolean,
    services: DesktopAppServices,
    onClick: () -> Unit
) {
    val enemySprite = rememberDesktopAssetPainter("images/enemies/${enemy.id}.webp", services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .shadow(if (isSelected) 10.dp else 2.dp, RoundedCornerShape(12.dp), spotColor = HealthRed)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF1C1322) else PanelDark.copy(alpha = 0.9f))
            .border(
                BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) HealthRed else PanelBorder),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = enemySprite,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, if (isSelected) HealthRed else PanelBorder), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = enemy.name,
                        color = if (isSelected) HealthRed else TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "INTENT: ${enemy.intent.uppercase()}",
                        color = NeonAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // HP Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "HP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${enemy.hp}/${enemy.maxHp}", color = TextWhite, fontSize = 10.sp)
                }
                LinearProgressIndicator(
                    progress = { enemy.hp.toFloat() / enemy.maxHp.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = HealthRed,
                    trackColor = Color(0xFF22121C)
                )

                if (enemy.maxShield > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { enemy.shield.toFloat() / enemy.maxShield.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ShieldBlue,
                        trackColor = Color(0xFF131E30)
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopCrewBattlerCard(
    name: String,
    role: String,
    hp: Int,
    maxHp: Int,
    shield: Int,
    maxShield: Int,
    energy: Int,
    maxEnergy: Int,
    isActiveTurn: Boolean,
    services: DesktopAppServices
) {
    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .shadow(if (isActiveTurn) 12.dp else 2.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActiveTurn) Color(0xFF0F1E33) else PanelDark.copy(alpha = 0.9f))
            .border(
                BorderStroke(if (isActiveTurn) 1.5.dp else 1.dp, if (isActiveTurn) NeonCyan else PanelBorder),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = portrait,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f)), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = if (isActiveTurn) "ACTIVE TURN" else "WAITING", color = if (isActiveTurn) NeonAmber else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // HP Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "HULL INTEGRITY", color = TextMuted, fontSize = 10.sp)
                    Text(text = "$hp/$maxHp", color = TextWhite, fontSize = 10.sp)
                }
                LinearProgressIndicator(
                    progress = { hp.toFloat() / maxHp.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = HealthGreen,
                    trackColor = Color(0xFF14242A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Shield Bar
                LinearProgressIndicator(
                    progress = { shield.toFloat() / maxShield.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = ShieldBlue,
                    trackColor = Color(0xFF101B2E)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Energy Bar
                LinearProgressIndicator(
                    progress = { energy.toFloat() / maxEnergy.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = NeonAmber,
                    trackColor = Color(0xFF2E2210)
                )
            }
        }
    }
}

@Composable
private fun DesktopTacticalCommandConsole(
    actionMenu: CombatActionMenu,
    isPlayerTurn: Boolean,
    playerEnergy: Int,
    combatLog: List<String>,
    onAttack: () -> Unit,
    onOpenSkills: () -> Unit,
    onSkill1: () -> Unit,
    onSkill2: () -> Unit,
    onDefend: () -> Unit,
    onItem: () -> Unit,
    onBack: () -> Unit,
    onFlee: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFA060914))
            .border(BorderStroke(1.dp, PanelBorder))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Combat Directives Log Feed
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF04060C))
                    .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(text = "MISSION COMBAT LOG", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                combatLog.takeLast(3).forEach { entry ->
                    Text(
                        text = "> $entry",
                        color = TextWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            // Command Deck Buttons
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (actionMenu == CombatActionMenu.ROOT) {
                    DesktopCommandButton("[1] STRIKE", NeonCyan, enabled = isPlayerTurn, onClick = onAttack)
                    DesktopCommandButton("[2] SKILLS", NeonAmber, enabled = isPlayerTurn, onClick = onOpenSkills)
                    DesktopCommandButton("[3] DEFEND", ShieldBlue, enabled = isPlayerTurn, onClick = onDefend)
                    DesktopCommandButton("[4] REPAIR", HealthGreen, enabled = isPlayerTurn, onClick = onItem)
                    DesktopCommandButton("[5] RETREAT", HealthRed, enabled = isPlayerTurn, onClick = onFlee)
                } else if (actionMenu == CombatActionMenu.SKILLS) {
                    DesktopCommandButton(
                        "[1] Plasma Burst (25 EN)",
                        NeonCyan,
                        enabled = isPlayerTurn && playerEnergy >= 25,
                        onClick = onSkill1
                    )
                    DesktopCommandButton(
                        "[2] Railgun (40 EN)",
                        NeonAmber,
                        enabled = isPlayerTurn && playerEnergy >= 40,
                        onClick = onSkill2
                    )
                    DesktopCommandButton("[ESC] BACK", TextMuted, enabled = true, onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun DesktopCommandButton(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color,
            disabledContainerColor = Color(0xFF0C1220),
            disabledContentColor = TextMuted.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.8f) else PanelBorder),
        modifier = Modifier.height(56.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
