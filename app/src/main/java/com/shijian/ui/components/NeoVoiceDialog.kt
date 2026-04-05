package com.shijian.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.window.Dialog

@Composable
@Suppress("SpellCheckingInspection")
fun NeoVoiceDialog(
    state: VoiceState,
    text: String,
    rmsdB: Float,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        NeoCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            backgroundColor = Color(0xFFFACC15), // Pure Yellow
            shadowOffset = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp), // More padding for chunkiness
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = when (state) {
                        VoiceState.LISTENING -> "正在倾听..."
                        VoiceState.PROCESSING -> "正在处理..."
                        VoiceState.SUCCESS -> "识别成功"
                        VoiceState.ERROR -> "哎呀，没听清"
                        else -> "准备就绪"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0A0910), // Hardcoded NeoBlack
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Waveform Animation Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(3.dp, Color(0xFF0A0910), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        VoiceState.LISTENING -> VoiceWaveform(rmsdB)
                        VoiceState.PROCESSING -> CircularProgressIndicator(color = Color(0xFF0A0910), strokeWidth = 4.dp)
                        else -> {
                            Text(
                                text = when {
                                    state == VoiceState.ERROR -> "请尝试再次说话"
                                    text.isEmpty() -> "点击麦克风开始说..."
                                    else -> text
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0A0910),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }


                NeoButton(
                    text = when (state) {
                        VoiceState.SUCCESS -> "完成添加"
                        VoiceState.PROCESSING -> "正在识别..."
                        VoiceState.ERROR -> "好的"
                        else -> "说完了"
                    },
                    onClick = onFinish,
                    enabled = state != VoiceState.PROCESSING,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFB923C) // Solid Orange
                )
            }
        }
    }
}

@Composable
@Suppress("SpellCheckingInspection")
fun VoiceWaveform(rmsdB: Float) {
    val barCount = 5
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 10f,
                targetValue = 60f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300 + i * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
            
            // Influence by audio volume
            val finalHeight = (animatedHeight + (rmsdB.coerceIn(0f, 10f) * 5)).dp
            
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(finalHeight)
                    .background(Color(0xFF0A0910), RoundedCornerShape(6.dp)) // Hardcoded NeoBlack
                    .border(2.dp, Color.White, RoundedCornerShape(6.dp))
            )
        }
    }
}
