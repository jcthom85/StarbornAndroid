package com.example.starborn.data.assets

import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.Npc
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.SkillTreeDefinition
import com.example.starborn.domain.model.SkillTreeNode
import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.model.World
import com.example.starborn.domain.leveling.LevelingData
import com.example.starborn.domain.leveling.ProgressionData

class WorldAssetDataSource(
    private val assetReader: AssetJsonReader
) {

    fun loadWorlds(): List<World> = assetReader.readList("worlds.json")

    fun loadHubs(): List<Hub> = assetReader.readList("hubs.json")
    fun loadHubNodes(): List<HubNode> = assetReader.readList("hub_nodes.json")

    fun loadRooms(): List<Room> = assetReader.readList("rooms.json")

    fun loadCharacters(): List<Player> = assetReader.readList("characters.json")

    fun loadEnemies(): List<Enemy> = assetReader.readList("enemies.json")

    fun loadNpcs(): List<Npc> = assetReader.readList("npcs.json")

    fun loadSkills(): List<Skill> = assetReader.readList("skills.json")

    fun loadStatuses(): List<StatusDefinition> = assetReader.readList("statuses.json")

    fun loadLevelingData(): LevelingData? = assetReader.readObject("leveling_data.json")

    fun loadProgressionData(): ProgressionData? = assetReader.readObject("progression.json")

    fun loadSkillTrees(): List<SkillTreeDefinition> {
        val assetManager = assetReader.context.assets
        val files = runCatching { assetManager.list(SKILL_TREE_DIR) }.getOrNull().orEmpty()
        if (files.isEmpty()) return emptyList()
        val adapter = assetReader.moshi.adapter(SkillTreeDefinition::class.java)
        val trees = mutableListOf<SkillTreeDefinition>()
        files.filter { it.endsWith(".json") }.forEach { name ->
            val path = "$SKILL_TREE_DIR/$name"
            val definition = runCatching {
                assetManager.open(path).bufferedReader().use { reader ->
                    adapter.fromJson(reader.readText())
                }
            }.getOrNull()
            if (definition != null) {
                trees += definition
            }
        }
        return trees
    }

    fun loadSkillNodes(): Map<String, SkillTreeNode> =
        loadSkillTrees()
            .flatMap { tree -> tree.branches.values.flatten() }
            .associateBy { node -> node.id }

    fun missingHubAssets(hubs: List<Hub>, nodes: List<HubNode>): List<String> {
        val missing = mutableSetOf<String>()
        hubs.mapNotNull { it.backgroundImage.takeIf { path -> path.isNotBlank() } }
            .forEach { path -> if (!assetReader.assetExists(path)) missing += path }
        nodes.mapNotNull { it.iconImage?.takeIf { path -> path.isNotBlank() } }
            .forEach { path -> if (!assetReader.assetExists(path)) missing += path }
        return missing.toList()
    }

    companion object {
        private const val SKILL_TREE_DIR = "skill_trees"
    }
}
