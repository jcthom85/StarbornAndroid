package com.example.starborn.feature.exploration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.starborn.domain.prompt.MilestonePrompt
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.prompt.UIPrompt

@Composable
fun UIPromptOverlay(
    prompt: UIPrompt?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (prompt) {
        is TutorialPrompt -> {
            TutorialBanner(
                prompt = prompt.entry,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        is MilestonePrompt -> {
            MilestoneBanner(
                prompt = prompt.event,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        else -> Unit
    }
}
