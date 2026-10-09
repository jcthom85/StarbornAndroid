package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

/** Center artwork travels inside a fixed frame; reflected atmosphere only crossfades. */
@Composable
internal fun DesktopRoomTransitionBackdrop(services: DesktopAppServices, ui: ExplorationUiState, progress: Float,
    background: String?, content: @Composable BoxWithConstraintsScope.(PortraitBackdropLayout) -> Unit) {
    val painter = rememberDesktopAssetPainter(background, services.assetProvider)
    val outgoing = rememberDesktopAssetPainter(ui.roomTransition?.fromBackgroundImage, services.assetProvider)
    val reducedMotion = LocalExplorationSettings.current.disableScreenshake
    val direction = ui.roomTransition?.direction
    val enterX = when (direction) { "east" -> -1f; "west" -> 1f; else -> 0f }
    val enterY = when (direction) { "north" -> 1f; "south" -> -1f; else -> 0f }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val intrinsic = painter.intrinsicSize
        val stableImageSize = if (intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.width > 0 && intrinsic.height > 0) intrinsic
            else outgoing.intrinsicSize.takeIf { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 } ?: Size(9f, 16f)
        val layout = portraitBackdropLayout(with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }, stableImageSize)
        val moving = ui.roomTransition?.fromBackgroundImage != null && progress < 1f
        val eased = progress.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
        // Neither reflection layer receives travel, scale, or directional blur.
        DesktopPortraitBackdrop(painter, null, showCenter = false)
        if (moving) DesktopPortraitBackdrop(outgoing, null,
            Modifier.graphicsLayer { alpha = 1f - eased }, showCenter = false)
        DesktopPortraitBackdrop(painter, ui.currentRoom?.title, showExtensions = false,
            centerModifier = Modifier.graphicsLayer {
                translationX = if (reducedMotion) 0f else enterX * size.width * .12f * (1f - progress)
                translationY = if (reducedMotion) 0f else enterY * size.height * .12f * (1f - progress)
                scaleX = if (reducedMotion) 1f else .99f + .01f * progress; scaleY = scaleX
            })
        if (moving) {
            DesktopPortraitBackdrop(outgoing, null, showExtensions = false,
                centerModifier = Modifier.blur((if (reducedMotion) 0f else 6f * (progress / .55f).coerceIn(0f, 1f)).dp).graphicsLayer {
                    translationX = if (reducedMotion) 0f else -enterX * size.width * .78f * progress
                    translationY = if (reducedMotion) 0f else -enterY * size.height * .78f * progress
                    alpha = if (reducedMotion) 1f - eased else (1f - progress / .82f).coerceIn(0f, 1f)
                    scaleX = if (reducedMotion) 1f else 1f - .025f * progress; scaleY = scaleX
                })
        }
        content(layout)
    }
}
