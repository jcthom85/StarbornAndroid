package com.example.starborn.domain.session

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class CrewProgressionMigrationTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private fun legacy() = GameSessionState(worldId = "world_1", playerId = "nova", partyMembers = listOf("nova"))

    @Test fun frozenCompatibilityTableMatchesHistoricalCatalogAndKeepsRealAbilities() {
        val historical = assets.loadSkillTrees(0).filter { it.character in setOf("nova", "zeke", "orion", "gh0st") }
            .flatMap { it.branches.values.flatten() }.associate { it.id to it.costAp }
        assertEquals(100, historical.size)
        assertEquals(224, historical.values.sum())
        assertEquals(historical, LegacyCrewProgression.nodeCosts)
        assertEquals(historical.keys.intersect(assets.loadSkills(0).map { it.id }.toSet()), LegacyCrewProgression.retainedCombatSkills)
        val changed = legacy().copy(unlockedSkills = historical.keys).migrateCrewProgressionState()
        assertEquals(224, changed.playerAp)
        assertEquals(LegacyCrewProgression.retainedCombatSkills, changed.unlockedSkills)
        assertTrue(changed.purchasedSkillNodes.isEmpty())
        assertEquals(changed, changed.migrateCrewProgressionState())
    }

    @Test fun compensationPreservesCoreSkillsAndBackfillsOnlyProvenNewAwards() {
        val before = legacy().copy(playerAp = 7, playerCredits = 123,
            partyMembers = listOf("nova", "zeke", "orion", "gh0st"),
            unlockedSkills = setOf("nova_light_fingers", "nova_smoke_bomb", "gh0st_headshot", "nova_link"),
            completedQuests = setOf("w1_mq01", "w1_mq03"), activeQuests = setOf("w1_mq04"),
            learnedSchematics = setOf("known_recipe"), inventory = mapOf("medkit" to 3))
        val changed = before.migrateCrewProgressionState()
        assertEquals(20, changed.playerAp) // 7 unused + 5 respec credit + 4 quest AP + 4 recruitment AP.
        assertEquals(setOf("nova_smoke_bomb", "gh0st_headshot", "nova_link", Crew.PATCH_SKILL), changed.unlockedSkills)
        assertEquals(setOf("w1_mq01", "w1_mq03", "recruit:orion", "recruit:gh0st"), changed.progressionAwardClaims)
        assertEquals(before.inventory, changed.inventory)
        assertEquals(before.learnedSchematics, changed.learnedSchematics)
        assertEquals(before.completedQuests, changed.completedQuests)
        assertEquals(before.activeQuests, changed.activeQuests)
        assertEquals(before.playerCredits, changed.playerCredits)
        assertEquals(changed, changed.migrateCrewProgressionState())
    }

    @Test fun completedRecruitmentSurvivesTemporaryPartySeparationWithoutInventingARecruit() {
        val changed = legacy().copy(questTasksCompleted = mapOf("w2_mq03" to setOf("talk_to_orion")))
            .migrateCrewProgressionState()
        assertEquals(2, changed.playerAp)
        assertEquals(listOf("nova"), changed.partyMembers)
        assertEquals(setOf("recruit:orion"), changed.progressionAwardClaims)
        assertTrue(Crew.PATCH_SKILL in changed.unlockedSkills)
        val unrecruited = legacy().copy(activeQuests = setOf("w2_mq03"),
            questTasksCompleted = mapOf("w2_mq03" to setOf("align_stasis_rings"))).migrateCrewProgressionState()
        assertEquals(0, unrecruited.playerAp)
        assertFalse(Crew.PATCH_SKILL in unrecruited.unlockedSkills)
    }

    @Test fun previousClaimsPrototypeReceiptsAndFutureVersionsAreNotPaidAgain() {
        val claimed = legacy().copy(completedQuests = setOf("w1_mq01"), progressionAwardClaims = setOf("w1_mq01"))
            .migrateCrewProgressionState()
        assertEquals(0, claimed.playerAp)
        val current = legacy().copy(progressionPrototypeVersion = Crew.VERSION, playerAp = 4,
            purchasedSkillNodes = mapOf(Nova.GROUNDED to 1), completedQuests = setOf("w1_mq01"))
        assertSame(current, current.migrateCrewProgressionState())
        val future = current.copy(progressionPrototypeVersion = 99)
        assertSame(future, future.migrateCrewProgressionState())
        val empty = GameSessionState()
        assertSame(empty, empty.migrateCrewProgressionState())
    }

    @Test fun largeCreditBalanceDoesNotOverflowDuringCompensationOrBackfill() {
        val changed = legacy().copy(playerAp = Int.MAX_VALUE, unlockedSkills = setOf("nova_light_fingers"),
            completedQuests = setOf("w1_mq01"), partyMembers = listOf("nova", "orion"))
            .migrateCrewProgressionState()
        assertEquals(Int.MAX_VALUE, changed.playerAp)
        assertEquals(setOf("w1_mq01", "recruit:orion"), changed.progressionAwardClaims)
        val store = GameSessionStore().apply { restore(changed) }
        assertTrue(store.purchasePrototypeNode(Nova.STEPS))
        store.addAp(8)
        assertEquals(Int.MAX_VALUE, store.state.value.playerAp)
        assertTrue(store.resetPrototypeBuild("nova"))
        assertEquals(Int.MAX_VALUE, store.state.value.playerAp)
    }

    @Test fun battleRollbackAndEverySaveKindKeepMigrationIdempotent() = runBlocking {
        val folder = Files.createTempDirectory("starborn-crew-migration-").toFile()
        try {
            val before = legacy().copy(playerAp = 5, unlockedSkills = setOf("nova_light_fingers"),
                completedQuests = setOf("w1_mq01"), pendingBattleJson = "pending")
            val live = before.copy(playerAp = 50, battleCheckpoint = before).migrateCrewProgressionState()
            val checkpoint = requireNotNull(live.battleCheckpoint)
            assertEquals(8, checkpoint.playerAp)
            assertEquals(Crew.VERSION, checkpoint.progressionPrototypeVersion)
            assertEquals(live, live.migrateCrewProgressionState())
            val disk = GameSessionPersistence(folder)
            disk.persist(live)
            disk.writeSlot(1, live)
            disk.writeAutosave(live)
            disk.writeQuickSave(live)
            val saved = listOf(disk.readSlot(1), disk.readAutosave(), disk.readQuickSave())
            saved.forEach { value ->
                assertEquals(checkpoint, value)
                assertEquals(checkpoint, requireNotNull(value).migrateCrewProgressionState())
            }
            // A later overwrite and damaged primary exercise the retained backup loader too.
            disk.writeSlot(1, checkpoint)
            folder.resolve("game_session_slot1.pb").writeBytes(byteArrayOf(0))
            assertEquals(checkpoint, disk.readSlot(1)?.migrateCrewProgressionState())
        } finally { folder.deleteRecursively() }
    }

    @Test fun ngPlusMigratesOnceAndCarriesBuildButStartsNewCampaignClaims() {
        val old = legacy().copy(playerAp = 5, unlockedSkills = setOf("nova_light_fingers", "nova_smoke_bomb"),
            completedQuests = setOf("w1_mq01"), learnedSchematics = setOf("known_recipe"))
        val plus = GameBootstrap.newGamePlus(assets.loadCharacters(), emptyList(), old)
        assertEquals(Crew.VERSION, plus.progressionPrototypeVersion)
        assertEquals(11, plus.playerAp)
        assertEquals(setOf("nova_smoke_bomb"), plus.unlockedSkills)
        assertEquals(old.learnedSchematics, plus.learnedSchematics)
        assertTrue(plus.progressionAwardClaims.isEmpty())
        assertEquals(plus, plus.migrateCrewProgressionState())
        val store = GameSessionStore().apply { restore(plus) }
        store.completeQuest("w1_mq01")
        store.completeQuest("w1_mq01")
        assertEquals(13, store.state.value.playerAp)
    }

    @Test fun freshGamesUseOnlyTheFourCrewTreesWithNoSeededApOrPurchases() {
        val fresh = GameBootstrap.newGame(assets.loadCharacters(), emptyList())
        assertEquals(Crew.VERSION, fresh.progressionPrototypeVersion)
        assertEquals(0, fresh.playerAp)
        assertTrue(fresh.purchasedSkillNodes.isEmpty())
        assertTrue(fresh.progressionAwardClaims.isEmpty())
        assertEquals(setOf("nova", "zeke", "orion", "gh0st"), assets.loadSkillTrees(fresh.progressionPrototypeVersion).map { it.character }.toSet())
        assertEquals(32, assets.loadSkillNodes(fresh.progressionPrototypeVersion).size)
    }
}
