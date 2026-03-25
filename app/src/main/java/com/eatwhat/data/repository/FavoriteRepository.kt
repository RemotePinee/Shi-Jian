package com.eatwhat.data.repository

import android.content.Context
import com.eatwhat.data.model.FavoriteRecipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoriteRepository(context: Context) {
    private val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "favorite_recipes"
    private var cachedFavorites: List<FavoriteRecipe>? = null

    private val _favoritesFlow = MutableStateFlow<List<FavoriteRecipe>>(emptyList())
    val favoritesFlow: StateFlow<List<FavoriteRecipe>> = _favoritesFlow.asStateFlow()

    init {
        // Load initial data
        _favoritesFlow.value = loadFromPrefs()
    }

    fun getFavorites(): List<FavoriteRecipe> {
        return _favoritesFlow.value
    }

    private fun loadFromPrefs(): List<FavoriteRecipe> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<FavoriteRecipe>>() {}.type
        val parsed: List<FavoriteRecipe> = gson.fromJson(json, type)
        cachedFavorites = parsed
        return parsed
    }

    fun addFavorite(favorite: FavoriteRecipe) {
        val list = getFavorites().toMutableList()
        if (list.none { it.recipe.id == favorite.recipe.id }) {
            list.add(0, favorite)
            saveList(list)
        }
    }

    fun removeFavorite(recipeId: String) {
        val list = getFavorites().toMutableList()
        list.removeAll { it.recipe.id == recipeId }
        saveList(list)
    }


    fun isFavorite(recipeId: String): Boolean {
        return getFavorites().any { it.recipe.id == recipeId }
    }


    private fun saveList(list: List<FavoriteRecipe>) {
        val json = gson.toJson(list)
        prefs.edit { putString(key, json) }
        cachedFavorites = list
        _favoritesFlow.value = list // Emit to flow
    }
}
