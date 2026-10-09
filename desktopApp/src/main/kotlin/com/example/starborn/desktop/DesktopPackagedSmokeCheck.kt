package com.example.starborn.desktop

import java.io.File
import java.nio.file.Files

/** Invoked by the packaged launcher to verify its JVM and resources without changing player saves. */
internal object DesktopPackagedSmokeCheck {
    fun run(args: Array<String>) {
        val directory = Files.createTempDirectory("starborn-package-check-").toFile()
        val reportIndex = args.indexOf("--report")
        val report = args.getOrNull(reportIndex + 1)?.takeIf { reportIndex >= 0 }?.let(::File)
        var services: DesktopAppServices? = null
        try {
            check(java.lang.Boolean.getBoolean("starborn.packagedAssets")) { "Packaged asset mode is not enabled" }
            services = DesktopAppServices(directory)
            val assets = services.assetProvider
            val manifest = requireNotNull(assets.open("asset-manifest.txt")).bufferedReader().use { it.readLines() }
            check(manifest.isNotEmpty()) { "Asset manifest is empty" }
            manifest.forEach { path -> assets.open(path).use { stream -> check(stream != null && stream.read() >= 0) { "Missing or empty asset: $path" } } }
            val rooms = services.worldDataSource.loadRooms()
            val characters = services.worldDataSource.loadCharacters()
            check(rooms.any { it.id == "pit_nova_bunk" })
            check(characters.any { it.id == "nova" })
            val result = "PASS\nAssets: ${manifest.size}\nRooms: ${rooms.size}\nCharacters: ${characters.size}\nJava: ${System.getProperty("java.version")}\nWorking directory: ${File(".").canonicalPath}\n"
            report?.writeText(result)
            println(result)
        } catch (error: Throwable) {
            report?.writeText("FAIL: ${error.stackTraceToString()}")
            throw error
        } finally {
            services?.close()
            check(directory.canonicalFile.parentFile == File(System.getProperty("java.io.tmpdir")).canonicalFile)
            directory.deleteRecursively()
        }
    }
}
