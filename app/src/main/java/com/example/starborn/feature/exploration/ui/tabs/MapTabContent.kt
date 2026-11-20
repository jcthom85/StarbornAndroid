package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    objective: String?,
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
        objective?.takeIf { it.isNotBlank() }?.let { objText ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = FieldMenuDesign.panel,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = FieldMenuDesign.gold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "NEXT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            ),
                            color = FieldMenuDesign.gold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = objText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (LocalModernFieldMenu.current) FieldMenuDesign.text else Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        val fullMapAvailable = fullMap?.cells?.isNotEmpty() == true
        if (fullMapAvailable) {
            InteractiveMapPanel(
                fullMap = fullMap,
                onOpenMapLegend = {
                    onMenuAction()
                    onOpenMapLegend()
                }
            )
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
}
