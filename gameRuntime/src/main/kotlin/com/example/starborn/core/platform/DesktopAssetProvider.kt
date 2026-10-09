package com.example.starborn.core.platform

import java.io.File
import java.io.InputStream

/** Filesystem overrides are explicit in distribution builds; the packaged manifest supports listing. */
class DesktopAssetProvider(
    private val devAssetDirs: List<File> = defaultAssetDirs(),
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
        ?: DesktopAssetProvider::class.java.classLoader
) : AssetProvider {
    private val manifest: List<String> by lazy {
        classLoader.getResourceAsStream("assets/asset-manifest.txt")?.bufferedReader()?.use { reader ->
            reader.readLines().filter { it.isNotBlank() }
        }.orEmpty()
    }

    private fun normalize(path: String): String? {
        val clean = path.replace('\\', '/').trimStart('/')
        if (clean.split('/').any { it == ".." } || ':' in clean) return null
        return clean
    }

    override fun list(dir: String): List<String> {
        val clean = normalize(dir)?.trimEnd('/') ?: return emptyList()
        val prefix = if (clean.isEmpty()) "" else "$clean/"
        val packaged = manifest.asSequence().filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).substringBefore('/') }.filter { it.isNotEmpty() }.toList()
        return (devAssetDirs.flatMap { File(it, clean).list()?.toList().orEmpty() } + packaged).distinct().sorted()
    }

    override fun open(path: String): InputStream? {
        val clean = normalize(path)?.takeIf { it.isNotBlank() } ?: return null
        for (root in devAssetDirs) {
            val file = File(root, clean)
            if (file.isFile) return file.inputStream()
        }
        return classLoader.getResourceAsStream("assets/$clean") ?: classLoader.getResourceAsStream(clean)
    }

    override fun exists(path: String): Boolean {
        val clean = normalize(path)?.takeIf { it.isNotBlank() } ?: return false
        return devAssetDirs.any { File(it, clean).isFile } ||
            classLoader.getResource("assets/$clean") != null || classLoader.getResource(clean) != null
    }

    companion object {
        private fun defaultAssetDirs(): List<File> {
            val override = System.getProperty("starborn.devAssetsDir")?.let(::File)
            if (override != null) return listOf(override)
            if (java.lang.Boolean.getBoolean("starborn.packagedAssets")) return emptyList()
            return listOf("app/src/main/assets", "world_assets/src/main/assets", "app/src/main/res",
                "app/src/main/res/drawable-nodpi", "app/src/main/res/raw", "../app/src/main/assets",
                "../world_assets/src/main/assets", "../app/src/main/res", "../app/src/main/res/drawable-nodpi",
                "../app/src/main/res/raw").map(::File)
        }
    }
}
