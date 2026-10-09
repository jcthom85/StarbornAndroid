package com.example.starborn.domain.cinematic

import com.example.starborn.data.assets.CinematicAssetDataSource

class CinematicService(
    private val dataSource: CinematicAssetDataSource
) {
    private val scenes: Map<String, CinematicScene> by lazy {
        dataSource.loadScenes()
            .associateBy { it.id }
    }

    fun scene(id: String?): CinematicScene? {
        if (id.isNullOrBlank()) return null
        return scenes[id]
    }
}
