package com.example.starborn.ui.vfx

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val AGSL_GLOW_BAR = """
uniform float2 size;
uniform float progress;
uniform float glowWidth;
uniform float4 tint;

half4 main(float2 fragCoord) {
    float pct = fragCoord.x / max(size.x, 0.0001);
    float fill = step(pct, progress);
    if (fill <= 0.0) {
        return half4(0.0);
    }
    float safeProgress = max(progress, 0.0001);
    float normalized = clamp(pct / safeProgress, 0.0, 1.0);
    float bodyGlow = smoothstep(0.0, 1.0, normalized);
    float tipDistance = max(progress - pct, 0.0);
    float tipGlow = 1.0 - smoothstep(0.0, glowWidth, tipDistance);
    float glow = clamp(bodyGlow * 0.55 + tipGlow, 0.0, 1.0);
    float intensity = mix(0.7, 1.0, glow);
    return half4(tint.rgb * intensity, tint.a);
}
"""

@Composable
fun GlowProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0x332C3444),
    glowColor: Color = Color(0xFF6CD5FF),
    height: Dp = 10.dp,
    cornerRadius: Dp = 999.dp,
    glowWidth: Float = 0.08f
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor)
            .height(height)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GlowRuntimeShaderBar(
                progress = clamped,
                glowColor = glowColor,
                glowWidth = glowWidth,
                height = height
            )
        } else {
            GlowFallbackBar(
                progress = clamped,
                glowColor = glowColor,
                height = height,
                glowWidth = glowWidth
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun GlowRuntimeShaderBar(
    progress: Float,
    glowColor: Color,
    glowWidth: Float,
    height: Dp
) {
    val shader = remember { RuntimeShader(AGSL_GLOW_BAR) }
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(height)) {
        val width = size.width
        val height = size.height
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("progress", progress)
        shader.setFloatUniform("glowWidth", glowWidth.coerceIn(0.02f, 0.35f))
        shader.setFloatUniform("tint", glowColor.red, glowColor.green, glowColor.blue, glowColor.alpha)
        drawRect(
            brush = ShaderBrush(shader),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )
    }
}

@Composable
private fun GlowFallbackBar(
    progress: Float,
    glowColor: Color,
    height: Dp,
    glowWidth: Float
) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(height)) {
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped <= 0f) return@Canvas
        val filledWidth = size.width * clamped
        if (filledWidth <= 0f) return@Canvas
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        val bodyBrush = Brush.horizontalGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.6f),
                glowColor.copy(alpha = 0.85f),
                glowColor
            )
        )
        drawRoundRect(
            brush = bodyBrush,
            topLeft = Offset.Zero,
            size = Size(filledWidth, size.height),
            cornerRadius = corner
        )
        val tipWidthFraction = glowWidth.coerceIn(0.05f, 0.35f)
        val tipWidth = filledWidth.coerceAtMost(size.width * tipWidthFraction).coerceAtLeast(size.height * 0.6f)
        val tipBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                glowColor.copy(alpha = 0.35f),
                Color.Transparent
            )
        )
        drawRoundRect(
            brush = tipBrush,
            topLeft = Offset(filledWidth - tipWidth, 0f),
            size = Size(tipWidth, size.height),
            cornerRadius = corner
        )
    }
}
