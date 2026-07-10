package com.example.starborn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF55C7FF),
    onPrimary = Color(0xFF001E2B),
    primaryContainer = Color(0xFF123B50),
    onPrimaryContainer = Color(0xFFC7EDFF),
    secondary = Color(0xFFFFA64D),
    onSecondary = Color(0xFF2C1600),
    secondaryContainer = Color(0xFF4A2C0C),
    onSecondaryContainer = Color(0xFFFFDDBB),
    tertiary = Color(0xFFFF6F91),
    background = Color(0xFF05080E),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF0B1019),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF18212D),
    onSurfaceVariant = Color(0xFFC0CAD7),
    outline = Color(0xFF6C7887),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF310000)
)

@Composable
fun StarbornTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
