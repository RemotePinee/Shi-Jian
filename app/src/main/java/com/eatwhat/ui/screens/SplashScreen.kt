package com.eatwhat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatwhat.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SplashScreen() {
    
    // Progress for the loading bar
    val progress = remember { Animatable(0f) }
    
    // Scale and entry states
    val heroScale = remember { Animatable(1f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            progress.animateTo(1f, tween(3000, easing = LinearEasing))
        }
        launch {
            contentAlpha.animateTo(1f, tween(800))
        }
        // Subtle heart-beat or bounce
        while(true) {
            heroScale.animateTo(1.05f, tween(1000, easing = FastOutSlowInEasing))
            heroScale.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoYellow),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Background Grid (Neo-Brutalist signature)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            val strokeWidth = 1.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.05f),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = strokeWidth
                )
            }
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.05f),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = strokeWidth
                )
            }
        }


        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value; clip = false }
        ) {
            // LAYER 3: Hero Multi-Card Sticker (Now Vertically Centered)
            Box(
                modifier = Modifier
                    .size(320.dp) // Increased from 240dp to prevent clipping of rotated 200dp cards
                    .graphicsLayer { clip = false }
                    .scale(heroScale.value),
                contentAlignment = Alignment.Center
            ) {
                // Background Card (Violet) with Shadow
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer { clip = false } // Ensure shadow doesn't clip
                        .rotate(8f)
                        .drawBehind {
                            drawRoundRect(
                                color = NeoBlack,
                                topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(48.dp.toPx())
                            )
                        }
                        .background(NeoViolet, RoundedCornerShape(48.dp))
                        .border(4.dp, NeoBlack, RoundedCornerShape(48.dp))
                )
                // Main Card (White) - REVERTED TO NO SHADOW
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer { clip = false }
                        .rotate(-4f)
                        .background(Color.White, RoundedCornerShape(48.dp))
                        .border(6.dp, NeoBlack, RoundedCornerShape(48.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🍱",
                        fontSize = 120.sp
                    )
                }
            }
        }
    }
}
