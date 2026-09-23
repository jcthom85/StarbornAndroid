package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.navigation.NavigationHost
import com.example.starborn.ui.theme.StarbornTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class BurgQuestNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun astraWorkbenchOpensAndCraftsStockedRecipe() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        instrumentation.runOnMainSync {
            services = AppServices(IsolatedProgressionContext(base), isBurgQuestSession = true)
            assertTrue(services.startDebugScenario("burgfest_astra"))
            services.sessionStore.setRoom("astra_cargo_bay")
        }
        try {
            val recipe = services.craftingService.tinkeringRecipes.first { it.result == "functional_cryo_inductor" }
            compose.setContent { StarbornTheme {
                NavigationHost(providedServices = services, initialDestination = "exploration")
            } }
            compose.waitUntil(30_000) {
                compose.onAllNodesWithContentDescription("Astra Workbench action", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Astra Workbench action", ignoreCase = true).performScrollTo().performClick()
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Schematics").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Schematics").performClick()
            compose.onNodeWithContentDescription("Craft ${recipe.name}").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10_000) { (services.sessionStore.state.value.inventory["functional_cryo_inductor"] ?: 0) > 0 }
            val output = File(base.getExternalFilesDir(null), "burgquest-prep").apply { mkdirs() }
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(output, "09-workbench-crafted.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        } finally { instrumentation.runOnMainSync { services.release() } }
    }

    @Test fun boothLaunchRetryArcadeAndSaveIsolation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        val context = IsolatedProgressionContext(base)
        val evidence = File(base.getExternalFilesDir(null), "burgquest-prep").apply { mkdirs() }
        lateinit var campaign: AppServices
        fun capture(name: String) {
            compose.waitForIdle()
            SystemClock.sleep(300)
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(evidence, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            File(evidence, "$name.txt").writeText(compose.onAllNodes(isRoot(), useUnmergedTree = true)
                .fetchSemanticsNodes().joinToString("\n") { root ->
                    compose.onNode(SemanticsMatcher("root ${root.id}") { it.id == root.id }, useUnmergedTree = true)
                        .printToString(maxDepth = 20)
                })
        }
        fun waitText(text: String) = compose.waitUntil(30_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeBy(100)
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        fun launch(id: String) {
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("demo-launch-$id"))
            compose.onNodeWithTag("demo-launch-$id").performClick()
            waitText("Begin demo")
        }
        fun finishDemo() {
            compose.onNodeWithText("Demo", substring = false).performClick()
            waitText("Finish demo")
            compose.onNodeWithText("Finish demo").performClick()
        }
        instrumentation.runOnMainSync { campaign = AppServices(context) }
        try {
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync { assertTrue(campaign.startNewGame()) }
            runBlocking { campaign.saveSlot(3); assertTrue(campaign.quickSave()) }
            val original = campaign.sessionStore.state.value
            compose.setContent { StarbornTheme { NavigationHost(providedServices = campaign) } }
            waitText("BurgQuest Demo")
            compose.onNodeWithText("New Game").assertDoesNotExist()
            compose.onNodeWithText("Debug Scenarios").assertDoesNotExist()
            compose.onNodeWithText("Load Game").assertDoesNotExist()
            compose.onNodeWithText("Settings").assertExists()
            compose.onNodeWithText("BurgQuest Demo").performClick()
            waitText("Start the Starborn sampler")
            capture("01-picker")
            compose.onNodeWithTag("demo-start-sampler").performClick()
            waitText("Begin demo")
            capture("02-combat-guide")
            compose.onNodeWithText("Begin demo").performClick()
            waitText("Demo")
            waitText("Siren")
            capture("03-combat")
            compose.onNodeWithText("Demo", substring = false).performClick()
            compose.onNodeWithText("Demo guide").performClick()
            waitText("Back to demo")
            capture("04-guide-reopened")
            compose.onNodeWithText("Back to demo").performClick()
            finishDemo()
            waitText("Until the next adventure")
            capture("05-finish")
            compose.onNodeWithText("Explore another demo").performClick()
            compose.onNodeWithTag("demo-start-sampler").performClick()
            waitText("Begin demo")
            compose.onNodeWithText("Begin demo").performClick()
            waitText("Siren")
            // Real UI commands and production ATB, not an injected victory outcome.
            repeat(2) { attempt ->
            val started = SystemClock.elapsedRealtime()
            var turn = 0
            compose.mainClock.autoAdvance = false
            while (SystemClock.elapsedRealtime() - started < 180_000) {
                compose.mainClock.advanceTimeBy(500)
                SystemClock.sleep(250)
                if (compose.onAllNodesWithText("Victory! You completed", substring = true).fetchSemanticsNodes().isNotEmpty()) break
                if (compose.onAllNodesWithText("CHOOSE A TARGET", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()) {
                    val livingTargets = compose.onAllNodes(
                        (hasContentDescription("Siren Skimmer") or hasContentDescription("Spore-Spitter")) and hasClickAction())
                    if (livingTargets.fetchSemanticsNodes().isNotEmpty()) livingTargets.onFirst().performClick()
                    continue
                }
                val continueButton = compose.onAllNodes((hasText("Continue") or hasText("Next")) and hasClickAction())
                if (continueButton.fetchSemanticsNodes().isNotEmpty()) {
                    continueButton.onFirst().performClick()
                    continue
                }
                val attack = compose.onAllNodes(hasText("Attack") and hasClickAction())
                if (attack.fetchSemanticsNodes().isEmpty()) {
                    val member = listOf("Nova", "Zeke", "Orion", "Gh0st")[turn++ % 4]
                    val portrait = compose.onAllNodesWithContentDescription(member, ignoreCase = true)
                    if (portrait.fetchSemanticsNodes().isNotEmpty()) portrait.onFirst().performClick()
                } else {
                    attack.onFirst().performClick()
                    compose.mainClock.advanceTimeBy(200)
                    val enemy = listOf("Siren Skimmer", "Spore-Spitter")[turn++ % 2]
                    val target = compose.onAllNodesWithContentDescription(enemy, ignoreCase = true)
                    if (target.fetchSemanticsNodes().isNotEmpty()) target.onFirst().performClick()
                }
            }
            capture("05b-combat-result")
            compose.onNodeWithText("Victory! You completed the combat showcase.").assertExists()
            android.util.Log.i("BurgQuestFlow", "Real UI tactical completion ms=${SystemClock.elapsedRealtime() - started}; automated selection, not newcomer timing")
            compose.mainClock.autoAdvance = true
            if (attempt == 0) {
                compose.onNodeWithText("Play again (Tactical Combat)").performScrollTo().performClick()
                waitText("Begin demo")
                compose.onNodeWithText("Begin demo").performClick()
                waitText("Siren")
            }
            }
            compose.onNodeWithText("Visit the Astra").performClick()
            waitText("Everyone made it back.")
            capture("05c-homecoming")
            // Read all six lines using the visible controls a newcomer sees.
            val crewLines = listOf("Everyone made it back.", "Good day?", "I checked.",
                "See? Excellent day.", "Let the next adventure", "Welcome aboard the Astra.")
            crewLines.forEachIndexed { index, line ->
                waitText(line)
                compose.mainClock.advanceTimeBy(6000)
                compose.onNodeWithText(if (index == crewLines.lastIndex) "Explore the Astra" else "Next")
                    .performScrollTo().performClick()
            }
            waitText("Make yourself at home")
            capture("05d-astra-guide")
            compose.onNodeWithText("Continue").performClick()
            compose.waitUntil(30_000) {
                compose.onAllNodesWithContentDescription("Deep Mine Asteroid Drill action", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
            }
            capture("06-astra")
            compose.onNodeWithContentDescription("Talk to Orion").performScrollTo().performClick()
            waitText("The Astra's hull")
            compose.mainClock.advanceTimeBy(10_000)
            capture("06b-talk-to-orion")
            compose.onNodeWithContentDescription("Dialogue Popup. Tap to continue").performClick()
            compose.onNodeWithContentDescription("Deep Mine Asteroid Drill action", ignoreCase = true).performScrollTo().performClick()
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(1500)
            SystemClock.sleep(1000)
            capture("07-arcade")
            finishDemo()
            compose.mainClock.advanceTimeBy(1000)
            compose.onNodeWithText("Return to title").performClick()
            compose.mainClock.advanceTimeBy(1000)
            compose.mainClock.autoAdvance = true
            waitText("BurgQuest Demo")
            compose.onNodeWithText("BurgQuest Demo").performClick()
            launch("burgfest_boss")
            capture("10-titan-guide")
            compose.onNodeWithText("Begin demo").performClick()
            waitText("Titan Walker")
            capture("11-titan")
            finishDemo()
            compose.onNodeWithText("Explore another demo").performClick()
            launch("burgfest_story")
            compose.onNodeWithText("Begin demo").performClick()
            waitText("Demo")
            waitText("Nova's Bunk")
            compose.onNodeWithContentDescription("bunk light").assertExists()
            SystemClock.sleep(5000)
            capture("12-story")
            finishDemo()
            compose.onNodeWithText("Return to title").performClick()
            waitText("BurgQuest Demo")
            assertEquals(original, campaign.sessionStore.state.value)
            runBlocking {
                assertEquals(original, campaign.slotState(3))
                assertEquals(original, campaign.quickSaveInfo()?.state)
            }
            capture("08-return-title")
        } finally { instrumentation.runOnMainSync { campaign.release() } }
    }
}
