package com.example.starborn.ui.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UiEventBus {
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun tryEmit(event: UiEvent) = _events.tryEmit(event)
}
