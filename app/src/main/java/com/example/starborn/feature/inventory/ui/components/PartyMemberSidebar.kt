package com.example.starborn.feature.inventory.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.R
import com.example.starborn.feature.inventory.PartyMemberStatus
import com.example.starborn.feature.inventory.ui.InventoryHealPulse
import com.example.starborn.ui.background.rememberAssetPainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PartyMemberSidebar(
    modifier: Modifier,
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    healPulses: Map<String, InventoryHealPulse>,
    onSelectCharacter: (String) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (partyMembers.isEmpty()) {
             Box(contentAlignment = Alignment.Center) {
                 Text("No Party", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
             }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(partyMembers, key = { it.id }) { member ->
                    PartyMemberItem(
                        member = member,
                        isSelected = member.id == selectedCharacterId,
                        healPulse = healPulses[member.id],
                        onClick = { onSelectCharacter(member.id) },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PartyMemberItem(
    member: PartyMemberStatus,
    isSelected: Boolean,
    healPulse: InventoryHealPulse?,
    onClick: () -> Unit,
    accentColor: Color
) {
    val background = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    val border = if (isSelected) accentColor else Color.Transparent
    val portraitPainter = rememberAssetPainter(member.portraitPath, painterResource(R.drawable.main_menu_background))
    val hpRatio = if (member.maxHp <= 0) 0f else member.hp.coerceIn(0, member.maxHp).toFloat() / member.maxHp.toFloat()
    val displayedHpRatio = remember { Animatable(hpRatio) }
    LaunchedEffect(hpRatio) {
        displayedHpRatio.animateTo(
            hpRatio,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = portraitPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${member.hp}/${member.maxHp}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                    LinearProgressIndicator(
                        progress = { displayedHpRatio.value.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = Color(0xFF66E38A),
                        trackColor = Color.Black.copy(alpha = 0.42f)
                    )
                }
            }
            healPulse?.let { pulse ->
                InventoryHealPulseText(
                    pulse = pulse,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InventoryHealPulseText(
    pulse: InventoryHealPulse,
    modifier: Modifier = Modifier
) {
    val y = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val scale = remember { Animatable(1.18f) }
    LaunchedEffect(pulse.id) {
        y.snapTo(0f)
        alphaAnim.snapTo(1f)
        scale.snapTo(1.18f)
        launch {
            y.animateTo(-28f, tween(durationMillis = 720, easing = LinearEasing))
        }
        launch {
            delay(420)
            alphaAnim.animateTo(0f, tween(durationMillis = 320, easing = LinearEasing))
        }
        launch {
            scale.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        }
    }
    Text(
        text = "+${pulse.amount}",
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 16.sp
        ),
        color = Color(0xFF9CFFB6),
        modifier = modifier.graphicsLayer {
            translationY = y.value
            alpha = alphaAnim.value
            scaleX = scale.value
            scaleY = scale.value
        }
    )
}
