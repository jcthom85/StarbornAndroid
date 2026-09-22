package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

class AstraArcadeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showcaseCabinetsLaunchOnFirstTap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        val evidence = File(base.getExternalFilesDir(null), "astra-arcade-test").apply { mkdirs() }
        lateinit var services: AppServices
        lateinit var nav: NavHostController
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(base), true) }
        try {
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync {
                assertTrue(services.startDebugScenario("burgfest_astra"))
                services.sessionStore.setRoom("astra_common_room")
            }
            compose.setContent {
                StarbornTheme {
                    nav = rememberNavController()
                    NavigationHost(navController = nav, providedServices = services,
                        initialDestination = NavigationDestination.Exploration.route)
                }
            }
            val cabinets = listOf(
                "Deep Mine Asteroid Drill" to "deep_mine_asteroid_drill",
                "Canopy Hopper" to "canopy_hopper",
                "Spire Infiltrator" to "spire_infiltrator",
                "Slag Catcher" to "slag_catcher",
                "Orbital Defense 2000" to "orbital_defense",
                "Harmonic Pulse" to "harmonic_pulse")
            cabinets.forEach { (title, id) ->
                compose.waitUntil(30_000) {
                    compose.onAllNodesWithContentDescription("$title action", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithContentDescription("$title action", ignoreCase = true)
                    .performScrollTo().performClick()
                // Arcade engines animate continuously; they are not Compose-idle screens.
                compose.mainClock.autoAdvance = false
                compose.mainClock.advanceTimeBy(1000)
                compose.waitForIdle()
                SystemClock.sleep(1000)
                val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                File(evidence, "$id.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                screenshot.recycle()
                instrumentation.runOnMainSync {
                    assertEquals(title, NavigationDestination.Arcade.route, nav.currentDestination?.route)
                    assertEquals(id, nav.currentBackStackEntry?.arguments?.getString("cabinetId"))
                    android.util.Log.i("AstraArcadeTest", "PASS first tap: $id")
                    assertTrue(nav.popBackStack())
                }
                compose.mainClock.autoAdvance = true
                compose.waitForIdle()
            }
        } finally {
            instrumentation.runOnMainSync { services.release() }
        }
    }
}
