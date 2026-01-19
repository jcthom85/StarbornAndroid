package com.example.starborn.data.assets

import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicStep
import com.example.starborn.domain.cinematic.CinematicStepType
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
            durationSeconds = step.durationSeconds,
            emote = step.emote
        )
    }
}

data class CinematicSceneAsset(
    val id: String? = null,
    val title: String? = null,
    val steps: List<CinematicStepAsset>? = null
)

data class CinematicStepAsset(
    val type: String? = null,
    val speaker: String? = null,
    val text: String? = null,
    val line: String? = null,
    val durationSeconds: Double? = null,
    val emote: String? = null
)
