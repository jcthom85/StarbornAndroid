package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.ui.map.AreaMapCanvas
import com.example.starborn.ui.map.MapPalette
import androidx.compose.ui.unit.dp

@Composable
fun MinimapWidget(minimap: MinimapUiState?, onLegend: () -> Unit, modifier: Modifier = Modifier, obscured: Boolean = false) {
    Surface(modifier.semantics(mergeDescendants = true) {}.clip(RoundedCornerShape(12.dp)).clickable(enabled = !obscured, onClick = onLegend),
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MapPalette.visited.copy(alpha=.65f)), color = Color.Transparent) {
        AreaMapCanvas(minimap?.cells.orEmpty(), Modifier.fillMaxSize(), minimap = true, obscured = obscured)
    }
}
