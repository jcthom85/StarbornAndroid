package com.example.starborn.domain.telemetry

import java.io.File
import java.io.PrintWriter

private const val MAX_CRASH_FILES = 5
private const val PENDING_MARKER = ".pending"

/**
 * Offline crash capture: writes stacktrace plus recent telemetry to filesDir/crash,
 * then rethrows to the platform handler so the process still dies normally.
 */
object CrashRecorder {

    fun install(filesDir: File, telemetry: TelemetryLogger) {
        val crashDir = File(filesDir, "crash")
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashFile(crashDir, thread, throwable, telemetry) }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun crashFiles(filesDir: File): List<File> =
        File(filesDir, "crash")
            .listFiles { file -> file.name.startsWith("crash_") && file.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }
            .orEmpty()

    /** Returns true once per recorded crash so the UI can offer the bug-report flow. */
    fun consumePendingNotice(filesDir: File): Boolean {
        val marker = File(File(filesDir, "crash"), PENDING_MARKER)
        if (!marker.exists()) return false
        marker.delete()
        return true
    }

    private fun writeCrashFile(
        crashDir: File,
        thread: Thread,
        throwable: Throwable,
        telemetry: TelemetryLogger
    ) {
        crashDir.mkdirs()
        val stamp = System.currentTimeMillis()
        val file = File(crashDir, "crash_$stamp.txt")
        PrintWriter(file.bufferedWriter()).use { writer ->
            writer.println("time_ms: $stamp")
            writer.println("thread: ${thread.name}")
            throwable.printStackTrace(writer)
            writer.println()
            writer.println("--- recent telemetry ---")
            telemetry.recentLines(50).forEach(writer::println)
        }
        File(crashDir, PENDING_MARKER).writeText(stamp.toString())
        crashFiles(crashDir.parentFile ?: return)
            .drop(MAX_CRASH_FILES)
            .forEach { it.delete() }
    }
}
