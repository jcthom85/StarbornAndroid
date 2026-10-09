package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Inspection-card visuals with real modal focus and the original runtime choices. */
@Composable
internal fun DesktopExplorationModal(message: String, onDismiss: () -> Unit, title: String? = null,
    actions: @Composable ColumnScope.() -> Unit) {
    val settings = LocalExplorationSettings.current
    val accent = if (settings.highContrastMode) Color.White else Color(0xFF8DE2FF)
    val availableHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } - 100.dp
    val scroll = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.padding(20.dp).widthIn(max = 620.dp).fillMaxWidth().heightIn(max = availableHeight.coerceAtLeast(200.dp)),
            color = if (settings.highContrastMode) Color.Black else Color(0xFF050B12), shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = .72f)), shadowElevation = 18.dp) {
            Column(Modifier.background(Brush.verticalGradient(listOf(accent.copy(alpha = .12f), Color.Transparent))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.headlineSmall, color = accent) }
                if (message.isNotBlank()) DesktopExplorationScrollPane(scroll, Modifier.weight(1f, fill = false).fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.width(4.dp).height(52.dp).background(accent, RoundedCornerShape(8.dp)))
                        Text(message, Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp))
                    }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
            }
        }
    }
}
