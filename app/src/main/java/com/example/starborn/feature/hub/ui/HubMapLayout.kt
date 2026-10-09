package com.example.starborn.feature.hub.ui

/** Same centered cover transform as ContentScale.Crop: full bleed, no letterboxing. */
internal data class HubMapTransform(
    val width: Float, val imageHeight: Float, val offsetX: Float, val offsetY: Float
) {
    fun x(fraction: Float) = offsetX + width * fraction
    fun y(fraction: Float) = offsetY + imageHeight * fraction
    companion object {
        fun cover(viewWidth: Float, viewHeight: Float, imageAspect: Float): HubMapTransform {
            val width = maxOf(viewWidth, viewHeight * imageAspect)
            val height = width / imageAspect
            return HubMapTransform(width, height, (viewWidth - width) / 2, (viewHeight - height) / 2)
        }
    }
}
