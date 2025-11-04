package com.example.starborn.data.repository

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.World
import com.example.starborn.domain.repository.WorldRepository

/**
 * Temporary repository backed directly by JSON assets.
 * Will be replaced with a caching implementation once persistence is in place.
 */
class AssetWorldRepository(
    private val assetDataSource: WorldAssetDataSource
) : WorldRepository {

    override suspend fun getWorlds(): List<World> = assetDataSource.loadWorlds()

    override suspend fun getHubs(): List<Hub> = assetDataSource.loadHubs()

    override suspend fun getRooms(): List<Room> = assetDataSource.loadRooms()

    override suspend fun getHubNodes(): List<HubNode> = assetDataSource.loadHubNodes()
}
