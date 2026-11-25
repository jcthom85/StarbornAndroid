package com.example.starborn.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lwjgl.sdl.SDLGamepad
import org.lwjgl.sdl.SDLHints
import org.lwjgl.sdl.SDLInit
import org.lwjgl.sdl.SDLStdinc
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.math.abs

enum class DesktopInputDevice(val displayLabel: String) {
    KEYBOARD_MOUSE("Keyboard & Mouse"),
    GAMEPAD_XBOX_DECK("Xbox / Steam Deck Controller"),
    GAMEPAD_PLAYSTATION("PlayStation Controller");

    val isGamepad: Boolean get() = this != KEYBOARD_MOUSE

    fun glyphForCombat(action: DesktopAction, bindings: DesktopKeyBindings): String = when (this) {
        KEYBOARD_MOUSE -> "[${bindings.label(action)}]"
        GAMEPAD_XBOX_DECK -> when (action) {
            DesktopAction.COMBAT_ATTACK -> "(A)"
            DesktopAction.COMBAT_SKILLS -> "(X)"
            DesktopAction.COMBAT_ITEMS -> "(Y)"
            DesktopAction.COMBAT_GUARD -> "[LB]"
            DesktopAction.COMBAT_SNACK -> "[LT]"
            DesktopAction.COMBAT_RETREAT -> "[RT]"
            DesktopAction.COMBAT_LOG -> "[View]"
            else -> "[${bindings.label(action)}]"
        }
        GAMEPAD_PLAYSTATION -> when (action) {
            DesktopAction.COMBAT_ATTACK -> "(✕)"
            DesktopAction.COMBAT_SKILLS -> "(□)"
            DesktopAction.COMBAT_ITEMS -> "(△)"
            DesktopAction.COMBAT_GUARD -> "[L1]"
            DesktopAction.COMBAT_SNACK -> "[L2]"
            DesktopAction.COMBAT_RETREAT -> "[R2]"
            DesktopAction.COMBAT_LOG -> "[Create]"
            else -> "[${bindings.label(action)}]"
        }
    }

    fun glyphForExploration(action: DesktopAction, bindings: DesktopKeyBindings): String = when (this) {
        KEYBOARD_MOUSE -> bindings.label(action)
        GAMEPAD_XBOX_DECK -> when (action) {
            DesktopAction.INTERACT -> "A"
            DesktopAction.ACTIONS -> "X"
            DesktopAction.INVENTORY -> "Y"
            DesktopAction.QUICK_SAVE -> "LB"
            DesktopAction.MAP -> "RB"
            DesktopAction.MENU -> "Menu"
            DesktopAction.HELP -> "View"
            else -> bindings.label(action)
        }
        GAMEPAD_PLAYSTATION -> when (action) {
            DesktopAction.INTERACT -> "✕"
            DesktopAction.ACTIONS -> "□"
            DesktopAction.INVENTORY -> "△"
            DesktopAction.QUICK_SAVE -> "L1"
            DesktopAction.MAP -> "R1"
            DesktopAction.MENU -> "Options"
            DesktopAction.HELP -> "Create"
            else -> bindings.label(action)
        }
    }

    fun moveGlyph(bindings: DesktopKeyBindings): String = when (this) {
        KEYBOARD_MOUSE -> listOf(
            DesktopAction.NORTH,
            DesktopAction.WEST,
            DesktopAction.SOUTH,
            DesktopAction.EAST
        ).joinToString("/") { bindings.label(it) }
        GAMEPAD_XBOX_DECK, GAMEPAD_PLAYSTATION -> "L-Stick / D-Pad"
    }
}

enum class DesktopGamepadButton {
    SOUTH,
    EAST,
    WEST,
    NORTH,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_TRIGGER,
    RIGHT_TRIGGER,
    START,
    BACK
}

data class GamepadPollState(
    val connected: Boolean,
    val controllerName: String? = null,
    val pressedButtons: Set<DesktopGamepadButton> = emptySet(),
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f
)

data class DesktopGamepadContext(
    val screenState: DesktopScreenState = DesktopScreenState.MAIN_MENU,
    val isWindowForeground: Boolean = true,
    val bindings: DesktopKeyBindings = DesktopKeyBindings(),
    val explorationDialogueOrPromptOpen: Boolean = false,
    val explorationMenuOrStationOpen: Boolean = false,
    val combatSubmenuOpen: Boolean = false,
    val combatTargetOrTimedPromptActive: Boolean = false,
    val arcadeInGame: Boolean = false
)

interface DesktopGamepadBackend : AutoCloseable {
    fun poll(): GamepadPollState
    fun rumble(lowFrequency: Float, highFrequency: Float, durationMs: Int)
}

internal class NoOpGamepadBackend : DesktopGamepadBackend {
    override fun poll(): GamepadPollState = GamepadPollState(connected = false)
    override fun rumble(lowFrequency: Float, highFrequency: Float, durationMs: Int) = Unit
    override fun close() = Unit
}

internal class Sdl3GamepadBackend : DesktopGamepadBackend {
    private var initialized = false
    private var initFailed = false
    private var openHandle: Long = 0L
    private var openInstanceId: Int = 0

    private fun ensureInitialized(): Boolean {
        if (initialized) return true
        if (initFailed) return false
        return try {
            SDLHints.SDL_SetHint(SDLHints.SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "0")
            if (SDLInit.SDL_Init(SDLInit.SDL_INIT_GAMEPAD)) {
                initialized = true
                true
            } else {
                initFailed = true
                false
            }
        } catch (_: Throwable) {
            initFailed = true
            false
        }
    }

    override fun poll(): GamepadPollState {
        if (!ensureInitialized()) return GamepadPollState(connected = false)
        return try {
            SDLGamepad.SDL_UpdateGamepads()
            val ids = SDLGamepad.SDL_GetGamepads()
            if (ids == null || !ids.hasRemaining()) {
                if (ids != null) SDLStdinc.SDL_free(ids)
                closeActiveHandle()
                return GamepadPollState(connected = false)
            }
            val firstId = ids.get(0)
            SDLStdinc.SDL_free(ids)

            if (openHandle == 0L || openInstanceId != firstId || !SDLGamepad.SDL_GamepadConnected(openHandle)) {
                closeActiveHandle()
                openHandle = SDLGamepad.SDL_OpenGamepad(firstId)
                openInstanceId = if (openHandle != 0L) firstId else 0
            }
            if (openHandle == 0L) return GamepadPollState(connected = false)

            val name = SDLGamepad.SDL_GetGamepadName(openHandle) ?: "Gamepad"
            val lx = (SDLGamepad.SDL_GetGamepadAxis(openHandle, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX) / 32767f).coerceIn(-1f, 1f)
            val ly = (SDLGamepad.SDL_GetGamepadAxis(openHandle, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY) / 32767f).coerceIn(-1f, 1f)
            val lt = SDLGamepad.SDL_GetGamepadAxis(openHandle, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER)
            val rt = SDLGamepad.SDL_GetGamepadAxis(openHandle, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)

            val pressed = LinkedHashSet<DesktopGamepadButton>(8)
            fun check(btn: Int, mapped: DesktopGamepadButton) {
                if (SDLGamepad.SDL_GetGamepadButton(openHandle, btn)) pressed.add(mapped)
            }
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, DesktopGamepadButton.SOUTH)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_EAST, DesktopGamepadButton.EAST)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_WEST, DesktopGamepadButton.WEST)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH, DesktopGamepadButton.NORTH)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP, DesktopGamepadButton.DPAD_UP)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN, DesktopGamepadButton.DPAD_DOWN)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT, DesktopGamepadButton.DPAD_LEFT)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT, DesktopGamepadButton.DPAD_RIGHT)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, DesktopGamepadButton.LEFT_SHOULDER)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, DesktopGamepadButton.RIGHT_SHOULDER)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_START, DesktopGamepadButton.START)
            check(SDLGamepad.SDL_GAMEPAD_BUTTON_BACK, DesktopGamepadButton.BACK)

            if (lt > TRIGGER_PRESS_THRESHOLD) pressed.add(DesktopGamepadButton.LEFT_TRIGGER)
            if (rt > TRIGGER_PRESS_THRESHOLD) pressed.add(DesktopGamepadButton.RIGHT_TRIGGER)
            if (ly < -STICK_DEADZONE) pressed.add(DesktopGamepadButton.DPAD_UP)
            if (ly > STICK_DEADZONE) pressed.add(DesktopGamepadButton.DPAD_DOWN)
            if (lx < -STICK_DEADZONE) pressed.add(DesktopGamepadButton.DPAD_LEFT)
            if (lx > STICK_DEADZONE) pressed.add(DesktopGamepadButton.DPAD_RIGHT)

            GamepadPollState(
                connected = true,
                controllerName = name,
                pressedButtons = pressed,
                leftStickX = lx,
                leftStickY = ly
            )
        } catch (_: Throwable) {
            GamepadPollState(connected = false)
        }
    }

    override fun rumble(lowFrequency: Float, highFrequency: Float, durationMs: Int) {
        if (!initialized || openHandle == 0L || durationMs <= 0) return
        try {
            val low = (lowFrequency.coerceIn(0f, 1f) * 65535f).toInt().toShort()
            val high = (highFrequency.coerceIn(0f, 1f) * 65535f).toInt().toShort()
            SDLGamepad.SDL_RumbleGamepad(openHandle, low, high, durationMs)
        } catch (_: Throwable) {}
    }

    private fun closeActiveHandle() {
        if (openHandle != 0L) {
            try {
                SDLGamepad.SDL_CloseGamepad(openHandle)
            } catch (_: Throwable) {}
            openHandle = 0L
            openInstanceId = 0
        }
    }

    override fun close() {
        closeActiveHandle()
        if (initialized) {
            try {
                SDLInit.SDL_QuitSubSystem(SDLInit.SDL_INIT_GAMEPAD)
            } catch (_: Throwable) {}
            initialized = false
        }
    }

    companion object {
        private const val STICK_DEADZONE = 0.48f
        private const val TRIGGER_PRESS_THRESHOLD: Short = 11_000
    }
}

class DesktopGamepadService(
    private val backend: DesktopGamepadBackend = NoOpGamepadBackend(),
    startPollingLoop: Boolean = false,
    private val keyEventSink: ((keyCode: Int, pressed: Boolean) -> Unit)? = null
) : AutoCloseable {

    private val _activeInputDevice = MutableStateFlow(DesktopInputDevice.KEYBOARD_MOUSE)
    val activeInputDevice: StateFlow<DesktopInputDevice> = _activeInputDevice.asStateFlow()

    private val _connectedControllerName = MutableStateFlow<String?>(null)
    val connectedControllerName: StateFlow<String?> = _connectedControllerName.asStateFlow()

    @Volatile
    var context: DesktopGamepadContext = DesktopGamepadContext()

    @Volatile
    private var attachedWindow: Window? = null

    private val activeKeyByButton = HashMap<DesktopGamepadButton, Int>()
    private var previousButtons: Set<DesktopGamepadButton> = emptySet()
    private var poller: ScheduledExecutorService? = null
    private var awtListener: AWTEventListener? = null

    init {
        if (startPollingLoop) {
            startPolling()
        }
    }

    fun attachWindow(window: Window?) {
        attachedWindow = window
        if (window != null && awtListener == null) {
            val listener = AWTEventListener { event ->
                when (event) {
                    is KeyEvent -> {
                        if (event.`when` != SYNTHETIC_EVENT_TIMESTAMP && _activeInputDevice.value != DesktopInputDevice.KEYBOARD_MOUSE) {
                            _activeInputDevice.value = DesktopInputDevice.KEYBOARD_MOUSE
                        }
                    }
                    is MouseEvent -> {
                        if ((event.id == MouseEvent.MOUSE_PRESSED || event.id == MouseEvent.MOUSE_WHEEL) &&
                            _activeInputDevice.value != DesktopInputDevice.KEYBOARD_MOUSE
                        ) {
                            _activeInputDevice.value = DesktopInputDevice.KEYBOARD_MOUSE
                        }
                    }
                }
            }
            awtListener = listener
            runCatching {
                Toolkit.getDefaultToolkit().addAWTEventListener(
                    listener,
                    AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_WHEEL_EVENT_MASK
                )
            }
        }
    }

    private fun startPolling() {
        val executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "starborn-gamepad-poller").apply { isDaemon = true }
        }
        poller = executor
        executor.scheduleAtFixedRate(::pollOnce, 32, 16, TimeUnit.MILLISECONDS)
    }

    fun pollOnce() {
        val state = backend.poll()
        _connectedControllerName.value = if (state.connected) state.controllerName else null
        val ctx = context
        if (!state.connected || !ctx.isWindowForeground) {
            if (activeKeyByButton.isNotEmpty()) {
                val toRelease = activeKeyByButton.values.toList()
                activeKeyByButton.clear()
                previousButtons = emptySet()
                toRelease.forEach { code -> emitKey(code, pressed = false) }
            }
            if (!state.connected && _activeInputDevice.value.isGamepad) {
                _activeInputDevice.value = DesktopInputDevice.KEYBOARD_MOUSE
            }
            return
        }

        val current = state.pressedButtons
        val newlyPressed = current - previousButtons
        val newlyReleased = previousButtons - current
        previousButtons = current

        if (newlyPressed.isNotEmpty()) {
            _activeInputDevice.value = classifyControllerFamily(state.controllerName)
        }

        newlyReleased.forEach { button ->
            activeKeyByButton.remove(button)?.let { code ->
                emitKey(code, pressed = false)
            }
        }

        newlyPressed.forEach { button ->
            val keyCode = resolveGamepadAwtKeyCode(button, ctx)
            if (keyCode != null) {
                activeKeyByButton[button] = keyCode
                emitKey(keyCode, pressed = true)
            }
        }
    }

    fun rumble(lowFrequency: Float, highFrequency: Float, durationMs: Int = 160) {
        if (!context.isWindowForeground) return
        backend.rumble(lowFrequency, highFrequency, durationMs)
    }

    private fun emitKey(keyCode: Int, pressed: Boolean) {
        keyEventSink?.let { sink ->
            sink(keyCode, pressed)
            return
        }
        val window = attachedWindow ?: return
        SwingUtilities.invokeLater {
            val target: Component = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                ?.takeIf { SwingUtilities.isDescendingFrom(it, window) }
                ?: window
            val id = if (pressed) KeyEvent.KEY_PRESSED else KeyEvent.KEY_RELEASED
            val keyChar = keyCharForAwtCode(keyCode)
            val event = KeyEvent(
                target,
                id,
                SYNTHETIC_EVENT_TIMESTAMP,
                0,
                keyCode,
                keyChar,
                KeyEvent.KEY_LOCATION_STANDARD
            )
            target.dispatchEvent(event)
        }
    }

    override fun close() {
        awtListener?.let { listener ->
            runCatching { Toolkit.getDefaultToolkit().removeAWTEventListener(listener) }
            awtListener = null
        }
        poller?.let { exec ->
            try {
                exec.submit { backend.close() }.get(500, TimeUnit.MILLISECONDS)
            } catch (_: Throwable) {
                runCatching { backend.close() }
            }
            exec.shutdownNow()
            poller = null
        } ?: runCatching { backend.close() }
    }

    companion object {
        internal const val SYNTHETIC_EVENT_TIMESTAMP = 77_777_777L

        fun classifyControllerFamily(controllerName: String?): DesktopInputDevice {
            val lower = controllerName.orEmpty().lowercase()
            return if (
                lower.contains("dualsense") ||
                lower.contains("dualshock") ||
                lower.contains("playstation") ||
                lower.contains("ps4") ||
                lower.contains("ps5") ||
                lower.contains("sony")
            ) {
                DesktopInputDevice.GAMEPAD_PLAYSTATION
            } else {
                DesktopInputDevice.GAMEPAD_XBOX_DECK
            }
        }

        fun resolveGamepadAwtKeyCode(button: DesktopGamepadButton, ctx: DesktopGamepadContext): Int? {
            val b = ctx.bindings
            return when (button) {
                DesktopGamepadButton.DPAD_UP -> KeyEvent.VK_UP
                DesktopGamepadButton.DPAD_DOWN -> KeyEvent.VK_DOWN
                DesktopGamepadButton.DPAD_LEFT -> KeyEvent.VK_LEFT
                DesktopGamepadButton.DPAD_RIGHT -> KeyEvent.VK_RIGHT
                DesktopGamepadButton.EAST -> KeyEvent.VK_ESCAPE
                DesktopGamepadButton.START -> KeyEvent.VK_ESCAPE
                DesktopGamepadButton.BACK -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> b.awtKeyCode(DesktopAction.COMBAT_LOG) ?: KeyEvent.VK_L
                    DesktopScreenState.EXPLORATION -> b.awtKeyCode(DesktopAction.HELP) ?: KeyEvent.VK_H
                    else -> null
                }
                DesktopGamepadButton.SOUTH -> when (ctx.screenState) {
                    DesktopScreenState.FISHING -> KeyEvent.VK_SPACE
                    DesktopScreenState.ARCADE -> if (ctx.arcadeInGame) KeyEvent.VK_SPACE else KeyEvent.VK_ENTER
                    DesktopScreenState.COMBAT -> when {
                        ctx.combatTargetOrTimedPromptActive -> KeyEvent.VK_SPACE
                        ctx.combatSubmenuOpen -> KeyEvent.VK_1
                        else -> b.awtKeyCode(DesktopAction.COMBAT_ATTACK) ?: KeyEvent.VK_1
                    }
                    DesktopScreenState.EXPLORATION -> when {
                        ctx.explorationDialogueOrPromptOpen || ctx.explorationMenuOrStationOpen -> KeyEvent.VK_ENTER
                        else -> b.awtKeyCode(DesktopAction.INTERACT) ?: KeyEvent.VK_E
                    }
                    else -> KeyEvent.VK_ENTER
                }
                DesktopGamepadButton.WEST -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> b.awtKeyCode(DesktopAction.COMBAT_SKILLS) ?: KeyEvent.VK_2
                    DesktopScreenState.ARCADE -> b.awtKeyCode(DesktopAction.INTERACT) ?: KeyEvent.VK_E
                    DesktopScreenState.EXPLORATION -> if (ctx.explorationMenuOrStationOpen) KeyEvent.VK_1 else (b.awtKeyCode(DesktopAction.ACTIONS) ?: KeyEvent.VK_Q)
                    else -> null
                }
                DesktopGamepadButton.NORTH -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> b.awtKeyCode(DesktopAction.COMBAT_ITEMS) ?: KeyEvent.VK_3
                    DesktopScreenState.EXPLORATION -> if (ctx.explorationMenuOrStationOpen) KeyEvent.VK_2 else (b.awtKeyCode(DesktopAction.INVENTORY) ?: KeyEvent.VK_I)
                    else -> null
                }
                DesktopGamepadButton.LEFT_SHOULDER -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> b.awtKeyCode(DesktopAction.COMBAT_GUARD) ?: KeyEvent.VK_G
                    DesktopScreenState.ARCADE -> b.awtKeyCode(DesktopAction.ARCADE_LANE_1) ?: KeyEvent.VK_D
                    DesktopScreenState.EXPLORATION -> if (ctx.explorationMenuOrStationOpen) KeyEvent.VK_Q else (b.awtKeyCode(DesktopAction.QUICK_SAVE) ?: KeyEvent.VK_F5)
                    DesktopScreenState.FIELD_KIT -> KeyEvent.VK_OPEN_BRACKET
                    else -> KeyEvent.VK_Q
                }
                DesktopGamepadButton.RIGHT_SHOULDER -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> KeyEvent.VK_TAB
                    DesktopScreenState.ARCADE -> b.awtKeyCode(DesktopAction.ARCADE_LANE_4) ?: KeyEvent.VK_K
                    DesktopScreenState.EXPLORATION -> if (ctx.explorationMenuOrStationOpen) KeyEvent.VK_E else (b.awtKeyCode(DesktopAction.MAP) ?: KeyEvent.VK_M)
                    DesktopScreenState.FIELD_KIT -> KeyEvent.VK_CLOSE_BRACKET
                    else -> KeyEvent.VK_E
                }
                DesktopGamepadButton.LEFT_TRIGGER -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> b.awtKeyCode(DesktopAction.COMBAT_SNACK) ?: KeyEvent.VK_4
                    DesktopScreenState.ARCADE -> b.awtKeyCode(DesktopAction.ARCADE_LANE_2) ?: KeyEvent.VK_F
                    else -> KeyEvent.VK_OPEN_BRACKET
                }
                DesktopGamepadButton.RIGHT_TRIGGER -> when (ctx.screenState) {
                    DesktopScreenState.COMBAT -> b.awtKeyCode(DesktopAction.COMBAT_RETREAT) ?: KeyEvent.VK_R
                    DesktopScreenState.ARCADE -> b.awtKeyCode(DesktopAction.ARCADE_LANE_3) ?: KeyEvent.VK_J
                    else -> KeyEvent.VK_CLOSE_BRACKET
                }
            }
        }

        private fun keyCharForAwtCode(code: Int): Char = when (code) {
            in KeyEvent.VK_A..KeyEvent.VK_Z -> ('a'.code + (code - KeyEvent.VK_A)).toChar()
            in KeyEvent.VK_0..KeyEvent.VK_9 -> ('0'.code + (code - KeyEvent.VK_0)).toChar()
            KeyEvent.VK_SPACE -> ' '
            KeyEvent.VK_ENTER -> '\n'
            KeyEvent.VK_ESCAPE -> 27.toChar()
            KeyEvent.VK_TAB -> '\t'
            KeyEvent.VK_OPEN_BRACKET -> '['
            KeyEvent.VK_CLOSE_BRACKET -> ']'
            else -> KeyEvent.CHAR_UNDEFINED
        }
    }
}
