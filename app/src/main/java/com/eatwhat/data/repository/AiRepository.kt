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

    fun chatStream(messages: List<ChatMessage>, imageUri: String? = null): Flow<String> = flow {
        // Plan B: Detection logic for vision session
        // It's a vision session if it has a new image OR if any message in history is multimodal
        val isVision = imageUri != null || messages.any { it.content is List<*> }
        
        // Use dedicated settings per role, no fallback
        val model = if (isVision) settings.visionModel else settings.chatModel
        val baseUrl = if (isVision) settings.visionBaseUrl else settings.chatBaseUrl
        val apiKey = if (isVision) settings.visionApiKey else settings.chatApiKey

        if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            val category = if (isVision) "识图 (Vision)" else "对话 (Chat)"
            val help = if (isVision) "\n\n提示：此对话包含图片，需配置识图模型。如果你想共用对话模型，请将对话配置复制一份到识图配置中。" else ""
            throw Exception("请先在设置中配置 $category 的接口地址、Key 和模型。$help")
        }

        // Messages are now prepared by ViewModel for multimodal history (Plan B).
        // For vision sessions, some providers (Ark, ZhipuAI) have strict requirements:
        // 1. Ark: Do not allow 'system' role; system instructions must be merged into the first user message.
        // 2. ZhipuAI: Large 'maxTokens' or non-default 'temperature' can cause 400.
        
        val isArk = baseUrl.contains("volces.com") || baseUrl.contains("ark")
        val isZhipu = baseUrl.contains("bigmodel.cn") || baseUrl.contains("zhipuai")

        val finalMessages = if (isVision) {
            val systemMsg = messages.firstOrNull { it.role == "system" }
            val otherMessages = messages.filter { it.role != "system" }
            
            if (isArk && systemMsg != null) {
                // Merge system into first available user message
                val firstUserMsg = otherMessages.firstOrNull { it.role == "user" }
                if (firstUserMsg != null) {
                    val updatedFirstUser = firstUserMsg.copy(
                        content = if (firstUserMsg.content is String) {
                            "${systemMsg.content}\n\n${firstUserMsg.content}"
                        } else if (firstUserMsg.content is List<*>) {
                            // Multimodal: Prepend system text to the text part
                            firstUserMsg.content.map { part ->
                                if (part is ChatContentPart && part.type == "text") {
                                    part.copy(text = "${systemMsg.content}\n\n${part.text}")
                                } else part
                            }
                        } else firstUserMsg.content
                    )
                    otherMessages.map { if (it === firstUserMsg) updatedFirstUser else it }
                } else otherMessages
            } else {
                messages // Standard OpenAI style
            }
        } else messages

        val api = ApiClient.getService(baseUrl, apiKey)
        val request = ChatRequest(
            model = model,
            messages = finalMessages,
            temperature = if (isVision && isZhipu) null else 0.7f,
            maxTokens = if (isVision && isZhipu) null else 2000,
            stream = true
        )

        val responseBody = try {
            api.getChatCompletionStream(request)
        } catch (e: Exception) {
            throw Exception("连接服务器失败: ${e.localizedMessage}")
        }

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
        val prompt = buildString {
            append("${cuisine.prompt}\n\n用户提供的食材：${ingredients.joinToString("、")}")
            if (customPrompt != null) {
                append("\n\n用户的特殊要求：$customPrompt")
            }
            append("""
                
                请生成一份详细实用的菜谱，要求：
                1. 食材清单要包含具体用量（如：猪肉300g、生抽2勺、盐1茶匙）
                2. 每个食材/调料必须是独立的数组项，严禁将多种调料用逗号或顿号合并成一个字符串（如：不要出现"调料：盐、生抽"这种合并项）。
                3. 制作步骤要详细具体，包含具体的操作方法、准确的时间控制、火候掌握及关键判断标准。
                4. 烹饪技巧要实用。
                
                请按照以下JSON格式返回菜谱：
                {
                  "name": "菜品名称",
                  "ingredients": ["猪肉 300g", "生抽 2勺", "青椒 100g"],
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
            """.trimIndent())
        }
        
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
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var balance = 0
            
            for (char in item) {
                when (char) {
                    '(', '（' -> {
                        balance++
                        current.append(char)
                    }
                    ')', '）' -> {
                        if (balance > 0) balance--
                        current.append(char)
                    }
                    ',', '，', '、' -> {
                        if (balance == 0) {
                            val part = current.toString().trim()
                            if (part.isNotEmpty()) result.add(part)
                            current.setLength(0)
                        } else {
                            current.append(char)
                        }
                    }
                    else -> current.append(char)
                }
            }
            
            val lastPart = current.toString().trim()
            if (lastPart.isNotEmpty()) result.add(lastPart)
            
            result.map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { 
                    // 移除冗余的前缀标签，如 "主料1", "调料2", "必备调味1", "主要食材" 等
                    val pattern = "^(主料|调料|配料|主要食材|必备调味|主要成分|辅料|食材)\\d*[:：\\s]*".toRegex()
                    it.replace(pattern, "").trim()
                }
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
              "ingredients": ["猪肉 200g", "生抽 1勺"],
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
            用户身份：星座[$zodiac]，生肖[$animal]。
            作为一位精通周易五行与西方占星术的【料理占卜师】，请为用户推演今日运势并推荐一道“开运料理”。
            
            【起名与推演逻辑 - 核心规范】：
            1. **严禁复读示例**：下文提供的示例（如离火、墨池）仅为逻辑参考，严禁直接使用。
            2. **生肖冲突避让**：绝对禁止推荐以用户生肖动物[$animal]为主要肉类来源的菜品。
            3. **推导式命名**：菜名严禁直接包含“$zodiac”或“$animal”字眼。必须通过五行色彩、星象能量空间或祈愿寓意进行二次创作推导。
               - 示例逻辑：辛辣/温补 -> 离火/赤焰；黑色滋补 -> 墨池/北冥；清新/嫩叶 -> 晨曦/春华。请通过你的文字底蕴创造更高级的名称。
            4. **文案艺术**：`mysticalMessage` 需具备大师般的诗意与预言感；`description` 需深度解析该菜品如何从玄学层面调和今日运势。
            5. **忌宜系统**：必须提供“宜”与“忌”的建议，涵盖饮食、心态或小仪式线索。

            请严格按照以下JSON格式返回：
            {
              "dishName": "创意推导出的雅致菜名",
              "mysticalMessage": "充满玄学美感与治愈力的诗意预言",
              "description": "基于星座生肖特质与五行调和理论的深度运势解析",
              "luckyIndex": 60-100之间的整数,
              "reason": "玄学层面的推荐动机（如：风象星座今日思虑过载，宜用根茎类食材固本培元）",
              "tips": ["厨艺方面的点睛之笔"],
              "luckyAdvice": ["今日宜执行的具体开运建议"],
              "tabooAdvice": ["今日应避开的行为或雷区"],
              "difficulty": "easy/medium/hard",
              "cookingTime": 整数(分钟),
              "ingredients": ["具体食材名 份量", "调料名 份量"],
              "steps": ["1. 详细步骤说明", "2. 详细步骤说明"]
            }
        """.trimIndent()
        
        val systemMsg = "你是一位游历四方、精通星象易理与食疗哲学的神秘料理占卜师。你说话优雅且富有禅意。请严格按照JSON格式返回，严禁任何多余解释。"
        val res = callAi(systemMsg, prompt, FortuneResult::class.java)
        return res.map { it.copy(
            id = "fortune-${System.currentTimeMillis()}", 
            type = "daily", 
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            ingredients = it.ingredients?.let { list -> sanitizeIngredients(list) }
        ) }
    }

    suspend fun generateMoodCooking(moods: List<String>, intensity: Int): Result<FortuneResult> {
        val prompt = """
            当前心情：${moods.joinToString("、")}，情绪感应强度：$intensity/5。
            作为一位【情感治愈料理占卜师】，请通过食物的质感、温度与营养成分来调理用户的心情。
            
            【要求】：
            1. **情感共鸣**：`mysticalMessage` 应如深夜电台般温暖且富有哲理；`description` 需解析食材如何对应其情绪（如：碳水化合物带来的安全感，水果酸甜带来的多巴胺释放）。
            2. **起名艺术**：菜名应体现“治愈”与“意象”，严禁平铺直叙。
            3. **生活指引**：必须产出针对该心情的“今日宜/忌”建议。

            请严格按照以下JSON格式返回：
            {
              "dishName": "治愈系雅致菜名",
              "mysticalMessage": "温暖治愈的心情语录",
              "description": "针对当前情绪的料理疗愈解析",
              "luckyIndex": 60-100之间的整数,
              "reason": "推荐逻辑（解析食材与心理压力的科学或玄学联结）",
              "tips": ["厨艺方面的点睛之笔"],
              "luckyAdvice": ["今日宜执行的心灵/生活建议"],
              "tabooAdvice": ["今日应避开的负能量雷区"],
              "difficulty": "easy/medium/hard",
              "cookingTime": 整数(分钟),
              "ingredients": ["食材名 份量"],
              "steps": ["1. 制作步骤", "2. 制作步骤"]
            }
        """.trimIndent()
        
        val systemMsg = "你是一位极度温柔且擅长情感疗愈的神秘料理师。你的话语能抚平焦虑。请严格按照JSON格式返回，严禁任何多余解释。"
        val res = callAi(systemMsg, prompt, FortuneResult::class.java)
        return res.map { it.copy(
            id = "mood-${System.currentTimeMillis()}", 
            type = "mood",
            ingredients = it.ingredients?.let { list -> sanitizeIngredients(list) }
        ) }
    }

    suspend fun generateNumberFortune(number: Int): Result<FortuneResult> {
        val prompt = """
            幸运数字：[$number]。
            作为一位精通【数秘学】与【能量平衡】的料理大师，请解析该数字背后的命运振动，并推荐一道“契合能量”的开运菜。
            
            【要求】：
            1. **数字寓意**：`mysticalMessage` 应揭示数字深层含义；`description` 需解析数字能量与食材性质的对应关系。
            2. **起名艺术**：菜名应基于数字的神秘几何或历史寓意进行推演。
            3. **行为指引**：必须产出针对该数字能量的“今日宜/忌”建议。

            请严格按照以下JSON格式返回：
            {
              "dishName": "基于数字背景推导出的雅致菜名",
              "mysticalMessage": "关于数字命理的神秘寄语",
              "description": "数字占卜与料理能量的深度对等分析",
              "luckyIndex": 60-100之间的整数,
              "reason": "推荐逻辑（解析数字频率如何与食材共振）",
              "tips": ["厨艺方面的点睛之笔"],
              "luckyAdvice": ["今日宜执行的开运小仪式"],
              "tabooAdvice": ["今日绝对要避开的混乱区域"],
              "difficulty": "easy/medium/hard",
              "cookingTime": 整数(分钟),
              "ingredients": ["食材名 份量"],
              "steps": ["1. 制作步骤", "2. 制作步骤"]
            }
        """.trimIndent()
        
        val systemMsg = "你是一位冷静睿智、精通数秘学与食材能量的料理占卜师。你洞悉万物律动。请严格按照JSON格式返回，严禁任何多余解释。"
        val res = callAi(systemMsg, prompt, FortuneResult::class.java)
        return res.map { it.copy(
            id = "num-${System.currentTimeMillis()}", 
            type = "number",
            ingredients = it.ingredients?.let { list -> sanitizeIngredients(list) }
        ) }
    }

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

    suspend fun getWinePairing(recipe: Recipe): Result<WinePairing> {
        val prompt = """
            请为菜品'${recipe.name}'推荐一款绝佳的佐餐灵魂饮品。
            食材组成：${recipe.ingredients.joinToString("、")}
            
            要求：
            1. **严禁创作**：饮品必须是现实生活中真实存在、符合大众审美且极其正常的市售或成熟单品（如：特定的商业品牌饮料、连锁咖啡厅/奶茶店常驻单品）。
            2. **绝对防雷**：禁止任何将菜品调料或特色食材混入饮品的尝试（例如严禁出现：大蒜咖啡、辣酱苏打、香菜奶茶、韭菜果汁等）。
            3. **品类范围**（优先考虑现代感、多样性）：
               - **经典苏打/气泡饮**（如：鲜榨西柚气泡、接骨木花苏打、经典莫吉托无酒精版、圣培露矿泉水加青柠等）
               - **精品咖啡/冷萃**（如：冰美式、手冲瑰夏、椰青美式、燕麦拿铁等）
               - **现代果茶/奶露**（如：多肉葡萄、芝士芒芒、鸭屎香柠檬茶、厚椰乳等）
               - **传统消食饮品**（如：冰镇酸梅汤、日式煎茶/大麦茶、洛神花茶、马蹄水等）
               - **专业中式茶**（如：陈皮普洱、大红袍、铁观音、正山小种等）
            4. **科学逻辑与温度**：
               - **解辣必备**：辛辣川湘菜优先推荐冰镇乳制品（厚椰乳、燕麦奶）或冰镇甜性气泡型饮料。
               - **去腻首选**：油腻红肉或煎炸食品优先推荐冰美式、冷萃或重焙火乌龙（如大红袍）。
               - **清淡平衡**：清淡海鲜或白肉优先推荐清新果香系气泡水或花果香系茶。
               - **温度建议**：除非是特定的暖胃需求，否则优先考虑更具清爽感的 5°C 冰镇或常温建议。
            
            请按照以下JSON格式返回：
            {
              "name": "真实规范的饮品名",
              "type": "饮品类别",
              "reason": "专业、感性且基于口味科学的互补理由",
              "servingTemperature": "饮用温度建议（如：5°C 冰镇 / 常温 / 85°C 热饮）",
              "flavor": "口感细节特征（如：酸甜平衡、清新果香、醇厚回甘）"
            }
        """.trimIndent()
        return callAi("你是一位精致生活的顶级侍酒/侍茶师，深谙佐餐逻辑。请严格按JSON返回结果，严禁推荐黑暗料理。", prompt, WinePairing::class.java)
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

    suspend fun getImageSummary(imageUri: String): Result<String> {
        val prompt = "请以顶级大厨的视角，非常详细地描述这张图片中的所有内容。包括食材种类、新鲜程度、可能的用途以及任何视觉细节。这个描述将作为后续对话的文字背景，请务必客观且详尽。150字以内。"
        return callAi("你是一位专业的视觉描述专家。", prompt, Map::class.java, isVision = true, imageUri = imageUri)
            .map { it["summary"]?.toString() ?: it.values.firstOrNull()?.toString() ?: "" }
            .mapCatching { 
                it.ifEmpty { 
                    // Try direct string response if JSON parsing failed or returned empty
                    val res = callAi("你是一位专业的视觉描述专家。", prompt, String::class.java, isVision = true, imageUri = imageUri)
                    res.getOrThrow()
                }

            }
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

    fun uriToBase64(uriString: String): String? {
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
        
        return try {
            fixUnterminatedJson(clean)
        } catch (_: Exception) {
            clean
        }
    }

    /**
     * Attempts to fix JSON strings that were truncated (e.g., due to AI max tokens).
     * It closes unclosed strings and balances brackets using a stack.
     */
    private fun fixUnterminatedJson(json: String): String {
        val result = json.trim()
        if (result.isEmpty()) return result
        
        val openBrackets = mutableListOf<Char>()
        var inString = false
        var escaped = false
        
        for (i in result.indices) {
            val char = result[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\') {
                escaped = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }
            
            if (!inString) {
                when (char) {
                    '{', '[' -> openBrackets.add(char)
                    '}', ']' -> {
                        if (openBrackets.isNotEmpty()) {
                            val last = openBrackets.last()
                            if ((char == '}' && last == '{') || (char == ']' && last == '[')) {
                                openBrackets.removeAt(openBrackets.size - 1)
                            }
                        }
                    }
                }
            }
        }
        
        var fixedResult = result
        // 1. Close string if it was cut off inside a string literal
        if (inString) {
            fixedResult += "\""
        }
        
        // 2. Remove trailing commas if they are followed by nothing (invalid in JSON)
        fixedResult = fixedResult.trim().removeSuffix(",")
        
        // 3. Close open brackets in reverse order
        while (openBrackets.isNotEmpty()) {
            val last = openBrackets.removeAt(openBrackets.size - 1)
            fixedResult += if (last == '{') "}" else "]"
        }
        
        return fixedResult
    }
}
