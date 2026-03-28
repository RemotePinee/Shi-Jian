package com.eatwhat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eatwhat.data.model.Recipe
import com.eatwhat.ui.theme.NeoBlack
import com.eatwhat.ui.components.*
import com.eatwhat.ui.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel) {
    val favorites = viewModel.filterFavorites()
    val searchQuery by viewModel.searchQuery
    val selectedRecipe = remember { mutableStateOf<Recipe?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15)) // Yellow-400
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NeoHeader(
            title = "美味回忆",
            subtitle = "共珍藏了 ${favorites.size} 道惊艳食谱",
            backgroundColor = Color(0xFFF87171), // Back to Red
            heroEmoji = "❤️"
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (favorites.isEmpty() && searchQuery.isEmpty()) {
                NeoEmptyState(
                    title = "暂无珍藏",
                    subtitle = "食见 AI 等待为您记录下每一个美味瞬间。",
                    emoji = "🤍",
                    backgroundColor = Color(0xFFFFF1F2), // Even paler pink
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize()
                .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search with Shadow (Only show if there are favorites or active search)
                if (favorites.isNotEmpty() || searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRoundRect(
                                    color = NeoBlack,
                                    topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                                    size = size,
                                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                )
                            }
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearch(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(3.dp, NeoBlack, RoundedCornerShape(12.dp)), // Thicker border
                            placeholder = { Text("搜点想吃的...", color = NeoBlack.copy(alpha = 0.3f)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = NeoBlack
                            )
                        )
                    }
                }

                if (favorites.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .fadingEdge(top = 10.dp, bottom = 16.dp)
                    ) {
                        LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                    ) {
                        items(favorites, key = { it.recipe.id }) { fav ->
                            FavoriteCompactCard(
                                recipe = fav.recipe,
                                onClick = { selectedRecipe.value = fav.recipe },
                                onRemove = { viewModel.removeFavorite(fav.recipe.id) }
                            )
                        }
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("未找到相关菜谱", color = NeoBlack.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

    // Detail Dialog with Sticky Header
    if (selectedRecipe.value != null) {
        Dialog(onDismissRequest = { selectedRecipe.value = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = 680.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(3.dp, NeoBlack, RoundedCornerShape(16.dp)),
                    color = Color.White
                ) {
                    // WRAPPER: Scrollable Column to let focus bubble up
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        RecipeCard(
                            recipe = selectedRecipe.value!!,
                            isFavorite = true,
                            onFavoriteClick = { 
                                selectedRecipe.value?.id?.let { viewModel.removeFavorite(it) }
                                selectedRecipe.value = null
                            },
                            onGenerateImage = {
                                val recipe = selectedRecipe.value!!
                                android.widget.Toast.makeText(context, "🪄 正在为 '${recipe.name}' 创作图鉴...", android.widget.Toast.LENGTH_LONG).show()
                                viewModel.generateImage(recipe) { url ->
                                    if (url != null) {
                                        android.widget.Toast.makeText(context, "创作成功！已收录至 GALLERY", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "创作失败，请检查设置或重试", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isFlat = true,
                            isContentScrollable = false, // SIGNAL BUBBLING ENABLED: Bubbles up to Column
                            isGenerating = viewModel.isGeneratingImage.value,
                            isAnalyzingDeepInsights = viewModel.isAnalyzingDeepInsights.value,
                            onUnlockDeepInsights = {
                                viewModel.unlockDeepInsights(selectedRecipe.value!!) { updated ->
                                    selectedRecipe.value = updated
                                }
                            }
                        )
                    }
                }
        }
    }
    }
}

@Composable
fun FavoriteCompactCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val cardColor = remember(recipe.id) {
        listOf(
            Color(0xFFFBD2E0), // Pinkish
            Color(0xFFDCFCE7), // Green
            Color(0xFFFEF9C3), // Yellow
            Color(0xFFE0F2F1), // Teal
            Color(0xFFF3E8FF), // Purple
            Color(0xFFFFEDD5)  // Orange
        ).random()
    }

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .neoClickable { onClick() },
        backgroundColor = cardColor,
        padding = 0.dp,
        shadowOffset = 6.dp,
        cornerRadius = 12.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Type tags
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NeoBlack,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = recipe.cuisine,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    if (recipe.isSauce) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFACC15), // Yellow-400
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeoBlack)
                        ) {
                            Text(
                                text = "蘸料",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack
                            )
                        }
                    }
                }
                
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Text("🗑️", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = recipe.name,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = NeoBlack,
                lineHeight = 22.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp)) // Increased spacer to move arrow up from bottom edge

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp), // Add bottom cushion to lift arrow from card edge
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Perfect vertical centering
            ) {
                Text(
                    text = "⏱️ ${recipe.cookingTime}m",
                    fontSize = 12.sp, // Slightly bigger for readability
                    fontWeight = FontWeight.ExtraBold,
                    color = NeoBlack.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = NeoBlack,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp) // Consistent size
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("➔", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
