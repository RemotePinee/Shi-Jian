package com.eatwhat.data.api

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 2000,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: Any // Can be String or List<ChatContentPart>
)

data class ChatContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrlPart? = null
)

data class ImageUrlPart(
    val url: String // Should be "data:image/jpeg;base64,{base64_image}"
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessage
)

data class ChatStreamResponse(
    val choices: List<ChatStreamChoice>
)

data class ChatStreamChoice(
    val delta: ChatStreamDelta
)

data class ChatStreamDelta(
    val content: String? = null
)

data class ImageRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024"
)

data class ImageResponse(
    val data: List<ImageData>
)

data class ImageData(
    val url: String
)

data class TranscriptionResponse(
    val text: String
)

// DashScope Native Models (Sync Generation)
data class DashScopeImageRequest(
    @SerializedName("model") val model: String,
    @SerializedName("input") val input: DashScopeImageInput,
    @SerializedName("parameters") val parameters: DashScopeImageParams? = null
)

data class DashScopeImageInput(
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("messages") val messages: List<DashScopeChatMessage>? = null
)

data class DashScopeChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: List<DashScopeChatContent>
)

data class DashScopeChatContent(
    @SerializedName("text") val text: String? = null,
    @SerializedName("image") val image: String? = null
)

data class DashScopeImageParams(
    @SerializedName("n") val n: Int = 1,
    @SerializedName("size") val size: String = "1024*1024"
)

data class DashScopeImageResponse(
    val output: DashScopeImageOutput? = null,
    @SerializedName("request_id") val requestId: String? = null,
    val message: String? = null,
    val code: String? = null
)

data class DashScopeImageOutput(
    val choices: List<DashScopeImageChoice>? = null,
    @SerializedName("task_id") val taskId: String? = null,
    @SerializedName("task_status") val taskStatus: String? = null
)

data class DashScopeImageChoice(
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("message") val message: DashScopeImageMessage? = null
)

data class DashScopeImageMessage(
    @SerializedName("content") val content: List<DashScopeImageContent>? = null
)

data class DashScopeImageContent(
    @SerializedName("image") val image: String? = null
)
