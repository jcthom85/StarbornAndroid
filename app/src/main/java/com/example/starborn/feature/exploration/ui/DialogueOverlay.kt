package com.example.starborn.feature.exploration.ui

import com.example.starborn.ui.components.maestroTag
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import com.example.starborn.shared.ui.typewriterText

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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.starborn.R
import com.example.starborn.ui.background.rememberAssetPainter
import androidx.compose.ui.graphics.painter.Painter
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

@Composable
fun DialogueOverlay(
    dialogue: DialogueUi,
    choices: List<DialogueChoiceUi>,
    onAdvance: () -> Unit,
    onChoice: (String) -> Unit,
    onPlayVoice: (String) -> Unit,
    onPlayMurmur: (String) -> Unit = {},
    onRevealFinished: () -> Unit = {},
    revealAllRequest: Int = 0,
    canDismissByTap: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = dialogue.canDismiss && onDismiss != null) { onDismiss?.invoke() }
    val line = dialogue.line
    val fullText = remember(line.text) { line.text.replace('[', ' ').replace(']', ' ').trim() }
    val textScroll = rememberScrollState()
    val choiceScroll = rememberScrollState()
    val scrollScope = androidx.compose.runtime.rememberCoroutineScope()
    var textLayout by remember(line.id, fullText) { androidx.compose.runtime.mutableStateOf<TextLayoutResult?>(null) }
    var textViewportHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(line.id, fullText) {
        textScroll.scrollTo(0)
        choiceScroll.scrollTo(0)
    }
    var revealedCount by remember(line.id, fullText) { mutableIntStateOf(0) }
    val revealFinished = revealedCount >= fullText.length
    val displayedText = fullText.take(revealedCount.coerceIn(0, fullText.length))
    val lastVisibleCharacter = displayedText.indexOfLast { !it.isWhitespace() }
    val showTextCue = textScroll.canScrollForward && textViewportHeight > 0 &&
        lastVisibleCharacter >= 0 && textLayout?.let { layout ->
            layout.getLineBottom(layout.getLineForOffset(lastVisibleCharacter)) > textScroll.value + textViewportHeight + 1f
        } == true
    val voiceProfile = remember(line.speaker) { DialogueVoiceProfile.forSpeaker(line.speaker) }
    val latestOnPlayMurmur by rememberUpdatedState(onPlayMurmur)
    val latestOnRevealFinished by rememberUpdatedState(onRevealFinished)

    LaunchedEffect(line.id, fullText, voiceProfile) {
        revealedCount = 0
        if (fullText.isBlank()) {
            revealedCount = fullText.length
            return@LaunchedEffect
        }
        val random = Random(line.id.hashCode())
        for (index in fullText.indices) {
            if (revealedCount >= fullText.length) return@LaunchedEffect
            revealedCount = index + 1
            val char = fullText[index]
            if (voiceProfile != DialogueVoiceProfile.NONE && shouldPlayMurmur(fullText, index)) {
                latestOnPlayMurmur(voiceProfile.randomCue(random))
            }
            delay(revealDelayMs(char))
        }
    }

    LaunchedEffect(line.id, revealFinished) {
        if (revealFinished) latestOnRevealFinished()
    }

    LaunchedEffect(line.id, revealAllRequest) {
        if (revealAllRequest > 0) revealedCount = fullText.length
    }

    val portraitPath = remember(dialogue.portrait) {
        dialogue.portrait?.takeIf { it.isNotBlank() }
    }
    val portraitPainter = rememberAssetPainter(portraitPath, painterResource(R.drawable.main_menu_background))

    val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val outlineColor = accentColor.copy(alpha = 0.55f)
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    val tapToAdvanceAllowed = canDismissByTap && (choices.isEmpty() || !revealFinished)
    val tapInteraction = remember { MutableInteractionSource() }
    val containerModifier = Modifier
        .fillMaxWidth()
        .maestroTag(if (tapToAdvanceAllowed) "dialogue-advance" else "dialogue-choice-prompt")
        .semantics { contentDescription = if (tapToAdvanceAllowed) "Dialogue Popup. Tap to continue" else "Dialogue Popup" }
        .let { base ->
            if (tapToAdvanceAllowed) {
                base.clickable(
                    interactionSource = tapInteraction,
                    indication = null
                ) {
                    if (!revealFinished) {
                        revealedCount = fullText.length
                    } else {
                        onAdvance()
                    }
                }
            } else {
                base
            }
        }
    val hasVoice = !dialogue.voiceCue.isNullOrBlank()
    val maximumCardHeight = (LocalConfiguration.current.screenHeightDp.dp - 160.dp).coerceAtLeast(240.dp)

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
                    .heightIn(max = maximumCardHeight)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (portraitPath != null) PortraitCard(portraitPainter,
                        dialogue.displaySpeaker ?: line.speaker, accentColor)
                    SpeakerName(dialogue.displaySpeaker ?: line.speaker, accentColor, Modifier.weight(1f))
                    if (hasVoice && dialogue.canDismiss) {
                        IconButton(onClick = { onPlayVoice(dialogue.voiceCue.orEmpty()) }) {
                            Icon(Icons.Default.VolumeUp, "Replay voice", tint = accentColor)
                        }
                    }
                    if (dialogue.canDismiss && onDismiss != null) {
                        Surface(onClick = onDismiss, shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = .12f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = .5f)),
                            modifier = Modifier.heightIn(min = 48.dp).maestroTag("dialogue-leave")
                                .semantics { contentDescription = "Leave conversation" }) {
                            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text("Leave", color = accentColor, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    if (hasVoice && !dialogue.canDismiss) VoicePulseChip(
                        onClick = { onPlayVoice(dialogue.voiceCue.orEmpty()) }, accentColor = accentColor)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 240.dp)
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
                    Box(Modifier.weight(1f).heightIn(max = 240.dp)) {
                        Box(Modifier.fillMaxWidth().onSizeChanged { textViewportHeight = it.height }
                            .verticalScroll(textScroll)
                            .pointerInput(fullText, revealFinished) {
                                if (!revealFinished) awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    var moved = false
                                    var event = awaitPointerEvent(PointerEventPass.Initial)
                                    while (true) {
                                        if (event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }) moved = true
                                        if (event.changes.none { it.pressed }) break
                                        event = awaitPointerEvent(PointerEventPass.Initial)
                                    }
                                    if (!moved) {
                                        event.changes.forEach { it.consume() }
                                        revealedCount = fullText.length
                                    }
                                }
                            }
                        ) {
                        Text(
                            text = typewriterText(buildAnnotatedString {
                                append(fullText)
                                // Keep narration distinct from speech without displaying authoring brackets.
                                val leadingSpace = line.text.length - line.text.trimStart().length
                                val offset = leadingSpace + if (line.text.trimStart().startsWith("[")) 1 else 0
                                Regex("\\[[^\\]]+]").findAll(line.text).forEach { match ->
                                    val start = (match.range.first - offset).coerceIn(0, length)
                                    val end = (match.range.last + 1 - offset).coerceIn(start, length)
                                    addStyle(SpanStyle(fontStyle = FontStyle.Italic,
                                        color = accentColor.copy(alpha = .85f)), start, end)
                                }
                            }, displayedText.length),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Left,
                            onTextLayout = { textLayout = it }
                        )
                        }
                        ScrollContinuationCue(showTextCue, accentColor, "Scroll for more dialogue", "dialogue-more-text",
                            onClick = { scrollScope.launch {
                                textScroll.animateScrollTo((textScroll.value + textViewportHeight * .7f).toInt().coerceAtMost(textScroll.maxValue))
                            } }, modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }

                if (choices.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 220.dp)) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(choiceScroll)
                            .alpha(if (revealFinished) 1f else 0f)
                            .then(
                                if (revealFinished) Modifier
                                else Modifier.clearAndSetSemantics { }
                            ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Responses",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.8f)
                        )
                        choices.sortedBy { if (it.id.endsWith("_leave")) 0 else 1 }.forEach { choice ->
                            val parsed = remember(choice.label) { parseChoiceLabel(choice.label) }
                            DialogueChoiceButton(
                                choice = choice,
                                parsed = parsed,
                                onChoice = onChoice,
                                accentColor = accentColor,
                                enabled = revealFinished
                            )
                        }
                    }
                    ScrollContinuationCue(revealFinished && choiceScroll.maxValue != Int.MAX_VALUE && choiceScroll.viewportSize > 0 && choiceScroll.canScrollForward, accentColor,
                        "Scroll for more responses", "dialogue-more-responses",
                        onClick = { scrollScope.launch {
                            choiceScroll.animateScrollTo((choiceScroll.value + choiceScroll.viewportSize * .7f).toInt().coerceAtMost(choiceScroll.maxValue))
                        } }, modifier = Modifier.align(Alignment.BottomCenter))
                    }
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
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
        fontWeight = FontWeight.Bold,
        color = accentColor.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VoicePulseChip(
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
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
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
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
    painter: Painter,
    speaker: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val portraitScale = remember { androidx.compose.animation.core.Animatable(0.92f) }
    LaunchedEffect(painter) {
        portraitScale.snapTo(0.92f)
        portraitScale.animateTo(
            targetValue = 1.0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
            .size(PortraitSize)
            .graphicsLayer {
                scaleX = portraitScale.value
                scaleY = portraitScale.value
            }
    ) {
        Image(
            painter = painter,
            contentDescription = speaker,
            contentScale = ContentScale.Crop
        )
    }
}


@Composable
private fun DialogueChoiceButton(
    choice: DialogueChoiceUi,
    parsed: ParsedChoiceLabel,
    onChoice: (String) -> Unit,
    accentColor: Color,
    enabled: Boolean
) {
    Surface(
        onClick = { onChoice(choice.id) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = parsed.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = if (ChoiceTag.ACTION in parsed.tags) FontStyle.Italic else FontStyle.Normal),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (parsed.tags.any { it != ChoiceTag.ACTION }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    parsed.tags.filter { it != ChoiceTag.ACTION }.forEach { tag ->
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
        ChoiceTag.ACTION -> "Action" to Color(0xFFB9DCD5)
        ChoiceTag.QUEST -> "Quest" to Color(0xFF43A047)
        ChoiceTag.TUTORIAL -> "Tutorial" to Color(0xFF1E88E5)
        ChoiceTag.MILESTONE -> "Milestone" to Color(0xFF7E57C2)
        ChoiceTag.SHOP -> "🛒 Shop" to Color(0xFFFFB300)
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

private enum class ChoiceTag { QUEST, TUTORIAL, MILESTONE, SHOP, ACTION }

private fun parseChoiceLabel(raw: String): ParsedChoiceLabel {
    var remaining = raw.trim()
    val tags = mutableListOf<ChoiceTag>()
    val bracketRegex = Regex("^\\s*\\[(.*?)\\]\\s*")
    while (true) {
        val match = bracketRegex.find(remaining) ?: break
        val tagText = match.groupValues[1].trim().lowercase(Locale.getDefault())
        val tag = tagText.toChoiceTag()
        if (tag == null) {
            tags += ChoiceTag.ACTION
            remaining = listOf(match.groupValues[1].trim(), remaining.removeRange(match.range).trim()).filter { it.isNotBlank() }.joinToString(" ")
            break
        }
        tags += tag
        remaining = remaining.removeRange(match.range).trimStart()
    }
    if (tags.isEmpty()) {
        val lowered = remaining.lowercase(Locale.getDefault())
        when {
            Regex("\\bquest\\b").containsMatchIn(lowered) -> tags += ChoiceTag.QUEST
            Regex("\\btutorial\\b").containsMatchIn(lowered) -> tags += ChoiceTag.TUTORIAL
            Regex("\\bmilestone\\b").containsMatchIn(lowered) -> tags += ChoiceTag.MILESTONE
            Regex("\\b(shop|trade|contraband|wares|buy|sell)\\b").containsMatchIn(lowered) -> tags += ChoiceTag.SHOP
        }
    }
    val cleaned = remaining.removePrefix("-").trimStart()
    return ParsedChoiceLabel(text = cleaned.ifBlank { raw }, tags = tags.distinct())
}

private fun String.toChoiceTag(): ChoiceTag? = when (this.lowercase(Locale.getDefault())) {
    "action" -> ChoiceTag.ACTION
    "quest" -> ChoiceTag.QUEST
    "tutorial" -> ChoiceTag.TUTORIAL
    "milestone" -> ChoiceTag.MILESTONE
    "shop", "trade" -> ChoiceTag.SHOP
    else -> null
}

internal enum class DialogueVoiceProfile(private val cuePrefix: String?) {
    NOVA("voice_murmur_female"),
    ORION("voice_murmur_male"),
    ZEKE("voice_murmur_male"),
    GH0ST("voice_murmur_male"),
    FEMALE("voice_murmur_female"),
    MALE("voice_murmur_male"),
    MACHINE("voice_murmur_machine"),
    NONE(null);

    fun randomCue(random: Random): String {
        val prefix = cuePrefix ?: return ""
        val variant = random.nextInt(1, 5)
        return "${prefix}_${variant.toString().padStart(2, '0')}"
    }

    companion object {
        fun forSpeaker(speaker: String): DialogueVoiceProfile {
            val normalized = speaker.trim().lowercase(Locale.getDefault())
            return when (normalized) {
                "nova",
                "player" -> NOVA

                "???",
                "orion" -> ORION

                "zeke",
                "ollie" -> ZEKE

                "gh0st",
                "ghost" -> GH0ST

                "maddie",
                "ellie",
                "mika",
                "elara",
                "caretaker iva",
                "doctor sera",
                "juno",
                "pasha",
                "rana",
                "sima",
                "schoolteacher kasey" -> FEMALE

                "sentinel-3" -> MACHINE

                "system",
                "lab terminal",
                "maintenance bot" -> NONE

                "vale",
                "arden vale" -> MALE

                else -> MALE
            }
        }
    }
}

private fun shouldPlayMurmur(text: String, index: Int): Boolean {
    val char = text[index]
    if (!char.isLetterOrDigit()) return false
    val spokenCharsBefore = text.take(index).count { it.isLetterOrDigit() }
    return spokenCharsBefore % 3 == 0
}

private fun revealDelayMs(char: Char): Long = when (char) {
    '.', '!', '?' -> 95L
    ',', ';', ':' -> 55L
    else -> 26L
}

private val PortraitSize = 64.dp
