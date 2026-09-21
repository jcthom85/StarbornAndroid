package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.di.AppServices
import com.example.starborn.navigation.NavigationDestination
import com.example.starborn.navigation.NavigationHost
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real navigation/combat UI with disposable persistence; packaged only in the test APK. */
class FoundryPlaytestLauncherTest {
    @get:Rule val compose = createComposeRule()

    @Test fun launchIsolatedFoundry() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val scenario = args.getString("foundryScenario") ?: "campaign_w4_mq17"
        require(scenario.matches(Regex("campaign_w4_mq(16|17|18|19|20)")))
        val enemies = args.getString("foundryEnemies")?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        val permitted = setOf("slag_golem", "welder_bot", "magma_drone", "phantom_prototype",
            "flame_trooper", "conveyor_crusher", "titan_walker_boss")
        require(enemies.size <= 4 && enemies.all { it in permitted })
        val holdSeconds = (args.getString("foundryHoldSeconds") ?: "0").toLong()
        require(holdSeconds in 0..900)
        val base = ApplicationProvider.getApplicationContext<Context>()
        val isolated = IsolatedProgressionContext(base)
        val output = File(base.getExternalFilesDir(null), "foundry-playtest/${System.currentTimeMillis()}").apply { mkdirs() }
        lateinit var services: AppServices
        lateinit var nav: NavHostController
        instrumentation.runOnMainSync { services = AppServices(isolated, true) }
        try {
            // Allow the initial empty DataStore hydration to finish before restoring the fixture.
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync {
                assertTrue(services.debugScenarioError, services.startDebugScenario(scenario))
                val prepared = services.sessionStore.state.value
                services.sessionStore.restore(prepared.copy(
                    equippedWeapons = mapOf("nova" to "mining_pistol", "zeke" to "zeke_shock_fists",
                        "orion" to "orion_prism_focus", "gh0st" to "gh0st_whisperblade"),
                    equippedArmors = mapOf("nova" to "nova_flux_liner", "zeke" to "zeke_surge_harness",
                        "orion" to "orion_channeler_mantle", "gh0st" to "gh0st_phaseweave_jacket")
                ))
                val session = services.sessionStore.state.value
                assertEquals("world_4", session.worldId)
                assertEquals(4, session.partyMembers.size)
                // Smoke fixture only: do not imply level 9/default gear is calibrated campaign balance.
                File(output, "fixture.txt").writeText("scenario=$scenario\nenemies=$enemies\n" +
                    "fixture=explicit-starter-gear; NOT balance-calibrated\n" +
                    "isolatedFiles=${isolated.filesDir}\nnormalFiles=${base.filesDir}\n$session\n")
                assertNotEquals(base.filesDir.canonicalPath, isolated.filesDir.canonicalPath)
            }
            compose.setContent {
                StarbornTheme {
                    nav = rememberNavController()
                    NavigationHost(navController = nav, providedServices = services,
                        initialDestination = NavigationDestination.Exploration.route)
                }
            }
            compose.waitForIdle()
            val expectedRoom = services.worldDataSource.loadRooms().first {
                it.id == services.sessionStore.state.value.roomId
            }.title
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText(expectedRoom, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            if (enemies.isNotEmpty()) {
                instrumentation.runOnMainSync { nav.navigate(NavigationDestination.Combat.create(enemies)) }
                compose.waitForIdle()
                SystemClock.sleep(4000)
            }
            compose.waitForIdle()
            instrumentation.runOnMainSync {
                val route = nav.currentDestination?.route
                File(output, "destination.txt").writeText(route.orEmpty())
                assertEquals(if (enemies.isEmpty()) NavigationDestination.Exploration.route else
                    NavigationDestination.Combat.route, route)
            }
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(output, "screen.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
            android.util.Log.i("FoundryPlaytest", "Evidence: ${output.absolutePath}")
            if (holdSeconds > 0) SystemClock.sleep(holdSeconds * 1000)
        } finally {
            instrumentation.runOnMainSync { services.release() }
        }
    }
}
