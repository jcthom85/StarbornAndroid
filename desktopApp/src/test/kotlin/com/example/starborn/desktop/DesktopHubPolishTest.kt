package com.example.starborn.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.*
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DesktopHubPolishTest {
    @get:Rule val compose=createComposeRule()
    @Test fun originalHubCueNamesResolveToExistingSoundsAndExplicitBindingsWin() {
        val router=AudioRouter(AudioBindings(ui=mapOf("click" to "sfx_ui_button_click")))
        assertEquals("sfx_ui_button_click",(router.commandsForUi("sfx_hub_node_select").single() as AudioCommand.Play).cueId)
        assertEquals("ui_room_move",(router.commandsForUi("sfx_room_transition").single() as AudioCommand.Play).cueId)
        assertTrue(router.commandsForUi("undefined-event").isEmpty())
        val authored=AudioRouter(AudioBindings(ui=mapOf("sfx_hub_node_select" to "authored_override")))
        assertEquals("authored_override",(authored.commandsForUi("sfx_hub_node_select").single() as AudioCommand.Play).cueId)
    }
    @Test fun compactReserveUsesMeasuredContentAndCapsLongDescriptions() {
        assertEquals(136f,hubCompactReserve(120f,650f),.001f)
        assertEquals(180f,hubCompactReserve(350f,650f),.001f)
        assertEquals(112f,hubCompactReserve(350f,400f),.001f)
    }
    @Test fun navigationOccursAtBlackAndFadeSurvivesOutgoingScreenDisposal() {
        compose.mainClock.autoAdvance=false
        lateinit var travel:DesktopHubTravel
        lateinit var outgoing:CoroutineScope
        var screen by mutableIntStateOf(0)
        var calls=0
        compose.setContent {
            val host=rememberCoroutineScope()
            travel=remember { DesktopHubTravel(host) }
            key(screen) { val scope=rememberCoroutineScope();if(screen==0) outgoing=scope }
        }
        compose.runOnIdle {
            assertTrue(travel.request(outgoing) { assertEquals(1f,travel.opacity.value,.001f);calls++;screen=1 })
            assertFalse(travel.request(outgoing) { calls++ })
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { assertEquals(0,calls);assertTrue(travel.busy) }
        compose.mainClock.advanceTimeBy(112)
        compose.runOnIdle { assertEquals(1,calls);assertTrue(travel.busy) }
        compose.mainClock.advanceTimeBy(240)
        compose.runOnIdle { assertEquals(1,calls);assertFalse(travel.busy);assertEquals(0f,travel.opacity.value,.001f) }
    }
    @Test fun failedTravelClearsOverlayAndAllowsRetry() {
        compose.mainClock.autoAdvance=false
        lateinit var scope:CoroutineScope
        lateinit var travel:DesktopHubTravel
        var failed=false
        var retried=false
        compose.setContent { scope=rememberCoroutineScope();travel=remember { DesktopHubTravel(scope) } }
        compose.runOnIdle { travel.request(scope,onFailure={ failed=true }) { error("Simulated navigation failure") } }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {
            assertTrue(failed);assertFalse(travel.busy);assertEquals(0f,travel.opacity.value,.001f)
            assertTrue(travel.request(scope) { retried=true })
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertTrue(retried);assertFalse(travel.busy) }
    }
}
