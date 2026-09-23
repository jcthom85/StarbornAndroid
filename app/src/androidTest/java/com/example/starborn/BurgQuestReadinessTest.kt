package com.example.starborn

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.feature.mainmenu.BurgQuestDemo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.example.starborn.domain.session.GameSessionPersistence
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Inspects real packaged showcase launch states without touching normal saves. */
class BurgQuestReadinessTest {
    @Test fun boothWritesDoNotReplaceCampaignSaves() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        lateinit var campaign: AppServices
        lateinit var demo: AppServices
        instrumentation.runOnMainSync {
            campaign = AppServices(context)
            demo = campaign.createBurgQuestSession()
        }
        try {
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync { assertTrue(campaign.startNewGame()) }
            SystemClock.sleep(1000)
            val original = campaign.sessionStore.state.value
            runBlocking { campaign.saveSlot(3); campaign.quickSave() }
            val normalDisk = GameSessionPersistence(File(context.filesDir, "datastore"))
            val originalAutosave = runBlocking { normalDisk.readAutosave() }
            assertNotNull(originalAutosave)
            instrumentation.runOnMainSync { assertTrue(demo.startDebugScenario("burgfest_boss")) }
            runBlocking { demo.saveSlot(3); demo.quickSave() }
            SystemClock.sleep(1000)
            runBlocking {
                assertEquals(original, campaign.slotState(3))
                assertEquals(original, campaign.quickSaveInfo()?.state)
                assertEquals(original, normalDisk.sessionFlow.first())
                assertEquals(originalAutosave, normalDisk.readAutosave())
                assertEquals("foundry_titan_dock", demo.slotState(3)?.roomId)
            }
            assertEquals(original, campaign.sessionStore.state.value)
        } finally { instrumentation.runOnMainSync { demo.release(); campaign.release() } }
    }

    @Test fun inspectAllShowcaseFixtures() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(context), true, isBurgQuestSession = true) }
        try {
            SystemClock.sleep(1000)
            DebugScenarioCatalog.burgfestScenarios.forEach { scenario ->
                instrumentation.runOnMainSync {
                    assertTrue(scenario.id + ": " + services.debugScenarioError, services.startDebugScenario(scenario.id))
                    val state = services.sessionStore.state.value
                    val room = services.worldDataSource.loadRooms().single { it.id == state.roomId }
                    if (scenario.id != "burgfest_story") {
                        assertEquals(BurgQuestDemo.party, state.partyMembers)
                        assertEquals(BurgQuestDemo.weapons, state.equippedWeapons)
                        assertEquals(BurgQuestDemo.skills, state.unlockedSkills)
                        assertEquals(BurgQuestDemo.level(scenario.id), state.playerLevel)
                        assertEquals(0, state.playerCredits)
                        assertTrue(state.inventory.size < 25)
                        if (scenario.id == "burgfest_astra") {
                            assertEquals("astra_common_room", room.id)
                            assertEquals(6, state.arcadeProgress.values.count { it.installed })
                            val recipe = services.craftingService.tinkeringRecipes.first { it.result == "functional_cryo_inductor" }
                            services.craftingService.ingredientsFor(recipe).forEach { (id, count) ->
                                assertTrue("Astra recipe ingredient $id", (state.inventory[id] ?: 0) >= count)
                            }
                            assertTrue(services.craftingService.isSchematicLearned(recipe.id))
                            assertTrue(services.craftingService.canCraft(recipe))
                        }
                        services.sessionStore.setPartyMemberHp("nova", 1)
                        services.inventoryService.restore(emptyMap())
                        assertTrue(services.startDebugScenario(scenario.id))
                        val reset = services.sessionStore.state.value
                        assertEquals(state.inventory, reset.inventory)
                        assertEquals(state.partyMemberHp, reset.partyMemberHp)
                    }
                    android.util.Log.i("BurgQuestAudit", "${scenario.id}: room=${room.id} party=${state.partyMembers} " +
                        "level=${state.playerLevel} partyLevels=${state.partyMemberLevels} credits=${state.playerCredits} " +
                        "inventoryKinds=${state.inventory.size} skills=${state.unlockedSkills.size} " +
                        "weapons=${state.equippedWeapons} armor=${state.equippedArmors} " +
                        "arcades=${state.arcadeProgress.filterValues { it.installed }.keys}")
                }
            }
        } finally { instrumentation.runOnMainSync { services.release() } }
    }
}
