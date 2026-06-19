package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.exploration.viewmodel.EventAnnouncementUi
import com.example.starborn.ui.theme.themeColor
import java.util.Locale

@Composable
fun EventAnnouncementOverlay(
    announcement: EventAnnouncementUi,
    theme: Theme?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventAccent = Color(announcement.accentColor)
    val accentColor = themeColor(theme?.accent, eventAccent)
    val outlineColor = themeColor(theme?.border, eventAccent.copy(alpha = 0.55f))
    val backgroundColor = themeColor(theme?.bg, Color(0xFF060B14)).copy(alpha = 0.96f)
    val hasTitle = !announcement.title.isNullOrBlank()
    val hasEyebrow = !announcement.eyebrow.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .semantics { contentDescription = "Event Announcement. Tap to continue" }
                .clickable(onClick = onDismiss),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.44f),
                        outlineColor.copy(alpha = 0.18f),
                        accentColor.copy(alpha = 0.28f)
                    )
                )
            ),
            color = backgroundColor,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            radius = 460f
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.0f),
                                    accentColor.copy(alpha = 0.65f),
                                    accentColor.copy(alpha = 0.0f)
                                )
                            )
                        )
                )
                if (hasEyebrow) {
                    Text(
                        text = announcement.eyebrow!!.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = accentColor.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                }
                if (hasTitle) {
                    Text(
                        text = announcement.title!!,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            shadow = Shadow(color = accentColor.copy(alpha = 0.35f), blurRadius = 12f)
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (announcement.items.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        announcement.items.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accentColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.86f)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to continue",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = accentColor.copy(alpha = 0.55f)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.0f),
                                    accentColor.copy(alpha = 0.55f),
                                    accentColor.copy(alpha = 0.0f)
                                )
                            )
                        )
                )
            }
        }
    }
}
