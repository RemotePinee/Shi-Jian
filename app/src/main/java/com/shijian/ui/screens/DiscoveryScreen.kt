package com.shijian.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.ui.components.NeoCard
import com.shijian.ui.components.NeoHeader
import com.shijian.ui.components.neoClickable
import com.shijian.ui.components.NeoBlack
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds

data class DiscoveryTool(
    val name: String,
    val description: String,
    val icon: String,
    val route: String,
    val color: Color
)

@Composable
fun DiscoveryScreen(
    onNavigate: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val tools = listOf(
        DiscoveryTool("烹饪教程", "从入门到精通的厨艺百科", "🎓", "tips", Color(0xFF818CF8)), // Indigo
        DiscoveryTool("随机盲盒", "不知道吃什么？让上天决定！", "🎲", "mystery", Color(0xFF3B82F6)), // Blue
        DiscoveryTool("食谱百科", "357道经典菜谱，随时翻阅", "📖", "cookbook", Color(0xFF34D399)), // Green
        DiscoveryTool("酱料设计", "定制你的专属灵魂蘸料", "🍯", "sauce", Color(0xFFFB923C)), // Orange
        DiscoveryTool("料理占卜", "看看今天的烹饪运势如何", "🔮", "fortune", Color(0xFFF472B6)) // Pink
    )

    Box(modifier = Modifier.fillMaxSize()) {
        FoodStickerBombing(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NeoHeader(
                title = "百宝箱",
                subtitle = "探索更多有趣的功能",
                backgroundColor = Color(0xFF818CF8),
                heroEmoji = "🧰",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = NeoBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            // Dynamic Bento Grid Layout
            Row(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Hero Column (Left)
                tools.getOrNull(0)?.let { tool ->
                    ToolCardSophisticated(
                        id = "HERO-01",
                        tool = tool,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        iconSize = 140.sp
                    )
                }

                // Stacked Column (Right)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    tools.getOrNull(1)?.let { tool ->
                        ToolCardSophisticated(
                            id = "BOX-02",
                            tool = tool,
                            onClick = { onNavigate(tool.route) },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            iconSize = 80.sp
                        )
                    }
                    tools.getOrNull(2)?.let { tool ->
                        ToolCardSophisticated(
                            id = "BOOK-03",
                            tool = tool,
                            onClick = { onNavigate(tool.route) },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            iconSize = 80.sp
                        )
                    }
                }
            }

            // Bottom Row: Symmetric Balanced pair
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                tools.getOrNull(3)?.let { tool ->
                    ToolCardSophisticated(
                        id = "DIP-04",
                        tool = tool,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        iconSize = 90.sp
                    )
                }

                tools.getOrNull(4)?.let { tool ->
                    ToolCardSophisticated(
                        id = "DIV-05",
                        tool = tool,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        iconSize = 90.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FoodStickerBombing(modifier: Modifier = Modifier) {
    Box(modifier = modifier.clipToBounds()) {
        Text("🍕", fontSize = 80.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 10.dp, y = 20.dp).graphicsLayer { rotationZ = -15f; alpha = 0.05f })
        Text("🥨", fontSize = 65.sp, modifier = Modifier.align(Alignment.TopCenter).offset(x = (-30).dp, y = 30.dp).graphicsLayer { rotationZ = 10f; alpha = 0.04f })
        Text("🍩", fontSize = 75.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 15.dp).graphicsLayer { rotationZ = -20f; alpha = 0.04f })
        Text("🍔", fontSize = 95.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-100).dp, y = 200.dp).graphicsLayer { rotationZ = -10f; alpha = 0.05f })
        Text("🍟", fontSize = 90.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = 0.dp, y = (-240).dp).graphicsLayer { rotationZ = 10f; alpha = 0.06f })
        Text("🍜", fontSize = 100.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-30).dp, y = (-40).dp).graphicsLayer { rotationZ = 20f; alpha = 0.06f })
    }
}

@Composable
private fun ToolCardSophisticated(
    id: String,
    tool: DiscoveryTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.TextUnit = 110.sp
) {
    NeoCard(
        modifier = modifier.neoClickable(onClick = onClick),
        backgroundColor = tool.color,
        shadowOffset = 6.dp,
        cornerRadius = 24.dp,
        padding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Massive background icon (Hero)
            Text(
                text = tool.icon,
                fontSize = iconSize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 15.dp, y = 10.dp)
                    .graphicsLayer { alpha = 0.35f }
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = tool.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )
                Text(
                    text = tool.description,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 12.sp
                )
            }

            // Price Tag Label
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .background(Color.Yellow)
                    .border(1.5.dp, NeoBlack)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "ID:$id",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack
                )
            }

            Text(
                "⊕",
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}
