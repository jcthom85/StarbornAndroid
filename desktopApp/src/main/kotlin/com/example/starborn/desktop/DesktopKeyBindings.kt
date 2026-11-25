package com.example.starborn.desktop

import androidx.compose.ui.input.key.*
import java.awt.event.KeyEvent as AwtKeyEvent

enum class DesktopBindingScope(val title: String) {
    EXPLORATION("Exploration"),
    COMBAT("Combat"),
    ARCADE("Arcade")
}

/** Scoped PC key bindings. Arrow navigation and Esc/Tab/Enter remain stable UI controls. */
enum class DesktopAction(
    val label: String,
    val defaultKey: String,
    val alias: Key? = null,
    val scope: DesktopBindingScope = DesktopBindingScope.EXPLORATION
) {
    NORTH("Travel north", "W", Key.DirectionUp, DesktopBindingScope.EXPLORATION),
    SOUTH("Travel south", "S", Key.DirectionDown, DesktopBindingScope.EXPLORATION),
    WEST("Travel west", "A", Key.DirectionLeft, DesktopBindingScope.EXPLORATION),
    EAST("Travel east", "D", Key.DirectionRight, DesktopBindingScope.EXPLORATION),
    INTERACT("Use the first available room action", "E", scope = DesktopBindingScope.EXPLORATION),
    ACTIONS("Choose a room interaction", "Q", scope = DesktopBindingScope.EXPLORATION),
    INVENTORY("Open inventory", "I", scope = DesktopBindingScope.EXPLORATION),
    MAP("Open field map", "M", scope = DesktopBindingScope.EXPLORATION),
    MENU("Open field menu", "Esc", Key.Escape, DesktopBindingScope.EXPLORATION),
    QUICK_SAVE("Quick save", "F5", scope = DesktopBindingScope.EXPLORATION),
    QUICK_LOAD("Load quicksave", "F9", scope = DesktopBindingScope.EXPLORATION),
    HELP("Open controls guide", "H", scope = DesktopBindingScope.EXPLORATION),

    COMBAT_ATTACK("Attack", "1", scope = DesktopBindingScope.COMBAT),
    COMBAT_SKILLS("Abilities menu", "2", scope = DesktopBindingScope.COMBAT),
    COMBAT_ITEMS("Items menu", "3", scope = DesktopBindingScope.COMBAT),
    COMBAT_SNACK("Equipped snack", "4", scope = DesktopBindingScope.COMBAT),
    COMBAT_GUARD("Guard stance", "G", scope = DesktopBindingScope.COMBAT),
    COMBAT_RETREAT("Retreat from battle", "R", scope = DesktopBindingScope.COMBAT),
    COMBAT_LOG("Battle log", "L", scope = DesktopBindingScope.COMBAT),

    ARCADE_LANE_1("Harmonic Pulse lane 1", "D", scope = DesktopBindingScope.ARCADE),
    ARCADE_LANE_2("Harmonic Pulse lane 2", "F", scope = DesktopBindingScope.ARCADE),
    ARCADE_LANE_3("Harmonic Pulse lane 3", "J", scope = DesktopBindingScope.ARCADE),
    ARCADE_LANE_4("Harmonic Pulse lane 4", "K", scope = DesktopBindingScope.ARCADE)
}

data class DesktopKeyBindings(val overrides: Map<DesktopAction, String> = emptyMap()) {
    fun label(action: DesktopAction): String = overrides[action] ?: action.defaultKey
    fun key(action: DesktopAction): Key = KEYS.getValue(label(action))
    fun awtKeyCode(action: DesktopAction): Int? = AWT_KEYS[label(action)]

    fun matches(action: DesktopAction, event: KeyEvent): Boolean =
        !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed &&
            (event.key == key(action) || event.key == action.alias)

    fun actionFor(event: KeyEvent, scope: DesktopBindingScope = DesktopBindingScope.EXPLORATION): DesktopAction? =
        DesktopAction.entries.firstOrNull { it.scope == scope && matches(it, event) }

    fun conflict(action: DesktopAction, keyName: String): String? {
        if (keyName !in KEYS) return "Choose a letter, number, Space, or function key."
        if (keyName in RESERVED && keyName != action.defaultKey) return "$keyName is reserved for navigation or display controls."
        val other = DesktopAction.entries.firstOrNull { it != action && it.scope == action.scope && label(it) == keyName }
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
        val AWT_KEYS: Map<String, Int> = linkedMapOf(
            "A" to AwtKeyEvent.VK_A, "B" to AwtKeyEvent.VK_B, "C" to AwtKeyEvent.VK_C, "D" to AwtKeyEvent.VK_D,
            "E" to AwtKeyEvent.VK_E, "F" to AwtKeyEvent.VK_F, "G" to AwtKeyEvent.VK_G, "H" to AwtKeyEvent.VK_H,
            "I" to AwtKeyEvent.VK_I, "J" to AwtKeyEvent.VK_J, "K" to AwtKeyEvent.VK_K, "L" to AwtKeyEvent.VK_L,
            "M" to AwtKeyEvent.VK_M, "N" to AwtKeyEvent.VK_N, "O" to AwtKeyEvent.VK_O, "P" to AwtKeyEvent.VK_P,
            "Q" to AwtKeyEvent.VK_Q, "R" to AwtKeyEvent.VK_R, "S" to AwtKeyEvent.VK_S, "T" to AwtKeyEvent.VK_T,
            "U" to AwtKeyEvent.VK_U, "V" to AwtKeyEvent.VK_V, "W" to AwtKeyEvent.VK_W, "X" to AwtKeyEvent.VK_X,
            "Y" to AwtKeyEvent.VK_Y, "Z" to AwtKeyEvent.VK_Z,
            "0" to AwtKeyEvent.VK_0, "1" to AwtKeyEvent.VK_1, "2" to AwtKeyEvent.VK_2, "3" to AwtKeyEvent.VK_3,
            "4" to AwtKeyEvent.VK_4, "5" to AwtKeyEvent.VK_5, "6" to AwtKeyEvent.VK_6, "7" to AwtKeyEvent.VK_7,
            "8" to AwtKeyEvent.VK_8, "9" to AwtKeyEvent.VK_9,
            "F1" to AwtKeyEvent.VK_F1, "F2" to AwtKeyEvent.VK_F2, "F3" to AwtKeyEvent.VK_F3, "F4" to AwtKeyEvent.VK_F4,
            "F5" to AwtKeyEvent.VK_F5, "F6" to AwtKeyEvent.VK_F6, "F7" to AwtKeyEvent.VK_F7, "F8" to AwtKeyEvent.VK_F8,
            "F9" to AwtKeyEvent.VK_F9, "F10" to AwtKeyEvent.VK_F10, "F11" to AwtKeyEvent.VK_F11, "F12" to AwtKeyEvent.VK_F12,
            "Space" to AwtKeyEvent.VK_SPACE, "Esc" to AwtKeyEvent.VK_ESCAPE, "Tab" to AwtKeyEvent.VK_TAB, "Enter" to AwtKeyEvent.VK_ENTER
        )
        private val RESERVED = setOf("Esc", "Tab", "Enter", "F8", "F11")
        fun nameOf(key: Key): String? = KEYS.entries.firstOrNull { it.value == key }?.key
    }
}
