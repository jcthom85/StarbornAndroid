package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.starborn.desktop.DesktopAppServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NeonCyan = Color(0xFF00F5D4)
private val NeonAmber = Color(0xFFFFB703)
private val HealthGreen = Color(0xFF00E676)
private val HealthRed = Color(0xFFFF3366)
private val ShieldBlue = Color(0xFF2979FF)
private val GlassDark = Color(0xDD090E18)
private val GlassBorder = Color(0x3300F5D4)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFFA0B3C6)

data class FloatingCombatText(
    val id: Long,
    val text: String,
    val color: Color,
    val isCritical: Boolean = false
)

data class DesktopEnemyState(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val shield: Int,
    val maxShield: Int,
    val intent: String
)

enum class CombatActionMenu {
    ROOT, SKILLS
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
    val floatingTexts = remember { mutableStateListOf<FloatingCombatText>() }
    var isPlayerTurn by remember { mutableStateOf(true) }

    // Start Combat BGM
    LaunchedEffect(Unit) {
        val cmds = services.audioRouter.commandsForBattle("battle_start")
        services.audioDriver.executeAll(cmds)
    }

    val executePlayerAttack: (Int, String) -> Unit = { damage, skillName ->
        if (isPlayerTurn && enemies.isNotEmpty()) {
            val targetIndex = selectedEnemyIndex.coerceIn(0, enemies.size - 1)
            val target = enemies[targetIndex]

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

            if (newHp <= 0) {
                enemies = enemies.filterIndexed { idx, _ -> idx != targetIndex }
                selectedEnemyIndex = 0
            } else {
                enemies = enemies.mapIndexed { idx, e ->
                    if (idx == targetIndex) e.copy(hp = newHp, shield = newShield) else e
                }
            }

            // Check Victory
            if (enemies.isEmpty()) {
                coroutineScope.launch {
                    delay(1200)
                    onVictory()
                }
            } else {
                // Enemy Counterattack
                isPlayerTurn = false
                coroutineScope.launch {
                    delay(700)
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
                        delay(500)
                    }

                    if (playerHp <= 0) {
                        delay(1200)
                        onDefeat()
                    } else {
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
            .background(Color(0xFF04060C))
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
                                playerShield = minOf(playerMaxShield, playerShield + 35)
                                isPlayerTurn = false
                                coroutineScope.launch {
                                    delay(700)
                                    isPlayerTurn = true
                                }
                            }
                            true
                        }
                        Key.Four -> {
                            playerHp = minOf(playerMaxHp, playerHp + 60)
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
        // 1. Full-Bleed Combat Arena Background Art
        val arenaBg = rememberDesktopAssetPainter("images/rooms/combat_arena.webp", services.assetProvider)
        Image(
            painter = arenaBg,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Subtle Arena Gradient Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x7704060C), Color(0xDD04060C)),
                        radius = 1000f
                    )
                )
        )

        // 3. Top Floating Turn Timeline
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GlassDark)
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "TURN:", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                // Nova active pill
                Text(
                    text = "▶ NOVA",
                    color = if (isPlayerTurn) NeonCyan else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                enemies.forEachIndexed { idx, enemy ->
                    Text(
                        text = "• ${enemy.name.uppercase()}",
                        color = if (!isPlayerTurn && idx == 0) HealthRed else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (!isPlayerTurn && idx == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 4. Main Side-by-Side Combat Flanks (Enemies on Left, Battler on Right)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Flank: Enemies with floating overhead health bars
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                enemies.forEachIndexed { index, enemy ->
                    DesktopFloatingEnemyUnit(
                        enemy = enemy,
                        isSelected = selectedEnemyIndex == index,
                        services = services,
                        onClick = { selectedEnemyIndex = index }
                    )
                }
            }

            // Center Floating Damage Numbers
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                floatingTexts.takeLast(2).forEach { ft ->
                    Text(
                        text = ft.text,
                        color = ft.color,
                        fontSize = if (ft.isCritical) 36.sp else 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right Flank: Nova Player Battler
            DesktopFloatingPlayerUnit(
                name = "Commander Nova",
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

        // 5. Minimal Bottom Floating Command Console
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.7f)
                .padding(bottom = 28.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassDark)
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionMenu == CombatActionMenu.ROOT) {
                    DesktopCombatActionButton("[1] STRIKE", NeonCyan, enabled = isPlayerTurn, onClick = { executePlayerAttack(45, "Kinetic Strike") })
                    DesktopCombatActionButton("[2] SKILLS", NeonAmber, enabled = isPlayerTurn, onClick = { actionMenu = CombatActionMenu.SKILLS })
                    DesktopCombatActionButton("[3] DEFEND", ShieldBlue, enabled = isPlayerTurn, onClick = {
                        playerShield = minOf(playerMaxShield, playerShield + 35)
                        isPlayerTurn = false
                        coroutineScope.launch { delay(700); isPlayerTurn = true }
                    })
                    DesktopCombatActionButton("[4] REPAIR", HealthGreen, enabled = isPlayerTurn, onClick = { playerHp = minOf(playerMaxHp, playerHp + 60) })
                    DesktopCombatActionButton("[5] FLEE", HealthRed, enabled = isPlayerTurn, onClick = onFlee)
                } else {
                    DesktopCombatActionButton("[1] Plasma Burst (25 EN)", NeonCyan, enabled = isPlayerTurn && playerEnergy >= 25, onClick = {
                        playerEnergy -= 25
                        executePlayerAttack(85, "Plasma Burst")
                    })
                    DesktopCombatActionButton("[2] Railgun (40 EN)", NeonAmber, enabled = isPlayerTurn && playerEnergy >= 40, onClick = {
                        playerEnergy -= 40
                        executePlayerAttack(130, "Supercharge Railgun")
                    })
                    DesktopCombatActionButton("[ESC] BACK", TextMuted, enabled = true, onClick = { actionMenu = CombatActionMenu.ROOT })
                }
            }
        }
    }
}

@Composable
private fun DesktopFloatingEnemyUnit(
    enemy: DesktopEnemyState,
    isSelected: Boolean,
    services: DesktopAppServices,
    onClick: () -> Unit
) {
    val enemySprite = rememberDesktopAssetPainter("images/enemies/${enemy.id}.webp", services.assetProvider)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Floating Name & Overhead Health Gauge
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = enemy.name,
                color = if (isSelected) HealthRed else TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "⚔ ${enemy.intent}",
                color = NeonAmber,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Overhead HP bar
        LinearProgressIndicator(
            progress = { enemy.hp.toFloat() / enemy.maxHp.toFloat() },
            modifier = Modifier.width(110.dp).height(5.dp).clip(RoundedCornerShape(2.dp)),
            color = HealthRed,
            trackColor = Color(0x55FF3366)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Enemy Sprite standing directly in scene
        Image(
            painter = enemySprite,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) HealthRed else Color(0x33FFFFFF)),
                    RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DesktopFloatingPlayerUnit(
    name: String,
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

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Overhead Vitals
        Text(text = name, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { hp.toFloat() / maxHp.toFloat() },
                modifier = Modifier.width(70.dp).height(5.dp).clip(RoundedCornerShape(2.dp)),
                color = HealthGreen,
                trackColor = Color(0xFF14242A)
            )
            LinearProgressIndicator(
                progress = { shield.toFloat() / maxShield.toFloat() },
                modifier = Modifier.width(45.dp).height(5.dp).clip(RoundedCornerShape(2.dp)),
                color = ShieldBlue,
                trackColor = Color(0xFF101B2E)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Battler Sprite / Portrait standing directly in scene
        Image(
            painter = portrait,
            contentDescription = null,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(if (isActiveTurn) 2.dp else 1.dp, if (isActiveTurn) NeonCyan else Color(0x4400F5D4)),
                    RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DesktopCombatActionButton(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) color.copy(alpha = 0.18f) else Color(0x11FFFFFF))
            .border(BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.7f) else Color(0x22FFFFFF)), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) color else TextMuted.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
