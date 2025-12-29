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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StarbornTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    NavigationHost()
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
