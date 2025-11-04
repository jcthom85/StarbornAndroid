package com.example.starborn.navigation

import android.net.Uri

sealed class NavigationDestination(val route: String) {
    data object MainMenu : NavigationDestination("main_menu")
    data object Hub : NavigationDestination("hub")
    data object Exploration : NavigationDestination("exploration")
    data object Combat : NavigationDestination("combat/{enemyIds}") {
        fun create(enemyIds: List<String>): String {
            val payload = Uri.encode(enemyIds.joinToString(","))
            return "combat/$payload"
        }
    }
    data object Inventory : NavigationDestination("inventory")
    data object Tinkering : NavigationDestination("tinkering")
    data object Cooking : NavigationDestination("cooking")
    data object FirstAid : NavigationDestination("first_aid")
    data object Fishing : NavigationDestination("fishing?zoneId={zoneId}") {
        fun create(zoneId: String?): String =
            if (zoneId.isNullOrBlank()) "fishing"
            else "fishing?zoneId=${Uri.encode(zoneId)}"
    }
    data object Shop : NavigationDestination("shop/{shopId}") {
        fun create(shopId: String): String {
            val payload = Uri.encode(shopId)
            return "shop/$payload"
        }
    }
}
