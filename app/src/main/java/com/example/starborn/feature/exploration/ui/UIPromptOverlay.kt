package com.example.starborn.feature.exploration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.starborn.domain.prompt.ItemBatchGrantedPrompt
import com.example.starborn.domain.prompt.ItemGrantedPrompt
import com.example.starborn.domain.prompt.MilestonePrompt
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.prompt.UIPrompt

@Composable
fun UIPromptOverlay(
    prompt: UIPrompt?,
    onDismiss: () -> Unit,
    onCollectAll: (String) -> Unit = {},
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
        is ItemGrantedPrompt -> {
            ItemGrantedBanner(
                prompt = prompt,
                onDismiss = onDismiss,
                onCollectAll = onCollectAll,
                modifier = modifier
            )
        }
        is ItemBatchGrantedPrompt -> {
            ItemBatchGrantedBanner(
                summary = prompt.summary,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        else -> Unit
    }
}
