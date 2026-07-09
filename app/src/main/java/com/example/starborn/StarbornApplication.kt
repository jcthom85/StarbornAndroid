package com.example.starborn

import android.app.Application
import com.example.starborn.domain.telemetry.CrashRecorder
import com.example.starborn.domain.telemetry.TelemetryLogger

class StarbornApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashRecorder.install(filesDir, TelemetryLogger.get(this))
    }
}
