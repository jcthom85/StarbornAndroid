package com.example.starborn.ui.environment

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.starborn.domain.environment.*
import kotlin.math.sin

/** Painted sources stay fixed; light and projected foliage move only on their receiving planes. */
internal fun DrawScope.drawLocalizedLighting(
    effect: ResolvedEnvironmentalEffect, time: Float, opacity: Float, geometry: EnvironmentalGeometry,
    lightBrush: Brush?, shadowBrush: Brush, path: Path
) {
    if (lightBrush == null) return
    val style = effect.preset.style
    val animate = effect.flashes && effect.fieldMotion
    val sw = geometry.spaceWidth(effect.space)
    val sh = geometry.spaceHeight(effect.space)
    fun point(x: Float, y: Float) = Offset(geometry.x(x, effect.space), geometry.y(y, effect.space))
    fun DrawScope.pool(center: Offset, rx: Float, ry: Float, strength: Float, shadow: Boolean = false, angle: Float = 0f) {
        if (rx <= 0f || ry <= 0f) return
        withTransform({ translate(center.x, center.y); rotate(angle, Offset.Zero); scale(rx, ry, Offset.Zero) }) {
            drawCircle(if (shadow) shadowBrush else lightBrush, 1f, Offset.Zero,
                alpha = (strength * opacity * effect.intensity).coerceIn(0f, 1f),
                blendMode = if (shadow) BlendMode.SrcOver else BlendMode.Screen)
        }
    }
    if (style != "canopy") effect.emitters.forEachIndexed { index, source ->
        val motion = localizedLightMotion(style, time, effect.seed, index, animate)
        val radius = sw * source.radius
        pool(point(source.x, source.y), radius * motion.scaleX, radius * .8f * motion.scaleY, motion.strength)
    }
    effect.lightSurfaces.forEach { surface ->
        path.reset()
        surface.points.forEachIndexed { index, p ->
            val at = point(p.x, p.y)
            if (index == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
        }
        path.close()
        val bounds = path.getBounds()
        val motion = localizedLightMotion(style, time, effect.seed, surface.source, animate)
        clipPath(path) {
            if (style == "canopy") {
                // A wind-stirred canopy projects overlapping soft leaf shadows and sunflecks.
                // Foliage moves together, slowly; there is no roaming spotlight in the forest.
                val wind = if (animate) sin(time * .48f + effect.seed.mod(11)) else 0f
                repeat(4) { leaf ->
                    val across = when (leaf) { 0 -> .31f; 1 -> .62f; 2 -> .41f; else -> .58f }
                    val x = bounds.left + bounds.width * (across + motion.x)
                    val y = bounds.top + bounds.height * (.20f + leaf * .18f + motion.y)
                    pool(Offset(x, y), bounds.width * (.16f + leaf * .02f), bounds.height * (.055f + leaf * .005f), .45f * surface.strength,
                        shadow = true, angle = -30f + leaf % 2 * 24f + wind * 7f)
                    pool(Offset(x + bounds.width * .07f, y + bounds.height * .10f),
                        bounds.width * .14f * motion.scaleX, bounds.height * .055f,
                        .70f * surface.strength, angle = -25f + wind * 5f)
                }
            } else {
                pool(bounds.center + Offset(bounds.width * motion.x, bounds.height * motion.y),
                    bounds.width * .60f * motion.scaleX, bounds.height * .60f * motion.scaleY,
                    motion.strength * surface.strength)
            }
        }
    }
    if (style == "firelight" && animate) {
        // Thin luminous edges within the already painted blue flame, rooted in its bowl.
        effect.emitters.take(1).forEach { source ->
            val at = point(source.x, source.y)
            repeat(3) { tongue ->
                val phase = time * (1.8f + tongue * .19f) + tongue * 2.1f
                val base = at + Offset((tongue - 1) * sw * .025f, sh * .025f)
                val tip = at + Offset((tongue - 1) * sw * .018f + sin(phase) * sw * .012f,
                    -sh * (.055f + tongue * .012f + sin(phase * .71f) * .009f))
                path.reset(); path.moveTo(base.x, base.y)
                path.cubicTo(base.x - sw * .025f, base.y - sh * .035f,
                    tip.x + sw * .025f, tip.y + sh * .025f, tip.x, tip.y)
                val color = Color(.12f, .85f, 1f, .40f * opacity * effect.intensity)
                drawPath(path, color, style = Stroke(sw * .012f, cap = StrokeCap.Round), blendMode = BlendMode.Screen)
                drawPath(path, color.copy(alpha = .68f * opacity * effect.intensity), style = Stroke(sw * .0035f, cap = StrokeCap.Round), blendMode = BlendMode.Screen)
            }
        }
    }
}
