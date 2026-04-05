package com.shijian.data.repository

import android.content.Context
import com.shijian.data.model.GalleryImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class GalleryRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "gallery_images"
    private var cachedGallery: List<GalleryImage>? = null

    suspend fun addToGalleryWithDownload(
        id: String,
        url: String,
        recipeName: String,
        recipeId: String,
        cuisine: String,
        ingredients: List<String>,
        generatedAt: String,
        prompt: String? = null
    ): String? {
        val localPath = com.shijian.util.ImageDownloader.downloadImage(context, url)
        val image = GalleryImage(
            id = id,
            url = url,
            localPath = localPath,
            recipeName = recipeName,
            recipeId = recipeId,
            cuisine = cuisine,
            ingredients = ingredients,
            generatedAt = generatedAt,
            prompt = prompt
        )
        addToGallery(image)
        return localPath
    }

    fun getGalleryImages(): List<GalleryImage> {
        cachedGallery?.let { return it }
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<GalleryImage>>() {}.type
        val parsed: List<GalleryImage> = gson.fromJson(json, type)
        cachedGallery = parsed
        return parsed
    }

    fun addToGallery(image: GalleryImage) {
        val list = getGalleryImages().toMutableList()
        list.add(0, image)
        saveList(list)
    }

    fun removeFromGallery(imageId: String) {
        val list = getGalleryImages().toMutableList()
        val imageToRemove = list.find { it.id == imageId }
        
        // Delete local file if it exists
        imageToRemove?.localPath?.let { path ->
            com.shijian.util.ImageDownloader.deleteFile(path)
        }
        
        list.removeAll { it.id == imageId }
        saveList(list)
    }

    fun clearGallery() {
        val list = getGalleryImages()
        list.forEach { image ->
            image.localPath?.let { path ->
                com.shijian.util.ImageDownloader.deleteFile(path)
            }
        }
        prefs.edit { remove(key) }
        cachedGallery = null
    }

    private fun saveList(list: List<GalleryImage>) {
        val json = gson.toJson(list)
        prefs.edit { putString(key, json) }
        cachedGallery = list // Update cache directly
    }
}
