package com.example.starborn.desktop.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.combat.presentation.CombatTutorialCopy
import com.example.starborn.feature.combat.viewmodel.CombatTutorialState

/** Android's tutorial framing, sized for a desktop battlefield. Shared copy and gating remain canonical. */
@Composable
internal fun DesktopCombatTutorialOverlay(
    tutorial: CombatTutorialState,
    accent: Color,
    highContrast: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val panel = if (highContrast) Color.Black else Color(0xF0061018)
    val border = if (highContrast) Color.White else accent.copy(alpha = .72f)
    if (tutorial.showsModal) {
        Dialog(onDismissRequest = {}, properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false
        )) {
            Surface(Modifier.widthIn(max = 540.dp).fillMaxWidth().padding(20.dp),
                shape = RoundedCornerShape(16.dp), color = panel,
                border = BorderStroke(1.dp, border), shadowElevation = 12.dp) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(CombatTutorialCopy.title(tutorial), style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(CombatTutorialCopy.message(tutorial), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .88f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (tutorial.canSkip) TextButton(onClick = onSkip) { Text("Skip Training", color = Color.White.copy(alpha = .72f)) }
                        Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(
                            containerColor = accent, contentColor = Color(0xFF041018))) {
                            Text(CombatTutorialCopy.continueLabel(tutorial), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 480.dp).padding(horizontal = 20.dp),
                shape = RoundedCornerShape(999.dp), color = panel,
                border = BorderStroke(1.2.dp, border), shadowElevation = 10.dp) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(999.dp)))
                    Text(CombatTutorialCopy.message(tutorial), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
internal fun DesktopCombatActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.ArrowForward,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) .6f else .15f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0B1820), contentColor = Color.White,
            disabledContainerColor = Color(0xFF081017), disabledContentColor = Color(0xFF60717B)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .3f))
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
