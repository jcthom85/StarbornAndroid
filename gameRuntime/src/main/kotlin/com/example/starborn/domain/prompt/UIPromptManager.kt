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
    private val presentationBlockers = mutableSetOf<String>()
    private var activeItemSequence: String? = null
    private var activeUngroupedItems = false

    private fun promptPriority(prompt: UIPrompt): Int = when (prompt) {
        is TutorialPrompt -> if (prompt.afterRewards) 50 else 300
        is MilestonePrompt -> 200
        is ItemGrantedPrompt -> if (prompt.category == "consumed") 350 else 100
        is ItemBatchGrantedPrompt -> 100
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

    /** Independent owners cannot accidentally resume each other's blocked notifications. */
    fun setPresentationBlocked(owner: String, blocked: Boolean) {
        synchronized(lock) {
            val changed = if (blocked) presentationBlockers.add(owner) else presentationBlockers.remove(owner)
            if (changed && presentationBlockers.isEmpty() && _state.value.current == null) promoteNextLocked()
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

    fun dismissCurrent(expectedId: String? = null) {
        synchronized(lock) {
            val current = _state.value.current ?: return
            if (expectedId != null && current.id != expectedId) return
            _state.update { it.copy(current = null) }
            current.onDismiss()
            if (_state.value.current == null) promoteNextLocked()
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
            val dismissesCurrent = current is ItemGrantedPrompt && current.sequenceId == sequenceId
            if (activeItemSequence == sequenceId) activeItemSequence = null
            if (dismissesCurrent) _state.update { it.copy(current = null) }
            queue.removeIf { prompt ->
                prompt is ItemGrantedPrompt && prompt.sequenceId == sequenceId
            }
            if (dismissesCurrent) current?.onDismiss()
            if (_state.value.current == null) promoteNextLocked()
            else _state.update { it.copy(queue = queue.toList()) }
        }
    }

    private fun promoteNextLocked() {
        if (presentationBlockers.isNotEmpty()) {
            _state.update { it.copy(current = null, queue = queue.toList()) }
            return
        }
        if (queue.isEmpty()) {
            activeItemSequence = null
            activeUngroupedItems = false
            _state.update { it.copy(current = null, queue = emptyList()) }
            return
        }
        // Once a reward group starts, finish it before an unrelated queued tutorial or milestone.
        // Scene blockers still pause presentation; they never discard the remainder of the group.
        val groupIndex = if (activeUngroupedItems) {
            queue.indexOfFirst { it is ItemGrantedPrompt && it.sequenceId == null }
        } else activeItemSequence?.let { sequence ->
            queue.indexOfFirst { it is ItemGrantedPrompt && it.sequenceId == sequence }
        } ?: -1
        if (groupIndex < 0) {
            activeItemSequence = null
            activeUngroupedItems = false
        }
        val nextIndex = when {
            groupIndex >= 0 && isToastsSuspended -> -1
            groupIndex >= 0 -> groupIndex
            isToastsSuspended -> queue.indexOfFirst { it !is ItemGrantedPrompt && it !is ItemBatchGrantedPrompt }
            else -> 0
        }
        if (nextIndex < 0) {
            _state.update { it.copy(current = null, queue = queue.toList()) }
            return
        }
        val list = queue.toMutableList()
        val next = list.removeAt(nextIndex)
        if (next is ItemGrantedPrompt && next.category != "consumed") {
            activeItemSequence = next.sequenceId
            activeUngroupedItems = next.sequenceId == null
        }
        queue.clear()
        queue.addAll(list)
        _state.update { it.copy(current = next, queue = queue.toList()) }
        next.onShow()
    }
}
