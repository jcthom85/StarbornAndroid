package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.feature.exploration.viewmodel.CinematicStepUi
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import java.util.Locale

object CinematicPlaybackManager {
    fun scrubDescription(text: String?, tokens: List<String>): String? {
        if (text.isNullOrBlank()) return null
        val source = requireNotNull(text)
        var updated = source
        tokens.forEach { token ->
            if (token.isNotBlank()) {
                updated = updated.replace(token, " ")
            }
        }
        val normalized = updated
            .replace(Regex("\\s{2,}"), " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .trim()
        val original = source.trim()
        return if (normalized.isEmpty() || normalized == original) null else normalized
    }

    fun toUiState(
        playback: CinematicPlaybackState,
        resolveEmotePortrait: (String, String?) -> String?,
        resolvePortraitKey: (String) -> String?
    ): CinematicUiState {
        val steps = playback.scene.steps
        if (steps.isEmpty()) {
            return CinematicUiState(
                sceneId = playback.scene.id,
                title = playback.scene.title,
                backdrop = playback.scene.backdrop,
                stepIndex = 0,
                stepCount = 0,
                step = CinematicStepUi(
                    type = CinematicStepType.NARRATION,
                    speaker = null,
                    text = "",
                    portrait = null
                )
            )
        }
        val safeIndex = playback.stepIndex.coerceIn(0, steps.lastIndex)
        val step = steps[safeIndex]
        val portrait = step.speaker?.let { speaker ->
            resolveEmotePortrait(speaker, step.emote) ?: resolvePortraitKey(speaker)
        }
        val stepUi = CinematicStepUi(
            type = step.type,
            speaker = step.speaker,
            text = step.text,
            portrait = portrait
        )
        return CinematicUiState(
            sceneId = playback.scene.id,
            title = playback.scene.title,
            backdrop = playback.scene.backdrop,
            stepIndex = safeIndex,
            stepCount = steps.size,
            step = stepUi
        )
    }
}
