package com.example.starborn.feature.fishing.viewmodel

import com.example.starborn.domain.fishing.FishingFightPhase
import kotlin.math.abs

data class FishingCue(val title: String, val instruction: String, val urgent: Boolean = false)

fun fishingCue(state: FishingUiState): FishingCue = when (state.fishingState) {
    FishingState.WAITING -> FishingCue("Let the water settle", "Watch the float. A ripple will tell you when to hook.")
    FishingState.HOOKSET -> FishingCue("A bite — set the hook", if (state.hookState?.practiceHook == true)
        "Swipe up on the water, or tap Set hook. This first bite waits for you." else "Swipe up on the water, or tap Set hook.")
    FishingState.READY -> FishingCue("Feel the line", "Hold to reel. When the fish pulls hard, release and give it slack. Your first fight waits until you're ready.")
    FishingState.REELING -> when {
        state.reelState?.salvage == true -> FishingCue("Bring it ashore", "Hold to retrieve. Release if the line gets tight.")
        (state.reelState?.tension ?: 0f) >= .75f -> FishingCue("The line is tight", "Release the reel and let the line relax.", true)
        state.reelState?.phase == FishingFightPhase.WARNING -> FishingCue("A surge is coming", "Release now. Let the fish pull.", true)
        state.reelState?.phase == FishingFightPhase.SURGE -> FishingCue("Give it slack", "Keep the reel released until the water settles.", true)
        state.reelState?.phase == FishingFightPhase.PULL -> FishingCue("A steady pull", "Reel gently. Release if the line gets tight.")
        (state.reelState?.staminaRemaining ?: 1f) < .25f -> FishingCue("It's tiring", "Keep a steady rhythm. You're bringing it home.")
        else -> FishingCue("Reel it closer", "Hold to reel while the water is calm.")
    }
    FishingState.RESULT -> FishingCue("Back on shore", "Your catch is secured before you leave.")
    else -> FishingCue("Take a moment by the water", "Choose your gear and cast when you're ready.")
}

/** A deliberate upward stroke; sideways/downward drags and small adjustments never hook. */
fun isUpwardHookSwipe(horizontal: Float, vertical: Float, threshold: Float): Boolean =
    threshold > 0f && vertical <= -threshold && abs(vertical) > abs(horizontal) * 1.25f
