package com.example.starborn.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class TypewriterTextTest {
    private val narrationColor = Color(0xFF80E0FF)
    private val source = buildAnnotatedString {
        append("She smiles. Hello.")
        addStyle(SpanStyle(color = narrationColor, fontStyle = FontStyle.Italic), 0, 11)
    }

    private fun styleAt(text: AnnotatedString, index: Int): SpanStyle =
        text.spanStyles.filter { index >= it.start && index < it.end }
            .fold(SpanStyle()) { style, range -> style.merge(range.item) }

    @Test fun narrationStaysHiddenUntilRevealedWithoutChangingLayout() {
        for (revealed in 0..source.length) {
            val result = typewriterText(source, revealed)
            assertEquals(source.text, result.text)
            for (index in source.indices) {
                val style = styleAt(result, index)
                if (index >= revealed) assertEquals(Color.Transparent, style.color)
                else if (index < 11) {
                    assertEquals(narrationColor, style.color)
                    assertEquals(FontStyle.Italic, style.fontStyle)
                }
            }
        }
    }

    @Test fun revealCountClampsAndPlainTextKeepsItsExistingBehavior() {
        assertEquals(Color.Transparent, styleAt(typewriterText(source, -1), 0).color)
        assertEquals(source, typewriterText(source, source.length + 10))
        assertEquals(Color.Transparent, styleAt(typewriterText("Hello", 2), 2).color)
        assertEquals(AnnotatedString("Hello"), typewriterText("Hello", 20))
    }
}
