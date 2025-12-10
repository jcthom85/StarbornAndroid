package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.starborn.R
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.crafting.TinkeringBenchState
import com.example.starborn.feature.crafting.TinkeringFilter
import com.example.starborn.feature.crafting.TinkeringItemChoice
import com.example.starborn.feature.crafting.TinkeringPreview
import com.example.starborn.feature.crafting.TinkeringRequirementStatus
import com.example.starborn.feature.crafting.TinkeringRecipeUi
import com.example.starborn.feature.crafting.TinkeringUiState
import com.example.starborn.feature.exploration.viewmodel.EventAnnouncementUi
import com.example.starborn.feature.exploration.ui.UIPromptOverlay
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinkeringRoute(
    viewModel: CraftingViewModel,
    onBack: () -> Unit,
    onCrafted: (CraftingOutcome.Success) -> Unit,
    onClosed: () -> Unit,
    promptManager: UIPromptManager,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme? = null
){
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val promptState by promptManager.state.collectAsState()
    var announcement by remember { mutableStateOf<EventAnnouncementUi?>(null) }
    val accentColor = themeColor(theme?.accent, Color(0xFFF5B437))

    LaunchedEffect(Unit) {
        launch {
            viewModel.messages.collectLatest { message ->
                announcement = EventAnnouncementUi(
                    id = System.currentTimeMillis(),
                    title = "Tinkering",
                    message = message,
                    accentColor = accentColor.toArgb().toLong()
                )
            }
        }
        launch {
            viewModel.craftResults.collectLatest { onCrafted(it) }
        }
    }

    DisposableEffect(Unit) {
        onDispose { onClosed() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TinkeringScreen(
            state = uiState,
            snackbarHostState = snackbarHost,
            onRecipeCraft = viewModel::craft,
            onBenchCraft = viewModel::craftFromBench,
            onAutoFillRecipe = viewModel::autoFill,
            onClearBench = viewModel::clearBench,
            onSelectMain = viewModel::selectMain,
            onSelectComponent = viewModel::selectComponent,
            onScrap = viewModel::scrap,
            onFilterChange = viewModel::setFilter,
            onBack = onBack,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets,
            theme = theme,
            modifier = Modifier.fillMaxSize()
        )

        UIPromptOverlay(
            prompt = promptState.current,
            onDismiss = { promptManager.dismissCurrent() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
        announcement?.let { current ->
            CraftAnnouncementOverlay(
                announcement = current,
                theme = theme,
                onDismiss = { announcement = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TinkeringScreen(
    state: TinkeringUiState,
    snackbarHostState: SnackbarHostState,
    onRecipeCraft: (String) -> Unit,
    onBenchCraft: () -> Unit,
    onAutoFillRecipe: (String) -> Unit,
    onClearBench: () -> Unit,
    onSelectMain: (String?) -> Unit,
    onSelectComponent: (Int, String?) -> Unit,
    onScrap: (String) -> Unit,
    onFilterChange: (TinkeringFilter) -> Unit,
    onBack: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme?,
    modifier: Modifier = Modifier
){
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(theme, highContrastMode, colorScheme) {
        val accent = if (highContrastMode) colorScheme.primary else themeColor(theme?.accent, Color(0xFFF5B437))
        val base = if (highContrastMode) Color(0xFF0E1623) else themeColor(theme?.bg, Color(0xFF0C131D))
        val panel = base.copy(alpha = if (highContrastMode) 0.96f else 0.9f)
        val slot = if (highContrastMode) Color(0xFF0F1A27) else base.copy(alpha = 0.75f)
        val border = if (highContrastMode) Color.White.copy(alpha = 0.35f) else themeColor(theme?.border, Color.White.copy(alpha = 0.28f))
        val text = if (highContrastMode) Color.White else themeColor(theme?.fg, colorScheme.onSurface)
        val muted = if (highContrastMode) Color.White.copy(alpha = 0.78f) else text.copy(alpha = 0.78f)
        val card = if (highContrastMode) Color(0xFF121A28) else base.copy(alpha = 0.82f)
        TinkeringColors(
            accent = accent,
            panel = panel,
            slot = slot,
            border = border,
            textPrimary = text,
            textSecondary = muted,
            card = card
        )
    }
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var scrapSelection by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf(TinkeringSection.Tinker) }
    var selectedRecipePreview by remember { mutableStateOf<TinkeringPreview?>(null) }

    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.tinkeringtable_1,
        vignetteRes = R.drawable.tinkering_vignette
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                StationHeader(
                    title = "Let's Tinker!",
                    iconRes = R.drawable.tinkering_icon,
                    onBack = onBack,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTabs(
                    selected = selectedSection,
                    onSelect = { selectedSection = it },
                    colors = colors
                )
                if (state.scrapChoices.isNotEmpty() && scrapSelection == null) {
                    scrapSelection = state.scrapChoices.firstOrNull()?.id
                } else if (state.scrapChoices.isEmpty()) {
                    scrapSelection = null
                }

                if (state.isLoading) {
                    Text("Loading schematics...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    when (selectedSection) {
                        TinkeringSection.Tinker -> TinkerSection(
                            state = state,
                            colors = colors,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            onBenchCraft = onBenchCraft,
                            onClearBench = onClearBench,
                            onSelectMain = { pickerTarget = PickerTarget.Main },
                            onSelectComponent1 = { pickerTarget = PickerTarget.Component1 },
                            onSelectComponent2 = { pickerTarget = PickerTarget.Component2 }
                        )
                        TinkeringSection.Schematics -> SchematicsSection(
                            state = state,
                            selectedPreview = selectedRecipePreview,
                            onPreview = {
                                selectedRecipePreview = it
                            },
                            onAutoFillRecipe = onAutoFillRecipe,
                            onCraft = onRecipeCraft,
                            onFilterChange = onFilterChange,
                            colors = colors,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                        )
                        TinkeringSection.Scrap -> ScrapSection(
                            state = state,
                            selectedId = scrapSelection,
                            onSelect = { scrapSelection = it },
                            onScrap = onScrap,
                            colors = colors,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                        )
                    }
                }
            }
        }
    }

    pickerTarget?.let { target ->
        ItemPickerDialog(
            title = when (target) {
                PickerTarget.Main -> "Select Main Item"
                PickerTarget.Component1 -> "Select Component 1"
                PickerTarget.Component2 -> "Select Component 2"
            },
            choices = state.inventory,
            highContrastMode = highContrastMode,
            onSelect = { id ->
                when (target) {
                    PickerTarget.Main -> onSelectMain(id)
                    PickerTarget.Component1 -> onSelectComponent(0, id)
                    PickerTarget.Component2 -> onSelectComponent(1, id)
                }
                pickerTarget = null
            },
            onClear = {
                when (target) {
                    PickerTarget.Main -> onSelectMain(null)
                    PickerTarget.Component1 -> onSelectComponent(0, null)
                    PickerTarget.Component2 -> onSelectComponent(1, null)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
    }
}

@Composable
private fun SectionTabs(
    selected: TinkeringSection,
    onSelect: (TinkeringSection) -> Unit,
    colors: TinkeringColors
) {
    val tabs = listOf(
        TinkeringSection.Tinker to "Tinker",
        TinkeringSection.Schematics to "Schematics",
        TinkeringSection.Scrap to "Scrap"
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        tabs.forEach { (section, label) ->
            val active = selected == section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(section) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (active) colors.accent else colors.textSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (active) colors.accent else colors.border.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun TinkerSection(
    state: TinkeringUiState,
    colors: TinkeringColors,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    onBenchCraft: () -> Unit,
    onClearBench: () -> Unit,
    onSelectMain: () -> Unit,
    onSelectComponent1: () -> Unit,
    onSelectComponent2: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TinkeringBenchCard(
            bench = state.bench,
            colors = colors,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets,
            onSelectMain = onSelectMain,
            onSelectComponent1 = onSelectComponent1,
            onSelectComponent2 = onSelectComponent2,
            onCraft = onBenchCraft,
            onClear = onClearBench
        )
        PreviewCard(
            preview = state.bench.preview,
            colors = colors,
            highContrastMode = highContrastMode
        )
    }
}

@Composable
private fun SchematicsSection(
    state: TinkeringUiState,
    selectedPreview: TinkeringPreview?,
    onPreview: (TinkeringPreview?) -> Unit,
    onAutoFillRecipe: (String) -> Unit,
    onCraft: (String) -> Unit,
    onFilterChange: (TinkeringFilter) -> Unit,
    colors: TinkeringColors,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val recipes = remember(state.filter, state.learnedRecipes, state.lockedRecipes) {
        when (state.filter) {
            TinkeringFilter.LEARNED -> state.learnedRecipes
            TinkeringFilter.ALL -> state.learnedRecipes + state.lockedRecipes
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RecipeFilterRow(
            learnedCount = state.learnedRecipes.size,
            lockedCount = state.lockedRecipes.size,
            selected = state.filter,
            onFilterChange = onFilterChange
        )
        if (recipes.isEmpty()) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "You haven't learned any schematics yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    TinkeringRecipeCard(
                        recipe = recipe,
                        onAutoFill = onAutoFillRecipe,
                        onCraft = onCraft,
                        onPreview = {
                            val preview = TinkeringPreview(
                                recipeId = it.id,
                                name = it.name,
                                description = it.description,
                                resultId = it.resultId,
                                learned = it.learned
                            )
                            onPreview(preview)
                        },
                        highContrastMode = highContrastMode,
                        largeTouchTargets = largeTouchTargets
                    )
                }
            }
        }
        PreviewCard(
            preview = selectedPreview,
            colors = colors,
            highContrastMode = highContrastMode,
            emptyMessage = "Tap a schematic to see its result."
        )
    }
}

@Composable
private fun ScrapSection(
    state: TinkeringUiState,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onScrap: (String) -> Unit,
    colors: TinkeringColors,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.scrapChoices.isEmpty()) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No crafted items to scrap right now.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            ScrapCard(
                choices = state.scrapChoices,
                selectedId = selectedId,
                onSelect = onSelect,
                onScrap = { id -> id?.let(onScrap) },
                highContrastMode = highContrastMode,
                largeTouchTargets = largeTouchTargets
            )
        }
        val recipe = findRecipeForResult(selectedId, state)
        ScrapPreviewCard(
            itemName = selectedId,
            recipe = recipe,
            colors = colors,
            highContrastMode = highContrastMode
        )
    }
}

@Composable
private fun ScrapPreviewCard(
    itemName: String?,
    recipe: TinkeringRecipeUi?,
    colors: TinkeringColors,
    highContrastMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Scrap Preview", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            when {
                itemName.isNullOrBlank() -> Text(
                    text = "Choose a crafted item to see what you'll reclaim.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                recipe == null -> Text(
                    text = "No breakdown data found for $itemName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                else -> {
                    Text(
                        text = "Scrapping $itemName yields:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    ItemPill(label = "Base", value = recipe.base, highContrastMode = highContrastMode)
                    if (recipe.components.isNotEmpty()) {
                        ItemPill(
                            label = "Components",
                            value = recipe.components.joinToString(),
                            highContrastMode = highContrastMode
                        )
                    }
                }
            }
        }
    }
}

private fun findRecipeForResult(resultId: String?, state: TinkeringUiState): TinkeringRecipeUi? {
    if (resultId.isNullOrBlank()) return null
    val normalized = normalizeToken(resultId)
    val all = state.learnedRecipes + state.lockedRecipes
    return all.firstOrNull { recipe ->
        val tokens = listOf(
            normalizeToken(recipe.resultId),
            normalizeToken(recipe.name),
            normalizeToken(recipe.id)
        )
        tokens.any { it == normalized }
    }
}

private fun normalizeToken(raw: String): String =
    raw.trim().lowercase().replace("[^a-z0-9]+".toRegex(), "")

@Composable
private fun TinkeringRecipeCard(
    recipe: TinkeringRecipeUi,
    onAutoFill: (String) -> Unit,
    onCraft: (String) -> Unit,
    onPreview: (TinkeringRecipeUi) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview(recipe) },
        shape = RoundedCornerShape(18.dp)
    ) {
        Surface(
            color = if (highContrastMode) Color(0xFF0E1623) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = titleColor)
                        recipe.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = bodyColor)
                        }
                    }
                    val statusText: String
                    val statusColor: Color
                    when {
                        !recipe.learned -> {
                            statusText = "Locked"
                            statusColor = MaterialTheme.colorScheme.error
                        }
                        recipe.canCraft -> {
                            statusText = "Ready"
                            statusColor = MaterialTheme.colorScheme.tertiary
                        }
                        else -> {
                            statusText = "Need parts"
                            statusColor = MaterialTheme.colorScheme.primary
                        }
                    }
                    StatusPill(text = statusText, color = statusColor, highContrastMode = highContrastMode)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ItemPill(label = "Base", value = recipe.base, highContrastMode = highContrastMode)
                    if (recipe.components.isNotEmpty()) {
                        ItemPill(
                            label = "Components",
                            value = recipe.components.joinToString(),
                            highContrastMode = highContrastMode
                        )
                    }
                }
                Surface(
                    color = if (highContrastMode) Color(0xFF121A28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = if (highContrastMode) 0.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Result: ${recipe.resultId} crafted at the bench.",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onAutoFill(recipe.id) },
                        modifier = Modifier.heightIn(min = buttonHeight).defaultMinSize(minWidth = 120.dp)
                    ) {
                        Text("Auto-Fill")
                    }
                    Button(
                        onClick = { onCraft(recipe.id) },
                        enabled = recipe.canCraft,
                        modifier = Modifier.heightIn(min = buttonHeight).defaultMinSize(minWidth = 120.dp)
                    ) {
                        Text("Craft")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    preview: TinkeringPreview?,
    colors: TinkeringColors,
    highContrastMode: Boolean,
    emptyMessage: String = "Add items to see what this combo produces."
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Preview", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            if (preview == null) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Text(preview.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                preview.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Text(
                    text = "Result: ${preview.resultId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                val statusText = if (preview.learned) "Known schematic" else "Discovery"
                StatusPill(text = statusText, color = if (preview.learned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, highContrastMode = highContrastMode)
            }
        }
    }
}

@Composable
private fun TinkeringBenchCard(
    bench: TinkeringBenchState,
    colors: TinkeringColors,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    onSelectMain: () -> Unit,
    onSelectComponent1: () -> Unit,
    onSelectComponent2: () -> Unit,
    onCraft: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        color = colors.panel,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Combine a main item with up to two components to modify or enhance gear.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SlotTile(
                    title = bench.mainItemName ?: "Select Main Item",
                    subtitle = bench.mainItemName?.let { "Tap to change" } ?: "Tap to choose",
                    icon = if (bench.mainItemName != null) Icons.Filled.Close else Icons.Filled.Add,
                    optional = false,
                    colors = colors,
                    onClick = onSelectMain
                )
                SlotTile(
                    title = bench.componentNames.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Add Component",
                    subtitle = bench.componentNames.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { "Tap to change" } ?: "Optional",
                    icon = if (bench.componentNames.getOrNull(0)?.isNotBlank() == true) Icons.Filled.Close else Icons.Filled.Add,
                    optional = true,
                    colors = colors,
                    onClick = onSelectComponent1
                )
                SlotTile(
                    title = bench.componentNames.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Add Component",
                    subtitle = bench.componentNames.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { "Tap to change" } ?: "Optional",
                    icon = if (bench.componentNames.getOrNull(1)?.isNotBlank() == true) Icons.Filled.Close else Icons.Filled.Add,
                    optional = true,
                    colors = colors,
                    onClick = onSelectComponent2
                )
            }
            if (bench.requirements.isNotEmpty()) {
                RequirementSummary(
                    requirements = bench.requirements,
                    colors = colors
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight)
                        .defaultMinSize(minWidth = 136.dp),
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.textPrimary
                    )
                ) {
                    Text("Clear", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
                Button(
                    onClick = onCraft,
                    enabled = bench.canCraftSelection,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight)
                        .defaultMinSize(minWidth = 136.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Craft", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.SlotTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    optional: Boolean,
    colors: TinkeringColors,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = colors.slot,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.border.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                if (optional && subtitle.isBlank()) "Optional" else subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RequirementSummary(
    requirements: List<TinkeringRequirementStatus>,
    colors: TinkeringColors
) {
    if (requirements.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Requirements",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        requirements.forEach { req ->
            val meetsRequirement = req.available >= req.required
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = req.label,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${req.available}/${req.required}",
                    color = if (meetsRequirement) colors.accent else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RecipeFilterRow(
    learnedCount: Int,
    lockedCount: Int,
    selected: TinkeringFilter,
    onFilterChange: (TinkeringFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = selected == TinkeringFilter.LEARNED,
            onClick = { onFilterChange(TinkeringFilter.LEARNED) },
            label = { Text("Learned ($learnedCount)") }
        )
        FilterChip(
            selected = selected == TinkeringFilter.ALL,
            onClick = { onFilterChange(TinkeringFilter.ALL) },
            label = { Text("All (${learnedCount + lockedCount})") }
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color, highContrastMode: Boolean) {
    Surface(
        color = if (highContrastMode) Color(0xFF111B28) else color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (highContrastMode) 0.dp else 1.dp
    ) {
        Text(
            text = text,
            color = if (highContrastMode) Color.White else color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ItemPill(label: String, value: String, highContrastMode: Boolean) {
    Surface(
        color = if (highContrastMode) Color(0xFF101826) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (highContrastMode) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (highContrastMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ScrapCard(
    choices: List<TinkeringItemChoice>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onScrap: (String?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        color = if (highContrastMode) Color(0xFF0C1520) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Scrap", style = MaterialTheme.typography.titleSmall, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
                val current = choices.firstOrNull { it.id == selectedId }
                Text(
                    current?.name ?: "Select a crafted item",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    current?.description ?: "Break crafted items back into their parts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = {
                    val next = choices.dropWhile { it.id != selectedId }.drop(1).firstOrNull() ?: choices.firstOrNull()
                    onSelect(next?.id)
                },
                modifier = Modifier
                    .defaultMinSize(minWidth = 116.dp)
                    .heightIn(min = buttonHeight)
            ) {
                Text("Cycle")
            }
            Button(
                onClick = { onScrap(selectedId) },
                enabled = selectedId != null,
                modifier = Modifier
                    .defaultMinSize(minWidth = 116.dp)
                    .heightIn(min = buttonHeight)
            ) {
                Text("Scrap")
            }
        }
    }
}

@Composable
private fun ItemPickerDialog(
    title: String,
    choices: List<TinkeringItemChoice>,
    highContrastMode: Boolean,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(title) },
        text = {
            if (choices.isEmpty()) {
                Text("No items available.", color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(choices, key = { it.id }) { choice ->
                        Surface(
                            tonalElevation = 1.dp,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(choice.id) },
                            color = if (highContrastMode) Color(0xFF0E1623) else MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(choice.name, style = MaterialTheme.typography.titleSmall, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
                                Text("Qty: ${choice.quantity}", style = MaterialTheme.typography.labelSmall, color = if (highContrastMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                choice.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onClear(); onDismiss() }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun CraftAnnouncementOverlay(
    announcement: EventAnnouncementUi,
    theme: Theme?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventAccent = Color(announcement.accentColor)
    val accentColor = themeColor(theme?.accent, eventAccent)
    val outlineColor = themeColor(theme?.border, accentColor.copy(alpha = 0.8f))
    val backgroundColor = themeColor(theme?.bg, Color(0xFF040914)).copy(alpha = 0.95f)
    val hasTitle = !announcement.title.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, outlineColor),
            color = backgroundColor,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasTitle) {
                    Text(
                        text = announcement.title!!,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            shadow = Shadow(
                                color = accentColor.copy(alpha = 0.65f),
                                blurRadius = 18f
                            )
                        ),
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(color = accentColor.copy(alpha = 0.4f))
                }
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.4f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    label: String,
    value: String,
    detail: String,
    highContrastMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (highContrastMode) Color(0xFF0E1623) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (highContrastMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (highContrastMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class PickerTarget { Main, Component1, Component2 }
private enum class TinkeringSection { Tinker, Schematics, Scrap }

private data class TinkeringColors(
    val accent: Color,
    val panel: Color,
    val slot: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val card: Color
)

@Preview
@Composable
fun TinkeringScreenPreview() {
    val previewState = TinkeringUiState(
        isLoading = false,
        learnedRecipes = listOf(
            TinkeringRecipeUi(
                id = "mod_power_lens_1",
                name = "Power Lens Mk. I",
                description = "Replaces a standard lens to boost energy output.",
                base = "Focusing Lens",
                components = listOf("Wiring Bundle"),
                resultId = "Power Lens Mk. I",
                canCraft = true,
                learned = true
            )
        ),
        lockedRecipes = listOf(
            TinkeringRecipeUi(
                id = "mod_ergonomic_grip_1",
                name = "Ergonomic Grip",
                description = "Improves stability.",
                base = "Ballistic Weave",
                components = listOf("Scrap Metal", "Scrap Metal"),
                resultId = "Ergonomic Grip",
                canCraft = false,
                learned = false
            )
        ),
        bench = TinkeringBenchState(
            mainItemId = "Focusing Lens",
            mainItemName = "Focusing Lens",
            componentIds = listOf("Wiring Bundle"),
            componentNames = listOf("Wiring Bundle"),
            activeRecipeId = "mod_power_lens_1",
            preview = TinkeringPreview(
                recipeId = "mod_power_lens_1",
                name = "Power Lens Mk. I",
                description = "Replaces a standard lens to boost energy output.",
                resultId = "Power Lens Mk. I",
                learned = true
            ),
            requirements = listOf(
                TinkeringRequirementStatus("Focusing Lens", 1, 1),
                TinkeringRequirementStatus("Wiring Bundle", 1, 0)
            ),
            canCraftSelection = false
        ),
        inventory = listOf(
            TinkeringItemChoice("Focusing Lens", "Focusing Lens", "Base lens", 1),
            TinkeringItemChoice("Wiring Bundle", "Wiring Bundle", "Bundled wires", 2)
        ),
        scrapChoices = listOf(
            TinkeringItemChoice("Power Lens Mk. I", "Power Lens Mk. I", "Crafted lens", 1)
        )
    )
    TinkeringScreen(
        state = previewState,
        snackbarHostState = SnackbarHostState(),
        onRecipeCraft = {},
        onBenchCraft = {},
        onAutoFillRecipe = {},
        onClearBench = {},
        onSelectMain = {},
        onSelectComponent = { _, _ -> },
        onScrap = {},
        onFilterChange = {},
        onBack = {},
        highContrastMode = false,
        largeTouchTargets = false,
        theme = null
    )
}
