package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuHighContrast
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlin.math.sin

internal val LocalTerminalPresentation = staticCompositionLocalOf { false }
internal val LocalTerminalPage = staticCompositionLocalOf { DesktopMenuTab.STATS }

/** The field menu has its own palette; game screens outside it retain their theme. */
@Composable
internal fun DesktopFieldMenuTheme(services: DesktopAppServices, content: @Composable () -> Unit) {
    val settings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    val originalType = desktopStarbornTypography(services)
    val type = if (!LocalTerminalPresentation.current) originalType else originalType.copy(
        titleLarge = originalType.titleMedium.copy(fontSize = originalType.titleLarge.fontSize),
        titleMedium = originalType.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = originalType.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    )
    val colors = MaterialTheme.colorScheme.copy(
        primary = FieldMenuDesign.gold, onPrimary = FieldMenuDesign.shell,
        secondary = FieldMenuDesign.cyan, onSecondary = FieldMenuDesign.shell,
        surface = FieldMenuDesign.panel, surfaceVariant = FieldMenuDesign.elevatedPanel,
        surfaceContainer = FieldMenuDesign.panel, surfaceContainerHigh = FieldMenuDesign.elevatedPanel,
        onSurface = FieldMenuDesign.text, onSurfaceVariant = FieldMenuDesign.textMuted,
        outline = FieldMenuDesign.cyan.copy(alpha = .28f), outlineVariant = FieldMenuDesign.cyan.copy(alpha = .12f)
    )
    CompositionLocalProvider(LocalFieldMenuHighContrast provides settings.highContrastMode, LocalFieldMenuLargeTargets provides settings.largeTouchTargets) {
    MaterialTheme(colorScheme = if (settings.highContrastMode) colors.copy(surface = Color.Black, surfaceVariant = Color.Black, onSurface = Color.White, onSurfaceVariant = Color.White, outline = Color.White) else colors, typography = type, shapes = MaterialTheme.shapes.copy(
        small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(18.dp)
    ), content = content)
    }
}

@Composable
internal fun DesktopMenuCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = if (LocalFieldMenuHighContrast.current) Color.Black else if (LocalTerminalPresentation.current) when (LocalTerminalPage.current) {
        DesktopMenuTab.JOURNAL -> Color(0xFF151A1D)
        DesktopMenuTab.FIELD_KIT -> Color(0xFF0C1B20)
        DesktopMenuTab.STATS -> Color(0xFF101B22)
        else -> Color(0xFF0C1821)
    } else FieldMenuDesign.panel, shape = RoundedCornerShape(if (LocalTerminalPresentation.current) 6.dp else 14.dp),
        border = if (LocalFieldMenuHighContrast.current) BorderStroke(1.dp, Color.White) else if (LocalTerminalPresentation.current) null else BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .16f))) {
        Column(content = content)
    }
}

/** Slow signal lights and etched geometry stay behind the readable content. */
@Composable
internal fun DesktopMenuAtmosphere(modifier: Modifier, accent: Color, animate: Boolean, page: DesktopMenuTab = DesktopMenuTab.STATS) {
    Canvas(modifier) {
        val pageColor = when (page) {
            DesktopMenuTab.STATS -> Color(0xFF273023)
            DesktopMenuTab.JOURNAL -> Color(0xFF302C23)
            DesktopMenuTab.FIELD_KIT -> Color(0xFF153238)
            else -> Color(0xFF142B3A)
        }
        drawRect(Brush.linearGradient(listOf(pageColor.copy(alpha = .40f), Color.Transparent),
            Offset.Zero, Offset(size.width * .65f, size.height)))
        // Etchings stay in the shell margins, away from text and controls.
        val inset = 7.dp.toPx()
        val edge = Color(0xFF9BA9AC).copy(alpha = .18f)
        repeat(12) { index ->
            val y = 115.dp.toPx() + index * 37.dp.toPx()
            if (y < size.height - 30.dp.toPx()) drawLine(edge, Offset(inset, y), Offset(inset + if (index % 3 == 0) 8.dp.toPx() else 4.dp.toPx(), y))
        }
        val top = 7.dp.toPx()
        repeat(5) { index ->
            val x = size.width - 150.dp.toPx() + index * 22.dp.toPx()
            drawLine(edge, Offset(x, top), Offset(x + 10.dp.toPx(), top), 1.dp.toPx())
        }
        if (page == DesktopMenuTab.FIELD_KIT || page == DesktopMenuTab.MAP) {
            val corner = Offset(size.width - 12.dp.toPx(), size.height - 12.dp.toPx())
            drawLine(accent.copy(alpha = .2f), corner - Offset(70.dp.toPx(), 0f), corner)
            drawLine(accent.copy(alpha = .2f), corner - Offset(0f, 50.dp.toPx()), corner)
        }
    }
}
