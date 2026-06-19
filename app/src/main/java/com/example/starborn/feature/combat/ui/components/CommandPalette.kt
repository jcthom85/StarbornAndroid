package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Player
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.ui.titleCaseName
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.theme.themeColor
import com.example.starborn.ui.vfx.GlowProgressBar
import java.util.Locale

@Composable
fun TargetInstructionBadge(text: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF0A0C10).copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )

            Text(
                text = text.uppercase(Locale.getDefault()),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 2f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CommandPalette(
    visible: Boolean,
    actor: Player?,
    currentHp: Int,
    maxHp: Int,
    atbProgress: Float,
    canAttack: Boolean,
    hasSkills: Boolean,
    hasItems: Boolean,
    onAttack: () -> Unit,
    onSkills: () -> Unit,
    onItems: () -> Unit,
    snackLabel: String,
    canSnack: Boolean,
    snackCooldown: Int,
    onSnack: () -> Unit,
    onRetreat: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme?,
    targetInstruction: String? = null,
    onCancelTarget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (actor == null) return
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { full -> full }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { full -> full / 2 })
    ) {
        val portraitPainter = rememberAssetPainter(actor.miniIconPath, painterResource(R.drawable.main_menu_background))
        val paletteBase = themeColor(theme?.bg, Color(0xFF0F1118))
        val paletteColor = paletteBase.copy(alpha = if (highContrastMode) 0.96f else 0.90f)
        val borderColor = themeColor(theme?.border, Color.White.copy(alpha = if (highContrastMode) 0.65f else 0.5f))
        val accentColor = themeColor(theme?.accent, Color(0xFFFF6A5F))
        val isTargetMode = !targetInstruction.isNullOrBlank()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = paletteColor,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 10.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(1.25.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF1C1F24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = portraitPainter,
                                contentDescription = actor.name,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = titleCaseName(actor.name),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${currentHp.coerceAtLeast(0)}/${maxHp.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.74f)
                            )
                            AtbBar(
                                progress = atbProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            )
                        }
                        if (isTargetMode && onCancelTarget != null) {
                            TextButton(onClick = onCancelTarget) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Cancel", color = Color.White)
                            }
                        }
                    }
                    if (isTargetMode) {
                        TargetInstructionBadge(
                            text = targetInstruction.orEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val commands = listOf(
                            CommandEntry("Attack", Icons.Rounded.Whatshot, canAttack, onAttack),
                            CommandEntry("Abilities", Icons.Rounded.AutoAwesome, hasSkills, onSkills),
                            CommandEntry("Items", Icons.Rounded.Inventory2, hasItems, onItems),
                            CommandEntry(snackLabel, Icons.Rounded.Restaurant, canSnack, onSnack, cooldown = snackCooldown),
                            CommandEntry("Retreat", Icons.Rounded.ExitToApp, true, onRetreat)
                        )
                        val rows = listOf(commands.take(3), commands.drop(3))
                        rows.forEach { chunk ->
                            if (chunk.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunk.forEach { entry ->
                                        CombatCommandButton(
                                            label = entry.label,
                                            icon = entry.icon,
                                            enabled = entry.enabled,
                                            onClick = entry.onClick,
                                            largeTouchTargets = largeTouchTargets,
                                            cooldownRemaining = entry.cooldown,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CommandPaletteMarkers(
                color = accentColor,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

data class CommandEntry(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val cooldown: Int = 0
)

@Composable
fun CombatCommandButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    largeTouchTargets: Boolean,
    cooldownRemaining: Int = 0,
    modifier: Modifier = Modifier
) {
    val minHeight = if (largeTouchTargets) 62.dp else 54.dp
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (enabled) Color(0xFF1E2534) else Color(0xFF1B1F29)
    Box(
        modifier = modifier
            .widthIn(min = 88.dp)
            .heightIn(min = minHeight)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = background,
            tonalElevation = if (enabled) 4.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (cooldownRemaining > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cooldownRemaining.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                )
            }
        }
    }
}

@Composable
fun CommandPaletteMarkers(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp, vertical = 8.dp)
    ) {
        val strokeWidth = 4.dp.toPx()
        val markerLength = 38.dp.toPx()
        val y = strokeWidth / 2f
        val tint = color.copy(alpha = 0.9f)
        drawLine(
            color = tint,
            start = Offset(0f, y),
            end = Offset(markerLength, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width - markerLength, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CombatItemsDialog(
    items: List<InventoryEntry>,
    theme: Theme?,
    highContrastMode: Boolean,
    onItemSelected: (InventoryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = CombatNameFont,
                        color = Color.White
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accent.copy(alpha = 0.80f), Color.Transparent)
                            )
                        )
                )
            }
        },
        text = {
            if (items.isEmpty()) {
                Text(
                    text = "No usable items available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = if (highContrastMode) 0.10f else 0.07f),
                            border = BorderStroke(1.dp, border.copy(alpha = 0.34f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = entry.item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "x${entry.quantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent.copy(alpha = 0.86f)
                                    )
                                }
                                Button(
                                    onClick = { onItemSelected(entry) },
                                    enabled = entry.quantity > 0
                                ) {
                                    Text("Use")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun ReadyAura(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ready_aura_transition")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_aura_pulse"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                alpha = 0.42f + (pulse - 0.94f) * 1.4f
            }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .border(BorderStroke(2.dp, color.copy(alpha = 0.65f)), CircleShape)
    )
}

@Composable
fun AtbBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2D9CFF)
) {
    val clamped = progress.coerceIn(0f, 1f)
    val ready = clamped >= 0.999f
    val barTransition = rememberInfiniteTransition(label = "atb_bar_transition")
    val tipGlow by barTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atb_tip_glow"
    )
    val readyPulse by barTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atb_ready_pulse"
    )
    val readyFlash = remember { Animatable(0f) }
    LaunchedEffect(ready) {
        if (ready) {
            readyFlash.snapTo(1f)
            readyFlash.animateTo(0f, tween(durationMillis = 360))
        } else {
            readyFlash.snapTo(0f)
        }
    }
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
    ) {
        if (ready) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.55f + 0.35f * readyPulse
                        scaleX = 1f + 0.04f * readyPulse
                        scaleY = 1f + 0.04f * readyPulse
                    }
            ) {
                val corner = CornerRadius(size.height, size.height)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.05f),
                            color.copy(alpha = 0.45f + 0.25f * readyPulse),
                            color.copy(alpha = 0.05f)
                        )
                    ),
                    size = size,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f + 0.12f * readyPulse),
                    size = size,
                    cornerRadius = corner,
                    style = Stroke(width = size.height * 0.2f)
                )
            }
        }
        GlowProgressBar(
            progress = clamped,
            modifier = Modifier
                .fillMaxSize(),
            trackColor = Color.Black.copy(alpha = 0.45f),
            glowColor = color.copy(alpha = 0.95f),
            height = 8.dp,
            glowWidth = 0.18f
        )
        if (clamped > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tipX = size.width * clamped
                drawRoundRect(
                    color = color.copy(alpha = 0.2f),
                    topLeft = Offset.Zero,
                    size = Size(size.width * clamped, size.height),
                    cornerRadius = CornerRadius(size.height, size.height)
                )
                drawCircle(
                    color = color.copy(alpha = 0.25f + 0.45f * tipGlow),
                    radius = size.height * 0.75f,
                    center = Offset(tipX, size.height / 2f)
                )
            }
        }
        if (readyFlash.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f * readyFlash.value),
                    size = size,
                    cornerRadius = CornerRadius(size.height, size.height)
                )
            }
        }
    }
}
