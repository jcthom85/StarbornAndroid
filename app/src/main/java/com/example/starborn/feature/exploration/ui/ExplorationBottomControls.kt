package com.example.starborn.feature.exploration.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Transparent space shared by the Menu, south exit, scene entities and notices. */
internal object ExplorationControlSpacing {
    val menuBottomPadding = 16.dp
    val southExitBottomClearance = 86.dp
    val sceneBottomClearance = 132.dp
    val noticeBottomClearance = 160.dp
}

@Composable
internal fun ExplorationBottomControls(
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isTutorialBeacon: Boolean = false
) {
    Box(modifier.fillMaxWidth().navigationBarsPadding()
        .padding(bottom = ExplorationControlSpacing.menuBottomPadding)
        .testTag("exploration-bottom-controls"), contentAlignment = Alignment.BottomCenter) {
        MenuToggleButton(isOpen = false, onToggle = onOpenMenu,
            enabled = enabled, isTutorialBeacon = isTutorialBeacon,
            modifier = Modifier.testTag("exploration-menu-button"))
    }
}
