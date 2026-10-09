package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.DesktopAppServices

internal val LocalExplorationSettings = staticCompositionLocalOf { UserSettings() }
internal val LocalExplorationAccent = staticCompositionLocalOf { Color(0xFFFF9F2E) }
internal class ExplorationDrawerState { var panel by mutableStateOf<String?>(null) }
internal val LocalExplorationDrawer = staticCompositionLocalOf<ExplorationDrawerState?> { null }

@Composable
internal fun DesktopExplorationTheme(services: DesktopAppServices, content: @Composable () -> Unit) {
    val settings by services.userSettingsStore.settings.collectAsState(initial = UserSettings())
    val base = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalExplorationSettings provides settings) {
        MaterialTheme(typography = desktopStarbornTypography(services), colorScheme = if (settings.highContrastMode)
            base.copy(surface = Color.Black, onSurface = Color.White, outline = Color.White, onSurfaceVariant = Color.White) else base,
            content = content)
    }
}

/** Draw feedback without consuming pointer events or adding another focus target. */
@Composable
internal fun Modifier.explorationFeedback(enabled: Boolean = true, accent: Color = Color(0xFF63E6FF)): Modifier {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val contrast = LocalExplorationSettings.current.highContrastMode
    return hoverable(interactions, enabled).onFocusChanged { focused = it.hasFocus }.drawWithContent {
        drawContent()
        if (enabled && (hovered || focused)) {
            val color = if (contrast) Color.White else accent
            drawRoundRect(color.copy(alpha = if (focused) .12f else .07f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            drawRoundRect(color.copy(alpha = if (focused) .9f else .5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()), style = Stroke(if (focused) 2.dp.toPx() else 1.dp.toPx()))
        }
    }
}

@Composable
internal fun DesktopExplorationScrollPane(state: ScrollState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        Box(Modifier.fillMaxWidth().padding(end = if (state.maxValue > 0) 10.dp else 0.dp).verticalScroll(state)) { content() }
        if (state.maxValue > 0) VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(7.dp),
            style = LocalScrollbarStyle.current.copy(unhoverColor = Color(0xFF63E6FF).copy(alpha = .4f), hoverColor = Color(0xFF63E6FF)))
    }
}

@Composable
internal fun DesktopExplorationActionRow(label: String, kind: String, enabled: Boolean, onClick: () -> Unit) {
    val settings = LocalExplorationSettings.current
    val accent = if (kind == "enemy") Color(0xFFFF8A80) else Color(0xFF63E6FF)
    val icon = when (kind) {
        "cook" -> androidx.compose.material.icons.Icons.Rounded.Restaurant
        "rest" -> androidx.compose.material.icons.Icons.Rounded.Hotel
        "travel" -> androidx.compose.material.icons.Icons.Rounded.ArrowForward
        "enemy" -> androidx.compose.material.icons.Icons.Rounded.Warning
        "item" -> androidx.compose.material.icons.Icons.Rounded.Inventory2
        else -> androidx.compose.material.icons.Icons.Rounded.Search
    }
    Surface(onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (settings.largeTouchTargets) 56.dp else 44.dp).explorationFeedback(enabled, accent),
        color = if (settings.highContrastMode) Color.Black else Color(0xFF0B1A24), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (settings.highContrastMode) Color.White else accent.copy(alpha = if (enabled) .3f else .12f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = accent.copy(alpha = if (enabled) 1f else .35f))
            Text(label, Modifier.weight(1f), color = Color.White.copy(alpha = if (enabled) 1f else .45f), style = MaterialTheme.typography.labelLarge)
        }
    }
}
