package com.example.starborn.feature.fishing.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FishingHaptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun nibble() {
        vibrate(15, 40)
    }

    fun bite() {
        vibrate(30, 120)
    }

    fun warning() {
        vibrate(50, 80)
    }

    fun lineSnap() {
        vibrate(120, 300)
    }

    fun catchSuccess() {
        val pattern = longArrayOf(0, 60, 60, 80, 60, 150)
        val amplitudes = intArrayOf(0, 25, 0, 45, 0, 80)
        vibratePattern(pattern, amplitudes)
    }

    private fun vibrate(amplitude: Int, durationMs: Long) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    private fun vibratePattern(pattern: LongArray, amplitudes: IntArray) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
        }
    }
}
