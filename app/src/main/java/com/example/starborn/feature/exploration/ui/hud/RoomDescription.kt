package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import kotlin.math.roundToInt

private const val ACTION_TAG = "action"

sealed interface InlineActionTarget {
    data class Room(val action: RoomAction) : InlineActionTarget
    data class Npc(val name: String) : InlineActionTarget
    data class Enemy(val id: String, val label: String) : InlineActionTarget
}

data class InlineActionSegment(
    val id: String,
    val target: InlineActionTarget,
    val start: Int,
    val end: Int,
    val locked: Boolean
)

data class InlineActionPlan(
    val description: String,
    val segments: List<InlineActionSegment>
)

private data class InlineActionHitBox(
    val segment: InlineActionSegment,
    val line: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
}

private fun InlineActionSegment.accessibilityLabel(): String =
    when (val target = target) {
        is InlineActionTarget.Room -> target.action.name
        is InlineActionTarget.Npc -> target.name
        is InlineActionTarget.Enemy -> target.label
    }

private fun buildInlineActionHitBoxes(
    segments: List<InlineActionSegment>,
    layout: TextLayoutResult
): List<InlineActionHitBox> =
    buildList {
        segments.forEach { segment ->
            val start = segment.start.coerceIn(0, layout.layoutInput.text.length)
            val end = segment.end.coerceIn(start, layout.layoutInput.text.length)
            if (start >= end) return@forEach

            val lineBounds = linkedMapOf<Int, Rect>()
            for (offset in start until end) {
                val bounds = layout.getBoundingBox(offset)
                if (bounds.width <= 0f || bounds.height <= 0f) continue
                val line = layout.getLineForOffset(offset)
                lineBounds[line] = lineBounds[line]?.let { existing ->
                    Rect(
                        left = minOf(existing.left, bounds.left),
                        top = minOf(existing.top, bounds.top),
                        right = maxOf(existing.right, bounds.right),
                        bottom = maxOf(existing.bottom, bounds.bottom)
                    )
                } ?: bounds
            }

            lineBounds.forEach { (line, bounds) ->
                add(
                    InlineActionHitBox(
                        segment = segment,
                        line = line,
                        left = bounds.left,
                        top = bounds.top,
                        right = bounds.right,
                        bottom = bounds.bottom
                    )
                )
            }
        }
    }

@Composable
fun RoomDescription(
    plan: InlineActionPlan?,
    description: String?,
    isDark: Boolean,
    textColor: Color,
    onAction: (RoomAction) -> Unit,
    onNpcClick: (String) -> Unit,
    onEnemyClick: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (description.isNullOrBlank()) {
        Text(
            text = "No description available.",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = 0.92f),
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }
    if (plan == null) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }

    val defaultColor = textColor
    val highlightColor = accentColor.copy(alpha = if (isDark) 0.78f else 0.92f)
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val enemyAccent = Color(0xFFFF8A80)

    val annotatedText = remember(plan, highlightColor, disabledColor, accentColor, isDark) {
        buildAnnotatedString {
            append(plan.description)
            plan.segments.forEach { segment ->
                val (color, weight, decoration) = when (segment.target) {
                    is InlineActionTarget.Npc -> Triple(Color.White, FontWeight.Bold, TextDecoration.Underline)
                    is InlineActionTarget.Enemy -> Triple(enemyAccent, FontWeight.SemiBold, TextDecoration.Underline)
                    is InlineActionTarget.Room -> {
                        val clr = if (segment.locked) disabledColor else highlightColor
                        Triple(clr, FontWeight.Bold, if (segment.locked) TextDecoration.None else TextDecoration.Underline)
                    }
                }
                val backgroundColor = when (segment.target) {
                    is InlineActionTarget.Npc -> accentColor.copy(alpha = if (isDark) 0.34f else 0.26f)
                    else -> Color.Transparent
                }
                addStyle(
                    SpanStyle(
                        color = color,
                        fontWeight = weight,
                        textDecoration = decoration,
                        background = backgroundColor
                    ),
                    start = segment.start,
                    end = segment.end
                )
                addStringAnnotation(
                    tag = ACTION_TAG,
                    annotation = segment.id,
                    start = segment.start,
                    end = segment.end
                )
            }
        }
    }
    val actionLookup = remember(plan) {
        plan.segments.associateBy { it.id }
    }
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = defaultColor, textAlign = TextAlign.Start)
    var textLayout by remember(plan) { mutableStateOf<TextLayoutResult?>(null) }

    fun activateSegment(segment: InlineActionSegment) {
        when (val target = segment.target) {
            is InlineActionTarget.Room -> if (!segment.locked) onAction(target.action)
            is InlineActionTarget.Npc -> onNpcClick(target.name)
            is InlineActionTarget.Enemy -> onEnemyClick(target.id)
        }
    }

    Box(modifier = modifier) {
        ClickableText(
            text = annotatedText,
            style = bodyStyle,
            onTextLayout = { textLayout = it }
        ) { offset ->
            annotatedText.getStringAnnotations(ACTION_TAG, offset, offset).firstOrNull()?.let { annotation ->
                val segment = actionLookup[annotation.item] ?: return@ClickableText
                activateSegment(segment)
            }
        }

        val hitBoxes = remember(textLayout, plan) {
            textLayout?.let { buildInlineActionHitBoxes(plan.segments, it) }.orEmpty()
        }

        hitBoxes.forEach { hitBox ->
            val segment = hitBox.segment
            val label = segment.accessibilityLabel()
            val interactionSource = remember(segment.id, hitBox.line) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = hitBox.left.roundToInt(),
                            y = hitBox.top.roundToInt()
                        )
                    }
                    .size(
                        width = with(LocalDensity.current) { hitBox.width.toDp() },
                        height = with(LocalDensity.current) { hitBox.height.toDp() }
                    )
                    .semantics {
                        contentDescription = label
                        if (!segment.locked) {
                            onClick(label = label) {
                                activateSegment(segment)
                                true
                            }
                        }
                    }
                    .clickable(
                        enabled = !segment.locked,
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        activateSegment(segment)
                    }
            )
        }
    }
}

@Composable
fun RoomDescriptionPanel(
    currentRoom: Room?,
    description: String?,
    plan: InlineActionPlan?,
    isDark: Boolean,
    textColor: Color,
    onAction: (RoomAction) -> Unit,
    onNpcClick: (String) -> Unit,
    onEnemyClick: (String) -> Unit,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (currentRoom == null && description.isNullOrBlank()) return
    val scrollState = rememberScrollState()
    LaunchedEffect(currentRoom?.id) {
        scrollState.scrollTo(0)
    }
    Surface(
        modifier = modifier,
        color = Color(0xFF061018).copy(alpha = if (isDark) 0.76f else 0.50f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.72f else 0.42f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.05f else 0.07f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isDark) 0.10f else 0.04f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF9F2E).copy(alpha = 0.50f),
                                accentColor.copy(alpha = 0.26f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                RoomDescription(
                    plan = plan,
                    description = description,
                    isDark = isDark,
                    textColor = textColor,
                    onAction = onAction,
                    onNpcClick = onNpcClick,
                    onEnemyClick = onEnemyClick,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
