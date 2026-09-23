package com.example.starborn.feature.mainmenu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.feature.exploration.ui.DialogueOverlay
import com.example.starborn.feature.exploration.viewmodel.DialogueUi

/** Presentation-only booth scene. No campaign dialogue triggers, milestones or rewards. */
private val homecomingLines = listOf(
    "Nova" to "Everyone made it back. I’m calling that a good day.",
    "Zeke" to "Good day? We haven’t checked the high scores yet.",
    "Gh0st" to "I checked. Yours are intact.",
    "Zeke" to "See? Excellent day.",
    "Orion" to "Let the next adventure wait a moment.",
    "Nova" to "Welcome aboard the Astra. Make yourself at home."
)

@Composable
internal fun BurgQuestHomecoming(audioCuePlayer: AudioCuePlayer, onComplete: () -> Unit) {
    var lineIndex by remember { mutableIntStateOf(0) }
    var revealed by remember(lineIndex) { mutableStateOf(false) }
    var revealRequest by remember(lineIndex) { mutableIntStateOf(0) }
    val (speaker, text) = homecomingLines[lineIndex]
    val advance: () -> Unit = { if (lineIndex < homecomingLines.lastIndex) lineIndex++ else onComplete() }
    Dialog(onDismissRequest = onComplete) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("burgquest-homecoming"),
            horizontalAlignment = Alignment.End
        ) {
            DialogueOverlay(
                dialogue = DialogueUi(
                    line = DialogueLine("burgquest_homecoming_$lineIndex", speaker, text),
                    portrait = "images/characters/${speaker.lowercase(java.util.Locale.ROOT)}_portrait.png",
                    voiceCue = null
                ),
                choices = emptyList(),
                onAdvance = advance,
                onChoice = {},
                onPlayVoice = {},
                onRevealFinished = { revealed = true },
                revealAllRequest = revealRequest,
                onPlayMurmur = { cue -> audioCuePlayer.execute(listOf(
                    AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L, gain = 0.85f)
                )) }
            )
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onComplete) { Text("Skip") }
                    Button(onClick = { if (revealed) advance() else revealRequest++ }) {
                        Text(if (!revealed) "Show text" else if (lineIndex == homecomingLines.lastIndex) "Explore the Astra" else "Next")
                    }
                }
            }
        }
    }
}
