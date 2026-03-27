package com.eatwhat.ui.viewmodel

import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eatwhat.data.model.*
import com.eatwhat.data.repository.AiRepository
import com.eatwhat.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CuisineSlot(
    val id: String,
    val name: String,
    val recipe: Recipe? = null,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

class HomeViewModel(
    private val aiRepository: AiRepository,
    private val favoriteRepository: FavoriteRepository,
    private val galleryRepository: com.eatwhat.data.repository.GalleryRepository
) : ViewModel() {

    private val _isGeneratingImage = mutableStateOf<Map<String, Boolean>>(emptyMap())

    private val _ingredients = mutableStateListOf<String>()
    val ingredients: List<String> = _ingredients

    private val _selectedCuisines = mutableStateListOf<String>()
    val selectedCuisines: List<String> = _selectedCuisines

    private val _customPrompt = mutableStateOf("")

    private val _cuisineSlots = MutableStateFlow<List<CuisineSlot>>(emptyList())
    val cuisineSlots: StateFlow<List<CuisineSlot>> = _cuisineSlots.asStateFlow()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isRecognizing = mutableStateOf(false)
    val isRecognizing: State<Boolean> = _isRecognizing

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _favoritesRevision = mutableIntStateOf(0)
    val favoritesRevision: IntState = _favoritesRevision

    private val _isAnalyzingDeepInsights = mutableStateOf(false)
    val isAnalyzingDeepInsights: State<Boolean> = _isAnalyzingDeepInsights

    init {
        viewModelScope.launch {
            favoriteRepository.favoritesFlow.collect { list ->
                _favoritesRevision.intValue += 1
                
                // Sync cuisine slots with updated data from favorites
                val currentSlots = _cuisineSlots.value
                val updatedSlots = currentSlots.map { slot ->
                    slot.recipe?.let { recipe ->
                        list.find { it.recipe.id == recipe.id }?.let { fav ->
                            // If the favorite entry has more info (nutrition/pairing), sync it
                            if (fav.recipe.nutritionAnalysis != null || fav.recipe.winePairing != null) {
                                slot.copy(recipe = fav.recipe)
                            } else slot
                        } ?: slot
                    } ?: slot
                }
                if (updatedSlots != currentSlots) {
                    _cuisineSlots.value = updatedSlots
                }
            }
        }
    }

    fun isGenerating(recipeId: String): Boolean = _isGeneratingImage.value[recipeId] ?: false

    fun clearError() { _errorMessage.value = null }

    fun addIngredient(ingredient: String) {
        if (ingredient.isNotBlank() && !_ingredients.contains(ingredient) && _ingredients.size < 10) {
            _ingredients.add(ingredient)
        }
    }

    fun removeIngredient(ingredient: String) {
        _ingredients.remove(ingredient)
    }

    fun toggleCuisine(cuisineId: String) {
        if (_selectedCuisines.contains(cuisineId)) {
            _selectedCuisines.remove(cuisineId)
        } else {
            _selectedCuisines.add(cuisineId)
        }
    }


    fun onImageInput(uri: String) {
        viewModelScope.launch {
            _isRecognizing.value = true
            _errorMessage.value = null
            val result = aiRepository.recognizeIngredients(uri)
            result.onSuccess { list ->
                list.forEach { addIngredient(it) }
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "识别失败"
            }
            _isRecognizing.value = false
        }
    }


    fun generateRecipes() {
        if (_ingredients.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            
            // Initialize slots
            val slots = if (_customPrompt.value.isNotBlank()) {
                listOf(CuisineSlot("custom", "自定义推荐", isLoading = true))
            } else {
                _selectedCuisines.map { id ->
                    val name = ConfigData.cuisines.find { it.id == id }?.name ?: id
                    CuisineSlot(id, name, isLoading = true)
                }
            }
            _cuisineSlots.value = slots

            val jobs = slots.map { slot ->
                launch {
                    val cuisine = ConfigData.cuisines.find { it.id == slot.id } 
                        ?: CuisineType(slot.id, slot.name, "", "", "", "")
                    
                    val result = aiRepository.generateRecipe(_ingredients, cuisine, _customPrompt.value)
                    
                    val updatedSlots = _cuisineSlots.value.toMutableList()
                    val index = updatedSlots.indexOfFirst { it.id == slot.id }
                    if (index != -1) {
                        result.onSuccess { recipe ->
                            updatedSlots[index] = updatedSlots[index].copy(recipe = recipe, isLoading = false, progress = 100f)
                        }
                        result.onFailure { error ->
                            updatedSlots[index] = updatedSlots[index].copy(isLoading = false, error = error.message)
                        }
                        _cuisineSlots.value = updatedSlots
                    }
                }
            }
            
            // Wait for all recipes to be generated before finishing the loading ritual
            jobs.joinAll()
            _isLoading.value = false
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        if (favoriteRepository.isFavorite(recipe.id)) {
            favoriteRepository.removeFavorite(recipe.id)
        } else {
            favoriteRepository.addFavorite(
                FavoriteRecipe(
                    id = recipe.id,
                    recipe = recipe,
                    favoriteDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
            )
        }
        // Force UI refresh by updating revision
        _favoritesRevision.intValue += 1
    }

    fun isFavorite(recipeId: String): Boolean {
        // Access revision to subscribe to changes
        favoritesRevision.intValue
        return favoriteRepository.isFavorite(recipeId)
    }

    fun generateImage(recipe: Recipe, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _isGeneratingImage.value += (recipe.id to true)
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
            result.onFailure {
                onComplete(null)
            }
            _isGeneratingImage.value += (recipe.id to false)
        }
    }

    fun unlockDeepInsights(recipe: Recipe) {
        viewModelScope.launch {
            _isAnalyzingDeepInsights.value = true
            
            // Parallel execution with synchronized update
            val nutritionDeferred = async { aiRepository.getNutritionAnalysis(recipe) }
            val wineDeferred = async { aiRepository.getWinePairing(recipe) }
            
            val nutritionRes = nutritionDeferred.await()
            val wineRes = wineDeferred.await()
            
            updateRecipeInSlots(recipe.id, nutritionRes.getOrNull(), wineRes.getOrNull())
            _isAnalyzingDeepInsights.value = false
        }
    }

    private fun updateRecipeInSlots(recipeId: String, nutrition: NutritionAnalysis?, pairing: WinePairing?) {
        val updatedSlots = _cuisineSlots.value.toMutableList()
        val index = updatedSlots.indexOfFirst { it.recipe?.id == recipeId }
        if (index != -1) {
            val currentRecipe = updatedSlots[index].recipe ?: return
            val newRecipe = currentRecipe.copy(
                nutritionAnalysis = nutrition ?: currentRecipe.nutritionAnalysis,
                winePairing = pairing ?: currentRecipe.winePairing
            )
            updatedSlots[index] = updatedSlots[index].copy(recipe = newRecipe)
            _cuisineSlots.value = updatedSlots
        }
    }
}
