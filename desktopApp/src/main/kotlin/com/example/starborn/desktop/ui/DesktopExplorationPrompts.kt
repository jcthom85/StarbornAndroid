package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.prompt.*

/** Android's inspection card treatment, with the authored message supplied unchanged. */
@Composable
internal fun DesktopInspectionPrompt(message: String, onDismiss: () -> Unit, tapToDismiss: Boolean,
    title: String? = null, accent: Color = Color(0xFF8DE2FF)) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .56f))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            if (tapToDismiss) onDismiss()
        }, contentAlignment = Alignment.Center) {
        Surface(Modifier.padding(28.dp).widthIn(max = 620.dp).fillMaxWidth(), color = Color(0xF7050B12),
            shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, accent.copy(alpha = .72f)), shadowElevation = 18.dp) {
            Column(Modifier.background(Brush.verticalGradient(listOf(accent.copy(alpha = .12f), Color.Transparent)))
                .padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleLarge, color = accent) }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.width(4.dp).height(52.dp).background(accent, RoundedCornerShape(8.dp)))
                    Text(message, Modifier.weight(1f).heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                        color = Color.White, fontSize = 18.sp, lineHeight = 28.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Continue", color = accent) }
            }
        }
    }
}

/** Tutorial, milestone and acquisition notifications are banners on Android, not extra dialogs. */
@Composable
internal fun DesktopExplorationPromptBanner(services: com.example.starborn.desktop.DesktopAppServices, prompt: UIPrompt, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (prompt is ItemGrantedPrompt) {
        DesktopFieldMenuTheme(services) { DesktopItemGrantedBanner(services, prompt, onDismiss, modifier) }
        return
    }
    if (prompt is ItemBatchGrantedPrompt) {
        DesktopFieldMenuTheme(services) { DesktopItemBatchBanner(prompt.summary, onDismiss, modifier) }
        return
    }
    val title: String?
    val message: String
    val action: String
    val accent: Color
    when (prompt) {
        is TutorialPrompt -> { title = "Tutorial"; message = prompt.entry.message; action = "Continue"; accent = Color(0xFF7BE8FF) }
        is MilestonePrompt -> { title = null; message = prompt.event.message; action = "Dismiss"; accent = Color(0xFFFFD27F) }
        is ItemGrantedPrompt -> {
            title = prompt.itemName + if (prompt.quantity > 1) " x${prompt.quantity}" else ""
            message = prompt.description.orEmpty() + if (prompt.sequenceTotal > 1) "\n${prompt.sequenceIndex} / ${prompt.sequenceTotal}" else ""
            action = "Continue"; accent = Color(0xFF7BE8FF)
        }
        is ItemBatchGrantedPrompt -> { title = null; message = "Acquired ${prompt.summary}"; action = "Dismiss"; accent = Color(0xFF7BE8FF) }
        else -> return
    }
    Surface(modifier.widthIn(max = 620.dp).fillMaxWidth().clickable(enabled = prompt is ItemBatchGrantedPrompt, onClick = onDismiss), shape = RoundedCornerShape(14.dp), color = Color(0xFA07111A),
        border = BorderStroke(1.dp, accent.copy(alpha = .6f)), shadowElevation = 12.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            title?.let { Text(it, color = accent, style = MaterialTheme.typography.titleMedium) }
            if (message.isNotBlank()) Text(message, Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                color = Color.White, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text(action, color = accent) }
        }
    }
}
