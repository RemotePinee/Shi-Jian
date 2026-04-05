package com.shijian.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageDownloader {
    private val client = OkHttpClient()

    /**
     * Downloads an image from a URL and saves it to the app's internal storage.
     * Returns the absolute path of the saved file, or null if failed.
     */
    suspend fun downloadImage(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext null
            
            val body = response.body
            val bytes = body.bytes()
            
            // Create gallery directory in internal files
            val galleryDir = File(context.filesDir, "gallery")
            if (!galleryDir.exists()) {
                galleryDir.mkdirs()
            }
            
            // Generate a unique filename
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(galleryDir, fileName)
            
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a local image file.
     */
    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else true
        } catch (e: Exception) {
            false
        }
    }
}
