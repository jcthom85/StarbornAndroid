package com.example.starborn.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/** Keep complete words in the layout while revealing their letters. */
fun typewriterText(text: String, revealedCount: Int): AnnotatedString =
    typewriterText(AnnotatedString(text), revealedCount)

/** Apply the reveal mask last so authored colors cannot expose unrevealed narration. */
fun typewriterText(text: AnnotatedString, revealedCount: Int): AnnotatedString = buildAnnotatedString {
    append(text)
    val visibleEnd = revealedCount.coerceIn(0, text.length)
    if (visibleEnd < text.length) {
        addStyle(SpanStyle(color = Color.Transparent), visibleEnd, text.length)
    }
}
