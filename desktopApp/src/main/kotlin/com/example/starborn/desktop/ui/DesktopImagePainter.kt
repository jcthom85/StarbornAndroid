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

private val imageCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
private val imageDecodeMutex = kotlinx.coroutines.sync.Mutex()

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

    var imageBitmap by remember(path) { mutableStateOf(imageCache[path]) }

    LaunchedEffect(path, assetProvider) {
        if (imageBitmap == null) {
            val decoded = withContext(Dispatchers.IO) {
                imageDecodeMutex.withLock {
                    // Cinematic preloads and reflected layers share the same decoded bitmap.
                    imageCache[path] ?: runCatching {
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
                        stream?.use { loadImageBitmap(it) }?.also { imageCache[path] = it }
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
