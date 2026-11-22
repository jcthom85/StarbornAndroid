package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.presentation.AstraArcadeStatusUi
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@Composable
internal fun CargoRecordTabs(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("cargo" to "Supplies", "catch" to "Catch Log", "relics" to "Relics").forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
internal fun AstraRecreationDialog(
    games: List<AstraArcadeStatusUi>,
    tokenBalance: Int,
    prizesAvailable: Boolean,
    onGame: (String) -> Unit,
    onFilms: () -> Unit,
    onPrizes: () -> Unit,
    onClose: () -> Unit
) {
    val installedCount = games.count { it.installed }
    val totalCount = games.size.coerceAtLeast(1)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().padding(16.dp),
            color = FieldMenuDesign.shell,
            contentColor = FieldMenuDesign.text,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.35f))
        ) {
            Column(Modifier.heightIn(max = 620.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recreation Lounge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (prizesAvailable) {
                            Text(
                                "Arcade Tokens: $tokenBalance · Restored $installedCount/$totalCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFFC857)
                            )
                        }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onFilms, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Great Frontier Films")
                    }
                    if (prizesAvailable) {
                        OutlinedButton(
                            onClick = onPrizes,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.65f))
                        ) {
                            Text(
                                "Arcade Prize Dispenser ($tokenBalance Tokens)",
                                color = Color(0xFFFFC857),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        "Arcade Cabinets ($installedCount/$totalCount Restored)",
                        color = FieldMenuDesign.cyan,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    games.forEachIndexed { index, game ->
                        if (game.installed) {
                            Surface(
                                color = Color(0xFF102234),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            game.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FieldMenuDesign.text
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "HI ${game.highScore}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB8D0E8)
                                            )
                                            listOf("BRONZE" to Color(0xFFCD7F32), "SILVER" to Color(0xFFC0C8D4), "GOLD" to Color(0xFFFFD54F)).forEach { (tier, tint) ->
                                                val earned = tier in game.claimedTiers
                                                Text(
                                                    text = tier.first().toString(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (earned) tint else Color.White.copy(alpha = 0.22f)
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = { onGame(game.id) },
                                        modifier = Modifier.heightIn(min = 42.dp)
                                    ) {
                                        Text("Play", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (game.discovered) {
                            Surface(
                                color = Color(0xFF1A1710),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        game.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFE08A)
                                    )
                                    Text(
                                        "Logic board recovered — repair core in Menu → Tinker to install.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC7B89A)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "Cabinet 0${index + 1} — Undiscovered",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = FieldMenuDesign.text.copy(alpha = 0.55f)
                                    )
                                    if (game.worldHint.isNotBlank()) {
                                        Text(
                                            "Signal trace: ${game.worldHint}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FieldMenuDesign.text.copy(alpha = 0.40f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
