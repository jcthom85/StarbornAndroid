package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.enemy.*
import com.example.starborn.data.local.UserSettings
import kotlin.math.sin

/** Render the same authored party/sprite/composite data Android uses in the room. */
@Composable
internal fun DesktopExplorationEnemyStage(services: DesktopAppServices, ui: ExplorationUiState, layout: PortraitBackdropLayout, blocked: Boolean) {
    if (ui.visualEnemyParties.isEmpty()) return
    val density = LocalDensity.current
    val settings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    val width = with(density) { layout.center.width.toDp() }
    val height = with(density) { layout.center.height.toDp() }
    val stageHeight = minOf(340.dp, height * .45f)
    BoxWithConstraints(Modifier.offset(x = with(density) { layout.center.left.toDp() },
        y = with(density) { layout.center.bottom.toDp() } - stageHeight - 64.dp)
        .width(width).height(stageHeight).testTag("exploration-enemies")) {
        val base = if (width < 380.dp) 160.dp else if (width < 520.dp) 185.dp else 210.dp
        ui.visualEnemyParties.forEachIndexed { lane, party -> key(ui.currentRoom?.id, party.id) {
            val count = party.enemies.size.coerceAtLeast(1)
            val overlap = when { count >= 4 -> .42f; count == 3 -> .36f; count == 2 -> .28f; else -> 0f }
            val memberSize = minOf(base, (width - 24.dp) / (1f + (count - 1) * (1f - overlap)))
            val clusterWidth = memberSize * (1f + (count - 1) * (1f - overlap))
            val arrival = remember { Animatable(0f) }
            LaunchedEffect(party.enteringFrom, party.leavingTo) { arrival.snapTo(0f); arrival.animateTo(1f, tween(450)) }
            val transition = rememberInfiniteTransition(label = "room enemies")
            val wave by transition.animateFloat(0f, 6.28318f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "breathing")
            val direction = party.leavingTo ?: party.enteringFrom
            val distance = if (party.leavingTo != null) arrival.value else 1f - arrival.value
            Box(Modifier.align(Alignment.BottomCenter).offset(y = -(memberSize * .55f) * (lane % 2))
                .width(clusterWidth).height(memberSize + 28.dp).graphicsLayer {
                    alpha = if (party.leavingTo != null) 1f - arrival.value else arrival.value
                    translationX = when (direction?.lowercase()) { "west" -> -distance * 90.dp.toPx(); "east" -> distance * 90.dp.toPx(); else -> 0f }
                    translationY = when (direction?.lowercase()) { "north" -> -distance * 60.dp.toPx(); "south" -> distance * 60.dp.toPx(); else -> 0f }
                }) {
                party.enemies.forEachIndexed { index, enemyId ->
                    val icon = ui.enemyIcons[enemyId]
                    val tierScale = explorationEnemySpriteScale(enemyPresentationTier(ui.enemyTiers[enemyId]))
                    Box(Modifier.offset(x = memberSize * index * (1f - overlap)).size(memberSize)
                        .zIndex((count - index).toFloat()).graphicsLayer {
                            scaleX = tierScale; scaleY = tierScale
                            if (!settings.disableScreenshake) translationY = sin(wave + index) * 2.dp.toPx()
                        }.explorationFeedback(!blocked && party.leavingTo == null, Color(0xFFFF8A80)).clickable(enabled = !blocked && party.leavingTo == null) { services.exploration.engageEnemy(enemyId) }
                        .semantics { contentDescription = "Engage ${icon?.displayName ?: services.contentName(enemyId)}" }) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawOval(Color.Black.copy(alpha = .45f), topLeft = androidx.compose.ui.geometry.Offset(size.width * .15f, size.height * .89f),
                                size = androidx.compose.ui.geometry.Size(size.width * .7f, size.height * .09f))
                        }
                        val composite = icon?.composite
                        if (composite != null && composite.parts.isNotEmpty()) {
                            val extent = composite.parts.maxOf { maxOf(kotlin.math.abs(it.offsetX) + it.widthScale / 2, kotlin.math.abs(it.offsetY) + it.heightScale / 2) }.coerceAtLeast(.5f) * 2
                            val unit = memberSize / extent
                            composite.parts.forEach { part ->
                                Image(rememberDesktopAssetPainter(part.spritePath, services.assetProvider), null,
                                    Modifier.align(Alignment.Center).offset(unit * (part.offsetX + composite.groupOffsetX), unit * (part.offsetY + composite.groupOffsetY))
                                        .size(unit * part.widthScale, unit * part.heightScale).zIndex(part.z), contentScale = ContentScale.Fit)
                            }
                        } else Image(rememberDesktopAssetPainter(icon?.spritePath ?: services.enemyDefinitions[enemyId]?.portrait, services.assetProvider), null,
                            Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        if (index == 0) Surface(Modifier.align(Alignment.BottomCenter), shape = RoundedCornerShape(8.dp),
                            color = Color(0xE807111A), border = BorderStroke(1.dp, if (party.isAggressive) Color(0xFFFF7766) else Color(0xFFFFBB55))) {
                            Text("FIGHT", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } }
    }
}
