package com.example.starborn.domain.telemetry

import android.content.Context
import android.os.Build
import com.example.starborn.BuildConfig
import java.io.File
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val MAX_SESSION_FILES = 10
private const val RECENT_LINE_CAPACITY = 200

/**
 * Append-only JSONL playtest log, one file per app process. Fully offline; files are
 * only exported through the in-app bug report share flow.
 */
class TelemetryLogger internal constructor(
    private val telemetryDir: File
) {
    private val sessionFile = File(telemetryDir, "session_${System.currentTimeMillis()}$SESSION_SUFFIX")
    private val lines = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recent = ArrayDeque<String>()
    @Volatile private var lastRoomKey: String? = null

    init {
        scope.launch {
            runCatching {
                telemetryDir.mkdirs()
                pruneSessions()
            }
            for (line in lines) {
                runCatching { sessionFile.appendText(line + "\n") }
            }
        }
    }

    fun log(event: String, vararg fields: Pair<String, Any?>) {
        val line = runCatching {
            val json = JSONObject()
            json.put("t", System.currentTimeMillis())
            json.put("e", event)
            fields.forEach { (key, value) ->
                if (value != null) {
                    json.put(key, JSONObject.wrap(value) ?: value.toString())
                }
            }
            json.toString()
        }.getOrNull() ?: return
        synchronized(recent) {
            recent.addLast(line)
            while (recent.size > RECENT_LINE_CAPACITY) {
                recent.removeFirst()
            }
        }
        lines.trySend(line)
    }

    /** Deduped against the last room so redundant session-state emissions don't double-log. */
    fun logRoomEnter(worldId: String?, hubId: String?, roomId: String?) {
        if (roomId.isNullOrBlank()) return
        val key = "$worldId|$hubId|$roomId"
        if (key == lastRoomKey) return
        lastRoomKey = key
        log("room_enter", "world" to worldId, "hub" to hubId, "room" to roomId)
    }

    fun recentLines(count: Int = 50): List<String> =
        synchronized(recent) { recent.toList() }.takeLast(count)

    fun sessionFiles(): List<File> =
        telemetryDir.listFiles { file -> file.name.endsWith(SESSION_SUFFIX) }
            ?.sortedByDescending { it.name }
            .orEmpty()

    private fun pruneSessions() {
        val existing = telemetryDir.listFiles { file -> file.name.endsWith(SESSION_SUFFIX) }
            ?.sortedByDescending { it.name }
            ?: return
        existing.drop(MAX_SESSION_FILES - 1).forEach { it.delete() }
    }

    companion object {
        private const val SESSION_SUFFIX = ".jsonl"

        @Volatile private var instance: TelemetryLogger? = null

        fun get(context: Context): TelemetryLogger =
            instance ?: synchronized(this) {
                instance ?: TelemetryLogger(File(context.applicationContext.filesDir, "telemetry"))
                    .also { logger ->
                        instance = logger
                        logger.log(
                            "session_start",
                            "version" to BuildConfig.VERSION_NAME,
                            "build" to BuildConfig.VERSION_CODE,
                            "model" to Build.MODEL,
                            "sdk" to Build.VERSION.SDK_INT
                        )
                    }
            }

        fun peek(): TelemetryLogger? = instance
    }
}
