package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.feature.exploration.presentation.InlineActionTarget
import com.example.starborn.feature.exploration.presentation.buildInlineActionPlan
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import com.example.starborn.feature.exploration.viewmodel.MinimapService
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopMerchantInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun withMerchant(room: String, block: (DesktopAppServices) -> Unit) {
        val directory = Files.createTempDirectory("starborn-merchant-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            assertTrue(services.startDebugScenario("tut_npc_dialogue"))
            services.sessionStore.restore(services.sessionStore.state.value.copy(
                roomId = room, activeQuests = emptySet(), completedQuests = setOf("w1_mq01"),
                completedMilestones = setOf("ms_w1_mq01_complete"), completedEvents = emptySet(),
                roomStates = emptyMap(), unlockedExits = emptySet()
            ))
            compose.waitUntil(10000) {
                val ui = services.exploration.uiState.value
                ui.currentRoom?.id == room && ui.minimap?.cells?.any { it.isCurrent && it.roomId == room } == true &&
                    ui.fullMap?.cells?.any { it.isCurrent && it.roomId == room } == true
            }
            block(services)
        } finally {
            services.close()
            directory.deleteRecursively()
        }
    }

    @Test fun marketHasOneMerchantAndProseOpensTheSameTradeMenu() = withMerchant("trade_strip") { s ->
        val runtime = s.exploration
        val ui = runtime.uiState.value
        assertEquals(listOf("shopkeep"), ui.npcs)
        assertTrue(ui.actions.none { it is ShopAction })
        assertTrue(MinimapService.SHOP in ui.minimap!!.cells.first { it.isCurrent }.services)
        assertTrue(MinimapService.SHOP in ui.fullMap!!.cells.first { it.isCurrent }.services)
        val plan = buildInlineActionPlan(ui.currentRoom?.description, ui.actions, ui.actionHints, ui.currentRoom)!!
        val npc = plan.segments.map { it.target }.filterIsInstance<InlineActionTarget.Npc>().single()
        runtime.onNpcInteraction(npc.name)
        assertNull(runtime.uiState.value.activeDialogue)
        val greeting = runtime.uiState.value.shopGreeting!!
        assertEquals("Shopkeep", greeting.shopName)
        assertEquals("general_store", greeting.shopId)
        assertEquals("images/npcs/shopkeep.webp", greeting.portraitPath)
        val topic = greeting.choices.first { it.action == ShopDialogueAction.SMALLTALK }
        runtime.onShopChoiceSelected(topic.id)
        assertFalse(runtime.uiState.value.shopGreeting!!.choices.first { it.id == topic.id }.enabled)
        assertEquals("Trade", runtime.uiState.value.shopGreeting!!.choices.first().label)

        var openedShop: String? = null
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        try {
            scope.launch { runtime.events.collect { if (it is ExplorationEvent.OpenShop) openedShop = it.shopId } }
            runtime.onShopChoiceSelected(greeting.choices.first { it.action == ShopDialogueAction.ENTER_SHOP }.id)
            compose.waitUntil(5000) { openedShop == "general_store" }
            assertNull(runtime.uiState.value.shopGreeting)
        } finally { scope.cancel() }
    }

    @Test fun scrapperQuestOfferAndTurnInCompleteBeforeTradeWithoutRepeatingRewards() = withMerchant("trade_scrapper") { s ->
        val runtime = s.exploration
        assertTrue(runtime.uiState.value.actions.none { it is ShopAction })
        runtime.onNpcInteraction("scrapper")
        assertEquals("scrapper_w1_sq01_intro_1", runtime.uiState.value.activeDialogue?.line?.id)
        assertNull(runtime.uiState.value.shopGreeting)
        repeat(2) { runtime.advanceDialogue() }
        assertFalse("w1_sq01" in s.sessionStore.state.value.activeQuests)
        runtime.advanceDialogue()
        assertTrue("w1_sq01" in s.sessionStore.state.value.activeQuests)
        assertEquals("scrappers_contraband", runtime.uiState.value.shopGreeting?.shopId)
        runtime.dismissShopGreeting()

        // Simulate returning with the cache proof; the real authored turn-in must still run.
        s.sessionStore.setMilestone("ms_w1_sq01_cache_opened")
        val grenades = s.inventoryService.snapshot()["pulse_grenade"] ?: 0
        runtime.onNpcInteraction("Scrapper")
        assertEquals("scrapper_w1_sq01_turnin_1", runtime.uiState.value.activeDialogue?.line?.id)
        assertNull(runtime.uiState.value.shopGreeting)
        repeat(2) { runtime.advanceDialogue() }
        assertFalse("w1_sq01" in s.sessionStore.state.value.completedQuests)
        assertEquals(grenades, s.inventoryService.snapshot()["pulse_grenade"] ?: 0)
        runtime.advanceDialogue()
        val state = s.sessionStore.state.value
        assertTrue("w1_sq01" in state.completedQuests)
        assertTrue("ms_w1_sq01_complete" in state.completedMilestones)
        assertTrue("return_cache_proof" in state.questTasksCompleted["w1_sq01"].orEmpty())
        assertEquals(grenades + 2, s.inventoryService.snapshot()["pulse_grenade"] ?: 0)
        assertEquals("scrappers_contraband", runtime.uiState.value.shopGreeting?.shopId)
        runtime.dismissShopGreeting()
        runtime.onNpcInteraction("scrapper")
        assertEquals("scrapper_w1_sq01_complete", runtime.uiState.value.activeDialogue?.line?.id)
        runtime.advanceDialogue()
        assertEquals(grenades + 2, s.inventoryService.snapshot()["pulse_grenade"] ?: 0)
        assertNotNull(runtime.uiState.value.shopGreeting)
    }
}
