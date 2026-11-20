package com.example.starborn.feature.exploration.ui.menu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.staticCompositionLocalOf

/** Default keeps shared components identical for classic menus and other screens. */
val LocalModernFieldMenu = staticCompositionLocalOf { false }
val LocalFieldMenuHighContrast = staticCompositionLocalOf { false }
val LocalFieldMenuLargeTargets = staticCompositionLocalOf { false }
val LocalFieldMenuReducedMotion = staticCompositionLocalOf { false }

/** Stable visual language for the out-of-world field menu. */
object FieldMenuDesign {
    val shell = Color(0xFF02070E)
    val panel = Color(0xFF081520)
    val elevatedPanel = Color(0xFF102434)
    val deepCard = Color(0xFF061018)
    val primaryActionFill = Color(0xFF1B4B5A)
    val primaryActionHighlight = Color(0xFF28687A)
    val cookingActionFill = Color(0xFF4E2B18)
    val cookingActionBorder = Color(0xFFFFBD80)
    val cyan = Color(0xFF7FE6FF)
    val gold = Color(0xFFFFC857)
    val mint = Color(0xFF7CE8C8)
    val coral = Color(0xFFFF9E8C)
    val ember = Color(0xFFFFBD80)
    val text = Color(0xFFF4F7FA)
    val textMuted = Color(0xFFB8C9D4)
    val border = Color(0xFF7FE6FF)
    val subtleBorder = Color(0xFF244354)

    val shellRadius = 14.dp
    val cardRadius = 12.dp
    val controlRadius = 10.dp
}

enum class MenuDetailKind {
    QUEST,
    INVENTORY_ITEM,
    PARTY_MEMBER,
    SKILL_TREE,
    MAP_LEGEND
}
