package com.eatwhat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.eatwhat.R
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable

val NeoBlack = Color(0xFF0A0910)

/**
 * A custom clickable modifier that eliminates the default gray ripple/mask.
 * Optimized for maximum stability - No visual movement during press to avoid hit-testing issues.
 */
fun Modifier.neoClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        interactionSource = interactionSource,
        indication = null, // No ripple
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    backgroundColor: Color = Color.White,
    borderColor: Color = NeoBlack,
    borderWidth: Int = 3,
    padding: Dp = 16.dp,
    shadowOffset: Dp = 6.dp,
    cornerRadius: Dp = 12.dp,
    fullWidth: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset) // 新丑风核心：让布局包含阴影 (Include shadow in layout)
            .drawBehind {
                drawRoundRect(
                    color = NeoBlack,
                    topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
                .clip(shape)
                .background(backgroundColor)
                .border(borderWidth.dp, borderColor, shape)
                .padding(padding)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeoBlack,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFFB923C), // Theme Orange
    textColor: Color = NeoBlack, // Default to Black for contrast
    shadowOffset: Dp = 4.dp, // Standardized to 4.dp
    enabled: Boolean = true
) {
    val isWhite = backgroundColor == Color.White
    val currentTextColor = if (enabled) {
        if (isWhite) NeoBlack else textColor
    } else {
        if (isWhite) NeoBlack.copy(alpha = 0.5f) else textColor.copy(alpha = 0.7f)
    }
    
    val shape = RoundedCornerShape(8.dp)
    
    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset) // 新丑风核心：让布局包含阴影 (Include shadow in layout)
            .neoClickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        color = NeoBlack,
                        topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape)
                .border(2.dp, NeoBlack, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = currentTextColor,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NeoTag(
    text: String,
    onRemove: (() -> Unit)? = null,
    backgroundColor: Color = Color(0xFFFACC15), // Yellow-400
    textColor: Color = NeoBlack
) {
    Row(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(2.dp, NeoBlack, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✕",
                modifier = Modifier.neoClickable { onRemove() },
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
fun NeoHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backgroundColor: Color = Color(0xFFA78BFA), // NeoViolet
    onBack: (() -> Unit)? = null,
    heroEmoji: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = backgroundColor,
        padding = 16.dp,
        shadowOffset = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Hero Icon (Integrated into the background)
            if (heroEmoji != null) {
                Text(
                    text = heroEmoji,
                    fontSize = 32.sp, // Keep layout size small (same as title)
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 24.dp, y = 0.dp)
                        .graphicsLayer {
                            scaleX = 4.5f // Scale visually to match "Jumbo" feel
                            scaleY = 4.5f
                            alpha = 0.15f
                            rotationZ = -20f
                        }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = NeoBlack,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            lineHeight = 36.sp
                        )
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                if (actions != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                }
            }
        }
    }
}

@Composable
fun NeoNavigationBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    // Floating Capsule Design
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp) // Floating from edges
    ) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = backgroundColor,
            padding = 0.dp,
            shadowOffset = 6.dp, // Enhanced depth
            cornerRadius = 20.dp, // Softer capsule look
            fullWidth = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun RowScope.NeoNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedColor: Color = Color(0xFFA78BFA),
    isProminent: Boolean = false
) {
    if (isProminent) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .neoClickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Signature Square Button (Neo-Brutalist Mechanical Switch look)
            val boxSize = 52.dp
            val bgColor = if (selected) Color(0xFFFB923C) else Color(0xFFD4A574)
            
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .border(2.5.dp, NeoBlack, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ai_chef),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .neoClickable(onClick = onClick)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .then(
                        if (selected) {
                            Modifier
                                .background(selectedColor.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        } else {
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) NeoBlack else Color(0xFF94A3B8), // Cool Slate for unselected
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) NeoBlack else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
@Composable
fun NeoEmptyState(
    title: String,
    subtitle: String,
    emoji: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Large Editorial Icon Box
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { rotationZ = -5f }
                .drawBehind {
                    drawRoundRect(
                        color = NeoBlack,
                        topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .border(3.dp, NeoBlack, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 60.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Large Editorial Title
        Text(
            text = title,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = NeoBlack,
            lineHeight = 52.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Poetic Subtitle
        Text(
            text = subtitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeoBlack.copy(alpha = 0.7f),
            lineHeight = 26.sp,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(48.dp))
            action()
        }
    }
}
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeholders = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val spacingPx = spacing.roundToPx()
        
        var currentRowWidth = 0
        var currentRowHeight = 0
        var totalHeight = 0
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()

        placeholders.forEach { placeable ->
            if (currentRowWidth + placeable.width + spacingPx > constraints.maxWidth) {
                rows.add(currentRow)
                totalHeight += currentRowHeight + spacingPx
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
                currentRowHeight = placeable.height
            } else {
                currentRow.add(placeable)
                currentRowWidth += placeable.width + spacingPx
                currentRowHeight = maxOf(currentRowHeight, placeable.height)
            }
        }
        rows.add(currentRow)
        totalHeight += currentRowHeight

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                var maxHeight = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + spacingPx
                    maxHeight = maxOf(maxHeight, placeable.height)
                }
                y += maxHeight + spacingPx
            }
        }
    }
}
