package com.example.starborn.ui.background

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.starborn.R
import java.util.Locale

@Composable
fun rememberAssetPainter(
    imagePath: String?,
    @DrawableRes fallbackRes: Int = R.drawable.main_menu_background
): Painter {
    val context = LocalContext.current
    val fallback = painterResource(fallbackRes)
    if (imagePath.isNullOrBlank()) return fallback

    val (resolvedId, assetPainter) = remember(imagePath) {
        val resourceName = imagePath
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .lowercase(Locale.getDefault())
        val drawableId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        val painter = runCatching {
            context.assets.open(imagePath).use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bitmap ->
                    BitmapPainter(bitmap.asImageBitmap())
                }
            }
        }.getOrNull()
        drawableId to painter
    }
    return when {
        resolvedId != 0 -> painterResource(resolvedId)
        assetPainter != null -> assetPainter
        else -> fallback
    }
}

@Composable
fun rememberRoomBackgroundPainter(imagePath: String?): Painter =
    rememberAssetPainter(imagePath, fallbackRes = R.drawable.main_menu_background)
