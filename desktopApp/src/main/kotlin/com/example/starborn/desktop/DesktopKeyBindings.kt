package com.example.starborn.desktop

import androidx.compose.ui.input.key.*

/** Exploration bindings. Arrow navigation and Esc/Tab/Enter remain stable UI controls. */
enum class DesktopAction(val label: String, val defaultKey: String, val alias: Key? = null) {
    NORTH("Travel north", "W", Key.DirectionUp),
    SOUTH("Travel south", "S", Key.DirectionDown),
    WEST("Travel west", "A", Key.DirectionLeft),
    EAST("Travel east", "D", Key.DirectionRight),
    INTERACT("Use the first available room action", "E"),
    ACTIONS("Choose a room interaction", "Q"),
    INVENTORY("Open inventory", "I"),
    MAP("Open field map", "M"),
    MENU("Open field menu", "Esc", Key.Escape),
    QUICK_SAVE("Quick save", "F5"),
    QUICK_LOAD("Load quicksave", "F9"),
    HELP("Open controls guide", "H")
}

data class DesktopKeyBindings(val overrides: Map<DesktopAction, String> = emptyMap()) {
    fun label(action: DesktopAction): String = overrides[action] ?: action.defaultKey
    fun key(action: DesktopAction): Key = KEYS.getValue(label(action))
    fun matches(action: DesktopAction, event: KeyEvent): Boolean =
        !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed &&
            (event.key == key(action) || event.key == action.alias)

    fun actionFor(event: KeyEvent): DesktopAction? = DesktopAction.entries.firstOrNull { matches(it, event) }

    fun conflict(action: DesktopAction, keyName: String): String? {
        if (keyName !in KEYS) return "Choose a letter, number, Space, or function key."
        if (keyName in RESERVED && keyName != action.defaultKey) return "$keyName is reserved for navigation or display controls."
        val other = DesktopAction.entries.firstOrNull { it != action && label(it) == keyName }
        return other?.let { "$keyName is already assigned to ${it.label.lowercase()}." }
    }

    companion object {
        val KEYS: Map<String, Key> = linkedMapOf(
            "A" to Key.A, "B" to Key.B, "C" to Key.C, "D" to Key.D, "E" to Key.E,
            "F" to Key.F, "G" to Key.G, "H" to Key.H, "I" to Key.I, "J" to Key.J,
            "K" to Key.K, "L" to Key.L, "M" to Key.M, "N" to Key.N, "O" to Key.O,
            "P" to Key.P, "Q" to Key.Q, "R" to Key.R, "S" to Key.S, "T" to Key.T,
            "U" to Key.U, "V" to Key.V, "W" to Key.W, "X" to Key.X, "Y" to Key.Y, "Z" to Key.Z,
            "0" to Key.Zero, "1" to Key.One, "2" to Key.Two, "3" to Key.Three, "4" to Key.Four,
            "5" to Key.Five, "6" to Key.Six, "7" to Key.Seven, "8" to Key.Eight, "9" to Key.Nine,
            "F1" to Key.F1, "F2" to Key.F2, "F3" to Key.F3, "F4" to Key.F4, "F5" to Key.F5,
            "F6" to Key.F6, "F7" to Key.F7, "F8" to Key.F8, "F9" to Key.F9, "F10" to Key.F10,
            "F11" to Key.F11, "F12" to Key.F12, "Space" to Key.Spacebar,
            "Esc" to Key.Escape, "Tab" to Key.Tab, "Enter" to Key.Enter
        )
        private val RESERVED = setOf("Esc", "Tab", "Enter", "F8", "F11")
        fun nameOf(key: Key): String? = KEYS.entries.firstOrNull { it.value == key }?.key
    }
}
