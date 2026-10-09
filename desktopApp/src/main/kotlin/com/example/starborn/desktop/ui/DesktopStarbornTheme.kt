package com.example.starborn.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices

val StarbornShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun DesktopStarbornTheme(
    services: DesktopAppServices? = null,
    content: @Composable () -> Unit
) {
    val fontBundle = services?.assetProvider?.let { rememberStarbornFonts(it) } ?: LocalStarbornFonts.current
    val typography = services?.let { desktopStarbornTypography(it) } ?: MaterialTheme.typography

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF63E6FF), onPrimary = Color(0xFF061018),
            secondary = Color(0xFFFFC857), onSecondary = Color(0xFF061018),
            secondaryContainer = Color(0xFF234251), onSecondaryContainer = Color(0xFFF0F4FA),
            background = Color(0xFF05070D), onBackground = Color(0xFFF0F4FA),
            surface = Color(0xFF101C2A), onSurface = Color(0xFFF0F4FA),
            surfaceVariant = Color(0xFF182C3C), onSurfaceVariant = Color(0xFFD7EAF4),
            surfaceContainer = Color(0xFF101C2A), surfaceContainerHigh = Color(0xFF142635),
            surfaceContainerHighest = Color(0xFF182C3C),
            outline = Color(0xFF3F6172), error = Color(0xFFFF8A80)
        ),
        shapes = StarbornShapes,
        typography = typography
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalStarbornFonts provides fontBundle,
            content = content
        )
    }
}

