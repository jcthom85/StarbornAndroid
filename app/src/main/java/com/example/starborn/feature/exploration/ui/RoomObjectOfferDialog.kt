package com.example.starborn.feature.exploration.ui

import com.example.starborn.ui.components.maestroTag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.starborn.ui.theme.themeColor
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.exploration.presentation.RoomObjectOffer
import com.example.starborn.feature.exploration.viewmodel.NarrationPrompt

/** Activity choice using the same frame as room inspections; body taps never execute it. */
@Composable
fun RoomObjectOfferDialog(offer: RoomObjectOffer, theme: Theme?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val accent = themeColor(theme?.accent, Color(0xFF8DE2FF))
    Dialog(onDismissRequest = onDismiss) {
        NarrationCard(NarrationPrompt(offer.detail, tapToDismiss = false), theme, onDismiss,
            modifier = Modifier.padding(vertical = 24.dp), title = offer.title,
            actions = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Back") }
                    Button(onClick = onConfirm, modifier = Modifier.maestroTag("room-offer-confirm").heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF050B12))) {
                        Text(offer.confirmLabel)
                    }
                }
            })
    }
}
