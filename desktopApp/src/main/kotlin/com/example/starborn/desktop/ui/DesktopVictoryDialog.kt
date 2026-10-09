package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import com.example.starborn.desktop.DesktopAppServices
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.navigation.CombatResultPayload

internal enum class DesktopVictoryStage {
    SPOILS,
    LEVEL_UPS
}

@Composable
internal fun DesktopVictoryDialog(
    services: DesktopAppServices,
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String,
    portraitById: Map<String, String>,
    highContrastMode: Boolean,
    onContinue: () -> Unit,
    largeTouchTargets: Boolean = false
) {
    var stage by remember(payload) { mutableStateOf(DesktopVictoryStage.SPOILS) }
    var completed by remember(payload) { mutableStateOf(false) }
    var settled by remember(payload) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(stage) { settled = false; focus.requestFocus(); delay(180); settled = true }
    fun advance() {
        if (!settled || completed) return
        settled = false
        if (stage == DesktopVictoryStage.SPOILS && payload.levelUps.isNotEmpty()) stage = DesktopVictoryStage.LEVEL_UPS
        else { completed = true; onContinue() }
    }
    val nameFont = remember(services.assetProvider) {
        services.assetProvider.open("font/orbitron_medium.ttf")?.use {
            FontFamily(Font("starborn-combat-name", it.readBytes(), weight = FontWeight.Medium))
        } ?: FontFamily.Default
    }
    MaterialTheme(typography = MaterialTheme.typography.copy(titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = nameFont))) {
        val buttonLabel = if (stage == DesktopVictoryStage.SPOILS && payload.levelUps.isNotEmpty()) {
            "Next"
        } else {
            "Continue"
        }
        val title = when (stage) {
            DesktopVictoryStage.SPOILS -> "Spoils Recovered"
            DesktopVictoryStage.LEVEL_UPS -> "Level Up!"
        }
        val eyebrow = when (stage) {
            DesktopVictoryStage.SPOILS -> "Battle Rewards"
            DesktopVictoryStage.LEVEL_UPS -> "Progression"
        }
        val panelColor = Color(0xFF21130D).copy(alpha = if (highContrastMode) 0.98f else 0.94f)
        val cardColor = Color(0xFF171A24).copy(alpha = if (highContrastMode) 0.98f else 0.92f)
        val borderColor = Color(0xFFFF922B)
        val accentColor = Color(0xFFFF922B)
        val titleIcon = when (stage) {
            DesktopVictoryStage.SPOILS -> Icons.Filled.EmojiEvents
            DesktopVictoryStage.LEVEL_UPS -> Icons.Outlined.School
        }
        val dialogMaxWidth = 470.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize().testTag("combat-victory")
                .background(Color.Black.copy(alpha = 0.66f))
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .focusRequester(focus).onPreviewKeyEvent {
                    if (it.key == Key.Enter) { if (it.type == KeyEventType.KeyUp) advance(); true }
                    else it.key == Key.Escape
                }.focusable()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val contentLimit = (maxHeight - 188.dp).coerceAtLeast(48.dp)
            Surface(
                color = panelColor,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 18.dp,
                tonalElevation = 8.dp,
                border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.74f)),
                modifier = Modifier.widthIn(max = dialogMaxWidth).fillMaxWidth().heightIn(max = maxHeight)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.16f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.66f)),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = titleIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = eyebrow,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(accentColor.copy(alpha = 0.48f))
                    )
                    Box(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                    when (stage) {
                        DesktopVictoryStage.SPOILS -> DesktopVictorySpoilsContent(
                            payload = payload,
                            itemNameResolver = itemNameResolver,
                            cardColor = cardColor,
                            accentColor = accentColor,
                            contentLimit = contentLimit
                        )
                        DesktopVictoryStage.LEVEL_UPS -> DesktopVictoryLevelUpContent(
                            levelUps = payload.levelUps,
                            portraitById = portraitById,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            cardColor = cardColor,
                            highContrastMode = highContrastMode,
                            services = services,
                            contentLimit = contentLimit
                        )
                    }
                    }
                    Button(
                        onClick = ::advance, enabled = settled && !completed,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (largeTouchTargets) 56.dp else 50.dp)
                    ) {
                        Text(
                            text = buttonLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopVictorySpoilsContent(
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String,
    cardColor: Color,
    accentColor: Color,
    contentLimit: Dp = 300.dp
) {
    val resourceEntries = buildList {
        if (payload.rewardXp > 0) add("Experience" to "+${payload.rewardXp} XP")
        if (payload.rewardAp > 0) add("Ability Points" to "+${payload.rewardAp} AP")
        if (payload.rewardCredits > 0) add("Credits" to "+${payload.rewardCredits}")
    }
    val itemEntries = payload.rewardItems.entries
        .filter { it.value > 0 }
        .sortedBy { itemNameResolver(it.key) }

    if (resourceEntries.isEmpty() && itemEntries.isEmpty()) {
        Text(
            text = "No spoils collected.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    LazyColumn(Modifier.fillMaxWidth().heightIn(max = contentLimit), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(resourceEntries.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) -> DesktopVictoryRewardStatCard(label, value, cardColor, accentColor, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (itemEntries.isNotEmpty()) {
            item { Text("Loot", style = MaterialTheme.typography.titleMedium, color = Color.White) }
            items(itemEntries, key = { it.key }) { entry ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(itemNameResolver(entry.key), Modifier.weight(1f), color = Color.White)
                    Text("\u00d7${entry.value}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun DesktopVictoryRewardStatCard(
    label: String,
    value: String,
    cardColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = cardColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.54f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
internal fun DesktopVictoryLevelUpContent(
    levelUps: List<LevelUpSummary>,
    portraitById: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    cardColor: Color,
    highContrastMode: Boolean,
    services: DesktopAppServices,
    contentLimit: Dp = 340.dp
) {
    if (levelUps.isEmpty()) {
        Text(
            text = "No level ups recorded.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = contentLimit),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(levelUps, key = { summary -> summary.characterId }) { summary ->
            DesktopVictoryLevelUpCard(
                summary = summary,
                portraitById = portraitById,
                accentColor = accentColor,
                borderColor = borderColor,
                cardColor = cardColor,
                highContrastMode = highContrastMode, services = services
            )
        }
    }
}

@Composable
internal fun DesktopVictoryLevelUpCard(
    summary: LevelUpSummary,
    portraitById: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    cardColor: Color,
    highContrastMode: Boolean,
    services: DesktopAppServices
) {
    val portraitPath = portraitById[summary.characterId]
        ?: "images/characters/emotes/${summary.characterId}_cool.png"
    val portraitPainter = rememberDesktopAssetPainter(portraitPath, services.assetProvider)

    Surface(
        color = cardColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.1.dp, borderColor.copy(alpha = 0.44f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B0F15))
                        .border(1.dp, accentColor.copy(alpha = 0.56f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = portraitPainter,
                        contentDescription = summary.characterName,
                        modifier = Modifier
                            .matchParentSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = summary.characterName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "LEVEL ${summary.newLevel}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }

            if (summary.unlockedSkills.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = Color(0xFFFFE082)
                        )
                        Text(
                            text = if (summary.unlockedSkills.size == 1) "New Skill" else "New Skills",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFE082)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.widthIn(max = 1200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(summary.unlockedSkills, key = { skill -> skill.id }) { skill ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, Color(0xFFFFE082).copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = skill.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
