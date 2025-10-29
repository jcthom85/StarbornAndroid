package com.example.starborn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.example.starborn.feature.combat.ui.CombatScreen
import com.example.starborn.feature.mainmenu.ui.MainMenuScreen
import com.example.starborn.feature.exploration.ui.ExplorationScreen
import com.example.starborn.feature.inventory.InventoryViewModel
import com.example.starborn.feature.inventory.InventoryViewModelFactory
import com.example.starborn.feature.inventory.ui.InventoryRoute
import com.example.starborn.navigation.NavigationDestination.Combat
import com.example.starborn.navigation.NavigationDestination.Exploration
import com.example.starborn.navigation.NavigationDestination.MainMenu
import com.example.starborn.navigation.NavigationDestination.Inventory
import com.example.starborn.navigation.NavigationDestination.Tinkering
import com.example.starborn.di.AppServices
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.CraftingViewModelFactory
import com.example.starborn.feature.crafting.ui.TinkeringRoute

@Composable
fun NavigationHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val services = remember { AppServices(context) }
    NavHost(
        navController = navController,
        startDestination = MainMenu.route
    ) {
        composable(MainMenu.route) {
            MainMenuScreen(onStartGame = {
                navController.navigate(Exploration.route)
            })
        }
        composable(Exploration.route) {
            val explorationViewModel: ExplorationViewModel = viewModel(factory = ExplorationViewModelFactory(services))
            ExplorationScreen(
                onEnemySelected = { enemyId ->
                    navController.navigate(Combat.create(enemyId))
                },
                onOpenInventory = { navController.navigate(Inventory.route) },
                onOpenTinkering = { navController.navigate(Tinkering.route) },
                viewModel = explorationViewModel
            )
        }
        composable(Inventory.route) {
            val inventoryViewModel: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(
                    services.inventoryService,
                    services.craftingService
                )
            )
            InventoryRoute(
                viewModel = inventoryViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Tinkering.route) {
            val craftingViewModel: CraftingViewModel = viewModel(factory = CraftingViewModelFactory(services.craftingService))
            TinkeringRoute(
                viewModel = craftingViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Combat.route,
            arguments = listOf(navArgument("enemyId") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments
                ?.getString("enemyId")
                ?.let { CombatScreen(navController, it) }
        }
    }
}
