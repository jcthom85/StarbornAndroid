package com.example.starborn.desktop.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.starborn.core.platform.AssetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Crop only transparent canvas padding; preserve every visible pixel and the artwork's aspect ratio. */
@Composable
internal fun rememberCombatSpritePainter(path: String?, assets: AssetProvider): Painter {
    val original = rememberDesktopAssetPainter(path, assets)
    var cropped by remember(original) { mutableStateOf<Painter?>(null) }
    LaunchedEffect(original) {
        val bitmap = (original as? DesktopBitmapPainter)?.bitmap ?: return@LaunchedEffect
        cropped = withContext(Dispatchers.Default) {
            val pixels = bitmap.toPixelMap()
            var left = bitmap.width; var top = bitmap.height; var right = -1; var bottom = -1
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                if (pixels[x, y].alpha > .02f) {
                    left = minOf(left, x); top = minOf(top, y)
                    right = maxOf(right, x); bottom = maxOf(bottom, y)
                }
            }
            if (right < left) original else BitmapPainter(bitmap, IntOffset(left, top), IntSize(right - left + 1, bottom - top + 1))
        }
    }
    return cropped ?: original
}
