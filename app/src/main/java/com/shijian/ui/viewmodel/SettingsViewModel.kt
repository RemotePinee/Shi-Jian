package com.shijian.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijian.data.repository.AiRepository
import com.shijian.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiProviderPreset(
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val isVisionSupported: Boolean = false
)

val PROVIDER_PRESETS = listOf(
    AiProviderPreset("OpenAI", "https://api.openai.com/v1/", "gpt-4o", true),
    AiProviderPreset("DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
    AiProviderPreset("智谱 AI", "https://open.bigmodel.cn/api/paas/v4/", "glm-4", true),
    AiProviderPreset("Kimi", "https://api.moonshot.cn/v1/", "moonshot-v1-8k"),
    AiProviderPreset("豆包 (Ark)", "https://ark.cn-beijing.volces.com/api/v3/", "ep-...", true),
    AiProviderPreset("Groq", "https://api.groq.com/openai/v1/", "llama3-70b-8192")
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _chatProvider = mutableStateOf(repository.chatProvider)
    val chatProvider: State<String> = _chatProvider

    private val _visionProvider = mutableStateOf(repository.visionProvider)
    val visionProvider: State<String> = _visionProvider

    private val _imageProvider = mutableStateOf(repository.imageProvider)
    val imageProvider: State<String> = _imageProvider

    private val _imageApiKey = mutableStateOf(repository.imageApiKey)
    val imageApiKey: State<String> = _imageApiKey

    private val _imageBaseUrl = mutableStateOf(repository.imageBaseUrl)
    val imageBaseUrl: State<String> = _imageBaseUrl

    private val _chatApiKey = mutableStateOf(repository.chatApiKey)
    val chatApiKey: State<String> = _chatApiKey

    private val _chatBaseUrl = mutableStateOf(repository.chatBaseUrl)
    val chatBaseUrl: State<String> = _chatBaseUrl

    private val _visionApiKey = mutableStateOf(repository.visionApiKey)
    val visionApiKey: State<String> = _visionApiKey

    private val _visionBaseUrl = mutableStateOf(repository.visionBaseUrl)
    val visionBaseUrl: State<String> = _visionBaseUrl

    private val _chatModel = mutableStateOf(repository.chatModel)
    val chatModel: State<String> = _chatModel

    private val _visionModel = mutableStateOf(repository.visionModel)
    val visionModel: State<String> = _visionModel

    private val _imageModel = mutableStateOf(repository.imageModel)
    val imageModel: State<String> = _imageModel

    private val _imageModelList = mutableStateListOf<String>()
    val imageModelList: List<String> = _imageModelList

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> = _isSaved

    private val _chatModelList = mutableStateListOf<String>()
    val chatModelList: List<String> = _chatModelList

    private val _visionModelList = mutableStateListOf<String>()
    val visionModelList: List<String> = _visionModelList

    private val _isFetchingChat = MutableStateFlow(false)
    val isFetchingChat = _isFetchingChat.asStateFlow()
    
    private val _isFetchingVision = MutableStateFlow(false)
    val isFetchingVision = _isFetchingVision.asStateFlow()
    
    private val _isFetchingImage = MutableStateFlow(false)
    val isFetchingImage = _isFetchingImage.asStateFlow()

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun clearError() { _errorMessage.value = null }


    fun updateApiKey(value: String) { _imageApiKey.value = value; _isSaved.value = false }
    fun updateBaseUrl(value: String) { _imageBaseUrl.value = value; _isSaved.value = false }
    fun updateChatApiKey(value: String) { _chatApiKey.value = value; _isSaved.value = false }
    fun updateVisionApiKey(value: String) { _visionApiKey.value = value; _isSaved.value = false }
    fun updateChatBaseUrl(value: String) { 
        _chatBaseUrl.value = value 
        _isSaved.value = false 
    }
    fun updateVisionBaseUrl(value: String) { 
        _visionBaseUrl.value = value 
        _isSaved.value = false 
    }

    fun updateChatModel(value: String) { _chatModel.value = value; _isSaved.value = false }
    fun updateVisionModel(value: String) { _visionModel.value = value; _isSaved.value = false }
    fun updateImageModel(value: String) { _imageModel.value = value; _isSaved.value = false }

    fun updateChatProvider(value: String) { _chatProvider.value = value; _isSaved.value = false }
    fun updateVisionProvider(value: String) { _visionProvider.value = value; _isSaved.value = false }
    fun updateImageProvider(value: String) { _imageProvider.value = value; _isSaved.value = false }

    fun fetchModels(type: Int) { // 0: Chat, 1: Vision, 2: Image
        viewModelScope.launch {
            setLoading(type, true)
            _errorMessage.value = null
            val url = when(type) {
                1 -> _visionBaseUrl.value
                2 -> _imageBaseUrl.value
                else -> _chatBaseUrl.value
            }
            val key = when(type) {
                1 -> _visionApiKey.value
                2 -> _imageApiKey.value
                else -> _chatApiKey.value
            }
            
            aiRepository.fetchModels(url, key).onSuccess { models ->
                when(type) {
                    1 -> {
                        _visionModelList.clear()
                        _visionModelList.addAll(models)
                    }
                    2 -> {
                        _imageModelList.clear()
                        _imageModelList.addAll(models)
                    }
                    else -> {
                        _chatModelList.clear()
                        _chatModelList.addAll(models)
                    }
                }
                if (models.isEmpty()) {
                    _errorMessage.value = "未获取到模型列表，请确认 API Key 和 Base URL 是否正确"
                }
            }.onFailure { e ->
                _errorMessage.value = "获取失败: ${e.message}"
            }
            setLoading(type, false)
        }
    }

    private fun setLoading(type: Int, value: Boolean) {
        when(type) {
            0 -> _isFetchingChat.value = value
            1 -> _isFetchingVision.value = value
            2 -> _isFetchingImage.value = value
        }
    }

    fun saveSettings() {
        repository.imageApiKey = _imageApiKey.value
        repository.imageBaseUrl = _imageBaseUrl.value
        repository.chatApiKey = _chatApiKey.value
        repository.chatBaseUrl = _chatBaseUrl.value
        repository.visionApiKey = _visionApiKey.value
        repository.visionBaseUrl = _visionBaseUrl.value
        repository.chatModel = _chatModel.value
        repository.visionModel = _visionModel.value
        repository.imageModel = _imageModel.value

        repository.chatProvider = _chatProvider.value
        repository.visionProvider = _visionProvider.value
        repository.imageProvider = _imageProvider.value
        
        _isSaved.value = true
    }

    fun applyPreset(preset: AiProviderPreset, type: Int) { // 0: Chat, 1: Vision, 2: Image
        val isArk = preset.name.contains("豆包")
        when(type) {
            0 -> {
                _chatBaseUrl.value = preset.baseUrl
                _chatModel.value = preset.defaultModel
                _chatProvider.value = preset.name
                if (!isArk && _chatApiKey.value.isNotBlank()) fetchModels(0)
            }
            1 -> {
                _visionBaseUrl.value = preset.baseUrl
                _visionModel.value = preset.defaultModel
                _visionProvider.value = preset.name
                if (!isArk && _visionApiKey.value.isNotBlank()) fetchModels(1)
            }
            2 -> {
                _imageBaseUrl.value = preset.baseUrl
                _imageModel.value = preset.defaultModel
                _imageProvider.value = preset.name
                if (!isArk && _imageApiKey.value.isNotBlank()) fetchModels(2)
            }
        }
        _isSaved.value = false
    }

    fun testConnection(type: Int) {
        viewModelScope.launch {
            setLoading(type, true)
            _errorMessage.value = null
            
            val url = when(type) {
                1 -> _visionBaseUrl.value
                2 -> _imageBaseUrl.value
                else -> _chatBaseUrl.value
            }
            val key = when(type) {
                1 -> _visionApiKey.value
                2 -> _imageApiKey.value
                else -> _chatApiKey.value
            }
            val model = when(type) {
                1 -> _visionModel.value
                2 -> _imageModel.value
                else -> _chatModel.value
            }

            // Simple "Hello" test (Vision models need a dummy image)
            aiRepository.testConnection(url, key, model, isVision = (type == 1)).onSuccess {
                _errorMessage.value = "连接成功！接口响应正常。"
            }.onFailure { e ->
                _errorMessage.value = "连接失败: ${e.message}"
            }
            setLoading(type, false)
        }
    }
}
