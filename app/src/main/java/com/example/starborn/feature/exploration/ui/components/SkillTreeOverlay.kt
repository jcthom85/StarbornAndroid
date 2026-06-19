package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.exploration.viewmodel.SkillTreeBranchUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeNodeUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeOverlayUi
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.theme.themeColor

@Composable
fun SkillTreeOverlay(
    overlay: SkillTreeOverlayUi,
    theme: Theme?,
    onClose: () -> Unit,
    onUnlockSkill: (String) -> Unit
) {
    val accentColor = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val borderColor = themeColor(theme?.border, Color.White.copy(alpha = 0.65f))
    val scrimColor = Color.Black.copy(alpha = 0.82f)
    val scrollState = rememberScrollState()
    val portraitPainter = rememberAssetPainter(
        imagePath = overlay.portraitPath,
        fallback = painterResource(R.drawable.inventory_icon)
    )
    var selectedBranchIndex by rememberSaveable(overlay.characterId) { mutableStateOf(0) }
    if (overlay.branches.isNotEmpty()) {
        val maxIndex = overlay.branches.lastIndex
        if (selectedBranchIndex > maxIndex) {
            selectedBranchIndex = maxIndex
        } else if (selectedBranchIndex < 0) {
            selectedBranchIndex = 0
        }
    } else if (selectedBranchIndex != 0) {
        selectedBranchIndex = 0
    }
    val selectedBranch = overlay.branches.getOrNull(selectedBranchIndex)
    var selectedNodeId by rememberSaveable(overlay.characterId, selectedBranchIndex) {
        mutableStateOf(selectedBranch?.nodes?.firstOrNull()?.id)
    }
    val selectedNode = selectedBranch?.nodes?.firstOrNull { it.id == selectedNodeId }
        ?: selectedBranch?.nodes?.firstOrNull()

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .padding(20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .widthIn(max = 900.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF02070E).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = portraitPainter,
                                contentDescription = overlay.characterName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${overlay.characterName} – Skills",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "AP Available: ${overlay.availableAp}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        TextButton(onClick = onClose) {
                            Text("Close", color = accentColor)
                        }
                    }

                    SkillTreeBranchTabs(
                        branches = overlay.branches,
                        selectedIndex = selectedBranchIndex,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onSelect = { selectedBranchIndex = it }
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
                        color = Color.White.copy(alpha = 0.02f)
                    ) {
                        if (selectedBranch != null && selectedBranch.nodes.isNotEmpty()) {
                            SkillTreeGrid(
                                branch = selectedBranch,
                                accentColor = accentColor,
                                selectedNodeId = selectedNode?.id,
                                onSelectNode = { node ->
                                    selectedNodeId = node.id
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No skills available for this branch yet.",
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    SkillTreeNodeDetails(
                        node = selectedNode,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onUnlock = { onUnlockSkill(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTreeBranchTabs(
    branches: List<SkillTreeBranchUi>,
    selectedIndex: Int,
    accentColor: Color,
    borderColor: Color,
    onSelect: (Int) -> Unit
) {
    if (branches.isEmpty()) {
        Text(
            text = "No skill data available.",
            color = Color.White.copy(alpha = 0.7f)
        )
        return
    }
    Row(
        modifier = Modifier
            .widthIn(max = 1200.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        branches.forEachIndexed { index, branch ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) accentColor else borderColor.copy(alpha = 0.7f)
                ),
                color = if (selected) accentColor.copy(alpha = 0.15f) else Color.Transparent
            ) {
                Text(
                    text = branch.title,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SkillTreeGrid(
    branch: SkillTreeBranchUi,
    accentColor: Color,
    selectedNodeId: String?,
    onSelectNode: (SkillTreeNodeUi) -> Unit
) {
    val nodesByPosition = remember(branch) {
        branch.nodes.associateBy { it.row to it.column }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { col ->
                    val node = nodesByPosition[row to col]
                    SkillTreeGridCell(
                        node = node,
                        accentColor = accentColor,
                        selected = node?.id == selectedNodeId,
                        onSelect = onSelectNode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTreeGridCell(
    node: SkillTreeNodeUi?,
    accentColor: Color,
    selected: Boolean,
    onSelect: (SkillTreeNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (node == null) {
        Box(
            modifier = modifier.aspectRatio(1f)
        )
        return
    }
    val status = node.status
    val background = when {
        status.unlocked -> accentColor.copy(alpha = 0.25f)
        status.canPurchase -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.03f)
    }
    val borderColor = when {
        selected -> accentColor
        status.unlocked -> accentColor.copy(alpha = 0.9f)
        status.canPurchase -> accentColor.copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.3f)
    }
    Surface(
        onClick = { onSelect(node) },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        color = background,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = node.name,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2
            )
            Text(
                text = when {
                    status.unlocked -> "Learned"
                    else -> "Cost: ${node.costAp} AP"
                },
                color = Color.White.copy(alpha = if (status.canPurchase) 0.9f else 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SkillTreeNodeDetails(
    node: SkillTreeNodeUi?,
    accentColor: Color,
    borderColor: Color,
    onUnlock: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        color = Color.White.copy(alpha = 0.02f)
    ) {
        if (node == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a skill node to see the details.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            return@Surface
        }
        val status = node.status
        val unmetLabels = status.unmetRequirements.mapNotNull { unmetId ->
            node.requirements.firstOrNull { it.id == unmetId }?.label
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = node.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Cost: ${node.costAp} AP",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
            val description = node.description ?: "Unique ability upgrade."
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
            node.requirements.takeIf { it.isNotEmpty() }?.let { requirements ->
                Text(
                    text = "Requires: ${requirements.joinToString(", ") { it.label }}",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!status.meetsTierRequirement) {
                Text(
                    text = "Invest ${status.requiredApForTier} AP in this tree to unlock this tier.",
                    color = Color(0xFFFFD27F),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (unmetLabels.isNotEmpty()) {
                Text(
                    text = "Still needed: ${unmetLabels.joinToString(", ")}",
                    color = Color(0xFFFF9E8C),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onUnlock(node.id) },
                    enabled = status.canPurchase,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(
                        text = when {
                            status.unlocked -> "Learned"
                            status.canPurchase -> "Unlock"
                            else -> "Locked"
                        }
                    )
                }
            }
        }
    }
}
