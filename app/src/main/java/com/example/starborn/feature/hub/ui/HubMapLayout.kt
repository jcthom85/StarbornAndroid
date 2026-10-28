package com.example.starborn.feature.hub.ui

/** One transform keeps the map image, destination artwork and ground anchors aligned. */
internal data class HubMapTransform(
    val width: Float, val imageHeight: Float, val offsetX: Float, val offsetY: Float
) {
    fun x(fraction: Float) = offsetX + width * fraction
    fun y(fraction: Float) = offsetY + imageHeight * fraction
    companion object {
        // Trim unused sky/foreground while retaining the authored destination area.
        fun fit(viewWidth: Float, viewHeight: Float, imageAspect: Float): HubMapTransform {
            val top = .14f
            val bottom = .90f
            val width = minOf(viewWidth, viewHeight * imageAspect / (bottom - top))
            val height = width / imageAspect
            return HubMapTransform(width, height, (viewWidth - width) / 2,
                (viewHeight - height * (bottom - top)) / 2 - height * top)
        }
        fun cover(viewWidth: Float, viewHeight: Float, imageAspect: Float): HubMapTransform {
            val width = maxOf(viewWidth, viewHeight * imageAspect)
            val height = width / imageAspect
            return HubMapTransform(width, height, (viewWidth - width) / 2, (viewHeight - height) / 2)
        }
    }
}
