package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.ui.background.rememberAssetPainter
import kotlin.math.abs

enum class VictoryDialogStage {
    SPOILS,
    LEVEL_UPS
}

@Composable
fun VictoryDialog(
    stage: VictoryDialogStage,
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String,
    portraitById: Map<String, String>,
    highContrastMode: Boolean,
    theme: Theme?,
    onContinue: () -> Unit
) {
    val buttonLabel = if (stage == VictoryDialogStage.SPOILS && payload.levelUps.isNotEmpty()) {
        "Next"
    } else {
        "Continue"
    }
    val title = when (stage) {
        VictoryDialogStage.SPOILS -> "Spoils Recovered"
        VictoryDialogStage.LEVEL_UPS -> "Level Up!"
    }
    val eyebrow = when (stage) {
        VictoryDialogStage.SPOILS -> "Battle Rewards"
        VictoryDialogStage.LEVEL_UPS -> "Progression"
    }
    val panelColor = Color(0xFF21130D).copy(alpha = if (highContrastMode) 0.98f else 0.94f)
    val cardColor = Color(0xFF171A24).copy(alpha = if (highContrastMode) 0.98f else 0.92f)
    val borderColor = Color(0xFFFF922B)
    val accentColor = Color(0xFFFF922B)
    val titleIcon = when (stage) {
        VictoryDialogStage.SPOILS -> Icons.Filled.EmojiEvents
        VictoryDialogStage.LEVEL_UPS -> Icons.Outlined.School
    }
    val dialogMaxWidth = 470.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = panelColor,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 18.dp,
            tonalElevation = 8.dp,
            border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.74f)),
            modifier = Modifier.widthIn(max = dialogMaxWidth)
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
                                fontFamily = CombatNameFont,
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
                when (stage) {
                    VictoryDialogStage.SPOILS -> VictorySpoilsContent(
                        payload = payload,
                        itemNameResolver = itemNameResolver,
                        cardColor = cardColor,
                        accentColor = accentColor
                    )
                    VictoryDialogStage.LEVEL_UPS -> VictoryLevelUpContent(
                        levelUps = payload.levelUps,
                        portraitById = portraitById,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        cardColor = cardColor,
                        highContrastMode = highContrastMode
                    )
                }
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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

@Composable
fun VictorySpoilsContent(
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String,
    cardColor: Color,
    accentColor: Color
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (resourceEntries.isNotEmpty()) {
            resourceEntries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (label, value) ->
                        RewardStatCard(
                            label = label,
                            value = value,
                            cardColor = cardColor,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (itemEntries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Loot",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(itemEntries, key = { it.key }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = itemNameResolver(entry.key),
                                color = Color.White
                            )
                            Text(
                                text = "×${entry.value}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardStatCard(
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
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
fun VictoryLevelUpContent(
    levelUps: List<LevelUpSummary>,
    portraitById: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    cardColor: Color,
    highContrastMode: Boolean
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
            .heightIn(min = 120.dp, max = 340.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(levelUps, key = { summary -> summary.characterId }) { summary ->
            LevelUpCard(
                summary = summary,
                portraitById = portraitById,
                accentColor = accentColor,
                borderColor = borderColor,
                cardColor = cardColor,
                highContrastMode = highContrastMode
            )
        }
    }
}

@Composable
fun LevelUpCard(
    summary: LevelUpSummary,
    portraitById: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    cardColor: Color,
    highContrastMode: Boolean
) {
    val portraitPath = portraitById[summary.characterId]
        ?: "images/characters/emotes/${summary.characterId}_cool.png"
    val portraitPainter = rememberAssetPainter(
        portraitPath,
        painterResource(R.drawable.main_menu_background)
    )

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
                            fontFamily = CombatNameFont,
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
