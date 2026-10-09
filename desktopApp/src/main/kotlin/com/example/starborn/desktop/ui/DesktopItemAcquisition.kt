package com.example.starborn.desktop.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import java.util.Locale

private fun itemCategoryAccentColor(category: String?): Color {
    val norm = category?.lowercase(Locale.getDefault())?.trim().orEmpty()
    return when {
        norm.contains("key") || norm.contains("relic") || norm.contains("core") || norm.contains("quest") -> Color(0xFFFFD54F) // Golden Amber
        norm.contains("weapon") || norm.contains("gun") || norm.contains("sword") || norm.contains("armor") || norm.contains("gear") || norm.contains("accessory") -> Color(0xFF7BE8FF) // Cyber Cyan
        norm.contains("consumable") || norm.contains("food") || norm.contains("ration") || norm.contains("heal") || norm.contains("stim") || norm.contains("firstaid") -> Color(0xFF81C784) // Medical Jade
        norm.contains("void") || norm.contains("erosion") || norm.contains("anomaly") -> Color(0xFFCE93D8) // Void Violet
        norm.contains("material") || norm.contains("ingredient") || norm.contains("scrap") || norm.contains("ore") || norm.contains("fish") -> Color(0xFFB0BEC5) // Industrial Steel Slate
        else -> Color(0xFFA5D6A7)
    }
}

@Composable
internal fun DesktopItemGrantedBanner(
    services: DesktopAppServices,
    prompt: com.example.starborn.domain.prompt.ItemGrantedPrompt,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(prompt.category, prompt.itemId) { itemCategoryAccentColor(prompt.category) }
    val shape = RoundedCornerShape(18.dp)
    val category = prompt.category
        ?.replace('_', ' ')
        ?.replace('-', ' ')
        ?.trim()
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        ?.takeIf { it.isNotBlank() }
        ?: "Item"
    val itemLabel = if (prompt.quantity > 1) "${prompt.itemName} ×${prompt.quantity}" else prompt.itemName
    val hasSequence = prompt.sequenceTotal > 1 && !prompt.sequenceId.isNullOrBlank()
    val iconRes = remember(prompt.itemId, prompt.category) { acquiredItemIconPath(prompt.category) }

    Surface(
        onClick = onDismiss,
        modifier = modifier.desktopPointerHover()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 560.dp).fillMaxWidth()
            .semantics {
                contentDescription = "Item acquired: $itemLabel. Continue."
            },
        color = Color(0xFF060B13).copy(alpha = 0.98f),
        contentColor = Color.White,
        shape = shape,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.60f)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color(0xFF060B13)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ITEM ACQUIRED",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
                if (hasSequence) {
                    Text(
                        text = "${prompt.sequenceIndex} / ${prompt.sequenceTotal}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                ) {
                    Image(
                        painter = rememberDesktopAssetPainter(iconRes, services.assetProvider),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = itemLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.90f),
                        fontWeight = FontWeight.SemiBold
                    )
                    prompt.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                            color = Color.White.copy(alpha = 0.74f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (prompt.sequenceIndex < prompt.sequenceTotal) "Next ▸" else "Continue",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.52f)
                )
            }
        }
    }
}

private fun acquiredItemIconPath(category: String?): String {
    val type = category?.lowercase(Locale.ROOT).orEmpty()
    val name = when {
        type.contains("food") || type == "snack" -> "food"
        type in setOf("consumable", "medicine", "tonic", "drink") -> "consumable"
        type.contains("fish") -> "fish"
        type.contains("fishing") -> "fishing"
        type.contains("lure") -> "lure"
        type.contains("ingredient") || type.contains("material") -> "ingredient"
        type.contains("component") || type.contains("resource") || type.contains("part") -> "material"
        type.contains("armor") -> "armor"
        type.contains("accessory") -> "accessory"
        type.contains("weapon") || type.contains("gear") -> "sword"
        type.contains("mod") -> "material"
        else -> "generic"
    }
    return "drawable-nodpi/item_icon_$name.webp"
}

/** Android's compact batch-acquisition banner. */
@Composable
internal fun DesktopItemBatchBanner(summary: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Color(0xFF7BE8FF)
    val shape = RoundedCornerShape(18.dp)
    Surface(onClick = onDismiss, modifier = modifier.padding(horizontal = 16.dp).widthIn(max = 520.dp).fillMaxWidth(),
        shape = shape, color = Color.Transparent, shadowElevation = 12.dp) {
        Row(Modifier.background(Brush.linearGradient(listOf(Color(0xF5060B13), Color(0xEB060B13), accent.copy(alpha = .14f))))
            .border(1.dp, accent.copy(alpha = .42f), shape).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(accent.copy(alpha = .42f), accent.copy(alpha = .12f))), RoundedCornerShape(14.dp))
                .border(1.dp, accent.copy(alpha = .55f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(20.dp), tint = Color.White)
            }
            Text("Acquired $summary", Modifier.weight(1f), color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onDismiss) { Text("DISMISS", color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        }
    }
}
