package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.inventory.ui.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InventoryCategoryColumn(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    credits: Int,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val creditsLabel = remember(credits) { formatter.format(credits.coerceAtLeast(0)) }

    Surface(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Wallet", accentColor = accentColor)
            WalletSummaryCard(
                creditsLabel = creditsLabel,
                accentColor = accentColor,
                borderColor = borderColor
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f))
            Text(
                text = "Supplies",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            categories.forEach { key ->
                val selected = key == selectedCategory
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onSelectCategory(key) },
                    color = if (selected) accentColor.copy(alpha = 0.8f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selected) accentColor else borderColor.copy(alpha = 0.6f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryLabel(key),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) Color(0xFF020409) else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SectionHeading(
    label: String,
    accentColor: Color
) {
    Text(
        text = label.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = accentColor
    )
}

@Composable
private fun WalletSummaryCard(
    creditsLabel: String,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "WALLET",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor.copy(alpha = 0.85f)
            )
            Text(
                text = "$creditsLabel c",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
