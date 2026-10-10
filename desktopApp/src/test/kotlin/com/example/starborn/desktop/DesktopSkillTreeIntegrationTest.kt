package com.example.starborn.desktop

import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.combat.viewmodel.CombatController
import com.example.starborn.feature.exploration.skilltree.evaluateNodeStatus
import com.example.starborn.feature.exploration.viewmodel.SkillNodeCategory
import com.example.starborn.feature.exploration.viewmodel.buildSkillTreeOverlayUi
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopSkillTreeIntegrationTest {

    @Test
    fun characterTreesExposeKeystoneCombatAbilitiesAndCorrectCategories() {
        val directory = Files.createTempDirectory("starborn-skill-test-").toFile()
        val services = DesktopAppServices(directory)
        try {
            val assets = services.worldDataSource
            val trees = assets.loadSkillTrees().associateBy { it.character }
            val characters = assets.loadCharacters().associateBy { it.id }
            val skillsById = services.skillDefinitions

            // 1. Nova Tree Check
            val novaTree = requireNotNull(trees["nova"])
            val novaOverlay = requireNotNull(
                buildSkillTreeOverlayUi(
                    tree = novaTree,
                    character = characters["nova"],
                    sessionState = GameSessionState(playerId = "nova", playerAp = 10),
                    skillsById = skillsById
                )
            )
            val novaNodes = novaOverlay.branches.flatMap { it.nodes }.associateBy { it.id }

            // Verify Keystone nodes exist and have activeCombatSkill metadata
            val hydraulicKick = requireNotNull(novaNodes["nova_hydraulic_kick"])
            assertEquals(SkillNodeCategory.KEYSTONE, hydraulicKick.category)
            assertNotNull(hydraulicKick.activeCombatSkill)
            assertEquals("Hydraulic Kick", hydraulicKick.activeCombatSkill?.name)
            assertTrue(hydraulicKick.activeCombatSkill?.basePower ?: 0 > 0)

            val smokeBomb = requireNotNull(novaNodes["nova_smoke_bomb"])
            assertEquals(SkillNodeCategory.KEYSTONE, smokeBomb.category)
            assertNotNull(smokeBomb.activeCombatSkill)

            val quietSteps = requireNotNull(novaNodes["nova_quiet_steps"])
            assertEquals(SkillNodeCategory.STAT, quietSteps.category)
            assertNull(quietSteps.activeCombatSkill)

            // 2. Zeke Tree Check
            val zekeTree = requireNotNull(trees["zeke"])
            val zekeOverlay = requireNotNull(
                buildSkillTreeOverlayUi(
                    tree = zekeTree,
                    character = characters["zeke"],
                    sessionState = GameSessionState(playerId = "zeke", playerAp = 10),
                    skillsById = skillsById
                )
            )
            val zekeNodes = zekeOverlay.branches.flatMap { it.nodes }.associateBy { it.id }

            val bulwarkStance = requireNotNull(zekeNodes["zeke_bulwark_stance"])
            assertEquals(SkillNodeCategory.KEYSTONE, bulwarkStance.category)
            assertNotNull(bulwarkStance.activeCombatSkill)

            val overloadFists = requireNotNull(zekeNodes["zeke_overload_fists"])
            assertEquals(SkillNodeCategory.KEYSTONE, overloadFists.category)
            assertNotNull(overloadFists.activeCombatSkill)

            val quakeSlam = requireNotNull(zekeNodes["zeke_quake_slam"])
            assertEquals(SkillNodeCategory.KEYSTONE, quakeSlam.category)
            assertNotNull(quakeSlam.activeCombatSkill)

            val trainingSession = requireNotNull(zekeNodes["zeke_training_session"])
            assertEquals(SkillNodeCategory.STAT, trainingSession.category)

            // 3. Orion & Gh0st Trees Check
            val orionTree = requireNotNull(trees["orion"])
            val orionOverlay = requireNotNull(
                buildSkillTreeOverlayUi(
                    tree = orionTree,
                    character = characters["orion"],
                    sessionState = GameSessionState(playerId = "orion", playerAp = 10),
                    skillsById = skillsById
                )
            )
            val orionNodes = orionOverlay.branches.flatMap { it.nodes }.associateBy { it.id }
            assertEquals(SkillNodeCategory.KEYSTONE, orionNodes["orion_nano_repair"]?.category)
            assertEquals(SkillNodeCategory.KEYSTONE, orionNodes["orion_disruption_pulse"]?.category)
            assertEquals(SkillNodeCategory.KEYSTONE, orionNodes["orion_stasis_field"]?.category)

            val gh0stTree = requireNotNull(trees["gh0st"])
            val gh0stOverlay = requireNotNull(
                buildSkillTreeOverlayUi(
                    tree = gh0stTree,
                    character = characters["gh0st"],
                    sessionState = GameSessionState(playerId = "gh0st", playerAp = 10),
                    skillsById = skillsById
                )
            )
            val gh0stNodes = gh0stOverlay.branches.flatMap { it.nodes }.associateBy { it.id }
            assertEquals(SkillNodeCategory.KEYSTONE, gh0stNodes["gh0st_headshot"]?.category)
            assertEquals(SkillNodeCategory.KEYSTONE, gh0stNodes["gh0st_venom_edge"]?.category)
            assertEquals(SkillNodeCategory.KEYSTONE, gh0stNodes["gh0st_system_crash"]?.category)
            assertEquals(SkillNodeCategory.KEYSTONE, gh0stNodes["gh0st_shadow_flurry"]?.category)
            assertEquals(SkillNodeCategory.STAT, gh0stNodes["gh0st_scope"]?.category)
        } finally {
            services.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun regressionRootNodesRemainAccessibleAndPurchasableAtRootTier() {
        val directory = Files.createTempDirectory("starborn-root-test-").toFile()
        val services = DesktopAppServices(directory)
        try {
            val assets = services.worldDataSource
            val trees = assets.loadSkillTrees()

            val regressionNodeIds = listOf(
                "nova_quiet_steps",
                "nova_night_cloak",
                "zeke_training_session",
                "zeke_motivational_speech",
                "zeke_budgeting",
                "gh0st_scope"
            )

            for (nodeId in regressionNodeIds) {
                val tree = trees.single { it.branches.values.flatten().any { n -> n.id == nodeId } }
                val node = tree.branches.values.flatten().single { it.id == nodeId }
                val status = evaluateNodeStatus(
                    node = node,
                    tree = tree,
                    unlockedSkills = emptySet(),
                    completedMilestones = emptySet(),
                    availableAp = 1,
                    apInvested = 0
                )
                assertTrue("Node $nodeId must be purchasable at root with 1 AP", status.canPurchase)
                assertTrue("Node $nodeId must meet tier requirement", status.meetsTierRequirement)
                assertTrue("Node $nodeId must have unmet requirements empty", status.unmetRequirements.isEmpty())
            }
        } finally {
            services.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun unlockingKeystoneInSessionExposesMoveImmediatelyInCombatRuntime() {
        val directory = Files.createTempDirectory("starborn-combat-skill-test-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            val sessionStore = services.sessionStore

            // Before purchase: Nova has only starting skill arc tether
            val combatBefore = CombatController(
                worldAssets = services.worldDataSource,
                combatEngine = services.combatEngine,
                statusRegistry = services.statusRegistry,
                sessionStore = sessionStore,
                inventoryService = services.inventoryService,
                itemCatalog = services.itemRepository,
                levelingManager = services.levelingManager,
                progressionData = services.progressionData,
                audioRouter = services.audioRouter,
                themeRepository = services.themeRepository,
                environmentThemeManager = services.environmentThemeManager,
                encounterCoordinator = services.encounterCoordinator,
                enemyIds = listOf("faulted_loader")
            )
            try {
                val novaSkillsBefore = combatBefore.skillsForPlayer("nova").map { it.id }.toSet()
                assertEquals(setOf("nova_arc_tether"), novaSkillsBefore)
                assertFalse("nova_hydraulic_kick" in novaSkillsBefore)
            } finally {
                combatBefore.close()
            }

            // Grant 10 AP and unlock hydraulic kick in session
            sessionStore.addAp(10)
            assertTrue(sessionStore.spendAp(2))
            sessionStore.unlockSkill("nova_hydraulic_kick")

            assertTrue("nova_hydraulic_kick" in sessionStore.state.value.unlockedSkills)

            // After purchase: Nova now immediately has Hydraulic Kick in combat!
            val combatAfter = CombatController(
                worldAssets = services.worldDataSource,
                combatEngine = services.combatEngine,
                statusRegistry = services.statusRegistry,
                sessionStore = sessionStore,
                inventoryService = services.inventoryService,
                itemCatalog = services.itemRepository,
                levelingManager = services.levelingManager,
                progressionData = services.progressionData,
                audioRouter = services.audioRouter,
                themeRepository = services.themeRepository,
                environmentThemeManager = services.environmentThemeManager,
                encounterCoordinator = services.encounterCoordinator,
                enemyIds = listOf("faulted_loader")
            )
            try {
                val novaSkillsAfter = combatAfter.skillsForPlayer("nova").map { it.id }.toSet()
                assertTrue("nova_arc_tether" in novaSkillsAfter)
                assertTrue("nova_hydraulic_kick" in novaSkillsAfter)
                val kick = combatAfter.skillsForPlayer("nova").single { it.id == "nova_hydraulic_kick" }
                assertEquals("Hydraulic Kick", kick.name)
                assertEquals(90, kick.basePower)
            } finally {
                combatAfter.close()
            }
        } finally {
            services.close()
            directory.deleteRecursively()
        }
    }
}
