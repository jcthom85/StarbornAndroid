package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import com.example.starborn.domain.combat.*
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

class DesktopCrewBuildTest {
    @get:Rule val compose = createComposeRule()
    private fun withServices(block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-crew-builds-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            assertTrue(services.startNewGame(novaPrototype = true))
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            runBlocking { withTimeout(10000) { services.exploration.uiState.first { !it.isLoading } } }
            block(services)
        } finally { services.close(); directory.deleteRecursively() }
    }
    private fun combat(s: DesktopAppServices) = CombatController(s.worldDataSource, s.combatEngine,
        s.statusRegistry, s.sessionStore, s.inventoryService, s.itemRepository, s.levelingManager,
        s.progressionData, s.audioRouter, s.themeRepository, s.environmentThemeManager,
        s.encounterCoordinator, listOf("the_iron_warden"), tutorialsEnabled = false)
    private fun waitFor(test: () -> Boolean) = compose.waitUntil(10000, test)
    private fun purchase(s: DesktopAppServices, owner: String, nodes: List<String>) {
        s.exploration.openSkillTree(owner)
        waitFor { s.exploration.uiState.value.skillTreeOverlay?.characterId == owner }
        assertEquals(8, s.exploration.uiState.value.skillTreeOverlay!!.branches.sumOf { it.nodes.size })
        nodes.forEach { id ->
            s.exploration.unlockSkillNode(id)
            waitFor { id in s.sessionStore.state.value.purchasedSkillNodes }
        }
    }

    @Test fun everyPublicTreePurchasesRealStatsSkillsAndResetsOnlyThatCharactersReceipts() = withServices { s ->
        val store = s.sessionStore
        store.restore(store.state.value.copy(partyMembers = listOf("nova", "zeke", "orion", "gh0st"), playerAp = 80,
            completedMilestones = setOf("ms_w4_mq20_complete"), unlockedSkills = store.state.value.unlockedSkills + "gh0st_headshot"))
        val baseline = combat(s)
        val baseStats = try { baseline.combatState!!.combatants.filterKeys { it in store.state.value.partyMembers }.mapValues { it.value.combatant.stats } }
            finally { baseline.close() }
        Crew.trees.forEach { tree -> purchase(s, tree.character, tree.branches.values.flatten().map { it.id }) }
        assertEquals(32, store.state.value.purchasedSkillNodes.size)
        assertEquals(0, store.state.value.playerAp)
        val bought = combat(s)
        try {
            val states = bought.combatState!!.combatants
            assertEquals(baseStats.getValue("nova").speed + 1, states.getValue("nova").combatant.stats.speed)
            assertEquals(baseStats.getValue("zeke").strength + 1, states.getValue("zeke").combatant.stats.strength)
            assertEquals(baseStats.getValue("zeke").vitality + 1, states.getValue("zeke").combatant.stats.vitality)
            assertEquals(baseStats.getValue("zeke").maxHp + 10, states.getValue("zeke").combatant.stats.maxHp)
            assertEquals(baseStats.getValue("orion").vitality + 1, states.getValue("orion").combatant.stats.vitality)
            assertEquals(baseStats.getValue("orion").maxHp + 10, states.getValue("orion").combatant.stats.maxHp)
            assertEquals(baseStats.getValue("orion").focus + 1, states.getValue("orion").combatant.stats.focus)
            assertEquals(baseStats.getValue("gh0st").agility + 1, states.getValue("gh0st").combatant.stats.agility)
            for (owner in store.state.value.partyMembers) {
                assertEquals(8, states.getValue(owner).combatant.talents.size)
                val expected = Crew.activeSkills(store.state.value.purchasedSkillNodes).filter { id -> Crew.skills.any { it.id == id && it.character == owner } }
                assertTrue(bought.skillsForPlayer(owner).map { it.id }.containsAll(expected))
                assertFalse(bought.skillsForPlayer(owner).any { it.character != owner && it.character in store.state.value.partyMembers })
            }
            assertEquals("Field Patch", bought.skillsForPlayer("orion").single { it.id == Crew.PATCH_SKILL }.name)
        } finally { bought.close() }
        compose.setContent { DesktopStarbornTheme(s) { Box(Modifier.size(1280.dp, 800.dp)) {
            DesktopFieldMenuContent(s, onOpenFieldKit = {}, onReturnToTitle = {}, onDismiss = {})
        } } }
        waitFor { s.exploration.uiState.value.skillTreeOverlay?.respecRefund == 20 }
        screenshot("gh0st-tree")
        compose.onNodeWithText("Reset build (+20 AP)").performClick()
        waitFor { store.state.value.purchasedSkillNodes.size == 24 }
        assertEquals(20, store.state.value.playerAp)
        assertTrue("gh0st_headshot" in store.state.value.unlockedSkills)
        assertTrue(Crew.PATCH_SKILL in store.state.value.unlockedSkills)
        assertFalse(store.resetPrototypeBuild("gh0st"))
        assertEquals(20, store.state.value.playerAp)
        purchase(s, "gh0st", Crew.gh0st.branches.getValue("Tactical").map { it.id })
        val saved = store.state.value
        assertTrue(s.saveManager.saveGame(1, saved))
        assertTrue(store.resetPrototypeBuild())
        assertEquals(80, store.state.value.playerAp)
        assertTrue(s.loadSlot(1))
        assertEquals(saved.purchasedSkillNodes, store.state.value.purchasedSkillNodes)
        assertEquals(saved.playerAp, store.state.value.playerAp)
        store.setMilestone("ms_game_complete")
        assertTrue(s.startNewGamePlus())
        assertEquals(saved.purchasedSkillNodes, store.state.value.purchasedSkillNodes)
        assertEquals(saved.playerAp, store.state.value.playerAp)
        val plus = combat(s)
        try { assertTrue(Crew.RECENTER in plus.combatState!!.combatants.getValue("gh0st").combatant.talents) }
        finally { plus.close() }
    }

    @Test fun earnedCheckpointsLoadAllFourCrewWithExactBudgetAndClaimReplayCannotPayAgain() = withServices { s ->
        for ((id, ap) in listOf("early" to 26, "middle" to 36, "late" to 44)) {
            assertTrue(s.startDebugScenario("crew_build_$id"))
            runBlocking { withTimeout(10000) { s.exploration.uiState.first { !it.isLoading } } }
            val snapshot = s.sessionStore.state.value
            assertEquals(ap, snapshot.playerAp)
            assertEquals(4, snapshot.partyMembers.size)
            assertTrue(snapshot.purchasedSkillNodes.isEmpty())
            assertTrue(Crew.PATCH_SKILL in snapshot.unlockedSkills)
            s.sessionStore.completeQuest("w1_mq01")
            s.sessionStore.addPartyMember("orion")
            s.sessionStore.addPartyMember("gh0st")
            assertEquals(ap, s.sessionStore.state.value.playerAp)
            Crew.trees.forEach { tree ->
                purchase(s, tree.character, tree.branches.values.first().take(3).map { it.id })
            }
            assertEquals(ap - 24, s.sessionStore.state.value.playerAp)
            val last = Crew.gh0st.branches.values.first().last()
            s.exploration.openSkillTree("gh0st")
            waitFor { s.exploration.uiState.value.skillTreeOverlay?.characterId == "gh0st" }
            val allowed = s.exploration.uiState.value.skillTreeOverlay!!.branches.flatMap { it.nodes }.single { it.id == last.id }.status
            assertEquals(id != "early", allowed.unmetRequirements.isEmpty())
            val battle = combat(s)
            try {
                assertTrue(battle.skillsForPlayer("zeke").any { it.id == Crew.COVER_SKILL })
                assertTrue(battle.skillsForPlayer("orion").any { it.id == Crew.TREATMENT_SKILL })
            } finally { battle.close() }
        }
    }
    private fun screenshot(name: String) {
        val path = File("build/reports/progression-prototype/crew-$name.png").apply { requireNotNull(parentFile).mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", path)
    }
}

