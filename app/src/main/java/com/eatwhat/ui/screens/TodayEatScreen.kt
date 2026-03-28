package com.eatwhat.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.eatwhat.ui.components.*
import com.eatwhat.ui.viewmodel.Preference
import com.eatwhat.ui.viewmodel.TodayEatViewModel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.airbnb.lottie.compose.*
import com.eatwhat.R
import androidx.compose.animation.core.*
import com.eatwhat.ui.theme.NeoBlack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.clip

@Composable
fun TodayEatScreen(
    viewModel: TodayEatViewModel,
    onBack: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val isSelecting by viewModel.isSelecting
    val selectedDishes by viewModel.selectedDishes
    val selectedMaster by viewModel.selectedMaster
    val recipe by viewModel.recipe
    val isGenerating by viewModel.isGenerating
    val selectionStatus by viewModel.selectionStatus
    val selectionProgress by viewModel.selectionProgress
    val currentSelection by viewModel.currentSelection
    val showPreference by viewModel.showPreference
    val preference by viewModel.preference
    val isAnalyzingDeepInsights by viewModel.isAnalyzingDeepInsights
    val isGeneratingImage by viewModel.isGeneratingImage

    val blurRadius by animateFloatAsState(
        targetValue = if (isGenerating) 30f else 0f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "blur_radius"
    )

    // --- Scroll Focus Management (Observation Pattern) ---
    var lastRecipeId by remember { mutableStateOf(recipe?.id) }
    var autoScrollWindowUntil by remember { mutableLongStateOf(0L) }
    var lastMaxValue by remember { mutableIntStateOf(scrollState.maxValue) }

    // Global Scroll-to-Bottom only when a NEW recipe is generated
    LaunchedEffect(recipe?.id) {
        val hasNewResult = recipe != null && recipe?.id != lastRecipeId
        
        if (hasNewResult) {
            autoScrollWindowUntil = System.currentTimeMillis() + 2500 // 2.5s window
            lastMaxValue = 0 // Reset to force scroll for new result
            
            // Initial follow-up for the start of the result display
            val startTime = System.currentTimeMillis()
            var lastTarget = 0
            while (System.currentTimeMillis() - startTime < 1000) {
                if (scrollState.maxValue > lastTarget) {
                    scrollState.animateScrollTo(
                        scrollState.maxValue,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                    lastTarget = scrollState.maxValue
                }
                withFrameNanos { }
            }
        }
        lastRecipeId = recipe?.id
    }

    // Windowed observer for automated scrolling during the result reveal window
    LaunchedEffect(scrollState.maxValue) {
        // PROFESSIONAL FIX: Only auto-scroll globally if we are NOT in an explicit focus operation
        // and only during the initial generation window.
        if (System.currentTimeMillis() < autoScrollWindowUntil && scrollState.maxValue > lastMaxValue) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        }
        lastMaxValue = scrollState.maxValue
    }

    // Explicitly kill any active scroll window when starting a new generation or selection
    val cancelGlobalScroll = {
        autoScrollWindowUntil = 0
        lastMaxValue = scrollState.maxValue 
    }

    LaunchedEffect(isGenerating, isSelecting) {
        if (isGenerating || isSelecting) {
            cancelGlobalScroll()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val errorMessage by viewModel.errorMessage

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFACC15)) // Yellow-400
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
                .graphicsLayer {
                    // High-End Seamless Blur Animation
                    if (blurRadius > 0.1f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        this.renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            blurRadius, blurRadius, android.graphics.Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                    }
                },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NeoHeader(
                title = "随机盲盒",
                subtitle = "不知道吃什么？让命运替你决定",
                backgroundColor = Color(0xFFFACC15), // Same as BG
                onBack = onBack,
                heroEmoji = "🎁"
            )

            // Main Viewport Container with Fading Edge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fadingEdge(top = 10.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 10.dp, bottom = 16.dp), // Maintained to match Home Screen logic
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Hero Section / Start
                    if (!isSelecting && selectedDishes.isEmpty()) {
                        NeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text("🍱", fontSize = 80.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("准备好了吗？", fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                Text("点击下方按钮开启随机美食之旅", fontSize = 14.sp, color = NeoBlack, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                NeoButton(
                                    text = "开始随机选择",
                                    onClick = { viewModel.startRandomSelection() },
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    shadowOffset = 4.dp // Consistent shadow
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Preference Toggle
                                Row(
                                    modifier = Modifier.neoClickable { viewModel.togglePreference() },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("偏好设置", color = NeoBlack, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    Icon(
                                        imageVector = if (showPreference) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = NeoBlack
                                    )
                                }

                                AnimatedVisibility(visible = showPreference) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Preference.entries.chunked(2).forEach { chunk ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                chunk.forEach { pref ->
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .neoClickable { viewModel.setPreference(pref) }
                                                            .padding(vertical = 4.dp),
                                                        color = if (preference == pref) Color(0xFFF97316) else Color.White,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = rowBorder(2.dp, NeoBlack) // Thicker border
                                                    ) {
                                                        Text(
                                                            text = "${pref.icon} ${pref.label}",
                                                            modifier = Modifier.padding(8.dp),
                                                            textAlign = TextAlign.Center,
                                                            color = if (preference == pref) Color.White else Color.Black,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selection Process
                    if (isSelecting) {
                        NeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(selectionStatus, fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeoBlack)
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { selectionProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).border(1.dp, NeoBlack, RoundedCornerShape(4.dp)),
                                    color = Color(0xFFF97316),
                                    trackColor = Color.White
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                currentSelection?.let { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                                            .border(2.dp, NeoBlack, RoundedCornerShape(12.dp))
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(if (item.type == "dish") "🍱" else item.avatar, fontSize = 48.sp)
                                        Text(item.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeoBlack)
                                        if (item.specialty.isNotBlank()) {
                                            Text(item.specialty, fontSize = 14.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Results
                    if (!isSelecting && selectedDishes.isNotEmpty()) {
                        NeoCard(modifier = Modifier.fillMaxWidth()) {
                            Text("今日推荐", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp, color = NeoBlack)
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                                        .border(1.dp, NeoBlack, RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    Text("🥗 推荐菜品", fontWeight = FontWeight.Black, color = NeoBlack, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    selectedDishes.forEach { dish ->
                                        Text(text = "• $dish", fontSize = 14.sp, color = NeoBlack, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFAF5FF), RoundedCornerShape(12.dp))
                                        .border(1.dp, NeoBlack, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("👑 推荐主厨", fontWeight = FontWeight.Black, color = NeoBlack, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    selectedMaster?.let { master ->
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .background(Color.White, RoundedCornerShape(16.dp))
                                                .border(2.dp, NeoBlack, RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(master.avatar, fontSize = 40.sp)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(master.name, fontWeight = FontWeight.Black, color = NeoBlack, fontSize = 16.sp, textAlign = TextAlign.Center)
                                        if (master.specialty.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                master.specialty, 
                                                fontSize = 11.sp, 
                                                color = NeoBlack, 
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1.2f))
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            NeoButton(
                                text = if (isGenerating) "正在生成..." else "生成菜谱",
                                onClick = { viewModel.generateRecipe() },
                                modifier = Modifier.fillMaxWidth(),
                                textColor = NeoBlack,
                                shadowOffset = 4.dp, // Consistent shadow
                                enabled = !isGenerating
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            NeoButton(
                                text = "重新选择",
                                onClick = { viewModel.reset() },
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFFF3F4F6),
                                textColor = NeoBlack,
                                shadowOffset = 4.dp // Consistent shadow
                            )
                        }
                        
                        if (!isGenerating && recipe != null) {
                            recipe?.let {
                                // Removed animateContentSize(tween(400)) to prevent conflict with scroll-follow
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Box {
                                    RecipeCard(
                                        recipe = it,
                                        isFavorite = viewModel.isFavorite(it.id),
                                        isGenerating = isGeneratingImage,
                                        isAnalyzingDeepInsights = isAnalyzingDeepInsights,
                                        shadowOffset = 6.dp, // 恢复至豪华版 6.dp (Restore to deluxe 6.dp)
                                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                                        onUnlockDeepInsights = {
                                            viewModel.recipe.value?.let { currentRecipe ->
                                                viewModel.unlockDeepInsights(currentRecipe)
                                            }
                                        },
                                        onGenerateImage = {
                                            android.widget.Toast.makeText(context, "🪄 正在为 '${it.name}' 创作图鉴...", android.widget.Toast.LENGTH_LONG).show()
                                            viewModel.generateImage(it) { url ->
                                                if (url != null) {
                                                    android.widget.Toast.makeText(context, "创作成功！已收录至 GALLERY", android.widget.Toast.LENGTH_LONG).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "创作失败，请检查设置或重试", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onExpandedChange = {
                                            autoScrollWindowUntil = System.currentTimeMillis() + 1500
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- High-End Seamless Blind Box Ritual Overlay ---
        AnimatedVisibility(
            visible = isGenerating,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(600))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .neoClickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Floating Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "floating")
                    val dy by infiniteTransition.animateFloat(
                        initialValue = -15f,
                        targetValue = 15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "float_y"
                    )

                    AnimatedVisibility(
                        visible = isGenerating,
                        enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                        modifier = Modifier.graphicsLayer { translationY = dy }
                    ) {
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.gift_premium_animation)
                        )
                        val progress by animateLottieCompositionAsState(
                            composition,
                            iterations = LottieConstants.IterateForever
                        )
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.size(280.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    TodayEatLoadingProgress()
                }
            }
        }
    }
}

@Composable
fun TodayEatLoadingProgress() {
    val infiniteTransition = rememberInfiniteTransition(label = "filling")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(14.dp)
                .background(Color.White, RoundedCornerShape(7.dp))
                .border(2.dp, NeoBlack, RoundedCornerShape(7.dp))
                .clip(RoundedCornerShape(7.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Color(0xFFF97316)) // Theme Orange
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "烹饪灵感汇聚计算中...",
            color = Color.White.copy(0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(0.6f), blurRadius = 8f)
            )
        )
    }
}

private fun rowBorder(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
