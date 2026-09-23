package com.example.starborn.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.starborn.di.AppServices
import com.example.starborn.R
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.feature.mainmenu.BurgQuestDemo
import com.example.starborn.feature.mainmenu.BurgQuestLaunch
import com.example.starborn.feature.mainmenu.ui.BurgfestDemoDialog
import com.example.starborn.feature.mainmenu.ui.BurgQuestHomecoming
import kotlinx.coroutines.CancellationException

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
    var demo by remember { mutableStateOf<BurgQuestLaunch?>(null) }
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
    } else key(selected, attempt) {
        BurgQuestVisit(selected, showCombatActionText, services::createBurgQuestSession,
            onRetry = { attempt++ },
            onChoose = { demo = it; attempt++ },
            onExit = { demo = null })
    }
}

internal enum class BurgQuestEnding(val message: String) {
    VICTORY("Victory! You completed the combat showcase."),
    DEFEAT("The crew was defeated. Try again with fresh supplies, or take a breather aboard the Astra."),
    RETREAT("The crew withdrew. Try again with fresh supplies, or take a breather aboard the Astra."),
    FINISHED(BurgQuestDemo.farewell),
    START_FAILED("This showcase could not start. Please choose another demo.");

    val isCombatResult: Boolean get() = this == VICTORY || this == DEFEAT || this == RETREAT
}

@Composable
internal fun BurgQuestVisit(
    launch: BurgQuestLaunch,
    showCombatActionText: Boolean,
    createServices: () -> AppServices,
    onRetry: () -> Unit,
    onChoose: (BurgQuestLaunch) -> Unit,
    onExit: () -> Unit,
    startScenario: (AppServices, String) -> Boolean = { services, id ->
        services.startDebugScenario(id, allowInGameLaunch = true)
    }
) {
    Box(Modifier.fillMaxSize()) {
        // A static backdrop survives session teardown without keeping game/audio loops alive.
        Image(painterResource(R.drawable.title_background_starborn), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        BurgQuestVisitContent(launch, showCombatActionText, createServices, onRetry, onChoose, onExit, startScenario)
    }
}

@Composable
private fun BurgQuestVisitContent(
    launch: BurgQuestLaunch,
    showCombatActionText: Boolean,
    createServices: () -> AppServices,
    onRetry: () -> Unit,
    onChoose: (BurgQuestLaunch) -> Unit,
    onExit: () -> Unit,
    startScenario: (AppServices, String) -> Boolean
) {
    val scenario = launch.scenario
    var begun by remember { mutableStateOf(launch.isHomecoming) }
    var ending by remember { mutableStateOf<BurgQuestEnding?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    var showHomecoming by remember { mutableStateOf(launch.isHomecoming) }
    var showDemoMenu by remember { mutableStateOf(false) }
    var chooseAnother by remember { mutableStateOf(false) }
    BackHandler(enabled = begun && ending == null) { showDemoMenu = true }

    // Finishing unmounts the whole session: game loops, owned ViewModels and audio.
    val services = remember { createServices() }
    DisposableEffect(services) { onDispose {
        services.release()
    } }
    if (begun && ending == null) {
        val visitNavigation = rememberNavController()
        val visitEntry by visitNavigation.currentBackStackEntryAsState()
        val exploring = visitEntry?.destination?.route == NavigationDestination.Exploration.route
        val visitOwner = remember { object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        } }
        var ready by remember { mutableStateOf(false) }
        DisposableEffect(services) { onDispose {
            visitOwner.viewModelStore.clear()
        } }
        LaunchedEffect(services) {
            ready = try {
                startScenario(services, scenario.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("BurgQuest", "Could not start ${scenario.id}", error)
                false
            }
            if (!ready) ending = BurgQuestEnding.START_FAILED
        }
        Box(Modifier.fillMaxSize()) {
            if (ready) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides visitOwner) {
                    CampaignNavigationHost(
                        navController = visitNavigation,
                        showCombatActionText = showCombatActionText,
                        providedServices = services,
                        initialDestination = NavigationDestination.Exploration.route,
                        demoEnemies = BurgQuestDemo.enemies(scenario.id),
                        demoPaused = showGuide || showDemoMenu || showHomecoming,
                        onDemoRootBack = { showDemoMenu = true },
                        onDemoExit = { ending = BurgQuestEnding.FINISHED },
                        onDemoCombatResult = { result ->
                            ending = when (result.outcome) {
                                CombatResultPayload.Outcome.VICTORY -> BurgQuestEnding.VICTORY
                                CombatResultPayload.Outcome.DEFEAT -> BurgQuestEnding.DEFEAT
                                CombatResultPayload.Outcome.RETREAT -> BurgQuestEnding.RETREAT
                            }
                        }
                    )
                }
                Surface(
                    modifier = if (exploring) {
                        Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 12.dp, bottom = 12.dp)
                    } else {
                        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 4.dp, end = 8.dp)
                    },
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 4.dp
                ) {
                    TextButton(onClick = { showDemoMenu = true }) { Text("Demo") }
                }
                if (showHomecoming && exploring) BurgQuestHomecoming(
                    audioCuePlayer = services.audioCuePlayer,
                    onComplete = { showHomecoming = false; showGuide = true }
                )
            } else {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    if (showDemoMenu && ending == null) AlertDialog(
        onDismissRequest = { showDemoMenu = false },
        title = { Text("Demo menu") },
        text = { Column {
            TextButton(onClick = { showDemoMenu = false; showGuide = true }) { Text("Demo guide") }
            TextButton(onClick = { showDemoMenu = false; ending = BurgQuestEnding.FINISHED }) { Text("Finish demo") }
        } },
        confirmButton = { TextButton(onClick = { showDemoMenu = false }) { Text("Resume demo") } }
    )
    var homecomingTutorialShown by remember { mutableStateOf(false) }
    val showHomecomingTutorialIfNeeded = {
        if (launch.isHomecoming && !homecomingTutorialShown) {
            homecomingTutorialShown = true
            services.promptManager.enqueue(
                TutorialPrompt(
                    TutorialEntry(
                        key = "tut_astra_sampler_movement",
                        context = "Exploration",
                        message = "Swipe to move between rooms, or tap the direction arrows. Head west to find the workshop."
                    )
                )
            )
        }
    }
    if ((!begun || showGuide) && ending == null) AlertDialog(
        onDismissRequest = {
            if (begun) {
                showGuide = false
                showHomecomingTutorialIfNeeded()
            } else onExit()
        },
        title = { Text(if (launch.isHomecoming) "Make yourself at home" else scenario.title) },
        text = {
            val introduction = if (!begun && scenario.id == "burgfest_combat") {
                BurgQuestDemo.combatIntroduction
            } else BurgQuestDemo.briefing(scenario.id)
            Text(introduction + if (!begun) "\n\nA fresh demo for every visitor. Your campaign saves stay safe." else "",
                Modifier.verticalScroll(rememberScrollState()))
        },
        confirmButton = { TextButton(onClick = {
            begun = true
            showGuide = false
            showHomecomingTutorialIfNeeded()
        }) {
            Text(if (launch.isHomecoming) "Continue" else if (begun) "Back to demo" else "Begin demo")
        } },
        dismissButton = if (launch.isHomecoming) null else {
            { TextButton(onClick = onExit) { Text("Title screen") } }
        }
    )
    ending?.let { result ->
        if (!chooseAnother) BurgQuestFinishDialog(
            ending = result,
            onVisitAstra = { onChoose(BurgQuestLaunch.homecoming()) },
            onRetry = onRetry,
            onFinish = { ending = BurgQuestEnding.FINISHED },
            onChoose = { chooseAnother = true },
            onExit = onExit
        )
    }
    if (chooseAnother) BurgfestDemoDialog(onLaunch = onChoose, onDismiss = { chooseAnother = false })
}

@Composable
internal fun BurgQuestFinishDialog(
    ending: BurgQuestEnding,
    onVisitAstra: () -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
    onChoose: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (ending == BurgQuestEnding.FINISHED) "Until the next adventure" else "BurgQuest showcase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(ending.message)
                if (ending.isCombatResult) {
                    if (ending != BurgQuestEnding.VICTORY) {
                        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again — fresh start") }
                    }
                    Button(onClick = onVisitAstra, modifier = Modifier.fillMaxWidth()) { Text("Visit the Astra") }
                    Text("The Astra starts fresh, with a rested crew and stocked supplies.", style = MaterialTheme.typography.bodySmall)
                    if (ending == BurgQuestEnding.VICTORY) {
                        TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Play again — fresh start") }
                    }
                    TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish demo") }
                } else {
                    Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Return to title") }
                }
                TextButton(onClick = onChoose, modifier = Modifier.fillMaxWidth()) { Text("Explore another demo") }
            }
        },
        confirmButton = {}
    )
}
