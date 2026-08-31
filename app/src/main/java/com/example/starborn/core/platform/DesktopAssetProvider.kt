package com.example.starborn.core.platform

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Desktop (Windows/macOS/Linux) implementation of AssetProvider.
 * Checks filesystem locations first (for development/hot-reloading) and falls back to classpath resources.
 */
class DesktopAssetProvider(
    private val devAssetDirs: List<File> = listOf(
        File("app/src/main/assets"),
        File("world_assets/src/main/assets"),
        File("assets")
    )
) : AssetProvider {

    override fun open(path: String): InputStream? {
        if (path.isBlank()) return null
        val cleanPath = path.removePrefix("/")

        // 1. Check development directories
        for (dir in devAssetDirs) {
            val file = File(dir, cleanPath)
            if (file.exists() && file.isFile) {
                return try {
                    FileInputStream(file)
                } catch (_: Throwable) {
                    null
                }
            }
        }

        // 2. Check ClassLoader classpath resources
        val classLoader = Thread.currentThread().contextClassLoader ?: DesktopAssetProvider::class.java.classLoader
        val resourceStream = classLoader?.getResourceAsStream(cleanPath)
            ?: classLoader?.getResourceAsStream("assets/$cleanPath")

        return resourceStream
    }

    override fun exists(path: String): Boolean {
        if (path.isBlank()) return false
        val cleanPath = path.removePrefix("/")

        for (dir in devAssetDirs) {
            val file = File(dir, cleanPath)
            if (file.exists() && file.isFile) return true
        }

        val classLoader = Thread.currentThread().contextClassLoader ?: DesktopAssetProvider::class.java.classLoader
        return classLoader?.getResource(cleanPath) != null || classLoader?.getResource("assets/$cleanPath") != null
    }
}
