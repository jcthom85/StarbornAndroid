package com.example.starborn.desktop

import java.lang.reflect.Proxy
import javax.sound.sampled.*

/** Controllable output line; the driver still decodes the real game assets. */
internal class TestAudioClip {
    @Volatile var open = false
    @Volatile var running = false
    @Volatile var closed = false
    @Volatile var positionMicros = 0L
    val gain = object : FloatControl(Type.MASTER_GAIN, -80f, 6f, .1f, 1, 0f, "dB") {}
    val clip: Clip = Proxy.newProxyInstance(Clip::class.java.classLoader, arrayOf(Clip::class.java)) { proxy, method, args ->
        when (method.name) {
            "open" -> {
                (args?.firstOrNull() as? AudioInputStream)?.readAllBytes()
                open = true
                null
            }
            "start", "loop" -> { running = true; null }
            "stop" -> { running = false; null }
            "close" -> { running = false; open = false; closed = true; null }
            "isOpen" -> open
            "isRunning", "isActive" -> running
            "isControlSupported" -> args?.firstOrNull() == FloatControl.Type.MASTER_GAIN
            "getControl" -> gain
            "getControls" -> arrayOf(gain)
            "getFrameLength" -> 44100
            "getFramePosition" -> 0
            "getMicrosecondPosition" -> positionMicros
            "setMicrosecondPosition" -> { positionMicros = args!![0] as Long; null }
            "getLongFramePosition", "getMicrosecondLength" -> 0L
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            "toString" -> "TestAudioClip"
            else -> null
        }
    } as Clip
}
