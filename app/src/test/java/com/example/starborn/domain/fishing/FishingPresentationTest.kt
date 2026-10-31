package com.example.starborn.domain.fishing

import com.example.starborn.feature.fishing.viewmodel.*
import org.junit.Assert.*
import org.junit.Test

class FishingPresentationTest {
    @Test fun onlyDeliberateUpwardSwipesSetTheHook() {
        assertTrue(isUpwardHookSwipe(10f, -70f, 48f))
        assertFalse(isUpwardHookSwipe(70f, -50f, 48f))
        assertFalse(isUpwardHookSwipe(0f, 70f, 48f))
        assertFalse(isUpwardHookSwipe(0f, -12f, 48f))
    }
    @Test fun dangerOverridesReelingAdviceAndNeverDependsOnlyOnColor() {
        fun state(phase: FishingFightPhase, tension: Float) = FishingUiState(fishingState = FishingState.REELING,
            reelState = FishingReelState(.4f, tension, false, "Fish", null, phase))
        assertTrue(fishingCue(state(FishingFightPhase.SURGE, .2f)).urgent)
        assertTrue(fishingCue(state(FishingFightPhase.CALM, .9f)).instruction.contains("Release"))
        assertFalse(fishingCue(state(FishingFightPhase.CALM, .2f)).urgent)
    }
    @Test fun firstBiteAndReelLessonAreExplicitlyUnhurried() {
        val hook = fishingCue(FishingUiState(fishingState = FishingState.HOOKSET,
            hookState = FishingHookState(0, false, true, true)))
        assertTrue(hook.instruction.contains("waits"))
        assertTrue(hook.instruction.contains("tap"))
        assertTrue(fishingCue(FishingUiState(fishingState = FishingState.READY)).instruction.contains("waits"))
    }
}
