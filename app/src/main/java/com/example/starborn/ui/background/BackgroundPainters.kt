package com.example.starborn.ui.background

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.starborn.R
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val assetImageCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
fun rememberAssetPainter(
    imagePath: String?,
    fallback: Painter = painterResource(R.drawable.main_menu_background),
    async: Boolean = false
): Painter {
    val context = LocalContext.current.applicationContext
    if (imagePath.isNullOrBlank()) return fallback

    val resolvedId = remember(imagePath) {
        val resourceName = imagePath
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .lowercase(Locale.getDefault())
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    if (resolvedId != 0) return painterResource(resolvedId)

    if (async) {
        var imageBitmap by remember(imagePath) { mutableStateOf(assetImageCache[imagePath]) }
        LaunchedEffect(imagePath) {
            if (imageBitmap == null) {
                imageBitmap = withContext(Dispatchers.IO) {
                    loadAssetImage(context, imagePath)
                }
            }
        }
        return imageBitmap?.let { bitmap ->
            remember(bitmap) { BitmapPainter(bitmap) }
        } ?: fallback
    }

    val imageBitmap = remember(imagePath) {
        loadAssetImage(context, imagePath)
    }
    return imageBitmap?.let { bitmap ->
        remember(bitmap) { BitmapPainter(bitmap) }
    } ?: fallback
}

@Composable
fun rememberRoomBackgroundPainter(imagePath: String?): Painter =
    rememberAssetPainter(imagePath, fallback = ColorPainter(Color.Black))

private fun loadAssetImage(
    context: android.content.Context,
    imagePath: String
): ImageBitmap? {
    assetImageCache[imagePath]?.let { return it }
    return runCatching {
        context.assets.open(imagePath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()?.also { bitmap ->
        assetImageCache[imagePath] = bitmap
    }
}
