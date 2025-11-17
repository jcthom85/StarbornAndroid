package com.example.starborn.domain.prompt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque

data class UIPromptState(
    val current: UIPrompt? = null,
    val queue: List<UIPrompt> = emptyList()
)

class UIPromptManager {

    private val lock = Any()
    private val queue: ArrayDeque<UIPrompt> = ArrayDeque()

    private val _state = MutableStateFlow(UIPromptState())
    val state: StateFlow<UIPromptState> = _state.asStateFlow()

    fun enqueue(prompt: UIPrompt, allowDuplicates: Boolean = false) {
        synchronized(lock) {
            if (!allowDuplicates) {
                val alreadyQueued = queue.any { it.id == prompt.id }
                if (alreadyQueued || _state.value.current?.id == prompt.id) {
                    return
                }
            }
            queue.addLast(prompt)
            if (_state.value.current == null) {
                promoteNextLocked()
            }
        }
    }

    fun dismissCurrent() {
        synchronized(lock) {
            _state.value.current?.onDismiss()
            promoteNextLocked()
        }
    }

    private fun promoteNextLocked() {
        val next = if (queue.isEmpty()) null else queue.removeFirst()
        _state.update { it.copy(current = next, queue = queue.toList()) }
        next?.onShow()
    }
}