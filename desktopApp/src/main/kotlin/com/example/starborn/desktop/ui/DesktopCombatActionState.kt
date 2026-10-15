package com.example.starborn.desktop.ui

internal fun combatActionState(alive: Boolean, executing: Boolean, choosing: Boolean, ready: Boolean): String? = when {
    !alive -> "DOWN"
    executing -> "ACTING"
    choosing -> "CHOOSING"
    ready -> "READY"
    else -> null
}
