package com.example.starborn.core.platform

import android.content.Context
import java.io.IOException
import java.io.InputStream

/**
 * Android implementation of AssetProvider that reads assets via Android's AssetManager.
 */
class AndroidAssetProvider(
    private val context: Context
) : AssetProvider {

    override fun open(path: String): InputStream? {
        if (path.isBlank()) return null
        return try {
            context.assets.open(path)
        } catch (_: IOException) {
            null
        }
    }

    override fun list(dir: String): List<String> {
        return try {
            context.assets.list(dir)?.toList() ?: emptyList()
        } catch (_: IOException) {
            emptyList()
        }
    }

    override fun exists(path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            context.assets.open(path).use { }
            true
        } catch (_: IOException) {
            false
        }
    }
}
