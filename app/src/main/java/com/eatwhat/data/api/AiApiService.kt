package com.eatwhat.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

@Suppress("SpellCheckingInspection")
interface AiApiService {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @Streaming
    @POST("chat/completions")
    suspend fun getChatCompletionStream(
        @Body request: ChatRequest
    ): okhttp3.ResponseBody

    @POST("images/generations")
    suspend fun generateImage(
        @Body request: ImageRequest
    ): Response<ImageResponse>

    @POST("image/generations")
    suspend fun generateImageSingular(
        @Body request: ImageRequest
    ): Response<ImageResponse>

    @POST("api/v1/services/aigc/multimodal-generation/generation")
    suspend fun generateImageDashScope(
        @Body request: DashScopeImageRequest
    ): Response<DashScopeImageResponse>

    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part model: MultipartBody.Part,
        @Part language: MultipartBody.Part? = null
    ): Response<TranscriptionResponse>

    @GET("models")
    suspend fun getModels(): Response<ModelResponse>
}

data class ModelResponse(
    val data: List<AiModel>
)

data class AiModel(
    val id: String,
    val ownedBy: String? = null
)
