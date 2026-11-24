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
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decoded asset bitmaps, bounded by a byte budget rather than an entry count.
 *
 * A room background is 1088x1920 ARGB_8888 (~8 MB); an emote is a fraction of that, so counting
 * entries would be meaningless. World 1 alone has 88 rooms, and an unbounded cache retained every
 * background the player had ever walked into for the life of the process -- roughly 330 MB after
 * forty rooms, against a heap that is 192-512 MB on most devices.
 */
private object AssetImageCache {
    private const val MIN_BUDGET_BYTES = 16L * 1024 * 1024
    private const val MAX_BUDGET_BYTES = 128L * 1024 * 1024

    private val budgetBytes: Long = run {
        val quarterHeap = Runtime.getRuntime().maxMemory() / 4
        quarterHeap.coerceIn(MIN_BUDGET_BYTES, MAX_BUDGET_BYTES)
    }

    private var retainedBytes = 0L

    // accessOrder = true makes iteration order least-recently-used first.
    private val entries = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {}
    private val missingPaths = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun sizeOf(bitmap: ImageBitmap): Long =
        bitmap.width.toLong() * bitmap.height.toLong() * 4L

    @Synchronized
    operator fun get(path: String): ImageBitmap? = entries[path]

    fun isMissing(path: String): Boolean = missingPaths.contains(path)

    fun markMissing(path: String) {
        missingPaths.add(path)
    }

    @Synchronized
    fun put(path: String, bitmap: ImageBitmap) {
        missingPaths.remove(path)
        val size = sizeOf(bitmap)
        // A single image larger than the whole budget is served but never retained.
        if (size > budgetBytes) return
        entries.put(path, bitmap)?.let { retainedBytes -= sizeOf(it) }
        retainedBytes += size
        val iterator = entries.entries.iterator()
        while (retainedBytes > budgetBytes && iterator.hasNext()) {
            val evicted = iterator.next()
            iterator.remove()
            retainedBytes -= sizeOf(evicted.value)
        }
    }
}

@Composable
fun rememberCachedAssetPainterOrNull(
    imagePath: String?,
    async: Boolean = true
): Painter? {
    val context = LocalContext.current.applicationContext
    if (imagePath.isNullOrBlank()) return null

    val resolvedId = remember(imagePath) {
        val resourceName = imagePath
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .lowercase(Locale.getDefault())
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    if (resolvedId != 0) return painterResource(resolvedId)

    if (async) {
        var imageBitmap by remember(imagePath) { mutableStateOf(AssetImageCache[imagePath]) }
        LaunchedEffect(imagePath) {
            if (imageBitmap == null && !AssetImageCache.isMissing(imagePath)) {
                imageBitmap = withContext(Dispatchers.IO) {
                    loadAssetImage(context, imagePath)
                }
            }
        }
        return imageBitmap?.let { bitmap ->
            remember(bitmap) { BitmapPainter(bitmap) }
        }
    }

    val imageBitmap = remember(imagePath) {
        loadAssetImage(context, imagePath)
    }
    return imageBitmap?.let { bitmap ->
        remember(bitmap) { BitmapPainter(bitmap) }
    }
}

@Composable
fun rememberAssetPainter(
    imagePath: String?,
    fallback: Painter = painterResource(R.drawable.main_menu_background),
    async: Boolean = false
): Painter {
    return rememberCachedAssetPainterOrNull(imagePath, async = async) ?: fallback
}

@Composable
fun rememberRoomBackgroundPainter(imagePath: String?): Painter {
    val context = LocalContext.current.applicationContext
    if (imagePath.isNullOrBlank()) {
        return remember { ColorPainter(Color.Black) }
    }

    val resolvedId = remember(imagePath) {
        val resourceName = imagePath
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .lowercase(Locale.getDefault())
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    if (resolvedId != 0) {
        return painterResource(resolvedId)
    }

    var imageBitmap by remember(imagePath) { mutableStateOf(AssetImageCache[imagePath]) }
    var isMissing by remember(imagePath) { mutableStateOf(AssetImageCache.isMissing(imagePath)) }
    LaunchedEffect(imagePath) {
        if (imageBitmap == null && !isMissing) {
            val loaded = withContext(Dispatchers.IO) {
                loadAssetImage(context, imagePath)
            }
            if (loaded != null) {
                imageBitmap = loaded
            } else {
                isMissing = true
            }
        }
    }

    imageBitmap?.let { bitmap ->
        return remember(bitmap) { BitmapPainter(bitmap) }
    }

    if (isMissing && (imagePath.contains("world_2") || imagePath.contains("sector9"))) {
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

    return remember { ColorPainter(Color.Black) }
}

private fun loadAssetImage(
    context: android.content.Context,
    imagePath: String
): ImageBitmap? {
    AssetImageCache[imagePath]?.let { return it }
    if (AssetImageCache.isMissing(imagePath)) return null
    val decoded = runCatching {
        context.assets.open(imagePath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
    if (decoded != null) {
        AssetImageCache.put(imagePath, decoded)
    } else {
        AssetImageCache.markMissing(imagePath)
    }
    return decoded
}

/** Warm only adjacent room art; cancellation stops work when the player moves again. */
@Composable
fun PrefetchRoomBackgrounds(paths: List<String>) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(paths) {
        paths.distinct().forEach { path ->
            if (AssetImageCache[path] == null) {
                withContext(Dispatchers.IO) { loadAssetImage(context, path) }
            }
        }
    }
}
