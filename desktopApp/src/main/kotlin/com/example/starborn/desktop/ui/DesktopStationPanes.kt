package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Keep both selection and details visible without sideways scrolling. */
@Composable
internal fun DesktopStationPanes(
    modifier: Modifier = Modifier,
    stackBelow: androidx.compose.ui.unit.Dp = 700.dp,
    primary: @Composable (Modifier) -> Unit,
    detail: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < stackBelow) {
            Column(Modifier.fillMaxSize().testTag("station-stacked-panes"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                primary(Modifier.weight(.32f).fillMaxWidth())
                detail(Modifier.weight(.68f).fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxSize().testTag("station-side-by-side-panes"), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                primary(Modifier.weight(1f).fillMaxHeight())
                detail(Modifier.weight(1.2f).fillMaxHeight())
            }
        }
    }
}
