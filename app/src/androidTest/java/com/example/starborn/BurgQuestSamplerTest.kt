package com.example.starborn

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.feature.mainmenu.BurgQuestLaunch
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.navigation.BurgQuestEnding
import com.example.starborn.navigation.BurgQuestFinishDialog
import com.example.starborn.navigation.BurgQuestVisit
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BurgQuestSamplerTest {
    @get:Rule val compose = createComposeRule()

    private fun waitText(text: String) = compose.waitUntil(30_000) {
        if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeBy(100)
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun allCombatOutcomesOfferAstraWithoutRequiringVictory() {
        var ending by mutableStateOf(BurgQuestEnding.VICTORY)
        var visited = 0
        var retries = 0
        var finishes = 0
        compose.setContent { StarbornTheme {
            BurgQuestFinishDialog(ending, { visited++ }, { retries++ }, { finishes++ }, {}, {})
        } }
        listOf(BurgQuestEnding.VICTORY, BurgQuestEnding.DEFEAT, BurgQuestEnding.RETREAT).forEachIndexed { index, result ->
            compose.runOnIdle { ending = result }
            compose.onNodeWithText("Visit the Astra").performScrollTo().assertIsEnabled().performClick()
            compose.onNodeWithText(if (result == BurgQuestEnding.VICTORY) "Play again — fresh start" else "Try again — fresh start")
                .performScrollTo().performClick()
            compose.onNodeWithText("Finish demo").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(index + 1, visited)
                assertEquals(index + 1, retries)
                assertEquals(index + 1, finishes)
            }
        }
    }

    @Test fun failedLaunchCanChooseAnotherDemoOrReturnToTitle() {
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        var exited = false
        compose.setContent { StarbornTheme {
            BurgQuestVisit(BurgQuestLaunch.sampler(), true,
                { AppServices(context, isBurgQuestSession = true) }, {}, {}, { exited = true },
                startScenario = { _, _ -> false })
        } }
        compose.onNodeWithText("Begin demo").performClick()
        waitText("This showcase could not start.")
        compose.onNodeWithText("Visit the Astra").assertDoesNotExist()
        compose.onNodeWithText("Explore another demo").performClick()
        waitText("Start the Starborn sampler")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Return to title").performClick()
        compose.runOnIdle { assertTrue(exited) }
    }

    @Test fun tenFreshVisitsResetSceneSuppliesAndRespectNestedBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>())
        var visit by mutableIntStateOf(0)
        var active by mutableStateOf(true)
        lateinit var current: AppServices
        var created = 0
        val directAstra = BurgQuestLaunch(DebugScenarioCatalog.burgfestScenarios.single { it.id == "burgfest_astra" })
        compose.setContent { StarbornTheme {
            if (active) key(visit) {
                BurgQuestVisit(if (visit % 2 == 0) BurgQuestLaunch.homecoming() else directAstra, true,
                    createServices = {
                        AppServices(context, isBurgQuestSession = true).also { current = it; created++ }
                    }, onRetry = { visit++ }, onChoose = {}, onExit = { active = false })
            } else Text("Ready for next visitor")
        } }
        var baselineInventory: Map<String, Int>? = null
        var baselineHp: Map<String, Int>? = null
        repeat(10) { number ->
            if (number % 2 == 0) {
                waitText("Skip")
                compose.onNodeWithTag("burgquest-homecoming").assertExists()
                compose.onNodeWithText("Skip").performClick()
                waitText("Make yourself at home")
                compose.onNodeWithText("Continue").performClick()
            } else {
                waitText("Begin demo")
                compose.onNodeWithText("Begin demo").performClick()
            }
            compose.waitUntil(30_000) {
                compose.onAllNodesWithContentDescription("Deep Mine Asteroid Drill action", ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("burgquest-homecoming").assertDoesNotExist()
            compose.runOnIdle {
                val state = current.sessionStore.state.value
                if (number == 0) { baselineInventory = state.inventory; baselineHp = state.partyMemberHp }
                assertEquals(baselineInventory, state.inventory)
                assertEquals(baselineHp, state.partyMemberHp)
                assertEquals(number + 1, created)
            }
            // Preserve the arcade's own Back behavior: pause first, then return to the ship.
            if (number == 0) {
                compose.onNodeWithContentDescription("Deep Mine Asteroid Drill action", ignoreCase = true)
                    .performScrollTo().performClick()
                compose.mainClock.autoAdvance = false
                compose.mainClock.advanceTimeBy(1500)
                SystemClock.sleep(500)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                compose.mainClock.advanceTimeBy(300)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                compose.mainClock.advanceTimeBy(1500)
                compose.mainClock.autoAdvance = true
                waitText("Astra Common Room")
                compose.onNodeWithText("Demo menu").assertDoesNotExist()
            }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            waitText("Demo menu")
            compose.onNodeWithText("Demo guide").performClick()
            waitText("Back to demo")
            compose.onNodeWithTag("burgquest-homecoming").assertDoesNotExist()
            compose.onNodeWithText("Back to demo").performClick()
            compose.runOnIdle {
                current.sessionStore.setPartyMemberHp("nova", 1)
                current.inventoryService.restore(emptyMap())
            }
            compose.onNodeWithText("Demo", substring = false).performClick()
            compose.onNodeWithText("Finish demo").performClick()
            waitText("Until the next adventure")
            compose.onNodeWithText("Return to title").performClick()
            waitText("Ready for next visitor")
            android.util.Log.i("BurgQuestCycles", "PASS visitor ${number + 1}: reset, scene, back, finish")
            if (number < 9) compose.runOnIdle { visit++; active = true }
        }
    }
}
