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
}
