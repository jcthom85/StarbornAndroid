package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.arcade.games.orbitaldefense.*
import kotlinx.coroutines.delay

private val NeonCyan = Color(0xFF00F5D4)
private val NeonAmber = Color(0xFFFFB703)
private val NeonPink = Color(0xFFFF007F)
private val NeonGreen = Color(0xFF00E676)
private val ArcadeDark = Color(0xFF07090F)
private val BezelDark = Color(0xFF121620)
private val GlassBorder = Color(0x4400F5D4)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFFA0B3C6)

data class ArcadeCabinetMetadata(
    val id: String,
    val title: String,
    val genre: String,
    val description: String,
    val costCredits: Int,
    val highscore: Int
)

@Composable
fun DesktopArcadeScreen(
    services: DesktopAppServices,
    onClose: () -> Unit
) {
    val cabinets = remember {
        listOf(
            ArcadeCabinetMetadata("orbital_defense", "ORBITAL DEFENSE 2084", "Space Shmup", "Defend orbital stasis rings from swarming alien fleets and flagships.", 10, 48200),
            ArcadeCabinetMetadata("deep_mine", "DEEP MINE DESCENT", "Precision Excavator", "Navigate mining drills through shifting tectonic strata and diamond veins.", 10, 32150),
            ArcadeCabinetMetadata("slag_catcher", "SLAG CATCHER", "High-Speed Reflex", "Catch falling molten slag cores before foundry containment fails.", 10, 24600),
            ArcadeCabinetMetadata("spire_infiltrator", "SPIRE INFILTRATOR", "Stealth Cyber-Hacker", "Bypass security laser grids and corporate surveillance nodes.", 15, 18900),
            ArcadeCabinetMetadata("canopy_hopper", "CANOPY HOPPER", "Vertical Platformer", "Ascend massive biophilic canopy spires escaping toxic floor mist.", 10, 29400),
            ArcadeCabinetMetadata("harmonic_pulse", "HARMONIC PULSE", "Rhythm Synthesizer", "Align frequency resonators with the synth soundtrack beat waves.", 15, 51300)
        )
    }

    var selectedCabinetId by remember { mutableStateOf<String?>("orbital_defense") }
    var isPlayingGame by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcadeDark)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    if (isPlayingGame) {
                        isPlayingGame = false
                    } else {
                        onClose()
                    }
                    true
                } else false
            }
    ) {
        if (!isPlayingGame) {
            // Cabinet Selector Viewport
            DesktopArcadeCabinetSelector(
                cabinets = cabinets,
                selectedId = selectedCabinetId,
                onSelect = { selectedCabinetId = it },
                onLaunchGame = { isPlayingGame = true },
                onClose = onClose
            )
        } else {
            // Active Orbital Defense Game Screen
            DesktopOrbitalDefenseRunner(
                services = services,
                onExitGame = { isPlayingGame = false }
            )
        }
    }
}

@Composable
private fun DesktopArcadeCabinetSelector(
    cabinets: List<ArcadeCabinetMetadata>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onLaunchGame: () -> Unit,
    onClose: () -> Unit
) {
    val selectedCabinet = cabinets.firstOrNull { it.id == selectedId } ?: cabinets.first()

    Box(modifier = Modifier.fillMaxSize()) {
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.06f)
        DesktopVignetteOverlay(intensity = 0.7f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            // Top HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ASTRA ARCADE PARLOR // RETRO ENTERTAINMENT TERMINAL",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Insert digital token credits to challenge high scores and earn Astra tickets",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                DesktopMinimalPillButton("[ESC] LEAVE ARCADE", onClick = onClose)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Grid and Cabinet Detail View
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left: Cabinets Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(cabinets) { cab ->
                        val isSelected = cab.id == selectedCabinet.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x3300F5D4) else Color(0xFF0F1420))
                                .border(
                                    BorderStroke(1.5.dp, if (isSelected) NeonCyan else Color(0xFF222B3D)),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelect(cab.id) }
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cab.title,
                                        color = if (isSelected) NeonCyan else TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${cab.costCredits} CR",
                                        color = NeonAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Genre: ${cab.genre}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Record: ${cab.highscore} PTS",
                                    color = NeonPink,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Right: Cabinet Marquee & Launcher
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BezelDark)
                        .border(BorderStroke(1.5.dp, GlassBorder), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "CABINET MARQUEE",
                                color = NeonAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = selectedCabinet.title,
                                color = TextWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = selectedCabinet.description,
                                color = TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF090D14))
                                    .border(BorderStroke(1.dp, Color(0xFF1E2838)), RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "CONTROLS:", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "• [A] / [D] or Arrow Keys: Maneuver Ship", color = TextWhite, fontSize = 12.sp)
                                    Text(text = "• [SPACEBAR]: Pulse Laser Cannon", color = TextWhite, fontSize = 12.sp)
                                    Text(text = "• [E]: Tactical EMP Shockwave", color = TextWhite, fontSize = 12.sp)
                                }
                            }
                        }

                        Button(
                            onClick = onLaunchGame,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = "INSERT COIN & PLAY [SPACE]",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopOrbitalDefenseRunner(
    services: DesktopAppServices,
    onExitGame: () -> Unit
) {
    val engine = remember { OrbitalDefenseEngine() }
    var currentSnapshot by remember { mutableStateOf(engine.snapshot()) }

    var isMovingLeft by remember { mutableStateOf(false) }
    var isMovingRight by remember { mutableStateOf(false) }
    var isFiring by remember { mutableStateOf(false) }
    var isTriggeringEmp by remember { mutableStateOf(false) }

    // 60 FPS Game Loop
    LaunchedEffect(Unit) {
        while (true) {
            val input = OrbitalDefenseInput(
                moveLeft = isMovingLeft,
                moveRight = isMovingRight,
                fire = isFiring,
                triggerEmp = isTriggeringEmp
            )
            isTriggeringEmp = false // one-shot pulse
            engine.advance(0.016f, input)
            currentSnapshot = engine.snapshot()
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030509))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onExitGame()
                            true
                        }
                        Key.A, Key.DirectionLeft -> {
                            isMovingLeft = true
                            true
                        }
                        Key.D, Key.DirectionRight -> {
                            isMovingRight = true
                            true
                        }
                        Key.Spacebar -> {
                            isFiring = true
                            true
                        }
                        Key.E -> {
                            isTriggeringEmp = true
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.A, Key.DirectionLeft -> {
                            isMovingLeft = false
                            true
                        }
                        Key.D, Key.DirectionRight -> {
                            isMovingRight = false
                            true
                        }
                        Key.Spacebar -> {
                            isFiring = false
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // CRT Aesthetics & Border
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.08f)
        DesktopVignetteOverlay(intensity = 0.8f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arcade Marquee Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SCORE: ${currentSnapshot.score}",
                        color = NeonAmber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SHIPS: ${currentSnapshot.lives}",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "EMP BOMBS: ${currentSnapshot.empBombs}",
                        color = NeonPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                DesktopMinimalPillButton("[ESC] QUIT TO LOBBY", onClick = onExitGame)
            }

            // CRT Game Screen Display Frame
            Box(
                modifier = Modifier
                    .width(700.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF04060E))
                    .border(BorderStroke(2.dp, NeonCyan.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Starfield
                    for (i in 0 until 40) {
                        val starX = ((i * 127f) % w)
                        val starY = ((i * 89f) + (currentSnapshot.elapsedSeconds * 40f)) % h
                        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 1.2f, center = Offset(starX, starY))
                    }

                    // 2. Draw Enemies
                    currentSnapshot.enemies.filter { it.alive }.forEach { enemy ->
                        val enemyCenter = Offset(enemy.x * w, enemy.y * h)
                        val enemyColor = when (enemy.type) {
                            com.example.starborn.feature.arcade.games.orbitaldefense.EnemyType.FLAGSHIP -> NeonPink
                            com.example.starborn.feature.arcade.games.orbitaldefense.EnemyType.CRUISER -> NeonAmber
                            com.example.starborn.feature.arcade.games.orbitaldefense.EnemyType.SWARM -> NeonCyan
                            com.example.starborn.feature.arcade.games.orbitaldefense.EnemyType.MYSTERY -> Color.Yellow
                        }
                        drawCircle(
                            color = enemyColor,
                            radius = 12f,
                            center = enemyCenter
                        )
                    }

                    // 3. Draw Projectiles
                    currentSnapshot.projectiles.filter { it.active }.forEach { proj ->
                        val projCenter = Offset(proj.x * w, proj.y * h)
                        drawCircle(
                            color = if (proj.isPlayer) NeonGreen else Color.Red,
                            radius = 4.5f,
                            center = projCenter
                        )
                    }

                    // 4. Draw Player Ship
                    val px = currentSnapshot.playerX * w
                    val py = 0.90f * h
                    drawCircle(
                        color = NeonCyan,
                        radius = 14f,
                        center = Offset(px, py)
                    )
                }

                if (currentSnapshot.gameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC05070E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "GAME OVER",
                                color = Color.Red,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp
                            )
                            Text(
                                text = "FINAL SCORE: ${currentSnapshot.score} PTS",
                                color = NeonAmber,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Button(
                                onClick = {
                                    engine.reset()
                                    currentSnapshot = engine.snapshot()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "RETRY [SPACE]", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
