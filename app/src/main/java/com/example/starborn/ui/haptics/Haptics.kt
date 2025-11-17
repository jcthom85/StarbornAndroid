package com.example.starborn.ui.haptics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

enum class HapticType { TICK, SUCCESS, ALERT }

object Haptics {

    @Suppress("DEPRECATION")
    fun pulse(context: Context, type: HapticType) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        val effect = when (type) {
            HapticType.TICK -> VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticType.SUCCESS -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticType.ALERT -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}
