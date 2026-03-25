package com.eatwhat.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val services = mutableMapOf<String, AiApiService>()

    /**
     * Get or create an AiApiService for a specific base URL and API key.
     */
    fun getService(baseUrl: String, apiKey: String): AiApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cacheKey = "$normalizedUrl|$apiKey"
        
        return synchronized(services) {
            services.getOrPut(cacheKey) {
                createService(normalizedUrl, apiKey)
            }
        }
    }


    private fun createService(baseUrl: String, apiKey: String): AiApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS // Critical: Level.BODY breaks @Streaming
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApiService::class.java)
    }

}
