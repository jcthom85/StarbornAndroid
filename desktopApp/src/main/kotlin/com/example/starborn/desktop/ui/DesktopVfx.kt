package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.data.local.Theme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.random.Random

// --- CRT & VIGNETTE OVERLAYS ---

@Composable
fun DesktopVignetteOverlay(
    intensity: Float = 0.65f,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    if (intensity <= 0f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = maxOf(size.width, size.height) / 1.4f
        val brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.2f * intensity),
                color.copy(alpha = 0.85f * intensity)
            ),
            center = center,
            radius = radius
        )
        drawRect(brush = brush, size = size)
    }
}

@Composable
fun DesktopCrtScanlineOverlay(
    modifier: Modifier = Modifier,
    scanlineSpacingDp: Dp = 4.dp,
    scanlineAlpha: Float = 0.08f
) {
    if (scanlineAlpha <= 0f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = scanlineSpacingDp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = scanlineAlpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.2f
            )
            y += step
        }
    }
}

// --- THEME BANDS ---

@Composable
fun DesktopThemeBandOverlay(
    theme: Theme?,
    modifier: Modifier = Modifier,
    bandHeight: Dp = 18.dp
) {
    val accentColor = remember(theme) {
        val list = theme?.accent
        if (list != null && list.size >= 3) {
            Color(
                red = list.getOrElse(0) { 0f },
                green = list.getOrElse(1) { 0.96f },
                blue = list.getOrElse(2) { 0.83f },
                alpha = list.getOrElse(3) { 1f }
            )
        } else {
            Color(0xFF00F5D4)
        }
    }

    val bgColor = remember(theme) {
        val list = theme?.bg
        if (list != null && list.size >= 3) {
            Color(
                red = list.getOrElse(0) { 0.02f },
                green = list.getOrElse(1) { 0.03f },
                blue = list.getOrElse(2) { 0.05f },
                alpha = list.getOrElse(3) { 1f }
            )
        } else {
            Color(0xFF05070D)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Top Band
        Canvas(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(bandHeight)) {
            val grad = Brush.verticalGradient(
                colors = listOf(
                    bgColor.copy(alpha = 0.85f),
                    bgColor.copy(alpha = 0.1f)
                )
            )
            drawRect(brush = grad, size = size)
            drawLine(
                color = accentColor.copy(alpha = 0.7f),
                start = Offset(0f, size.height - 1f),
                end = Offset(size.width, size.height - 1f),
                strokeWidth = 1.5f
            )
        }

        // Bottom Band
        Canvas(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bandHeight)) {
            val grad = Brush.verticalGradient(
                colors = listOf(
                    bgColor.copy(alpha = 0.1f),
                    bgColor.copy(alpha = 0.85f)
                )
            )
            drawRect(brush = grad, size = size)
            drawLine(
                color = accentColor.copy(alpha = 0.7f),
                start = Offset(0f, 1f),
                end = Offset(size.width, 1f),
                strokeWidth = 1.5f
            )
        }
    }
}

// --- WEATHER PARTICLE SIMULATOR ---

private data class VfxParticle(
    var position: Offset,
    var velocity: Offset,
    var size: Pair<Float, Float>,
    var color: Color,
    var life: Float,
    val maxLife: Float,
    var turbulence: List<Float>? = null
)

@Composable
fun DesktopWeatherOverlay(
    weatherId: String?,
    modifier: Modifier = Modifier
) {
    val cleanWeather = weatherId?.lowercase()?.trim() ?: return

    when (cleanWeather) {
        "rain" -> RainEffect(modifier, intensity = "medium", color = Color(0.8f, 0.9f, 1.0f))
        "storm" -> StormEffect(modifier, intensity = "high", color = Color(0.8f, 0.9f, 1.0f))
        "dust" -> DustEffect(modifier, color = Color(1.0f, 0.93f, 0.75f))
        "snow" -> SnowEffect(modifier, intensity = "medium", color = Color.White)
        "sparks" -> SparksEffect(modifier, color = Color(1.0f, 0.65f, 0.15f))
        "starfall" -> StarfallEffect(modifier, color = Color(0.9f, 0.95f, 1.0f))
        "cave_drip" -> CaveDripEffect(modifier, color = Color(0.6f, 0.8f, 1.0f))
        else -> {}
    }
}

@Composable
private fun RainEffect(
    modifier: Modifier,
    intensity: String,
    color: Color
) {
    val particles = remember { mutableStateListOf<VfxParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(intensity) {
        while (true) {
            val active = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(active)

            val emit = when (intensity) {
                "high" -> 20
                else -> 10
            }

            repeat(emit) {
                val x = random.nextFloat() * 1.5f - 0.25f
                val y = -0.05f
                val size = (random.nextFloat() * 0.002f + 0.001f) to (random.nextFloat() * 0.02f + 0.01f)
                val vel = Offset((random.nextFloat() - 0.5f) * 0.005f - 0.004f, random.nextFloat() * 0.02f + 0.02f)
                val life = random.nextFloat() * 0.6f + 0.3f
                particles.add(VfxParticle(Offset(x, y), vel, size, color, life, life))
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 0.85f)
            rotate(degrees = 12f, pivot = Offset(p.position.x * size.width, p.position.y * size.height)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(p.position.x * size.width, p.position.y * size.height),
                    size = Size(p.size.first * size.width, p.size.second * size.height)
                )
            }
        }
    }
}

@Composable
private fun StormEffect(
    modifier: Modifier,
    intensity: String,
    color: Color
) {
    val lightningAlpha = remember { Animatable(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        RainEffect(Modifier.matchParentSize(), intensity, color)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = lightningAlpha.value))
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200L, 6000L))
            val strikes = Random.nextInt(1, 3)
            repeat(strikes) {
                lightningAlpha.animateTo(0.85f, animationSpec = tween(60, easing = LinearEasing))
                lightningAlpha.animateTo(0.2f, animationSpec = tween(80, easing = LinearEasing))
                delay(80L)
            }
            lightningAlpha.animateTo(0f, animationSpec = tween(400, easing = LinearEasing))
        }
    }
}

@Composable
private fun DustEffect(modifier: Modifier, color: Color) {
    val particles = remember { mutableStateListOf<VfxParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < 30) {
                repeat(2) {
                    val fromLeft = random.nextBoolean()
                    val x = if (fromLeft) -0.05f else 1.05f
                    val y = random.nextFloat()
                    val vx = if (fromLeft) random.nextFloat() * 0.0015f + 0.0005f else -(random.nextFloat() * 0.0015f + 0.0005f)
                    val vy = (random.nextFloat() - 0.5f) * 0.0005f
                    val w = random.nextFloat() * 0.008f + 0.003f
                    val life = random.nextFloat() * 4f + 3f
                    particles.add(VfxParticle(Offset(x, y), Offset(vx, vy), w to w * 0.6f, color, life, life))
                }
            }

            val survivors = mutableListOf<VfxParticle>()
            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
                if (p.life > 0f && p.position.x in -0.1f..1.1f && p.position.y in -0.1f..1.1f) {
                    survivors.add(p)
                }
            }
            particles.clear()
            particles.addAll(survivors)
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val alpha = (kotlin.math.sin(progress * PI.toFloat()) * 0.4f).coerceIn(0f, 1f)
            val center = Offset(p.position.x * size.width, p.position.y * size.height)
            drawOval(
                color = p.color.copy(alpha = alpha),
                topLeft = Offset(center.x - (p.size.first * size.width / 2f), center.y - (p.size.second * size.height / 2f)),
                size = Size(p.size.first * size.width, p.size.second * size.height)
            )
        }
    }
}

@Composable
private fun SnowEffect(modifier: Modifier, intensity: String, color: Color) {
    val particles = remember { mutableStateListOf<VfxParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(intensity) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            repeat(2) {
                val x = random.nextFloat()
                val y = -0.05f
                val radius = random.nextFloat() * 0.004f + 0.002f
                val vel = Offset(random.nextFloat() * 0.001f - 0.0005f, random.nextFloat() * 0.002f + 0.001f)
                val life = random.nextFloat() * 6f + 4f
                particles.add(VfxParticle(Offset(x, y), vel, radius to radius, color, life, life))
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 0.75f)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size.first * size.width / 2f,
                center = Offset(p.position.x * size.width, p.position.y * size.height)
            )
        }
    }
}

@Composable
private fun SparksEffect(modifier: Modifier, color: Color) {
    val particles = remember { mutableStateListOf<VfxParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            if (particles.size < 25) {
                repeat(3) {
                    val x = random.nextFloat() * 0.8f + 0.1f
                    val y = 0.95f
                    val vx = (random.nextFloat() - 0.5f) * 0.004f
                    val vy = -(random.nextFloat() * 0.008f + 0.003f)
                    val s = random.nextFloat() * 0.003f + 0.0015f
                    val life = random.nextFloat() * 0.8f + 0.4f
                    particles.add(VfxParticle(Offset(x, y), Offset(vx, vy), s to s, color, life, life))
                }
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = p.color.copy(alpha = progress * 0.9f),
                radius = p.size.first * size.width,
                center = Offset(p.position.x * size.width, p.position.y * size.height)
            )
        }
    }
}

@Composable
private fun StarfallEffect(modifier: Modifier, color: Color) {
    val particles = remember { mutableStateListOf<VfxParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            repeat(1) {
                val x = random.nextFloat()
                val y = -0.05f
                val w = random.nextFloat() * 0.003f + 0.002f
                val h = random.nextFloat() * 0.04f + 0.02f
                val vel = Offset(-0.006f, random.nextFloat() * 0.025f + 0.02f)
                val life = random.nextFloat() * 0.6f + 0.2f
                particles.add(VfxParticle(Offset(x, y), vel, w to h, color, life, life))
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 0.9f)
            drawRoundRect(
                color = p.color.copy(alpha = alpha),
                topLeft = Offset(p.position.x * size.width, p.position.y * size.height),
                size = Size(p.size.first * size.width, p.size.second * size.height),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

@Composable
private fun CaveDripEffect(modifier: Modifier, color: Color) {
    val particles = remember { mutableStateListOf<VfxParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            if (particles.size < 6) {
                val x = random.nextFloat()
                val vel = Offset(0f, random.nextFloat() * 0.015f + 0.008f)
                val size = (random.nextFloat() * 0.002f + 0.0015f) to (random.nextFloat() * 0.008f + 0.004f)
                val life = random.nextFloat() * 1.5f + 0.8f
                particles.add(VfxParticle(Offset(x, -0.02f), vel, size, color, life, life))
            }

            for (p in particles) {
                p.position = Offset(p.position.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 0.8f)
            drawRoundRect(
                color = p.color.copy(alpha = alpha),
                topLeft = Offset(p.position.x * size.width, p.position.y * size.height),
                size = Size(p.size.first * size.width, p.size.second * size.height),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}
