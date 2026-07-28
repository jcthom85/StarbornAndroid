package com.example.starborn.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAwakePolicyTest {

    @Test
    fun mainMenuAndUnresolvedRoutesFollowDeviceTimeout() {
        assertFalse(shouldKeepScreenAwake(null))
        assertFalse(shouldKeepScreenAwake(""))
        assertFalse(shouldKeepScreenAwake(NavigationDestination.MainMenu.route))
    }

    @Test
    fun everyGameplayDestinationKeepsScreenAwake() {
        listOf(
            NavigationDestination.Hub.route,
            NavigationDestination.Exploration.route,
            NavigationDestination.Combat.route,
            NavigationDestination.Tinkering.route,
            NavigationDestination.FirstAid.route,
            NavigationDestination.Fishing.route,
            NavigationDestination.Shop.route
        ).forEach { route ->
            assertTrue("Gameplay route should keep the screen awake: $route", shouldKeepScreenAwake(route))
        }
    }
}
