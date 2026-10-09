package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.combat.ActiveBuff
import com.example.starborn.domain.combat.StatusEffect

internal data class StatusEffectInfo(
    val title: String,
    val summary: String,
    val isDebuff: Boolean,
    val accent: Color,
    val icon: ImageVector
)

internal fun describeStatus(id: String, services: DesktopAppServices): StatusEffectInfo {
    val key = id.lowercase()
    val definition = services.statusRegistry.definition(id)
    val baseName = definition?.displayName ?: definition?.name ?: services.contentName(id)

    return when {
        key in setOf("shock", "shocked") -> StatusEffectInfo(
            title = "SHOCK",
            summary = "Electrical overload disrupting circuits. Takes 25% increased damage and stability damage from electric attacks.",
            isDebuff = true,
            accent = Color(0xFFFFD27F),
            icon = Icons.Rounded.Bolt
        )
        key in setOf("stun", "stunned", "paralyze", "paralyzed") -> StatusEffectInfo(
            title = "STUNNED",
            summary = "Action systems locked. Unable to act or fill readiness meter until recovery.",
            isDebuff = true,
            accent = Color(0xFFD3A1FF),
            icon = Icons.Rounded.HourglassTop
        )
        key in setOf("burn", "burning", "fire", "ignited") -> StatusEffectInfo(
            title = "BURNING",
            summary = "Thermal scorch inflicting ongoing fire damage at the start of each turn.",
            isDebuff = true,
            accent = Color(0xFFFF6D55),
            icon = Icons.Rounded.LocalFireDepartment
        )
        key in setOf("erosion", "acid", "corrosion", "corroded") -> StatusEffectInfo(
            title = "CORROSION",
            summary = "Acidic wash melting hull plating. Incoming physical and direct damage increased.",
            isDebuff = true,
            accent = Color(0xFF6AE6A0),
            icon = Icons.Rounded.Science
        )
        key in setOf("regen", "regeneration", "recharge") -> StatusEffectInfo(
            title = "REGENERATION",
            summary = "Auxiliary medical nanites or life support active. Restores HP at the start of each turn.",
            isDebuff = false,
            accent = Color(0xFF75E8B0),
            icon = Icons.Rounded.Healing
        )
        key in setOf("guard", "shield", "shielded", "barrier", "invulnerable") -> StatusEffectInfo(
            title = "BARRIER",
            summary = "Active defense field absorbing incoming kinetic and energy damage.",
            isDebuff = false,
            accent = Color(0xFF63E6FF),
            icon = Icons.Rounded.Shield
        )
        key in setOf("target_lock", "locked_on") -> StatusEffectInfo(
            title = "TARGET LOCK",
            summary = "Tactical sensors locked onto vulnerabilities. Direct attacks against this target have guaranteed critical hit rate.",
            isDebuff = true,
            accent = Color(0xFFFF5252),
            icon = Icons.Rounded.GpsFixed
        )
        key in setOf("jammed", "jamming") -> StatusEffectInfo(
            title = "JAMMED",
            summary = "Communications and fire control scrambled. Special abilities and skills cannot be used.",
            isDebuff = true,
            accent = Color(0xFFFFB74D),
            icon = Icons.Rounded.PortableWifiOff
        )
        key in setOf("radiators_exposed") -> StatusEffectInfo(
            title = "RADIATORS EXPOSED",
            summary = "Cooling manifolds breached. Vulnerable to amplified damage from all elements.",
            isDebuff = true,
            accent = Color(0xFFFF8A65),
            icon = Icons.Rounded.Warning
        )
        key in setOf("stability_broken", "broken") -> StatusEffectInfo(
            title = "GUARD BROKEN",
            summary = "Stability depleted. Takes 25% bonus damage from direct hits and cooldowns are extended.",
            isDebuff = true,
            accent = Color(0xFFFFC107),
            icon = Icons.Rounded.HeartBroken
        )
        else -> {
            val isDebuff = definition?.skipReason != null ||
                (definition?.incomingMultiplier ?: 1.0) > 1.0 ||
                definition?.blockSkills == true ||
                (definition?.tick?.mode in setOf("burn", "damage", "erosion", "bleed"))

            val summaryText = buildString {
                definition?.skipReason?.let { append("Action skipped: $it. ") }
                definition?.incomingMultiplier?.let { if (it > 1.0) append("+${((it - 1.0) * 100).toInt()}% incoming damage. ") else if (it < 1.0) append("-${((1.0 - it) * 100).toInt()}% incoming damage. ") }
                definition?.outgoingMultiplier?.let { if (it > 1.0) append("+${((it - 1.0) * 100).toInt()}% outgoing damage. ") else if (it < 1.0) append("-${((1.0 - it) * 100).toInt()}% outgoing damage. ") }
                if (definition?.blockSkills == true) append("Abilities blocked. ")
                definition?.tick?.let { append("Inflicts ${it.mode} damage per turn. ") }
                if (isEmpty()) append("Active combat status modifying battle efficiency.")
            }

            StatusEffectInfo(
                title = baseName.uppercase(),
                summary = summaryText,
                isDebuff = isDebuff,
                accent = if (isDebuff) Color(0xFFD3A1FF) else Color(0xFF80E7A0),
                icon = if (isDebuff) Icons.Rounded.Warning else Icons.Rounded.AutoAwesome
            )
        }
    }
}

internal data class BuffInfo(
    val title: String,
    val summary: String,
    val isDebuff: Boolean,
    val accent: Color
)

internal fun describeBuff(buff: ActiveBuff, services: DesktopAppServices): BuffInfo {
    val statName = services.contentName(buff.effect.stat).replace('_', ' ').uppercase()
    val isPositive = buff.effect.value >= 0
    val formattedValue = if (isPositive) "+${buff.effect.value}" else "${buff.effect.value}"
    val sign = if (isPositive) "boosted" else "reduced"
    return BuffInfo(
        title = "$statName $formattedValue",
        summary = "Combat $statName is $sign by $formattedValue for the duration of this effect.",
        isDebuff = !isPositive,
        accent = if (isPositive) Color(0xFF80E7A0) else Color(0xFFFF8A80)
    )
}

/** Rich desktop status badge with icon, name, turns indicator, and hover tooltip. */
@Composable
internal fun DesktopCombatStatusChip(
    status: StatusEffect,
    services: DesktopAppServices,
    modifier: Modifier = Modifier
) {
    val info = describeStatus(status.id, services)
    val tooltipText = "${info.title} [${if (info.isDebuff) "DEBUFF" else "BUFF"}]\n" +
        "Duration: ${status.remainingTurns} ${if (status.remainingTurns == 1) "turn" else "turns"}" +
        (if (status.stacks > 1) " • ${status.stacks} stacks" else "") + "\n\n" +
        info.summary

    DesktopTooltip(text = tooltipText, accent = info.accent) {
        Surface(
            modifier = modifier.desktopPointerHover(),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xF008131C),
            border = BorderStroke(1.dp, info.accent.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = info.accent,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${info.title.lowercase().replaceFirstChar { it.uppercase() }} ${status.remainingTurns}t",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalStarbornFonts.current.orbitron,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Rich desktop buff badge with stat modifier and hover tooltip. */
@Composable
internal fun DesktopCombatBuffChip(
    buff: ActiveBuff,
    services: DesktopAppServices,
    modifier: Modifier = Modifier
) {
    val info = describeBuff(buff, services)
    val tooltipText = "${info.title}\nDuration: ${buff.remainingTurns} ${if (buff.remainingTurns == 1) "turn" else "turns"}\n\n${info.summary}"

    DesktopTooltip(text = tooltipText, accent = info.accent) {
        Surface(
            modifier = modifier.desktopPointerHover(),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xF008131C),
            border = BorderStroke(1.dp, info.accent.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = if (info.isDebuff) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = info.accent,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${info.title} (${buff.remainingTurns}t)",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalStarbornFonts.current.orbitron,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
