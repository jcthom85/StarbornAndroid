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

/** The field menu has its own palette; game screens outside it retain their theme. */
@Composable
internal fun DesktopFieldMenuTheme(services: DesktopAppServices, content: @Composable () -> Unit) {
    val settings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    val type = desktopStarbornTypography(services)
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
    Surface(modifier, color = if (LocalFieldMenuHighContrast.current) Color.Black else FieldMenuDesign.panel, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (LocalFieldMenuHighContrast.current) Color.White else FieldMenuDesign.cyan.copy(alpha = .16f))) {
        Column(content = content)
    }
}

/** Slow signal lights and etched geometry stay behind the readable content. */
@Composable
internal fun DesktopMenuAtmosphere(modifier: Modifier, accent: Color, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "menu atmosphere")
    val phase by transition.animateFloat(0f, 6.28318f,
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "signal drift")
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(accent.copy(alpha = .10f), Color.Transparent,
            FieldMenuDesign.cyan.copy(alpha = .035f)), Offset.Zero, Offset(size.width, size.height)))
        val step = 48.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(accent.copy(alpha = .025f), Offset(x, 0f), Offset(x, size.height)); x += step }
        var y = 0f
        while (y < size.height) { drawLine(accent.copy(alpha = .025f), Offset(0f, y), Offset(size.width, y)); y += step }
        val origin = Offset(size.width * .91f, size.height * .1f)
        listOf(100f, 150f, 205f).forEach { radius ->
            drawCircle(accent.copy(alpha = .055f), radius.dp.toPx(), origin, style = Stroke(1.dp.toPx()))
        }
        repeat(18) { index ->
            val alpha = if (animate) .10f + .08f * sin(phase + index).toFloat() else .12f
            drawCircle(accent.copy(alpha = alpha.coerceAtLeast(.02f)), 1.5.dp.toPx(),
                Offset(size.width * ((index * 37 % 97) / 100f), size.height * ((index * 23 % 89) / 100f)))
        }
    }
}
