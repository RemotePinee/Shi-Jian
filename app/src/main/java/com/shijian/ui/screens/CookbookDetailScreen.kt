package com.shijian.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.data.model.CookbookRecipe
import com.shijian.ui.components.NeoBlack
import com.shijian.ui.components.NeoButton
import com.shijian.ui.components.NeoCard
import com.shijian.ui.components.fadingEdge

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CookbookDetailScreen(
    recipe: CookbookRecipe?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAskAiImprovement: () -> Unit
) {
    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFACC15)), contentAlignment = Alignment.Center) {
            Text("食谱加载失败", fontWeight = FontWeight.Bold)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15))
            .padding(top = 4.dp) // Small breathing room instead of double status bar padding
    ) {
        // FIXED HERO CARD - Moved out of the list to stay fixed at top
        val headerColor = if (recipe.category == "meat_dish") Color(0xFFEF4444) else Color(0xFF34D399)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp) // Tightened bottom padding
        ) {
            NeoCard(
                backgroundColor = headerColor,
                padding = 0.dp,
                shadowOffset = 6.dp,
                cornerRadius = 20.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Top Row: Premium Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PremiumControlCircle(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                        PremiumControlCircle(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (isFavorite) Color.Red else NeoBlack,
                            onClick = onToggleFavorite
                        )
                    }

                    // Content Row
                    Row(
                        modifier = Modifier.padding(top = 52.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recipe.name,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoBlack,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .border(1.5.dp, NeoBlack, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(recipe.categoryName, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "★".repeat(recipe.difficulty),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        Text(
                            text = getCategoryEmoji(recipe.category),
                            fontSize = 64.sp,
                            modifier = Modifier.graphicsLayer { rotationZ = 8f }
                        )
                    }
                }
            }
        }

        val listState = rememberLazyListState()
        val showTopShadow by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }

        // SCROLLABLE CONTENT - Now below the fixed card
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fadingEdge(
                    top = if (showTopShadow) 16.dp else 0.dp, // Dynamic top shadow
                    bottom = 24.dp
                ),
            contentPadding = PaddingValues(top = 0.dp, bottom = 24.dp)
        ) {

            // Ingredients
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    NeoCard(
                        title = "原料清单",
                        backgroundColor = Color.White,
                        shadowOffset = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recipe.ingredients.forEach { IngredientTag(it) }
                            }
                            
                            if (recipe.portions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("详细配比", fontSize = 14.sp, fontWeight = FontWeight.Black, color = NeoBlack.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recipe.portions.forEach { portion ->
                                        PortionItem(portion)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Steps
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    NeoCard(
                        title = "烹饪步骤",
                        backgroundColor = Color.White,
                        shadowOffset = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            recipe.steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF3B82F6), CircleShape)
                                            .border(1.5.dp, NeoBlack, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Text(
                                        text = step.description,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 22.sp,
                                        color = NeoBlack
                                    )
                                }
                                
                                if (index < recipe.steps.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 36.dp),
                                        thickness = 1.dp,
                                        color = NeoBlack.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Optimization
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    NeoButton(
                        text = "用 AI 智能调优口味",
                        onClick = onAskAiImprovement,
                        backgroundColor = Color(0xFFA78BFA),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                    
                    if (recipe.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NeoCard(
                            title = "💡 温馨提示",
                            backgroundColor = Color(0xFFFEF3C7)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                recipe.tips.forEach { tip ->
                                    Text("• $tip", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
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
private fun PremiumControlCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = NeoBlack,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            // Double border effect for premium look
            .border(2.dp, NeoBlack, RoundedCornerShape(10.dp))
            .padding(2.dp)
            .border(0.5.dp, NeoBlack.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
 private fun PortionItem(text: String, modifier: Modifier = Modifier) {
    // Clean markdown artifacts like ![image](url) and ### headers
    val cleanedText = remember(text) {
        var result = text.replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // Remove images
            .replace(Regex("#{1,6}\\s?"), "") // Remove markdown headers
            .trim()
            
        // Auto-fix unbalanced parentheses if any were swallowed by markdown stripping
        val openCount = result.count { it == '(' }
        val closeCount = result.count { it == ')' }
        if (openCount > closeCount) {
            result += ")"
        }
        result
    }
    
    if (cleanedText.isEmpty()) return

    Surface(
        modifier = modifier,
        color = Color(0xFFF1F5F9), 
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NeoBlack.copy(alpha = 0.1f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Default.Scale, 
                null, 
                modifier = Modifier.size(12.dp), 
                tint = NeoBlack.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = cleanedText, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Bold,
                color = NeoBlack
            )
        }
    }
}

@Composable
private fun IngredientTag(name: String) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.5.dp, NeoBlack, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "meat_dish" -> "🥩"
        "vegetable_dish" -> "🥬"
        "aquatic" -> "🦀"
        "breakfast" -> "🍳"
        "staple" -> "🍜"
        "semi-finished" -> "🥟"
        "soup" -> "🍲"
        "drink" -> "🍹"
        "condiment" -> "🧂"
        "dessert" -> "🧁"
        else -> "🍽️"
    }
}
