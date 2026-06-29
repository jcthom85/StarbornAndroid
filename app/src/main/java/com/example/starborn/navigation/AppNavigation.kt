package com.example.starborn.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.starborn.feature.exploration.ui.CombatTransitionOverlay
import com.example.starborn.navigation.NavigationDestination.Combat
import com.example.starborn.navigation.NavigationDestination.Exploration
import com.example.starborn.navigation.NavigationDestination.MainMenu
import com.example.starborn.navigation.NavigationDestination.Hub
import com.example.starborn.navigation.NavigationDestination.Tinkering
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
import com.example.starborn.feature.crafting.FirstAidViewModel
import com.example.starborn.feature.crafting.FirstAidViewModelFactory
import com.example.starborn.feature.crafting.ui.FirstAidRoute
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.feature.fishing.ui.FishingRoute
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.example.starborn.feature.fishing.viewmodel.FishingViewModelFactory
import com.example.starborn.feature.hub.ui.HubScreen
import com.example.starborn.feature.hub.viewmodel.HubViewModel
import com.example.starborn.feature.hub.viewmodel.HubViewModelFactory
import com.example.starborn.data.local.UserSettings
import com.example.starborn.ui.events.UiEvent
import androidx.compose.runtime.DisposableEffect
import com.example.starborn.feature.exploration.ui.TransitionMode
import androidx.compose.ui.draw.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun NavigationHost(
    navController: NavHostController = rememberNavController(),
    showCombatActionText: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val services = remember { AppServices(context) }
    val userSettings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    val sessionState by services.sessionStore.state.collectAsState()
    val environmentThemeState by services.environmentThemeManager.state.collectAsState()
    val settingsScope = rememberCoroutineScope()

    LaunchedEffect(
        services.audioCuePlayer,
        userSettings.musicVolume,
        userSettings.sfxVolume,
        userSettings.voiceVolume
    ) {
        services.audioCuePlayer.setUserMusicGain(userSettings.musicVolume)
        services.audioCuePlayer.setUserSfxGain(userSettings.sfxVolume)
        services.audioCuePlayer.setUserVoiceGain(userSettings.voiceVolume)
    }

    DisposableEffect(lifecycleOwner, services) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> services.audioCuePlayer.pauseForBackground()
                Lifecycle.Event.ON_START -> services.audioCuePlayer.resumeFromBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = MainMenu.route
    ) {
        composable(MainMenu.route) {
            val mainMenuViewModel: MainMenuViewModel = viewModel(factory = MainMenuViewModelFactory(services))
            MainMenuScreen(
                viewModel = mainMenuViewModel,
                audioCuePlayer = services.audioCuePlayer,
                audioRouter = services.audioRouter,
                userSettings = userSettings,
                onMusicVolumeChange = { value ->
                    settingsScope.launch { services.userSettingsStore.setMusicVolume(value) }
                },
                onSfxVolumeChange = { value ->
                    settingsScope.launch { services.userSettingsStore.setSfxVolume(value) }
                },
                onVoiceVolumeChange = { value ->
                    settingsScope.launch { services.userSettingsStore.setVoiceVolume(value) }
                },
                onToggleTutorials = { enabled ->
                    settingsScope.launch { services.userSettingsStore.setTutorialsEnabled(enabled) }
                },
                onToggleVignette = { enabled ->
                    settingsScope.launch { services.userSettingsStore.setVignetteEnabled(enabled) }
                },
                onStartGame = {
                    navController.navigate(Exploration.route) {
                        popUpTo(MainMenu.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onStartHub = {
                    navController.navigate(Hub.route) {
                        popUpTo(MainMenu.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSlotLoaded = {
                    navController.navigate(Exploration.route) {
                        popUpTo(MainMenu.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Hub.route) {
            val hubViewModel: HubViewModel = viewModel(
                factory = HubViewModelFactory(
                    services.worldDataSource,
                    services.questRepository,
                    services.sessionStore
                )
            )
            HubScreen(
                viewModel = hubViewModel,
                userSettings = userSettings,
                onMusicVolumeChange = { value ->
                    settingsScope.launch { services.userSettingsStore.setMusicVolume(value) }
                },
                onSfxVolumeChange = { value ->
                    settingsScope.launch { services.userSettingsStore.setSfxVolume(value) }
                },
                onVoiceVolumeChange = { value ->
                    settingsScope.launch { services.userSettingsStore.setVoiceVolume(value) }
                },
                onToggleTutorials = { enabled ->
                    settingsScope.launch { services.userSettingsStore.setTutorialsEnabled(enabled) }
                },
                onToggleVignette = { enabled ->
                    settingsScope.launch { services.userSettingsStore.setVignetteEnabled(enabled) }
                },
                onQuickSave = {
                    settingsScope.launch { services.quickSave() }
                },
                onEnterNode = { node ->
                    navController.navigate(Exploration.route) {
                        popUpTo(Hub.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Exploration.route) { backStackEntry ->
            val explorationViewModel: ExplorationViewModel = viewModel(factory = ExplorationViewModelFactory(services))
            var combatTransitionVisible by remember { mutableStateOf(false) }
            var pendingCombatEnemyIds by remember { mutableStateOf<List<String>?>(null) }
            val pulseScale = remember { androidx.compose.animation.core.Animatable(1f) }

            DisposableEffect(Unit) {
                onDispose {
                    combatTransitionVisible = false
                }
            }

            LaunchedEffect(combatTransitionVisible) {
                if (combatTransitionVisible) {
                    pulseScale.animateTo(
                        targetValue = 1.05f,
                        animationSpec = androidx.compose.animation.core.tween(100)
                    )
                    pulseScale.animateTo(
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.tween(100)
                    )
                } else {
                    pulseScale.snapTo(1f)
                }
            }

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
                                    explorationViewModel.onCombatRetreat(result)
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
            Box(modifier = Modifier.fillMaxSize()) {
                ExplorationScreen(
                    viewModel = explorationViewModel,
                    audioCuePlayer = services.audioCuePlayer,
                    uiEventBus = services.uiEventBus,
                    modifier = Modifier.scale(pulseScale.value),
                    onEnemySelected = { enemyIds ->
                        if (enemyIds.isEmpty() || combatTransitionVisible) return@ExplorationScreen
                        pendingCombatEnemyIds = enemyIds
                        combatTransitionVisible = true
                    },
                    onOpenTinkering = {
                        backStackEntry.savedStateHandle["tinkering_closed"] = false
                        backStackEntry.savedStateHandle["tinkering_craft"] = ""
                        navController.navigate(Tinkering.route)
                    },
                    onOpenFirstAid = { navController.navigate(FirstAid.route) },
                    onOpenFishing = { zoneId -> navController.navigate(Fishing.create(zoneId)) },
                    onOpenShop = { shopId ->
                        navController.navigate(Shop.create(shopId))
                    },
                    onReturnToHub = {
                        navController.navigate(Hub.route) {
                            popUpTo(Exploration.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    fxEvents = services.uiFxBus.fxEvents
                )

                CombatTransitionOverlay(
                    visible = combatTransitionVisible,
                    theme = environmentThemeState.theme,
                    suppressFlashes = userSettings.disableFlashes,
                    highContrastMode = userSettings.highContrastMode,
                    mode = TransitionMode.ENTER,
                    onFinished = {
                        val enemyIds = pendingCombatEnemyIds
                        pendingCombatEnemyIds = null
                        if (!enemyIds.isNullOrEmpty()) {
                            navController.navigate(Combat.create(enemyIds))
                        } else {
                            combatTransitionVisible = false
                        }
                    }
                )
            }
        }
        composable(Tinkering.route) {
            val craftingViewModel: CraftingViewModel = viewModel(
                factory = CraftingViewModelFactory(
                    services.craftingService,
                    services.inventoryService,
                    services.sessionStore
                )
            )
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
                promptManager = services.promptManager,
                highContrastMode = userSettings.highContrastMode,
                largeTouchTargets = userSettings.largeTouchTargets,
                theme = environmentThemeState.theme
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
                ?.ifEmpty { null }
            if (enemyIds != null) {
                val combatViewModel: CombatViewModel = viewModel(
                    factory = CombatViewModelFactory(
                        services = services,
                        enemyIds = enemyIds,
                        tutorialsEnabled = userSettings.tutorialsEnabled
                    )
                )
                CombatScreen(
                    navController = navController,
                    viewModel = combatViewModel,
                    audioCuePlayer = services.audioCuePlayer,
                    suppressFlashes = userSettings.disableFlashes,
                    suppressScreenshake = userSettings.disableScreenshake,
                    highContrastMode = userSettings.highContrastMode,
                    largeTouchTargets = userSettings.largeTouchTargets,
                    showCombatActionText = showCombatActionText,
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
