package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import kotlinx.coroutines.delay
import kotlin.random.Random

private val NeonCyan = Color(0xFF00F5D4)
private val NeonAmber = Color(0xFFFFB703)
private val NeonPink = Color(0xFFFF007F)
private val WaterBlue = Color(0xFF00B4D8)
private val DeepSpaceDark = Color(0xFF05070D)
private val GlassDark = Color(0xDD090E18)
private val GlassBorder = Color(0x3300F5D4)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFFA0B3C6)

enum class FishingState {
    IDLE, CASTING, WAITING_FOR_BITE, REELING, CAUGHT
}

data class FishCatchSummary(
    val name: String,
    val rarity: String,
    val weightKg: Float,
    val valueCredits: Int,
    val lore: String
)

@Composable
fun DesktopFishingScreen(
    services: DesktopAppServices,
    zoneId: String = "glow_moss_cavern",
    onClose: () -> Unit
) {
    var fishingState by remember { mutableStateOf(FishingState.IDLE) }
    var tension by remember { mutableStateOf(0.4f) }
    var reelProgress by remember { mutableStateOf(0.1f) }
    var biteTimer by remember { mutableStateOf(0f) }
    var lastCatch by remember { mutableStateOf<FishCatchSummary?>(null) }
    var isHoldingReel by remember { mutableStateOf(false) }

    val bgPainter = rememberDesktopAssetPainter("images/rooms/world_1/pit_shaft_v5.webp", services.assetProvider)

    // Fishing Simulation Loop
    LaunchedEffect(fishingState, isHoldingReel) {
        when (fishingState) {
            FishingState.WAITING_FOR_BITE -> {
                delay(Random.nextLong(1800, 3800))
                fishingState = FishingState.REELING
                tension = 0.5f
                reelProgress = 0.2f
            }
            FishingState.REELING -> {
                while (fishingState == FishingState.REELING) {
                    if (isHoldingReel) {
                        tension = minOf(1.0f, tension + 0.035f)
                        reelProgress = minOf(1.0f, reelProgress + 0.018f)
                    } else {
                        tension = maxOf(0.0f, tension - 0.025f)
                        reelProgress = maxOf(0.0f, reelProgress - 0.008f)
                    }

                    // Fish struggle fluctuations
                    tension += (Random.nextFloat() - 0.5f) * 0.02f
                    tension = tension.coerceIn(0f, 1f)

                    if (tension >= 0.95f) {
                        // Line snapped
                        fishingState = FishingState.IDLE
                        lastCatch = null
                        break
                    }

                    if (reelProgress >= 1.0f) {
                        // Successfully caught from zone data
                        val zone = services.fishingService.getFishingZone(zoneId)
                            ?: services.fishingService.getFishingZone("glow_moss_cavern")
                        val defaultRod = services.fishingService.getAvailableRods().firstOrNull()
                            ?: com.example.starborn.domain.fishing.FishingRod(id = "basic_rod", name = "Basic Rod", fishingPower = 1.0)
                        val defaultLure = services.fishingService.getAvailableLures().firstOrNull()
                            ?: com.example.starborn.domain.fishing.FishingLure(id = "fiber_lure", name = "Fiber Lure")

                        val result = if (zone != null) {
                            services.fishingService.getCatchResult(
                                zone = zone,
                                rod = defaultRod,
                                lure = defaultLure,
                                minigameResult = com.example.starborn.domain.fishing.MinigameResult.PERFECT
                            )
                        } else null

                        val itemId = result?.itemId ?: "bioluminescent_ray"
                        val displayName = services.itemRepository.findItem(itemId)?.name
                            ?: result?.itemId?.replace("_", " ")?.capitalize()
                            ?: "Bioluminescent Abyssal Ray"

                        services.inventoryService.addItem(itemId, 1)
                        services.sessionStore.setInventory(services.inventoryService.snapshot())

                        lastCatch = FishCatchSummary(
                            name = displayName,
                            rarity = result?.rarity?.name ?: "RARE",
                            weightKg = 3.5f + Random.nextFloat() * 2.5f,
                            valueCredits = (services.itemRepository.findItem(itemId)?.value ?: 75),
                            lore = result?.flavorText ?: "Recovered from resonant waters."
                        )
                        fishingState = FishingState.CAUGHT
                        break
                    }
                    delay(30)
                }
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.Spacebar -> {
                            if (fishingState == FishingState.IDLE) {
                                fishingState = FishingState.WAITING_FOR_BITE
                            } else if (fishingState == FishingState.REELING) {
                                isHoldingReel = true
                            } else if (fishingState == FishingState.CAUGHT) {
                                fishingState = FishingState.IDLE
                            }
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    if (keyEvent.key == Key.Spacebar) {
                        isHoldingReel = false
                        true
                    } else false
                } else false
            }
    ) {
        // 1. Panoramic Biome Dock Background Art
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Water Surface Glow & Weather Overlays
        DesktopWeatherOverlay("cave_drip")
        DesktopVignetteOverlay(intensity = 0.65f)
        DesktopCrtScanlineOverlay(scanlineAlpha = 0.05f)

        // 3. Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BIOME ANGLING DOCK // GLOW-MOSS CAVERN",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Atmospheric Pressure: 1.04 ATM • Salinity: Optimal",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            DesktopMinimalPillButton("[ESC] RETURN TO SHORE", onClick = onClose)
        }

        // 4. Center Stage: Fishing Line & Reeling Gauge
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (fishingState) {
                FishingState.IDLE -> {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        color = GlassDark,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "READY TO CAST",
                                color = NeonAmber,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Press [SPACE] or Click to cast your magnetic lure into the luminous pool",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { fishingState = FishingState.WAITING_FOR_BITE },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "[SPACE] CAST LURE", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                FishingState.WAITING_FOR_BITE -> {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        color = GlassDark,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(32.dp))
                            Text(
                                text = "Lure submerged... Awaiting bio-resonance bite",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                FishingState.REELING -> {
                    Surface(
                        modifier = Modifier
                            .width(420.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassDark)
                            .border(BorderStroke(1.5.dp, NeonPink), RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "FISH ON THE LINE!",
                                color = NeonPink,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )

                            // Reel Progress Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "CATCH REEL PROGRESS", color = TextMuted, fontSize = 11.sp)
                                    Text(text = "${(reelProgress * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { reelProgress },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = NeonCyan,
                                    trackColor = Color(0xFF141F32)
                                )
                            }

                            // Line Tension Gauge
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "LINE TENSION (Keep in Green)", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        text = if (tension > 0.8f) "WARNING: SNAP IMMINENT" else "STABLE",
                                        color = if (tension > 0.8f) Color.Red else Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { tension },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = if (tension > 0.8f) Color.Red else if (tension > 0.55f) NeonAmber else Color(0xFF00E676),
                                    trackColor = Color(0xFF141F32)
                                )
                            }

                            Text(
                                text = "HOLD [SPACE] to reel in • RELEASE to slack tension",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                FishingState.CAUGHT -> {
                    lastCatch?.let { catchItem ->
                        Surface(
                            modifier = Modifier
                                .width(460.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassDark)
                                .border(BorderStroke(1.5.dp, NeonAmber), RoundedCornerShape(16.dp))
                                .padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "★ NEW CATCH RECORDED ★",
                                    color = NeonAmber,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = catchItem.name,
                                    color = TextWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rarity: ${catchItem.rarity}  •  Weight: ${catchItem.weightKg} kg  •  Value: ${catchItem.valueCredits} CR",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = catchItem.lore,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                                Button(
                                    onClick = { fishingState = FishingState.IDLE },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "STOW IN CARGO [SPACE]", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
