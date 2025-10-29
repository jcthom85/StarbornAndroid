package com.example.starborn.data.assets

import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.Npc
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.World

class WorldAssetDataSource(
    private val assetReader: AssetJsonReader
) {

    fun loadWorlds(): List<World> = assetReader.readList("worlds.json")

    fun loadHubs(): List<Hub> = assetReader.readList("hubs.json")

    fun loadRooms(): List<Room> = assetReader.readList("rooms.json")

    fun loadCharacters(): List<Player> = assetReader.readList("characters.json")

    fun loadEnemies(): List<Enemy> = assetReader.readList("enemies.json")

    fun loadNpcs(): List<Npc> = assetReader.readList("npcs.json")

    fun loadSkills(): List<Skill> = assetReader.readList("skills.json")
}
