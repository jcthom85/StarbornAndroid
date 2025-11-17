package com.example.starborn.ui.vfx

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val AGSL_VIGNETTE_SHADER = """
uniform float2 size;
uniform float intensity;
uniform float feather;
uniform float4 color;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / size;
    float2 delta = uv - 0.5;
    float dist = length(delta);
    float vignette = smoothstep(0.5, 0.5 - feather, dist);
    float alpha = clamp(vignette * intensity, 0.0, 1.0);
    return half4(color.rgb, alpha * color.a);
}
"""

@Composable
fun VignetteOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    intensity: Float = 0.8f,
    color: Color = Color(0.1f, 0.1f, 0.1f, 1.0f),
    feather: Float = 0.2f,
    margin: Dp = 0.dp
) {
    if (!visible || intensity <= 0f) return
    BoxWithConstraints(modifier = modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShaderVignette(
                intensity = intensity,
                feather = feather,
                color = color,
                margin = margin
            )
        } else {
            RadialGradientVignette(
                intensity = intensity,
                feather = feather,
                color = color,
                margin = margin
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun BoxWithConstraintsScope.RuntimeShaderVignette(
    intensity: Float,
    feather: Float,
    color: Color,
    margin: Dp
) {
    val shader = remember { RuntimeShader(AGSL_VIGNETTE_SHADER) }
    val density = LocalDensity.current
    val marginPx = with(density) { margin.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val insetWidth = width - marginPx * 2f
        val insetHeight = height - marginPx * 2f

        shader.setFloatUniform("size", insetWidth, insetHeight)
        shader.setFloatUniform("intensity", intensity.coerceIn(0f, 1f))
        shader.setFloatUniform("feather", feather.coerceIn(0.05f, 0.4f)) // Adjusted feather range
        shader.setFloatUniform("color", color.red, color.green, color.blue, color.alpha)

        val brush = ShaderBrush(shader)
        val left = marginPx
        val top = marginPx

        drawRect(
            brush = brush,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(insetWidth, insetHeight),
            blendMode = BlendMode.SrcOver
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.RadialGradientVignette(
    intensity: Float,
    feather: Float,
    color: Color,
    margin: Dp
) {
    val density = LocalDensity.current
    val marginPx = with(density) { margin.toPx() }
    val canvasTint = color.copy(alpha = color.alpha * intensity.coerceIn(0f, 1f))
    val fadeRadius = feather.coerceIn(0.05f, 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val radius = (width.coerceAtLeast(height)) / 2f
        val innerRadius = radius * (1f - fadeRadius)

        val brush = Brush.radialGradient(
            center = center,
            radius = radius,
            colorStops = arrayOf(
                (innerRadius / radius) to Color.Transparent,
                1f to canvasTint
            )
        )
        drawRect(
            brush = brush,
            topLeft = Offset(marginPx, marginPx),
            size = androidx.compose.ui.geometry.Size(
                width - marginPx * 2f,
                height - marginPx * 2f
            )
        )
    }
}
