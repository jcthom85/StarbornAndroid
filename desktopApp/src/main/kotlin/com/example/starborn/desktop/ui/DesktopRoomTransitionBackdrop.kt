package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

/**
 * Center artwork travels with smooth, symmetrical parallax along the direction of travel;
 * ambient atmospheric extensions crossfade smoothly without displacement.
 */
@Composable
internal fun DesktopRoomTransitionBackdrop(
    services: DesktopAppServices,
    ui: ExplorationUiState,
    progress: Float,
    background: String?,
    content: @Composable BoxWithConstraintsScope.(PortraitBackdropLayout) -> Unit
) {
    val painter = rememberDesktopAssetPainter(background, services.assetProvider)
    val outgoing = rememberDesktopAssetPainter(ui.roomTransition?.fromBackgroundImage, services.assetProvider)
    val reducedMotion = LocalExplorationSettings.current.disableScreenshake
    val direction = ui.roomTransition?.direction

    // Physical travel direction:
    // Moving east means camera pans east: incoming enters from right (+X), outgoing exits left (-X).
    // Moving south means camera pans south: incoming enters from bottom (+Y), outgoing exits top (-Y).
    val moveSignX = when (direction?.lowercase()) { "east" -> 1f; "west" -> -1f; else -> 0f }
    val moveSignY = when (direction?.lowercase()) { "south" -> 1f; "north" -> -1f; else -> 0f }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val intrinsic = painter.intrinsicSize
        val stableImageSize = if (intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.width > 0 && intrinsic.height > 0) intrinsic
            else outgoing.intrinsicSize.takeIf { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 } ?: Size(9f, 16f)
        val layout = portraitBackdropLayout(with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }, stableImageSize)
        val moving = ui.roomTransition?.fromBackgroundImage != null && progress < 1f
        val clampedProgress = progress.coerceIn(0f, 1f)
        // Smooth hermite curve for tactile, fluid deceleration
        val eased = clampedProgress * clampedProgress * (3f - 2f * clampedProgress)

        val travelDistanceX = layout.center.width * 0.28f
        val travelDistanceY = layout.center.height * 0.28f

        // Extension background atmosphere crossfades cleanly
        DesktopPortraitBackdrop(painter, null,
            modifier = Modifier.graphicsLayer { alpha = if (moving) eased else 1f },
            showCenter = false)
        if (moving) {
            DesktopPortraitBackdrop(outgoing, null,
                modifier = Modifier.graphicsLayer { alpha = 1f - eased },
                showCenter = false)
        }

        // Incoming center room artwork
        DesktopPortraitBackdrop(painter, ui.currentRoom?.title, showExtensions = false,
            centerModifier = Modifier.graphicsLayer {
                translationX = if (reducedMotion) 0f else moveSignX * travelDistanceX * (1f - eased)
                translationY = if (reducedMotion) 0f else moveSignY * travelDistanceY * (1f - eased)
                alpha = if (moving) (eased * 1.15f).coerceIn(0f, 1f) else 1f
                scaleX = if (reducedMotion) 1f else 0.98f + 0.02f * eased
                scaleY = scaleX
            })

        // Outgoing center room artwork
        if (moving) {
            DesktopPortraitBackdrop(outgoing, null, showExtensions = false,
                centerModifier = Modifier.graphicsLayer {
                    translationX = if (reducedMotion) 0f else -moveSignX * travelDistanceX * eased
                    translationY = if (reducedMotion) 0f else -moveSignY * travelDistanceY * eased
                    alpha = (1f - eased).coerceIn(0f, 1f)
                    scaleX = if (reducedMotion) 1f else 1f - 0.02f * eased
                    scaleY = scaleX
                })
        }

        content(layout)
    }
}
