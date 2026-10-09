package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun DesktopHubControl(label: String, icon: ImageVector, onClick: () -> Unit,
    modifier: Modifier=Modifier, enabled: Boolean=true, primary: Boolean=false,
    accent: Color=LocalExplorationAccent.current) {
    val contrast=LocalExplorationSettings.current.highContrastMode
    val color=if(contrast) Color.White else accent
    OutlinedButton(onClick=onClick, enabled=enabled,
        modifier=modifier.heightIn(min=if(LocalExplorationSettings.current.largeTouchTargets) 56.dp else 44.dp).explorationFeedback(enabled,color),
        shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,color.copy(alpha=if(enabled) if(primary) .8f else .4f else .15f)),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=if(contrast) Color.Black else if(primary) Color(0xFF10222C) else Color(0xFF061018),
            contentColor=Color.White,disabledContainerColor=Color(0xFF061018),disabledContentColor=Color(0xFF60717B)),
        contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp)) {
        Icon(icon,null,Modifier.size(18.dp),tint=if(enabled) color else Color(0xFF60717B))
        Spacer(Modifier.width(8.dp));Text(label)
    }
}
internal fun hubCompactReserve(measured: Float, viewportHeight: Float)=
    (measured+16f).coerceAtMost(minOf(viewportHeight*.28f,180f)).coerceAtLeast(0f)
