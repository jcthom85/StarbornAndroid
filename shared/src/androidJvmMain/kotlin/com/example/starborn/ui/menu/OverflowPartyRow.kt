package com.example.starborn.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fades only indicate actual overflow; selection stays wholly inside the viewport. */
@Composable
fun OverflowPartyRow(keys: List<String>, selectedKey: String?, itemWidth: Dp, background: Color,
    modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(selectedKey, keys) {
        val index = keys.indexOf(selectedKey)
        if (index >= 0) {
            val info = state.layoutInfo
            val selected = info.visibleItemsInfo.firstOrNull { it.index == index }
            if (selected == null || selected.offset < info.viewportStartOffset ||
                selected.offset + selected.size > info.viewportEndOffset) state.animateScrollToItem(index)
        }
    }
    Box(modifier.fillMaxWidth()) {
        LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("party-selector")) {
            items(keys.size, key = { keys[it] }) { index -> Box(Modifier.width(itemWidth)) { content(index) } }
        }
        if (state.canScrollBackward) Box(Modifier.matchParentSize()) { Box(Modifier.align(Alignment.CenterStart).width(20.dp).fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(background, Color.Transparent))).testTag("party-more-left")) }
        if (state.canScrollForward) Box(Modifier.matchParentSize()) { Box(Modifier.align(Alignment.CenterEnd).width(20.dp).fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Color.Transparent, background))).testTag("party-more-right")) }
    }
}
