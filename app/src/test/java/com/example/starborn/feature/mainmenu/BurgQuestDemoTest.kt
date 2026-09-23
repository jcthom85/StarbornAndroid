package com.example.starborn.feature.mainmenu

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.*
import org.junit.Test

class BurgQuestDemoTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)

    @Test fun `showcase catalog is honest and combat comes first`() {
        val demos = DebugScenarioCatalog.burgfestScenarios
        assertEquals("burgfest_combat", demos.first().id)
        assertEquals(4, demos.map { it.id }.distinct().size)
        assertTrue(demos.none { "BURGFEST" in it.title })
        assertTrue(demos.all { BurgQuestDemo.briefing(it.id).isNotBlank() })
    }

    @Test fun `fixed loadouts resolve assets and discard inherited debug grants`() {
        val items = reader.readList<Item>("items.json").map { it.id }.toSet()
        val skills = assets.loadSkills().map { it.id }.toSet()
        val enemies = assets.loadEnemies().map { it.id }.toSet()
        val leveling = LevelingManager(requireNotNull(assets.loadLevelingData()))
        for (id in listOf("burgfest_combat", "burgfest_boss", "burgfest_astra")) {
            val before = GameSessionState(playerCredits = 50_000, playerAp = 99,
                inventory = mapOf("debug_junk" to 999), partyMemberHp = mapOf("nova" to 1),
                equippedItems = mapOf("nova:accessory" to "debug_junk"), unlockedSkills = setOf("debug_skill"))
            val state = BurgQuestDemo.curate(before, id, leveling.levelBounds(BurgQuestDemo.level(id)).first)
            assertEquals(state, BurgQuestDemo.curate(before, id, state.playerXp))
            assertEquals(BurgQuestDemo.party, state.partyMembers)
            assertEquals(state.playerLevel, leveling.levelForXp(state.playerXp))
            assertEquals(0, state.playerCredits)
            assertEquals(0, state.playerAp)
            assertTrue(state.partyMemberHp.isEmpty())
            assertTrue(items.containsAll(state.inventory.keys))
            assertTrue(skills.containsAll(state.unlockedSkills))
            assertTrue(enemies.containsAll(BurgQuestDemo.enemies(id)))
            assertEquals(8, state.unlockedSkills.size)
            assertEquals(4, state.equippedItems.size)
            assertFalse(state.inventory.containsKey("debug_junk"))
        }
    }
}
