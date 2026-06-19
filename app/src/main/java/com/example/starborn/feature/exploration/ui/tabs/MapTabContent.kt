package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.ui.FullMapCanvas
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.ui.ThemedMenuButton
import com.example.starborn.feature.exploration.ui.hud.MinimapWidget
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
    onOpenMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit
) {
    MenuSectionCard(
        title = "Navigation",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        MapPreviewPanel(
            minimap = minimap,
            fullMap = fullMap,
            isCurrentRoomDark = isCurrentRoomDark,
            accentColor = accentColor,
            onMenuAction = onMenuAction,
            onMapLegend = onOpenMapLegend,
            onOpenFullMap = onOpenFullMap
        )
    }
}

@Composable
private fun MapPreviewPanel(
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    isCurrentRoomDark: Boolean,
    accentColor: Color,
    onMenuAction: () -> Unit,
    onMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit
) {
    val fullMapAvailable = fullMap?.cells?.isNotEmpty() == true
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (fullMapAvailable && fullMap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                FullMapCanvas(
                    fullMap = fullMap,
                    scale = 1f,
                    offset = Offset.Zero,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Text(
                text = "Survey more rooms in this node to unlock the full map.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            minimap?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MinimapWidget(
                        minimap = it,
                        onLegend = {
                            onMenuAction()
                            onOpenFullMap()
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
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onMenuAction()
                onMapLegend()
            }
        )
    }
}
