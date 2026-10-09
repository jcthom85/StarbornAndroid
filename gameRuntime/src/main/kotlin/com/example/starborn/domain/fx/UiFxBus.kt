package com.example.starborn.domain.fx

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UiFxBus {
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val fxEvents: SharedFlow<String> = events.asSharedFlow()

    fun trigger(id: String) {
        if (id.isBlank()) return
        events.tryEmit(id)
    }
}
