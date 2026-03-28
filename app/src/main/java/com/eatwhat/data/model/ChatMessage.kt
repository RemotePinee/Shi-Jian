package com.eatwhat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "default",
    val content: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),

    val recipeJson: String? = null,
    val isRecipeLoading: Boolean = false,
    val imageUri: String? = null,
    val imageSummary: String? = null,
    val generatedImageUrl: String? = null,
    val isImageLoading: Boolean = false
)


data class ChatSessionSummary(
    val sessionId: String,
    val firstMessage: String
)
