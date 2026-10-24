package com.example.starborn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.starborn.debug.DebugTestRegistry
import com.example.starborn.di.AppServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugTestSessionInstrumentedTest {
    @Test fun earnedWorld1CheckpointsPreserveProgressAndSeparateSaves() = runBlocking(Dispatchers.Main) {
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        val campaign = AppServices(context)
        val test = AppServices(context, isTestSession = true)
        try {
            delay(250)
            assertTrue(campaign.startNewGame())
            campaign.sessionStore.addCredits(123)
            campaign.saveSlot(1)
            val protected = campaign.slotState(1)
            val expectedRooms = mapOf(
                "w1_mq02" to "workshop_floor", "w1_mq03" to "checkpoint_booth",
                "w1_mq04" to "echo_heart", "w1_mq05" to "launch_lift",
                "w1_sq01" to "trade_scrapper", "w1_sq02" to "medbay_exam1",
                "w1_sq03" to "admin_lobby", "w1_sq04" to "server_hub",
                "w1_sq05" to "mine_shunt", "w1_minecart_puzzle" to "mine_junction")
            expectedRooms.forEach { (id, room) ->
                assertTrue("$id: ${test.debugScenarioError}", test.startDebugScenario("campaign_$id"))
                val checkpoint = test.sessionStore.state.value
                assertEquals(room, checkpoint.roomId)
                assertEquals("mining_pistol", checkpoint.equippedWeapons["nova"])
                assertEquals("nova_flux_liner", checkpoint.equippedArmors["nova"])
                assertTrue("w1_mq01" in checkpoint.completedQuests)
                assertFalse(id in checkpoint.completedQuests)
                test.saveSlot(1)
                test.sessionStore.addCredits(100)
                assertTrue(test.loadSlot(1))
                assertEquals(checkpoint.inventory, test.sessionStore.state.value.inventory)
                assertEquals(checkpoint.completedMilestones, test.sessionStore.state.value.completedMilestones)
                assertEquals(checkpoint.unlockedSkills, test.sessionStore.state.value.unlockedSkills)
                assertEquals(checkpoint.equippedWeapons, test.sessionStore.state.value.equippedWeapons)
                assertEquals(checkpoint.equippedArmors, test.sessionStore.state.value.equippedArmors)
                assertEquals(protected, campaign.slotState(1))
            }
        } finally { campaign.release(); test.release() }
    }

    @Test fun crewCheckpointsAndTestSavesLeaveCampaignSlotsUntouched() = runBlocking(Dispatchers.Main) {
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        val campaign = AppServices(context)
        val test = AppServices(context, isTestSession = true)
        try {
            delay(250)
            assertTrue(campaign.startNewGame())
            campaign.sessionStore.addCredits(123)
            campaign.saveSlot(1)
            campaign.quickSave()
            val protectedSlot = campaign.slotState(1)
            val protectedQuick = campaign.quickSaveInfo()?.state
            for ((id, ap) in listOf("early" to 26, "middle" to 36, "late" to 44)) {
                assertTrue(test.startDebugScenario("crew_build_$id"))
                assertEquals(ap, test.sessionStore.state.value.playerAp)
                assertEquals(4, test.sessionStore.state.value.partyMembers.size)
                assertTrue(test.sessionStore.state.value.purchasedSkillNodes.isEmpty())
                test.saveSlot(1)
                test.quickSave()
            }
            assertEquals(protectedSlot, campaign.slotState(1))
            assertEquals(protectedQuick, campaign.quickSaveInfo()?.state)
        } finally { campaign.release(); test.release() }
    }

    @Test fun rebuiltFixturesLaunchAndTestSavesNeverReplaceCampaignSaves() = runBlocking(Dispatchers.Main) {
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        val campaign = AppServices(context)
        val test = AppServices(context, isTestSession = true)
        try {
            // Allow initial disk hydration before preparing fixtures.
            delay(250)
            assertTrue(campaign.startNewGame())
            campaign.sessionStore.addCredits(123)
            campaign.saveSlot(1)
            campaign.quickSave()
            val protectedSlot = campaign.slotState(1)
            val protectedQuickSave = campaign.quickSaveInfo()?.state
            assertFalse(campaign.startDebugScenario("campaign_w1_mq01"))
            assertNotNull(campaign.debugScenarioError)

            DebugTestRegistry.allScenarios.forEach { scenario ->
                assertTrue("${scenario.id}: ${test.debugScenarioError}", test.startDebugScenario(scenario.id))
                assertTrue("nova" in test.sessionStore.state.value.partyMembers)
                scenario.procedure?.cabinetId?.let { cabinetId ->
                    assertEquals(setOf(cabinetId), test.sessionStore.state.value.arcadeProgress.keys)
                    assertTrue(test.arcadeService.progress(cabinetId).installed)
                    assertEquals(0, test.arcadeService.progress(cabinetId).highScore)
                    assertTrue(test.arcadeService.progress(cabinetId).claimedTiers.isEmpty())
                }
            }
            verifyRecipeBoundaries(test)
            assertEquals(protectedSlot, campaign.slotState(1))
            assertEquals(protectedQuickSave, campaign.quickSaveInfo()?.state)

            // A -> B -> A must discard pending encounters/scenes and not inherit synthetic inventory.
            test.cinematicCoordinator.play("intro_prologue") { error("Abandoned cinematic completed") }
            assertTrue(test.startDebugScenario("campaign_w1_mq01"))
            assertNull(test.cinematicCoordinator.state.value)
            assertEquals("pit_nova_bunk", test.sessionStore.state.value.roomId)
            assertFalse("functional_cryo_inductor" in test.sessionStore.state.value.inventory)
        } finally {
            campaign.release()
            test.release()
        }
    }

    @Test fun recipeBoundariesPreserveExactStockAcrossSaveLoad() = runBlocking(Dispatchers.Main) {
        val services = AppServices(IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>()), true)
        try {
            delay(250)
            verifyRecipeBoundaries(services)
        } finally { services.release() }
    }

    private suspend fun verifyRecipeBoundaries(test: AppServices) {
        assertTrue(test.startDebugScenario("recipe_cryo_exact"))
        val recipe = test.craftingService.tinkeringRecipes.first { it.id == "repair_cryo_inductor" }
        assertTrue(test.craftingService.canCraft(recipe))
        val exactState = test.sessionStore.state.value
        test.saveSlot(1)
        test.quickSave()

        assertTrue(test.startDebugScenario("recipe_cryo_missing"))
        assertFalse(test.craftingService.canCraft(recipe))
        assertEquals(exactState.inventory - "scrap_metal", test.sessionStore.state.value.inventory)
        val beforeRefusal = test.inventoryService.snapshot()
        assertTrue(test.craftingService.craftTinkering(recipe.id) is com.example.starborn.domain.crafting.CraftingOutcome.Failure)
        assertEquals(beforeRefusal, test.inventoryService.snapshot())
        assertTrue(test.loadSlot(1))
        assertEquals(exactState.inventory, test.sessionStore.state.value.inventory)
        assertEquals(exactState.roomId, test.sessionStore.state.value.roomId)
        assertTrue(test.craftingService.craftTinkering(recipe.id) is com.example.starborn.domain.crafting.CraftingOutcome.Success)
        assertEquals(1, test.inventoryService.snapshot()[recipe.result])
        assertFalse(test.craftingService.canCraft(recipe))
        test.saveSlot(2)
        assertTrue(test.startDebugScenario("recipe_cryo_missing"))
        assertTrue(test.loadSlot(2))
        assertEquals(1, test.inventoryService.snapshot()[recipe.result])
    }
}
