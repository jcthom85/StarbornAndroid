@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@Composable
internal fun DesktopConsoleModal(
    eyebrow: String,
    title: String,
    onDismiss: () -> Unit,
    accent: Color = FieldMenuDesign.cyan,
    maxWidth: Dp = 760.dp,
    maxHeightFraction: Float = 0.88f,
    testTag: String? = null,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val highContrast = LocalExplorationSettings.current.highContrastMode
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .background(Color.Black.copy(alpha = 0.76f))
                .pointerInput(onDismiss) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                                onDismiss()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                }
                .focusable()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val effectiveMaxHeight = if (maxHeight > 0.dp) (maxHeight * maxHeightFraction).coerceAtLeast(280.dp) else 680.dp
            Surface(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = effectiveMaxHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(16.dp),
                color = if (highContrast) Color.Black else FieldMenuDesign.shell,
                contentColor = FieldMenuDesign.text,
                shadowElevation = 20.dp,
                tonalElevation = 8.dp,
                border = BorderStroke(1.25.dp, if (highContrast) Color.White else accent.copy(alpha = 0.52f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = eyebrow.uppercase(),
                                color = if (highContrast) Color.White else accent,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )
                            Text(
                                text = title,
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        headerTrailing?.invoke(this)
                        IconButton(onClick = onDismiss, modifier = Modifier.desktopPointerHover()) {
                            Icon(Icons.Default.Close, contentDescription = "Close modal", tint = FieldMenuDesign.textMuted)
                        }
                    }
                    HorizontalDivider(color = accent.copy(alpha = 0.28f))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content
                    )
                    if (footer != null) {
                        HorizontalDivider(color = accent.copy(alpha = 0.18f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            content = footer
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopConsoleLazyList(
    modifier: Modifier = Modifier,
    accent: Color = FieldMenuDesign.cyan,
    maxHeight: Dp = 460.dp,
    content: LazyListScope.() -> Unit
) {
    val listState = rememberLazyListState()
    Box(modifier = modifier.fillMaxWidth().heightIn(max = maxHeight)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
        Box(modifier = Modifier.matchParentSize()) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp),
                style = ScrollbarStyle(
                    minimalHeight = 16.dp,
                    thickness = 5.dp,
                    shape = RoundedCornerShape(3.dp),
                    hoverDurationMillis = 150,
                    unhoverColor = accent.copy(alpha = 0.28f),
                    hoverColor = accent.copy(alpha = 0.75f)
                )
            )
        }
    }
}
