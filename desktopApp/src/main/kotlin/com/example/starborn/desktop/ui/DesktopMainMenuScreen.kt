package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import kotlinx.coroutines.launch
import kotlin.random.Random

private val NeonCyan = Color(0xFF00F5D4)
private val NeonPink = Color(0xFFFF007F)
private val NeonAmber = Color(0xFFFFB703)
private val DeepSpaceDark = Color(0xFF05070D)
private val CardBackground = Color(0xFF0C101C)
private val CardBorder = Color(0xFF1E283E)
private val TextWhite = Color(0xFFE8EEF5)
private val TextMuted = Color(0xFF8899AC)

@Composable
fun DesktopMainMenuScreen(
    services: DesktopAppServices,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedMenuIndex by remember { mutableStateOf(0) }
    val menuItems = listOf(
        MenuItem("NEW GAME", "Begin a new odyssey across the star systems", "[N] / [1]"),
        MenuItem("CONTINUE", "Resume previous journey from last checkpoint", "[C] / [2]"),
        MenuItem("SETTINGS", "Audio, graphics, keybindings & accessibility", "[S] / [3]"),
        MenuItem("QUIT", "Exit Starborn to desktop", "[ESC] / [4]")
    )

    // Trigger Title Music on desktop launch
    LaunchedEffect(Unit) {
        val commands = services.audioRouter.commandsForRoom(hubId = "main_menu", roomId = "main_menu")
        services.audioDriver.executeAll(commands)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionDown, Key.S -> {
                            selectedMenuIndex = (selectedMenuIndex + 1) % menuItems.size
                            true
                        }
                        Key.DirectionUp, Key.W -> {
                            selectedMenuIndex = if (selectedMenuIndex - 1 < 0) menuItems.size - 1 else selectedMenuIndex - 1
                            true
                        }
                        Key.Enter, Key.Spacebar -> {
                            when (selectedMenuIndex) {
                                0 -> onStartGame()
                                1 -> onStartGame()
                                2 -> onOpenSettings()
                                3 -> onQuit()
                            }
                            true
                        }
                        Key.N, Key.One -> {
                            onStartGame()
                            true
                        }
                        Key.C, Key.Two -> {
                            onStartGame()
                            true
                        }
                        Key.Three -> {
                            onOpenSettings()
                            true
                        }
                        Key.Escape, Key.Four -> {
                            onQuit()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Dynamic Starfield background
        DesktopStarfieldBackground()

        // Main Landscape 16:9 Split Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hero Banner
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STARBORN",
                    color = NeonCyan,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    style = MaterialTheme.typography.displayLarge
                )

                Text(
                    text = "CHRONICLES OF THE PERIMETER",
                    color = NeonAmber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground.copy(alpha = 0.8f))
                        .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(8.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "SECTOR 7 • ANOMALY ACTIVE",
                            color = NeonPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deep-space tactical RPG featuring real-time turn manipulation, crew progression, orbital encounters, and procedural planetary exploration.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Platform Version & Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan)
                    )
                    Text(
                        text = "Windows Widescreen Edition • Keyboard & Mouse Ready",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Right Action Menu
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    menuItems.forEachIndexed { index, item ->
                        DesktopMenuCard(
                            item = item,
                            isSelected = selectedMenuIndex == index,
                            onClick = {
                                selectedMenuIndex = index
                                when (index) {
                                    0 -> onStartGame()
                                    1 -> onStartGame()
                                    2 -> onOpenSettings()
                                    3 -> onQuit()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Controls HUD
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[W/S or ↑/↓] Navigate  •  [ENTER/SPACE] Select  •  [F11] Fullscreen  •  [ESC] Back",
                color = TextMuted.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

data class MenuItem(
    val title: String,
    val description: String,
    val hotkey: String
)

@Composable
private fun DesktopMenuCard(
    item: MenuItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val borderColor = if (isSelected) NeonCyan.copy(alpha = glowAlpha) else CardBorder
    val backgroundColor = if (isSelected) Color(0xFF131E33) else CardBackground.copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isSelected) 12.dp else 2.dp, RoundedCornerShape(10.dp), spotColor = NeonCyan)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (isSelected) NeonCyan else TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = if (isSelected) TextWhite.copy(alpha = 0.9f) else TextMuted,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF090E17))
                    .border(BorderStroke(1.dp, if (isSelected) NeonAmber else CardBorder), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.hotkey,
                    color = if (isSelected) NeonAmber else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DesktopStarfieldBackground() {
    val stars = remember {
        List(140) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2.5f + 0.8f,
                speed = Random.nextFloat() * 0.0003f + 0.0001f,
                alpha = Random.nextFloat() * 0.7f + 0.3f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Subtle cosmic gradient nebula glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF141F36), Color(0xFF05070D)),
                center = Offset(width * 0.3f, height * 0.4f),
                radius = width * 0.7f
            )
        )

        stars.forEach { star ->
            val curY = (star.y + progress * star.speed * 1000f) % 1f
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = star.size,
                center = Offset(star.x * width, curY * height)
            )
        }
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
