package com.shijian.data.model

data class GalleryImage(
    val id: String,
    val url: String,
    val localPath: String? = null, // Path to local saved file
    val recipeName: String,
    val recipeId: String,
    val cuisine: String,
    val ingredients: List<String>,
    val generatedAt: String,
    val prompt: String? = null
)

