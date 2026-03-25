@file:Suppress("SpellCheckingInspection")
package com.eatwhat.data.repository

import com.eatwhat.data.api.*
import com.eatwhat.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException

import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import android.util.Log
import java.io.File
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.net.SocketTimeoutException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Base64

class AiRepository(private val context: Context, private val settings: SettingsRepository) {
    private val gson = Gson()

    private suspend fun <T> callAi(
        systemMsg: String, 
        userPrompt: String, 
        clazz: Class<T>, 
        isVision: Boolean = false,
        imageUri: String? = null
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val model = (if (isVision) settings.visionModel else settings.chatModel).lowercase().trim()
            val baseUrl = if (isVision) settings.visionBaseUrl else settings.chatBaseUrl
            val apiKey = if (isVision) settings.visionApiKey else settings.chatApiKey

            if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
                val category = if (isVision) "识别 (Vision)" else "对话 (Chat)"
                return@withContext Result.failure(Exception("请先在设置中配置 $category 的接口地址、Key 和模型"))
            }

            val api = ApiClient.getService(baseUrl, apiKey)
            
            // Construct messages based on provider and task type
            val messages = if (isVision && imageUri != null) {
                val base64 = uriToBase64(imageUri)
                if (base64.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("图片编码失败，请检查文件是否存在"))
                }
                
                val isArk = baseUrl.contains("volces.com") || baseUrl.contains("ark")
                
                if (isArk) {
                    // Both require merging system instructions into user message.
                    // Keep standard OpenAI names for the HTTP API.
                    val textType = "text"
                    val imageType = "image_url"
                    
                    listOf(
                        ChatMessage("user", listOf(
                            ChatContentPart(type = textType, text = "$systemMsg\n\n$userPrompt"),
                            ChatContentPart(type = imageType, imageUrl = ImageUrlPart(url = "data:image/jpeg;base64,$base64"))
                        ))
                    )
                } else {
                    // Standard OpenAI compatible format for other providers (GPT-4o, DeepSeek, etc.)
                    listOf(
                        ChatMessage("system", systemMsg),
                        ChatMessage("user", listOf(
                            ChatContentPart(type = "text", text = userPrompt),
                            ChatContentPart(type = "image_url", imageUrl = ImageUrlPart(url = "data:image/jpeg;base64,$base64"))
                        ))
                    )
                }
            } else {
                // Non-vision or missing image: standard system/user sequence
                listOf(
                    ChatMessage("system", systemMsg),
                    ChatMessage("user", userPrompt)
                )
            }

            // For vision tasks with Zhipu, we avoid sending temperature/max_tokens as they might cause 400
            val isZhipuVision = isVision && (baseUrl.contains("bigmodel.cn") || baseUrl.contains("zhipuai"))
            val request = ChatRequest(
                model = model,
                messages = messages,
                temperature = if (isZhipuVision) null else 0.7f,
                maxTokens = if (isZhipuVision) null else 2000,
                stream = if (isZhipuVision) null else false
            )

            val response = api.getChatCompletion(request)
            if (response.isSuccessful) {
                val rawContent = response.body()?.choices?.firstOrNull()?.message?.content?.toString() ?: ""
                val cleanJson = cleanJsonResponse(rawContent)
                Result.success(gson.fromJson(cleanJson, clazz))
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Log.e("AiRepository", "API Error (${response.code()}): $errorBody")
                
                // Add more context to 400 error for debugging
                val userError = if (response.code() == 400) {
                    "参数错误 (400): 请检查模型名 $model 是否正确支持识图，或尝试在设置中换一个视觉模型。详细：$errorBody"
                } else {
                    "API Error: ${response.code()} - $errorBody"
                }
                Result.failure(Exception(userError))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Exception in callAi: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun chatStream(messages: List<ChatMessage>): Flow<String> = flow {
        val model = settings.chatModel
        val baseUrl = settings.chatBaseUrl
        val apiKey = settings.chatApiKey

        if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            throw Exception("请先在设置中配置对话接口参数")
        }

        val api = ApiClient.getService(baseUrl, apiKey)
        val request = ChatRequest(
            model = model,
            messages = messages,
            temperature = 0.7f,
            maxTokens = 2000,
            stream = true
        )

        val responseBody = api.getChatCompletionStream(request)

        responseBody.use { body ->
            val reader = body.source().inputStream().bufferedReader()
            reader.use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    // 1. Explicitly check for cancellation in the I/O loop
                    currentCoroutineContext().ensureActive()
                    
                    val currentLine = line ?: break
                    if (currentLine.contains("data:")) {
                        val data = line.substringAfter("data:").trim()
                        if (data == "[DONE]") break
                        if (data.isEmpty()) continue
                        
                        try {
                            val chunk = gson.fromJson(data, ChatStreamResponse::class.java)
                            val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        } catch (ce: CancellationException) {
                            // 2. MUST RE-THROW CancellationException to terminate the Flow properly
                            throw ce
                        } catch (e: Exception) {
                            Log.e("AiRepository", "SSE Parse Error: ${e.message} on line: $line")
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    @Suppress("unused")
    suspend fun chat(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = settings.chatModel
            val baseUrl = settings.chatBaseUrl
            val apiKey = settings.chatApiKey

            if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
                return@withContext Result.failure(Exception("请先在设置中配置对话接口参数"))
            }

            val api = ApiClient.getService(baseUrl, apiKey)
            val request = ChatRequest(
                model = model,
                messages = messages,
                temperature = 0.7f,
                maxTokens = 2000,
                stream = false
            )
            val response = api.getChatCompletion(request)
            if (response.isSuccessful) {
                Result.success(response.body()?.choices?.firstOrNull()?.message?.content?.toString() ?: "")
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateRecipe(ingredients: List<String>, cuisine: CuisineType, customPrompt: String? = null): Result<Recipe> {
        var prompt = "${cuisine.prompt}\n\n用户提供的食材：${ingredients.joinToString("、")}"
        if (customPrompt != null) prompt += "\n\n用户的特殊要求：$customPrompt"
        
        prompt += """
            
            请生成一份详细实用的菜谱，要求：
            1. 食材清单要包含具体用量（如：猪肉300g、生抽2勺、盐1茶匙）
            2. 每个食材/调料必须是独立的数组项，严禁将多种调料用逗号或顿号合并成一个字符串（如：不要出现"调料：盐、生抽"这种合并项）。
            3. 制作步骤要详细具体，包含具体的操作方法、准确的时间控制、火候掌握及关键判断标准。
            4. 烹饪技巧要实用。
            
            请按照以下JSON格式返回菜谱：
            {
              "name": "菜品名称",
              "ingredients": ["主料1 300g", "调料1 2勺", "配菜1 100g"],
              "steps": [
                {
                  "step": 1,
                  "description": "详细的操作描述",
                  "time": 5,
                  "temperature": "中火"
                }
              ],
              "cookingTime": 30,
              "difficulty": "medium",
              "tips": ["技巧1"]
            }
        """.trimIndent()
        
        val systemMsg = "你是一位经验丰富的专业厨师。请严格按照JSON格式返回，不要包含任何其他文字。"
        val res = callAi(systemMsg, prompt, Recipe::class.java)
        
        return res.map { recipe ->
            recipe.copy(
                id = "recipe-${cuisine.id}-${System.currentTimeMillis()}",
                cuisine = cuisine.name,
                ingredients = sanitizeIngredients(recipe.ingredients)
            )
        }
    }

    private fun sanitizeIngredients(list: List<String>): List<String> {
        return list.flatMap { item ->
            // Split by common delimiters if the AI groups them
            val parts = item.split("[,，、]".toRegex())
            parts.map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { if (it.startsWith("调料：") || it.startsWith("调料:")) it.substring(3).trim() else it }
                .filter { it.isNotEmpty() }
        }
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String, isVision: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (baseUrl.isEmpty() || apiKey.isBlank()) {
                return@withContext Result.failure(Exception("请提供有效的地址和 Key"))
            }

            val api = ApiClient.getService(baseUrl, apiKey)
            
            val isArk = baseUrl.contains("volces.com") || baseUrl.contains("ark")
            
            val messages = if (isVision) {
                // Dynamically generate a 100x100 gradient JPEG to ensure it looks like a real image
                val bitmap = createBitmap(100, 100, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                val paint = Paint()
                paint.shader = LinearGradient(0f, 0f, 100f, 100f, Color.GREEN, Color.BLUE, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
                
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val dynamicBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                bitmap.recycle()
                
                val testPrompt = "这是一张待识别的食材图片。"
                
                if (isArk) {
                    listOf(
                        ChatMessage("user", listOf(
                            ChatContentPart(type = "text", text = testPrompt),
                            ChatContentPart(type = "image_url", imageUrl = ImageUrlPart(url = "data:image/jpeg;base64,$dynamicBase64"))
                        ))
                    )
                } else {
                    // Standard OpenAI format (system + user)
                    listOf(
                        ChatMessage("system", "你是一个极其专业的食材识别专家。"),
                        ChatMessage("user", listOf(
                            ChatContentPart(type = "text", text = testPrompt),
                            ChatContentPart(type = "image_url", imageUrl = ImageUrlPart(url = "data:image/jpeg;base64,$dynamicBase64"))
                        ))
                    )
                }
            } else {
                listOf(ChatMessage("user", "ping"))
            }

            val isZhipuVision = isVision && (baseUrl.contains("bigmodel.cn") || baseUrl.contains("zhipuai"))
            val request = ChatRequest(
                model = model.trim(),
                messages = messages,
                maxTokens = if (isZhipuVision) null else if (isVision) 2000 else 5,
                temperature = if (isZhipuVision) null else 0.1f,
                stream = if (isZhipuVision) null else false
            )
            val response = api.getChatCompletion(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = response.errorBody()?.string() ?: "未知错误"
                Log.e("AiRepository", "Test Connection Failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Test Connection Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Suppress("unused")
    suspend fun generateDishRecipeByName(dishName: String): Result<Recipe> {
        val prompt = """
            请为'$dishName'这道菜生成详细的制作教程。
            请按照以下JSON格式返回：
            {
              "name": "$dishName",
              "ingredients": ["主要食材1 200g", "必备调味1 1勺"],
              "steps": [
                {
                  "step": 1,
                  "description": "详细的操作步骤描述",
                  "time": 5,
                  "temperature": "中火"
                }
              ],
              "cookingTime": 25,
              "difficulty": "medium",
              "tips": ["成功要点1"]
            }
        """.trimIndent()
        
        val systemMsg = "你是一位经验丰富的专业厨师。请严格按照JSON格式返回，不要包含任何其他文字。"
        val res = callAi(systemMsg, prompt, Recipe::class.java)
        return res.map { it.copy(
            id = "dish-recipe-${System.currentTimeMillis()}", 
            cuisine = "推荐菜品",
            ingredients = sanitizeIngredients(it.ingredients)
        ) }
    }

    suspend fun generateSauceRecipe(sauceName: String): Result<SauceRecipe> {
        val prompt = """
            请为'$sauceName'这种酱料生成详细的制作配方。
            请按照以下JSON格式返回：
            {
              "name": "$sauceName",
              "category": "spicy",
              "ingredients": ["原料1 50g", "配料2 10g"],
              "steps": [{"step": 1, "description": "操作步骤描述", "time": 5, "temperature": "常温"}],
              "makingTime": 15,
              "difficulty": "easy",
              "tips": ["保存建议"],
              "storage": {"method": "冷藏", "duration": "7天", "temperature": "4℃"},
              "pairings": ["拌面", "蘸料"],
              "tags": ["家常", "万能"],
              "spiceLevel": 1,
              "sweetLevel": 1,
              "saltLevel": 2,
              "sourLevel": 1,
              "description": "酱料特色描述"
            }
        """.trimIndent()
        
        val systemMsg = "你是一位专业的酱料制作大师。请严格按照JSON格式返回，不要包含任何其他文字。"
        val res = callAi(systemMsg, prompt, SauceRecipe::class.java)
        return res.map { it.copy(id = "sauce-recipe-${System.currentTimeMillis()}") }
    }

    suspend fun recommendSauces(preferences: SaucePreference): Result<List<String>> = withContext(Dispatchers.IO) {
        val prompt = """
            根据偏好推荐酱料：
            - 辣度：${preferences.spiceLevel}/5, 甜度：${preferences.sweetLevel}/5, 咸度：${preferences.saltLevel}/5, 酸度：${preferences.sourLevel}/5
            - 场景：${preferences.useCase.joinToString("、")}
            - 现有食材：${preferences.availableIngredients.joinToString("、")}
            请推荐5-8种酱料名称。按JSON格式返回 {"recommendations": ["酱料1", "酱料2"]}
        """.trimIndent()
        
        val res = callAi("你是一位酱料推荐专家。", prompt, Map::class.java)
        res.map { (it["recommendations"] as? List<*>)?.map { item -> item.toString() } ?: emptyList() }
    }

    suspend fun generateDailyFortune(zodiac: String, animal: String): Result<FortuneResult> {
        val prompt = """
            星座：$zodiac，生肖：$animal。请结合星座生肖为用户推荐今日幸运菜并给出占卜理由。
            请按照以下JSON格式返回：
            {
              "dishName": "菜名",
              "reason": "占卜理由",
              "luckyIndex": 95,
              "description": "今日运势描述",
              "tips": ["建议1"],
              "difficulty": "easy",
              "cookingTime": 15,
              "mysticalMessage": "神秘寄语",
              "ingredients": ["主要食材 200g", "配料 10g"],
              "steps": ["1. 步骤描述", "2. 步骤描述"]
            }
        """.trimIndent()
        val res = callAi("你是一位神秘的料理占卜师。请严格按JSON返回。每个食材/调料必须是独立的数组项，严禁合并。", prompt, FortuneResult::class.java)
        return res.map { it.copy(
            id = "fortune-${System.currentTimeMillis()}", 
            type = "daily", 
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            ingredients = it.ingredients?.let { list -> sanitizeIngredients(list) }
        ) }
    }

    suspend fun generateMoodCooking(moods: List<String>, intensity: Int): Result<FortuneResult> {
        val prompt = """
            当前心情：${moods.joinToString("、")}，强度：$intensity/5。请推荐一道治愈菜品并给出情感分析。
            请按照以下JSON格式返回：
            {
              "dishName": "治愈菜名",
              "reason": "情感解析与推荐逻辑",
              "luckyIndex": 88,
              "description": "心情寄语",
              "tips": ["厨艺小贴士"],
              "difficulty": "medium",
              "cookingTime": 20,
              "mysticalMessage": "治愈系的一句话",
              "ingredients": ["食材1 100g", "调料2"],
              "steps": ["1. 第一步操作", "2. 第二步操作"]
            }
        """.trimIndent()
        val res = callAi("你是一位温暖的情感治愈料理占卜师。请严格按JSON返回。每个食材/调料必须是独立的数组项，严禁合并。", prompt, FortuneResult::class.java)
        return res.map { it.copy(
            id = "mood-${System.currentTimeMillis()}", 
            type = "mood",
            ingredients = it.ingredients?.let { list -> sanitizeIngredients(list) }
        ) }
    }

    suspend fun generateNumberFortune(number: Int): Result<FortuneResult> {
        val prompt = """
            幸运数字：$number。请解析数字寓意并推荐对应的幸运菜。
            请按照以下JSON格式返回：
            {
              "dishName": "幸运数字菜名",
              "reason": "数字寓意解析",
              "luckyIndex": 99,
              "description": "数字占卜解读",
              "tips": ["小贴士"],
              "difficulty": "easy",
              "cookingTime": 10,
              "mysticalMessage": "数字相关的寄语",
              "ingredients": ["对应食材"],
              "steps": ["1. 简单步骤", "2. 步骤"]
            }
        """.trimIndent()
        val res = callAi("你是一位精通数字占卜的料理大师。请严格按JSON返回。每个食材/调料必须是独立的数组项，严禁合并。", prompt, FortuneResult::class.java)
        return res.map { it.copy(
            id = "num-${System.currentTimeMillis()}", 
            type = "number",
            ingredients = it.ingredients?.let { list -> sanitizeIngredients(list) }
        ) }
    }

    @Suppress("unused")
    suspend fun getNutritionAnalysis(recipe: Recipe): Result<NutritionAnalysis> {
        val prompt = """
            请为菜品'${recipe.name}'生成营养分析。
            食材：${recipe.ingredients.joinToString("、")}
            请按照以下JSON格式返回：
            {
              "nutrition": {
                "calories": 300, "protein": 20, "carbs": 30, "fat": 10, "fiber": 5, "sodium": 500, "sugar": 5
              },
              "healthScore": 8,
              "balanceAdvice": ["建议搭配xx"],
              "dietaryTags": ["高蛋白", "低脂"],
              "servingSize": "1人份"
            }
        """.trimIndent()
        return callAi("你是一位专业的营养师。请严格按JSON返回数据。", prompt, NutritionAnalysis::class.java)
    }

    @Suppress("unused")
    suspend fun getWinePairing(recipe: Recipe): Result<WinePairing> {
        val prompt = """
            请为菜品'${recipe.name}'推荐搭配的平民化饮品。
            请按照以下JSON格式返回：
            {
              "name": "推荐饮品",
              "type": "soft_drink",
              "reason": "搭配理由",
              "servingTemperature": "冰镇",
              "flavor": "口感描述"
            }
        """.trimIndent()
        return callAi("你是一位专业的饮品搭配师。请严格按JSON返回结果。", prompt, WinePairing::class.java)
    }

    suspend fun generateImage(recipe: Recipe): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = settings.imageBaseUrl
            val apiKey = settings.imageApiKey

            if (baseUrl.isEmpty() || apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("请先在设置中完整配置图片生成的地址和 Key"))
            }

            if (settings.imageModel.isEmpty()) {
                return@withContext Result.failure(Exception("请先在设置中选择或输入生图模型"))
            }

            val ingredients = recipe.ingredients.joinToString("、")
            val cuisineStyle = recipe.cuisine.replace("大师", "").replace("菜", "")
            val prompt = "一道精美的${cuisineStyle}菜肴：${recipe.name}，主要食材包括${ingredients}。摆盘精致，色彩丰富，专业美食摄影风格，高清画质。"
            
            @Suppress("SpellCheckingInspection")
            val isDashScope = baseUrl.contains("dashscope.aliyuncs.com") || baseUrl.contains("dashscope-intl.aliyuncs.com")
            
            val result: Result<String> = try {
                if (isDashScope) {
                    @Suppress("SpellCheckingInspection")
                    val rootUrl = if (baseUrl.contains("-intl")) "https://dashscope-intl.aliyuncs.com/" else "https://dashscope.aliyuncs.com/"
                    val nativeApi = ApiClient.getService(rootUrl, apiKey)
                    val modelName = settings.imageModel
                    
                    // Qwen-Image series requires 'messages' array, Wanx uses 'prompt'
                    val isQwenImage = modelName.contains("qwen-image", ignoreCase = true)
                    
                    val dsInput = if (isQwenImage) {
                        DashScopeImageInput(
                            messages = listOf(
                                DashScopeChatMessage(
                                    role = "user",
                                    content = listOf(DashScopeChatContent(text = prompt))
                                )
                            )
                        )
                    } else {
                        DashScopeImageInput(prompt = prompt)
                    }
                    
                    val dsRequest = DashScopeImageRequest(
                        model = modelName,
                        input = dsInput,
                        parameters = DashScopeImageParams(size = "1024*1024")
                    )
                    
                    val dsResponse = nativeApi.generateImageDashScope(dsRequest)
                    
                    if (dsResponse.isSuccessful) {
                        val body = dsResponse.body()
                        val choice = body?.output?.choices?.firstOrNull()
                        
                        // Extract URL from either Wanx (image_url) or Qwen-Image (message.content.image)
                        val imageUrl = choice?.imageUrl 
                            ?: choice?.message?.content?.firstOrNull()?.image
                            ?: ""
                            
                        if (imageUrl.isNotEmpty()) {
                            Result.success(imageUrl)
                        } else {
                            val taskId = body?.output?.taskId ?: ""
                            if (taskId.isNotEmpty()) {
                                Result.failure(Exception("任务已提交 (ID: $taskId)，请稍后重试。"))
                            } else {
                                Result.failure(Exception("DashScope 返回空结果: ${body?.message ?: "原因未知"}"))
                            }
                        }
                    } else {
                        val errorBody = dsResponse.errorBody()?.string() ?: ""
                        Result.failure(Exception("DashScope 错误 (${dsResponse.code()}): $errorBody"))
                    }
                } else {
                    val api = ApiClient.getService(baseUrl, apiKey)
                    val request = ImageRequest(model = settings.imageModel, prompt = prompt, size = "1024x1024")
                    
                    var response = api.generateImage(request)
                    if (response.code() == 404) {
                        response = api.generateImageSingular(request)
                    }

                    if (response.isSuccessful) {
                        Result.success(response.body()?.data?.firstOrNull()?.url ?: "")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        val userError = when(response.code()) {
                            401 -> "API Key 错误，请检查设置"
                            404 -> "404 错误：模型不支持该接口路径。"
                            429 -> "额度不足或请求过快"
                            else -> "生成失败 (${response.code()}): $errorBody"
                        }
                        Result.failure(Exception(userError))
                    }
                }
            } catch (_: SocketTimeoutException) {
                Result.failure(Exception("连接超时，请检查网络设置"))
            } catch (e: Exception) {
                Result.failure(Exception("运行异常: ${e.localizedMessage}"))
            }
            
            result
        } catch (e: Exception) {
            Log.e("AiRepository", "Exception in generateImage wrapper: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun recognizeIngredients(imageUri: String): Result<List<String>> {
        val prompt = "请仔细观察这张图片，识别出其中包含的所有食材（包括蔬菜、肉类、调料等）。请只返回食材名称列表，不要有任何多余的描述。请务必以JSON格式返回，格式如：{\"ingredients\": [\"白菜\", \"猪肉\"]}"
        val res = callAi("你是一位极其专业的食材识别专家，能够精准识别各种食材。", prompt, Map::class.java, isVision = true, imageUri = imageUri)
        return res.map { (it["ingredients"] as? List<*>)?.map { item -> item.toString() } ?: emptyList() }
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (baseUrl.contains("volces.com") || baseUrl.contains("ark")) {
                return@withContext Result.failure(Exception("豆包 (Ark) 不支持自动获取列表。请在火山后台复制 '接入点 ID' (如 ep-xxx) 填入模型名。"))
            }
            
            val api = ApiClient.getService(baseUrl, apiKey)
            val response = api.getModels()
            if (response.isSuccessful) {
                val modelIds = response.body()?.data?.map { it.id } ?: emptyList()
                Result.success(modelIds.sorted())
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Log.e("AiRepository", "Fetch Models API Error: ${response.code()} - $errorBody")
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Exception in fetchModels: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Suppress("unused")
    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestFile = audioFile.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            val filePart = okhttp3.MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val modelPart = okhttp3.MultipartBody.Part.createFormData("model", "whisper-1") 
            
            val api = ApiClient.getService(settings.chatBaseUrl, settings.chatApiKey)
            val response = api.transcribeAudio(filePart, modelPart)
            if (response.isSuccessful) {
                Result.success(response.body()?.text ?: "")
            } else {
                Result.failure(Exception("Transcription API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e("AiRepository", "Failed to decode bitmap from URI: $uriString")
                return null
            }

            val maxDimension = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            Log.d("AiRepository", "Bitmap loaded: ${width}x$height")

            val newBitmap = if (width > maxDimension || height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(width, height)
                val sw = (width * scale).toInt()
                val sh = (height * scale).toInt()
                Log.d("AiRepository", "Scaling bitmap to: ${sw}x$sh")
                originalBitmap.scale(sw, sh, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            
            if (newBitmap != originalBitmap) newBitmap.recycle()
            originalBitmap.recycle()
            
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d("AiRepository", "Base64 encoded success, length: ${base64.length} (approx ${bytes.size / 1024} KB)")
            base64
        } catch (e: Exception) {
            Log.e("AiRepository", "Error in uriToBase64: ${e.message}", e)
            null
        }
    }

    private fun cleanJsonResponse(content: String): String {
        var clean = content.trim()
        if (clean.startsWith("```json")) clean = clean.removePrefix("```json").removeSuffix("```").trim()
        else if (clean.startsWith("```")) clean = clean.removePrefix("```").removeSuffix("```").trim()
        return clean
    }
}
