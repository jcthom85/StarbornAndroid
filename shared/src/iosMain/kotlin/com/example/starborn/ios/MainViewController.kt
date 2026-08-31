package com.example.starborn.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Main iOS Entry point returning a UIViewController for Swift / Xcode integration.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    IosStarbornApp()
}

@Composable
fun IosStarbornApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF07040A)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STARBORN",
                    color = Color(0xFF4DEEEA),
                    fontSize = 36.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "iOS Universal Target (Active)",
                    color = Color.LightGray,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Powered by Kotlin & Compose Multiplatform",
                    color = Color(0xFFFFB703),
                    fontSize = 13.sp
                )
            }
        }
    }
}
