package com.shijian.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.data.model.CookbookRecipe
import com.shijian.ui.components.NeoBlack
import com.shijian.ui.components.NeoCard
import com.shijian.ui.components.neoClickable
import com.shijian.ui.components.fadingEdge
import com.shijian.ui.viewmodel.CookbookViewModel

private val categoryColors = mapOf(
    "meat_dish" to Color(0xFFEF4444),
    "vegetable_dish" to Color(0xFF22C55E),
    "aquatic" to Color(0xFF3B82F6),
    "breakfast" to Color(0xFFFB923C),
    "staple" to Color(0xFFA78BFA),
    "semi-finished" to Color(0xFF64748B),
    "soup" to Color(0xFFEC4899),
    "drink" to Color(0xFF06B6D4),
    "condiment" to Color(0xFFF59E0B),
    "dessert" to Color(0xFFF472B6),
)

@Composable
fun CookbookScreen(
    viewModel: CookbookViewModel,
    onBack: () -> Unit,
    onRecipeClick: (CookbookRecipe) -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val isScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
        }
    }
    val isFilterVisible by remember {
        derivedStateOf {
            // 如果菜谱数量很少（<= 6），没必要折叠，直接常驻显示，避免动画打架
            if (recipes.size <= 6) return@derivedStateOf true
            
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 150
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15))
    ) {
        // --- PREMIUM BENTO HEADER (INTEGRATED) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
        ) {
            NeoCard(
                backgroundColor = Color(0xFF34D399),
                padding = 0.dp,
                shadowOffset = 6.dp,
                cornerRadius = 24.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Title (Always Visible)
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeoBlack)
                                    }
                                    Text("食谱百科", fontSize = 28.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                                }
                                Text("${totalCount}道经典菜谱", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeoBlack.copy(alpha = 0.6f), modifier = Modifier.padding(start = 48.dp))
                            }
                            Text("📖", fontSize = 48.sp, modifier = Modifier.graphicsLayer { rotationZ = -10f })
                        }
                    }

                    // Integrated Search Bar
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        CookbookSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = isFilterVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            // Integrated Category Grid
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .drawBehind {
                                        val strokeWidth = 2.dp.toPx()
                                        drawLine(
                                            color = NeoBlack,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                    .padding(16.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    maxItemsInEachRow = 4
                                ) {
                                    val allCategories = listOf(null to "全部") + categories.map { it.id to it.name }
                                    allCategories.take(9).forEach { (id, name) ->
                                        val isSelected = selectedCategory == id
                                        Box(
                                            modifier = Modifier
                                                .height(36.dp)
                                                .weight(1f)
                                                .background(if (isSelected) Color(0xFFFACC15) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                                .border(1.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                                                 .clickable { 
                                                     viewModel.onCategorySelected(id)
                                                     scope.launch { gridState.scrollToItem(0) }
                                                 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(Color(0xFF1F2937)) // Dark Premium Bar
                                    .drawBehind {
                                        val strokeWidth = 2.dp.toPx()
                                        drawLine(
                                            color = NeoBlack,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val diffs = listOf(null to "全部", 1 to "★", 2 to "★★", 3 to "★★★", 4 to "★★★★", 5 to "★★★★★")
                                diffs.forEach { (level, label) ->
                                    val isSelected = selectedDifficulty == level
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFFACC15) else Color.Transparent)
                                            .clickable { 
                                                 viewModel.onDifficultySelected(level)
                                                 scope.launch { gridState.scrollToItem(0) }
                                             }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = if (level == null) 13.sp else 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) NeoBlack else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Recipe Grid (Scrolling Area) ---
        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(top = if (isScrolled) 10.dp else 0.dp, bottom = 16.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    CookbookRecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
                }
            }

            // --- BACK TO TOP BUTTON ---
            CookbookBackToTopButton(
                visible = !isFilterVisible,
                onClick = {
                    scope.launch {
                        // 复合滚动策略：如果离顶部太远，先瞬移到近处再平滑滚动，消除计算卡顿
                        if (gridState.firstVisibleItemIndex > 10) {
                            gridState.scrollToItem(10)
                        }
                        gridState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun CookbookBackToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    
    // 微小浮动动画，增加灵动感
    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Box(
        modifier = modifier
            .padding(bottom = 32.dp, end = 36.dp)
            .graphicsLayer { translationY = translateY }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "scale"
            )

            // 增加容器尺寸到 72dp，为 5dp 的阴影偏移留出足够空间，防止裁剪
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { 
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                try { awaitRelease() } finally { isPressed = false }
                            },
                            onTap = { onClick() }
                        )
                    }
            ) {
                // 1. 底层阴影：预留偏移量，且不超出 72dp 容器
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(x = 5.dp, y = 5.dp)
                        .background(NeoBlack, RoundedCornerShape(20.dp))
                )
                
                // 2. 主体按钮：尺寸 64dp
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF34D399), RoundedCornerShape(20.dp))
                        .border(2.5.dp, NeoBlack, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowUp,
                            contentDescription = "回到顶部",
                            tint = NeoBlack,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "TOP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun CookbookSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 5.dp, bottom = 5.dp)
            .drawBehind {
                drawRoundRect(
                    color = NeoBlack,
                    topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(2.5.dp, NeoBlack, shape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = NeoBlack.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "搜索菜名或食材…",
                        color = NeoBlack.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = NeoBlack,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(NeoBlack),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = NeoBlack)
                }
            }
        }
    }
}

@Composable
private fun CookbookRecipeCard(
    recipe: CookbookRecipe,
    onClick: () -> Unit
) {
    val catColor = categoryColors[recipe.category] ?: Color(0xFF3B82F6)

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .neoClickable(onClick = onClick),
        backgroundColor = Color.White,
        shadowOffset = 6.dp,
        cornerRadius = 20.dp,
        padding = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(catColor.copy(alpha = 0.1f))
                    .drawBehind {
                        // Subtle grid pattern
                        val step = 15.dp.toPx()
                        for (i in 0..(size.width / step).toInt()) {
                            drawLine(catColor.copy(alpha = 0.05f), Offset(i * step, 0f), Offset(i * step, size.height))
                        }
                        for (i in 0..(size.height / step).toInt()) {
                            drawLine(catColor.copy(alpha = 0.05f), Offset(0f, i * step), Offset(size.width, i * step))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(recipe.category),
                    fontSize = 52.sp,
                    modifier = Modifier.graphicsLayer { rotationZ = -5f }
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Yellow, RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoBlack, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "★".repeat(recipe.difficulty),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoBlack
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recipe.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NeoBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(catColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recipe.categoryName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${recipe.ingredients.size} 食材",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack.copy(alpha = 0.4f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = recipe.ingredients.take(3).joinToString("·"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeoBlack.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
