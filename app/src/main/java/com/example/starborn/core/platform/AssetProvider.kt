package com.example.starborn.core.platform

import java.io.InputStream

/**
 * Common abstraction for loading raw game assets (JSON files, audio streams, sprites)
 * across Android, iOS, and Desktop (Windows).
 */
interface AssetProvider {
    /**
     * Opens an input stream for the specified asset path relative to the game's asset root.
     * Returns null if the asset cannot be found or opened.
     */
    fun open(path: String): InputStream?

    /**
     * Reads the entire text content of the asset file (e.g. JSON configs).
     * Returns null if the asset cannot be read.
     */
    fun readText(path: String): String? {
        return open(path)?.bufferedReader()?.use { it.readText() }
    }

    /**
     * Lists asset file paths inside the specified directory.
     */
    fun list(dir: String): List<String> = emptyList()

    /**
     * Checks if the specified asset exists.
     */
    fun exists(path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            open(path)?.use { } != null
        } catch (_: Throwable) {
            false
        }
    }
}
