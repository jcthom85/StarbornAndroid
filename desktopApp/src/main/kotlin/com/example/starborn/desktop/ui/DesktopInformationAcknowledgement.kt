package com.example.starborn.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

/** Only for acknowledgement cards; choices and confirmation dialogs must not use this. */
@Composable
internal fun Modifier.informationAcknowledgement(onAcknowledge: () -> Unit): Modifier {
    val focus = remember { FocusRequester() }
    val acknowledge by rememberUpdatedState(onAcknowledge)
    LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
    return this.focusRequester(focus).onPreviewKeyEvent { event ->
        if (event.key in listOf(Key.Enter, Key.Spacebar, Key.Escape)) {
            // Consume both edges; one physical key press acknowledges exactly one card.
            if (event.type == KeyEventType.KeyUp) acknowledge()
            true
        } else false
    }.focusable().desktopPointerHover().clickable { acknowledge() }
}
