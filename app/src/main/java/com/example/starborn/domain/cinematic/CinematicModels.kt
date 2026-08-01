package com.example.starborn.domain.cinematic

data class CinematicScene(
    val id: String,
    val title: String? = null,
    val backdrop: CinematicBackdrop = CinematicBackdrop.ROOM,
    val presentation: CinematicPresentation = CinematicPresentation.CARD,
    val ambientCue: String? = null,
    val skippable: Boolean = false,
    val steps: List<CinematicStep> = emptyList()
)

enum class CinematicPresentation {
    CARD,
    ILLUSTRATED;

    companion object {
        fun fromRaw(raw: String?): CinematicPresentation = when (raw?.lowercase()) {
            "illustrated" -> ILLUSTRATED
            else -> CARD
        }
    }
}

enum class CinematicBackdrop {
    ROOM,
    BLACK;

    companion object {
        fun fromRaw(raw: String?): CinematicBackdrop = when (raw?.lowercase()) {
            "black" -> BLACK
            else -> ROOM
        }
    }
}

data class CinematicStep(
    val type: CinematicStepType,
    val speaker: String? = null,
    val text: String,
    val portrait: String? = null,
    val durationSeconds: Double? = null,
    val emote: String? = null,
    val imagePath: String? = null,
    val cameraMotion: CinematicCameraMotion = CinematicCameraMotion.NONE,
    val cameraStartScale: Double? = null,
    val cameraEndScale: Double? = null,
    val cameraStartX: Double? = null,
    val cameraEndX: Double? = null,
    val cameraStartY: Double? = null,
    val cameraEndY: Double? = null,
    val transition: CinematicTransition = CinematicTransition.FADE,
    val audioCue: String? = null,
    val voiceCue: String? = null,
    /**
     * Starts a music track on the music layer for the rest of the scene. Unlike
     * [audioCue] (a one-shot on the UI layer) this is routed through AudioRouter so
     * the track is handed over cleanly when the next room's audio applies.
     */
    val musicCue: String? = null,
    /**
     * Dims this step's frame to black over the final N seconds of its duration.
     * Steps otherwise only fade in, so without this the last step of a scene pops
     * off in a single frame.
     */
    val fadeOutSeconds: Double? = null,
    val captionStyle: CinematicCaptionStyle = CinematicCaptionStyle.NARRATION
)

enum class CinematicCameraMotion {
    NONE,
    SLOW_PUSH,
    DRIFT_LEFT,
    DRIFT_RIGHT;

    companion object {
        fun fromRaw(raw: String?): CinematicCameraMotion = when (raw?.lowercase()) {
            "slow_push" -> SLOW_PUSH
            "drift_left" -> DRIFT_LEFT
            "drift_right" -> DRIFT_RIGHT
            else -> NONE
        }
    }
}

enum class CinematicTransition {
    FADE,
    CUT;

    companion object {
        fun fromRaw(raw: String?): CinematicTransition = when (raw?.lowercase()) {
            "cut" -> CUT
            else -> FADE
        }
    }
}

enum class CinematicCaptionStyle {
    LOCATION,
    SYSTEM,
    NARRATION,
    DIALOGUE,
    NONE;

    companion object {
        fun fromRaw(raw: String?, type: CinematicStepType): CinematicCaptionStyle = when (raw?.lowercase()) {
            "location" -> LOCATION
            "system" -> SYSTEM
            "dialogue" -> DIALOGUE
            "none" -> NONE
            "narration" -> NARRATION
            else -> if (type == CinematicStepType.DIALOGUE) DIALOGUE else NARRATION
        }
    }
}

enum class CinematicStepType {
    DIALOGUE,
    NARRATION,
    MESSAGE;

    companion object {
        fun fromRaw(raw: String?): CinematicStepType {
            return when (raw?.lowercase()) {
                "dialogue" -> DIALOGUE
                "message" -> MESSAGE
                "narration", "narrate" -> NARRATION
                else -> NARRATION
            }
        }
    }
}
