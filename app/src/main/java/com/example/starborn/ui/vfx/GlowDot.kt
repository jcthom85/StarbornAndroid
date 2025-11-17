package com.example.starborn.ui.vfx

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val AGSL_GLOW_DOT = """
uniform float2 size;
uniform float radius;
uniform float feather;
uniform float4 tint;

half4 main(float2 fragCoord) {
    float2 center = size * 0.5;
    float dist = distance(fragCoord, center);
    float glow = smoothstep(radius, radius - feather, dist);
    float alpha = clamp(1.0 - glow, 0.0, 1.0);
    return half4(tint.rgb, tint.a * alpha);
}
"""

@Composable
fun GlowDot(
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 28.dp,
    feather: Float = 6f
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimeShaderGlowDot(color, modifier.size(diameter), feather)
    } else {
        FallbackGlowDot(color, modifier.size(diameter))
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RuntimeShaderGlowDot(
    color: Color,
    modifier: Modifier,
    feather: Float
) {
    val shader = remember { RuntimeShader(AGSL_GLOW_DOT) }
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("feather", feather.coerceAtLeast(1f))
        shader.setFloatUniform("tint", color.red, color.green, color.blue, color.alpha)
        drawRect(
            brush = ShaderBrush(shader),
            size = Size(size.width, size.height)
        )
    }
}

@Composable
private fun FallbackGlowDot(
    color: Color,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = color.alpha), color.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
