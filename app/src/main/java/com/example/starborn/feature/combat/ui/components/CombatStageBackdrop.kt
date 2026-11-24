package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.viewmodel.CombatTutorialState
import com.example.starborn.feature.combat.viewmodel.CombatTutorialStep
import com.example.starborn.ui.components.maestroTag
import com.example.starborn.ui.theme.themeColor
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CombatEncounterHeader(
    locationTitle: String?,
    statusText: String,
    targetMode: Boolean,
    onCancelTarget: (() -> Unit)?,
    theme: Theme?,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = panel.copy(alpha = if (highContrastMode) 0.94f else 0.70f),
        border = BorderStroke(1.dp, border.copy(alpha = if (highContrastMode) 0.72f else 0.46f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 9.dp, end = 12.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = locationTitle?.uppercase(Locale.getDefault()) ?: "CURRENT AREA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        ),
                        color = accent.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (targetMode || statusText.length > 16) 13.sp else 14.sp,
                            lineHeight = 17.sp
                        ),
                        color = if (targetMode) Color(0xFFFFC8B8) else Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                actions?.invoke(this)
                if (targetMode && onCancelTarget != null) {
                    TextButton(onClick = onCancelTarget) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF7E78),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.78f),
                                border.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CombatPlaytestControls(
    isOpMode: Boolean,
    onToggleOpMode: () -> Unit,
    onInstaWin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isOpMode) Color(0xFFFFA000).copy(alpha = 0.35f) else Color(0xFF0D1B2A).copy(alpha = 0.70f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isOpMode) Color(0xFFFFD54F) else Color(0xFF5CCBE8).copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .testTag("combat-op-toggle")
                .semantics { contentDescription = "OP Mode" }
                .clickable(onClick = onToggleOpMode)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isOpMode) "⚡ OP: ON" else "⚡ OP",
                    color = if (isOpMode) Color(0xFFFFE082) else Color(0xFF7BE4FF),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFD32F2F).copy(alpha = 0.35f),
            border = BorderStroke(1.dp, Color(0xFFFF8A80)),
            modifier = Modifier
                .testTag("combat-op-win")
                .semantics { contentDescription = "Instant Win" }
                .clickable(onClick = onInstaWin)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "💥 Win",
                    color = Color(0xFFFFCDD2),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CombatBuildHintOverlay(
    title: String,
    message: String,
    theme: Theme?,
    highContrastMode: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
            border = BorderStroke(1.dp, border.copy(alpha = 0.72f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = CombatNameFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent.copy(alpha = 0.92f),
                            contentColor = Color(0xFF041018)
                        )
                    ) {
                        Text(
                            text = "Got it",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CombatTutorialOverlay(
    tutorial: CombatTutorialState,
    theme: Theme?,
    highContrastMode: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    val title = com.example.starborn.feature.combat.presentation.CombatTutorialCopy.title(tutorial)
    val message = com.example.starborn.feature.combat.presentation.CombatTutorialCopy.message(tutorial)
    if (tutorial.showsModal) {
        androidx.activity.compose.BackHandler(onBack = onContinue)
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
                border = BorderStroke(1.dp, border.copy(alpha = 0.72f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tutorial.canSkip) {
                            TextButton(onClick = onSkip) {
                                Text("Skip Training", color = Color.White.copy(alpha = 0.72f))
                            }
                        }
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(alpha = 0.92f),
                                contentColor = Color(0xFF041018)
                            )
                        ) {
                            Text(
                                text = com.example.starborn.feature.combat.presentation.CombatTutorialCopy.continueLabel(tutorial),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp), color = panel.copy(alpha = .96f),
            border = BorderStroke(1.dp, accent.copy(alpha = .65f))) {
            Text(message, Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun BattleStageBackdrop(
    accentColor: Color,
    borderColor: Color,
    panelColor: Color,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val motion = rememberInfiniteTransition(label = "battle_stage_backdrop")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battle_stage_phase"
    )
    Canvas(modifier = modifier) {
        val railAlpha = if (highContrastMode) 0.34f else 0.22f
        val centerY = size.height * 0.47f
        val laneHeight = size.height * 0.18f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    panelColor.copy(alpha = if (highContrastMode) 0.42f else 0.26f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, centerY - laneHeight / 2f),
            size = Size(size.width, laneHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )
        val pulse = 0.55f + 0.45f * sin(phase * 2f * PI).toFloat()
        drawLine(
            color = accentColor.copy(alpha = 0.18f + 0.10f * pulse),
            start = Offset(size.width * 0.08f, centerY),
            end = Offset(size.width * 0.92f, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.12f, size.height * 0.20f),
            end = Offset(size.width * 0.88f, size.height * 0.20f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.12f, size.height * 0.76f),
            end = Offset(size.width * 0.88f, size.height * 0.76f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        val tickCount = 7
        repeat(tickCount) { index ->
            val x = size.width * (0.16f + index * 0.68f / (tickCount - 1).coerceAtLeast(1))
            val alpha = 0.08f + 0.08f * ((phase + index * 0.13f) % 1f)
            drawLine(
                color = accentColor.copy(alpha = alpha),
                start = Offset(x, centerY - laneHeight * 0.28f),
                end = Offset(x, centerY + laneHeight * 0.28f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Ambient floating battle motes / embers
        val moteCount = 12
        repeat(moteCount) { i ->
            val moteSeed = i * 0.0833f
            val moteProgress = (phase + moteSeed) % 1f
            val moteX = size.width * (0.05f + 0.9f * ((moteSeed * 7.3f) % 1f)) + (sin(moteProgress * 2f * PI.toFloat() + i) * 14.dp.toPx())
            val moteY = size.height * (1f - moteProgress)
            val moteAlpha = (sin(moteProgress * PI.toFloat()) * (if (highContrastMode) 0.35f else 0.24f)).coerceIn(0f, 1f)
            val moteRadius = if (i % 3 == 0) 2.2.dp.toPx() else 1.4.dp.toPx()
            drawCircle(
                color = accentColor.copy(alpha = moteAlpha),
                radius = moteRadius,
                center = Offset(moteX, moteY)
            )
        }
    }
}

@Composable
fun CombatPauseButton(
    onClick: () -> Unit,
    theme: Theme?,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val buttonSize = if (largeTouchTargets) 48.dp else 38.dp
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = Color(0xFF0D1B2A).copy(alpha = if (highContrastMode) 0.94f else 0.78f),
        border = BorderStroke(1.dp, border.copy(alpha = if (highContrastMode) 0.80f else 0.56f)),
        modifier = modifier
            .size(buttonSize)
            .maestroTag("combat-pause-button")
            .semantics { contentDescription = "Pause & Settings" }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(if (largeTouchTargets) 22.dp else 18.dp)
            )
        }
    }
}

@Composable
fun CombatPauseOverlay(
    theme: Theme?,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    musicVolume: Float,
    sfxVolume: Float,
    voiceVolume: Float,
    suppressScreenshake: Boolean,
    suppressFlashes: Boolean,
    disableHaptics: Boolean,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleScreenshakeDisabled: (Boolean) -> Unit,
    onToggleFlashesDisabled: (Boolean) -> Unit,
    onToggleHapticsDisabled: (Boolean) -> Unit,
    onToggleLargeTouchTargets: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit,
    canRetreat: Boolean,
    retreatUnavailableHint: String?,
    onRetreat: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    val buttonMinHeight = if (largeTouchTargets) 52.dp else 44.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC03070E))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onResume
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.99f else 0.96f),
                border = BorderStroke(
                    width = 1.25.dp,
                    color = border.copy(alpha = if (highContrastMode) 0.88f else 0.68f)
                ),
                shadowElevation = 22.dp,
                tonalElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                            )
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "TACTICAL STANDBY · ATB HELD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = accent.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Combat Paused",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onResume) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close pause menu",
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.82f),
                                        border.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Audio Mix Section
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0B1420),
                        border = BorderStroke(1.dp, border.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AUDIO MIX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = accent
                            )
                            CombatPauseVolumeSlider(
                                label = "Music",
                                value = musicVolume,
                                accent = accent,
                                onValueChange = onMusicVolumeChange
                            )
                            CombatPauseVolumeSlider(
                                label = "Sound Effects",
                                value = sfxVolume,
                                accent = accent,
                                onValueChange = onSfxVolumeChange
                            )
                            CombatPauseVolumeSlider(
                                label = "Voice",
                                value = voiceVolume,
                                accent = accent,
                                onValueChange = onVoiceVolumeChange
                            )
                        }
                    }

                    // Comfort & Accessibility Toggles
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0B1420),
                        border = BorderStroke(1.dp, border.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "COMFORT & ACCESSIBILITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = accent
                            )
                            val toggles = listOf<Triple<String, Boolean, (Boolean) -> Unit>>(
                                Triple("Screen Shake", !suppressScreenshake) { onToggleScreenshakeDisabled(!it) },
                                Triple("Combat Flashes", !suppressFlashes) { onToggleFlashesDisabled(!it) },
                                Triple("Haptic Rumble", !disableHaptics) { onToggleHapticsDisabled(!it) },
                                Triple("Large Buttons", largeTouchTargets, onToggleLargeTouchTargets),
                                Triple("High Contrast", highContrastMode, onToggleHighContrast)
                            )
                            val cols = if (largeFont) 1 else 2
                            toggles.chunked(cols).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (label, checked, onToggle) ->
                                        CombatPauseToggleChip(
                                            label = label,
                                            checked = checked,
                                            accent = accent,
                                            border = border,
                                            largeTouchTargets = largeTouchTargets,
                                            onCheckedChange = onToggle,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size < cols) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (!canRetreat && !retreatUnavailableHint.isNullOrBlank()) {
                        Text(
                            text = retreatUnavailableHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9EB4C4)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onRetreat,
                            enabled = canRetreat,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (canRetreat) border.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.16f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF141C28),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF101620),
                                disabledContentColor = Color.White.copy(alpha = 0.38f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = buttonMinHeight)
                                .maestroTag("combat-pause-retreat")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retreat",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Button(
                            onClick = onResume,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color(0xFF041018)
                            ),
                            modifier = Modifier
                                .weight(1.25f)
                                .heightIn(min = buttonMinHeight)
                                .maestroTag("combat-pause-resume")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Resume",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
            CommandPaletteMarkers(
                color = accent,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun CombatPauseVolumeSlider(
    label: String,
    value: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFDCE7F0)
            )
            Text(
                text = "${(value.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )
    }
}

@Composable
private fun CombatPauseToggleChip(
    label: String,
    checked: Boolean,
    accent: Color,
    border: Color,
    largeTouchTargets: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val minHeight = if (largeTouchTargets) 48.dp else 40.dp
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (checked) accent.copy(alpha = 0.16f) else Color(0xFF121B28),
        border = BorderStroke(
            1.dp,
            if (checked) accent.copy(alpha = 0.72f) else border.copy(alpha = 0.26f)
        ),
        modifier = modifier
            .heightIn(min = minHeight)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (checked) Color.White else Color(0xFF9FB4C4),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (checked) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold
                ),
                color = if (checked) accent else Color(0xFF6E8494)
            )
        }
    }
}
