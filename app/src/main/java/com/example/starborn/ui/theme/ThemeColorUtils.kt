package com.example.starborn.ui.theme

import androidx.compose.ui.graphics.Color

fun themeColor(values: List<Float>?, fallback: Color): Color {
    if (values == null || values.size < 3) return fallback
    val alpha = values.getOrNull(3) ?: 1f
    return Color(
        red = values[0].coerceIn(0f, 1f),
        green = values[1].coerceIn(0f, 1f),
        blue = values[2].coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}
