package com.shijian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.shijian.ui.components.*
import com.shijian.ui.viewmodel.GalleryViewModel
import com.shijian.data.model.GalleryImage
import kotlinx.coroutines.launch
import android.widget.Toast
import com.shijian.util.PosterGenerator
import com.shijian.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onNavigateHome: () -> Unit = {}
) {
    val images by viewModel.images
    val isLoading by viewModel.isLoading
    val selectedImageState = remember { mutableStateOf<GalleryImage?>(null) }
    val selectedImage = selectedImageState.value
    val showClearConfirmState = remember { mutableStateOf(false) }
    val showClearConfirm = showClearConfirmState.value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Auto-refresh when entering the screen
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15)) // Yellow-400
            .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth()) {
            NeoHeader(
                title = "GALLERY",
                subtitle = "视觉典藏 (共 ${images.size} 张)",
                backgroundColor = Color(0xFF4ADE80),
                heroEmoji = "🖼️"
            )
            
            if (images.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmState.value = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeoBlack)
                }
            } else if (images.isEmpty()) {
                NeoEmptyState(
                    title = "虚位以待",
                    subtitle = "快去首页“开始创作”，用 AI 为您的厨艺库增添第一抹亮色吧！",
                    emoji = "🖼️",
                    backgroundColor = Color(0xFFF0FDF4),
                    modifier = Modifier.align(Alignment.Center),
                    action = {
                        NeoButton(
                            text = "✨ 开始生成",
                            onClick = onNavigateHome,
                            backgroundColor = Color(0xFFDCFCE7),
                            modifier = Modifier.padding(horizontal = 0.dp),
                            shadowOffset = 6.dp
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(top = 10.dp, bottom = 16.dp)
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                    ) {
                        items(images, key = { it.id }) { image ->
                            GalleryItem(image, viewModel, selectedImageState)
                        }
                    }
                }
            }
        }
    }

    // Image Preview Dialog
    selectedImage?.let { image ->
        Dialog(onDismissRequest = { selectedImageState.value = null }) {
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight(),
                padding = 0.dp,
                shadowOffset = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight * 0.85f)
                ) {
                    // 1. Fixed Top: Image Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .background(Color.LightGray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(image.localPath ?: image.url)
                                .crossfade(400)
                                .build(),
                            contentDescription = image.recipeName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    HorizontalDivider(thickness = 3.dp, color = NeoBlack)

                    // 2. Scrollable Content: Title and Ingredients
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = image.recipeName,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            color = NeoBlack
                        )
                        
                        Surface(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = NeoBlack,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = image.cuisine,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("主要食材", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            image.ingredients.forEach { name ->
                                Surface(
                                    color = Color(0xFFF3F4F6),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, NeoBlack)
                                ) {
                                    Text(
                                        text = name,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoBlack
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 3. Fixed Bottom: Action Buttons
                    HorizontalDivider(thickness = 1.dp, color = NeoBlack.copy(alpha = 0.1f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeoButton(
                                text = "💾 保存",
                                onClick = {
                                    scope.launch {
                                        val poster = PosterGenerator.createPoster(context, image)
                                        if (poster != null) {
                                            viewModel.saveBitmap(context, poster, image.recipeName) { success ->
                                                val msg = if (success) "已保存至“相册/食见”" else "保存失败"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "制作海报失败，请重试", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                backgroundColor = Color(0xFF4ADE80),
                                shadowOffset = 4.dp
                            )
                            
                            NeoButton(
                                text = "📤 分享",
                                onClick = {
                                    scope.launch {
                                        val poster = PosterGenerator.createPoster(context, image)
                                        if (poster != null) {
                                            ShareUtil.shareBitmap(context, poster, image.recipeName)
                                        } else {
                                            Toast.makeText(context, "制作海报失败，请重试", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                backgroundColor = Color.White,
                                shadowOffset = 4.dp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        NeoButton(
                            text = "关闭",
                            onClick = { selectedImageState.value = null },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFF3F4F6),
                            shadowOffset = 4.dp
                        )
                    }
                }
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirmState.value = false },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearAll()
                    showClearConfirmState.value = false
                }) {
                    Text("确认清空", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmState.value = false }) {
                    Text("取消", color = NeoBlack)
                }
            },
            title = { Text("清空图库", fontWeight = FontWeight.Black) },
            text = { Text("确定要删除所有生成的菜品图片吗？此操作不可恢复。") },
            containerColor = Color.White
        )
    }
}

@Composable
fun GalleryItem(
    image: GalleryImage,
    viewModel: GalleryViewModel,
    selectedImageState: MutableState<GalleryImage?>
) {
    val cardColor = remember(image.id) {
        listOf(
            Color(0xFFFBD2E0), Color(0xFFDCFCE7), Color(0xFFFEF9C3),
            Color(0xFFE0F2F1), Color(0xFFF3E8FF), Color(0xFFFFEDD5)
        ).random()
    }
    
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .neoClickable { selectedImageState.value = image },
        backgroundColor = cardColor,
        padding = 0.dp,
        shadowOffset = 4.dp,
        cornerRadius = 12.dp
    ) {
        Box {
            Column {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(image.localPath ?: image.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = image.recipeName,
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .border(
                            width = 2.dp,
                            color = NeoBlack,
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(12.dp).padding(bottom = 4.dp)) {
                    Surface(
                        color = NeoBlack,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = image.cuisine,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = image.recipeName,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = NeoBlack
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = image.generatedAt.take(10),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack.copy(alpha = 0.4f)
                        )
                        Surface(
                            color = NeoBlack,
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("➔", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .neoClickable { viewModel.deleteImage(image.id) }
                    .size(28.dp)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(2.dp, NeoBlack, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeoBlack, modifier = Modifier.size(16.dp))
            }
        }
    }
}
