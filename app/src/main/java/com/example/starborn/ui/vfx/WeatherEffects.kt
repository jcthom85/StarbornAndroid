package com.example.starborn.ui.vfx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed interface WeatherEffect {
    data class Rain(
        val intensity: String,
        val color: Color
    ) : WeatherEffect

    data class Snow(
        val intensity: String,
        val color: Color
    ) : WeatherEffect

    data class Dust(
        val color: Color
    ) : WeatherEffect

    data class Storm(
        val intensity: String,
        val color: Color,
        val lightningColor: Color
    ) : WeatherEffect

    data class CaveDrip(
        val color: Color
    ) : WeatherEffect

    data class Starfall(
        val color: Color
    ) : WeatherEffect
}

data class Particle(
    var position: Offset,
    var velocity: Offset,
    var size: Pair<Float, Float>,
    var color: Color,
    var life: Float,
    val maxLife: Float,
    var angle: Float = 0f,
    var state: String? = null,
    var hangTime: Float? = null,
    val initialSize: Pair<Float, Float>? = null,
    val turbulence: List<Float>? = null,
    val sway: List<Float>? = null
)