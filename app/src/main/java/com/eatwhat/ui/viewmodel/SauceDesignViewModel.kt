package com.eatwhat.ui.viewmodel

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eatwhat.data.model.*
import com.eatwhat.data.repository.AiRepository
import kotlinx.coroutines.launch
import com.eatwhat.data.repository.FavoriteRepository
import java.text.SimpleDateFormat
import java.util.*

class SauceDesignViewModel(
    private val aiRepository: AiRepository,
    private val favoriteRepository: FavoriteRepository,
    private val galleryRepository: com.eatwhat.data.repository.GalleryRepository
) : ViewModel() {

    private val _isGeneratingImage = mutableStateOf(false)
    val isGeneratingImage: State<Boolean> = _isGeneratingImage

    private val _spiceLevel = mutableFloatStateOf(3f)
    val spiceLevel: FloatState = _spiceLevel

    private val _sweetLevel = mutableFloatStateOf(2f)
    val sweetLevel: FloatState = _sweetLevel

    private val _saltLevel = mutableFloatStateOf(3f)
    val saltLevel: FloatState = _saltLevel

    private val _sourLevel = mutableFloatStateOf(2f)
    val sourLevel: FloatState = _sourLevel

    private val _selectedUseCases = mutableStateListOf<String>()
    val selectedUseCases: List<String> = _selectedUseCases


    private val _recommendations = mutableStateOf<List<String>>(emptyList())
    val recommendations: State<List<String>> = _recommendations

    private val _currentSauce = mutableStateOf<SauceRecipe?>(null)
    val currentSauce: State<SauceRecipe?> = _currentSauce

    private val _isLoadingRecommendations = mutableStateOf(false)
    val isLoadingRecommendations: State<Boolean> = _isLoadingRecommendations

    private val _isLoadingSauce = mutableStateOf(false)
    val isLoadingSauce: State<Boolean> = _isLoadingSauce


    fun updateSpice(level: Float) { _spiceLevel.floatValue = level }
    fun updateSweet(level: Float) { _sweetLevel.floatValue = level }
    fun updateSalt(level: Float) { _saltLevel.floatValue = level }
    fun updateSour(level: Float) { _sourLevel.floatValue = level }

    fun toggleUseCase(id: String) {
        if (_selectedUseCases.contains(id)) _selectedUseCases.remove(id)
        else _selectedUseCases.add(id)
    }


    fun getRecommendations() {
        viewModelScope.launch {
            _isLoadingRecommendations.value = true
            _currentSauce.value = null
            
            val pref = SaucePreference(
                spiceLevel = _spiceLevel.floatValue.toInt(),
                sweetLevel = _sweetLevel.floatValue.toInt(),
                saltLevel = _saltLevel.floatValue.toInt(),
                sourLevel = _sourLevel.floatValue.toInt(),
                useCase = _selectedUseCases.toList(),
                availableIngredients = emptyList()
            )
            
            val result = aiRepository.recommendSauces(pref)
            result.onSuccess {
                _recommendations.value = it
            }.onFailure {
                _recommendations.value = listOf("失败了，请重试")
            }
            _isLoadingRecommendations.value = false
        }
    }

    fun selectSauce(name: String) {
        viewModelScope.launch {
            _isLoadingSauce.value = true
            _currentSauce.value = null
            
            val result = aiRepository.generateSauceRecipe(name)
            result.onSuccess {
                _currentSauce.value = it
            }.onFailure {
                // Error handling
            }
            _isLoadingSauce.value = false
        }
    }

    fun isFavorite(id: String): Boolean {
        return favoriteRepository.isFavorite(id)
    }

    fun toggleFavorite(sauce: SauceRecipe) {
        if (favoriteRepository.isFavorite(sauce.id)) {
            favoriteRepository.removeFavorite(sauce.id)
        } else {
            val recipe = Recipe(
                id = sauce.id,
                name = sauce.name,
                cuisine = "香辣酱料", // Default or map from category
                ingredients = sauce.ingredients,
                steps = sauce.steps.map { RecipeStep(it.step, it.description, it.time, it.temperature) },
                cookingTime = sauce.makingTime,
                difficulty = sauce.difficulty,
                tips = sauce.tips
            )
            favoriteRepository.addFavorite(
                FavoriteRecipe(
                    id = recipe.id,
                    recipe = recipe,
                    favoriteDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
            )
        }
    }

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
}
