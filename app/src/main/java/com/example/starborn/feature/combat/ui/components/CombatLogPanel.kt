package com.example.starborn.feature.combat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.combat.ui.CombatImpactBanner
import com.example.starborn.feature.combat.viewmodel.CombatBannerMessage
import java.util.Locale

@Composable
fun CombatLogPanel(
    bannerMessage: CombatBannerMessage?,
    instruction: String?,
    showCancel: Boolean,
    instructionShownAbove: Boolean = false,
    highContrastMode: Boolean,
    theme: Theme?,
    onCancel: (() -> Unit)?
) {
    val instructionSlotHeight = 30.dp
    val cancelSlotHeight = 28.dp
    val baseSpacing = 6.dp
    val baseHeight = instructionSlotHeight + cancelSlotHeight + baseSpacing
    val hasInstruction = instructionShownAbove || !instruction.isNullOrBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(baseHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(baseSpacing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(instructionSlotHeight)
            ) {
                if (!instructionShownAbove && !instruction.isNullOrBlank()) {
                    TargetInstructionBadge(
                        text = instruction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 12.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cancelSlotHeight)
            ) {
                if (showCancel && onCancel != null) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(text = "Cancel target")
                    }
                }
            }
        }
        CombatImpactBanner(
            message = bannerMessage,
            hasInstruction = hasInstruction,
            highContrastMode = highContrastMode,
            theme = theme,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-56).dp)
                .padding(horizontal = 8.dp)
        )
    }
}
