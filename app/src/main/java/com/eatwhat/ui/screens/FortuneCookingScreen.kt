package com.eatwhat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatwhat.ui.theme.NeoBlack
import com.eatwhat.data.model.*
import com.eatwhat.ui.components.*
import com.eatwhat.ui.viewmodel.FortuneCookingViewModel
import com.eatwhat.ui.viewmodel.FortuneType

@Composable
fun FortuneCookingScreen(
    viewModel: FortuneCookingViewModel,
    onBack: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val selectedType by viewModel.selectedType
    val zodiac by viewModel.zodiac
    val luckyNumber by viewModel.luckyNumber
    val fortuneResult by viewModel.fortuneResult
    val isLoading by viewModel.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15)) // Yellow-400
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeoHeader(
            title = "料理占卜",
            subtitle = "让星星告诉你今天该吃什么",
            backgroundColor = Color(0xFFFDA4AF), // Soft Pink
            onBack = onBack,
            heroEmoji = "🔮"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // Step 1: Select Fortune Type
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            shadowOffset = 4.dp,
            borderWidth = 3
        ) {
            Text("1. 选择占卜类型", fontWeight = FontWeight.Black, color = Color(0xFFFDA4AF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FortuneType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoClickable { viewModel.selectType(type) },
                        color = if (isSelected) Color(0xFFFDA4AF) else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = rowBorder(3.dp, NeoBlack) // Thicker border
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(type.icon, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                             Column {
                                 Text(type.label, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else NeoBlack)
                                 Text(type.description, fontSize = 12.sp, color = if (isSelected) Color.White.copy(alpha = 0.9f) else NeoBlack.copy(alpha = 0.7f))
                             }
                        }
                    }
                }
            }
        }



        // Step 2: Params
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            shadowOffset = 4.dp,
            borderWidth = 3
        ) {
            Text("2. 配置占卜参数", fontWeight = FontWeight.Black, color = Color(0xFFFDA4AF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedType) {
                FortuneType.DAILY -> {
                    Text("选择你的星座", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConfigData.zodiacConfigs.chunked(6).forEach { rowZodiacs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowZodiacs.forEach { z ->
                                    val isSelected = zodiac == z.id
                                    Surface(
                                        modifier = Modifier
                                            .requiredSize(50.dp)
                                            .neoClickable { viewModel.setZodiac(z.id) },
                                        color = if (isSelected) Color(0xFFFDA4AF) else Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = rowBorder(2.dp, NeoBlack) // Thicker border
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Spacer(modifier = Modifier.height(2.dp)) // Move icon down 2dp
                                                Text(z.symbol, fontSize = 21.sp)
                                                Text(z.name, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else NeoBlack, modifier = Modifier.offset(y = (-2).dp)) // Text up 2dp
                                            }
                                        }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("选择你的生肖", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConfigData.animalConfigs.chunked(6).forEach { rowAnimals ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowAnimals.forEach { a ->
                                    val isSelected = viewModel.animal.value == a.id
                                    Surface(
                                        modifier = Modifier
                                            .requiredSize(50.dp)
                                            .neoClickable { viewModel.setAnimal(a.id) },
                                        color = if (isSelected) Color(0xFF10B981) else Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = rowBorder(2.dp, NeoBlack) // Thicker border
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Spacer(modifier = Modifier.height(2.dp)) // Move icon down 2dp
                                                Text(a.symbol, fontSize = 21.sp)
                                                Text(a.name, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else NeoBlack, modifier = Modifier.offset(y = (-2).dp)) // Text up 2dp
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
                FortuneType.MOOD -> {
                    Text("选择你的心情", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigData.moodConfigs.chunked(4).forEach { rowMoods ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                            ) {
                                rowMoods.forEach { m ->
                                    val isSelected = viewModel.selectedMoods.contains(m.id)
                                    Surface(
                                        modifier = Modifier
                                            .requiredSize(60.dp)
                                            .neoClickable { viewModel.toggleMood(m.id) },
                                        color = if (isSelected) Color(0xFFEC4899) else Color.White,
                                        shape = RoundedCornerShape(10.dp),
                                        border = rowBorder(2.dp, NeoBlack) // Thicker border
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(m.emoji, fontSize = 22.sp)
                                            Text(m.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else NeoBlack)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FortuneType.NUMBER -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(luckyNumber.toString(), fontSize = 48.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        NeoButton(text = "🎲 随机", onClick = { viewModel.generateRandomNumber() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            viewModel.errorMessage.value?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            NeoButton(
                text = if (isLoading) "占卜中..." else "开始占卜",
                onClick = { viewModel.startFortune() },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFDA4AF),
                shadowOffset = 4.dp, // Reduced to match homepage standard
                enabled = !isLoading && viewModel.isInputValid()
            )
        }

        // Result
        fortuneResult?.let { fortune ->
            LaunchedEffect(fortune.id) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            // Removed extra 24.dp spacer - parent Column Arrangement.spacedBy(16.dp) handles it
            val isFavorite = viewModel.isFavorite(fortune.id)
            val context = androidx.compose.ui.platform.LocalContext.current
            FortuneCard(
                fortune = fortune,
                isFavorite = isFavorite,
                isGenerating = viewModel.isGeneratingImage.value,
                onFavoriteClick = { viewModel.toggleFavorite(fortune) },
                onGenerateImage = {
                    val recipe = Recipe(
                        id = fortune.id,
                        name = fortune.dishName,
                        cuisine = when(fortune.type) {
                            "daily" -> "今日占卜"
                            "mood" -> "心情占卜"
                            "number" -> "数字占卜"
                            else -> "神秘占卜"
                        },
                        ingredients = fortune.ingredients ?: emptyList(),
                        steps = fortune.steps?.mapIndexed { index, s -> RecipeStep(index + 1, s) } ?: emptyList(),
                        cookingTime = fortune.cookingTime,
                        difficulty = fortune.difficulty,
                        tips = fortune.tips
                    )
                    android.widget.Toast.makeText(context, "🪄 正在为 '${fortune.dishName}' 创作图鉴...", android.widget.Toast.LENGTH_LONG).show()
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

@Composable
fun FortuneCard(
    fortune: FortuneResult,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onGenerateImage: () -> Unit = {},
    isGenerating: Boolean = false
) {
    NeoCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔮 占卜结果", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(fortune.dishName, fontWeight = FontWeight.Black, fontSize = 26.sp, color = Color(0xFFFDA4AF))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fortune.mysticalMessage, textAlign = TextAlign.Center, fontSize = 15.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("幸运指数", fontSize = 12.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                            Text("${fortune.luckyIndex}/10", fontWeight = FontWeight.Black, color = Color(0xFFDB2777))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("推荐理由", fontSize = 12.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                            Text(fortune.reason, fontWeight = FontWeight.Black, fontSize = 13.sp, color = NeoBlack)
                        }
                    }
                }

                Row(modifier = Modifier.align(Alignment.TopEnd)) {
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
            
            // Ingredients and Steps
            if (!fortune.ingredients.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("🥣 烹饪食材", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeoBlack)
                Spacer(modifier = Modifier.height(8.dp))
                fortune.ingredients.forEach { ing ->
                    Text("• $ing", fontSize = 14.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                }
            }

            if (!fortune.steps.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("📝 制作步骤", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeoBlack)
                Spacer(modifier = Modifier.height(8.dp))
                fortune.steps.forEachIndexed { index, step ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            modifier = Modifier.size(20.dp).background(NeoBlack, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((index + 1).toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(step, fontSize = 13.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun rowBorder(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
