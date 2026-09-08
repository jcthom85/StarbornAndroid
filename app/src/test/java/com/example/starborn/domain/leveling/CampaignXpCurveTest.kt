package com.example.starborn.domain.leveling

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Quest
import org.junit.Assert.*
import org.junit.Test

class CampaignXpCurveTest {
    @Test fun `main quest progression retains later skill tiers for later worlds`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val leveling = LevelingManager(requireNotNull(reader.readObject<LevelingData>("leveling_data.json")))
        val quests = reader.readList<Quest>("quests.json")
        fun xpThrough(world: Int) = quests.filter { quest ->
            (1..world).any { quest.id.startsWith("w${it}_mq") }
        }.flatMap { it.rewards }.filter { it.type == "xp" }.sumOf { it.amount ?: 0 }
        assertTrue("World 3 quests must not unlock the final tier", leveling.levelForXp(xpThrough(3)) < 12)
        assertTrue("World 5 quests must leave a level for World 6", leveling.levelForXp(xpThrough(5)) < 12)
        assertTrue("Final tier must be attainable with scripted battle earnings", leveling.levelForXp(xpThrough(6) + 9695) == 12)
        assertTrue("First level up should remain accessible in the opening", leveling.levelForXp(100) >= 2)
    }
}
