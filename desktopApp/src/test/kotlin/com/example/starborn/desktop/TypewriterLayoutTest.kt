package com.example.starborn.desktop

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.example.starborn.shared.ui.typewriterText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TypewriterLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun revealingLettersKeepsFinalLineBreaksAndGlyphPositions() {
        lateinit var measurer: TextMeasurer
        compose.setContent { measurer = rememberTextMeasurer() }
        compose.runOnIdle {
            val text = "Nova checks the unexpectedly narrow doorway.\nOrion says: \"Keep moving!\""
            for (width in listOf(180, 360)) for (scale in listOf(1f, 2f)) {
                val constraints = Constraints(maxWidth = width)
                val density = Density(1f, scale)
                val style = TextStyle(fontSize = 18.sp, lineHeight = 26.sp)
                val complete = measurer.measure(typewriterText(text, text.length), style, constraints = constraints, density = density)
                for (count in 0..text.length) {
                    val partial = measurer.measure(typewriterText(text, count), style, constraints = constraints, density = density)
                    assertEquals(complete.size, partial.size)
                    assertEquals(complete.lineCount, partial.lineCount)
                    for (index in 0 until count) {
                        assertEquals("width=$width scale=$scale reveal=$count glyph=$index", complete.getBoundingBox(index), partial.getBoundingBox(index))
                    }
                }
            }
        }
    }
}
