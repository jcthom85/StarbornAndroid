package com.example.starborn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.starborn.feature.combat.ui.CombatScreen
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModelFactory
import com.example.starborn.feature.mainmenu.ui.MainMenuScreen
import com.example.starborn.feature.mainmenu.MainMenuViewModel
import com.example.starborn.feature.mainmenu.MainMenuViewModelFactory
import com.example.starborn.feature.exploration.ui.ExplorationScreen
import com.example.starborn.feature.inventory.InventoryViewModel
import com.example.starborn.feature.inventory.InventoryViewModelFactory
import com.example.starborn.feature.inventory.ui.InventoryRoute
import com.example.starborn.feature.inventory.ui.InventoryTab
import com.example.starborn.feature.inventory.ui.InventoryLaunchOptions
import com.example.starborn.navigation.NavigationDestination.Combat
import com.example.starborn.navigation.NavigationDestination.Exploration
import com.example.starborn.navigation.NavigationDestination.MainMenu
import com.example.starborn.navigation.NavigationDestination.Hub
import com.example.starborn.navigation.NavigationDestination.Inventory
import com.example.starborn.navigation.NavigationDestination.Tinkering
import com.example.starborn.navigation.NavigationDestination.Cooking
import com.example.starborn.navigation.NavigationDestination.FirstAid
import com.example.starborn.navigation.NavigationDestination.Shop
import com.example.starborn.navigation.NavigationDestination.Fishing
import com.example.starborn.di.AppServices
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.CraftingViewModelFactory
import com.example.starborn.feature.crafting.ui.TinkeringRoute
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.feature.shop.ShopViewModel
import com.example.starborn.feature.shop.ShopViewModelFactory
import com.example.starborn.feature.shop.ui.ShopRoute
import com.example.starborn.feature.crafting.CookingViewModel
import com.example.starborn.feature.crafting.CookingViewModelFactory
import com.example.starborn.feature.crafting.FirstAidViewModel
import com.example.starborn.feature.crafting.FirstAidViewModelFactory
import com.example.starborn.feature.crafting.ui.CookingRoute
import com.example.starborn.feature.crafting.ui.FirstAidRoute
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.example.starborn.feature.fishing.viewmodel.FishingViewModelFactory
import com.example.starborn.feature.fishing.ui.FishingRoute
import com.example.starborn.feature.hub.ui.HubScreen
import com.example.starborn.feature.hub.viewmodel.HubViewModel
import com.example.starborn.feature.hub.viewmodel.HubViewModelFactory
import com.example.starborn.data.local.UserSettings
import java.util.Locale

@Composable
fun NavigationHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val services = remember { AppServices(context) }
    val userSettings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    val sessionState by services.sessionStore.state.collectAsState()
    val environmentThemeState by services.environmentThemeManager.state.collectAsState()
    NavHost(
        navController = navController,
        startDestination = MainMenu.route
    ) {
        composable(MainMenu.route) {
            val mainMenuViewModel: MainMenuViewModel = viewModel(factory = MainMenuViewModelFactory(services))
            MainMenuScreen(
                viewModel = mainMenuViewModel,
                onStartGame = {
                    navController.navigate(Hub.route) {
                        popUpTo(MainMenu.route) { inclusive = true }
                    }
                    navController.navigate(Exploration.route)
                },
                onSlotLoaded = {
                    navController.navigate(Hub.route) {
                        popUpTo(MainMenu.route) { inclusive = true }
                    }
                    navController.navigate(Exploration.route)
                }
            )
        }
        composable(Hub.route) {
            val hubViewModel: HubViewModel = viewModel(
                factory = HubViewModelFactory(
                    services.worldDataSource,
                    services.sessionStore
                )
            )
            HubScreen(
                viewModel = hubViewModel,
                onEnterNode = { node ->
                    navController.navigate(Exploration.route) {
                        popUpTo(Hub.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Exploration.route) { backStackEntry ->
            val explorationViewModel: ExplorationViewModel = viewModel(factory = ExplorationViewModelFactory(services))
            LaunchedEffect(backStackEntry) {
                val victoryFlow = backStackEntry.savedStateHandle
                    .getStateFlow("combat_victory", emptyList<String>())
                val resultFlow = backStackEntry.savedStateHandle
                    .getStateFlow("combat_result", CombatResultPayload.EMPTY)
                val tinkeringClosedFlow = backStackEntry.savedStateHandle
                    .getStateFlow("tinkering_closed", false)
                val tinkeringCraftFlow = backStackEntry.savedStateHandle
                    .getStateFlow("tinkering_craft", "")
                val fishingResultFlow = backStackEntry.savedStateHandle
                    .getStateFlow("fishing_result", null as FishingResultPayload?)

                launch {
                    victoryFlow.collect { defeatedIds ->
                        if (defeatedIds.isNotEmpty()) {
                            explorationViewModel.onCombatVictoryEnemiesCleared(defeatedIds)
                            backStackEntry.savedStateHandle["combat_victory"] = emptyList<String>()
                        }
                    }
                }

                launch {
                    resultFlow.collect { result ->
                        if (!result.isPlaceholder) {
                            when (result.outcome) {
                                CombatResultPayload.Outcome.VICTORY ->
                                    explorationViewModel.onCombatVictory(result)
                                CombatResultPayload.Outcome.DEFEAT ->
                                    explorationViewModel.onCombatDefeat(result.enemyIds)
                                CombatResultPayload.Outcome.RETREAT ->
                                    explorationViewModel.onCombatRetreat(result.enemyIds)
                            }
                            backStackEntry.savedStateHandle["combat_result"] = CombatResultPayload.EMPTY
                        }
                    }
                }

                launch {
                    tinkeringClosedFlow.collect { closed ->
                        if (closed) {
                            explorationViewModel.onTinkeringClosed()
                            backStackEntry.savedStateHandle["tinkering_closed"] = false
                        }
                    }
                }

                launch {
                    tinkeringCraftFlow.collect { craftedId ->
                        if (!craftedId.isNullOrBlank()) {
                            explorationViewModel.onTinkeringCrafted(craftedId)
                            backStackEntry.savedStateHandle["tinkering_craft"] = ""
                        }
                    }
                }

                launch {
                    fishingResultFlow.collect { payload ->
                        if (payload != null) {
                            explorationViewModel.onFishingResult(payload)
                            backStackEntry.savedStateHandle["fishing_result"] = null
                        }
                    }
                }
            }
            ExplorationScreen(
                viewModel = explorationViewModel,
                audioCuePlayer = services.audioCuePlayer,
                uiEventBus = services.uiEventBus,
                onEnemySelected = { enemyIds ->
                    if (enemyIds.isNotEmpty()) {
                        navController.navigate(Combat.create(enemyIds))
                    }
                },
                onOpenInventory = { options ->
                    val tabParam = options.initialTab?.name?.lowercase(Locale.getDefault())
                    val slotParam = options.focusSlot?.lowercase(Locale.getDefault())
                    val queryParts = buildList {
                        tabParam?.let { add("tab=${Uri.encode(it)}") }
                        slotParam?.let { add("slot=${Uri.encode(it)}") }
                    }
                    val destination = if (queryParts.isEmpty()) {
                        Inventory.route
                    } else {
                        "${Inventory.route}?${queryParts.joinToString("&")}"
                    }
                    navController.navigate(destination)
                },
                onOpenTinkering = {
                    backStackEntry.savedStateHandle["tinkering_closed"] = false
                    backStackEntry.savedStateHandle["tinkering_craft"] = ""
                    navController.navigate(Tinkering.route)
                },
                onOpenCooking = { navController.navigate(Cooking.route) },
                onOpenFirstAid = { navController.navigate(FirstAid.route) },
                onOpenFishing = { zoneId -> navController.navigate(Fishing.create(zoneId)) },
                onOpenShop = { shopId ->
                    navController.navigate(Shop.create(shopId))
                },
                onReturnToHub = { navController.popBackStack() },
                fxEvents = services.uiFxBus.fxEvents
            )
        }
        composable(
            route = "${Inventory.route}?tab={tab}&slot={slot}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("slot") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val inventoryViewModel: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(
                    services.inventoryService,
                    services.craftingService,
                    services.sessionStore
                )
            )
            val tabArg = backStackEntry.arguments?.getString("tab")
            val slotArg = backStackEntry.arguments?.getString("slot")
            val initialTab = tabArg?.let { arg ->
                InventoryTab.values().firstOrNull { it.name.equals(arg, ignoreCase = true) }
            }
            val initialSlot = slotArg?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault())
            InventoryRoute(
                viewModel = inventoryViewModel,
                onBack = { navController.popBackStack() },
                highContrastMode = userSettings.highContrastMode,
                largeTouchTargets = userSettings.largeTouchTargets,
                theme = environmentThemeState.theme,
                credits = sessionState.playerCredits,
                initialTab = initialTab,
                focusSlot = initialSlot
            )
        }
        composable(Tinkering.route) {
            val craftingViewModel: CraftingViewModel = viewModel(factory = CraftingViewModelFactory(services.craftingService))
            TinkeringRoute(
                viewModel = craftingViewModel,
                onBack = { navController.popBackStack() },
                onCrafted = { result ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("tinkering_craft", result.itemId)
                },
                onClosed = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("tinkering_closed", true)
                },
                highContrastMode = userSettings.highContrastMode,
                largeTouchTargets = userSettings.largeTouchTargets
            )
        }
        composable(Cooking.route) {
            val cookingViewModel: CookingViewModel = viewModel(factory = CookingViewModelFactory(services))
            CookingRoute(
                viewModel = cookingViewModel,
                onBack = { navController.popBackStack() },
                onPlayAudio = { cue ->
                    services.audioCuePlayer.execute(services.audioRouter.commandsForUi(cue))
                },
                onTriggerFx = services.uiFxBus::trigger,
                highContrastMode = userSettings.highContrastMode,
                largeTouchTargets = userSettings.largeTouchTargets
            )
        }
        composable(FirstAid.route) {
            val firstAidViewModel: FirstAidViewModel = viewModel(factory = FirstAidViewModelFactory(services))
            FirstAidRoute(
                viewModel = firstAidViewModel,
                onBack = { navController.popBackStack() },
                onPlayAudio = { cue ->
                    services.audioCuePlayer.execute(services.audioRouter.commandsForUi(cue))
                },
                onTriggerFx = services.uiFxBus::trigger,
                highContrastMode = userSettings.highContrastMode,
                largeTouchTargets = userSettings.largeTouchTargets
            )
        }
        composable(
            route = Fishing.route,
            arguments = listOf(
                navArgument("zoneId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val zoneId = backStackEntry.arguments?.getString("zoneId")
            if (zoneId != null) {
                val fishingViewModel: FishingViewModel = viewModel(factory = FishingViewModelFactory(services.fishingService, zoneId))
                FishingRoute(
                    viewModel = fishingViewModel,
                    onBack = { navController.popBackStack() },
                    onFinish = { result ->
                        if (result != null) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("fishing_result", result)
                        }
                        navController.popBackStack()
                    },
                    highContrastMode = userSettings.highContrastMode,
                    largeTouchTargets = userSettings.largeTouchTargets
                )
            } else {
                navController.popBackStack()
            }
        }
        composable(
            route = Combat.route,
            arguments = listOf(navArgument("enemyIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("enemyIds")
            val enemyIds = encoded
                ?.let { Uri.decode(it) }
                ?.split(',')
                ?.mapNotNull { id -> id.takeIf { it.isNotBlank() } }
                ?.distinct()
                ?.ifEmpty { null }
            if (enemyIds != null) {
                val combatViewModel: CombatViewModel = viewModel(
                    factory = CombatViewModelFactory(services, enemyIds)
                )
                CombatScreen(
                    navController = navController,
                    viewModel = combatViewModel,
                    audioCuePlayer = services.audioCuePlayer,
                    suppressFlashes = userSettings.disableFlashes,
                    suppressScreenshake = userSettings.disableScreenshake,
                    highContrastMode = userSettings.highContrastMode,
                    largeTouchTargets = userSettings.largeTouchTargets,
                    cinematicState = services.cinematicState,
                    onAdvanceCinematic = services.cinematicCoordinator::advance
                )
            }
        }
        composable(
            route = Shop.route,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("shopId")
            val shopId = encoded?.let { Uri.decode(it) }
            if (!shopId.isNullOrBlank()) {
                val shopViewModel: ShopViewModel = viewModel(
                    factory = ShopViewModelFactory(services, shopId)
                )
                ShopRoute(
                    viewModel = shopViewModel,
                    onBack = { navController.popBackStack() },
                    highContrastMode = userSettings.highContrastMode,
                    largeTouchTargets = userSettings.largeTouchTargets,
                    voiceoverController = services.voiceoverController
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
