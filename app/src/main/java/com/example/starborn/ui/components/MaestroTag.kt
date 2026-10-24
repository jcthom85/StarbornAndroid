package com.example.starborn.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/** Stable automation IDs, including controls hosted in separate dialog windows. */
fun Modifier.maestroTag(id: String): Modifier =
    semantics { testTagsAsResourceId = true }.testTag(id)
