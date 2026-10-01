package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.ui.InteractiveMapPanel
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.ui.ThemedMenuButton
import com.example.starborn.feature.exploration.ui.hud.MinimapWidget
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu
import com.example.starborn.feature.exploration.viewmodel.FullMapUiState
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState

@Composable
fun MapTabContent(
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    isCurrentRoomDark: Boolean,
    accentColor: Color,
    borderColor: Color,
    onMenuAction: () -> Unit,
    onOpenMapLegend: () -> Unit
) {
    MenuSectionCard(
        title = "Map",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        val fullMapAvailable = fullMap?.cells?.isNotEmpty() == true
        if (fullMapAvailable) {
            if (LocalModernFieldMenu.current) {
                Text(
                    "${fullMap!!.cells.count { it.visited }} rooms visited · ${fullMap.cells.count { it.discovered && !it.visited }} discovered",
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            InteractiveMapPanel(fullMap = fullMap)
        } else {
            Text(
                text = "Survey more rooms in this area to reveal the map.",
                color = if (LocalModernFieldMenu.current) FieldMenuDesign.textMuted else Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            minimap?.let {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MinimapWidget(
                        minimap = it,
                        onLegend = {
                            onMenuAction()
                            onOpenMapLegend()
                        },
                        obscured = isCurrentRoomDark,
                        modifier = Modifier.size(140.dp)
                    )
                }
            }
        }
        ThemedMenuButton(
            label = "Map Legend",
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp),
            onClick = {
                onMenuAction()
                onOpenMapLegend()
            }
        )
    }
}
