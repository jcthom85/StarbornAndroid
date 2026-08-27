package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryService
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingScreen(
    craftingService: CraftingService,
    inventoryService: InventoryService,
    source: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inventoryState by inventoryService.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val recipes = remember { craftingService.cookingRecipes }
    val isCampfire = source?.contains("camp", ignoreCase = true) == true || source?.contains("fire", ignoreCase = true) == true
    val headerTitle = if (isCampfire) "Campfire Cooking" else "Kitchen Provisions"
    val headerSubtitle = if (isCampfire) "Prepare hot trail meals over the open flame." else "Prepare nutritious field rations and restorative broths."

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121418)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            // Recipe List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    val canCook = craftingService.canCook(recipe)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (canCook) Color(0xFFED8936).copy(alpha = 0.5f) else Color(0xFF2D3748),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A202C)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Title & Yield
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = recipe.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF7FAFC)
                                    )
                                )
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
                                        border = if (hasEnough) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF48BB78).copy(alpha = 0.5f)) else null
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cook Button
                            Button(
                                onClick = {
                                    val outcome = craftingService.cookMeal(recipe.id)
                                    scope.launch {
                                        when (outcome) {
                                            is CraftingOutcome.Success -> {
                                                snackbarHostState.showSnackbar("?? ${outcome.message}")
                                            }
                                            is CraftingOutcome.Failure -> {
                                                snackbarHostState.showSnackbar("? ${outcome.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = canCook,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDD6B20),
                                    disabledContainerColor = Color(0xFF2D3748)
                                )
                            ) {
                                Text(
                                    text = if (canCook) "Cook ${recipe.name}" else "Missing Ingredients",
                                    fontWeight = FontWeight.Bold,
                                    color = if (canCook) Color.White else Color(0xFF718096)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
