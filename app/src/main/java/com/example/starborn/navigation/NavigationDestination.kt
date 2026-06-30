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
    data object Tinkering : NavigationDestination("tinkering?source={source}&filter={filter}") {
        fun create(source: String?, filter: String? = null): String {
            val params = buildList {
                source?.takeIf { it.isNotBlank() }?.let { add("source=${Uri.encode(it)}") }
                filter?.takeIf { it.isNotBlank() }?.let { add("filter=${Uri.encode(it)}") }
            }
            return if (params.isEmpty()) "tinkering" else "tinkering?${params.joinToString("&")}"
        }
    }
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
