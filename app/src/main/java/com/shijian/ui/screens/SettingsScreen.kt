package com.shijian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import android.widget.Toast
import android.provider.Settings
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.shijian.ui.components.*
import com.shijian.ui.viewmodel.*
import androidx.compose.ui.Alignment

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null
) {
    val imageApiKey by viewModel.imageApiKey
    val imageBaseUrl by viewModel.imageBaseUrl
    val chatApiKey by viewModel.chatApiKey
    val chatBaseUrl by viewModel.chatBaseUrl
    val visionApiKey by viewModel.visionApiKey
    val visionBaseUrl by viewModel.visionBaseUrl
    val chatModel by viewModel.chatModel
    val visionModel by viewModel.visionModel
    val imageModel by viewModel.imageModel
    val isSaved by viewModel.isSaved
    val chatModelList = viewModel.chatModelList
    val visionModelList = viewModel.visionModelList
    val imageModelList = viewModel.imageModelList
    val isFetchingChat by viewModel.isFetchingChat.collectAsState()
    val isFetchingVision by viewModel.isFetchingVision.collectAsState()
    val isFetchingImage by viewModel.isFetchingImage.collectAsState()
    val errorMessage by viewModel.errorMessage
    val context = LocalContext.current
    
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Clear focus when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFACC15)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        NeoHeader(
            title = "系统设置",
            subtitle = "配置您的 AI 大脑和接口地址",
            backgroundColor = Color(0xFF9CA3AF),
            onBack = onBack
        )

        // Chat Section
        ApiConfigCard(
            title = "AI 对话配置 (Chat)",
            backgroundColor = Color(0xFFFEE2E2),
            apiKey = chatApiKey,
            onApiKeyChange = { viewModel.updateChatApiKey(it) },
            baseUrl = chatBaseUrl,
            onBaseUrlChange = { viewModel.updateChatBaseUrl(it) },
            model = chatModel,
            onModelChange = { viewModel.updateChatModel(it) },
            defaultModels = chatModelList,
            onFetchModels = { viewModel.fetchModels(0) },
            isFetching = isFetchingChat,
            onPresetSelected = { viewModel.applyPreset(it, 0) },
            providerType = viewModel.chatProvider.value,
            onProviderTypeChange = { viewModel.updateChatProvider(it) },
            onTestConnection = { viewModel.testConnection(0) }
        )

        // Vision Section
        ApiConfigCard(
            title = "AI 识别配置 (Vision)",
            backgroundColor = Color(0xFFDCFCE7),
            apiKey = visionApiKey,
            onApiKeyChange = { viewModel.updateVisionApiKey(it) },
            baseUrl = visionBaseUrl,
            onBaseUrlChange = { viewModel.updateVisionBaseUrl(it) },
            model = visionModel,
            onModelChange = { viewModel.updateVisionModel(it) },
            defaultModels = visionModelList,
            onFetchModels = { viewModel.fetchModels(1) },
            isFetching = isFetchingVision,
            onPresetSelected = { viewModel.applyPreset(it, 1) },
            providerType = viewModel.visionProvider.value,
            onProviderTypeChange = { viewModel.updateVisionProvider(it) },
            onTestConnection = { viewModel.testConnection(1) }
        )

        // Image Section
        ApiConfigCard(
            title = "AI 图片生成配置 (Image)",
            backgroundColor = Color(0xFFE0F2FE),
            apiKey = imageApiKey,
            onApiKeyChange = { viewModel.updateApiKey(it) },
            baseUrl = imageBaseUrl,
            onBaseUrlChange = { viewModel.updateBaseUrl(it) },
            model = imageModel,
            onModelChange = { viewModel.updateImageModel(it) },
            defaultModels = imageModelList,
            onFetchModels = { viewModel.fetchModels(2) },
            isFetching = isFetchingImage,
            onPresetSelected = { viewModel.applyPreset(it, 2) },
            providerType = viewModel.imageProvider.value,
            onProviderTypeChange = { viewModel.updateImageProvider(it) },
            onTestConnection = { viewModel.testConnection(2) }
        )

        NeoButton(
            text = if (isSaved) "✅ 已保存" else "💾 保存配置",
            onClick = { viewModel.saveSettings() },
            backgroundColor = if (isSaved) Color(0xFF4ADE80) else Color(0xFFA78BFA),
            modifier = Modifier.fillMaxWidth()
        )

        // Background Survival Guidance Section
        NeoCard(
            title = "后台保活与稳定性",
            backgroundColor = Color(0xFFF3F4F6)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "如果应用频繁后台断线或重启，请尝试以下操作：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                
                NeoButton(
                    text = "⚙️ 打开系统电池优化设置",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    backgroundColor = Color(0xFF9CA3AF),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "建议：在系统设置中将本应用设为“不限制”电池使用，并允许“自启动”。",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigCard(
    title: String,
    backgroundColor: Color,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    defaultModels: List<String>,
    onFetchModels: () -> Unit,
    isFetching: Boolean,
    onPresetSelected: (AiProviderPreset) -> Unit,
    providerType: String,
    onProviderTypeChange: (String) -> Unit,
    onTestConnection: () -> Unit
) {

    NeoCard(
        title = title,
        backgroundColor = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Fill Presets
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "快速填充预设:", 
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                // First Row (3 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PROVIDER_PRESETS.take(3).forEach { preset ->
                        Surface(
                            onClick = { onPresetSelected(preset) },
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                            color = Color.White,
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(preset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                // Second Row (3 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PROVIDER_PRESETS.drop(3).forEach { preset ->
                        Surface(
                            onClick = { onPresetSelected(preset) },
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                            color = Color.White,
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(preset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("接口协议:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("OpenAI", "智谱 AI", "豆包 (Ark)").forEach { p ->
                        val selected = if (p == "OpenAI") {
                            providerType == "OpenAI" || providerType == "DeepSeek" || providerType == "Kimi" || providerType == "Groq"
                        } else {
                            providerType == p
                        }
                        Surface(
                            onClick = { onProviderTypeChange(p) },
                            shape = RoundedCornerShape(4.dp),
                            color = if (selected) NeoBlack else Color.Transparent,
                            border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray) else null,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = p,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (selected) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key", fontWeight = FontWeight.Bold) },
                placeholder = { Text("sk-...") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL", fontWeight = FontWeight.Bold) },
                placeholder = { Text("https://api.openai-hk.com/v1/") },
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            var showModelDialog by remember { mutableStateOf(false) }
            val isArk = providerType.contains("豆包")

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = model,
                    onValueChange = onModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { 
                        Text(
                            if (isArk) "接入点 ID (Endpoint ID)" else "模型名称", 
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    placeholder = { 
                        Text(if (isArk) "ep-2024..." else "输入或选择模型...") 
                    },
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = { 
                        IconButton(onClick = { showModelDialog = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = NeoBlack)
                        }
                    },
                    singleLine = true
                )
                if (isArk) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "🚨 豆包 (Ark) 必读：请填写 接入点 ID (如 ep-xxx) 而非模型名。请确保已部署 vision 模型。",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                NeoButton(
                    text = if (isFetching) "..." else "⚡ 测试连接",
                    onClick = onTestConnection,
                    backgroundColor = Color(0xFF93C5FD),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showModelDialog) {
                ModelSelectionDialog(
                    title = title,
                    models = defaultModels,
                    selectedModel = model,
                    isFetching = isFetching,
                    onFetchModels = onFetchModels,
                    onModelSelect = { 
                        onModelChange(it)
                        showModelDialog = false 
                    },
                    onDismiss = { showModelDialog = false }
                )
            }
        }
    }
}

@Composable
fun ModelSelectionDialog(
    title: String,
    models: List<String>,
    selectedModel: String,
    isFetching: Boolean,
    onFetchModels: () -> Unit,
    onModelSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Auto-fetch if list is empty when opened
    LaunchedEffect(Unit) {
        if (models.isEmpty()) {
            onFetchModels()
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            title = "选择模型 - $title",
            backgroundColor = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 500.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("可用列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    TextButton(onClick = onFetchModels, enabled = !isFetching) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isFetching) "获取中..." else "刷新列表")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isFetching && models.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (models.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("暂无数据，请尝试刷新", color = Color.Gray)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(models.size) { index ->
                            val modelName = models[index]
                            val isSelected = modelName == selectedModel
                            
                            Surface(
                                onClick = { onModelSelect(modelName) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFA78BFA) else Color(0xFFF3F4F6),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = modelName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                NeoButton(
                    text = "取消",
                    onClick = onDismiss,
                    backgroundColor = Color(0xFF9CA3AF),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
