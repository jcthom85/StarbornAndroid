package com.example.starborn.feature.exploration.ui.menu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Stable visual language for the out-of-world field menu. */
object FieldMenuDesign {
    val shell = Color(0xFF02070E)
    val panel = Color(0xFF061018)
    val elevatedPanel = Color(0xFF0A1720)
    val cyan = Color(0xFF7FE6FF)
    val gold = Color(0xFFFFC857)
    val text = Color(0xFFF4F7FA)
    val textMuted = Color(0xFFB8C1C9)
    val border = Color(0xFF7FE6FF)

    val shellRadius = 14.dp
    val cardRadius = 12.dp
    val controlRadius = 10.dp
}

enum class MenuDetailKind {
    QUEST,
    INVENTORY_ITEM,
    PARTY_MEMBER,
    SKILL_TREE,
    FULL_MAP,
    MAP_LEGEND
}
