package com.example.starborn.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CyberKeyBackground = Color(0xEB07111A)
private val CyberKeyBorder = Color(0xFF33434E)
private val CyberKeyCyan = Color(0xFF63E6FF)
private val CyberKeyGold = Color(0xFFFFC857)
private val CyberKeyMuted = Color(0xFF91A8B3)

/**
 * High-precision PC / Steam Deck key glyph badge with tactile feedback.
 */
@Composable
fun DesktopKeyBadge(
    keyGlyph: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    tooltip: String? = null,
    onClick: (() -> Unit)? = null
) {
    val accentColor = if (highlighted) CyberKeyGold else CyberKeyCyan
    val borderCol = if (highlighted) accentColor.copy(alpha = 0.75f) else CyberKeyBorder
    val shape = RoundedCornerShape(6.dp)

    val content = @Composable {
        Surface(
            modifier = modifier
                .then(
                    if (onClick != null && enabled) {
                        Modifier
                            .desktopPointerHover(enabled)
                            .clickable(enabled = enabled, onClick = onClick)
                    } else Modifier
                )
                .clearAndSetSemantics {
                    testTag = "key-legend-$keyGlyph"
                    set(androidx.compose.ui.semantics.SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString(label), androidx.compose.ui.text.AnnotatedString("$label [$keyGlyph]")))
                    contentDescription = "$label [$keyGlyph]"
                },
            shape = shape,
            color = CyberKeyBackground,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = if (highlighted) 0.15f else 0.06f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Key glyph badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = keyGlyph,
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalStarbornFonts.current.orbitron,
                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                    )
                }

                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (highlighted) accentColor else CyberKeyMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = label,
                    color = if (highlighted) Color.White else Color(0xFFD8E2E8),
                    fontSize = 11.sp,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }

    if (!tooltip.isNullOrBlank()) {
        DesktopTooltip(tooltip, accent = accentColor) {
            content()
        }
    } else {
        content()
    }
}

/**
 * Contextual bottom HUD strip for Desktop Exploration.
 * Offers immediate key visibility, mouse clickability, and quick-save feedback.
 */
@Composable
fun DesktopExplorationKeyLegendHud(
    ui: ExplorationUiState,
    blocked: Boolean,
    modifier: Modifier = Modifier,
    onInteractFirst: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onQuickSave: () -> Unit = {},
    onOpenControls: () -> Unit = {},
    onOpenMenu: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var savedNotice by remember { mutableStateOf(false) }

    val hasInteractiveActions = ui.actions.isNotEmpty() && !blocked
    val canMap = !blocked

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CyberKeyBackground,
        border = BorderStroke(1.dp, CyberKeyBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Primary quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Quick Help & Save
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopKeyBadge(
                        keyGlyph = "F5",
                        label = if (savedNotice) "Saved!" else "Save",
                        icon = Icons.Rounded.Save,
                        highlighted = savedNotice,
                        enabled = !blocked,
                        tooltip = "Quick save game state [F5]",
                        onClick = {
                            if (!blocked) {
                                onQuickSave()
                                coroutineScope.launch {
                                    savedNotice = true
                                    delay(1800)
                                    savedNotice = false
                                }
                            }
                        }
                    )

                    DesktopKeyBadge(
                        keyGlyph = "H",
                        label = "Controls",
                        icon = Icons.Rounded.Keyboard,
                        enabled = true,
                        tooltip = "View tactical keyboard and mouse controls [H]",
                        onClick = onOpenControls
                    )
                }

                // Right: Menu
                DesktopKeyBadge(
                    keyGlyph = "Esc",
                    label = "Menu",
                    icon = Icons.Rounded.Menu,
                    highlighted = true,
                    enabled = !blocked,
                    tooltip = "Open tactical field menu [Esc / I]",
                    onClick = onOpenMenu
                )
            }

            // Secondary contextual hotkeys strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesktopKeyBadge(
                    keyGlyph = "WASD",
                    label = "Move",
                    enabled = !blocked,
                    tooltip = "Navigate north (W), south (S), west (A), east (D)"
                )

                if (hasInteractiveActions) {
                    DesktopKeyBadge(
                        keyGlyph = "E",
                        label = "Interact",
                        highlighted = true,
                        enabled = !blocked,
                        tooltip = "Trigger primary room action [E]",
                        onClick = onInteractFirst
                    )
                }

                if (canMap) {
                    DesktopKeyBadge(
                        keyGlyph = "M",
                        label = "Map",
                        highlighted = ui.canReturnToHub,
                        enabled = !blocked,
                        tooltip = "Open regional Star Map & Sector Telemetry [M]",
                        onClick = onOpenMap
                    )
                }

                DesktopKeyBadge(
                    keyGlyph = "I",
                    label = "Items",
                    enabled = !blocked,
                    tooltip = "Open field inventory [I]",
                    onClick = onOpenInventory
                )
            }
        }
    }
}
