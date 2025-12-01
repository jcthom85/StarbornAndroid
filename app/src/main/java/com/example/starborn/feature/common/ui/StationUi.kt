package com.example.starborn.feature.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.R

@Composable
fun StationBackground(
    highContrastMode: Boolean,
    @DrawableRes backgroundRes: Int = R.drawable.starborn_menu_bg,
    @DrawableRes vignetteRes: Int? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseColor = if (highContrastMode) Color(0xFF0A1018) else Color.Black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        if (!highContrastMode) {
            Image(
                painter = painterResource(backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 1.0f
            )
            vignetteRes?.let { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.2f
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun StationHeader(
    title: String,
    @DrawableRes iconRes: Int,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier,
    actionLabel: String = "Back"
) {
    val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.heightIn(max = 56.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = titleColor
                )
            }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = if (largeTouchTargets) 52.dp else Dp.Unspecified),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = titleColor
            )
        ) {
            Text(actionLabel)
        }
    }
}
