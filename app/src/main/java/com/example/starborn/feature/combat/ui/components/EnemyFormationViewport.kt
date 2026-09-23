package com.example.starborn.feature.combat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Measure the complete formation, including composite bosses, before fitting it above the party. */
@Composable
fun EnemyFormationViewport(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = {
        // Leave room for idle motion, recoil and selection effects around the formation.
        Box(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) { content() }
    }) { measurables, constraints ->
        val formation = measurables.single().measure(
            Constraints(maxWidth = constraints.maxWidth, maxHeight = Constraints.Infinity)
        )
        val scale = minOf(1f, constraints.maxHeight.toFloat() / formation.height.coerceAtLeast(1))
        layout(constraints.maxWidth, constraints.maxHeight) {
            formation.placeWithLayer(
                ((constraints.maxWidth - formation.width * scale) / 2).roundToInt(),
                ((constraints.maxHeight - formation.height * scale) / 2).roundToInt()
            ) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
            }
        }
    }
}
