package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.*

private const val NODE_WIDTH_DP = 158
private const val NODE_HEIGHT_DP = 106
private const val COL_STEP_DP = 180
private const val ROW_STEP_DP = 140

@Composable
internal fun DesktopMenuSkillsContent(services: DesktopAppServices, tree: SkillTreeOverlayUi) {
    var branchId by rememberSaveable(tree.characterId) { mutableStateOf(tree.branches.firstOrNull()?.id) }
    val branch = tree.branches.firstOrNull { it.id == branchId } ?: tree.branches.firstOrNull()
    var selectedId by rememberSaveable(tree.characterId, branchId) { mutableStateOf<String?>(null) }
    val selected = branch?.nodes?.firstOrNull { it.id == selectedId } ?: branch?.nodes?.firstOrNull()
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    LaunchedEffect(branchId) {
        vertical.scrollTo(0)
        horizontal.scrollTo(0)
    }

    LaunchedEffect(selectedId) {
        if (selectedId == null) return@LaunchedEffect
        val node = selected ?: return@LaunchedEffect
        val minRow = branch?.nodes?.minOfOrNull { it.row } ?: 0
        val minCol = branch?.nodes?.minOfOrNull { it.column } ?: 0
        val top = ((node.row - minRow) * ROW_STEP_DP + 20) * density
        val left = ((node.column - minCol) * COL_STEP_DP + 20) * density
        if (top < vertical.value) {
            vertical.animateScrollTo(top.toInt())
        } else if (top + NODE_HEIGHT_DP * density > vertical.value + vertical.viewportSize) {
            vertical.animateScrollTo((top + NODE_HEIGHT_DP * density - vertical.viewportSize).toInt())
        }
        if (left < horizontal.value) {
            horizontal.animateScrollTo(left.toInt())
        } else if (left + NODE_WIDTH_DP * density > horizontal.value + horizontal.viewportSize) {
            horizontal.animateScrollTo((left + NODE_WIDTH_DP * density - horizontal.viewportSize).toInt())
        }
    }

    val names = tree.branches.flatMap { it.nodes }.associate { it.id to it.name }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // --- Header Navigation & AP Status ---
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tree.branches.forEach { entry ->
                    val isCurrent = entry.id == branch?.id
                    val unlockedCount = entry.nodes.count { it.status.unlocked }
                    val totalCount = entry.nodes.size
                    FilterChip(
                        selected = isCurrent,
                        onClick = { branchId = entry.id },
                        modifier = Modifier.desktopPointerHover(),
                        leadingIcon = {
                            if (unlockedCount == totalCount && totalCount > 0) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = FieldMenuDesign.gold)
                            }
                        },
                        label = {
                            Text("${entry.title} ($unlockedCount/$totalCount)")
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FieldMenuDesign.gold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.45f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, null, Modifier.size(16.dp), tint = FieldMenuDesign.gold)
                        Text(
                            "${tree.availableAp} AP Available",
                            color = FieldMenuDesign.gold,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Text(
                    "${tree.apInvested} AP Invested",
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // --- Main Workspace: Graph Canvas + Inspector ---
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val compact = maxWidth < 680.dp
            val inspectorWidth = if (maxWidth < 950.dp) 260.dp else 320.dp

            val graph: @Composable (Modifier) -> Unit = { modifier ->
                DesktopMenuCard(
                    modifier.onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                            if (selected?.status?.canPurchase == true) {
                                services.exploration.unlockSkillNode(selected.id)
                                return@onPreviewKeyEvent true
                            }
                        }
                        val direction = when (event.key) {
                            Key.DirectionLeft -> -1 to 0
                            Key.DirectionRight -> 1 to 0
                            Key.DirectionUp -> 0 to -1
                            Key.DirectionDown -> 0 to 1
                            else -> null
                        }
                        if (direction == null || selected == null) return@onPreviewKeyEvent false
                        branch?.nodes.orEmpty().filter { node ->
                            if (direction.first != 0) (node.column - selected.column) * direction.first > 0
                            else (node.row - selected.row) * direction.second > 0
                        }.minByOrNull { node ->
                            if (direction.first != 0) kotlin.math.abs(node.column - selected.column) * 100 + kotlin.math.abs(node.row - selected.row)
                            else kotlin.math.abs(node.row - selected.row) * 100 + kotlin.math.abs(node.column - selected.column)
                        }?.let { selectedId = it.id }
                        true
                    }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.fillMaxSize()
                                .padding(end = 12.dp, bottom = 12.dp)
                                .horizontalScroll(horizontal)
                                .verticalScroll(vertical)
                        ) {
                            val nodes = branch?.nodes.orEmpty()
                            val minRow = nodes.minOfOrNull { it.row } ?: 0
                            val minCol = nodes.minOfOrNull { it.column } ?: 0
                            val rows = (nodes.maxOfOrNull { it.row } ?: 0) - minRow + 1
                            val cols = (nodes.maxOfOrNull { it.column } ?: 0) - minCol + 1

                            Box(Modifier.size((cols * COL_STEP_DP + 40).dp, (rows * ROW_STEP_DP + 40).dp)) {
                                // Connecting dependency lines
                                Canvas(Modifier.fillMaxSize()) {
                                    val byId = nodes.associateBy { it.id }
                                    fun center(row: Int, col: Int) = Offset(
                                        ((col - minCol) * COL_STEP_DP + (NODE_WIDTH_DP / 2) + 20).dp.toPx(),
                                        ((row - minRow) * ROW_STEP_DP + (NODE_HEIGHT_DP / 2) + 20).dp.toPx()
                                    )
                                    nodes.forEach { node ->
                                        node.requirements.forEach { requirement ->
                                            byId[requirement.id]?.let { parent ->
                                                val isUnlocked = parent.status.unlocked && node.status.unlocked
                                                val isAvailable = parent.status.unlocked && node.status.canPurchase
                                                val lineColor = when {
                                                    isUnlocked -> FieldMenuDesign.gold.copy(alpha = 0.85f)
                                                    isAvailable -> FieldMenuDesign.cyan.copy(alpha = 0.65f)
                                                    else -> FieldMenuDesign.cyan.copy(alpha = 0.15f)
                                                }
                                                val strokeWidth = when {
                                                    isUnlocked -> 3.dp.toPx()
                                                    isAvailable -> 2.dp.toPx()
                                                    else -> 1.5.dp.toPx()
                                                }
                                                val start = center(parent.row, parent.column)
                                                val end = center(node.row, node.column)
                                                drawLine(
                                                    color = lineColor,
                                                    start = start,
                                                    end = end,
                                                    strokeWidth = strokeWidth,
                                                    cap = StrokeCap.Round
                                                )
                                                drawCircle(
                                                    color = lineColor,
                                                    radius = if (isUnlocked) 4.dp.toPx() else 3.dp.toPx(),
                                                    center = end
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive Nodes
                                nodes.forEach { node ->
                                    val isSelected = selected?.id == node.id
                                    val statusColor = when {
                                        node.status.unlocked -> FieldMenuDesign.gold
                                        node.status.canPurchase -> FieldMenuDesign.cyan
                                        else -> FieldMenuDesign.textMuted
                                    }
                                    val categoryColor = when (node.category) {
                                        SkillNodeCategory.KEYSTONE -> FieldMenuDesign.gold
                                        SkillNodeCategory.PERK -> FieldMenuDesign.cyan
                                        SkillNodeCategory.STAT -> Color(0xFF8DD7BD)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(
                                                ((node.column - minCol) * COL_STEP_DP + 20).dp,
                                                ((node.row - minRow) * ROW_STEP_DP + 20).dp
                                            )
                                            .size(NODE_WIDTH_DP.dp, NODE_HEIGHT_DP.dp)
                                    ) {
                                        DesktopRichTooltip(
                                            tooltip = {
                                                DesktopSkillTooltipContent(
                                                    skillName = node.name,
                                                    description = node.description.orEmpty(),
                                                    costAp = node.costAp,
                                                    unlocked = node.status.unlocked,
                                                    cooldown = node.activeCombatSkill?.cooldown,
                                                    targeting = node.activeCombatSkill?.targeting,
                                                    accent = statusColor
                                                )
                                            },
                                            accent = statusColor,
                                            maxWidth = 320.dp
                                        ) {
                                            DesktopSkillNodeCard(
                                                node = node,
                                                isSelected = isSelected,
                                                statusColor = statusColor,
                                                categoryColor = categoryColor,
                                                onClick = { selectedId = node.id }
                                            )
                                        }
                                    }
                                }

                                if (nodes.isEmpty()) {
                                    Text(
                                        "No skills available for this branch yet.",
                                        Modifier.padding(20.dp),
                                        color = FieldMenuDesign.textMuted
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(rememberScrollbarAdapter(vertical), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                        HorizontalScrollbar(rememberScrollbarAdapter(horizontal), Modifier.align(Alignment.BottomStart).fillMaxWidth())
                    }
                }
            }

            val details: @Composable (Modifier) -> Unit = { modifier ->
                DesktopMenuCard(modifier) {
                    DesktopMenuScrollPane(Modifier.fillMaxSize().padding(18.dp)) {
                        selected?.let { node ->
                            DesktopSkillDetailsPanel(
                                node = node,
                                names = names,
                                onUnlock = { services.exploration.unlockSkillNode(node.id) }
                            )
                        } ?: run {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a skill node to view details", color = FieldMenuDesign.textMuted)
                            }
                        }
                    }
                }
            }

            if (!compact) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    graph(Modifier.weight(1f).fillMaxHeight())
                    details(Modifier.width(inspectorWidth).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    graph(Modifier.weight(1f).fillMaxWidth())
                    details(Modifier.height(240.dp).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DesktopSkillNodeCard(
    node: SkillTreeNodeUi,
    isSelected: Boolean,
    statusColor: Color,
    categoryColor: Color,
    onClick: () -> Unit
) {
    val isKeystone = node.category == SkillNodeCategory.KEYSTONE
    val shape = when (node.category) {
        SkillNodeCategory.KEYSTONE -> RoundedCornerShape(10.dp)
        SkillNodeCategory.PERK -> CutCornerShape(8.dp)
        SkillNodeCategory.STAT -> RoundedCornerShape(14.dp)
    }

    val borderWidth = if (isSelected) 2.dp else if (isKeystone) 1.5.dp else 1.dp
    val borderColor = if (isSelected) statusColor else statusColor.copy(alpha = if (node.status.unlocked) 0.7f else 0.4f)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxSize().desktopPointerHover(),
        color = if (isSelected) FieldMenuDesign.elevatedPanel else FieldMenuDesign.shell,
        shape = shape,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Category Badge + Status/Cost
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val categoryIcon = when (node.category) {
                        SkillNodeCategory.KEYSTONE -> Icons.Default.Bolt
                        SkillNodeCategory.PERK -> Icons.Default.AutoAwesome
                        SkillNodeCategory.STAT -> Icons.AutoMirrored.Filled.TrendingUp
                    }
                    Icon(
                        categoryIcon,
                        null,
                        Modifier.size(13.dp),
                        tint = if (node.status.unlocked) categoryColor else statusColor
                    )
                    Text(
                        text = when (node.category) {
                            SkillNodeCategory.KEYSTONE -> "KEYSTONE"
                            SkillNodeCategory.PERK -> "PERK"
                            SkillNodeCategory.STAT -> "STAT"
                        },
                        color = categoryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Unlock status / AP Cost
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (node.status.unlocked) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = FieldMenuDesign.gold)
                    } else if (node.status.canPurchase) {
                        Icon(Icons.Default.Stars, null, Modifier.size(14.dp), tint = FieldMenuDesign.cyan)
                        Text("${node.costAp} AP", color = FieldMenuDesign.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Lock, null, Modifier.size(12.dp), tint = FieldMenuDesign.textMuted)
                        Text("${node.costAp} AP", color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                    }
                }
            }

            // Name
            Text(
                node.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isKeystone) FontWeight.Bold else FontWeight.SemiBold),
                color = if (node.status.unlocked) FieldMenuDesign.text else if (node.status.canPurchase) Color.White else FieldMenuDesign.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom summary hint
            val combat = node.activeCombatSkill
            if (combat != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (combat.basePower > 0) "Pwr ${combat.basePower}" else "Support",
                        color = categoryColor.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${combat.cooldown} CD",
                        color = FieldMenuDesign.textMuted,
                        fontSize = 10.sp
                    )
                }
            } else {
                Text(
                    node.description?.take(26).orEmpty().let { if (node.description?.length ?: 0 > 26) "$it…" else it },
                    color = FieldMenuDesign.textMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DesktopSkillDetailsPanel(
    node: SkillTreeNodeUi,
    names: Map<String, String>,
    onUnlock: () -> Unit
) {
    val categoryColor = when (node.category) {
        SkillNodeCategory.KEYSTONE -> FieldMenuDesign.gold
        SkillNodeCategory.PERK -> FieldMenuDesign.cyan
        SkillNodeCategory.STAT -> Color(0xFF8DD7BD)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // --- Header Tag ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = categoryColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = when (node.category) {
                        SkillNodeCategory.KEYSTONE -> "ACTIVE COMBAT KEYSTONE"
                        SkillNodeCategory.PERK -> "PASSIVE PERK"
                        SkillNodeCategory.STAT -> "STAT ATTRIBUTE BOOST"
                    },
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    color = categoryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // --- Name & Status ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                node.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = FieldMenuDesign.text
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (node.status.unlocked) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = FieldMenuDesign.gold)
                    Text("Mastered & Active", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(Icons.Default.Stars, null, Modifier.size(16.dp), tint = FieldMenuDesign.cyan)
                    Text("Requires ${node.costAp} AP", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.35f))

        // --- Description ---
        node.description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = FieldMenuDesign.text)
        }

        // --- Active Combat Ability Tactical Profile ---
        node.activeCombatSkill?.let { skill ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FieldMenuDesign.panel,
                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.22f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "COMBAT TACTICAL PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                        color = FieldMenuDesign.cyan
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Base Power", color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                            Text(
                                if (skill.basePower > 0) "${skill.basePower}" else "Support",
                                color = FieldMenuDesign.gold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Cooldown", color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                            Text("${skill.cooldown} Turns", color = FieldMenuDesign.text, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Scaling", color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                            Text(
                                skill.scaling?.replaceFirstChar { it.uppercase() } ?: "Direct",
                                color = FieldMenuDesign.text,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    skill.targeting?.let { target ->
                        Text(
                            "Target: ${target.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                            color = FieldMenuDesign.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (skill.statusApplications.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status:", color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                            skill.statusApplications.forEach { status ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = FieldMenuDesign.gold.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        status.replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = FieldMenuDesign.gold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Prerequisites ---
        if (node.requirements.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Prerequisites",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = FieldMenuDesign.text
                )
                node.requirements.forEach { requirement ->
                    val isMet = requirement.id !in node.status.unmetRequirements
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (isMet) Icons.Default.CheckCircle else Icons.Default.Lock,
                            null,
                            Modifier.size(14.dp),
                            tint = if (isMet) FieldMenuDesign.gold else FieldMenuDesign.textMuted
                        )
                        Text(
                            requirement.label,
                            color = if (isMet) FieldMenuDesign.text else FieldMenuDesign.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // --- Tier Gating Status ---
        if (!node.status.unlocked && !node.status.meetsTierRequirement) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, FieldMenuDesign.textMuted.copy(alpha = 0.3f))
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = FieldMenuDesign.textMuted)
                    Text(
                        "Invest ${node.status.requiredApForTier} AP in this tree to unlock this tier.",
                        color = FieldMenuDesign.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- Action Button ---
        if (node.status.unlocked) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = FieldMenuDesign.gold)
                Spacer(Modifier.width(8.dp))
                Text("Mastered", color = FieldMenuDesign.gold)
            }
        } else {
            Button(
                onClick = onUnlock,
                enabled = node.status.canPurchase,
                modifier = Modifier.fillMaxWidth().desktopPointerHover(node.status.canPurchase),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (node.status.canPurchase) FieldMenuDesign.gold else FieldMenuDesign.panel,
                    contentColor = if (node.status.canPurchase) Color.Black else FieldMenuDesign.textMuted
                )
            ) {
                Icon(
                    if (node.status.canPurchase) Icons.Default.Bolt else Icons.Default.Lock,
                    null,
                    Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                val buttonText = when {
                    node.status.canPurchase -> "Unlock (${node.costAp} AP)"
                    !node.status.hasEnoughAp -> "Not Enough AP (${node.costAp} Required)"
                    !node.status.meetsTierRequirement -> "Tier Locked"
                    else -> "Prerequisites Locked"
                }
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
