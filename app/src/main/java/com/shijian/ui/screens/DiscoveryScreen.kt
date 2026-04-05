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
        DiscoveryTool("随机盲盒", "不知道吃什么？让上天决定！", "🎲", "mystery", Color(0xFFFACC15)),
        DiscoveryTool("酱料设计", "定制你的专属灵魂蘸料", "🍯", "sauce", Color(0xFFF472B6)),
        DiscoveryTool("料理占卜", "看看今天的烹饪运势如何", "🔮", "fortune", Color(0xFFC084FC))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Deep Background Layer: Sticker Bombing
        FoodStickerBombing(modifier = Modifier.fillMaxSize())

        // Foreground Layer: Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            NeoHeader(
                title = "百宝箱",
                subtitle = "探索更多有趣的功能",
                backgroundColor = Color(0xFFA78BFA), // NeoViolet
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

            // Large Feature (Top)
            tools.getOrNull(0)?.let { tool ->
                ToolCardSophisticated(
                    id = "22-A",
                    tool = tool,
                    onClick = { onNavigate(tool.route) },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    backgroundColor = Color(0xFF3B82F6) // Electric Blue
                )
            }

            // Gap reduced (removed 16dp spacer)

            // Asymmetric Row (Middle)
            Row(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                tools.getOrNull(1)?.let { tool ->
                    ToolCardSophisticated(
                        id = "07-X",
                        tool = tool,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(1.1f).fillMaxHeight(),
                        backgroundColor = Color(0xFF22C55E) // Laser Green
                    )
                }

                tools.getOrNull(2)?.let { tool ->
                    ToolCardSophisticated(
                        id = "99-S",
                        tool = tool,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(0.9f).fillMaxHeight(),
                        backgroundColor = Color(0xFFF43F5E) // Hot Pink
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
        // Balanced Screen-Wide "Sticker Bomb" (approx 16-18 items, no clustering)
        
        // --- TOP ROW ---
        Text("🍕", fontSize = 80.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 10.dp, y = 20.dp).graphicsLayer { rotationZ = -15f; alpha = 0.12f })
        Text("🥨", fontSize = 65.sp, modifier = Modifier.align(Alignment.TopCenter).offset(x = (-30).dp, y = 30.dp).graphicsLayer { rotationZ = 10f; alpha = 0.08f })
        Text("🍩", fontSize = 75.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 15.dp).graphicsLayer { rotationZ = -20f; alpha = 0.1f })

        // --- UPPER-MIDDLE ---
        Text("🥐", fontSize = 70.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 120.dp, y = 160.dp).graphicsLayer { rotationZ = 45f; alpha = 0.08f })
        Text("🍔", fontSize = 95.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-100).dp, y = 200.dp).graphicsLayer { rotationZ = -10f; alpha = 0.11f })
        Text("🌮", fontSize = 85.sp, modifier = Modifier.align(Alignment.TopCenter).offset(x = 20.dp, y = 280.dp).graphicsLayer { rotationZ = 20f; alpha = 0.1f })

        // --- MIDDLE ---
        Text("🍳", fontSize = 80.sp, modifier = Modifier.align(Alignment.CenterStart).offset(x = 20.dp, y = (-20).dp).graphicsLayer { rotationZ = 35f; alpha = 0.09f })
        Text("🥓", fontSize = 75.sp, modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-30).dp, y = 40.dp).graphicsLayer { rotationZ = -20f; alpha = 0.11f })
        Text("🍰", fontSize = 85.sp, modifier = Modifier.align(Alignment.Center).offset(x = 0.dp, y = 100.dp).graphicsLayer { rotationZ = 15f; alpha = 0.1f })

        // --- LOWER-MIDDLE ---
        Text("🥞", fontSize = 90.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = 60.dp, y = (-320).dp).graphicsLayer { rotationZ = 5f; alpha = 0.14f })
        Text("🍣", fontSize = 80.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-80).dp, y = (-280).dp).graphicsLayer { rotationZ = -30f; alpha = 0.15f })
        Text("🍟", fontSize = 90.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = 0.dp, y = (-240).dp).graphicsLayer { rotationZ = 10f; alpha = 0.18f })

        // --- BOTTOM ROW ---
        Text("🌭", fontSize = 85.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = 10.dp, y = (-120).dp).graphicsLayer { rotationZ = -15f; alpha = 0.12f })
        Text("🌯", fontSize = 105.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = 5.dp, y = 15.dp).graphicsLayer { rotationZ = -10f; alpha = 0.18f })
        Text("🍜", fontSize = 100.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-30).dp, y = (-40).dp).graphicsLayer { rotationZ = 20f; alpha = 0.15f })
        Text("🍦", fontSize = 80.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-20).dp, y = (-100).dp).graphicsLayer { rotationZ = 15f; alpha = 0.1f })
        Text("🍗", fontSize = 90.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(x = 120.dp, y = (-10).dp).graphicsLayer { rotationZ = -10f; alpha = 0.13f })
    }
}

@Composable
private fun ToolCardSophisticated(
    id: String,
    tool: DiscoveryTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White
) {
    NeoCard(
        modifier = modifier.neoClickable(onClick = onClick),
        backgroundColor = backgroundColor,
        shadowOffset = 8.dp,
        cornerRadius = 20.dp,
        padding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Massive background icon (Hero)
            Text(
                text = tool.icon,
                fontSize = 110.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 20.dp, y = 10.dp)
                    .graphicsLayer { alpha = 0.5f }
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = tool.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color.White,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = tool.description,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Unique "Price Tag" Label
                Box(
                    modifier = Modifier
                        .background(Color.Yellow)
                        .border(2.dp, NeoBlack)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "DISC-ID:$id",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoBlack
                    )
                }
            }

            // Decorative Crosshair
            Text(
                "⊕",
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}
