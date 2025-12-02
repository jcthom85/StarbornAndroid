package com.example.starborn.feature.combat.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CombatSide {
    PLAYER,
    ENEMY
}

enum class LungeAxis {
    X, Y
}

@Composable
fun Lungeable(
    modifier: Modifier = Modifier,
    side: CombatSide,
    triggerToken: Any?,
    distance: Dp = 24.dp,
    axis: LungeAxis = LungeAxis.X,
    directionSign: Float = 1f,
    onFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val base = if (side == CombatSide.PLAYER) -1f else +1f
    val signedDirection = base * directionSign
    val resolvedDistance = if (side == CombatSide.PLAYER && axis == LungeAxis.Y) 0.dp else distance

    LaunchedEffect(triggerToken) {
        if (triggerToken != null) {
            offset.snapTo(0f)
            val travel = with(density) { resolvedDistance.toPx() }
            if (travel == 0f) {
                onFinished?.invoke()
                return@LaunchedEffect
            }
            val delta = travel * signedDirection
            offset.animateTo(
                targetValue = delta,
                animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing)
            )
            offset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            )
            onFinished?.invoke()
        }
    }

    val translation = if (axis == LungeAxis.X) {
        offset.value to 0f
    } else {
        0f to offset.value
    }

    Box(modifier = modifier.graphicsLayer { translationX = translation.first; translationY = translation.second }) {
        content()
    }
}
