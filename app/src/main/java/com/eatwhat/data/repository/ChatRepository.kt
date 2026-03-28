package com.eatwhat.data.repository

import com.eatwhat.data.local.ChatDao
import com.eatwhat.data.model.ChatMessageItem
import com.eatwhat.data.model.ChatSessionSummary
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    suspend fun getMessagesInitial(sessionId: String): List<ChatMessageItem> =
        chatDao.getMessagesBySessionOnce(sessionId)


    fun getSessionSummaries(): Flow<List<ChatSessionSummary>> = 
        chatDao.getAllSessionSummaries()

    suspend fun saveMessage(message: ChatMessageItem) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun updateMessageSummary(id: String, summary: String) {
        chatDao.updateMessageSummary(id, summary)
    }

}
