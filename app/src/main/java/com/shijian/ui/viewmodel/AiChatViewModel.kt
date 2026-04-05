package com.shijian.ui.viewmodel

import android.net.Uri

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijian.data.api.ChatMessage
import com.shijian.data.repository.AiRepository
import com.shijian.data.repository.ChatRepository
import com.shijian.data.model.ChatMessageItem
import com.shijian.data.model.ChatSessionSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import com.shijian.data.api.ChatContentPart
import com.shijian.data.api.ImageUrlPart
import java.util.UUID
import com.shijian.data.repository.FavoriteRepository
import com.shijian.data.model.Recipe
import com.shijian.data.model.FavoriteRecipe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.Gson

import androidx.lifecycle.SavedStateHandle

class AiChatViewModel(
    private val aiRepository: AiRepository,
    private val chatRepository: ChatRepository,
    private val favoriteRepository: FavoriteRepository,
    private val settingsRepository: com.shijian.data.repository.SettingsRepository,
    private val galleryRepository: com.shijian.data.repository.GalleryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    
    companion object {
        private const val KEY_SESSION_ID = "current_session_id"
    }

    private val _messages = mutableStateListOf<ChatMessageItem>()
    val messages: List<ChatMessageItem> = _messages

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // Reactive trigger for favorite status updates
    private val _favoritesVersion = mutableIntStateOf(0)
    val favoritesVersion: State<Int> = _favoritesVersion

    private val _currentSessionId = mutableStateOf(
        savedStateHandle.get<String>(KEY_SESSION_ID) ?: UUID.randomUUID().toString()
    )
    val currentSessionId: State<String> = _currentSessionId

    private val _sessions = mutableStateListOf<ChatSessionSummary>()
    val sessions: List<ChatSessionSummary> = _sessions

    private val _hasHistory = mutableStateOf(settingsRepository.hasChatHistory)
    val hasHistory: State<Boolean> = _hasHistory

    private var messageJob: Job? = null
    private var aiJob: Job? = null

    init {
        observeSessions()
        observeMessages(_currentSessionId.value)
    }

    private fun observeSessions() {
        viewModelScope.launch {
            chatRepository.getSessionSummaries().collectLatest { allSummaries ->
                _sessions.clear()
                _sessions.addAll(allSummaries)
                val hasNow = allSummaries.isNotEmpty()
                _hasHistory.value = hasNow
                settingsRepository.hasChatHistory = hasNow
            }
        }
    }

    private fun observeMessages(sessionId: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            // Revert to stable snapshot load to avoid concurrent list modification crashes
            val history = chatRepository.getMessagesInitial(sessionId)
            _messages.clear()
            _messages.addAll(history)
        }
    }

    fun switchSession(sessionId: String) {
        _currentSessionId.value = sessionId
        savedStateHandle[KEY_SESSION_ID] = sessionId
        observeMessages(sessionId)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewChat()
            }
        }
    }

    fun startNewChat() {
        stopGeneration() // Thoroughly terminate any pending AI response
        val newId = UUID.randomUUID().toString()
        _currentSessionId.value = newId
        savedStateHandle[KEY_SESSION_ID] = newId
        observeMessages(newId)
    }

    fun stopGeneration() {
        aiJob?.cancel()
        aiJob = null
        _isLoading.value = false
        // Optionally add a "Terminated" marker to the last message if it was incomplete
    }


    fun sendMessage(text: String, imageUri: Uri? = null) {
        if ((text.isBlank() && imageUri == null) || _isLoading.value) return

        val sessionId = _currentSessionId.value
        val userMsgItem = ChatMessageItem(
            sessionId = sessionId, 
            content = text, 
            isUser = true,
            imageUri = imageUri?.toString()
        )
        
        // Add to UI state immediately for instant feedback
        _messages.add(userMsgItem)
        
        // Background: Generate vision summary for future context (Plan 2)
        if (imageUri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val summaryResult = aiRepository.getImageSummary(imageUri.toString())
                    summaryResult.onSuccess { summary ->
                        chatRepository.updateMessageSummary(userMsgItem.id, summary)
                        // Also update the local list if it's still in the current session
                        val index = _messages.indexOfFirst { it.id == userMsgItem.id }
                        if (index != -1) {
                            _messages[index] = _messages[index].copy(imageSummary = summary)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AiChatViewModel", "Failed to generate image summary", e)
                }
            }
        }
        
        aiJob = viewModelScope.launch {
            chatRepository.saveMessage(userMsgItem)
            _isLoading.value = true

            val apiMessages = _messages.takeLast(10).map { 
                val isLatestUserMessage = it.id == userMsgItem.id
                
                if (it.imageUri != null) {
                    // Plan 2: Only send Base64 for the LATEST user message
                    // For history, send the text summary instead to save tokens
                    if (isLatestUserMessage) {
                        val base64 = aiRepository.uriToBase64(it.imageUri)
                        if (base64 != null) {
                            ChatMessage(
                                role = if (it.isUser) "user" else "assistant",
                                content = listOf(
                                    ChatContentPart(type = "text", text = it.content),
                                    ChatContentPart(type = "image_url", imageUrl = ImageUrlPart(url = "data:image/jpeg;base64,$base64"))
                                )
                            )
                        } else {
                            ChatMessage(role = if (it.isUser) "user" else "assistant", content = it.content)
                        }
                    } else {
                        // History message: Use summary if available, otherwise just text
                        val summaryText = if (it.imageSummary != null) {
                            "${it.content}\n\n[图片背景描述: ${it.imageSummary}]"
                        } else {
                            it.content
                        }
                        ChatMessage(role = if (it.isUser) "user" else "assistant", content = summaryText)
                    }
                } else {
                    ChatMessage(role = if (it.isUser) "user" else "assistant", content = it.content)
                }
            }
            
            val systemMsg = ChatMessage(
                role = "system", 
                content = """
                    你是一位拥有 30 年经验的、充满热情的顶级私人厨师，外号“厨神 AI”。
                    你的知识和回答范围**仅限于**：食材介绍、食物搭配、做菜技法、精美菜谱、饮食文化、营养建议等与“吃”相关的话题。

                    ### 核心约束（最高优先级）：
                    - **领域锁定**：如果用户提出的问题与上述美食领域无关（例如：询问天气、写代码、翻译非菜谱内容、政治、娱乐或其他日常闲聊），你必须礼貌且委婉地拒绝。
                    - **拒绝模板**：你可以说：“作为您的专属‘厨神 AI’，我的热情全在锅碗瓢盆和食材调味里。关于这个话题我可能不在行，不如我们聊聊今天想吃点什么，或者我教您做道拿手菜？”

                    ### 核心响应准则：
                    - **即时响应**：你必须优先回答用户的烹饪相关问题。如果是新会话，禁止只发送欢迎语而不理会用户指令。
                    
                    ### 你的性格特点：
                    - 专业风范：对食材和技法了如指掌。
                    - 充满活力：说话风趣，多用美食比喻激励用户。
                    ### 你的回复准则：
                    1. 饥饿救星：若用户不知道吃什么，给出 3 个方案（硬菜、小炒、开胃汤）。
                    2. 食材炼金术：对剩余食材给出最经典或最创意的搭配。
                    3. 厨神秘籍 (Chef's Secret)：做法必含关乎灵魂的火候或调味细节。
                    4. 视觉洞察：图片识别带有点评和改良建议。
                    5. 排版要求：分点列表，关键步骤加粗。使用标准 Markdown 格式（如 ### 标题，**加粗**）。
                    
                    ### 菜谱结构化输出（极重要）：
                    - 当你给出一个完整的菜谱时，必须在回复的末尾附带一个结构化的 JSON 数据块，并用 [RECIPE] 和 [/RECIPE] 标签包裹。
                    - JSON 格式必须严格符合以下结构：
                      "id": "一串唯一的字符串（如 UUID）",
                      "name": "菜品名称",
                      "cuisine": "菜系名称",
                      "ingredients": ["食材1", "食材2"],
                      "steps": [{"step": 1, "description": "步骤描述"}, {"step": 2, "description": "步骤描述"}],
                      "cookingTime": 20,
                      "difficulty": "medium",
                      "tips": ["小贴士1", "小贴士2"]
                    }
                    - 注意：JSON 块必须隐藏在标签内，用户不需要直接看到原始 JSON。
                """.trimIndent()
            )
            
            val aiMsgId = UUID.randomUUID().toString()
            val aiTimestamp = System.currentTimeMillis() + 10 // Guaranteed after user message
            val aiMsgItem = ChatMessageItem(
                id = aiMsgId, 
                sessionId = sessionId, 
                content = "厨师正在思考...", 
                isUser = false,
                timestamp = aiTimestamp
            )
            
            // 2. SAVE SHELL TO DATABASE IMMEDIATELY (Outside try block)
            // This ensures the record exists in history no matter what happens next
            chatRepository.saveMessage(aiMsgItem)
            
            // Add to UI state (though the Flow observer will also handle it eventually)
            _messages.add(aiMsgItem)

            var fullContent = ""
            var hasReceivedTokens = false
            var isActuallyCancelled = false
            var lastUpdateMillis = System.currentTimeMillis()
            var lastDbSaveMillis = System.currentTimeMillis()
            
            try {
                aiRepository.chatStream(listOf(systemMsg) + apiMessages, imageUri?.toString()).collect { token ->
                    hasReceivedTokens = true
                    fullContent += token
                    
                    // Throttle UI list updates to ~100ms
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMillis > 100) {
                        val index = _messages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            val displayContent = if (fullContent.contains("[RECIPE]")) {
                                fullContent.substringBefore("[RECIPE]").trim()
                            } else {
                                fullContent
                            }
                            val isRecipeLoading = fullContent.contains("[RECIPE]") && !fullContent.contains("[/RECIPE]")
                            val recipeJson = extractRecipe(fullContent, aiMsgId)
                            _messages[index] = _messages[index].copy(
                                content = displayContent, 
                                isRecipeLoading = isRecipeLoading,
                                recipeJson = recipeJson
                            )
                        }
                        lastUpdateMillis = now
                    }
                    
                    // 2. Periodic Database Sync (every 2 seconds)
                    // Reduces flicker in history if task is suddenly killed or switched
                    if (now - lastDbSaveMillis > 2000) {
                        val recipeJson = extractRecipe(fullContent, aiMsgId)
                        val displayContent = if (fullContent.contains("[RECIPE]")) {
                            fullContent.substringBefore("[RECIPE]").trim()
                        } else {
                            fullContent
                        }
                        
                        val midMsg = aiMsgItem.copy(
                            content = displayContent,
                            recipeJson = recipeJson,
                            isRecipeLoading = fullContent.contains("[RECIPE]") && !fullContent.contains("[/RECIPE]")
                        )
                        chatRepository.saveMessage(midMsg)
                        lastDbSaveMillis = now
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                isActuallyCancelled = true
                throw e
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误"
                val errorText = if (errorMsg.contains("配置") || errorMsg.contains("Key")) {
                    "🍳 厨师提醒：$errorMsg"
                } else {
                    "抱歉，由于网络波动，我现在的思绪有点乱：$errorMsg"
                }
                val eIndex = _messages.indexOfFirst { it.id == aiMsgId }
                if (eIndex != -1) {
                    _messages[eIndex] = _messages[eIndex].copy(content = errorText)
                }
            } finally {
                // 3. FINAL ROBUST PERSISTENCE (NonCancellable)
                withContext(NonCancellable) {
                    // Extract data from LOCAL variable fullContent
                    var displayContent = if (fullContent.contains("[RECIPE]")) {
                        fullContent.substringBefore("[RECIPE]").trim()
                    } else {
                        fullContent
                    }

                    // Handle termination cases using local flags
                    if (isActuallyCancelled) {
                        displayContent = if (displayContent.isBlank() || displayContent == "厨师正在思考...") {
                            "对话已取消"
                        } else {
                            "$displayContent [已终止]"
                        }
                    } else if (!hasReceivedTokens && (displayContent.isBlank() || displayContent == "厨师正在思考...")) {
                        // Recover error message set in catch block if tokens never arrived
                        val recovered = _messages.find { it.id == aiMsgId }?.content
                        if (recovered != null && recovered != "厨师正在思考...") {
                            displayContent = recovered
                        }
                    }
                    
                    val recipeJson = extractRecipe(fullContent, aiMsgId)
                    val finalMsg = ChatMessageItem(
                        id = aiMsgId,
                        sessionId = sessionId,
                        content = displayContent,
                        isUser = false,
                        recipeJson = recipeJson,
                        isRecipeLoading = false,
                        timestamp = aiTimestamp
                    )
                    
                    // Update UI state list (Critical for "Layout Loading" to disappear)
                    val index = _messages.indexOfFirst { it.id == aiMsgId }
                    if (index != -1) {
                        _messages[index] = finalMsg
                    }
                    
                    // Update Database
                    chatRepository.saveMessage(finalMsg)
                    
                    _isLoading.value = false
                    aiJob = null
                }
            }
        }
    }

    private fun extractRecipe(content: String, messageId: String): String? {
        val regex = Regex("""\[RECIPE]([\s\S]*?)\[/RECIPE]""")
        val match = regex.find(content)
        val rawJson = match?.groupValues?.get(1)?.trim() ?: return null
        
        // Ensure ID is unique and stable for this message
        try {
            val recipe: Recipe? = Gson().fromJson(rawJson, Recipe::class.java)
            if (recipe != null && recipe.id.isBlank()) {
                val updatedRecipe = recipe.copy(id = "recipe_$messageId")
                return Gson().toJson(updatedRecipe)
            }
        } catch (_: Exception) {}
        
        return rawJson
    }

    fun isFavorite(recipeId: String): Boolean {
        return favoriteRepository.isFavorite(recipeId)
    }

    fun toggleFavorite(recipe: Recipe) {
        if (isFavorite(recipe.id)) {
            favoriteRepository.removeFavorite(recipe.id)
        } else {
            val favorite = FavoriteRecipe(
                id = UUID.randomUUID().toString(),
                recipe = recipe,
                favoriteDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
            favoriteRepository.addFavorite(favorite)
        }
        
        // Trigger UI update
        _favoritesVersion.intValue++
        
        // Match existing messages to trigger local update
        val affectedIndices = _messages.indices.filter { _messages[it].recipeJson?.contains(recipe.id) == true }
        affectedIndices.forEach { idx ->
            _messages[idx] = _messages[idx].copy() // Trigger a shallow copy update
        }
    }

    fun generateImage(message: ChatMessageItem) {
        val recipeJson = message.recipeJson ?: return
        val recipe = try { Gson().fromJson(recipeJson, Recipe::class.java) } catch (_: Exception) { null } ?: return

        // Check if image config exists
        if (settingsRepository.imageApiKey.isBlank() || settingsRepository.imageBaseUrl.isBlank()) {
            val index = _messages.indexOfFirst { it.id == message.id }
            if (index != -1) {
                // If it's a configuration error, we append a hint to content (AiChatScreen will show button)
                if (!message.content.contains("🍳 厨师提醒")) {
                     val updated = message.copy(
                        content = message.content + "\n\n🍳 厨师提醒：请先在设置中完整配置图片生成的地址和 Key，以便为您创作精美图鉴。"
                    )
                    _messages[index] = updated
                    viewModelScope.launch(Dispatchers.IO) {
                        chatRepository.saveMessage(updated)
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            // Set loading state
            updateMessageState(message.id, isLoading = true)

            val result = aiRepository.generateImage(recipe)
            result.onSuccess { url ->
                if (url.isNotBlank()) {
                    // 1. Initial UI update with online URL for speed
                    updateMessageState(message.id, isLoading = false, imageUrl = url)
                    
                    // 2. SYNC TO GLOBAL GALLERY & DOWNLOAD
                    try {
                        val localPath = galleryRepository.addToGalleryWithDownload(
                            id = "chat-img-${message.id}",
                            url = url,
                            recipeName = recipe.name,
                            recipeId = recipe.id,
                            cuisine = recipe.cuisine,
                            ingredients = recipe.ingredients,
                            generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        )
                        
                        // 3. CONVERT TO PERMANENT LOCAL PATH if download was successful
                        if (localPath != null) {
                            updateMessageState(message.id, isLoading = false, imageUrl = localPath)
                            Log.d("AiChatViewModel", "Localized chat image: $localPath")
                        }
                    } catch (e: Exception) {
                        Log.e("AiChatViewModel", "Gallery sync/download failed", e)
                    }
                } else {
                    updateMessageState(message.id, isLoading = false)
                }
            }.onFailure { error ->

                Log.e("AiChatViewModel", "Generation failed: ${error.message}")
                updateMessageState(message.id, isLoading = false)
            }
        }
    }

    private fun updateMessageState(messageId: String, isLoading: Boolean, imageUrl: String? = null): ChatMessageItem? {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val updated = _messages[index].copy(
                isImageLoading = isLoading,
                generatedImageUrl = imageUrl ?: _messages[index].generatedImageUrl
            )
            _messages[index] = updated
            // Persist to DB asynchronously
            viewModelScope.launch(Dispatchers.IO) {
                chatRepository.saveMessage(updated)
            }
            return updated
        }
        return null
    }
}

