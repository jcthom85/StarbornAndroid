package com.example.starborn.ui.events

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun questObjectiveFeedbackProgress(
    state: QuestPresentationState,
    update: UiEvent.ShowQuestBanner?,
    visible: Boolean,
    reducedMotion: Boolean,
    onCheck: () -> Unit
): Float {
    val checkedIds = update?.checkedObjectives()?.map { it.id }
    val progress = remember(update?.questId, checkedIds) { Animatable(0f) }
    val playCheck = rememberUpdatedState(onCheck)
    LaunchedEffect(state, progress, visible, reducedMotion) {
        if (!visible || update == null) return@LaunchedEffect
        if (state.markObjectivesShown(update)) {
            playCheck.value()
            if (reducedMotion) progress.snapTo(1f)
            else progress.animateTo(1f, tween(470, easing = LinearOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    return if (reducedMotion) 1f else progress.value
}

@Composable
fun QuestObjectiveCheckmark(accent: Color, progress: Float, modifier: Modifier = Modifier, showPulse: Boolean = true) {
    Canvas(modifier.size(18.dp)) {
        val radius = size.minDimension * .34f
        val pulse = if (showPulse) sin(progress * Math.PI).toFloat().coerceAtLeast(0f) else 0f
        val badge = radius * (1f + .12f * pulse)
        drawCircle(accent.copy(alpha = .15f + .12f * pulse), badge)
        drawCircle(accent.copy(alpha = .72f), badge, style = Stroke(1.dp.toPx()))
        if (showPulse && progress > 0f && progress < 1f) {
            drawCircle(accent.copy(alpha = .52f * (1f - progress)),
                radius * (1f + .5f * progress), style = Stroke(1.dp.toPx()))
        }
        val start = center + Offset(-badge * .48f, 0f)
        val joint = center + Offset(-badge * .12f, badge * .32f)
        val end = center + Offset(badge * .49f, -badge * .34f)
        val firstStroke = (progress * 2.5f).coerceIn(0f, 1f)
        val secondStroke = ((progress - .4f) / .6f).coerceIn(0f, 1f)
        if (firstStroke > 0f) drawLine(accent, start, start + (joint - start) * firstStroke,
            1.6.dp.toPx(), StrokeCap.Round)
        if (secondStroke > 0f) drawLine(accent, joint, joint + (end - joint) * secondStroke,
            1.6.dp.toPx(), StrokeCap.Round)
    }
}
