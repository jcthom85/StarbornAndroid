package com.example.starborn

import android.content.Context
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

class AstraTransitNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun flightStaysAboardThenDisembarksToMapAndCanReboard() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = ApplicationProvider.getApplicationContext<Context>()
        lateinit var services: AppServices
        lateinit var nav: NavHostController
        instrumentation.runOnMainSync { services = AppServices(IsolatedProgressionContext(base), true) }
        try {
            instrumentation.runOnMainSync {
                assertTrue(services.startDebugScenario("burgfest_astra"))
                services.sessionStore.setRoom("astra_bridge")
            }
            compose.setContent {
                StarbornTheme {
                    nav = rememberNavController()
                    NavigationHost(navController = nav, providedServices = services,
                        initialDestination = NavigationDestination.Exploration.route)
                }
            }
            compose.waitUntil(30_000) {
                compose.onAllNodesWithContentDescription("nav console", ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithContentDescription("nav console", ignoreCase = true).onFirst().performClick()
            compose.onNodeWithText("The Mines").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Stay aboard").fetchSemanticsNodes().isNotEmpty() }
            instrumentation.runOnMainSync {
                assertEquals("astra_bridge", services.sessionStore.state.value.roomId)
                assertEquals("hub_1_homestead", services.sessionStore.state.value.astraReturnHubId)
            }
            compose.onNodeWithText("Disembark").performClick()
            compose.waitUntil(10_000) { nav.currentDestination?.route == NavigationDestination.Hub.route }
            instrumentation.runOnMainSync {
                assertEquals("hub_1_homestead", services.sessionStore.state.value.hubId)
                assertNull(services.sessionStore.state.value.roomId)
            }
            compose.onNodeWithContentDescription("Enter The Astra").assertIsDisplayed()
            compose.onAllNodesWithText("The Astra").onFirst().assertIsDisplayed()
            compose.onNodeWithContentDescription("Enter The Astra").performClick()
            compose.waitUntil(10_000) { nav.currentDestination?.route == NavigationDestination.Exploration.route }
            instrumentation.runOnMainSync { assertEquals("astra_cargo_bay", services.sessionStore.state.value.roomId) }
        } finally {
            instrumentation.runOnMainSync { services.release() }
        }
    }
}
