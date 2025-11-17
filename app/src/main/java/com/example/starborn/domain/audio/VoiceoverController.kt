package com.example.starborn.domain.audio

import com.example.starborn.domain.audio.AudioCommand.Play
import com.example.starborn.domain.audio.AudioCueType.UI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VoiceoverController(
    private val audioRouter: AudioRouter,
    private val dispatchCommands: (List<AudioCommand>) -> Unit,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) {

    private data class VoiceRequest(
        val cueId: String,
        val duckLayers: Boolean
    )

    private val queue = ArrayDeque<VoiceRequest>()
    private var currentJob: Job? = null
    private var isPlaying = false

    fun enqueue(cueId: String, duckLayers: Boolean = true) {
        val trimmed = cueId.trim()
        if (trimmed.isEmpty()) return
        queue += VoiceRequest(trimmed, duckLayers)
        if (!isPlaying) {
            startNext()
        }
    }

    fun clear() {
        queue.clear()
        currentJob?.cancel()
        currentJob = null
        isPlaying = false
        val restore = audioRouter.restoreAfterVoiceover()
        if (restore.isNotEmpty()) {
            dispatchCommands(restore)
        }
    }

    private fun startNext() {
        val request = queue.firstOrNull()
        if (request == null) {
            finishQueue()
            return
        }
        val plan = audioRouter.voiceoverPlan(request.cueId, duckLayers = request.duckLayers)
        if (plan == null) {
            queue.removeFirst()
            dispatchCommands(listOf(Play(UI, request.cueId, loop = false, fadeMs = 0L)))
            startNext()
            return
        }
        isPlaying = true
        dispatchCommands(plan.commands)
        currentJob?.cancel()
        currentJob = scope.launch(dispatcher) {
            val waitMs = plan.estimatedDurationMs.coerceAtLeast(0L)
            if (waitMs > 0) {
                delay(waitMs)
            }
            if (queue.isNotEmpty()) {
                queue.removeFirst()
            }
            if (queue.isEmpty()) {
                finishQueue()
            } else {
                startNext()
            }
        }
    }

    private fun finishQueue() {
        isPlaying = false
        currentJob?.cancel()
        currentJob = null
        val restore = audioRouter.restoreAfterVoiceover()
        if (restore.isNotEmpty()) {
            dispatchCommands(restore)
        }
    }
}
