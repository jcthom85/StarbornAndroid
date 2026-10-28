package com.example.starborn.desktop

import com.example.starborn.domain.model.GenericAction
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopCoffeeRewardTest {
    @Test fun repeatedDispenserGrantsUpdateInventoryImmediatelyAndSurviveTravelAndReload() = runBlocking {
        val directory = Files.createTempDirectory("starborn-coffee-grants").toFile()
        val services = DesktopAppServices(directory)
        try {
            lateinit var runtime: com.example.starborn.feature.exploration.viewmodel.ExplorationController
            withContext(Dispatchers.Main) {
                check(services.startDebugScenario("tut_npc_dialogue"))
                services.sessionStore.setRoom("admin_lounge")
                runtime = services.exploration
            }
            withTimeout(15000) { while (runtime.uiState.value.isLoading) delay(20) }
            val initial = services.inventoryService.snapshot()["black_coffee"] ?: 0
            repeat(5) { index ->
                withContext(Dispatchers.Main) {
                    runtime.onActionSelected(GenericAction("executive dispenser", "generic", actionEvent = "w1_dispense_admin_coffee"))
                }
                val expected = initial + index + 1
                assertEquals(expected, services.inventoryService.snapshot()["black_coffee"])
                assertEquals(expected, services.sessionStore.state.value.inventory["black_coffee"])
                withTimeout(3000) {
                    while (runtime.uiState.value.inventoryPreview.firstOrNull { it.id == "black_coffee" }?.quantity != expected) delay(10)
                }
            }
            withContext(Dispatchers.Main) { services.sessionStore.setRoom("admin_kiosk") }
            withTimeout(5000) { while (runtime.uiState.value.currentRoom?.id != "admin_kiosk") delay(10) }
            assertEquals(initial + 5, services.inventoryService.snapshot()["black_coffee"])
            assertTrue(runtime.saveGame(3))
            services.inventoryService.removeItem("black_coffee", initial + 5)
            assertEquals(null, services.inventoryService.snapshot()["black_coffee"])
            withContext(Dispatchers.Main) { assertTrue(services.loadSlot(3, resetRuntime = false)) }
            withTimeout(5000) {
                while (runtime.uiState.value.inventoryPreview.firstOrNull { it.id == "black_coffee" }?.quantity != initial + 5) delay(10)
            }
            assertEquals(initial + 5, services.inventoryService.snapshot()["black_coffee"])
            assertEquals(initial + 5, services.sessionStore.state.value.inventory["black_coffee"])
        } finally { services.close(); directory.deleteRecursively() }
    }
}
