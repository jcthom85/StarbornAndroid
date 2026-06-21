package com.example.starborn.feature.mainmenu.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.BuildConfig
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.feature.mainmenu.MainMenuViewModel
import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.feature.mainmenu.DebugScenarioCategory
import com.example.starborn.feature.mainmenu.DebugScenarioDestination
import com.example.starborn.ui.components.SaveLoadDialog
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val TitleGold = Color(0xFFFFC857)
private val TitleAmber = Color(0xFFFF9F2E)
private val TitleCyan = Color(0xFF63E6FF)
private val TitlePanel = Color(0xFF061018)
private val TitleText = Color(0xFFF7FBFF)
private val TitleMutedText = Color(0xFFD7EAF4)

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    audioCuePlayer: AudioCuePlayer,
    audioRouter: AudioRouter,
    onStartGame: () -> Unit,
    onStartHub: () -> Unit,
    onSlotLoaded: () -> Unit
) {
    var startingGame by remember { mutableStateOf(false) }
    var pendingScenario by remember { mutableStateOf<DebugScenario?>(null) }
    var showDebugBrowser by remember { mutableStateOf(false) }
    var saveLoadMode by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val menuTheme = remember { viewModel.mainMenuTheme }
    val accentColor = remember(menuTheme) {
        themeColor(menuTheme?.accent, Color(0xFF7BE4FF))
    }
    val panelColor = remember(menuTheme) {
        themeColor(menuTheme?.bg, Color(0xFF0B111A)).copy(alpha = 0.96f)
    }
    val borderColor = remember(menuTheme) {
        themeColor(menuTheme?.border, Color.White.copy(alpha = 0.16f))
    }
    val textColor = remember(menuTheme) {
        themeColor(menuTheme?.fg, Color.White)
    }
    val fadeInAlpha = remember { Animatable(1f) }
    val fadeOutAlpha = remember { Animatable(0f) }
    val versionLabel = remember {
        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    LaunchedEffect(Unit) {
        fadeInAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(audioCuePlayer, audioRouter) {
        audioCuePlayer.execute(
            audioRouter.commandsForLayerOverride(
                layer = AudioCueType.MUSIC,
                cueId = "music_title_theme",
                fadeMs = 900L,
                loop = true
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(saveLoadMode) {
        if (saveLoadMode != null) {
            viewModel.refreshSlots()
        }
    }

    LaunchedEffect(startingGame, pendingScenario) {
        if (!startingGame && pendingScenario == null) {
            fadeOutAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            return@LaunchedEffect
        }
        fadeOutAlpha.stop()
        fadeOutAlpha.snapTo(0f)
        // Smooth fade out before heavy work begins.
        fadeOutAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
        )
        // Give the frame a beat to present the black overlay before loading.
        delay(120)
        val onFailure: () -> Unit = {
            startingGame = false
            pendingScenario = null
        }
        val scenario = pendingScenario
        if (scenario != null) {
            viewModel.startDebugScenario(
                scenario = scenario,
                onComplete = {
                    when (scenario.destination) {
                        DebugScenarioDestination.HUB -> onStartHub()
                        DebugScenarioDestination.EXPLORATION -> onStartGame()
                    }
                },
                onFailure = onFailure
            )
        } else if (startingGame) {
            viewModel.startNewGame(onComplete = { onStartGame() }, onFailure = onFailure)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 760.dp

        Image(
            painter = painterResource(id = R.drawable.title_background_starborn),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.32f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.66f)
                        )
                    )
                )
        )

        StarbornTitleLogo(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(
                    start = 14.dp,
                    top = if (compactHeight) 10.dp else 18.dp,
                    end = 14.dp
                )
                .fillMaxWidth()
                .heightIn(max = if (compactHeight) 180.dp else 240.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 34.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .heightIn(max = if (compactHeight) 430.dp else 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 30.dp,
                    top = if (compactHeight) 22.dp else 34.dp,
                    end = 30.dp,
                    bottom = if (compactHeight) 22.dp else 34.dp
                ),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StarbornTitleButton(
                text = "New Game",
                onClick = { startingGame = true },
                enabled = !startingGame && pendingScenario == null,
                primary = true
            )
            if (BuildConfig.DEBUG) {
                StarbornTitleButton(
                    text = "Debug Scenarios",
                    onClick = { showDebugBrowser = true },
                    enabled = !startingGame && pendingScenario == null
                )
            }
            StarbornTitleButton(
                text = "Load Game",
                onClick = { saveLoadMode = "load" },
                enabled = !startingGame && pendingScenario == null
            )
        }

        Text(
            text = versionLabel,
            color = TitleMutedText.copy(alpha = 0.62f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 10.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        saveLoadMode?.let { mode ->
            SaveLoadDialog(
                mode = mode,
                slots = slots,
                onSave = { slot ->
                    scope.launch {
                        viewModel.saveSlot(slot)
                        viewModel.refreshSlots()
                        saveLoadMode = null
                    }
                },
                onLoad = { slot ->
                    scope.launch {
                        val success = viewModel.loadSlot(slot)
                        if (success) {
                            saveLoadMode = null
                            onSlotLoaded()
                        }
                    }
                },
                onDelete = { slot ->
                    scope.launch {
                        viewModel.deleteSlot(slot)
                    }
                },
                onRefresh = { viewModel.refreshSlots() },
                onDismiss = { saveLoadMode = null },
                accentColor = accentColor,
                panelColor = panelColor,
                borderColor = borderColor,
                textColor = textColor
            )
        }

        if (BuildConfig.DEBUG && showDebugBrowser) {
            DebugScenarioDialog(
                onLaunch = { scenario ->
                    showDebugBrowser = false
                    pendingScenario = scenario
                },
                onDismiss = { showDebugBrowser = false }
            )
        }

        if (fadeOutAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fadeOutAlpha.value))
            )
        }

        if (fadeInAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fadeInAlpha.value))
            )
        }
    }
}

@Composable
private fun DebugScenarioDialog(
    onLaunch: (DebugScenario) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<DebugScenarioCategory?>(null) }
    val filtered = remember(query, category) {
        DebugScenarioCatalog.scenarios.filter { scenario ->
            (category == null || scenario.category == category) &&
                (query.isBlank() || listOf(scenario.title, scenario.description, scenario.worldLabel)
                    .any { it.contains(query, ignoreCase = true) })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug Scenarios", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Launching replaces the current unsaved session.", color = TitleMutedText, fontSize = 13.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search world, quest, room, or system") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DebugScenarioCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option.takeUnless { it == category } },
                            label = { Text(option.label, fontSize = 11.sp) }
                        )
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(min = 180.dp, max = 430.dp)
                ) {
                    items(filtered, key = { it.id }) { scenario ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunch(scenario) }
                                .background(TitleCyan.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(scenario.title, fontWeight = FontWeight.Bold, color = TitleText)
                            Text(
                                "${scenario.category.label}  |  ${scenario.worldLabel}",
                                color = TitleCyan,
                                fontSize = 11.sp
                            )
                            Text(scenario.description, color = TitleMutedText, fontSize = 13.sp)
                        }
                    }
                    if (filtered.isEmpty()) {
                        item { Text("No matching scenarios.", color = TitleMutedText) }
                    }
                }
                HorizontalDivider(color = TitleCyan.copy(alpha = 0.2f))
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
        containerColor = TitlePanel,
        titleContentColor = TitleText,
        textContentColor = TitleMutedText
    )
}

@Composable
private fun StarbornTitleLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "starborn_title_logo")
    val bobOffset by transition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starborn_title_logo_bob"
    )
    val logoScale by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starborn_title_logo_scale"
    )
    val shimmerSweep by transition.animateFloat(
        initialValue = -0.55f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600),
            repeatMode = RepeatMode.Restart
        ),
        label = "starborn_title_logo_sweep"
    )

    Image(
        painter = painterResource(id = R.drawable.title_logo_starborn),
        contentDescription = "Starborn",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer {
                translationY = bobOffset
                scaleX = logoScale
                scaleY = logoScale
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val x = size.width * shimmerSweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.18f),
                            TitleCyan.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset(x - size.width * 0.28f, 0f),
                        end = Offset(x + size.width * 0.12f, size.height)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
    )
}

@Composable
private fun StarbornTitleButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 14.dp,
                disabledElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = TitleGold,
                contentColor = Color(0xFF1B1608),
                disabledContainerColor = TitlePanel.copy(alpha = 0.58f),
                disabledContentColor = TitleMutedText.copy(alpha = 0.62f)
            ),
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                letterSpacing = 0.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, if (enabled) TitleCyan.copy(alpha = 0.82f) else TitleCyan.copy(alpha = 0.22f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = TitlePanel.copy(alpha = if (enabled) 0.70f else 0.42f),
                contentColor = if (enabled) TitleText else TitleMutedText.copy(alpha = 0.56f),
                disabledContentColor = TitleMutedText.copy(alpha = 0.56f)
            ),
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TitleCyan.copy(alpha = if (enabled) 0.13f else 0.04f),
                            TitleAmber.copy(alpha = if (enabled) 0.08f else 0.02f),
                            Color.Transparent
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.sp
            )
        }
    }
}
