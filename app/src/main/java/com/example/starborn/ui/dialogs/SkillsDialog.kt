package com.example.starborn.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.combat.CombatClarity
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.feature.combat.presentation.abilityCompactPowerLabel
import com.example.starborn.feature.combat.presentation.abilityPowerLabel
import com.example.starborn.feature.combat.presentation.isHealingSkill
import com.example.starborn.feature.combat.presentation.isPureSupportSkill
import com.example.starborn.feature.combat.presentation.weaknessLabel
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.ui.components.CommandPaletteMarkers
import com.example.starborn.feature.combat.ui.components.weaknessTintColor
import com.example.starborn.feature.combat.ui.titleCaseName
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.components.maestroTag
import com.example.starborn.ui.theme.themeColor
import java.util.Locale

private val ElementTags = listOf("shock", "burn", "freeze", "acid", "source", "physical")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsDialog(
    player: Player,
    skills: List<Skill>,
    viewModel: CombatViewModel,
    onDismiss: () -> Unit,
    onSkillSelected: (Skill) -> Unit,
    allowedSkillIds: Set<String>? = null,
    highlightedSkillId: String? = null,
    theme: Theme? = null,
    highContrastMode: Boolean = false
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    val momentum = viewModel.momentumFor(player.id)
    val (infusedElement, infusedStatus) = viewModel.weaponInfusionFor(player.id)
    var detailSkill by remember { mutableStateOf<Skill?>(null) }

    CombatDialogFrame(
        onDismiss = onDismiss,
        accent = accent,
        border = border,
        panel = panel,
        highContrastMode = highContrastMode
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val portraitPainter = rememberAssetPainter(
                player.miniIconPath,
                painterResource(R.drawable.main_menu_background)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF151C28),
                border = BorderStroke(1.dp, border.copy(alpha = 0.45f)),
                modifier = Modifier.size(44.dp)
            ) {
                Image(
                    painter = portraitPainter,
                    contentDescription = titleCaseName(player.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "${titleCaseName(player.name).uppercase(Locale.getDefault())} · READY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = CombatNameFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    ),
                    color = accent.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Abilities",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = CombatNameFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MomentumGaugePill(
                momentum = momentum,
                borderColor = border
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.82f),
                            border.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        OverchargeStrip(
            momentum = momentum,
            border = border
        )

        if (infusedElement != null || infusedStatus != null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Weapon Synergy:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF9FB4C4),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                infusedElement?.let { elem ->
                    AbilitySpecChip(
                        text = weaknessLabel(elem),
                        tint = weaknessTintColor(elem)
                    )
                }
                infusedStatus?.let { status ->
                    AbilitySpecChip(
                        text = CombatClarity.statusName(status),
                        tint = accent
                    )
                }
                Text(
                    text = "· Attack abilities",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8399A8),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        if (skills.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, border.copy(alpha = 0.24f))
            ) {
                Text(
                    text = "No abilities available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(skills, key = { it.id }) { skill ->
                    val allowedByGuide = allowedSkillIds == null || skill.id in allowedSkillIds
                    val unavailableReason = if (!allowedByGuide) {
                        "Follow the tutorial's highlighted ability"
                    } else {
                        viewModel.skillUnavailableReason(player.id, skill)
                    }
                    val canUse = unavailableReason == null
                    val cooldown = viewModel.skillCooldownRemaining(player.id, skill.id)
                    val highlighted = skill.id == highlightedSkillId
                    AbilityRow(
                        skill = skill,
                        cooldownRemaining = cooldown,
                        canUse = canUse,
                        unavailableReason = unavailableReason,
                        highlighted = highlighted,
                        momentum = momentum,
                        infusedElement = infusedElement,
                        infusedStatus = infusedStatus,
                        accent = accent,
                        border = border,
                        highContrastMode = highContrastMode,
                        onDetails = { detailSkill = skill },
                        onUse = { onSkillSelected(skill) },
                        targetScopeLabel = viewModel.skillTargetScopeLabel(skill)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (skills.isEmpty()) "" else "${skills.size} ${if (skills.size == 1) "ability" else "abilities"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF93A7B6)
            )
            DialogTacticalButton(
                label = "Close",
                icon = Icons.Filled.Close,
                accent = accent,
                border = border,
                onClick = onDismiss,
                modifier = Modifier.maestroTag("combat-abilities-close")
            )
        }
    }

    detailSkill?.let { skill ->
        AbilityDetailsDialog(
            skill = skill,
            targetRequirement = viewModel.targetRequirementFor(skill),
            cooldownRemaining = viewModel.skillCooldownRemaining(player.id, skill.id),
            momentum = momentum,
            infusedElement = infusedElement,
            infusedStatus = infusedStatus,
            accent = accent,
            border = border,
            panel = panel,
            highContrastMode = highContrastMode,
            onDismiss = { detailSkill = null }
        )
    }
}

@Composable
private fun CombatDialogFrame(
    onDismiss: () -> Unit,
    accent: Color,
    border: Color,
    panel: Color,
    highContrastMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.95f),
                border = BorderStroke(
                    width = 1.25.dp,
                    color = border.copy(alpha = if (highContrastMode) 0.85f else 0.65f)
                ),
                shadowElevation = 18.dp,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                            )
                        )
                        .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
            CommandPaletteMarkers(
                color = accent,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun DialogTacticalButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    border: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E2534),
        border = BorderStroke(1.dp, border.copy(alpha = 0.58f)),
        modifier = modifier
            .heightIn(min = 42.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun MomentumGaugePill(
    momentum: Int,
    borderColor: Color
) {
    val clamped = momentum.coerceIn(0, 3)
    val gaugeTint = when {
        clamped >= 3 -> Color(0xFFFFD700)
        clamped > 0 -> Color(0xFF00E5FF)
        else -> borderColor
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF101824),
        border = BorderStroke(
            1.dp,
            if (clamped > 0) gaugeTint.copy(alpha = 0.72f) else borderColor.copy(alpha = 0.34f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (clamped >= 3) "⚡ MAX" else "⚡ $clamped/3",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (clamped > 0) gaugeTint else Color(0xFFA7BCCB)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (index < clamped) gaugeTint else Color.White.copy(alpha = 0.14f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun OverchargeStrip(
    momentum: Int,
    border: Color
) {
    if (momentum > 0) {
        val (bannerBg, overchargeText) = when (momentum) {
            1 -> Color(0xFF00E5FF).copy(alpha = 0.18f) to "⚡ OVERCHARGE: +25% Power / Healing"
            2 -> Color(0xFF00E5FF).copy(alpha = 0.22f) to "⚡ OVERCHARGE: +50% Power + 1-Turn CD Refund"
            3 -> Color(0xFFFFD700).copy(alpha = 0.22f) to "⚡ MAX OVERCHARGE: +80% Power + 1-Turn CD Refund + Guaranteed CRIT!"
            else -> Color.Transparent to ""
        }
        val stripBorder = if (momentum >= 3) Color(0xFFFFD700) else Color(0xFF00E5FF)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = bannerBg,
            border = BorderStroke(1.dp, stripBorder.copy(alpha = 0.72f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = overchargeText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF101924),
            border = BorderStroke(1.dp, border.copy(alpha = 0.24f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚡ Basic attacks build Momentum to Overcharge abilities (up to 3 stacks)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB5C7D4),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AbilityRow(
    skill: Skill,
    cooldownRemaining: Int,
    canUse: Boolean,
    unavailableReason: String?,
    highlighted: Boolean,
    momentum: Int = 0,
    infusedElement: String? = null,
    infusedStatus: String? = null,
    accent: Color,
    border: Color,
    highContrastMode: Boolean,
    onDetails: () -> Unit,
    onUse: () -> Unit,
    targetScopeLabel: String? = null
) {
    val visuals = remember(skill) { abilityCardVisuals(skill) }
    val stackActions = LocalDensity.current.fontScale >= 1.5f
    val scalesWithOvercharge = momentum > 0 && (skill.basePower > 0 || skill.cooldown > 0)
    val overchargeHighlight = if (momentum >= 3) Color(0xFFFFD700) else Color(0xFF00E5FF)
    val cardColor = when {
        highlighted -> accent.copy(alpha = 0.16f)
        canUse -> Color(0xFF0D1622).copy(alpha = if (highContrastMode) 0.96f else 0.90f)
        else -> Color(0xFF0A1018).copy(alpha = 0.84f)
    }
    val cardBorder = when {
        highlighted -> accent
        !canUse -> Color.White.copy(alpha = 0.14f)
        scalesWithOvercharge -> overchargeHighlight.copy(alpha = 0.56f)
        else -> visuals.tint.copy(alpha = 0.38f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (canUse) visuals.tint else visuals.tint.copy(alpha = 0.42f)
                        )
                )
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = CombatNameFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (canUse) Color.White else Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.weight(1f)
                )
                val badgeLabel = when {
                    visuals.isSourceArt -> "SOURCE ART"
                    else -> visuals.primaryLabel.uppercase(Locale.getDefault())
                }
                AbilitySpecChip(
                    text = badgeLabel,
                    tint = visuals.tint,
                    muted = !canUse
                )
            }

            val powerChip = remember(skill, momentum) {
                abilityCompactPowerLabel(skill, momentum)
            }
            val scopeText = targetScopeLabel ?: inferTargetScopeFallback(skill, visuals)
            val statusPreviews = remember(skill, visuals) {
                skill.statusApplications.orEmpty()
                    .mapNotNull { raw ->
                        raw.takeIf { it.isNotBlank() }?.let(CombatClarity::statusName)
                    }
                    .distinct()
                    .filterNot { it.equals(visuals.primaryLabel, ignoreCase = true) }
                    .take(2)
            }
            val metaParts = buildList {
                add(abilityCooldownLabel(skill.cooldown, cooldownRemaining, momentum))
                skill.scaling?.takeIf { it.isNotBlank() }?.let { add("${it.toDisplayLabel()} scaling") }
                skill.usesPerBattle?.let { uses -> add("$uses/battle") }
            }

            if (stackActions) {
                AbilityDetailsBlock(
                    skill = skill,
                    visuals = visuals,
                    powerChip = powerChip,
                    scopeText = scopeText,
                    statusPreviews = statusPreviews,
                    metaParts = metaParts,
                    cooldownRemaining = cooldownRemaining,
                    unavailableReason = unavailableReason,
                    canUse = canUse,
                    momentum = momentum
                )
                AbilityRowActions(
                    skillName = skill.name,
                    cooldownRemaining = cooldownRemaining,
                    canUse = canUse,
                    overcharged = canUse && scalesWithOvercharge,
                    maxOvercharged = canUse && scalesWithOvercharge && momentum >= 3,
                    elementTint = visuals.tint,
                    accent = accent,
                    border = border,
                    stackedFooter = true,
                    onDetails = onDetails,
                    onUse = onUse
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AbilityDetailsBlock(
                        skill = skill,
                        visuals = visuals,
                        powerChip = powerChip,
                        scopeText = scopeText,
                        statusPreviews = statusPreviews,
                        metaParts = metaParts,
                        cooldownRemaining = cooldownRemaining,
                        unavailableReason = unavailableReason,
                        canUse = canUse,
                        momentum = momentum,
                        modifier = Modifier.weight(1f)
                    )
                    AbilityRowActions(
                        skillName = skill.name,
                        cooldownRemaining = cooldownRemaining,
                        canUse = canUse,
                        overcharged = canUse && scalesWithOvercharge,
                        maxOvercharged = canUse && scalesWithOvercharge && momentum >= 3,
                        elementTint = visuals.tint,
                        accent = accent,
                        border = border,
                        stackedFooter = false,
                        onDetails = onDetails,
                        onUse = onUse
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityDetailsBlock(
    skill: Skill,
    visuals: AbilityCardVisuals,
    powerChip: String?,
    scopeText: String,
    statusPreviews: List<String>,
    metaParts: List<String>,
    cooldownRemaining: Int,
    unavailableReason: String?,
    canUse: Boolean,
    momentum: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (skill.description.isNotBlank()) {
            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (canUse) Color(0xFFC7D5E0) else Color(0xFF8393A0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (skill.id == "nova_link") {
                AbilitySpecChip(
                    text = "Heals party + Regen",
                    tint = Color(0xFF81C784),
                    muted = !canUse
                )
            }
            if (powerChip != null) {
                val chipTint = when {
                    !canUse -> Color(0xFF8A9BA8)
                    momentum >= 3 && skill.basePower > 0 && !visuals.isNonHealSupport -> Color(0xFFFFD700)
                    momentum > 0 && skill.basePower > 0 && !visuals.isNonHealSupport -> Color(0xFF00E5FF)
                    visuals.isHeal -> Color(0xFF81C784)
                    else -> visuals.tint
                }
                AbilitySpecChip(text = powerChip, tint = chipTint, muted = !canUse)
            }
            AbilitySpecChip(
                text = scopeText,
                tint = Color(0xFF9CC2DA),
                muted = !canUse
            )
            if (visuals.breaksGuard) {
                AbilitySpecChip(
                    text = "Guard Break",
                    tint = Color(0xFFFFB74D),
                    muted = !canUse
                )
            }
            statusPreviews.forEach { statusName ->
                AbilitySpecChip(
                    text = statusName,
                    tint = Color(0xFFB39DDB),
                    muted = !canUse
                )
            }
        }
        Text(
            text = metaParts.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = if (cooldownRemaining > 0) Color(0xFFFFC857) else Color(0xFF9CB2C2)
        )
        unavailableReason?.let { reason ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFC857).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.42f))
            ) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFFD57E),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityRowActions(
    skillName: String,
    cooldownRemaining: Int,
    canUse: Boolean,
    overcharged: Boolean,
    maxOvercharged: Boolean,
    elementTint: Color,
    accent: Color,
    border: Color,
    stackedFooter: Boolean,
    onDetails: () -> Unit,
    onUse: () -> Unit
) {
    val useContainer = when {
        !canUse -> Color(0xFF141820)
        maxOvercharged -> Color(0xFF2B2310)
        overcharged -> Color(0xFF122838)
        else -> Color(0xFF1E2534)
    }
    val useContent = when {
        !canUse && cooldownRemaining > 0 -> Color(0xFFFFC857).copy(alpha = 0.88f)
        !canUse -> Color.White.copy(alpha = 0.42f)
        maxOvercharged -> Color(0xFFFFF3C4)
        else -> Color.White
    }
    val useBorderColor = when {
        !canUse -> Color.White.copy(alpha = 0.14f)
        maxOvercharged -> Color(0xFFFFD700)
        overcharged -> Color(0xFF00E5FF)
        else -> elementTint.copy(alpha = 0.78f)
    }

    @Composable
    fun DetailsAction(modifier: Modifier = Modifier) {
        TextButton(
            onClick = onDetails,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color(0xFF141D2A),
                contentColor = Color(0xFFD6E6F2)
            ),
            border = BorderStroke(1.dp, border.copy(alpha = 0.36f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            modifier = modifier
                .heightIn(min = if (stackedFooter) 48.dp else 40.dp)
                .semantics { contentDescription = "Details for $skillName" }
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Details",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    }

    @Composable
    fun UseAction(modifier: Modifier = Modifier) {
        Button(
            onClick = onUse,
            enabled = canUse,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(if (maxOvercharged) 1.5.dp else 1.1.dp, useBorderColor),
            colors = ButtonDefaults.buttonColors(
                containerColor = useContainer,
                contentColor = useContent,
                disabledContainerColor = useContainer,
                disabledContentColor = useContent
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = modifier
                .heightIn(min = 42.dp)
                .widthIn(min = 88.dp)
                .maestroTag("combat-skill-$skillName")
        ) {
            Text(
                text = if (cooldownRemaining > 0) cooldownRemaining.toString() else "Use",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }

    if (stackedFooter) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DetailsAction()
            UseAction()
        }
    } else {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            UseAction(modifier = Modifier.fillMaxWidth())
            DetailsAction(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityDetailsDialog(
    skill: Skill,
    targetRequirement: TargetRequirement,
    cooldownRemaining: Int,
    momentum: Int = 0,
    infusedElement: String? = null,
    infusedStatus: String? = null,
    accent: Color,
    border: Color,
    panel: Color,
    highContrastMode: Boolean,
    onDismiss: () -> Unit
) {
    val visuals = remember(skill) { abilityCardVisuals(skill) }
    CombatDialogFrame(
        onDismiss = onDismiss,
        accent = accent,
        border = border,
        panel = panel,
        highContrastMode = highContrastMode
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val eyebrow = if (visuals.isSourceArt) {
                "${visuals.primaryLabel.uppercase(Locale.getDefault())} · SOURCE ART"
            } else {
                visuals.primaryLabel.uppercase(Locale.getDefault())
            }
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                ),
                color = visuals.tint
            )
            Text(
                text = skill.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            visuals.tint.copy(alpha = 0.82f),
                            border.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 440.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = skill.description.ifBlank { "No description available." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            if (momentum > 0) {
                item {
                    val bonusText = when (momentum) {
                        1 -> "+25% Power / Healing"
                        2 -> "+50% Power / Healing & -1 Turn Cooldown Refund"
                        3 -> "+80% Power / Healing, -1 Turn Cooldown Refund & Guaranteed Critical Hit"
                        else -> ""
                    }
                    val overchargeBorder = if (momentum >= 3) Color(0xFFFFD700) else Color(0xFF00E5FF)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = overchargeBorder.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, overchargeBorder.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚡ Overcharge Active (Level $momentum)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = overchargeBorder
                            )
                            Text(
                                text = bonusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            if (!visuals.isHeal && (infusedElement != null || infusedStatus != null)) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Weapon Synergy Infusion",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                            Text(
                                text = buildString {
                                    if (infusedElement != null) append("Element: ${infusedElement.toDisplayLabel()}  ")
                                    if (infusedStatus != null) append("Status Proc: ${infusedStatus.toDisplayLabel()}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = if (highContrastMode) 0.10f else 0.06f),
                    border = BorderStroke(1.dp, border.copy(alpha = 0.34f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AbilityDetailLine("Type", skill.type.toDisplayLabel())
                        AbilityDetailLine("Power", abilityPowerLabel(skill, momentum))
                        AbilityDetailLine("Target selection", abilityTargetLabel(targetRequirement))
                        AbilityDetailLine(
                            "Effect strength",
                            "Results depend on your stats, bonuses, and the target's defenses."
                        )
                        AbilityDetailLine(
                            "Scaling",
                            skill.scaling?.toDisplayLabel() ?: "None"
                        )
                        AbilityDetailLine(
                            "Cooldown",
                            abilityCooldownLabel(skill.cooldown, cooldownRemaining, momentum)
                        )
                        skill.usesPerBattle?.let { uses ->
                            AbilityDetailLine("Battle limit", "$uses use${if (uses == 1) "" else "s"}")
                        }
                    }
                }
            }
            if (!skill.statusApplications.isNullOrEmpty()) {
                item {
                    AbilityDetailSection(
                        title = "Applies",
                        values = skill.statusApplications.orEmpty().map(CombatClarity::statusName),
                        tint = Color(0xFFB39DDB)
                    )
                }
            }
            if (!skill.combatTags.isNullOrEmpty()) {
                item {
                    AbilityDetailSection(
                        title = "Combat traits",
                        values = skill.combatTags.orEmpty().map { it.toDisplayLabel() },
                        tint = accent
                    )
                }
            }
            if (!skill.conditions.isNullOrEmpty()) {
                item {
                    AbilityDetailSection(
                        title = "Requirements",
                        values = skill.conditions.orEmpty().map { it.toDisplayLabel() },
                        tint = Color(0xFFFFC857)
                    )
                }
            }
            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = CombatClarity.SKILL_GUIDANCE,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9EB2C0)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            DialogTacticalButton(
                label = "Back to Abilities",
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                accent = accent,
                border = border,
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun AbilityDetailLine(label: String, value: String) {
    if (LocalDensity.current.fontScale >= 1.5f) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.62f))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.62f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityDetailSection(title: String, values: List<String>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = tint
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            values.forEach { label ->
                AbilitySpecChip(text = label, tint = tint)
            }
        }
    }
}

@Composable
private fun AbilitySpecChip(
    text: String,
    tint: Color,
    muted: Boolean = false
) {
    val effectiveTint = if (muted) tint.copy(alpha = 0.55f) else tint
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = effectiveTint.copy(alpha = if (muted) 0.08f else 0.14f),
        border = BorderStroke(1.dp, effectiveTint.copy(alpha = if (muted) 0.24f else 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (muted) Color.White.copy(alpha = 0.56f) else effectiveTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

private data class AbilityCardVisuals(
    val primaryLabel: String,
    val tint: Color,
    val isHeal: Boolean,
    val isSourceArt: Boolean,
    val isAoE: Boolean,
    val breaksGuard: Boolean,
    val isNonHealSupport: Boolean
)

private fun abilityCardVisuals(skill: Skill): AbilityCardVisuals {
    val tags = skill.combatTags.orEmpty().map { it.trim().lowercase(Locale.getDefault()) }
    val isSourceArt = skill.type.equals("source_art", ignoreCase = true)
    val isHeal = isHealingSkill(skill)
    val isNonHealSupport = isPureSupportSkill(skill)
    val isAoE = "aoe" in tags || "burst" in tags || "multi" in tags
    val breaksGuard = "guard_break" in tags
    val elementTag = ElementTags.firstOrNull { it in tags }
    val (label, tint) = when {
        elementTag != null -> weaknessLabel(elementTag) to weaknessTintColor(elementTag)
        isSourceArt -> "Source" to weaknessTintColor("source")
        isHeal -> "Heal" to Color(0xFF81C784)
        "debuff" in tags && skill.basePower <= 0 -> "Debuff" to Color(0xFFFFAB91)
        isNonHealSupport -> "Support" to Color(0xFF64B5F6)
        else -> "Physical" to weaknessTintColor("physical")
    }
    return AbilityCardVisuals(
        primaryLabel = label,
        tint = tint,
        isHeal = isHeal,
        isSourceArt = isSourceArt,
        isAoE = isAoE,
        breaksGuard = breaksGuard,
        isNonHealSupport = isNonHealSupport
    )
}

private fun inferTargetScopeFallback(skill: Skill, visuals: AbilityCardVisuals): String =
    when (skill.targeting?.trim()?.lowercase(Locale.getDefault())) {
        "all_enemies" -> "All enemies"
        "all_allies" -> "All allies"
        "self" -> "Self"
        "single_enemy" -> "One enemy"
        else -> when {
            visuals.isHeal && visuals.isAoE -> "All allies"
            visuals.isHeal || visuals.isNonHealSupport -> "Self"
            visuals.isAoE -> "All enemies"
            else -> "One enemy"
        }
    }

private fun String.toDisplayLabel(): String =
    trim()
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
