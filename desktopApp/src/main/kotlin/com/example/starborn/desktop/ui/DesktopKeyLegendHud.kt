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
import com.example.starborn.desktop.DesktopAction
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
                .heightIn(min = 28.dp)
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
                    if (onClick != null) {
                        role = Role.Button
                        if (enabled) this.onClick { onClick(); true } else disabled()
                    }
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
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
@OptIn(ExperimentalLayoutApi::class)
fun DesktopExplorationKeyLegendHud(
    ui: ExplorationUiState,
    blocked: Boolean,
    modifier: Modifier = Modifier,
    onInteractFirst: () -> Unit = {},
    onChooseActions: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onQuickSave: suspend () -> Boolean = { false },
    onOpenControls: () -> Unit = {},
    onOpenMenu: () -> Unit = {}
) {
    val bindings = LocalDesktopKeyBindings.current
    val coroutineScope = rememberCoroutineScope()
    var saveNotice by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                    DesktopKeyBadge(
                        keyGlyph = bindings.label(DesktopAction.QUICK_SAVE),
                        label = if (saving) "Saving..." else saveNotice ?: "Save",
                        highlighted = saveNotice == "Saved!",
                        enabled = !blocked && !saving,
                        tooltip = "Quick save game state [${bindings.label(DesktopAction.QUICK_SAVE)}]",
                        onClick = {
                            if (!blocked && !saving) {
                                coroutineScope.launch {
                                    saving = true
                                    val success = try { onQuickSave() }
                                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                        catch (_: Exception) { false }
                                    saving = false
                                    saveNotice = if (success) "Saved!" else "Save failed"
                                    delay(1800)
                                    saveNotice = null
                                }
                            }
                        }
                    )

                    DesktopKeyBadge(
                        keyGlyph = bindings.label(DesktopAction.HELP),
                        label = "Controls",
                        enabled = true,
                        tooltip = "View keyboard and mouse controls [${bindings.label(DesktopAction.HELP)}]",
                        onClick = onOpenControls
                    )
                // Menu
                DesktopKeyBadge(
                    keyGlyph = bindings.label(DesktopAction.MENU),
                    label = "Menu",
                    highlighted = true,
                    enabled = !blocked,
                    tooltip = "Open field menu [${bindings.label(DesktopAction.MENU)}]",
                    onClick = onOpenMenu
                )
            }

            // Secondary contextual hotkeys strip
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DesktopKeyBadge(
                    keyGlyph = listOf(DesktopAction.NORTH, DesktopAction.WEST, DesktopAction.SOUTH, DesktopAction.EAST).joinToString("/") { bindings.label(it) },
                    label = "Move",
                    enabled = !blocked,
                    tooltip = "Move with the displayed keys or arrow keys"
                )

                DesktopKeyBadge(
                    keyGlyph = bindings.label(DesktopAction.ACTIONS), label = "Actions",
                    enabled = !blocked, tooltip = "Choose a room interaction", onClick = onChooseActions
                )
                if (hasInteractiveActions) {
                    DesktopKeyBadge(
                        keyGlyph = bindings.label(DesktopAction.INTERACT),
                        label = "Interact",
                        highlighted = true,
                        enabled = !blocked,
                        tooltip = "Use the first available room action [${bindings.label(DesktopAction.INTERACT)}]",
                        onClick = onInteractFirst
                    )
                }

                if (canMap) {
                    DesktopKeyBadge(
                        keyGlyph = bindings.label(DesktopAction.MAP),
                        label = "Map",
                        highlighted = ui.canReturnToHub,
                        enabled = !blocked,
                        tooltip = "Open field map [${bindings.label(DesktopAction.MAP)}]",
                        onClick = onOpenMap
                    )
                }

                DesktopKeyBadge(
                    keyGlyph = bindings.label(DesktopAction.INVENTORY),
                    label = "Items",
                    enabled = !blocked,
                    tooltip = "Open inventory [${bindings.label(DesktopAction.INVENTORY)}]",
                    onClick = onOpenInventory
                )
            }
        }
    }
}
