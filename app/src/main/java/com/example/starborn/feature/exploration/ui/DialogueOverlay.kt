package com.example.starborn.feature.exploration.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.82f),
        contentColor = Color.White,
        shadowElevation = 12.dp,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (portraitRes != null) {
                    Image(
                        painter = painterResource(id = portraitRes),
                        contentDescription = line.speaker,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val initial = line.speaker.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = line.speaker,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!dialogue.voiceCue.isNullOrBlank()) {
                        FilledTonalButton(
                            onClick = { onPlayVoice(dialogue.voiceCue) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play voice"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voice")
                        }
                    }
                }
            }

            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Left
            )

            if (choices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    choices.forEachIndexed { index, choice ->
                        val parsed = remember(choice.label) { parseChoiceLabel(choice.label) }
                        DialogueChoiceButton(
                            index = index + 1,
                            choice = choice,
                            parsed = parsed,
                            onChoice = onChoice
                        )
                    }
                }
            } else {
                FilledTonalButton(onClick = onAdvance) {
                    Text(text = if (line.next != null) "Continue" else "Close")
                }
            }
        }
    }
}

@Composable
private fun DialogueChoiceButton(
    index: Int,
    choice: DialogueChoiceUi,
    parsed: ParsedChoiceLabel,
    onChoice: (String) -> Unit
) {
    val baseContainer = when {
        ChoiceTag.MILESTONE in parsed.tags -> Color(0xFF4527A0).copy(alpha = 0.85f)
        ChoiceTag.TUTORIAL in parsed.tags -> Color(0xFF1565C0).copy(alpha = 0.82f)
        ChoiceTag.QUEST in parsed.tags -> Color(0xFF2E7D32).copy(alpha = 0.82f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    FilledTonalButton(
        onClick = { onChoice(choice.id) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = baseContainer,
            contentColor = Color.White
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "$index. ${parsed.text}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
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
            disabledContainerColor = color.copy(alpha = 0.9f),
            disabledLabelColor = Color.White
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
