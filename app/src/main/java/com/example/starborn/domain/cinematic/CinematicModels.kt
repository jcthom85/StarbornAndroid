package com.example.starborn.domain.cinematic

data class CinematicScene(
    val id: String,
    val title: String? = null,
    val backdrop: CinematicBackdrop = CinematicBackdrop.ROOM,
    val steps: List<CinematicStep> = emptyList()
)

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
    val durationSeconds: Double? = null,
    val emote: String? = null
)

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
