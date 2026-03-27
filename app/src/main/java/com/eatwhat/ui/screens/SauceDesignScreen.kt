package com.eatwhat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.eatwhat.ui.theme.NeoBlack
import com.eatwhat.ui.components.*
import com.eatwhat.ui.viewmodel.SauceDesignViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SauceDesignScreen(
    viewModel: SauceDesignViewModel,
    onBack: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val spiceLevel by viewModel.spiceLevel
    val sweetLevel by viewModel.sweetLevel
    val saltLevel by viewModel.saltLevel
    val sourLevel by viewModel.sourLevel
    val selectedUseCases = viewModel.selectedUseCases
    val recommendations by viewModel.recommendations
    val currentSauce by viewModel.currentSauce
    val isLoadingRecs by viewModel.isLoadingRecommendations
    val isLoadingSauce by viewModel.isLoadingSauce
    val errorMessage by viewModel.errorMessage

    // --- Scroll Focus Management (Height-Observer Pattern) ---
    var lastRecSize by remember { mutableIntStateOf(recommendations.size) }
    var lastSauceName by remember { mutableStateOf(currentSauce?.name) }
    var autoScrollWindowUntil by remember { mutableLongStateOf(0L) }

    LaunchedEffect(recommendations.size, currentSauce?.name, isLoadingRecs, isLoadingSauce, errorMessage) {
        val hasNewRecs = recommendations.size > lastRecSize
        val hasNewSauce = currentSauce != null && currentSauce?.name != lastSauceName
        val hasNewError = errorMessage != null
        
        if (hasNewRecs || hasNewSauce || isLoadingRecs || isLoadingSauce || hasNewError) {
            autoScrollWindowUntil = System.currentTimeMillis() + 2500
        }
        
        lastRecSize = recommendations.size
        lastSauceName = currentSauce?.name
    }

    LaunchedEffect(scrollState.maxValue) {
        if (System.currentTimeMillis() < autoScrollWindowUntil) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15)) // Yellow-400
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NeoHeader(
            title = "酱料设计",
            subtitle = "调制出让你惊叹的灵魂酱汁",
            backgroundColor = Color(0xFF22C55E), // Discovery Green
            onBack = onBack,
            heroEmoji = "🍯"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Step 1: Intelligent Recommendation
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White
        ) {
            Text("1. 智能推荐", fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
            Spacer(modifier = Modifier.height(16.dp))

            // Sliders
            TasteSlider("🌶️ 辣度", spiceLevel, Color(0xFFEF4444)) { viewModel.updateSpice(it) }
            TasteSlider("🍯 甜度", sweetLevel, Color(0xFFEAB308)) { viewModel.updateSweet(it) }
            TasteSlider("🧂 咸度", saltLevel, Color(0xFF3B82F6)) { viewModel.updateSalt(it) }
            TasteSlider("🍋 酸度", sourLevel, Color(0xFF22C55E)) { viewModel.updateSour(it) }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("使用场景", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            val useCases = listOf(
                Pair("dip", "🥢 蘸料"), Pair("stir", "🍳 炒菜"),
                Pair("mix", "🍲 拌菜/拌面"), Pair("marinate", "🥩 腌制")
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                useCases.forEach { (id, name) ->
                    val isSelected = selectedUseCases.contains(id)
                    Surface(
                        modifier = Modifier
                            .neoClickable { viewModel.toggleUseCase(id) }
                            .padding(vertical = 4.dp),
                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF3F4F6),
                        border = rowBorder(1.dp, NeoBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else NeoBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            NeoButton(
                text = if (isLoadingRecs) "推荐中..." else "获取智能推荐",
                onClick = { viewModel.getRecommendations() },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF8B5CF6), // Purple-500
                shadowOffset = 4.dp
            )
        }

        // Step 2: Recommendations Result
        if (recommendations.isNotEmpty()) {
            // 预设的多彩色调 (Neo-Brutalist Pastels)
            val sauceCardColors = listOf(
                Color(0xFFF5F3FF), // Purple
                Color(0xFFFEF2F2), // Red/Pink
                Color(0xFFF0FDF4), // Green
                Color(0xFFFFFBEB), // Yellow/Amber
                Color(0xFFF0F9FF)  // Blue
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🥣 为您推荐", fontWeight = FontWeight.Black, color = NeoBlack, fontSize = 18.sp)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    recommendations.forEachIndexed { index, name ->
                        NeoCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(0.45f)
                                .neoClickable { viewModel.selectSauce(name) },
                            backgroundColor = sauceCardColors[index % sauceCardColors.size],
                            padding = 12.dp,
                            shadowOffset = 4.dp
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = NeoBlack,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        } else if (errorMessage != null && currentSauce == null) {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFEF2F2),
                shadowOffset = 4.dp
            ) {
                Text("推荐获取失败", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(errorMessage ?: "请检查 API 设置或网络连接", color = NeoBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Step 3: Sauce Details
        if (isLoadingSauce) {
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Text("AI大师正在准备酱料配方...", fontWeight = FontWeight.Black, color = NeoBlack)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).border(1.dp, NeoBlack, RoundedCornerShape(4.dp)), color = Color(0xFF8B5CF6), trackColor = Color.White)
            }
        } else if (errorMessage != null && currentSauce == null && recommendations.isNotEmpty()) {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFEF2F2),
                shadowOffset = 4.dp
            ) {
                Text("教程生成失败", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(errorMessage ?: "请检查 API 设置或稍后重试", color = NeoBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        currentSauce?.let { sauce ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📖 制作教程", fontWeight = FontWeight.Black, color = NeoBlack, fontSize = 18.sp)
                val isFavorite = viewModel.isFavorite(sauce.id)
                val context = androidx.compose.ui.platform.LocalContext.current
                SauceRecipeCard(
                    sauce = sauce,
                    isFavorite = isFavorite,
                    isGenerating = viewModel.isGeneratingImage.value,
                    onFavoriteClick = { viewModel.toggleFavorite(sauce) },
                    onGenerateImage = {
                        val recipe = com.eatwhat.data.model.Recipe(
                            id = sauce.id,
                            name = sauce.name,
                            cuisine = "香辣酱料",
                            ingredients = sauce.ingredients,
                            steps = sauce.steps.map { com.eatwhat.data.model.RecipeStep(it.step, it.description, it.time, it.temperature) },
                            cookingTime = sauce.makingTime,
                            difficulty = sauce.difficulty,
                            tips = sauce.tips
                        )
                        android.widget.Toast.makeText(context, "🪄 正在为 '${sauce.name}' 创作图鉴...", android.widget.Toast.LENGTH_LONG).show()
                        viewModel.generateImage(recipe) { url ->
                            if (url != null) {
                                android.widget.Toast.makeText(context, "创作成功！已收录至 GALLERY", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "创作失败，请检查设置或重试", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
fun TasteSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
            Text(value.toInt().toString(), fontWeight = FontWeight.Black, color = color)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun SauceRecipeCard(
    sauce: com.eatwhat.data.model.SauceRecipe,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onGenerateImage: () -> Unit = {},
    isGenerating: Boolean = false
) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(sauce.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeoBlack, modifier = Modifier.weight(1f))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Generate Image Button
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .neoClickable(enabled = !isGenerating) { onGenerateImage() },
                    color = if (isGenerating) Color(0xFF67E8F9) else Color(0xFF22D3EE),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NeoBlack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Favorite Button
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .neoClickable { onFavoriteClick() },
                    color = if (isFavorite) Color(0xFFFEE2E2) else Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NeoBlack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isFavorite) "❤️" else "🤍",
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(sauce.description ?: "美味的自制酱料配方", fontSize = 14.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("🥣 配料清单", fontWeight = FontWeight.Black, fontSize = 15.sp, color = NeoBlack)
        sauce.ingredients.forEach { ing ->
            Text("• $ing", fontSize = 14.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("📝 制作步骤", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        sauce.steps.forEachIndexed { index, sauceStep ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Box(
                    modifier = Modifier.size(20.dp).background(NeoBlack, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text((index + 1).toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(sauceStep.description, fontSize = 13.sp)
        }
    }
}
}

private fun rowBorder(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
