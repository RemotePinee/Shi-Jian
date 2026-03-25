package com.eatwhat.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke
import com.eatwhat.ui.theme.NeoBlack
import androidx.compose.ui.window.Dialog
import android.os.Environment
import android.widget.Toast
import java.io.File
import androidx.core.content.FileProvider
import com.eatwhat.data.model.ConfigData
import com.eatwhat.ui.components.*
import com.eatwhat.ui.viewmodel.HomeViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val ingredients = viewModel.ingredients
    val selectedCuisines = viewModel.selectedCuisines
    val isLoading by viewModel.isLoading
    val isRecognizing by viewModel.isRecognizing
    val cuisineSlots by viewModel.cuisineSlots.collectAsState()

    var currentInput by remember { mutableStateOf("") }
    val errorMessage by viewModel.errorMessage
    
    /* Permission Guide State */
    var showPermissionGuide by remember { mutableStateOf(false) }
    var permissionGuideTitle by remember { mutableStateOf("") }
    var permissionGuideMessage by remember { mutableStateOf("") }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Image Management
    val showImageSourceDialogState = remember { mutableStateOf(false) }
    val showImageSourceDialog by showImageSourceDialogState
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { viewModel.onImageInput(it.toString()) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageInput(it.toString()) }
    }

    fun launchCamera() {
        try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (directory == null) {
                Toast.makeText(context, "无法访问存储空间", Toast.LENGTH_SHORT).show()
                return
            }
            val f = File(directory, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.file_provider", f)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
            showImageSourceDialogState.value = false
        } catch (e: Exception) {
            Toast.makeText(context, "拍照启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Permission Management
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, isGranted) ->
            if (isGranted) {
                // Feature: Automatically launch camera if it was just granted
                if (permission == android.Manifest.permission.CAMERA) {
                    launchCamera()
                }
            } else {
                val activity = context as? android.app.Activity
                val showRationale = activity?.let { 
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, permission) 
                } ?: true
                
                if (!showRationale) {
                    val pName = if (permission == android.Manifest.permission.CAMERA) "相机" else "麦克风"
                    permissionGuideTitle = "${pName}权限已禁用"
                    permissionGuideMessage = "由于您拒绝了${pName}权限，请前往系统设置手动开启，否则无法使用该功能。"
                    showPermissionGuide = true
                }
            }
        }
    }

    fun checkAndRequestPermissions(permission: String, onGranted: () -> Unit) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            onGranted()
        } else {
            permissionsLauncher.launch(arrayOf(permission))
        }
    }

    // Voice Management
    val voiceManager = remember { VoiceInputManager(context) }
    
    // Properly clean up when the screen is disposed
    DisposableEffect(voiceManager) {
        onDispose {
            voiceManager.destroy()
        }
    }
    val showVoiceDialogState = remember { mutableStateOf(false) }
    val showVoiceDialog by showVoiceDialogState
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
                voiceManager.reset()
                showVoiceDialogState.value = false 
            },
            onFinish = {
                when (voiceState) {
                    VoiceState.LISTENING -> {
                        voiceManager.stopListening()
                    }
                    VoiceState.SUCCESS -> {
                        if (recognizedText.isNotBlank()) {
                            viewModel.addIngredient(recognizedText)
                        }
                        voiceManager.reset()
                        showVoiceDialogState.value = false
                    }
                    VoiceState.ERROR -> {
                        voiceManager.reset()
                        showVoiceDialogState.value = false
                    }
                    else -> {
                        showVoiceDialogState.value = false
                    }
                }
            }
        )
    }


    if (showImageSourceDialog) {
        Dialog(onDismissRequest = { showImageSourceDialogState.value = false }) {
            NeoCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                backgroundColor = Color.White,
                shadowOffset = 3.dp // Slightly thinner shadow for a cleaner look
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), // Balanced vertical air
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title removed to ensure perfect button centering as requested
                    
                    NeoButton(
                        text = "📸 立即拍照",
                        onClick = {
                            val permission = android.Manifest.permission.CAMERA
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                launchCamera()
                            } else {
                                permissionsLauncher.launch(arrayOf(permission))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFA78BFA) // Violet
                    )
                    
                    NeoButton(
                        text = "🖼️ 从相册选择",
                        onClick = {
                            galleryLauncher.launch("image/*")
                            showImageSourceDialogState.value = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFFACC15) // Yellow
                    )
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFACC15)) // Yellow-400
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeoHeader(
            title = "吃点什么？",
            subtitle = "让灵感碰撞随机分配你的下一顿",
            backgroundColor = Color(0xFFFB923C), // Reverted to original Orange
            heroEmoji = "🍱"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

        // Combined Input Block
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            shadowOffset = 6.dp, // Reverted to standard shadow
            borderWidth = 4 // Kept the bold borders as it defines the style
        ) {
            // Section 1: Ingredients
            Column {
                Text("1. 输入食材", fontWeight = FontWeight.Black, color = Color(0xFFE91E63), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("添加食材", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                Text("输入你现有的食材，点击添加", color = NeoBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                // Tag Cloud
                FlowRow(spacing = 4.dp) {
                    ingredients.forEach { name ->
                        NeoTag(text = name, onRemove = { viewModel.removeIngredient(name) })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Neo-Brutalist Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(3.dp, NeoBlack, RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = NeoBlack,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (currentInput.isNotBlank()) {
                                viewModel.addIngredient(currentInput)
                                currentInput = ""
                            }
                        }),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (currentInput.isEmpty()) {
                                        Text(
                                            "输入食材名称...",
                                            color = NeoBlack.copy(alpha = 0.4f),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    innerTextField()
                                }
                                IconButton(onClick = {
                                    checkAndRequestPermissions(android.Manifest.permission.RECORD_AUDIO) {
                                        voiceManager.startListening()
                                        showVoiceDialogState.value = true
                                    }
                                }) {
                                    Icon(Icons.Default.Mic, contentDescription = "语音输入", tint = NeoBlack)
                                }
                                IconButton(onClick = {
                                    showImageSourceDialogState.value = true
                                }) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "拍照/相册", tint = NeoBlack)
                                }
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                NeoButton(
                    text = if (isRecognizing) "识别中..." else "添加食材",
                    onClick = {
                        if (currentInput.isNotBlank()) {
                            viewModel.addIngredient(currentInput)
                            currentInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFB923C), // Consistent Orange
                    shadowOffset = 4.dp, // Stronger shadow
                    enabled = !isRecognizing
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section 2: Cuisine
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("2. 选择菜系", fontWeight = FontWeight.Black, color = Color(0xFF4CAF50), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Symmetrical 4-column Grid
                val cuisineRows = ConfigData.cuisines.take(8).chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cuisineRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { cuisine ->
                                val isSelected = selectedCuisines.contains(cuisine.id)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f) // RESTORED: This fixes the skinny button issue
                                        .neoClickable { viewModel.toggleCuisine(cuisine.id) },
                                    color = if (isSelected) Color(0xFF22C55E) else Color.White, // Changed Blue to NeoGreen
                                    border = rowBorder(3.dp, NeoBlack),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(cuisine.avatar, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            cuisine.name.replace("大师", ""), 
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else NeoBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Action
            val isReady = ingredients.isNotEmpty() && selectedCuisines.isNotEmpty()
            NeoButton(
                text = if (isLoading) "正在烹饪中..." else "开始创作",
                onClick = {
                    if (isReady && !isLoading) {
                        viewModel.generateRecipes()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                backgroundColor = if (isReady) Color(0xFF9CA3AF) else Color(0xFFF3F4F6),
                shadowOffset = 4.dp, // Reduced from 6.dp to 4.dp
                enabled = !isLoading
            )
        }

        // Results Section
        if (cuisineSlots.isNotEmpty()) {
            LaunchedEffect(cuisineSlots.size) {
                 // Scroll when slots are first created (generation starts)
                 scrollState.animateScrollTo(scrollState.maxValue)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. 菜谱结果", fontWeight = FontWeight.Black, color = NeoBlack, fontSize = 18.sp)
                
                cuisineSlots.forEach { slot ->
                    if (slot.recipe != null) {
                        RecipeCard(
                            recipe = slot.recipe,
                            isFavorite = viewModel.isFavorite(slot.recipe.id),
                            isGenerating = viewModel.isGenerating(slot.recipe.id),
                            onFavoriteClick = { viewModel.toggleFavorite(slot.recipe) },
                            onGenerateImage = {
                                Toast.makeText(context, "正在为 '${slot.recipe.name}' 创作图鉴...", Toast.LENGTH_LONG).show()
                                viewModel.generateImage(slot.recipe) { url ->
                                    if (url != null) {
                                        Toast.makeText(context, "创作成功！已收录至 GALLERY", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "创作失败，请检查设置或重试", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else if (slot.isLoading) {
                        NeoCard(modifier = Modifier.fillMaxWidth()) {
                            Text("${slot.name} 创作中...", fontWeight = FontWeight.Black, color = NeoBlack)
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(8.dp).border(1.dp, NeoBlack, RoundedCornerShape(4.dp)),
                                color = Color(0xFF22C55E), // Changed Blue to NeoGreen
                                trackColor = Color.White
                            )
                        }
                    }
                }
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
                                data = android.net.Uri.fromParts("package", context.packageName, null)
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
    }
}

private fun rowBorder(width: Dp, color: Color) = BorderStroke(width, color)
