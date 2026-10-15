package com.example.starborn.desktop.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.math.roundToInt

private val blurMutex = Mutex()
private val blurredImages = object : LinkedHashMap<Pair<ImageBitmap, Float>, ImageBitmap>(16, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<ImageBitmap, Float>, ImageBitmap>?) = size > 12
}

internal val LocalBackdropBlurLoader = staticCompositionLocalOf<suspend (ImageBitmap, Float) -> ImageBitmap> {
    { bitmap, sigma ->
        withContext(Dispatchers.IO) {
            blurMutex.withLock {
                synchronized(blurredImages) { blurredImages[bitmap to sigma] }
                    ?: blurBackdropBitmap(bitmap, sigma).also { image ->
                        synchronized(blurredImages) { blurredImages[bitmap to sigma] = image }
                    }
            }
        }
    }
}

/** Blur once at a small resolution; cinematic animation then moves an ordinary bitmap. */
@Composable
internal fun rememberBlurredBackdropPainter(painter: Painter, sigma: Float = 16f): Painter? {
    val bitmap = (painter as? DesktopBitmapPainter)?.bitmap ?: return null
    val loader = LocalBackdropBlurLoader.current
    val cached = remember(bitmap, sigma) { synchronized(blurredImages) { blurredImages[bitmap to sigma] } }
    val blurred by produceState(cached, bitmap, sigma, loader) { value = loader(bitmap, sigma) }
    return remember(blurred) { blurred?.let(::BitmapPainter) }
}

private fun blurBackdropBitmap(bitmap: ImageBitmap, sigma: Float): ImageBitmap {
    val height = 384
    val width = (height * bitmap.width.toFloat() / bitmap.height).roundToInt().coerceAtLeast(1)
    Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { original ->
        Surface.makeRasterN32Premul(width, height).use { downsample ->
            downsample.canvas.drawImageRect(original, Rect.makeWH(width.toFloat(), height.toFloat()))
            downsample.makeImageSnapshot().use { small ->
                Surface.makeRasterN32Premul(width, height).use { blurred ->
                    ImageFilter.makeBlur(sigma, sigma, FilterTileMode.MIRROR).use { filter ->
                        Paint().use { paint ->
                            paint.imageFilter = filter
                            blurred.canvas.drawImage(small, 0f, 0f, paint)
                        }
                    }
                    return blurred.makeImageSnapshot().use { it.toComposeImageBitmap() }
                }
            }
        }
    }
}
