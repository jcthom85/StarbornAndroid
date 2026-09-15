package com.example.starborn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run only on a disposable test installation: presets replace the current session. */
@RunWith(AndroidJUnit4::class)
class ProgressionReleaseInstrumentedTest {
    @Test fun allScenarioLaunchersAndDiskRecovery() {
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        lateinit var services: AppServices
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { services = AppServices(context) }
        val rooms = services.worldDataSource.loadRooms().map { it.id }.toSet()
        val hubs = services.worldDataSource.loadHubs().map { it.id }.toSet()
        val failures = mutableListOf<String>()
        DebugScenarioCatalog.scenarios.forEach { scenario ->
            instrumentation.runOnMainSync {
                try {
                    check(services.startDebugScenario(scenario.id)) { "launcher returned false" }
                    val state = services.sessionStore.state.value
                    check(state.roomId == null || state.roomId in rooms) { "bad room ${state.roomId}" }
                    check(state.hubId in hubs) { "bad hub ${state.hubId}" }
                    state.questStageById.forEach { (quest, stage) ->
                        check(services.questRepository.questById(quest)?.stages?.any { it.id == stage } == true) {
                            "bad stage $quest/$stage"
                        }
                    }
                    state.inventory.keys.forEach { check(services.itemRepository.findItem(it) != null) { "bad item $it" } }
                } catch (error: Throwable) { failures += "${scenario.id}: ${error.message}" }
            }
            instrumentation.waitForIdleSync()
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
        runBlocking(Dispatchers.Main) {
            assertTrue(services.startDebugScenario("qa_meal_reload"))
            val meal = services.sessionStore.state.value.activeMealBuff
            val health = services.sessionStore.state.value.partyMemberHp
            services.saveSlot(3)
            assertTrue(services.loadSlot(3))
            assertEquals(meal, services.sessionStore.state.value.activeMealBuff)
            assertEquals(health, services.sessionStore.state.value.partyMemberHp)
            repeat(3) { services.sessionStore.decrementMealBuffEncounter() }
            assertNull(services.sessionStore.state.value.activeMealBuff)
            services.inventoryService.restore(emptyMap())
            services.sessionStore.restore(GameSessionState(
                worldId = "world_1", hubId = "hub_1_homestead", roomId = "workshop_floor",
                playerId = "nova", partyMembers = listOf("nova"), activeQuests = setOf("w1_mq01"),
                questTasksCompleted = mapOf("w1_mq01" to setOf("equip_starter_gear"))
            ))
            services.saveSlot(3)
            assertTrue(services.loadSlot(3))
            assertEquals(1, services.sessionStore.state.value.inventory["cryo_inductor"])
            assertEquals(1, services.inventoryService.snapshot()["cryo_inductor"])
            services.sessionStore.restore(GameSessionState(
                worldId = "world_1", hubId = "hub_2_logistics", roomId = "launch_pod",
                playerId = "nova", activeQuests = setOf("w1_mq05"),
                completedEvents = setOf("w1_mq05_use_nav_console")
            ))
            services.saveSlot(3)
            assertTrue(services.loadSlot(3))
            assertFalse("w1_mq05_use_nav_console" in services.sessionStore.state.value.completedEvents)
        }
    }
}
