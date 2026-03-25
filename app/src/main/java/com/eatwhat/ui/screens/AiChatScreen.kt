package com.eatwhat.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import com.eatwhat.ui.components.*
import com.eatwhat.ui.theme.NeoBlack
import com.eatwhat.ui.theme.NeoYellow
import com.eatwhat.ui.theme.NeoOrange
import com.eatwhat.ui.theme.NeoGreen
import com.eatwhat.ui.theme.NeoViolet
import com.eatwhat.ui.viewmodel.AiChatViewModel
import com.eatwhat.data.model.ChatMessageItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Environment
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import android.net.Uri
import com.google.gson.Gson
import com.eatwhat.data.model.Recipe
import android.util.Log
import android.widget.Toast
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(viewModel: AiChatViewModel) {
    val messages = viewModel.messages
    val isLoading by viewModel.isLoading
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    /* Custom History Drawer State */
    var showHistory by remember { mutableStateOf(false) }

    /* Voice integration */
    val voiceManager = remember { VoiceInputManager(context) }
    DisposableEffect(voiceManager) { onDispose { voiceManager.destroy() } }
    
    val showVoiceDialogState = remember { mutableStateOf(false) }
    var showVoiceDialog by showVoiceDialogState
    val voiceState by voiceManager.state
    val recognizedText by voiceManager.text
    @Suppress("SpellCheckingInspection")
    val rmsdB by voiceManager.rmsdB

    if (showVoiceDialog) {
        NeoVoiceDialog(
            state = voiceState,
            text = recognizedText,
            rmsdB = rmsdB,
            onDismiss = { 
                showVoiceDialogState.value = false
                Log.d("AiChat", "Dialog dismissed")
            },
            onFinish = {
                when (voiceState) {
                    VoiceState.LISTENING -> voiceManager.stopListening()
                    VoiceState.SUCCESS -> {
                        inputText = recognizedText
                        showVoiceDialogState.value = false
                        voiceManager.reset()
                        Log.d("AiChat", "Input received: $inputText")
                    }
                    else -> {
                        showVoiceDialogState.value = false
                        Log.d("AiChat", "Voice ended, dialog closed")
                    }
                }
            }
        )
    }

    // Image Management (Reusing Home logic)
    val showImageSourceDialogState = remember { mutableStateOf(false) }
    var showImageSourceDialog by showImageSourceDialogState
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    /* Permission Guide State */
    var showPermissionGuide by remember { mutableStateOf(false) }
    var permissionGuideTitle by remember { mutableStateOf("") }
    var permissionGuideMessage by remember { mutableStateOf("") }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri -> 
                selectedImageUri = uri
                viewModel.recognizeIngredients(
                    uri.toString(), 
                    onSuccess = { result ->
                        inputText = if (inputText.isBlank()) result else "$inputText $result"
                    },
                    onFailure = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            selectedImageUri = it
            viewModel.recognizeIngredients(
                it.toString(),
                onSuccess = { result ->
                    inputText = if (inputText.isBlank()) result else "$inputText $result"
                },
                onFailure = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
            showVoiceDialogState.value = true
        } else {
            val activity = context as? android.app.Activity
            val showRationale = activity?.let { 
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO) 
            } ?: true
            
            if (!showRationale) {
                permissionGuideTitle = "麦克风权限已禁用"
                permissionGuideMessage = "由于您拒绝了麦克风权限，目前无法使用语音录入功能。请前往系统设置手动开启。"
                showPermissionGuide = true
            } else {
                Toast.makeText(context, "请授权麦克风权限以使用语音功能", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchCamera() {
        try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile("IMG_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.file_provider", file)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
            showImageSourceDialogState.value = false
        } catch (_: Exception) {
            Toast.makeText(context, "无法启动相机", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            val activity = context as? android.app.Activity
            val showRationale = activity?.let { 
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) 
            } ?: true
            
            if (!showRationale) {
                permissionGuideTitle = "相机权限已禁用"
                permissionGuideMessage = "由于您拒绝了相机权限，请前往系统设置手动开启，否则无法使用拍照识别功能。"
                showPermissionGuide = true
            } else {
                Toast.makeText(context, "请授权相机权限以进行拍摄", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPermissionGuide) {
        Dialog(onDismissRequest = { showPermissionGuide = false }) {
            NeoCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                backgroundColor = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(permissionGuideTitle, fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeoBlack)
                    Text(permissionGuideMessage, fontSize = 14.sp, color = NeoBlack, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    
                    NeoButton(
                        text = "🚀 前往系统设置",
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                            showPermissionGuide = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFFACC15)
                    )
                    
                    TextButton(onClick = { showPermissionGuide = false }) {
                        Text("稍后再说", color = NeoBlack.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- Visionary Entry Animations ---
    val isChatStarted = messages.isNotEmpty()
    if (isChatStarted) Log.d("AiChat", "Session active")
    val headerAlpha by animateFloatAsState(
        targetValue = if (isChatStarted) 1f else 0f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (isChatStarted) 0f else -20f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "headerTranslation"
    )

    if (showImageSourceDialog) {
        Dialog(onDismissRequest = { showImageSourceDialogState.value = false }) {
            NeoCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                backgroundColor = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("选择图片来源", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NeoBlack)
                    
                    NeoButton(
                        text = "📸 立即拍照",
                        onClick = {
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                            Log.d("AiChat", "Camera source attempt")
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    )
                    
                    NeoButton(
                        text = "🖼️ 从相册选择",
                        onClick = {
                            galleryLauncher.launch("image/*")
                            showImageSourceDialogState.value = false
                            Log.d("AiChat", "Source selected")
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        backgroundColor = Color(0xFFF3F4F6)
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = NeoYellow,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(bottom = 0.dp)
        ) {
        // 1. Dynamic Header (Reveals on chat start)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { 
                    alpha = headerAlpha
                    translationY = headerTranslationY
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title (Left)
            Text(
                text = "AI 厨神",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = NeoBlack
            )

            // Actions Group (Right)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp).neoClickable { showHistory = !showHistory },
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, NeoBlack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.History, null, tint = NeoBlack, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(38.dp).neoClickable { 
                        viewModel.startNewChat()
                        Toast.makeText(context, "新对话已开始", Toast.LENGTH_SHORT).show()
                    },
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, NeoBlack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = NeoBlack, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // 2. Main Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isChatStarted,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                WelcomeContent(
                    hasHistory = viewModel.hasHistory.value,
                    onCommandClick = { command -> viewModel.sendMessage(command) },
                    onHistoryClick = { showHistory = true }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isChatStarted,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically { it / 3 },
                exit = fadeOut()
            ) {
                LaunchedEffect(
                    messages.size, 
                    messages.lastOrNull()?.content,
                    messages.lastOrNull()?.isRecipeLoading,
                    messages.lastOrNull()?.recipeJson
                ) {
                    if (messages.isNotEmpty()) {
                        val lastMsg = messages.lastOrNull()
                        val isRecipeVisible = lastMsg?.recipeJson != null
                        
                        if (isLoading && !isRecipeVisible) {
                            // Standard streaming text, snap scroll is enough
                            listState.scrollToItem(messages.size) 
                        } else {
                            // Aggressively stick to the bottom during any transition (growing bubble or generation complete)
                            // This ensures that as the bubble grows with animateContentSize, the list 'follows' its expansion.
                            val startTime = System.currentTimeMillis()
                            while (System.currentTimeMillis() - startTime < 800) {
                                listState.scrollToItem(messages.size)
                                withFrameNanos { } // Sync with VSync (Supports 90Hz, 120Hz, etc.)
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val favVer by viewModel.favoritesVersion
                        ChatBubble(
                            message = msg,
                            isGenerating = messages.lastOrNull()?.id == msg.id && !msg.isUser && isLoading,
                            onFavoriteClick = { recipe -> viewModel.toggleFavorite(recipe) },
                            isFavorite = { id -> 
                                favVer.let { viewModel.isFavorite(id) } 
                            }
                        )
                    }
                    
                    item(key = "bottom_anchor") {
                        Spacer(modifier = Modifier.height(20.dp).fillMaxWidth())
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showHistory,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .neoClickable(enabled = true, onClick = { showHistory = false })
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .padding(top = 16.dp, bottom = 100.dp, start = 16.dp)
                            .neoClickable(enabled = true, onClick = { /* Consumes click */ }),
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, NeoBlack),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("会话历史", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeoBlack)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(viewModel.sessions) { summary ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (viewModel.currentSessionId.value == summary.sessionId) NeoYellow else Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(2.dp, NeoBlack)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).neoClickable { 
                                                    viewModel.switchSession(summary.sessionId)
                                                    showHistory = false
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                 Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = summary.firstMessage, 
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeoBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            
                                            IconButton(onClick = { viewModel.deleteSession(summary.sessionId) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                if (viewModel.sessions.isEmpty()) {
                                    item {
                                        Text("暂无历史记录", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // 3. Sticky Bottom Input (Redesigned for transition stability and corrected keyboard offset)
            val density = LocalDensity.current
            val imeInsets = WindowInsets.ime
            val navInsets = WindowInsets.navigationBars
            
            val restingMarginPx = with(density) { 35.dp.roundToPx() } // Standard "up a bit"
            val stickyMarginPx = with(density) { 3.dp.roundToPx() }
            
            
            Column(
                modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset {
                            val imePx = imeInsets.getBottom(this)
                            val navPx = navInsets.getBottom(this)
                            val barPx = with(this) { 68.dp.roundToPx() }
                            val totalBarPx = navPx + barPx
                            
                            // Calculate how much the keyboard incurs beyond the bottom bar area
                            val keyboardIncursion = (imePx - totalBarPx).coerceAtLeast(0)
                            
                            val yOffset = if (keyboardIncursion > 0) {
                                // Important: Subtract (baseFloorPx - stickyMarginPx) to account for the resting 35dp padding
                                -(keyboardIncursion - (restingMarginPx - stickyMarginPx))
                            } else {
                                0
                            }
                            IntOffset(x = 0, y = yOffset)
                        }
                        .padding(horizontal = 13.dp)
                        .padding(bottom = with(density) { restingMarginPx.toDp() })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = NeoBlack,
                                topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                            )
                        },
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(3.dp, NeoBlack)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 8.dp)
                                    .size(44.dp)
                                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)) // Pre-fill color
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = null,
                                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(2.dp, NeoBlack, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(16.dp)
                                        .background(Color.Red, CircleShape)
                                        .border(1.dp, NeoBlack, CircleShape)
                                        .neoClickable { selectedImageUri = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }

                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f).padding(start = if (selectedImageUri == null) 12.dp else 0.dp),
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeoBlack),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text("问点什么...", color = NeoBlack.copy(alpha = 0.3f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                innerTextField()
                            }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(onClick = {
                                val permission = Manifest.permission.RECORD_AUDIO
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    voiceManager.startListening()
                                    showVoiceDialogState.value = true
                                } else {
                                    micPermissionLauncher.launch(permission)
                                }
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Mic, null, tint = NeoBlack, modifier = Modifier.size(22.dp))
                            }
                            
                            IconButton(onClick = { 
                                showImageSourceDialogState.value = true
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.CameraAlt, null, tint = NeoBlack, modifier = Modifier.size(20.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            Surface(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 38.dp)
                                    .neoClickable(enabled = true) {
                                        if (isLoading) {
                                            viewModel.stopGeneration()
                                        } else if (inputText.isNotBlank() || selectedImageUri != null) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                            selectedImageUri = null
                                        }
                                    },
                                color = if (isLoading) Color.Red else if (inputText.isNotBlank() || selectedImageUri != null) NeoBlack else Color.White,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, NeoBlack)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isLoading) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color.White, RoundedCornerShape(2.dp))
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send, 
                                            null, 
                                            tint = if (inputText.isNotBlank() || selectedImageUri != null) Color.White else NeoBlack.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
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
}

@Composable
private fun FeatureTile(
    title: String,
    emoji: String,
    backgroundColor: Color,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    chips: List<QuickAction> = emptyList(),
    cardPadding: Dp = 12.dp,
    chipVerticalPadding: Dp = 6.dp,
    chipSpacing: Dp = 8.dp
) {
    NeoCard(
        modifier = modifier.wrapContentHeight(),
        backgroundColor = backgroundColor,
        shadowOffset = 6.dp,
        cornerRadius = 16.dp,
        padding = cardPadding
    ) {
        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeoBlack
                )
                Text(text = emoji, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.height(if (cardPadding.value < 10f) 4.dp else 12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(chipSpacing)) {
                chips.forEach { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, NeoBlack, RoundedCornerShape(8.dp))
                            .neoClickable { onChipClick(action.fullCommand) }
                            .padding(horizontal = 8.dp, vertical = chipVerticalPadding)
                    ) {
                        Text(
                            text = action.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeContent(
    hasHistory: Boolean,
    onCommandClick: (String) -> Unit,
    onHistoryClick: () -> Unit
) {
    val quickActions = listOf(
        QuickAction("🍳 学做地道家常菜", "教我做一道好吃的家常菜", NeoYellow.copy(alpha = 0.8f)),
        QuickAction("🥬 我今天想吃点清淡健康的", "我想吃点健康的", Color(0xFFE8F5E9)),
        QuickAction("🍅 冰箱里只有番茄和鸡蛋该怎么做？", "冰箱里只有番茄和鸡蛋", NeoOrange.copy(alpha = 0.2f)),
        QuickAction("🥒 低脂减产", "推荐一些低脂菜谱", Color(0xFFFFF3E0)),
        QuickAction("🥦 有没有减脂餐推荐？", "给我推荐低卡路里的晚餐", Color(0xFFF1F8E9)),
        QuickAction("🥟 手工水饺秘籍", "水饺怎么包才不破？", Color(0xFFFFFDE7)),
        QuickAction("🍜 来碗热腾腾的面条", "想吃一碗热腾腾的面", Color(0xFFE0F2F1)),
        QuickAction("🍜 学做各种甜点", "新手能做的简单甜点", Color(0xFFFCE4EC)),
        QuickAction("🥣 此季适合喝什么养生汤？", "现在季节适合喝什么汤？", Color(0xFFE1F5FE)),
        QuickAction("🍲 尝试新颖特色菜", "教我做一份有新意的菜谱", NeoYellow.copy(alpha = 0.8f))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = this.maxHeight
        // Calculate a dynamic scale factor that reduces sizes linearly on smaller screens
        val referenceHeight = 780.dp 
        val scaleFactor = (availableHeight / referenceHeight).coerceIn(0.75f, 1.0f)
        
        val spacing = (24 * scaleFactor).dp
        val cardPadding = (12 * scaleFactor).dp
        val chipVerticalPadding = (6 * scaleFactor).dp
        val chipSpacing = (8 * scaleFactor).dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.4f)) // Editorial Top Margin
            
            // --- Plan C: Minimalist Brand Wall (Typography First) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // "食见" with custom stroke styling
                    Text(
                        text = "食见",
                        style = TextStyle(
                            fontSize = (68 * scaleFactor).sp,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            letterSpacing = (-2).sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp * scaleFactor))
                    
                    // "AI" Badge
                    Box(
                        modifier = Modifier
                            .padding(bottom = (12 * scaleFactor).dp)
                            .background(NeoBlack, RoundedCornerShape((8 * scaleFactor).dp))
                            .padding(horizontal = (12 * scaleFactor).dp, vertical = (4 * scaleFactor).dp)
                    ) {
                        Text(
                            text = "AI",
                            fontSize = (26 * scaleFactor).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp * scaleFactor))
                
                // Minimalist Editorial Subtitle
                Text(
                    text = "YOUR AI CULINARY ASSISTANT",
                    fontSize = (11 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoBlack.copy(alpha = 0.4f),
                    letterSpacing = (2 * scaleFactor).sp
                )
                
                Spacer(modifier = Modifier.height(16.dp * scaleFactor))
                
                // The "Designer" Line
                Box(
                    modifier = Modifier
                        .width((48 * scaleFactor).dp)
                        .height((5 * scaleFactor).dp)
                        .background(NeoBlack)
                )
            }
            
            Spacer(modifier = Modifier.weight(0.6f)) // Re-balanced bottom spacer for cards

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureTile(
                        title = "探索",
                        emoji = "🔍",
                        backgroundColor = NeoYellow,
                        onChipClick = onCommandClick,
                        modifier = Modifier.weight(1.4f),
                        chips = listOf(quickActions[0], quickActions[6], quickActions[9]),
                        cardPadding = cardPadding,
                        chipVerticalPadding = chipVerticalPadding,
                        chipSpacing = chipSpacing
                    )
                    FeatureTile(
                        title = "轻食",
                        emoji = "🥬",
                        backgroundColor = NeoGreen,
                        onChipClick = onCommandClick,
                        modifier = Modifier.weight(1f),
                        chips = listOf(quickActions[1], quickActions[3], quickActions[4]),
                        cardPadding = cardPadding,
                        chipVerticalPadding = chipVerticalPadding,
                        chipSpacing = chipSpacing
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureTile(
                        title = "大厨",
                        emoji = "🥘",
                        backgroundColor = NeoOrange,
                        onChipClick = onCommandClick,
                        modifier = Modifier.weight(1f),
                        chips = listOf(quickActions[2], quickActions[5]),
                        cardPadding = cardPadding,
                        chipVerticalPadding = chipVerticalPadding,
                        chipSpacing = chipSpacing
                    )
                    FeatureTile(
                        title = "悦己",
                        emoji = "🍰",
                        backgroundColor = NeoViolet,
                        onChipClick = onCommandClick,
                        modifier = Modifier.weight(1.3f),
                        chips = listOf(quickActions[7], quickActions[8]),
                        cardPadding = cardPadding,
                        chipVerticalPadding = chipVerticalPadding,
                        chipSpacing = chipSpacing
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        if (hasHistory) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (8 * scaleFactor).dp) // Moved lower by reducing from 'spacing' to fixed 8dp (scaled)
                    .neoClickable { onHistoryClick() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = NeoBlack.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "查看历史会话",
                    fontSize = (13 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoBlack.copy(alpha = 0.4f)
                )
            }
        }
    }
}

data class QuickAction(
    val title: String,
    val fullCommand: String,
    val color: Color
)


@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    color: Color = NeoBlack
) {
    val trimmedText = remember(text) { text.trimEnd() }
    val lines = remember(trimmedText) { trimmedText.split("\n") }
    val headerRegex = remember { Regex("""^(#{1,6})\s*(.*)$""") }
    
    val annotatedString = buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (trimmedLine == "---" || trimmedLine == "***" || trimmedLine == "___") {
                withStyle(style = SpanStyle(color = color.copy(alpha = 0.2f), letterSpacing = (-2).sp)) {
                    append("__________________________________________")
                }
            } else {
                val headerMatch = headerRegex.find(line)
                if (headerMatch != null) {
                    val level = headerMatch.groupValues[1].length
                    val content = headerMatch.groupValues[2]
                    val style = when (level) {
                        1 -> SpanStyle(fontWeight = FontWeight.Black, fontSize = fontSize * 1.5f)
                        2 -> SpanStyle(fontWeight = FontWeight.Black, fontSize = fontSize * 1.35f)
                        3 -> SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = fontSize * 1.25f)
                        else -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize * 1.15f)
                    }
                    withStyle(style = style) {
                        appendInlineMarkdown(content, color)
                    }
                } else if (line.startsWith("> ")) {
                    withStyle(style = SpanStyle(color = color.copy(alpha = 0.6f), fontStyle = FontStyle.Italic)) {
                        append("▎")
                        appendInlineMarkdown(line.substring(2), color.copy(alpha = 0.6f))
                    }
                } else if (line.startsWith("* ") || line.startsWith("- ")) {
                    append(" • ")
                    appendInlineMarkdown(line.substring(2), color)
                } else if (line.startsWith("1. ")) {
                    append(" 1. ")
                    appendInlineMarkdown(line.substring(3), color)
                } else {
                    appendInlineMarkdown(line, color)
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            modifier = modifier,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            lineHeight = (fontSize.value * 1.5).sp,
            color = color
        )
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, color: Color) {
    val matches = Regex("""(\*\*.*?\*\*|`.*?`)""").findAll(text)
    var lastIndex = 0
    for (match in matches) {
        append(text.substring(lastIndex, match.range.first))
        val token = match.value
        if (token.startsWith("**") && token.endsWith("**") && token.length >= 4) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Black, color = color)) {
                append(token.substring(2, token.length - 2))
            }
        } else if (token.startsWith("`") && token.endsWith("`") && token.length >= 2) {
            withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace, background = color.copy(alpha = 0.1f))) {
                append(token.substring(1, token.length - 1))
            }
        } else append(token)
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}

@Composable
fun ChatBubble(
    message: ChatMessageItem, 
    isGenerating: Boolean = false,
    onFavoriteClick: (Recipe) -> Unit = {},
    isFavorite: (String) -> Boolean = { false }
) {
    val recipe = remember(message.recipeJson) {
        if (message.recipeJson != null) {
            try { Gson().fromJson(message.recipeJson, Recipe::class.java) } catch (_: Exception) { null }
        } else null
    }
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val timeStr = remember(message.timestamp) {
        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply { timeInMillis = message.timestamp }
        val format = when {
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) -> "HH:mm"
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> "MM-dd HH:mm"
            else -> "yyyy-MM-dd HH:mm"
        }
        SimpleDateFormat(format, Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
            if (!message.isUser) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(2.dp, NeoBlack)) {
                    Box(contentAlignment = Alignment.Center) { Text("🧑‍🍳", fontSize = 20.sp) }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
                NeoCard(
                    modifier = Modifier.widthIn(max = 280.dp).wrapContentWidth(),
                    backgroundColor = if (message.isUser) Color(0xFF22D3EE) else Color.White,
                    shadowOffset = 4.dp,
                    cornerRadius = 16.dp,
                    padding = 0.dp,
                    fullWidth = false
                ) {
                        Column(modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp).animateContentSize()) {
                            if (message.isUser) {
                                Text(text = message.content, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp, modifier = Modifier.padding(bottom = 4.dp))
                            } else {
                                if (message.content.trim().isEmpty() && isGenerating) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NeoBlack)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("厨神思考中...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    MarkdownText(text = message.content, modifier = Modifier.padding(bottom = 4.dp))
                                    if (message.isRecipeLoading) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp)).border(1.dp, NeoBlack.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeoBlack)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("正在为您排版精美菜谱...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeoBlack.copy(alpha = 0.6f))
                                        }
                                    }
                                    if (recipe != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        ChatRecipeLayout(recipe = recipe, isFavorite = isFavorite(recipe.id), onFavoriteClick = { onFavoriteClick(recipe) })
                                    }
                                }
                            }
                            Text(text = timeStr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeoBlack.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.End))
                        }
                }
                Text(text = if (message.isUser) "ME" else "CHEF", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoBlack.copy(alpha = 0.4f), modifier = Modifier.padding(start = 6.dp, top = 4.dp, end = 6.dp))
            }

            if (message.isUser) {
                Spacer(modifier = Modifier.width(10.dp))
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = NeoOrange, border = BorderStroke(2.dp, NeoBlack)) {
                    Box(contentAlignment = Alignment.Center) { Text("👤", fontSize = 20.sp) }
                }
            }
        }
    }
}

@Composable
fun ChatRecipeLayout(recipe: Recipe, isFavorite: Boolean, onFavoriteClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).border(2.dp, NeoBlack, RoundedCornerShape(12.dp))) {
        Row(modifier = Modifier.fillMaxWidth().background(NeoOrange.copy(alpha = 0.1f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeoBlack)
                Text("${recipe.cuisine} • ${recipe.cookingTime} min", fontSize = 11.sp, color = NeoBlack.copy(alpha = 0.6f))
            }
            IconButton(onClick = onFavoriteClick, modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.5.dp, NeoBlack, RoundedCornerShape(8.dp))) { Text(if (isFavorite) "❤️" else "🤍", fontSize = 18.sp) }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🥗 所需食材", fontWeight = FontWeight.Bold, fontSize = 12.sp); Spacer(modifier = Modifier.height(6.dp))
            FlowRow(spacing = 4.dp) { recipe.ingredients.forEach { Surface(color = Color(0xFFFEF9C3), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NeoBlack.copy(alpha = 0.2f))) { Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp) } } }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("👨‍🍳 制作步骤", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            val stepsToShow = if (isExpanded) recipe.steps else recipe.steps.take(2)
            stepsToShow.forEach { step ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.size(18.dp).background(NeoBlack, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text(step.step.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(8.dp)); Text(step.description, fontSize = 12.sp, color = Color.DarkGray)
                }
            }
            if (recipe.steps.size > 2) {
                Text(
                    text = if (isExpanded) "收起步骤 ▲" else "...还有 ${recipe.steps.size - 2} 个步骤，查看全部 ▼",
                    fontSize = 11.sp,
                    color = NeoOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 4.dp, start = 26.dp)
                )
            }
        }
    }
}
