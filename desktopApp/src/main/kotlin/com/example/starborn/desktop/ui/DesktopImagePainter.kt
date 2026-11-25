package com.example.starborn.desktop.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.loadImageBitmap
import com.example.starborn.core.platform.AssetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

internal class DesktopBitmapLruCache(
    private val maxEntries: Int = 192,
    private val maxPixelBytes: Long = 384L * 1024L * 1024L,
    private val byteEstimator: (ImageBitmap) -> Long = { bmp ->
        (bmp.width.toLong().coerceAtLeast(1L) * bmp.height.toLong().coerceAtLeast(1L) * 4L).coerceAtLeast(4_096L)
    }
) {
    private val lock = Any()
    private var currentBytes: Long = 0L
    private val entries = LinkedHashMap<String, Pair<ImageBitmap, Long>>(64, 0.75f, true)

    fun get(key: String): ImageBitmap? = synchronized(lock) {
        entries[key]?.first
    }

    fun containsKey(key: String): Boolean = synchronized(lock) {
        entries.containsKey(key)
    }

    fun put(key: String, bitmap: ImageBitmap) = synchronized(lock) {
        val bytes = byteEstimator(bitmap).coerceAtLeast(1L)
        entries.remove(key)?.let { (_, oldBytes) ->
            currentBytes = (currentBytes - oldBytes).coerceAtLeast(0L)
        }
        entries[key] = bitmap to bytes
        currentBytes += bytes
        trimToBudgetLocked()
    }

    fun size(): Int = synchronized(lock) { entries.size }

    fun totalBytes(): Long = synchronized(lock) { currentBytes }

    fun clear() = synchronized(lock) {
        entries.clear()
        currentBytes = 0L
    }

    private fun trimToBudgetLocked() {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext() && (entries.size > maxEntries || (currentBytes > maxPixelBytes && entries.size > 1))) {
            val eldest = iterator.next()
            currentBytes = (currentBytes - eldest.value.second).coerceAtLeast(0L)
            iterator.remove()
        }
    }
}

private val imageCache = DesktopBitmapLruCache()
private val missingAssetPaths = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
private val imageDecodeMutex = kotlinx.coroutines.sync.Mutex()

internal fun hasDesktopAsset(path: String?, assetProvider: AssetProvider): Boolean {
    val key = path?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    if (imageCache.containsKey(key)) return true
    if (missingAssetPaths.contains(key)) return false
    val exists = runCatching { assetProvider.open(key)?.use { true } ?: false }.getOrDefault(false)
    if (!exists) missingAssetPaths.add(key)
    return exists
}

internal class DesktopBitmapPainter(val bitmap: ImageBitmap) : Painter() {
    private val delegate = BitmapPainter(bitmap)
    override val intrinsicSize: Size get() = delegate.intrinsicSize
    override fun DrawScope.onDraw() { with(delegate) { draw(size) } }
}

@Composable
fun rememberDesktopAssetPainter(
    path: String?,
    assetProvider: AssetProvider,
    fallbackColor: Color = Color(0xFF090D18)
): Painter {
    if (path.isNullOrBlank()) return ColorPainter(fallbackColor)

    var imageBitmap by remember(path) { mutableStateOf(imageCache.get(path)) }

    LaunchedEffect(path, assetProvider) {
        if (imageBitmap == null) {
            val decoded = withContext(Dispatchers.IO) {
                imageDecodeMutex.withLock {
                    // Cinematic preloads and reflected layers share the same decoded bitmap.
                    imageCache.get(path) ?: runCatching {
                        val stream: InputStream? = assetProvider.open(path)
                            ?: assetProvider.open("images/$path")
                            ?: assetProvider.open("images/rooms/$path.webp")
                            ?: assetProvider.open("images/rooms/$path.png")
                            ?: assetProvider.open("images/hubs/$path.webp")
                            ?: assetProvider.open("images/characters/$path.webp")
                            ?: assetProvider.open("images/enemies/$path.webp")
                            ?: assetProvider.open("images/cinematics/$path.webp")
                            ?: assetProvider.open("drawable-nodpi/$path.webp")
                            ?: assetProvider.open("$path.webp")
                            ?: assetProvider.open("$path.png")
                        stream?.use { loadImageBitmap(it) }?.also { imageCache.put(path, it) }
                    }.getOrNull()
                }
            }
            imageBitmap = decoded
        }
    }

    return remember(imageBitmap, fallbackColor) {
        imageBitmap?.let { DesktopBitmapPainter(it) } ?: ColorPainter(fallbackColor)
    }
}
