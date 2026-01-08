package com.example.starborn.feature.exploration.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import java.util.Locale

@Composable
fun DialogueOverlay(
    dialogue: DialogueUi,
    choices: List<DialogueChoiceUi>,
    onAdvance: () -> Unit,
    onChoice: (String) -> Unit,
    onPlayVoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val line = dialogue.line
    val context = LocalContext.current
    val portraitRes = remember(dialogue.portrait) {
        val candidates = mutableListOf<String>()
        dialogue.portrait?.takeIf { it.isNotBlank() }?.let { provided ->
            candidates += provided
            if (provided.contains('/')) {
                candidates += provided.substringAfterLast('/').substringBeforeLast('.')
            }
        }
        candidates += "communicator_portrait"
        candidates.firstNotNullOfOrNull { candidate ->
            val trimmed = candidate.trim()
            if (trimmed.isEmpty()) {
                null
            } else {
                context.resources.getIdentifier(trimmed, "drawable", context.packageName).takeIf { it != 0 }
            }
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val outlineColor = accentColor.copy(alpha = 0.55f)
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    val canDismissByTap = choices.isEmpty()
    val tapInteraction = remember { MutableInteractionSource() }
    val containerModifier = Modifier.fillMaxWidth().let { base ->
        if (canDismissByTap) {
            base.clickable(
                interactionSource = tapInteraction,
                indication = null
            ) { onAdvance() }
        } else {
            base
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            modifier = containerModifier,
            color = surfaceColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, outlineColor),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 16.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DialogueMessageCard(
                    text = line.text,
                    speaker = line.speaker,
                    portraitRes = portraitRes,
                    accentColor = accentColor,
                    voiceCue = dialogue.voiceCue,
                    onPlayVoice = onPlayVoice
                )

                if (choices.isNotEmpty()) {
                    Text(
                        text = "Responses",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        choices.forEachIndexed { index, choice ->
                            val parsed = remember(choice.label) { parseChoiceLabel(choice.label) }
                            DialogueChoiceButton(
                                index = index + 1,
                                choice = choice,
                                parsed = parsed,
                                onChoice = onChoice,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueMessageCard(
    text: String,
    speaker: String,
    portraitRes: Int?,
    accentColor: Color,
    voiceCue: String?,
    onPlayVoice: (String) -> Unit
) {
    val hasVoice = !voiceCue.isNullOrBlank()
    val messageTopPadding = if (portraitRes != null) PortraitSize - PortraitLift - PortraitOverlap else 0.dp
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = messageTopPadding),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.16f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (portraitRes == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpeakerName(
                            name = speaker,
                            accentColor = accentColor
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (hasVoice) {
                            VoicePulseChip(
                                onClick = { onPlayVoice(voiceCue.orEmpty()) },
                                accentColor = accentColor
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.7f))
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Left
                    )
                }
            }
        }
        if (portraitRes != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .offset(x = PortraitInset, y = -PortraitLift),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PortraitCard(
                        portraitRes = portraitRes,
                        speaker = speaker,
                        accentColor = accentColor
                    )
                    SpeakerName(
                        name = speaker,
                        accentColor = accentColor
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (hasVoice) {
                    VoicePulseChip(
                        onClick = { onPlayVoice(voiceCue.orEmpty()) },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeakerName(
    name: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = accentColor.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VoicePulseChip(
    onClick: () -> Unit,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "voicePulse")
    val pulse by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulseAlpha"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 18.dp, height = 12.dp)) {
                val barWidth = size.width / 5f
                val gaps = barWidth / 2f
                val base = size.height * 0.35f
                val heights = listOf(0.6f, 1f, 0.7f)
                heights.forEachIndexed { index, mult ->
                    val x = index * (barWidth + gaps)
                    val barHeight = base + size.height * 0.45f * mult * pulse
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.9f),
                        topLeft = Offset(x, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth, barWidth)
                    )
                }
            }
            Text(
                text = "Voice",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun PortraitCard(
    portraitRes: Int,
    speaker: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.size(PortraitSize)
    ) {
        Image(
            painter = painterResource(id = portraitRes),
            contentDescription = speaker,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DialogueChoiceButton(
    index: Int,
    choice: DialogueChoiceUi,
    parsed: ParsedChoiceLabel,
    onChoice: (String) -> Unit,
    accentColor: Color
) {
    Surface(
        onClick = { onChoice(choice.id) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$index. ${parsed.text}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (parsed.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    parsed.tags.forEach { tag ->
                        ChoiceTagChip(tag)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceTagChip(tag: ChoiceTag) {
    val (label, color) = when (tag) {
        ChoiceTag.QUEST -> "Quest" to Color(0xFF43A047)
        ChoiceTag.TUTORIAL -> "Tutorial" to Color(0xFF1E88E5)
        ChoiceTag.MILESTONE -> "Milestone" to Color(0xFF7E57C2)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.18f),
            disabledLabelColor = color
        )
    )
}

private data class ParsedChoiceLabel(
    val text: String,
    val tags: List<ChoiceTag>
)

private enum class ChoiceTag { QUEST, TUTORIAL, MILESTONE }

private fun parseChoiceLabel(raw: String): ParsedChoiceLabel {
    var remaining = raw.trim()
    val tags = mutableListOf<ChoiceTag>()
    val bracketRegex = Regex("^\\s*\\[(.*?)\\]\\s*")
    while (true) {
        val match = bracketRegex.find(remaining) ?: break
        val tagText = match.groupValues[1].trim().lowercase(Locale.getDefault())
        tagText.toChoiceTag()?.let { tags += it }
        remaining = remaining.removeRange(match.range).trimStart()
    }
    if (tags.isEmpty()) {
        val lowered = remaining.lowercase(Locale.getDefault())
        when {
            "quest" in lowered -> tags += ChoiceTag.QUEST
            "tutorial" in lowered -> tags += ChoiceTag.TUTORIAL
            "milestone" in lowered -> tags += ChoiceTag.MILESTONE
        }
    }
    val cleaned = remaining.removePrefix("-").trimStart()
    return ParsedChoiceLabel(text = cleaned.ifBlank { raw }, tags = tags.distinct())
}

private fun String.toChoiceTag(): ChoiceTag? = when (this.lowercase(Locale.getDefault())) {
    "quest" -> ChoiceTag.QUEST
    "tutorial" -> ChoiceTag.TUTORIAL
    "milestone" -> ChoiceTag.MILESTONE
    else -> null
}

private val PortraitSize = 64.dp
private val PortraitLift = 8.dp
private val PortraitOverlap = 10.dp
private val PortraitInset = 6.dp
