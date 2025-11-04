package com.example.starborn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.starborn.R

private val RussoOne = FontFamily(
    Font(R.font.russo_one_regular, weight = FontWeight.Bold)
)

private val Oxanium = FontFamily(
    Font(R.font.oxanium_bold, weight = FontWeight.Bold)
)

private val SourceSans3 = FontFamily(
    Font(R.font.source_sans3_regular, weight = FontWeight.Normal),
    Font(R.font.source_sans3_medium, weight = FontWeight.Medium)
)

internal val PressStart2P = FontFamily(
    Font(R.font.press_start_2p_regular, weight = FontWeight.Normal)
)

// Compose typography tuned to match the original Starborn presentation
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = RussoOne,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp
    ),
    displayMedium = TextStyle(
        fontFamily = RussoOne,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 42.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Oxanium,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Oxanium,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Oxanium,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RussoOne,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Oxanium,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Oxanium,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

val MinimapTextStyle = TextStyle(
    fontFamily = PressStart2P,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)
