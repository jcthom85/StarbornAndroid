package com.example.starborn.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.SummaryType
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.flow.collect

@Composable
fun QuestSummaryOverlay(
    uiEventBus: UiEventBus,
    isSceneBlocking: Boolean,
    modifier: Modifier = Modifier,
    onShowDetails: (String) -> Unit
) {
    var summary by remember { mutableStateOf<UiEvent.ShowQuestSummary?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(uiEventBus) {
        uiEventBus.events.collect { event ->
            if (event is UiEvent.ShowQuestSummary) {
                summary = event
                visible = true
            }
        }
    }

    AnimatedVisibility(
        visible = visible && !isSceneBlocking && summary != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        summary?.let { data ->
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Quest Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    data.entries.forEach { entry ->
                        val prefix = when (entry.type) {
                            SummaryType.ACCEPTED -> "Accepted:"
                            SummaryType.UPDATED -> "Updated:"
                            SummaryType.COMPLETED -> "Completed:"
                            SummaryType.FAILED -> "Failed:"
                        }
                        val line = buildString {
                            append(prefix)
                            append(' ')
                            append(entry.questTitle)
                            entry.objectiveTitle?.takeIf { it.isNotBlank() }?.let {
                                append(" — ")
                                append(it)
                            }
                        }
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = {
                                visible = false
                                onShowDetails(entry.questId)
                                summary = null
                            }
                        ) {
                            Text("Details")
                        }
                    }
                    Button(
                        onClick = {
                            visible = false
                            summary = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
