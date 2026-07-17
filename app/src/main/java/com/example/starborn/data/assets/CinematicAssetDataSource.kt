package com.example.starborn.data.assets

import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicBackdrop
import com.example.starborn.domain.cinematic.CinematicStep
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.domain.cinematic.CinematicCameraMotion
import com.example.starborn.domain.cinematic.CinematicCaptionStyle
import com.example.starborn.domain.cinematic.CinematicPresentation
import com.example.starborn.domain.cinematic.CinematicTransition
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class CinematicAssetDataSource(
    private val reader: AssetJsonReader,
    private val moshi: Moshi
) {
    fun loadScenes(): List<CinematicScene> {
        val type = Types.newParameterizedType(
            List::class.java,
            CinematicSceneAsset::class.java
        )
        val assets = reader.read<List<CinematicSceneAsset>>("cinematics.json", type) ?: emptyList()
        return assets.map(::toDomainScene)
    }

    private fun toDomainScene(asset: CinematicSceneAsset): CinematicScene {
        return CinematicScene(
            id = asset.id.orEmpty(),
            title = asset.title,
            backdrop = CinematicBackdrop.fromRaw(asset.backdrop),
            presentation = CinematicPresentation.fromRaw(asset.presentation),
            ambientCue = asset.ambientCue,
            skippable = asset.skippable ?: false,
            steps = asset.steps.orEmpty()
                .mapNotNull { step -> toDomainStep(step) }
        )
    }

    private fun toDomainStep(step: CinematicStepAsset?): CinematicStep? {
        if (step == null) return null
        val text = step.text ?: step.line ?: return null
        val type = CinematicStepType.fromRaw(step.type)
        return CinematicStep(
            type = type,
            speaker = step.speaker,
            text = text,
            portrait = step.portrait,
            durationSeconds = step.durationSeconds,
            emote = step.emote,
            imagePath = step.imagePath,
            cameraMotion = CinematicCameraMotion.fromRaw(step.cameraMotion),
            cameraStartScale = step.cameraStartScale,
            cameraEndScale = step.cameraEndScale,
            cameraStartX = step.cameraStartX,
            cameraEndX = step.cameraEndX,
            cameraStartY = step.cameraStartY,
            cameraEndY = step.cameraEndY,
            transition = CinematicTransition.fromRaw(step.transition),
            audioCue = step.audioCue,
            voiceCue = step.voiceCue,
            captionStyle = CinematicCaptionStyle.fromRaw(step.captionStyle, type)
        )
    }
}

data class CinematicSceneAsset(
    val id: String? = null,
    val title: String? = null,
    val backdrop: String? = null,
    val presentation: String? = null,
    @Json(name = "ambient_cue") val ambientCue: String? = null,
    val skippable: Boolean? = null,
    val steps: List<CinematicStepAsset>? = null
)

data class CinematicStepAsset(
    val type: String? = null,
    val speaker: String? = null,
    val text: String? = null,
    val line: String? = null,
    val portrait: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Double? = null,
    val emote: String? = null,
    @Json(name = "image") val imagePath: String? = null,
    @Json(name = "camera_motion") val cameraMotion: String? = null,
    @Json(name = "camera_start_scale") val cameraStartScale: Double? = null,
    @Json(name = "camera_end_scale") val cameraEndScale: Double? = null,
    @Json(name = "camera_start_x") val cameraStartX: Double? = null,
    @Json(name = "camera_end_x") val cameraEndX: Double? = null,
    @Json(name = "camera_start_y") val cameraStartY: Double? = null,
    @Json(name = "camera_end_y") val cameraEndY: Double? = null,
    val transition: String? = null,
    @Json(name = "audio_cue") val audioCue: String? = null,
    @Json(name = "voice_cue") val voiceCue: String? = null,
    @Json(name = "caption_style") val captionStyle: String? = null
)
