package com.example.starborn.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.starborn.di.AppServices
import com.example.starborn.feature.mainmenu.BurgQuestDemo
import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.ui.BurgfestDemoDialog

/** Normal services stay alive while a separate, disposable booth session is on screen. */
@Composable
fun NavigationHost(
    navController: NavHostController = rememberNavController(),
    showCombatActionText: Boolean = true,
    providedServices: AppServices? = null,
    initialDestination: String = NavigationDestination.MainMenu.route
) {
    val context = LocalContext.current
    val services = remember(providedServices) { providedServices ?: AppServices(context) }
    var demo by remember { mutableStateOf<DebugScenario?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    DisposableEffect(services) { onDispose { services.release() } }
    LaunchedEffect(demo) {
        if (demo != null) services.audioCuePlayer.pauseForBackground()
        else services.audioCuePlayer.resumeFromBackground()
    }
    val selected = demo
    if (selected == null) {
        CampaignNavigationHost(navController, showCombatActionText, services, initialDestination,
            onBurgQuestLaunch = { demo = it })
    } else key(selected.id, attempt) {
        BurgQuestVisit(selected, showCombatActionText, services::createBurgQuestSession,
            onRetry = { attempt++ },
            onChoose = { demo = it; attempt++ },
            onExit = { demo = null })
    }
}

@Composable
private fun BurgQuestVisit(
    scenario: DebugScenario,
    showCombatActionText: Boolean,
    createServices: () -> AppServices,
    onRetry: () -> Unit,
    onChoose: (DebugScenario) -> Unit,
    onExit: () -> Unit
) {
    var begun by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf<String?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    var chooseAnother by remember { mutableStateOf(false) }
    BackHandler { finish = "Thanks for playing Starborn!" }

    if (begun) {
        val services = remember { createServices() }
        // A booth can cycle through many visitors without recreating the activity.
        // Own and clear each visit's navigation ViewModels as well as its services.
        val visitOwner = remember { object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        } }
        var ready by remember { mutableStateOf(false) }
        DisposableEffect(services) { onDispose {
            visitOwner.viewModelStore.clear()
            services.release()
        } }
        LaunchedEffect(services) {
            ready = services.startDebugScenario(scenario.id, allowInGameLaunch = true)
            if (!ready) finish = "This showcase could not start. Please choose another demo."
        }
        Column(Modifier.fillMaxSize()) {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showGuide = true }) { Text("Demo guide") }
                    TextButton(onClick = { finish = "Thanks for playing Starborn!" }) { Text("Finish demo") }
                }
            }
            // Finish is terminal: unmount the game, including an arcade's animation loop.
            // The next visit always creates a new controller and fresh fixture.
            if (ready && finish == null) Box(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides visitOwner) {
                    CampaignNavigationHost(
                        showCombatActionText = showCombatActionText,
                        providedServices = services,
                        initialDestination = NavigationDestination.Exploration.route,
                        demoEnemies = BurgQuestDemo.enemies(scenario.id),
                        demoPaused = showGuide,
                        onDemoExit = onExit,
                        onDemoCombatResult = { result ->
                            finish = when (result.outcome) {
                                CombatResultPayload.Outcome.VICTORY -> "Victory! You completed the combat showcase."
                                CombatResultPayload.Outcome.DEFEAT -> "The crew was defeated. Try again with fresh supplies, or explore another showcase."
                                CombatResultPayload.Outcome.RETREAT -> "The crew withdrew. Try again with fresh supplies, or explore another showcase."
                            }
                        }
                    )
                }
            }
        }
    }

    if ((!begun || showGuide) && finish == null) AlertDialog(
        onDismissRequest = { if (begun) showGuide = false else onExit() },
        title = { Text(scenario.title) },
        text = { Text(BurgQuestDemo.briefing(scenario.id) + "\n\nDemo progress is separate from campaign saves. Each launch resets the crew and supplies.",
            Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { begun = true; showGuide = false }) {
            Text(if (begun) "Back to demo" else "Begin demo")
        } },
        dismissButton = { TextButton(onClick = onExit) { Text("Title screen") } }
    )
    if (finish != null && !chooseAnother) AlertDialog(
        onDismissRequest = {},
        title = { Text("BurgQuest showcase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(requireNotNull(finish))
                TextButton(onClick = { chooseAnother = true }, modifier = Modifier.fillMaxWidth()) { Text("Choose another demo") }
                TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Play again - fresh start") }
                TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Title screen") }
            }
        },
        confirmButton = {}
    )
    if (chooseAnother) BurgfestDemoDialog(onLaunch = onChoose, onDismiss = onExit)
}
