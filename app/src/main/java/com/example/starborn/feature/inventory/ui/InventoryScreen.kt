package com.example.starborn.feature.inventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.feature.inventory.InventoryViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRoute(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    InventoryScreen(
        entries = entries,
        snackbarHostState = snackbarHostState,
        onUseItem = viewModel::useItem,
        onBack = onBack
    )
}

@Preview
@Composable
private fun InventoryScreenPreview() {
    val entries = listOf(
        InventoryEntry(sampleItem("brew", "Black Coffee", "consumable", "A brisk brew"), 2),
        InventoryEntry(sampleItem("kit", "Medkit", "consumable", "Restores 50 HP"), 1)
    )
    InventoryScreen(
        entries = entries,
        snackbarHostState = SnackbarHostState(),
        onUseItem = {},
        onBack = {}
    )
}

private fun sampleItem(
    id: String,
    name: String,
    type: String,
    description: String?
) = com.example.starborn.domain.model.Item(
    id = id,
    name = name,
    type = type,
    description = description
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryScreen(
    entries: List<InventoryEntry>,
    snackbarHostState: SnackbarHostState,
    onUseItem: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember(entries) { mutableStateOf<String?>(null) }
    val types = remember(entries) {
        entries.map { it.item.type.ifBlank { "other" } }.distinct().sorted()
    }
    val filtered = remember(entries, selectedType) {
        selectedType?.let { type ->
            entries.filter { it.item.type.equals(type, ignoreCase = true) }
        } ?: entries
    }
    var selectedEntry by remember(filtered) { mutableStateOf(filtered.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Inventory is empty", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                if (types.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("All") }
                        )
                        types.forEach { type ->
                            FilterChip(
                                selected = selectedType.equals(type, ignoreCase = true),
                                onClick = { selectedType = type },
                                label = { Text(type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filtered, key = { it.item.id }) { entry ->
                            InventoryListRow(
                                entry = entry,
                                selected = entry.item.id == selectedEntry?.item?.id,
                                onClick = { selectedEntry = entry }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    selectedEntry?.let { entry ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            InventoryDetail(
                                entry = entry,
                                onUseItem = onUseItem
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryListRow(
    entry: InventoryEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(entry.item.name, style = MaterialTheme.typography.titleMedium)
        Text("x${entry.quantity}", style = MaterialTheme.typography.bodySmall)
        entry.item.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Divider()
}

@Composable
private fun InventoryDetail(
    entry: InventoryEntry,
    onUseItem: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(entry.item.name, style = MaterialTheme.typography.titleLarge)
            Text("Quantity: ${entry.quantity}", style = MaterialTheme.typography.bodyMedium)
            entry.item.rarity?.let {
                Text("Rarity: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Type: ${entry.item.type}", style = MaterialTheme.typography.bodyMedium)
            entry.item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            entry.item.effect?.let { effect ->
                Divider()
                Text("Effect", style = MaterialTheme.typography.titleMedium)
                effect.restoreHp?.takeIf { it > 0 }?.let {
                    Text("Restore HP: $it", style = MaterialTheme.typography.bodySmall)
                }
                effect.restoreRp?.takeIf { it > 0 }?.let {
                    Text("Restore RP: $it", style = MaterialTheme.typography.bodySmall)
                }
                effect.damage?.takeIf { it > 0 }?.let {
                    Text("Damage: $it", style = MaterialTheme.typography.bodySmall)
                }
                effect.learnSchematic?.let {
                    Text("Teaches schematic: $it", style = MaterialTheme.typography.bodySmall)
                }
                effect.singleBuff?.let {
                    Text("Buff: ${it.stat}+${it.value}", style = MaterialTheme.typography.bodySmall)
                }
                effect.buffs?.forEach {
                    Text("Buff: ${it.stat}+${it.value}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val canUse = entry.item.effect != null
            Button(
                onClick = { onUseItem(entry.item.id) },
                enabled = canUse
            ) {
                Text("Use")
            }
        }
    }
}
