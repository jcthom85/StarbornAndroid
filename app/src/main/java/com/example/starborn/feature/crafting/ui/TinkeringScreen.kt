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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    source: String?,
    initialFilter: TinkeringFilter? = null,
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

    LaunchedEffect(initialFilter) {
        viewModel.setInitialFilter(initialFilter)
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.messages.collectLatest { message ->
                announcement = EventAnnouncementUi(
                    id = System.currentTimeMillis(),
                    title = null,
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
            source = source,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets,
            theme = theme,
            modifier = Modifier.fillMaxSize()
        )

        UIPromptOverlay(
            prompt = promptState.current,
            onDismiss = { promptManager.dismissCurrent() },
            onCollectAll = { sequenceId -> promptManager.dismissItemSequence(sequenceId) },
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
    source: String?,
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
    val fieldKitMode = source?.trim()?.equals("field_kit", ignoreCase = true) == true
    var selectedSection by remember(source) {
        mutableStateOf(if (fieldKitMode) TinkeringSection.Schematics else TinkeringSection.Tinker)
    }
    var selectedRecipePreview by remember { mutableStateOf<TinkeringPreview?>(null) }

    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.tinkeringtable_1
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                StationHeader(
                    title = tinkeringTitle(source),
                    iconRes = R.drawable.tinkering_icon,
                    onBack = onBack,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets,
                    actionContentDescription = "Close Tinkering"
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTabs(
                    selected = selectedSection,
                    onSelect = { selectedSection = it },
                    colors = colors,
                    source = source
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

private fun tinkeringTitle(source: String?): String = when (source?.trim()?.lowercase()) {
    "jeds_bench", "jed_bench", "workshop_floor" -> "Jed's Bench"
    "astra_bench" -> "Astra Bench"
    "field_kit" -> "Field Kit"
    else -> "Field Kit"
}

@Composable
private fun SectionTabs(
    selected: TinkeringSection,
    onSelect: (TinkeringSection) -> Unit,
    colors: TinkeringColors,
    source: String?
) {
    val fieldKitMode = source?.trim()?.equals("field_kit", ignoreCase = true) == true
    val tabs = listOf(
        TinkeringSection.Tinker to if (fieldKitMode) "Field Kit" else "Workbench",
        TinkeringSection.Schematics to "Schematics",
        TinkeringSection.Scrap to "Reclaim"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.panel.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (section, label) ->
                val active = selected == section
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { onSelect(section) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) colors.accent.copy(alpha = 0.18f) else Color.Transparent,
                    border = if (active) {
                        BorderStroke(1.dp, colors.accent.copy(alpha = 0.55f))
                    } else {
                        BorderStroke(1.dp, Color.Transparent)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) colors.accent else colors.textSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
        val all = state.learnedRecipes + state.lockedRecipes
        when (state.filter) {
            TinkeringFilter.ALL -> all
            TinkeringFilter.REPAIR -> all.filter { it.category.equals("repair", ignoreCase = true) }
            TinkeringFilter.GEAR -> all.filter { it.category.equals("gear", ignoreCase = true) }
            TinkeringFilter.PROVISION -> all.filter { it.category.equals("provision", ignoreCase = true) }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RecipeFilterRow(
            recipes = state.learnedRecipes + state.lockedRecipes,
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
                                category = it.category,
                                method = it.method,
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
                    text = "No crafted items to reclaim right now.",
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
            .heightIn(min = 104.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        color = colors.card.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Reclaim Preview", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            when {
                itemName.isNullOrBlank() -> Text(
                    text = "Choose a crafted item to see what you will reclaim.",
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
                        text = "Reclaiming $itemName yields:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    if (recipe.ingredients.isNotEmpty()) {
                        ItemPill(
                            label = "Returned supplies",
                            value = recipe.ingredients.joinToString { "${it.label} x${it.required}" },
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
    val buttonHeight = if (largeTouchTargets) 52.dp else 42.dp
    val titleColor = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (highContrastMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    val statusText: String
    val statusColor: Color
    when {
        recipe.canCraft -> {
            statusText = "Ready"
            statusColor = MaterialTheme.colorScheme.tertiary
        }
        !recipe.learned -> {
            statusText = "Locked"
            statusColor = MaterialTheme.colorScheme.error
        }
        else -> {
            statusText = "Need parts"
            statusColor = MaterialTheme.colorScheme.primary
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview(recipe) },
        shape = RoundedCornerShape(14.dp),
        color = if (highContrastMode) Color(0xFF0E1623) else MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (highContrastMode) 0.42f else 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        recipe.categoryLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = bodyColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(text = statusText, color = statusColor, highContrastMode = highContrastMode)
            }
            recipe.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = if (highContrastMode) Color(0xFF121A28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (highContrastMode) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Builds ${recipe.resultDisplayName()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = recipe.ingredients.joinToString { "${it.label} ${it.available}/${it.required}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = bodyColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onAutoFill(recipe.id) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight)
                        .semantics { contentDescription = "Auto-Fill ${recipe.name}" },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Load")
                }
                Button(
                    onClick = { onCraft(recipe.id) },
                    enabled = recipe.canCraft,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight)
                        .semantics { contentDescription = "Craft ${recipe.name}" },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Craft")
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
                Text(
                    text = preview.categoryLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                preview.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Text(
                    text = "Result: ${preview.resultDisplayName()}",
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
    val buttonHeight = if (largeTouchTargets) 52.dp else 44.dp
    val preview = bench.preview
    val hasSelection = bench.mainItemName != null || bench.componentNames.any { it.isNotBlank() }
    val benchStatus = when {
        bench.canCraftSelection -> "Ready"
        preview != null -> "Need parts"
        hasSelection -> "No match"
        else -> "Empty"
    }
    val statusColor = when (benchStatus) {
        "Ready" -> colors.accent
        "Need parts" -> MaterialTheme.colorScheme.primary
        "No match" -> MaterialTheme.colorScheme.error
        else -> colors.textSecondary
    }
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
        color = colors.panel.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Assembly",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preview?.name ?: "Load a schematic or choose parts manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(text = benchStatus, color = statusColor, highContrastMode = highContrastMode)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SlotTile(
                    label = "Base",
                    value = bench.mainItemName ?: "Choose item",
                    selected = bench.mainItemName != null,
                    icon = if (bench.mainItemName != null) Icons.Filled.Close else Icons.Filled.Add,
                    colors = colors,
                    onClick = onSelectMain
                )
                SlotTile(
                    label = "Part A",
                    value = bench.componentNames.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Optional",
                    selected = bench.componentNames.getOrNull(0)?.isNotBlank() == true,
                    icon = if (bench.componentNames.getOrNull(0)?.isNotBlank() == true) Icons.Filled.Close else Icons.Filled.Add,
                    colors = colors,
                    onClick = onSelectComponent1
                )
                SlotTile(
                    label = "Part B",
                    value = bench.componentNames.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Optional",
                    selected = bench.componentNames.getOrNull(1)?.isNotBlank() == true,
                    icon = if (bench.componentNames.getOrNull(1)?.isNotBlank() == true) Icons.Filled.Close else Icons.Filled.Add,
                    colors = colors,
                    onClick = onSelectComponent2
                )
            }
            WorkbenchResultStrip(
                preview = preview,
                colors = colors,
                highContrastMode = highContrastMode
            )
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
                        .defaultMinSize(minWidth = 120.dp),
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
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
                        .defaultMinSize(minWidth = 120.dp),
                    shape = RoundedCornerShape(12.dp),
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
    label: String,
    value: String,
    selected: Boolean,
    icon: ImageVector,
    colors: TinkeringColors,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.accent.copy(alpha = 0.12f) else colors.slot.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (selected) colors.accent.copy(alpha = 0.48f) else colors.border.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) colors.accent.copy(alpha = 0.18f) else colors.border.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) colors.accent else colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WorkbenchResultStrip(
    preview: TinkeringPreview?,
    colors: TinkeringColors,
    highContrastMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = if (highContrastMode) Color(0xFF0D1722) else colors.slot.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Output",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
            if (preview == null) {
                Text(
                    text = "No schematic loaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary
                )
            } else {
                Text(
                    text = preview.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                preview.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RequirementSummary(
    requirements: List<TinkeringRequirementStatus>,
    colors: TinkeringColors
) {
    if (requirements.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = colors.slot.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Required parts",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
            requirements.forEach { req ->
                val meetsRequirement = req.available >= req.required
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = req.label,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${req.available}/${req.required}",
                        color = if (meetsRequirement) colors.accent else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeFilterRow(
    recipes: List<TinkeringRecipeUi>,
    selected: TinkeringFilter,
    onFilterChange: (TinkeringFilter) -> Unit
) {
    val filters = listOf(
        TinkeringFilter.ALL to "All ${recipes.size}",
        TinkeringFilter.REPAIR to "Repair ${recipes.countCategory("repair")}",
        TinkeringFilter.GEAR to "Gear ${recipes.countCategory("gear")}",
        TinkeringFilter.PROVISION to "Food ${recipes.countCategory("provision")}"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        filters.forEach { (filter, label) ->
            val active = selected == filter
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clickable { onFilterChange(filter) },
                shape = RoundedCornerShape(11.dp),
                color = if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
                },
                border = BorderStroke(
                    1.dp,
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
                    }
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun List<TinkeringRecipeUi>.countCategory(category: String): Int =
    count { it.category.equals(category, ignoreCase = true) }

private fun TinkeringRecipeUi.categoryLabel(): String =
    listOfNotNull(category.displayLabel(), method.displayLabel()).filter { it.isNotBlank() }.joinToString(" - ")

private fun TinkeringPreview.categoryLabel(): String =
    listOfNotNull(category.displayLabel(), method.displayLabel()).filter { it.isNotBlank() }.joinToString(" - ")

private fun TinkeringRecipeUi.resultDisplayName(): String =
    resultId.displayLabel().ifBlank { name }

private fun TinkeringPreview.resultDisplayName(): String =
    resultId.displayLabel().ifBlank { name }

private fun String?.displayLabel(): String =
    this.orEmpty()
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

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
    val outlineColor = themeColor(theme?.border, eventAccent.copy(alpha = 0.55f))
    val backgroundColor = themeColor(theme?.bg, Color(0xFF060B14)).copy(alpha = 0.96f)
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
                .semantics { contentDescription = "Tinkering result. Tap to continue" }
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
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
                category = "gear",
                method = "mod",
                base = "Focusing Lens",
                components = listOf("Wiring Bundle"),
                ingredients = listOf(
                    TinkeringRequirementStatus("Focusing Lens", 1, 1),
                    TinkeringRequirementStatus("Wiring Bundle", 1, 2)
                ),
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
                category = "gear",
                method = "mod",
                base = "Ballistic Weave",
                components = listOf("Scrap Metal", "Scrap Metal"),
                ingredients = listOf(
                    TinkeringRequirementStatus("Ballistic Weave", 1, 0),
                    TinkeringRequirementStatus("Scrap Metal", 2, 1)
                ),
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
                category = "gear",
                method = "mod",
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
        source = "jeds_bench",
        highContrastMode = false,
        largeTouchTargets = false,
        theme = null
    )
}
