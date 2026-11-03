package com.example.starborn.ui.menu

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/** Prefer one line, with a strict readability floor; preserve the full name when it cannot fit. */
@Composable
fun ReadableEnemyName(name: String, modifier: Modifier = Modifier, baseSize: Int = 13) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
        val size = (baseSize downTo 11).firstOrNull { value ->
            measurer.measure(name, style.copy(fontSize = value.sp), softWrap = false).size.width <= constraints.maxWidth
        } ?: baseSize
        Text(name, modifier = modifier.fillMaxWidth(), color = Color.White,
            style = style.copy(fontSize = size.sp, lineHeight = (size + 3).sp))
    }
}
