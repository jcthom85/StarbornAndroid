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
fun rememberRoomBackgroundPainter(imagePath: String?, async: Boolean = false): Painter {
    val context = LocalContext.current
    if (!imagePath.isNullOrBlank()) {
        val resourceName = remember(imagePath) {
            imagePath
                .substringAfterLast('/')
                .substringBeforeLast('.')
                .lowercase(Locale.getDefault())
        }
        val resolvedId = remember(imagePath) {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
        if (resolvedId != 0) {
            return painterResource(resolvedId)
        }

        val exists = remember(imagePath) {
            runCatching {
                context.assets.open(imagePath).use { }
                true
            }.getOrDefault(false)
        }
        if (exists) {
            return rememberAssetPainter(imagePath, fallback = ColorPainter(Color.Black), async = async)
        }

        if (imagePath.contains("world_2") || imagePath.contains("sector9")) {
            return remember {
                androidx.compose.ui.graphics.painter.BrushPainter(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030D14),
                            Color(0xFF051D18),
                            Color(0xFF020E15)
                        )
                    )
                )
            }
        }
    }
    return rememberAssetPainter(imagePath, fallback = ColorPainter(Color.Black), async = async)
}

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

fun prefetchRoomBackground(context: android.content.Context, imagePath: String?) {
    if (imagePath.isNullOrBlank()) return
    val resourceName = imagePath
        .substringAfterLast('/')
        .substringBeforeLast('.')
        .lowercase(Locale.getDefault())
    val resolvedId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    if (resolvedId != 0) return

    if (!assetImageCache.containsKey(imagePath)) {
        runCatching {
            context.assets.open(imagePath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()?.also { bitmap ->
            assetImageCache[imagePath] = bitmap
            android.util.Log.d("BackgroundPainters", "Pre-fetched background image: $imagePath")
        }
    }
}
