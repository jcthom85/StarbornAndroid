package com.example.starborn.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

private val VoidBlack = Color(0xFF030712)
private val CyberCyan = Color(0xFF63E6FF)
private val CyberMutedCyan = Color(0xFF38BDF8)
private val CyberSlate = Color(0xFF94A3B8)

@Composable
internal fun DesktopHubTransitionOverlay(
    travel: DesktopHubTravel,
    modifier: Modifier = Modifier
) {
    if (!travel.busy) return

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(120f)
            .background(VoidBlack.copy(alpha = travel.opacity.value.coerceIn(0f, 1f)))
            .clickable(interactionSource = interactionSource, indication = null) {}
    ) {
        when (val type = travel.transitionType) {
            is DesktopHubTransitionType.DeployToNode -> {
                val cardAlpha = travel.cardAlpha.value.coerceIn(0f, 1f)
                val cardScale = travel.cardScale.value
                val beamProgress = travel.beamProgress.value.coerceIn(0f, 1f)

                if (cardAlpha > 0f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp)
                            .graphicsLayer {
                                alpha = cardAlpha
                                scaleX = cardScale
                                scaleY = cardScale
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category / Region Tag
                        val headerTag = type.regionTitle?.let { "// $it //" } ?: "// SECTOR DEPLOYMENT //"
                        Text(
                            text = headerTag.uppercase(),
                            color = CyberCyan.copy(alpha = 0.9f),
                            fontFamily = LocalStarbornFonts.current.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.5.sp,
                            textAlign = TextAlign.Center
                        )

                        // Primary Node Name
                        Text(
                            text = type.nodeTitle.uppercase(),
                            color = Color.White,
                            fontFamily = LocalStarbornFonts.current.orbitron,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            letterSpacing = 3.sp,
                            textAlign = TextAlign.Center
                        )

                        // Expanding Cyber Accent Beam
                        val beamWidth = (beamProgress * 340f).dp
                        Box(
                            modifier = Modifier
                                .width(beamWidth)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            CyberCyan.copy(alpha = 0.85f),
                                            CyberCyan,
                                            CyberCyan.copy(alpha = 0.85f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Destination / Entry Point Subtitle
                        type.entryRoomTitle?.let { roomTitle ->
                            Text(
                                text = "INITIAL INFILTRATION: ${roomTitle.uppercase()}",
                                color = CyberSlate,
                                fontFamily = LocalStarbornFonts.current.oxanium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                letterSpacing = 1.2.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Teaser Description (if available and brief)
                        type.description?.takeIf { it.isNotBlank() && it.length < 120 }?.let { desc ->
                            Text(
                                text = desc,
                                color = CyberSlate.copy(alpha = 0.7f),
                                fontFamily = LocalStarbornFonts.current.body,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp).widthIn(max = 480.dp)
                            )
                        }
                    }
                }
            }

            is DesktopHubTransitionType.ReturnToHub -> {
                val cardAlpha = travel.cardAlpha.value.coerceIn(0f, 1f)
                if (cardAlpha > 0f) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .graphicsLayer { alpha = cardAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "// DISENGAGING // RETURNING TO NAV CONSOLE",
                            color = CyberMutedCyan,
                            fontFamily = LocalStarbornFonts.current.orbitron,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            else -> Unit
        }
    }
}
