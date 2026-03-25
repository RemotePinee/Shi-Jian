package com.eatwhat.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("eat_what_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_API_KEY = "api_key" 
        const val KEY_BASE_URL = "base_url" 
        
        const val KEY_CHAT_API_KEY = "chat_api_key"
        const val KEY_CHAT_BASE_URL = "chat_base_url"
        const val KEY_CHAT_MODEL = "chat_model"
        
        const val KEY_VISION_API_KEY = "vision_api_key"
        const val KEY_VISION_BASE_URL = "vision_base_url"
        const val KEY_VISION_MODEL = "vision_model"
        
        const val KEY_IMAGE_MODEL = "image_model"
        
        const val KEY_CHAT_PROVIDER = "chat_provider"
        const val KEY_VISION_PROVIDER = "vision_provider"
        const val KEY_IMAGE_PROVIDER = "image_provider"
    }

    // Image Generation (Actually Global/Default slots)
    var imageApiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var imageBaseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BASE_URL, if (value.endsWith("/") || value.isEmpty()) value else "$value/") }

    // Chat Specific
    var chatApiKey: String
        get() = prefs.getString(KEY_CHAT_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CHAT_API_KEY, value) }

    var chatBaseUrl: String
        get() = prefs.getString(KEY_CHAT_BASE_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CHAT_BASE_URL, if (value.endsWith("/") || value.isEmpty()) value else "$value/") }

    // Vision Specific
    var visionApiKey: String
        get() = prefs.getString(KEY_VISION_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_VISION_API_KEY, value) }

    var visionBaseUrl: String
        get() = prefs.getString(KEY_VISION_BASE_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_VISION_BASE_URL, if (value.endsWith("/") || value.isEmpty()) value else "$value/") }

    var chatModel: String
        get() = prefs.getString(KEY_CHAT_MODEL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CHAT_MODEL, value) }

    var visionModel: String
        get() = prefs.getString(KEY_VISION_MODEL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_VISION_MODEL, value) }

    var imageModel: String
        get() = prefs.getString(KEY_IMAGE_MODEL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_IMAGE_MODEL, value) }

    var chatProvider: String
        get() = prefs.getString(KEY_CHAT_PROVIDER, "OpenAI") ?: "OpenAI"
        set(value) = prefs.edit { putString(KEY_CHAT_PROVIDER, value) }

    var visionProvider: String
        get() = prefs.getString(KEY_VISION_PROVIDER, "OpenAI") ?: "OpenAI"
        set(value) = prefs.edit { putString(KEY_VISION_PROVIDER, value) }

    var imageProvider: String
        get() = prefs.getString(KEY_IMAGE_PROVIDER, "OpenAI") ?: "OpenAI"
        set(value) = prefs.edit { putString(KEY_IMAGE_PROVIDER, value) }

    var hasChatHistory: Boolean
        get() = prefs.getBoolean("has_chat_history", false)
        set(value) = prefs.edit { putBoolean("has_chat_history", value) }
}
