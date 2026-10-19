package com.example.starborn.domain.session

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import org.junit.Assert.*
import org.junit.Test

/** Budget/acquisition contract; controller-route tests separately prove gameplay reachability. */
class CrewProgressionPacingTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))

    @Test fun coreCampaignAndRequiredTrainingPayForOneSpecializationPerCrewBeforeTheFinale() {
        val enemies = assets.loadEnemies().associateBy { it.id }
        val store = GameSessionStore().apply { restore(GameBootstrap.newGame(assets.loadCharacters(), emptyList())) }
        val checkpoints = listOf(
            listOf("w1_mq01", "w1_sq03", "w1_mq03", "w1_mq05"),
            listOf("w2_mq05"),
            listOf("w3_mq11", "w3_mq13", "w3_mq14", "w3_mq15"),
            listOf("w4_mq16", "w4_mq18", "w4_mq19"),
            listOf("w5_mq22", "w5_mq24"),
            listOf("w6_mq26", "w6_mq28", "w6_mq29")
        )
        // Boggs gates the mine on w1_sq03 despite its side-quest ID; its 2 AP is compulsory.
        val totals = listOf(8, 16, 26, 36, 44, 50)
        val ownerBudgets = listOf(listOf(3, 3, 0, 0), listOf(3, 3, 3, 3), listOf(6, 6, 6, 6),
            listOf(10, 10, 6, 6), listOf(10, 10, 10, 10), listOf(13, 11, 11, 11))
        val bosses = mapOf(2 to "administrator_boss", 3 to "titan_walker_boss", 4 to "compliance_avatar")
        checkpoints.forEachIndexed { world, quests ->
            quests.forEach { store.completeQuest(it); store.completeQuest(it) }
            if (world == 0) store.addPartyMember("zeke")
            if (world == 1) repeat(2) { store.addPartyMember("orion"); store.addPartyMember("gh0st") }
            bosses[world]?.let { store.addAp(requireNotNull(enemies.getValue(it).apReward)) }
            if (world == 3) store.setMilestone("ms_w4_mq20_complete")
            assertEquals("World ${world + 1} total AP", totals[world],
                store.state.value.playerAp + store.state.value.purchasedSkillNodes.values.sum())
            Crew.trees.forEachIndexed { index, tree ->
                var spent = store.state.value.purchasedSkillNodes.filterKeys { Crew.owners[it] == tree.character }.values.sum()
                val target = ownerBudgets[world][index]
                for (node in tree.branches.values.flatten()) {
                    if (node.id in store.state.value.purchasedSkillNodes || spent + node.costAp > target) continue
                    assertTrue("World ${world + 1}: ${tree.character} can afford ${node.id}", store.purchasePrototypeNode(node.id))
                    spent += node.costAp
                }
                assertEquals(target, spent)
            }
        }
        assertEquals(4, store.state.value.playerAp)
        assertTrue("Main-route choices remain meaningful", store.state.value.purchasedSkillNodes.size < Crew.nodes.size)
        assertEquals(80, Crew.nodes.values.sumOf { it.costAp })
    }

    @Test fun remainingSideQuestAwardsAddTenApWithoutTurningRepeatableCombatIntoApFarming() {
        val store = GameSessionStore().apply { restore(GameBootstrap.newGame(assets.loadCharacters(), emptyList())) }
        val sideQuests = listOf("w2_sq01", "w3_sq12", "w4_sq19", "w5_sq22", "w6_sq26")
        sideQuests.forEach { store.completeQuest(it); store.completeQuest(it) }
        assertEquals(10, store.state.value.playerAp)
        assertEquals(sideQuests.toSet(), store.state.value.progressionAwardClaims)
        assertTrue(assets.loadEnemies().filter { (it.apReward ?: 0) > 0 }.all { it.tier == "boss" })
    }
}
