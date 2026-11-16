package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.data.local.EnvironmentalEffectsQuality
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SettingsTabContent(
    settings: SettingsUiState,
    accentColor: Color,
    borderColor: Color,
    showSaveData: Boolean = true,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit = {},
    onToggleLargeTouchTargets: (Boolean) -> Unit = {},
    onToggleScreenshakeDisabled: (Boolean) -> Unit = {},
    onToggleFlashesDisabled: (Boolean) -> Unit = {},
    onToggleHapticsDisabled: (Boolean) -> Unit = {},
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null,
    onDebugTinkeringTutorial: (() -> Unit)? = null,
    onOpenWalkthrough: (() -> Unit)? = null,
    onEnvironmentalPreview: (() -> Unit)? = null,
    onToggleModernFieldMenu: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val effectSettingsStore = remember(context) { UserSettingsStore(context) }
    val effectSettings by effectSettingsStore.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val scope = rememberCoroutineScope()
    var showDevTools by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Save & Session Card (In-Game Menu only)
        if (showSaveData) {
            SettingsCardContainer(title = "SAVE & SESSION", accentColor = FieldMenuDesign.gold) {
                SaveDataGrid(
                    onQuickSave = onQuickSave,
                    onSaveGame = onSaveGame,
                    onLoadGame = onLoadGame,
                    onReturnToTitle = onReturnToTitle
                )
            }
        }

        // 2. Audio & Visuals Card
        SettingsCardContainer(title = "AUDIO & VISUALS", accentColor = FieldMenuDesign.cyan) {
            CompactVolumeRow(
                label = "Music",
                value = settings.musicVolume,
                onValueChange = onMusicVolumeChange
            )
            CompactVolumeRow(
                label = "Sound Effects",
                value = settings.sfxVolume,
                onValueChange = onSfxVolumeChange
            )
            CompactVolumeRow(
                label = "Voice",
                value = settings.voiceVolume,
                onValueChange = onVoiceVolumeChange
            )

            HorizontalDivider(color = FieldMenuDesign.subtleBorder)

            // Environment FX Quality Pill Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Environment FX",
                    color = FieldMenuDesign.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FieldMenuDesign.deepCard)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EnvironmentalEffectsQuality.entries.forEach { quality ->
                        val active = effectSettings.environmentalEffectsQuality == quality
                        Surface(
                            onClick = {
                                scope.launch { effectSettingsStore.setEnvironmentalEffectsQuality(quality) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) FieldMenuDesign.primaryActionFill else Color.Transparent,
                            border = if (active) BorderStroke(1.dp, FieldMenuDesign.cyan) else null
                        ) {
                            Text(
                                text = quality.label,
                                color = if (active) FieldMenuDesign.text else FieldMenuDesign.textMuted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Comfort & Accessibility Grid (2-Column Tactical Toggle Chips)
        SettingsCardContainer(title = "COMFORT & ACCESSIBILITY", accentColor = FieldMenuDesign.mint) {
            val largeFont = LocalDensity.current.fontScale >= 1.3f
            val toggles = buildList<Triple<String, Boolean, (Boolean) -> Unit>> {
                add(Triple("Screen Shake", !settings.disableScreenshake) { onToggleScreenshakeDisabled(!it) })
                add(Triple("Combat Flashes", !settings.disableFlashes) { onToggleFlashesDisabled(!it) })
                add(Triple("Haptic Rumble", !settings.disableHaptics) { onToggleHapticsDisabled(!it) })
                add(Triple("Room Vignette", settings.vignetteEnabled, onToggleVignette))
                add(Triple("Tutorials", settings.tutorialsEnabled, onToggleTutorials))
                add(Triple("Large Buttons", settings.largeTouchTargets, onToggleLargeTouchTargets))
                add(Triple("High Contrast", settings.highContrastMode, onToggleHighContrast))
                onToggleModernFieldMenu?.let { toggle ->
                    add(Triple("Modern Menu", settings.modernFieldMenu, toggle))
                }
            }
            val cols = if (largeFont) 1 else 2
            toggles.chunked(cols).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (label, enabled, onToggle) ->
                        CompactToggleTile(
                            label = label,
                            checked = enabled,
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

        // 4. Collapsible Developer & Playtest Tools Footer
        if (onDebugTinkeringTutorial != null || onOpenWalkthrough != null || onEnvironmentalPreview != null) {
            Surface(
                onClick = { showDevTools = !showDevTools },
                shape = RoundedCornerShape(12.dp),
                color = FieldMenuDesign.panel,
                border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEVELOPER & PLAYTEST TOOLS",
                            color = FieldMenuDesign.textMuted,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        )
                        Text(
                            text = if (showDevTools) "Hide ▴" else "Show ▾",
                            color = FieldMenuDesign.cyan,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    if (showDevTools) {
                        onOpenWalkthrough?.let { open ->
                            OutlinedButton(
                                onClick = open,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.5f))
                            ) {
                                Text("Playtest walkthrough", color = FieldMenuDesign.text)
                            }
                        }
                        onEnvironmentalPreview?.let { open ->
                            OutlinedButton(
                                onClick = open,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
                            ) {
                                Text("Environmental preview", color = FieldMenuDesign.text)
                            }
                        }
                        if (onDebugTinkeringTutorial != null) {
                            Button(
                                onClick = onDebugTinkeringTutorial,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, FieldMenuDesign.gold),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FieldMenuDesign.primaryActionFill,
                                    contentColor = FieldMenuDesign.text
                                )
                            ) {
                                Text("⚡ FAST TEST: Tinkering Tutorial (Cryo-Inductor)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCardContainer(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = FieldMenuDesign.panel,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = accentColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
            )
            content()
        }
    }
}

@Composable
private fun CompactVolumeRow(
    label: String,
    value: Float,
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
                color = FieldMenuDesign.text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "${(value * 100).roundToInt()}%",
                color = FieldMenuDesign.cyan,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = FieldMenuDesign.gold,
                activeTrackColor = FieldMenuDesign.cyan,
                inactiveTrackColor = FieldMenuDesign.deepCard
            )
        )
    }
}

@Composable
private fun CompactToggleTile(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val minH = if (LocalFieldMenuLargeTargets.current) 54.dp else 46.dp
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.heightIn(min = minH),
        shape = RoundedCornerShape(10.dp),
        color = if (checked) FieldMenuDesign.primaryActionFill.copy(alpha = 0.7f) else FieldMenuDesign.deepCard,
        border = BorderStroke(
            1.dp,
            if (checked) FieldMenuDesign.mint.copy(alpha = 0.65f) else FieldMenuDesign.subtleBorder
        )
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
                color = FieldMenuDesign.text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (checked) FieldMenuDesign.mint.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(
                    1.dp,
                    if (checked) FieldMenuDesign.mint else FieldMenuDesign.subtleBorder
                )
            ) {
                Text(
                    text = if (checked) "ON" else "OFF",
                    color = if (checked) FieldMenuDesign.mint else FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SaveDataGrid(
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null
) {
    val modern = LocalModernFieldMenu.current
    val largeFont = LocalDensity.current.fontScale >= 1.3f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (largeFont) {
            SaveActionButton("Quick Save", "Checkpoint now", Icons.Rounded.FlashOn, FieldMenuDesign.gold, onQuickSave, Modifier.fillMaxWidth())
            SaveActionButton("Save Slot", "Pick a manual slot", Icons.Rounded.Save, FieldMenuDesign.cyan, onSaveGame, Modifier.fillMaxWidth())
            SaveActionButton("Load Slot", "Resume a saved game", Icons.Rounded.Download, FieldMenuDesign.cyan, onLoadGame, Modifier.fillMaxWidth())
            if (onReturnToTitle != null) {
                SaveActionButton(
                    label = "Save & Exit",
                    detail = "Quicksave & title screen",
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    accentColor = FieldMenuDesign.ember,
                    onClick = {
                        if (!modern) onQuickSave()
                        onReturnToTitle()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SaveActionButton("Quick Save", "Checkpoint now", Icons.Rounded.FlashOn, FieldMenuDesign.gold, onQuickSave, Modifier.weight(1f))
                SaveActionButton("Save Slot", "Manual slot", Icons.Rounded.Save, FieldMenuDesign.cyan, onSaveGame, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SaveActionButton("Load Slot", "Resume slot", Icons.Rounded.Download, FieldMenuDesign.cyan, onLoadGame, Modifier.weight(1f))
                if (onReturnToTitle != null) {
                    SaveActionButton(
                        label = "Save & Exit",
                        detail = "Title screen",
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                        accentColor = FieldMenuDesign.ember,
                        onClick = {
                            if (!modern) onQuickSave()
                            onReturnToTitle()
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SaveActionButton(
    label: String,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (LocalFieldMenuLargeTargets.current) 60.dp else 50.dp),
        shape = RoundedCornerShape(12.dp),
        color = FieldMenuDesign.elevatedPanel,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = FieldMenuDesign.text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

