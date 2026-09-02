package com.example.starborn.feature.crafting.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingScreen(
    craftingService: CraftingService,
    inventoryService: InventoryService,
    source: String?,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val inventoryState by inventoryService.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyCookedRecipeId by remember { mutableStateOf<String?>(null) }
    var selectedChef by remember { mutableStateOf("nova") }

    val recipes = remember { craftingService.cookingRecipes }
    val isCampfire = source?.contains("camp", ignoreCase = true) == true || source?.contains("fire", ignoreCase = true) == true
    val isThermalCooker = source?.contains("thermal", ignoreCase = true) == true || source?.contains("cooker", ignoreCase = true) == true
    val headerTitle = when {
        isThermalCooker -> "Thermal Cooker"
        isCampfire -> "Campfire Cooking"
        else -> "Kitchen Provisions"
    }
    val headerSubtitle = when {
        isThermalCooker -> "Convert raw ingredients and forage into field rations and nutritional preserves."
        isCampfire -> "Prepare hot trail meals over the open flame."
        else -> "Prepare nutritious field rations and restorative broths."
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121418)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isCampfire) {
                CookingEmberCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.TopCenter)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E232B))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCampfire) Icons.Default.OutdoorGrill else Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = Color(0xFFF6AD55),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF7FAFC)
                                )
                            )
                        }
                        Text(
                            text = headerSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFA0AEC0)
                            )
                        )
                    }
                }

                // Companion Head Chef Selection
                val chefs = listOf(
                    Triple("nova", "Nova", "⚡ +10 Focus"),
                    Triple("zeke", "Zeke", "🛡️ +25 HP / +3 Stab"),
                    Triple("gh0st", "Gh0st", "💨 +5 Spd / +20% Res"),
                    Triple("orion", "Orion", "🎯 +8% Crit")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chefs.forEach { (id, name, perk) ->
                        val isSelected = selectedChef == id
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedChef = id },
                            color = if (isSelected) Color(0xFFED8936).copy(alpha = 0.25f) else Color(0xFF1A202C),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFED8936) else Color(0xFF2D3748)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFE2E8F0),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = perk,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color(0xFFFBD38D) else Color(0xFF718096),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Recipe List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        val canCook = craftingService.canCook(recipe)
                        val isRecentlyCooked = recentlyCookedRecipeId == recipe.id

                        val cardScale = remember(isRecentlyCooked) { Animatable(if (isRecentlyCooked) 1.04f else 1f) }
                        LaunchedEffect(isRecentlyCooked) {
                            if (isRecentlyCooked) {
                                cardScale.snapTo(1.04f)
                                cardScale.animateTo(1f, tween(durationMillis = 350, easing = EaseOutBack))
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = cardScale.value
                                    scaleY = cardScale.value
                                }
                                .border(
                                    width = if (isRecentlyCooked) 1.5.dp else 1.dp,
                                    color = when {
                                        isRecentlyCooked -> Color(0xFFFFD54F)
                                        canCook -> Color(0xFFED8936).copy(alpha = 0.6f)
                                        else -> Color(0xFF2D3748)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRecentlyCooked) Color(0xFF24221E) else Color(0xFF1A202C)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Title, Icon & Yield
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(previewItemIconRes(recipe.result)),
                                            contentDescription = recipe.name,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = recipe.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF7FAFC)
                                            )
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFF2D3748),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Yield: ${recipe.resultQuantity}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFCBD5E0)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Description
                                recipe.description?.let { desc ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFA0AEC0)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Ingredients required
                                Text(
                                    text = "Ingredients",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF718096),
                                        fontSize = 11.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    recipe.ingredients.forEach { (ingredientId, count) ->
                                        val currentQty = inventoryState.find {
                                            it.item.id.equals(ingredientId, ignoreCase = true) ||
                                                it.item.name.equals(ingredientId, ignoreCase = true) ||
                                                it.item.aliases.any { alias -> alias.equals(ingredientId, ignoreCase = true) }
                                        }?.quantity ?: 0
                                        val hasEnough = currentQty >= count
                                        val formattedName = ingredientId.replace("_", " ").replaceFirstChar { it.uppercase() }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (hasEnough) Color(0xFF22543D).copy(alpha = 0.6f) else Color(0xFF2D3748),
                                            border = if (hasEnough) BorderStroke(1.dp, Color(0xFF48BB78).copy(alpha = 0.5f)) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (hasEnough) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = if (hasEnough) Color(0xFF48BB78) else Color(0xFFE53E3E),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$formattedName $currentQty/$count",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (hasEnough) Color(0xFFE2E8F0) else Color(0xFFA0AEC0)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                var selectedBatch by remember(recipe.id) { mutableStateOf(1) }
                                val canCookCurrentBatch = craftingService.canCook(recipe, selectedBatch)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Portions:",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0AEC0))
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(1, 2, 3, 5).forEach { qty ->
                                            val isSelected = selectedBatch == qty
                                            val canCookQty = craftingService.canCook(recipe, qty)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) Color(0xFFED8936) else Color(0xFF2D3748),
                                                modifier = Modifier.clickable(enabled = canCookQty) { selectedBatch = qty }
                                            ) {
                                                Text(
                                                    text = "x$qty",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!canCookQty) Color(0xFF718096) else if (isSelected) Color.Black else Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Cook Button
                                Button(
                                    onClick = {
                                        val outcome = craftingService.cookMeal(recipe.id, chefId = selectedChef, batch = selectedBatch)
                                        scope.launch {
                                            when (outcome) {
                                                is CraftingOutcome.Success -> {
                                                    onPlayAudio("sfx_cooking_sizzle")
                                                    recentlyCookedRecipeId = recipe.id
                                                    snackbarHostState.showSnackbar("🍲 ${outcome.message}")
                                                    delay(1200)
                                                    if (recentlyCookedRecipeId == recipe.id) {
                                                        recentlyCookedRecipeId = null
                                                    }
                                                }
                                                is CraftingOutcome.Failure -> {
                                                    snackbarHostState.showSnackbar("❌ ${outcome.message}")
                                                }
                                            }
                                        }
                                    },
                                    enabled = canCookCurrentBatch,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDD6B20),
                                        disabledContainerColor = Color(0xFF2D3748)
                                    )
                                ) {
                                    Text(
                                        text = if (canCookCurrentBatch) "Cook ${recipe.name} (x$selectedBatch)" else "Missing Ingredients",
                                        fontWeight = FontWeight.Bold,
                                        color = if (canCookCurrentBatch) Color.White else Color(0xFF718096)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookingEmberCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 10
) {
    val transition = rememberInfiniteTransition(label = "campfire_embers")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ember_time"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        for (i in 0 until particleCount) {
            val phase = (time + i.toFloat() / particleCount) % 1f
            val x = (sin(phase * PI * 2.0 + i * 1.5).toFloat() * 0.4f + 0.5f) * width
            val y = (1f - phase) * height
            val alpha = (sin(phase * PI).toFloat()).coerceIn(0f, 1f)
            val radius = 2.5.dp.toPx() * (1f - phase * 0.5f)
            drawCircle(
                color = if (i % 2 == 0) Color(0xFFFF9800).copy(alpha = alpha * 0.45f) else Color(0xFFFFD54F).copy(alpha = alpha * 0.6f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}
