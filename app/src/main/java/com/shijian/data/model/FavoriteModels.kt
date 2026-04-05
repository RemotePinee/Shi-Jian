package com.shijian.data.model

data class FavoriteRecipe(
    val id: String,
    val recipe: Recipe,
    val favoriteDate: String,
    var notes: String = ""
)
