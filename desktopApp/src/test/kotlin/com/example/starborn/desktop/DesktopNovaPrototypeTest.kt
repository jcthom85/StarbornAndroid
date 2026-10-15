package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.combat.PartyHealth
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.session.ActiveMealBuff
import com.example.starborn.feature.combat.viewmodel.CombatController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopNovaPrototypeTest {
    @get:Rule val compose = createComposeRule()
    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-nova-prototype-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame(novaPrototype = true))
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            runBlocking { withTimeout(10000) { services.exploration.uiState.first { !it.isLoading && it.partyStatus.members.isNotEmpty() } } }
            block(services)
        } finally { services.close(); directory.deleteRecursively() }
    }
    private fun combat(services: DesktopAppServices) = CombatController(services.worldDataSource,
        services.combatEngine, services.statusRegistry, services.sessionStore, services.inventoryService,
        services.itemRepository, services.levelingManager, services.progressionData, services.audioRouter,
        services.themeRepository, services.environmentThemeManager, services.encounterCoordinator,
        listOf("echo_borer"), tutorialsEnabled = false)

    @Test fun menuShowsEffectiveHpAndPublicPurchaseReachesCombat() = withServices { services ->
        services.sessionStore.completeQuest("w1_mq01")
        compose.setContent { DesktopStarbornTheme(services) { Box(Modifier.size(1280.dp, 800.dp)) {
            DesktopFieldMenuContent(services, onOpenFieldKit = {}, onReturnToTitle = {}, onDismiss = {})
        } } }
        compose.waitUntil(10000) { compose.onAllNodesWithText("120 / 120 HP", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Skills").performClick()
        compose.waitUntil(10000) { services.exploration.uiState.value.skillTreeOverlay != null }
        assertEquals(8, services.exploration.uiState.value.skillTreeOverlay!!.branches.sumOf { it.nodes.size })
        compose.onNodeWithText("Unlock (1 AP)").performClick()
        compose.waitUntil(5000) { Nova.GROUNDED in services.sessionStore.state.value.purchasedSkillNodes }
        assertEquals(1, services.sessionStore.state.value.playerAp)
        compose.waitUntil(5000) { services.exploration.uiState.value.skillTreeOverlay?.apInvested == 1 }
        assertEquals(1, services.exploration.uiState.value.skillTreeOverlay!!.apInvested)
        val controller = combat(services)
        try { assertTrue(Nova.GROUNDED in controller.combatState!!.combatants.getValue("nova").combatant.talents) }
        finally { controller.close() }
        val target = File("build/reports/progression-prototype/nova-tree.png").apply { parentFile.mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", target)
    }

    @Test fun gearMealHpAndOwnedSkillPowerAgreeWithActualCombatAndRefreshDetails() = withServices { services ->
        val store = services.sessionStore
        store.completeQuest("w1_mq01"); store.completeQuest("w1_mq03")
        assertTrue(store.purchasePrototypeNode(Nova.GROUNDED))
        assertTrue(store.purchasePrototypeNode(Nova.VENT))
        assertTrue(store.purchasePrototypeNode(Nova.STEPS))
        store.unlockSkill("nova_cryo_vent")
        store.restore(store.state.value.copy(equippedArmors = mapOf("nova" to "nova_helioguard_plate"),
            unlockedArmors = store.state.value.unlockedArmors + "nova_helioguard_plate",
            activeMealBuff = ActiveMealBuff(recipeId = "hp-boundary-fixture", hpBonus = 17), partyMemberHp = mapOf("nova" to 84)))
        val nova = services.characterDefinitions.getValue("nova")
        val expected = PartyHealth.maxHp(nova, store.state.value, services.itemRepository::findItem,
            services.worldDataSource.loadSkillNodes(1))
        assertTrue(expected > 120)
        services.exploration.openPartyMemberDetails("nova")
        runBlocking { withTimeout(10000) { services.exploration.uiState.first { it.partyMemberDetails?.hpLabel == "84 / $expected HP" } } }
        assertEquals("84 / $expected HP", services.exploration.uiState.value.partyStatus.members.single().hpLabel)
        val controller = combat(services)
        try {
            val actor = controller.combatState!!.combatants.getValue("nova")
            assertEquals(expected, actor.combatant.stats.maxHp)
            assertEquals(84, actor.hp)
            assertEquals(65, controller.skillsForPlayer("nova").single { it.id == "nova_cryo_vent" }.basePower)
            assertEquals(12, actor.combatant.stats.speed)
        } finally { controller.close() }
        store.restore(store.state.value.copy(activeMealBuff = null))
        runBlocking { withTimeout(10000) { services.exploration.uiState.first { it.partyMemberDetails?.hpLabel == "84 / ${expected - 17} HP" } } }
    }

    @Test fun serviceNgPlusAndSaveReloadKeepTheActualPurchasedBuild() = withServices { services ->
        val store = services.sessionStore
        store.completeQuest("w1_mq01"); store.completeQuest("w1_mq03")
        assertTrue(store.purchasePrototypeNode(Nova.STEPS))
        assertTrue(store.purchasePrototypeNode(Nova.SCATTER))
        store.unlockSkill("nova_link")
        store.learnSchematic("mod_ergonomic_grip_1")
        store.setMilestone("ms_game_complete")
        val previous = store.state.value
        assertTrue(services.startNewGamePlus())
        assertEquals(previous.playerAp, store.state.value.playerAp)
        assertEquals(previous.purchasedSkillNodes, store.state.value.purchasedSkillNodes)
        assertTrue("nova_link" in store.state.value.unlockedSkills)
        assertTrue("mod_ergonomic_grip_1" in store.state.value.learnedSchematics)
        assertTrue(store.state.value.progressionAwardClaims.isEmpty())
        assertTrue(store.state.value.completedQuests.isEmpty())
        assertTrue(services.saveManager.saveGame(1, store.state.value))
        store.restore(store.state.value.copy(purchasedSkillNodes = emptyMap()))
        assertTrue(services.loadSlot(1))
        assertEquals(previous.purchasedSkillNodes, store.state.value.purchasedSkillNodes)
        val controller = combat(services)
        try { assertTrue(controller.skillsForPlayer("nova").any { it.id == Nova.SCATTER_SKILL }) }
        finally { controller.close() }
    }
}
