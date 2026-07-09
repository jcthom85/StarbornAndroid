package com.example.starborn.domain.telemetry

import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelemetryLoggerTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = File(
            System.getProperty("java.io.tmpdir"),
            "starborn-telemetry-${System.nanoTime()}"
        ).apply { mkdirs() }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun log_writesOneJsonObjectPerLine() {
        val logger = TelemetryLogger(dir)
        logger.log("test_event", "key" to "value", "count" to 3, "skipped" to null)
        logger.log("second_event")

        val lines = awaitLines(count = 2)
        val first = JSONObject(lines[0])
        assertEquals("test_event", first.getString("e"))
        assertEquals("value", first.getString("key"))
        assertEquals(3, first.getInt("count"))
        assertTrue(first.has("t"))
        assertTrue(!first.has("skipped"))
        assertEquals("second_event", JSONObject(lines[1]).getString("e"))
    }

    @Test
    fun logRoomEnter_dedupesConsecutiveSameRoom() {
        val logger = TelemetryLogger(dir)
        logger.logRoomEnter("w1", "h1", "room_a")
        logger.logRoomEnter("w1", "h1", "room_a")
        logger.logRoomEnter("w1", "h1", "room_b")
        logger.logRoomEnter("w1", "h1", null)

        assertEquals(2, logger.recentLines().size)
    }

    @Test
    fun init_prunesOldSessionFiles() {
        repeat(12) { index ->
            File(dir, "session_${1000 + index}.jsonl").writeText("{}\n")
        }
        val logger = TelemetryLogger(dir)
        logger.log("boot")

        awaitLines(count = 1)
        val sessionFiles = dir.listFiles { file -> file.name.endsWith(".jsonl") }.orEmpty()
        assertTrue("expected at most 10 session files, found ${sessionFiles.size}", sessionFiles.size <= 10)
    }

    private fun awaitLines(count: Int, timeoutMs: Long = 5_000): List<String> {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val newest = dir.listFiles { file -> file.name.endsWith(".jsonl") }
                ?.maxByOrNull { it.name }
            val lines = newest?.readLines()?.filter { it.isNotBlank() }.orEmpty()
            if (lines.size >= count) return lines
            Thread.sleep(20)
        }
        throw AssertionError("Timed out waiting for $count telemetry lines")
    }
}
