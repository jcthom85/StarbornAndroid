package com.example.starborn.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

/** Android's item category artwork, including its weapon-specific distinctions. */
@Composable
internal fun DesktopMenuItemArt(services: DesktopAppServices, item: Item?, modifier: Modifier = Modifier, slot: String? = null) {
    val type = (item?.equipment?.weaponType ?: item?.type ?: slot.orEmpty()).lowercase()
    val icon = when {
        type.contains("gun") -> "gun"
        type.contains("glove") -> "gloves"
        type.contains("pendant") || type.contains("jewel") -> "pendant"
        type.contains("sword") || type.contains("blade") || type == "weapon" -> "sword"
        type == "armor" -> "armor"
        type == "accessory" -> "accessory"
        type in setOf("snack", "meal", "food") -> "food"
        type == "consumable" -> "consumable"
        type == "ingredient" -> "ingredient"
        type == "fish" -> "fish"
        type == "lure" -> "lure"
        type.contains("fishing") -> "fishing"
        type in setOf("material", "component", "mod") -> "material"
        else -> "generic"
    }
    Box(modifier.background(Brush.radialGradient(listOf(FieldMenuDesign.cyan.copy(alpha = .12f), Color.Transparent)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Image(rememberDesktopAssetPainter("drawable-nodpi/item_icon_$icon.webp", services.assetProvider), null,
            Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun DesktopMenuSection(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = FieldMenuDesign.gold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted) }
    }
}

@Composable
internal fun DesktopMenuScrollPane(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Box(modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = ScrollbarStyle(16.dp, 5.dp, RoundedCornerShape(3.dp), 150,
                FieldMenuDesign.cyan.copy(alpha = .3f), FieldMenuDesign.cyan.copy(alpha = .7f)))
    }
}
