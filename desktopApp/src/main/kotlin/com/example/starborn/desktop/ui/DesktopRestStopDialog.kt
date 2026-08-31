package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@Composable
fun DesktopRestStopDialog(
    services: DesktopAppServices,
    onDismiss: () -> Unit
) {
    var hasRested by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(620.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(FieldMenuDesign.shellRadius),
            color = FieldMenuDesign.shell,
            border = BorderStroke(1.5.dp, FieldMenuDesign.cyan.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EXPEDITION REST POD & RECOVERY",
                    color = FieldMenuDesign.gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (hasRested) {
                        "✓ Stasis bio-regenerators cycled. Nova's Health, Shield matrices, and Energy reserves are fully restored."
                    } else {
                        "Connect to the local habitat grid to replenish expedition vitals, recharge power cells, and synthesize vital nanites."
                    },
                    color = FieldMenuDesign.text,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            hasRested = true
                        },
                        enabled = !hasRested,
                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.cyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = if (hasRested) "VITALS RESTORED" else "REST & REGENERATE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.elevatedPanel),
                        border = BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text(text = "STANDBY", color = FieldMenuDesign.text, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
