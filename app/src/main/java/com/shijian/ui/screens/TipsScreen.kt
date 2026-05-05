package com.shijian.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.data.model.CookbookTip
import com.shijian.ui.components.NeoBlack
import com.shijian.ui.components.NeoCard
import com.shijian.ui.components.NeoHeader
import com.shijian.ui.components.neoClickable
import com.shijian.ui.viewmodel.CookbookViewModel
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.ClickableText

@Composable
fun TipsScreen(
    viewModel: CookbookViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val tips by viewModel.tips.collectAsState()
    var selectedTip by remember { mutableStateOf<CookbookTip?>(null) }

    // Intercept back gesture if a tip is selected
    BackHandler(enabled = selectedTip != null) {
        selectedTip = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFACC15))
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            NeoHeader(
                title = "烹饪教程",
                subtitle = "掌握从入门到精通的厨艺技巧",
                backgroundColor = Color(0xFF818CF8), // Indigo
                heroEmoji = "🎓",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeoCard(
                modifier = Modifier.weight(1f),
                backgroundColor = Color.White,
                padding = 0.dp,
                shadowOffset = 6.dp
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tips.size) { index ->
                        val tip = tips[index]
                        TipListItem(
                            tip = tip,
                            onClick = { selectedTip = tip },
                            showDivider = index < tips.size - 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Overlay Detail
        AnimatedContent(
            targetState = selectedTip,
            transitionSpec = {
                if (targetState != null) {
                    (slideInVertically { it } + fadeIn()).togetherWith(fadeOut())
                } else {
                    fadeIn().togetherWith(slideOutVertically { it } + fadeOut())
                }
            },
            label = "tip_detail_transition"
        ) { tip ->
            if (tip != null) {
                TipDetailOverlay(
                    tip = tip,
                    onClose = { selectedTip = null },
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun TipListItem(
    tip: CookbookTip,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neoClickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFEE99), RoundedCornerShape(6.dp))
                    .border(1.5.dp, NeoBlack, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = NeoBlack, modifier = Modifier.size(18.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NeoBlack
                )
                if (tip.category.isNotBlank() && tip.category != "root") {
                    Text(
                        text = tip.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack.copy(alpha = 0.4f)
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeoBlack, modifier = Modifier.size(20.dp))
        }
    }
    if (showDivider) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NeoBlack.copy(alpha = 0.1f)).padding(horizontal = 16.dp))
    }
}

@Suppress("DEPRECATION")
@Composable
private fun TipDetailOverlay(
    tip: CookbookTip,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFAEC)) // Warm cream
    ) {
        // Integrated Header with Yellow theme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFACC15))
                .statusBarsPadding()
                .drawBehind {
                    drawLine(
                        color = NeoBlack,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tip.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoBlack
                    )
                    Text(
                        text = "百科知识 · ${tip.category}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(NeoBlack, RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Data is now directly updated in recipes.json
            val transformedContent = tip.content

            val lines = transformedContent.split("\n")
            var inCodeBlock = false
            val currentCode = StringBuilder()
            val tableRows = mutableListOf<List<String>>()

            lines.forEach { line ->
                val trimmed = line.trim()
                
                // Table detection
                if (trimmed.startsWith("|")) {
                    val cells = trimmed.split("|").filter { it.isNotBlank() }.map { it.trim() }
                    if (cells.isNotEmpty() && !trimmed.contains("---")) {
                        tableRows.add(cells)
                    }
                    return@forEach
                } else if (tableRows.isNotEmpty()) {
                    val rows = tableRows.toList()
                    item { TipTable(rows) }
                    tableRows.clear()
                }

                if (trimmed.startsWith("```")) {
                    if (inCodeBlock) {
                        val code = currentCode.toString()
                        item { CodeBlock(code) }
                        currentCode.setLength(0)
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                } else if (inCodeBlock) {
                    currentCode.append(line).append("\n")
                } else if (trimmed.startsWith("###")) {
                    item {
                        Text(
                            text = trimmed.removePrefix("###").trim(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                } else if (trimmed.startsWith("##")) {
                    item {
                        Text(
                            text = trimmed.removePrefix("##").trim(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )
                    }
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    item {
                        Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                            Text("• ", fontWeight = FontWeight.Black, color = NeoBlack)
                            val annotated = renderMarkdown(trimmed.substring(2).trim())
                            ClickableText(
                                text = annotated,
                                style = LocalTextStyle.current.copy(fontSize = 15.sp, color = NeoBlack),
                                onClick = { offset ->
                                    annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            if (annotation.item.startsWith("app://")) {
                                                onNavigate(annotation.item.removePrefix("app://"))
                                            } else {
                                                uriHandler.openUri(annotation.item)
                                            }
                                        }
                                }
                            )
                        }
                    }
                } else if (trimmed.isNotEmpty()) {
                    item {
                        val annotated = renderMarkdown(line)
                        ClickableText(
                            text = annotated,
                            style = LocalTextStyle.current.copy(fontSize = 15.sp, lineHeight = 22.sp, color = NeoBlack.copy(alpha = 0.9f)),
                            modifier = Modifier.padding(vertical = 4.dp),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        if (annotation.item.startsWith("app://")) {
                                            onNavigate(annotation.item.removePrefix("app://"))
                                        } else {
                                            uriHandler.openUri(annotation.item)
                                        }
                                    }
                            }
                        )
                    }
                } else {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
            
            if (tableRows.isNotEmpty()) {
                item { TipTable(tableRows) }
            }
        }
        
        // Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFACC15))
                .drawBehind {
                    drawLine(
                        color = NeoBlack,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                .navigationBarsPadding()
                .height(4.dp)
        )
    }
}

@Composable
private fun TipTable(rows: List<List<String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        rows.forEachIndexed { index, cells ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min) // MUST BE ON ROW: Syncs all cells to the tallest one
                    .background(if (index == 0) Color(0xFFFACC15) else Color.White)
                    .drawBehind {
                        if (index < rows.size - 1) {
                            drawLine(
                                color = NeoBlack,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
            ) {
                cells.forEachIndexed { cellIndex, cell ->
                    val weight = when(cellIndex) {
                        0 -> 1.2f
                        1 -> 1.0f
                        2 -> 1.2f
                        else -> 3.5f
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .drawBehind { // DRAW FIRST: To ensure lines touch the very edges
                                if (cellIndex < cells.size - 1) {
                                    drawLine(
                                        color = NeoBlack,
                                        start = Offset(size.width, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp), // PADDING SECOND: To protect text content
                        contentAlignment = if (cellIndex < 3) Alignment.Center else Alignment.TopStart
                    ) {
                        Text(
                            text = renderMarkdown(cell),
                            fontSize = 12.sp,
                            fontWeight = if (index == 0) FontWeight.Black else FontWeight.Bold,
                            color = NeoBlack,
                            lineHeight = 16.sp,
                            textAlign = if (cellIndex < 3) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
                            modifier = Modifier.fillMaxWidth() // Fill width to make textAlign effective
                        )
                    }
                }
            }
        }
    }
}

private fun renderMarkdown(text: String): AnnotatedString {
    // 1. Clean up HTML entities
    val processedText = text
        .replace("&deg;", "°")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    return buildAnnotatedString {
        // Regex to match [text](url)
        val linkRegex = Regex("""\[([^]]+)]\(([^)]+)\)""")
        var lastIndex = 0
        
        linkRegex.findAll(processedText).forEach { match ->
            // Text before match
            appendProcessedContent(processedText.substring(lastIndex, match.range.first))
            
            val linkText = match.groupValues[1]
            val url = match.groupValues[2]
            
            if (url.startsWith("http") || url.startsWith("app://")) {
                // External or App Link
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(
                    color = if (url.startsWith("app://")) Color(0xFFA78BFA) else Color(0xFF3B82F6),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )) {
                    append(linkText)
                }
                pop()
            } else {
                // Local dish reference
                appendProcessedContent(linkText)
            }
            
            lastIndex = match.range.last + 1
        }
        
        // Remaining text
        if (lastIndex < processedText.length) {
            appendProcessedContent(processedText.substring(lastIndex))
        }
    }
}

private fun AnnotatedString.Builder.appendProcessedContent(content: String) {
    val parts = content.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Black, color = NeoBlack)) {
                append(part)
            }
        } else {
            append(part)
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .border(1.5.dp, NeoBlack, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code.trim(),
            color = Color(0xFFADF19F),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}
