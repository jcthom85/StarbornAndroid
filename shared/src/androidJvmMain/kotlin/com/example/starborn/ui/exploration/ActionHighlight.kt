package com.example.starborn.ui.exploration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.feature.exploration.presentation.InlineActionPlan
import com.example.starborn.feature.exploration.presentation.InlineActionSegment
import com.example.starborn.feature.exploration.presentation.InlineActionTarget

fun InlineActionSegment.needsAttention(): Boolean =
    !locked && !inspected && ((target as? InlineActionTarget.Room)?.action as? GenericAction)?.drawAttention == true

/** A brief cue when an objective's room control becomes available. */
@Composable
fun actionHighlightAlpha(plan: InlineActionPlan, animate: Boolean): Float {
    val highlighted = plan.segments.filter { it.needsAttention() }.map { it.id }
    val alpha = remember(highlighted) { Animatable(0f) }
    LaunchedEffect(highlighted, animate) {
        alpha.snapTo(0f)
        if (highlighted.isNotEmpty() && animate) repeat(3) {
            alpha.animateTo(.30f, tween(380))
            alpha.animateTo(0f, tween(500))
        }
    }
    return alpha.value
}
