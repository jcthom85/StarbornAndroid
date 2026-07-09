package com.example.starborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.starborn.domain.telemetry.AppVisibility
import com.example.starborn.domain.telemetry.TelemetryLogger
import com.example.starborn.navigation.NavigationHost
import com.example.starborn.ui.theme.StarbornTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialDebugScenarioId = if (BuildConfig.DEBUG) {
            intent?.getStringExtra(DEBUG_SCENARIO_EXTRA)
        } else {
            null
        }
        setContent {
            StarbornTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    NavigationHost(
                        showCombatActionText = true,
                        initialDebugScenarioId = initialDebugScenarioId
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.onStart()
        TelemetryLogger.get(this).log("app_foreground")
    }

    override fun onStop() {
        TelemetryLogger.get(this).log("app_background")
        AppVisibility.onStop()
        super.onStop()
    }

    companion object {
        const val DEBUG_SCENARIO_EXTRA = "starborn.test.scenarioId"
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    StarbornTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            NavigationHost(showCombatActionText = true)
        }
    }
}
