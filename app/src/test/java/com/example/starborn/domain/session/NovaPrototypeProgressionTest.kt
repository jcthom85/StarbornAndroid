package com.example.starborn.domain.session

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class NovaPrototypeProgressionTest {
    @Test fun earnedAwardsAndPurchasesAreIdempotentAndFreeSkillsAreSeparate() {
        val store = GameSessionStore().apply { restore(GameSessionState(progressionPrototypeVersion = 1)) }
        assertFalse(store.purchasePrototypeNode(Nova.GROUNDED))
        store.completeQuest("w1_mq01")
        store.completeQuest("w1_mq01")
        assertEquals(2, store.state.value.playerAp)
        store.unlockSkill("nova_cryo_vent")
        assertFalse(store.purchasePrototypeNode(Nova.VENT))
        assertTrue(store.purchasePrototypeNode(Nova.GROUNDED))
        assertFalse(store.purchasePrototypeNode(Nova.GROUNDED))
        store.completeQuest("w1_mq03")
        assertTrue(store.purchasePrototypeNode(Nova.VENT))
        assertEquals(mapOf(Nova.GROUNDED to 1, Nova.VENT to 2), store.state.value.purchasedSkillNodes)
        assertEquals(setOf("nova_cryo_vent"), store.state.value.unlockedSkills)
        assertEquals(1, store.state.value.playerAp)
        assertFalse(store.purchasePrototypeNode("nova_light_fingers"))
    }

    @Test fun capstonesRequireFoundryAndNormalSessionsCannotBuyPrototypeNodes() {
        val store = GameSessionStore().apply { restore(GameSessionState(progressionPrototypeVersion = 1, playerAp = 20)) }
        listOf(Nova.STEPS, Nova.SCATTER, Nova.START).forEach { assertTrue(store.purchasePrototypeNode(it)) }
        assertFalse(store.purchasePrototypeNode(Nova.MOVING))
        store.setMilestone("ms_w4_mq20_complete")
        assertTrue(store.purchasePrototypeNode(Nova.MOVING))
        val normal = GameSessionStore().apply { addAp(20); completeQuest("w1_mq01") }
        assertEquals(20, normal.state.value.playerAp)
        assertFalse(normal.purchasePrototypeNode(Nova.STEPS))
    }

    @Test fun recruitmentClaimsAwardOnceWithoutPayingForPartyRestoration() {
        val store = GameSessionStore().apply { restore(GameSessionState(progressionPrototypeVersion = 1)) }
        store.setPartyMembers(listOf("nova", "zeke"))
        assertEquals(0, store.state.value.playerAp)
        repeat(2) { store.addPartyMember("orion"); store.addPartyMember("gh0st") }
        assertEquals(4, store.state.value.playerAp)
        assertEquals(setOf("recruit:orion", "recruit:gh0st"), store.state.value.progressionAwardClaims)
    }

    @Test fun actualProtobufAndNgPlusPreserveBuildAndResetCampaignClaims() = runBlocking {
        val folder = Files.createTempDirectory("starborn-prototype-save-").toFile()
        try {
            val store = GameSessionStore().apply { restore(GameSessionState(progressionPrototypeVersion = 1, playerAp = 10)) }
            store.completeQuest("w1_mq01")
            assertTrue(store.purchasePrototypeNode(Nova.STEPS))
            assertTrue(store.purchasePrototypeNode(Nova.SCATTER))
            store.unlockSkill("nova_link")
            store.learnSchematic("mod_ergonomic_grip_1")
            val state = store.state.value
            val persistence = GameSessionPersistence(folder)
            persistence.writeSlot(1, state)
            val saved = requireNotNull(persistence.readSlot(1))
            assertEquals(state.purchasedSkillNodes, saved.purchasedSkillNodes)
            assertEquals(state.progressionAwardClaims, saved.progressionAwardClaims)
            assertEquals(state.playerAp, saved.playerAp)
            val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
            val plus = GameBootstrap.newGamePlus(assets.loadCharacters(), emptyList(), saved)
            assertEquals(saved.purchasedSkillNodes, plus.purchasedSkillNodes)
            assertEquals(saved.unlockedSkills, plus.unlockedSkills)
            assertEquals(saved.learnedSchematics, plus.learnedSchematics)
            assertEquals(saved.playerAp, plus.playerAp)
            assertEquals(1, plus.progressionPrototypeVersion)
            assertTrue(plus.progressionAwardClaims.isEmpty())
            assertTrue(plus.completedQuests.isEmpty())
        } finally { folder.deleteRecursively() }
    }
}
