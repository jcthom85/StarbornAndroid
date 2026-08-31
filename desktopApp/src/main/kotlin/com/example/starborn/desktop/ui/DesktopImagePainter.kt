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

private val imageCache = mutableMapOf<String, ImageBitmap>()

@Composable
fun rememberDesktopAssetPainter(
    path: String?,
    assetProvider: AssetProvider,
    fallbackColor: Color = Color(0xFF090D18)
): Painter {
    if (path.isNullOrBlank()) return ColorPainter(fallbackColor)

    var imageBitmap by remember(path) { mutableStateOf(imageCache[path]) }

    LaunchedEffect(path) {
        if (imageBitmap == null) {
            withContext(Dispatchers.IO) {
                try {
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

                    if (stream != null) {
                        stream.use { s ->
                            val decoded = loadImageBitmap(s)
                            imageCache[path] = decoded
                            imageBitmap = decoded
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to color
                }
            }
        }
    }

    return imageBitmap?.let { BitmapPainter(it) } ?: ColorPainter(fallbackColor)
}
