package com.eatwhat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eatwhat.data.model.ChatMessageItem
import com.eatwhat.data.model.ChatSessionSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesBySessionOnce(sessionId: String): List<ChatMessageItem>

    @Query("""
        SELECT sessionId, content AS firstMessage 
        FROM chat_messages 
        WHERE id IN (SELECT MIN(id) FROM chat_messages GROUP BY sessionId)
        ORDER BY timestamp DESC
    """)
    fun getAllSessionSummaries(): Flow<List<ChatSessionSummary>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageItem)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

}
