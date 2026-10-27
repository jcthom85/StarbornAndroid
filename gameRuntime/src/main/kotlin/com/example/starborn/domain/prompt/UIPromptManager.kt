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
    private var isToastsSuspended = false

    private fun promptPriority(prompt: UIPrompt): Int = when (prompt) {
        is TutorialPrompt -> 300
        is MilestonePrompt -> 200
        is ItemGrantedPrompt, is ItemBatchGrantedPrompt -> 100
        else -> 0
    }

    private val _state = MutableStateFlow(UIPromptState())
    val state: StateFlow<UIPromptState> = _state.asStateFlow()

    fun setToastsSuspended(suspended: Boolean) {
        synchronized(lock) {
            if (isToastsSuspended == suspended) return
            isToastsSuspended = suspended
            if (!isToastsSuspended && _state.value.current == null) {
                promoteNextLocked()
            }
        }
    }

    fun enqueue(prompt: UIPrompt, allowDuplicates: Boolean = false) {
        synchronized(lock) {
            if (!allowDuplicates) {
                val alreadyQueued = queue.any { it.id == prompt.id }
                if (alreadyQueued || _state.value.current?.id == prompt.id) {
                    return
                }
            }
            val pPriority = promptPriority(prompt)
            var insertIndex = queue.size
            for ((index, item) in queue.withIndex()) {
                if (promptPriority(item) < pPriority) {
                    insertIndex = index
                    break
                }
            }
            val list = queue.toMutableList()
            list.add(insertIndex, prompt)
            queue.clear()
            queue.addAll(list)
            if (_state.value.current == null) {
                promoteNextLocked()
            } else {
                _state.update { it.copy(queue = queue.toList()) }
            }
        }
    }

    fun dismissCurrent() {
        synchronized(lock) {
            _state.value.current?.onDismiss()
            promoteNextLocked()
        }
    }

    /** Discard obsolete queued prompts without firing callbacks for unseen UI. */
    fun removeQueued(predicate: (UIPrompt) -> Boolean) {
        synchronized(lock) {
            queue.removeIf(predicate)
            _state.update { it.copy(queue = queue.toList()) }
        }
    }

    /** Remove matching UI without treating cancellation as user completion. */
    fun discardMatching(predicate: (UIPrompt) -> Boolean) {
        synchronized(lock) {
            queue.removeIf(predicate)
            if (_state.value.current?.let(predicate) == true) {
                promoteNextLocked()
            } else {
                _state.update { it.copy(queue = queue.toList()) }
            }
        }
    }

    fun dismissItemSequence(sequenceId: String) {
        synchronized(lock) {
            val current = _state.value.current
            if (current is ItemGrantedPrompt && current.sequenceId == sequenceId) {
                current.onDismiss()
            }
            queue.removeIf { prompt ->
                prompt is ItemGrantedPrompt && prompt.sequenceId == sequenceId
            }
            promoteNextLocked()
        }
    }

    private fun promoteNextLocked() {
        if (queue.isEmpty()) {
            _state.update { it.copy(current = null, queue = emptyList()) }
            return
        }
        val nextIndex = if (isToastsSuspended) {
            queue.indexOfFirst { it !is ItemGrantedPrompt && it !is ItemBatchGrantedPrompt }
        } else {
            0
        }
        if (nextIndex < 0) {
            _state.update { it.copy(current = null, queue = queue.toList()) }
            return
        }
        val list = queue.toMutableList()
        val next = list.removeAt(nextIndex)
        queue.clear()
        queue.addAll(list)
        _state.update { it.copy(current = next, queue = queue.toList()) }
        next.onShow()
    }
}
