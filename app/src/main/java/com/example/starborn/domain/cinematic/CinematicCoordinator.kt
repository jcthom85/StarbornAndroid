package com.example.starborn.domain.cinematic

import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CinematicPlaybackState(
    val scene: CinematicScene,
    val stepIndex: Int
)

class CinematicCoordinator(
    private val cinematicService: CinematicService
) {

    private val queue: ArrayDeque<Pair<CinematicScene, () -> Unit>> = ArrayDeque()
    private val _state = MutableStateFlow<CinematicPlaybackState?>(null)
    val state: StateFlow<CinematicPlaybackState?> = _state.asStateFlow()

    private val lock = Any()
    private var activeScene: CinematicScene? = null
    private var activeCompletion: (() -> Unit)? = null
    private var stepIndex: Int = 0
    private var onSceneStart: (CinematicScene) -> Unit = {}
    private var onSceneEnd: () -> Unit = {}

    fun setCallbacks(
        onSceneStart: (CinematicScene) -> Unit = {},
        onSceneEnd: () -> Unit = {}
    ) {
        synchronized(lock) {
            this.onSceneStart = onSceneStart
            this.onSceneEnd = onSceneEnd
        }
    }

    fun play(sceneId: String?, onComplete: () -> Unit = {}): Boolean {
        val scene = cinematicService.scene(sceneId)
        if (scene == null || scene.steps.isEmpty()) {
            return false
        }
        synchronized(lock) {
            if (activeScene == null) {
                startScene(scene, onComplete)
            } else {
                queue.addLast(scene to onComplete)
            }
        }
        return true
    }

    fun advance() {
        synchronized(lock) {
            val scene = activeScene ?: return
            val nextIndex = stepIndex + 1
            if (nextIndex >= scene.steps.size) {
                finishActiveScene()
            } else {
                stepIndex = nextIndex
                _state.value = CinematicPlaybackState(scene, stepIndex)
            }
        }
    }

    private fun startScene(scene: CinematicScene, completion: () -> Unit) {
        activeScene = scene
        activeCompletion = completion
        stepIndex = 0
        onSceneStart(scene)
        _state.value = CinematicPlaybackState(scene, stepIndex)
    }

    private fun finishActiveScene() {
        val completion = activeCompletion
        activeScene = null
        activeCompletion = null
        stepIndex = 0
        _state.value = null
        onSceneEnd()
        completion?.invoke()
        promoteNextScene()
    }

    private fun promoteNextScene() {
        if (queue.isEmpty()) return
        val (scene, completion) = queue.removeFirst()
        startScene(scene, completion)
    }
}
