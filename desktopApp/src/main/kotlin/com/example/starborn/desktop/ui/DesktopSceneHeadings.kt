package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.UserSettings

@Composable
internal fun DesktopCombatReadyPrompt(accent: Color, ready: Boolean, settings: UserSettings) {
    val animate = ready && !settings.disableScreenshake && !settings.disableFlashes
    val alpha = if (animate) {
        val pulse = rememberInfiniteTransition(label = "ready prompt")
        val value by pulse.animateFloat(.55f, .95f,
            infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "ready accent")
        value
    } else .85f
    val tint = if (settings.highContrastMode) Color.White else accent
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).testTag("combat-ready-prompt"),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Rounded.TouchApp, null, Modifier.size(28.dp), tint = tint.copy(alpha = alpha))
        Text("Select a ready character", color = if (settings.highContrastMode) Color.White else Color(0xFFF3F4F5),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold))
        Canvas(Modifier.width(64.dp).height(3.dp)) { drawRoundRect(tint.copy(alpha = alpha)) }
    }
}

@Composable
internal fun DesktopRoomHeading(title: String, accent: Color, compact: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.fillMaxWidth().testTag("exploration-room-title"), color = accent,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = if (compact) 30.sp else 36.sp,
                lineHeight = if (compact) 36.sp else 42.sp, fontWeight = FontWeight.ExtraBold))
        Canvas(Modifier.width(64.dp).height(2.dp)) { drawRoundRect(accent.copy(alpha = .8f)) }
    }
}
