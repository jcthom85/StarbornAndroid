package com.example.starborn.navigation

sealed class NavigationDestination(val route: String) {
    data object MainMenu : NavigationDestination("main_menu")
    data object Exploration : NavigationDestination("exploration")
    data object Combat : NavigationDestination("combat/{enemyId}") {
        fun create(enemyId: String) = "combat/$enemyId"
    }
    data object Inventory : NavigationDestination("inventory")
    data object Tinkering : NavigationDestination("tinkering")
}
