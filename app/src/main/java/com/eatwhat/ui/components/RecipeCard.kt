package com.eatwhat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatwhat.data.model.Recipe
import com.eatwhat.data.model.RecipeStep

@Composable
fun RecipeCard(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isGenerating: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onGenerateImage: () -> Unit = {},
    isAnalyzingDeepInsights: Boolean = false,
    onUnlockDeepInsights: () -> Unit = {},
    shadowOffset: androidx.compose.ui.unit.Dp = 6.dp,
    isFlat: Boolean = false,
    isContentScrollable: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    @Composable
    fun ColumnScope.CardContent() {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF472B6))
                .border(3.dp, NeoBlack)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.name,
                        color = NeoBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NeoBlack,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "👨‍🍳 ${recipe.cuisine}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⏱️ ${recipe.cookingTime} min",
                            color = NeoBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Generate Image Button
                    IconButton(
                        onClick = onGenerateImage,
                        enabled = !isGenerating,
                        modifier = Modifier
                            .background(if (isGenerating) Color(0xFF67E8F9) else Color(0xFF22D3EE), RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .size(40.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeoBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("🪄", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .size(40.dp)
                    ) {
                        Text(if (isFavorite) "❤️" else "🤍", fontSize = 20.sp)
                    }
                }
            }
        }

        // Content
        val internalScrollState = rememberScrollState()
        
        // Auto-scroll when insights updated (for Overlay/Dialog usage)
        LaunchedEffect(recipe.nutritionAnalysis != null, recipe.winePairing != null) {
            if (isContentScrollable && (recipe.nutritionAnalysis != null || recipe.winePairing != null)) {
                // Wait for content layout update
                kotlinx.coroutines.delay(100)
                internalScrollState.animateScrollTo(internalScrollState.maxValue)
            }
        }

        val contentModifier = if (isContentScrollable) {
            Modifier.weight(1f, fill = false).verticalScroll(internalScrollState)
        } else Modifier

        Column(modifier = Modifier.padding(16.dp).then(contentModifier)) {
            // Ingredients
            Text(text = "🥬 所需食材", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                spacing = 4.dp
            ) {
                recipe.ingredients.forEach { ingredient ->
                    Surface(
                        color = Color(0xFFFACC15), // Yellow-400
                        border = rowBorder(1.dp, NeoBlack),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = ingredient,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Steps Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📝 制作步骤", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Steps Content
            val displaySteps = if (isExpanded) recipe.steps else recipe.steps.take(3)
            displaySteps.forEach { step ->
                RecipeStepItem(step = step)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (recipe.steps.size > 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoClickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isExpanded) "收起步骤" else "还有 ${recipe.steps.size - 3} 个步骤，查看全部",
                            color = if (isExpanded) Color.Gray else Color(0xFFF97316),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "▲" else "▼",
                            fontSize = 10.sp,
                            color = if (isExpanded) Color.Gray else Color(0xFFF97316)
                        )
                    }
                }
            }

            // Deep Insights Section
            if (!recipe.isSauce) {
                DeepInsightsSection(
                    nutrition = recipe.nutritionAnalysis,
                    pairing = recipe.winePairing,
                    isLoading = isAnalyzingDeepInsights,
                    onUnlock = onUnlockDeepInsights
                )
            }
        }
    }

    if (isFlat) {
        Column(modifier = modifier.fillMaxWidth()) {
            CardContent()
        }
    } else {
        NeoCard(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            padding = 0.dp,
            shadowOffset = shadowOffset
        ) {
            CardContent()
        }
    }
}

@Composable
fun RecipeStepItem(step: RecipeStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(NeoBlack, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step.step.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(text = step.description, fontSize = 13.sp, color = Color(0xFF374151))
    }
}


private fun rowBorder(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
