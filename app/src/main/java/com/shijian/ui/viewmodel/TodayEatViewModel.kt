package com.shijian.ui.viewmodel

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijian.data.model.*
import com.shijian.data.repository.AiRepository
import com.shijian.data.repository.FavoriteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Preference(val label: String, val icon: String) {
    MEAT_HEAVY("荤菜多", "🥩"),
    VEG_HEAVY("素菜多", "🥬"),
    VEG_ONLY("纯素", "🌱"),
    MEAT_ONLY("纯荤", "🍖")
}

data class SelectionItem(
    val type: String,
    val name: String,
    val avatar: String = "",
    val specialty: String = ""
)

class TodayEatViewModel(
    private val aiRepository: AiRepository,
    private val favoriteRepository: FavoriteRepository,
    private val galleryRepository: com.shijian.data.repository.GalleryRepository,
    private val cookbookRepository: com.shijian.data.repository.CookbookRepository
) : ViewModel() {

    private var generateJob: Job? = null

    private val _isSelecting = mutableStateOf(false)
    val isSelecting: State<Boolean> = _isSelecting

    private val _isGeneratingImage = mutableStateOf(false)
    val isGeneratingImage: State<Boolean> = _isGeneratingImage

    private val _isGenerating = mutableStateOf(false)
    val isGenerating: State<Boolean> = _isGenerating

    private val _selectedDishes = mutableStateOf<List<String>>(emptyList())
    val selectedDishes: State<List<String>> = _selectedDishes

    private val _selectedMaster = mutableStateOf<CuisineType?>(null)
    val selectedMaster: State<CuisineType?> = _selectedMaster

    private val _recipe = mutableStateOf<Recipe?>(null)
    val recipe: State<Recipe?> = _recipe

    private val _preference = mutableStateOf<Preference?>(null)
    val preference: State<Preference?> = _preference

    private val _isAnalyzingDeepInsights = mutableStateOf(false)
    val isAnalyzingDeepInsights: State<Boolean> = _isAnalyzingDeepInsights

    private val _selectionStatus = mutableStateOf("")
    val selectionStatus: State<String> = _selectionStatus

    private val _selectionProgress = mutableFloatStateOf(0f)
    val selectionProgress: FloatState = _selectionProgress

    private val _currentSelection = mutableStateOf<SelectionItem?>(null)
    val currentSelection: State<SelectionItem?> = _currentSelection

    private val _showPreference = mutableStateOf(false)
    val showPreference: State<Boolean> = _showPreference

    private val _favorites = mutableStateOf<List<FavoriteRecipe>>(emptyList())

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        viewModelScope.launch {
            favoriteRepository.favoritesFlow.collect { list ->
                _favorites.value = list
                // Sync current recipe if it exists and has updates in favorites
                _recipe.value?.let { current ->
                    list.find { it.recipe.id == current.id }?.let { fav ->
                        if (fav.recipe.nutritionAnalysis != null || fav.recipe.winePairing != null) {
                             if (_recipe.value != fav.recipe) {
                                 _recipe.value = fav.recipe
                             }
                        }
                    }
                }
            }
            _isGeneratingImage.value = false
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun togglePreference() {
        _showPreference.value = !_showPreference.value
    }

    fun setPreference(pref: Preference) {
        _preference.value = pref
    }

    fun startRandomSelection() {
        viewModelScope.launch {
            generateJob?.cancel() // Cancel any ongoing recipe generation
            _isGenerating.value = false
            _isSelecting.value = true
            _selectedDishes.value = emptyList()
            _selectedMaster.value = null
            _recipe.value = null
            _selectionProgress.floatValue = 0f

            // Stage 1: Select Dishes
            _selectionStatus.value = "正在随机选择菜品..."
            selectRandomDishes()

            // Stage 2: Select Master
            _selectionStatus.value = "正在匹配主厨大师..."
            selectRandomMaster()

            _selectionStatus.value = "选择完成！"
            _selectionProgress.floatValue = 1f
            delay(1000)
            _isSelecting.value = false
        }
    }

    private suspend fun selectRandomDishes() {
        val allIngredients = ConfigData.ingredientCategories.flatMap { it.items }
        
        _preference.value?.let { _ ->
            // Simple filtering logic based on preference
            // In a real app, I'd map ingredients to meat/veg categories more strictly
        }

        val dishCount = 6
        val pool = allIngredients.shuffled().take(dishCount)

        // Animation
        for (i in 0..10) {
            _currentSelection.value = SelectionItem("dish", pool.random())
            _selectionProgress.floatValue = (i / 10f) * 0.5f
            delay(100)
        }

        _selectedDishes.value = pool
    }

    private suspend fun selectRandomMaster() {
        val masters = ConfigData.cuisines
        
        for (i in 0..15) {
            val m = masters.random()
            _currentSelection.value = SelectionItem("master", m.name, m.avatar, m.specialty)
            _selectionProgress.floatValue = 0.5f + (i / 15f) * 0.5f
            delay(80)
        }

        _selectedMaster.value = masters.random()
    }

    fun generateRecipe() {
        if (_selectedMaster.value == null || _selectedDishes.value.isEmpty()) return
        
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            
            // Step 1: Find reference recipes from local database based on ingredients
            val references = cookbookRepository.matchByIngredients(_selectedDishes.value).take(3)
            
            // Step 2: Pass references to AI for real benchmarking
            val result = aiRepository.generateRecipe(
                ingredients = _selectedDishes.value, 
                cuisine = _selectedMaster.value!!, 
                customPrompt = "",
                referenceRecipes = references
            )
            
            result.onSuccess {
                _recipe.value = it
            }.onFailure { e ->
                _errorMessage.value = "生成失败: ${e.message}"
            }
            _isGenerating.value = false
        }
    }

    fun reset() {
        generateJob?.cancel()
        _isGenerating.value = false
        _selectedDishes.value = emptyList()
        _selectedMaster.value = null
        _recipe.value = null
        _currentSelection.value = null
        _selectionProgress.floatValue = 0f
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
    }

    fun isFavorite(recipeId: String): Boolean {
        return _favorites.value.any { it.id == recipeId }
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
            result.onFailure {
                onComplete(null)
            }
            _isGeneratingImage.value = false
        }
    }

    fun unlockDeepInsights(recipe: Recipe) {
        android.util.Log.d("EatWhat-AI", "Unlock requested for recipe: ${recipe.name}")
        viewModelScope.launch {
            _isAnalyzingDeepInsights.value = true
            
            // Parallel execution with synchronized update
            val nutritionDeferred = async { aiRepository.getNutritionAnalysis(recipe) }
            val wineDeferred = async { aiRepository.getWinePairing(recipe) }
            
            val nutritionRes = nutritionDeferred.await()
            val wineRes = wineDeferred.await()
            
            _recipe.value = _recipe.value?.copy(
                nutritionAnalysis = nutritionRes.getOrNull(),
                winePairing = wineRes.getOrNull()
            )
            
            _isAnalyzingDeepInsights.value = false
        }
    }
}
