package com.example.starborn.domain.telemetry

import org.json.JSONObject
import java.io.File
import java.util.UUID

interface PlaytestTelemetry {
    fun startSession(origin: String)
    fun endSession(exitPoint: String?, worldId: String?, roomId: String?)
    fun record(event: String, properties: Map<String, Any?> = emptyMap())
    fun questStarted(questId: String)
    fun questCompleted(questId: String)
    fun scoreArc(score: FunArcScore)
}

data class FunArcScore(
    val arcId: String,
    val anticipation: Int,
    val agency: Int,
    val mastery: Int,
    val variety: Int,
    val payoff: Int,
    val momentum: Int,
    val findingLabels: Set<String> = emptySet()
) {
    init {
        listOf(anticipation, agency, mastery, variety, payoff, momentum).forEach { score ->
            require(score in 1..5) { "Fun scores must be between 1 and 5." }
        }
    }
}

object NoOpPlaytestTelemetry : PlaytestTelemetry {
    override fun startSession(origin: String) = Unit
    override fun endSession(exitPoint: String?, worldId: String?, roomId: String?) = Unit
    override fun record(event: String, properties: Map<String, Any?>) = Unit
    override fun questStarted(questId: String) = Unit
    override fun questCompleted(questId: String) = Unit
    override fun scoreArc(score: FunArcScore) = Unit
}

class LocalPlaytestTelemetry(
    directory: File,
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val elapsedRealtimeMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES
) : PlaytestTelemetry {
    private val outputFile = File(directory, "fun-events.jsonl")
    private val previousFile = File(directory, "fun-events.previous.jsonl")
    private val questStartedAt = mutableMapOf<String, Long>()
    private val combatAttempts = mutableMapOf<String, Int>()
    private var sessionId = UUID.randomUUID().toString()

    init {
        directory.mkdirs()
    }

    @Synchronized
    override fun startSession(origin: String) {
        sessionId = UUID.randomUUID().toString()
        questStartedAt.clear()
        combatAttempts.clear()
        record("session_started", mapOf("origin" to origin))
    }

    @Synchronized
    override fun endSession(exitPoint: String?, worldId: String?, roomId: String?) {
        record(
            "session_exited",
            mapOf("exit_point" to exitPoint, "world_id" to worldId, "room_id" to roomId)
        )
    }

    @Synchronized
    override fun questStarted(questId: String) {
        if (questId.isBlank()) return
        questStartedAt.putIfAbsent(questId, elapsedRealtimeMs())
        record("quest_started", mapOf("quest_id" to questId))
    }

    @Synchronized
    override fun questCompleted(questId: String) {
        if (questId.isBlank()) return
        val durationMs = questStartedAt.remove(questId)?.let { startedAt ->
            (elapsedRealtimeMs() - startedAt).coerceAtLeast(0L)
        }
        record("quest_completed", mapOf("quest_id" to questId, "duration_ms" to durationMs))
    }

    @Synchronized
    override fun scoreArc(score: FunArcScore) {
        record(
            "fun_arc_scored",
            mapOf(
                "arc_id" to score.arcId,
                "anticipation" to score.anticipation,
                "agency" to score.agency,
                "mastery" to score.mastery,
                "variety" to score.variety,
                "payoff" to score.payoff,
                "momentum" to score.momentum,
                "finding_labels" to score.findingLabels.sorted()
            )
        )
    }

    @Synchronized
    override fun record(event: String, properties: Map<String, Any?>) {
        if (event.isBlank()) return
        rotateIfNeeded()
        val enrichedProperties = properties.toMutableMap()
        val combatKey = combatKey(properties)
        if (event == "combat_started" && combatKey != null) {
            val attempt = combatAttempts.getOrDefault(combatKey, 0) + 1
            combatAttempts[combatKey] = attempt
            enrichedProperties["attempt"] = attempt
        }
        val payload = linkedMapOf<String, Any?>(
            "schema" to 1,
            "timestamp_ms" to wallClockMs(),
            "session_id" to sessionId,
            "event" to event
        )
        enrichedProperties.forEach { (key, value) ->
            if (key.isNotBlank()) payload[key] = telemetryValue(value)
        }
        outputFile.appendText(JSONObject(payload).toString() + System.lineSeparator())
        if (event == "combat_completed" && properties["outcome"] == "victory" && combatKey != null) {
            combatAttempts.remove(combatKey)
        }
    }

    private fun combatKey(properties: Map<String, Any?>): String? {
        val roomId = properties["room_id"]?.toString().orEmpty()
        val enemies = (properties["enemy_ids"] as? Collection<*>)
            ?.map { it.toString() }
            ?.sorted()
            .orEmpty()
        if (roomId.isBlank() && enemies.isEmpty()) return null
        return "$roomId:${enemies.joinToString("|")}"
    }

    private fun rotateIfNeeded() {
        if (!outputFile.exists() || outputFile.length() < maxFileBytes) return
        if (previousFile.exists()) previousFile.delete()
        outputFile.renameTo(previousFile)
    }

    private fun telemetryValue(value: Any?): Any? = when (value) {
        null, is String, is Number, is Boolean -> value
        is Map<*, *> -> value.entries.associate { (key, nestedValue) ->
            key.toString() to telemetryValue(nestedValue)
        }
        is Collection<*> -> value.map(::telemetryValue)
        is Array<*> -> value.map(::telemetryValue)
        else -> value.toString()
    }

    companion object {
        private const val DEFAULT_MAX_FILE_BYTES = 2L * 1024L * 1024L
    }
}
