package com.example.starborn.desktop.ui.arcade

import androidx.compose.runtime.*
import java.awt.KeyboardFocusManager
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener

@Composable
internal fun DesktopArcadeKeys(onKey: (Int, Boolean) -> Unit) {
    val callback by rememberUpdatedState(onKey)
    DisposableEffect(Unit) {
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val heldKeys = mutableSetOf<Int>()
        fun releaseHeldKeys() {
            heldKeys.toList().forEach { callback(it, false) }
            heldKeys.clear()
        }
        val focusListener = PropertyChangeListener { event ->
            if (event.newValue == null) releaseHeldKeys()
        }
        val dispatcher = KeyEventDispatcher { event ->
            if (event.id == KeyEvent.KEY_PRESSED || event.id == KeyEvent.KEY_RELEASED) {
                val pressed = event.id == KeyEvent.KEY_PRESSED
                if (pressed) heldKeys.add(event.keyCode) else heldKeys.remove(event.keyCode)
                callback(event.keyCode, pressed)
            }
            false
        }
        manager.addKeyEventDispatcher(dispatcher)
        manager.addPropertyChangeListener("activeWindow", focusListener)
        onDispose {
            releaseHeldKeys()
            manager.removePropertyChangeListener("activeWindow", focusListener)
            manager.removeKeyEventDispatcher(dispatcher)
        }
    }
}

@Composable
internal fun BackHandler(onBack: () -> Unit) {
    DesktopArcadeKeys { code, pressed -> if (code == KeyEvent.VK_ESCAPE && pressed) onBack() }
}
