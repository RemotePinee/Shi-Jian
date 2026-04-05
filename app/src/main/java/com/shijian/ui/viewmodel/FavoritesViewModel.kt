package com.shijian.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijian.data.model.FavoriteRecipe
import com.shijian.data.model.Recipe
import com.shijian.data.repository.AiRepository
import com.shijian.data.repository.FavoriteRepository
import com.shijian.data.repository.GalleryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*

class FavoritesViewModel(
    private val repository: FavoriteRepository,
    private val aiRepository: AiRepository,
    private val galleryRepository: GalleryRepository
) : ViewModel() {


    private val _favorites = mutableStateOf<List<FavoriteRecipe>>(emptyList())
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _selectedCuisine = mutableStateOf("")

    init {
        viewModelScope.launch {
            repository.favoritesFlow.collect { list ->
                _favorites.value = list
            }
        }
    }


    fun updateSearch(query: String) { _searchQuery.value = query }

    fun removeFavorite(id: String) {
        repository.removeFavorite(id)
    }


    fun filterFavorites(): List<FavoriteRecipe> {
        return _favorites.value.filter { fav ->
            val nameMatch = fav.recipe.name.contains(_searchQuery.value, ignoreCase = true)
            val cuisineMatch = _selectedCuisine.value.isEmpty() || fav.recipe.cuisine == _selectedCuisine.value
            nameMatch && cuisineMatch
        }
    }

    private val _isGeneratingImage = mutableStateOf(false)
    val isGeneratingImage: State<Boolean> = _isGeneratingImage

    private val _isAnalyzingDeepInsights = mutableStateOf(false)
    val isAnalyzingDeepInsights: State<Boolean> = _isAnalyzingDeepInsights

    fun generateImage(recipe: Recipe, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _isGeneratingImage.value = true
            val result = aiRepository.generateImage(recipe)
            result.onSuccess { url ->
                if (url.isNotBlank()) {
                    galleryRepository.addToGalleryWithDownload(
                        id = "img-${System.currentTimeMillis()}",
                        url = url,
                        recipeName = recipe.name,
                        recipeId = recipe.id,
                        cuisine = recipe.cuisine,
                        ingredients = recipe.ingredients,
                        generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    )
                    onComplete(url)
                } else {
                    onComplete(null)
                }
            }
            result.onFailure { onComplete(null) }
            _isGeneratingImage.value = false
        }
    }

    fun unlockDeepInsights(recipe: Recipe, onComplete: (Recipe) -> Unit) {
        viewModelScope.launch {
            _isAnalyzingDeepInsights.value = true
            
            // Parallel execution with synchronized update and persistence
            val nutritionDeferred = async { aiRepository.getNutritionAnalysis(recipe) }
            val wineDeferred = async { aiRepository.getWinePairing(recipe) }
            
            val nutritionRes = nutritionDeferred.await()
            val wineRes = wineDeferred.await()
            
            val updated = recipe.copy(
                nutritionAnalysis = nutritionRes.getOrNull(),
                winePairing = wineRes.getOrNull()
            )
            repository.updateFavoriteRecipe(recipe.id, updated)
            onComplete(updated)
            
            _isAnalyzingDeepInsights.value = false
        }
    }
}
