package com.example.starborn.domain.cinematic

import org.junit.Assert.assertEquals
import org.junit.Test

class CinematicModelsTest {

    @Test
    fun legacyValuesKeepCardPresentationAndSafeMotionDefaults() {
        assertEquals(CinematicPresentation.CARD, CinematicPresentation.fromRaw(null))
        assertEquals(CinematicCameraMotion.NONE, CinematicCameraMotion.fromRaw(null))
        assertEquals(CinematicTransition.FADE, CinematicTransition.fromRaw(null))
        assertEquals(
            CinematicCaptionStyle.DIALOGUE,
            CinematicCaptionStyle.fromRaw(null, CinematicStepType.DIALOGUE)
        )
    }

    @Test
    fun illustratedValuesParseAllAuthoredPresentationOptions() {
        assertEquals(CinematicPresentation.ILLUSTRATED, CinematicPresentation.fromRaw("illustrated"))
        assertEquals(CinematicCameraMotion.SLOW_PUSH, CinematicCameraMotion.fromRaw("slow_push"))
        assertEquals(CinematicCameraMotion.DRIFT_LEFT, CinematicCameraMotion.fromRaw("drift_left"))
        assertEquals(CinematicTransition.CUT, CinematicTransition.fromRaw("cut"))
        assertEquals(
            CinematicCaptionStyle.SYSTEM,
            CinematicCaptionStyle.fromRaw("system", CinematicStepType.MESSAGE)
        )
        assertEquals(
            CinematicCaptionStyle.NONE,
            CinematicCaptionStyle.fromRaw("none", CinematicStepType.NARRATION)
        )
    }
}
