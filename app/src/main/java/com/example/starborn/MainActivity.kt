package com.example.starborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.starborn.navigation.NavigationHost
import com.example.starborn.ui.theme.StarbornTheme

private const val EXTRA_DEBUG_COMBAT_ACTION_TEXT = "debug_combat_action_text"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val showCombatActionText = intent?.getBooleanExtra(EXTRA_DEBUG_COMBAT_ACTION_TEXT, false) == true
        setContent {
            StarbornTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    NavigationHost(showCombatActionText = showCombatActionText)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    StarbornTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            NavigationHost()
        }
    }
}
