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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.feature.mainmenu.MainMenuViewModel
import com.example.starborn.ui.components.SaveLoadDialog
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class StartMode { NORMAL, DEBUG }

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
    onSlotLoaded: () -> Unit
) {
    var pendingStartMode by remember { mutableStateOf<StartMode?>(null) }
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

    LaunchedEffect(pendingStartMode) {
        if (pendingStartMode == null) {
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
        val onFailure: () -> Unit = { pendingStartMode = null }
        when (pendingStartMode) {
            StartMode.NORMAL -> {
                viewModel.startNewGame(
                    onComplete = { onStartGame() },
                    onFailure = onFailure
                )
            }
            StartMode.DEBUG -> {
                viewModel.startNewGameWithFullInventory(
                    onComplete = { onStartGame() },
                    onFailure = onFailure
                )
            }
            null -> Unit
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
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = if (compactHeight) 22.dp else 34.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StarbornTitleButton(
                text = "New Game",
                onClick = { pendingStartMode = StartMode.NORMAL },
                enabled = pendingStartMode == null,
                primary = true
            )
            StarbornTitleButton(
                text = "Debug: Full Inventory",
                onClick = { pendingStartMode = StartMode.DEBUG },
                enabled = pendingStartMode == null
            )
            StarbornTitleButton(
                text = "Load Game",
                onClick = { saveLoadMode = "load" },
                enabled = pendingStartMode == null
            )
        }

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
