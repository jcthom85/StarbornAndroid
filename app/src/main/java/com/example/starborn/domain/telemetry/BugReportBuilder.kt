package com.example.starborn.domain.telemetry

import android.content.Context
import android.os.Build
import androidx.datastore.dataStoreFile
import com.example.starborn.BuildConfig
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val MAX_REPORTS = 2

/**
 * Bundles everything a tester's device knows into one zip for the share sheet:
 * telemetry sessions, crash records, device info, and the save files.
 */
object BugReportBuilder {

    fun build(context: Context): File {
        val appContext = context.applicationContext
        val reportsDir = File(appContext.cacheDir, "reports").apply { mkdirs() }
        reportsDir.listFiles()
            ?.sortedByDescending { it.name }
            ?.drop(MAX_REPORTS - 1)
            ?.forEach { it.delete() }
        val zipFile = File(reportsDir, "starborn_bugreport_${System.currentTimeMillis()}.zip")
        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            zip.putTextEntry("device_info.txt", deviceInfo())
            File(appContext.filesDir, "telemetry")
                .listFiles { file -> file.name.endsWith(".jsonl") }
                ?.forEach { file -> zip.putFileEntry("telemetry/${file.name}", file) }
            CrashRecorder.crashFiles(appContext.filesDir).forEach { file ->
                zip.putFileEntry("crash/${file.name}", file)
            }
            listOf(
                "game_session.pb",
                "game_session_autosave.pb",
                "game_session_quicksave.pb",
                "game_session_slot1.pb",
                "game_session_slot2.pb",
                "game_session_slot3.pb"
            ).forEach { name ->
                val file = appContext.dataStoreFile(name)
                if (file.exists()) {
                    zip.putFileEntry("saves/$name", file)
                }
            }
        }
        return zipFile
    }

    private fun deviceInfo(): String = buildString {
        appendLine("app_version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("build_type: ${BuildConfig.BUILD_TYPE}")
        appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("sdk: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        appendLine("locale: ${Locale.getDefault()}")
        appendLine("time_ms: ${System.currentTimeMillis()}")
    }

    private fun ZipOutputStream.putTextEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }

    private fun ZipOutputStream.putFileEntry(name: String, file: File) {
        runCatching {
            putNextEntry(ZipEntry(name))
            file.inputStream().use { it.copyTo(this) }
            closeEntry()
        }
    }
}
