package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private fun formatLogEntry(line: String): Pair<Color, String> {
    val lower = line.lowercase()
    return when {
        "critical" in lower || "broken bonus" in lower || "stability broken" in lower ->
            Color(0xFFFFD27F) to line
        "defeated" in lower || "knockout" in lower ->
            Color(0xFFFF7060) to line
        "recovers" in lower || "heal" in lower ->
            Color(0xFF75E8B0) to line
        "prepares" in lower || "acting" in lower ->
            Color(0xFF63E6FF) to line
        "shock" in lower || "burn" in lower || "corrosion" in lower || "stun" in lower ->
            Color(0xFFD3A1FF) to line
        else ->
            Color(0xFFE0EBEF) to line
    }
}

/**
 * Docked tactical battle feed rendered in the battlefield's contextual left column.
 * Gives desktop players a live chronicle without obstructing action menus.
 */
@Composable
internal fun DesktopCombatLiveFeed(
    history: List<String>,
    onExpandLog: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF63E6FF)
) {
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.lastIndex)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xF207111A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        shadowElevation = 8.dp
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "TACTICAL FEED",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalStarbornFonts.current.orbitron,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .desktopPointerHover()
                            .clickable(onClick = onExpandLog)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LOG [L]",
                                color = accentColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalStarbornFonts.current.orbitron
                            )
                            Icon(
                                imageVector = Icons.Rounded.OpenInFull,
                                contentDescription = "Expand Log",
                                tint = accentColor,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = accentColor.copy(alpha = 0.2f))

            if (history.isEmpty()) {
                Text(
                    text = "Awaiting initial tactical contact...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = LocalStarbornFonts.current.orbitron,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(history.takeLast(12)) { entry ->
                        val (textColor, text) = formatLogEntry(entry)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "›",
                                color = accentColor.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontFamily = LocalStarbornFonts.current.orbitron
                            )
                            Text(
                                text = text,
                                color = textColor,
                                fontSize = 10.5.sp,
                                fontFamily = LocalStarbornFonts.current.orbitron,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full sci-fi transcript modal overlay displaying the comprehensive combat log history.
 */
@Composable
internal fun DesktopCombatLogDialog(
    history: List<String>,
    onDismiss: () -> Unit,
    accentColor: Color = Color(0xFF63E6FF)
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF5060F17),
            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.7f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TACTICAL COMBAT CHRONICLE",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = LocalStarbornFonts.current.orbitron,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Chronological Round Events & Engagements",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.desktopPointerHover()
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = accentColor.copy(alpha = 0.25f))

                // Scrollable log body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (history.isEmpty()) {
                        item {
                            Text(
                                text = "No combat actions recorded yet.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontFamily = LocalStarbornFonts.current.orbitron,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(history.asReversed().toList()) { entry ->
                            val (textColor, text) = formatLogEntry(entry)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "•",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = text,
                                        color = textColor,
                                        fontSize = 11.5.sp,
                                        fontFamily = LocalStarbornFonts.current.orbitron
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = accentColor.copy(alpha = 0.2f))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Entries: ${history.size}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.5.sp,
                        fontFamily = LocalStarbornFonts.current.orbitron
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.desktopPointerHover(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF041018)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Resume [L / Esc]", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}
