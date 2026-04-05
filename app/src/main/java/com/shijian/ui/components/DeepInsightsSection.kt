package com.shijian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.data.model.NutritionAnalysis
import com.shijian.data.model.WinePairing
import com.shijian.ui.theme.NeoBlack

@Composable
fun DeepInsightsSection(
    nutrition: NutritionAnalysis?,
    pairing: WinePairing?,
    isLoading: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        if (nutrition == null && pairing == null) {
            NeoButton(
                text = if (isLoading) "🔎 AI 正在为您深度分析这道美味..." else "🔎 查看营养看板与灵感配饮",
                onClick = onUnlock,
                enabled = !isLoading,
                backgroundColor = Color(0xFF818CF8), // Indigo-400
                shadowOffset = 4.dp,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).border(2.dp, NeoBlack, RoundedCornerShape(4.dp)),
                    color = Color(0xFF818CF8),
                    trackColor = Color.White
                )
            }
        } else {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🧠", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("深度全案解析", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeoBlack)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nutrition Grid
            nutrition?.let { analysis ->
                Text(
                    text = "📊 营养看板 (每${analysis.servingSize})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeoBlack.copy(0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Health Score Badge
                NeoCard(
                    backgroundColor = getHealthColor(analysis.healthScore),
                    borderWidth = 2,
                    shadowOffset = 0.dp,
                    padding = 8.dp,
                    fullWidth = false
                ) {
                    Text(
                        text = "健康评分: ${analysis.healthScore}/10",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = NeoBlack
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Nutrition Grid (2 columns)
                val items = listOf(
                    "热量" to "${analysis.nutrition.calories}kcal",
                    "蛋白质" to "${analysis.nutrition.protein}g",
                    "碳水" to "${analysis.nutrition.carbs}g",
                    "脂肪" to "${analysis.nutrition.fat}g",
                    "纤维" to "${analysis.nutrition.fiber}g",
                    "钠" to "${analysis.nutrition.sodium}mg"
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in items.indices step 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutritionItem(items[i].first, items[i].second, Modifier.weight(1f))
                            if (i + 1 < items.size) {
                                NutritionItem(items[i+1].first, items[i+1].second, Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Advice Tags
                FlowRow(spacing = 6.dp) {
                    analysis.dietaryTags.forEach { tag ->
                        Surface(
                            color = Color(0xFFFDE68A), // Amber-200
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeoBlack),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "# $tag",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack
                            )
                        }
                    }
                }
            }
            
            // Drink Pairing
            pairing?.let { drink ->
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(thickness = 2.dp, color = NeoBlack.copy(0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "🥤 灵感饮品搭配",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeoBlack.copy(0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                NeoCard(
                    backgroundColor = Color(0xFFECFDF5), // Emerald-50
                    borderWidth = 2,
                    shadowOffset = 4.dp,
                    padding = 12.dp
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = drink.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeoBlack
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF34D399),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = drink.servingTemperature,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = drink.reason,
                            fontSize = 13.sp,
                            color = NeoBlack.copy(0.8f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 口感: ${drink.flavor}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, NeoBlack.copy(0.1f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = NeoBlack.copy(0.6f))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = NeoBlack)
    }
}

private fun getHealthColor(score: Int): Color {
    return when {
        score >= 8 -> Color(0xFFBBF7D0) // Green-200
        score >= 6 -> Color(0xFFFEF08A) // Yellow-200
        else -> Color(0xFFFECACA) // Red-200
    }
}
