package com.example.starborn.feature.fishing.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Simple upward-jerk detector that listens for a sudden negative acceleration on the device's Y axis.
 * Uses the linear acceleration sensor when available, falling back to the general accelerometer.
 */
class HookMotionDetector(
    private val sensorManager: SensorManager?
) : SensorEventListener {

    private var sensor: Sensor? = null
    private var running = false
    private var lastTriggerTime = 0L
    private var callback: (() -> Unit)? = null

    fun isSupported(): Boolean = resolveSensor() != null

    fun start(onHook: () -> Unit) {
        val targetSensor = resolveSensor() ?: return
        callback = onHook
        if (running) return
        sensorManager?.registerListener(this, targetSensor, SensorManager.SENSOR_DELAY_GAME)
        running = true
    }

    fun stop() {
        if (!running) return
        sensorManager?.unregisterListener(this)
        running = false
        callback = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < COOLDOWN_MS) return
        val axisY = event.values.getOrNull(1) ?: return
        val axisZ = event.values.getOrNull(2) ?: 0f
        val jerkDetected = -axisY > UPWARD_THRESHOLD && abs(axisZ) < Z_FILTER
        if (jerkDetected) {
            lastTriggerTime = now
            callback?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun resolveSensor(): Sensor? {
        if (sensor != null) return sensor
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        return sensor
    }

    companion object {
        private const val UPWARD_THRESHOLD = 4.5f
        private const val Z_FILTER = 5f
        private const val COOLDOWN_MS = 400L
    }
}
