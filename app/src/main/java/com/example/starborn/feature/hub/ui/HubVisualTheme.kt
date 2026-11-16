package com.example.starborn.feature.hub.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

internal data class HubVisualTheme(
    val worldLabel: String,
    val accent: Color,
    val secondaryAccent: Color,
    val border: Color,
    val panelColor: Color,
    val backdropColor: Color,
    val motePrimary: Color,
    val moteSecondary: Color
)

internal fun hubVisualTheme(hubId: String?, worldId: String?): HubVisualTheme =
    when {
        hubId == "hub_astra" || worldId == "world_astra" -> HubVisualTheme(
            worldLabel = "STARSHIP ASTRA",
            accent = Color(0xFF7BE4FF),
            secondaryAccent = Color(0xFFFFC857),
            border = Color(0xFF5CCBE8),
            panelColor = Color(0xFF07111A),
            backdropColor = Color(0xFF060E16),
            motePrimary = Color(0xFF8BE7FF),
            moteSecondary = Color(0xFFFFD480)
        )
        worldId == "world_1" || hubId in setOf("hub_1_homestead", "hub_2_logistics") -> HubVisualTheme(
            worldLabel = "MINING COLONY",
            accent = Color(0xFF7BE4FF),
            secondaryAccent = Color(0xFFFFC857),
            border = Color(0xFF5CCBE8),
            panelColor = Color(0xFF07111A),
            backdropColor = Color(0xFF060E15),
            motePrimary = Color(0xFF8AE6FF),
            moteSecondary = Color(0xFFFFD07B)
        )
        worldId == "world_2" || hubId in setOf("hub_3_sector9", "hub_4_facility") -> HubVisualTheme(
            worldLabel = "SECTOR 9",
            accent = Color(0xFF66F0C8),
            secondaryAccent = Color(0xFFB5F57A),
            border = Color(0xFF4CD3AC),
            panelColor = Color(0xFF071412),
            backdropColor = Color(0xFF05100E),
            motePrimary = Color(0xFF77F2CB),
            moteSecondary = Color(0xFFBEF58B)
        )
        worldId == "world_3" || hubId in setOf("hub_5_lower_city", "hub_6_upper_city") -> HubVisualTheme(
            worldLabel = "THE SPIRE",
            accent = Color(0xFF7CC6FF),
            secondaryAccent = Color(0xFFC49BFF),
            border = Color(0xFF5FA8E8),
            panelColor = Color(0xFF08101C),
            backdropColor = Color(0xFF060C16),
            motePrimary = Color(0xFF8CCEFF),
            moteSecondary = Color(0xFFCBA6FF)
        )
        worldId == "world_4" || hubId in setOf("hub_7_slag_pits", "hub_8_assembly_line") -> HubVisualTheme(
            worldLabel = "THE FOUNDRY",
            accent = Color(0xFFFFA366),
            secondaryAccent = Color(0xFFFFD166),
            border = Color(0xFFE07F45),
            panelColor = Color(0xFF140C09),
            backdropColor = Color(0xFF100906),
            motePrimary = Color(0xFFFF9A52),
            moteSecondary = Color(0xFFFFD166)
        )
        worldId == "world_5" || hubId in setOf("hub_9_orbital_ring", "hub_10_deep_ring") -> HubVisualTheme(
            worldLabel = "ORBITAL RING",
            accent = Color(0xFF9BE8FF),
            secondaryAccent = Color(0xFF7EB8FF),
            border = Color(0xFF6EC3E0),
            panelColor = Color(0xFF07111D),
            backdropColor = Color(0xFF050D17),
            motePrimary = Color(0xFFAEEBFF),
            moteSecondary = Color(0xFF89C4FF)
        )
        worldId == "world_6" || hubId in setOf("hub_11_event_horizon", "hub_12_singularity") -> HubVisualTheme(
            worldLabel = "THE SOURCE",
            accent = Color(0xFFD3A6FF),
            secondaryAccent = Color(0xFF8AEBFF),
            border = Color(0xFFB07EE6),
            panelColor = Color(0xFF100B1C),
            backdropColor = Color(0xFF0C0716),
            motePrimary = Color(0xFFD8B0FF),
            moteSecondary = Color(0xFF91ECFF)
        )
        else -> HubVisualTheme(
            worldLabel = "REGION MAP",
            accent = Color(0xFF7BE4FF),
            secondaryAccent = Color(0xFFFFC857),
            border = Color(0xFF5CCBE8),
            panelColor = Color(0xFF071018),
            backdropColor = Color(0xFF060E16),
            motePrimary = Color(0xFF7BE4FF),
            moteSecondary = Color(0xFFFFD477)
        )
    }

@Composable
internal fun HubCornerBrackets(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val inset = 8.dp.toPx()
        val length = 14.dp.toPx()
        val stroke = 1.6.dp.toPx()
        val tint = color.copy(alpha = 0.62f)
        val right = size.width - inset
        val bottom = size.height - inset

        // Top-left
        drawLine(tint, Offset(inset, inset), Offset(inset + length, inset), stroke, StrokeCap.Round)
        drawLine(tint, Offset(inset, inset), Offset(inset, inset + length), stroke, StrokeCap.Round)
        // Top-right
        drawLine(tint, Offset(right - length, inset), Offset(right, inset), stroke, StrokeCap.Round)
        drawLine(tint, Offset(right, inset), Offset(right, inset + length), stroke, StrokeCap.Round)
        // Bottom-left
        drawLine(tint, Offset(inset, bottom - length), Offset(inset, bottom), stroke, StrokeCap.Round)
        drawLine(tint, Offset(inset, bottom), Offset(inset + length, bottom), stroke, StrokeCap.Round)
        // Bottom-right
        drawLine(tint, Offset(right - length, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
        drawLine(tint, Offset(right, bottom - length), Offset(right, bottom), stroke, StrokeCap.Round)
    }
}
