package com.eatwhat.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eatwhat.data.model.*
import com.eatwhat.data.repository.AiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

import com.eatwhat.data.repository.FavoriteRepository
import java.text.SimpleDateFormat
import java.util.*

enum class FortuneType(val label: String, val icon: String, val description: String) {
    DAILY("今日运势菜", "⭐", "根据星座生肖推荐幸运菜品"),
    MOOD("心情料理师", "💝", "根据心情推荐治愈菜品"),
    NUMBER("幸运数字菜", "🔢", "通过数字占卜推荐菜品")
}

class FortuneCookingViewModel(
    private val aiRepository: AiRepository,
    private val favoriteRepository: FavoriteRepository,
    private val galleryRepository: com.eatwhat.data.repository.GalleryRepository
) : ViewModel() {

    private val _isGeneratingImage = mutableStateOf(false)
    val isGeneratingImage: State<Boolean> = _isGeneratingImage

    private val _selectedType = mutableStateOf(FortuneType.DAILY)
    val selectedType: State<FortuneType> = _selectedType

    private val _zodiac = mutableStateOf("")
    val zodiac: State<String> = _zodiac

    private val _animal = mutableStateOf("")
    val animal: State<String> = _animal

    private val _selectedMoods = mutableStateListOf<String>()
    val selectedMoods: List<String> = _selectedMoods

    private val _moodIntensity = mutableFloatStateOf(3f)

    private val _favoritesRevision = mutableIntStateOf(0)
    val favoritesRevision: State<Int> = _favoritesRevision


    private val _luckyNumber = mutableIntStateOf(1)
    val luckyNumber: State<Int> = _luckyNumber

    private val _fortuneResult = mutableStateOf<FortuneResult?>(null)
    val fortuneResult: State<FortuneResult?> = _fortuneResult

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        viewModelScope.launch {
            favoriteRepository.favoritesFlow.collect { list ->
                _favoritesRevision.intValue += 1
                // Sync current fortune result with updates from favorites
                _fortuneResult.value?.let { current ->
                    list.find { it.recipe.id == current.id }?.let { fav ->
                        if (fav.recipe.nutritionAnalysis != null || fav.recipe.winePairing != null) {
                             _fortuneResult.value = _fortuneResult.value?.copy(
                                 nutritionAnalysis = fav.recipe.nutritionAnalysis,
                                 winePairing = fav.recipe.winePairing
                             )
                        }
                    }
                }
            }
        }
    }

    private val _isAnalyzingDeepInsights = mutableStateOf(false)
    val isAnalyzingDeepInsights: State<Boolean> = _isAnalyzingDeepInsights

    private val _favorites = mutableStateOf<List<FavoriteRecipe>>(emptyList())

    init {
        viewModelScope.launch {
            favoriteRepository.favoritesFlow.collect {
                _favorites.value = it
            }
        }
    }

    fun selectType(type: FortuneType) {
        _selectedType.value = type
        _fortuneResult.value = null
        _errorMessage.value = null
    }

    fun setZodiac(id: String) { 
        _zodiac.value = id 
        _errorMessage.value = null
    }
    fun setAnimal(id: String) { 
        _animal.value = id 
        _errorMessage.value = null
    }

    fun toggleMood(id: String) {
        if (_selectedMoods.contains(id)) _selectedMoods.remove(id)
        else _selectedMoods.add(id)
        _errorMessage.value = null
    }

    fun generateRandomNumber() { _luckyNumber.intValue = (1..99).random() }

    fun isInputValid(): Boolean {
        return when (_selectedType.value) {
            FortuneType.DAILY -> _zodiac.value.isNotEmpty() && _animal.value.isNotEmpty()
            FortuneType.MOOD -> _selectedMoods.isNotEmpty()
            FortuneType.NUMBER -> true
        }
    }

    fun startFortune() {
        if (!isInputValid()) {
            _errorMessage.value = "请先选好星座和生肖哦！"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _fortuneResult.value = null
            _errorMessage.value = null
            
            // Artificial delay for mystical feel
            delay(1500)

            val result = when (_selectedType.value) {
                FortuneType.DAILY -> aiRepository.generateDailyFortune(_zodiac.value, _animal.value)
                FortuneType.MOOD -> aiRepository.generateMoodCooking(_selectedMoods.toList(), _moodIntensity.floatValue.toInt())
                FortuneType.NUMBER -> aiRepository.generateNumberFortune(_luckyNumber.intValue)
            }
            
            result.onSuccess {
                _fortuneResult.value = it
            }.onFailure { e ->
                _errorMessage.value = "占卜失败：${e.localizedMessage ?: "未知错误"}"
            }
            _isLoading.value = false
        }
    }

    fun isFavorite(id: String): Boolean {
        // Read favoritesRevision to trigger recomposition when favorites change
        favoritesRevision.value
        return _favorites.value.any { it.id == id }
    }

    fun toggleFavorite(fortune: FortuneResult) {
        if (favoriteRepository.isFavorite(fortune.id)) {
            favoriteRepository.removeFavorite(fortune.id)
            _favoritesRevision.intValue++
        } else {
            val recipe = Recipe(
                id = fortune.id,
                name = fortune.dishName,
                cuisine = when(fortune.type) {
                    "daily" -> "今日占卜"
                    "mood" -> "心情占卜"
                    "number" -> "数字占卜"
                    else -> "神秘占卜"
                },
                ingredients = fortune.ingredients ?: emptyList(),
                steps = fortune.steps?.mapIndexed { index, s -> RecipeStep(index + 1, s) } ?: emptyList(),
                cookingTime = fortune.cookingTime,
                difficulty = fortune.difficulty,
                tips = fortune.tips
            )
            favoriteRepository.addFavorite(
                FavoriteRecipe(
                    id = recipe.id,
                    recipe = recipe,
                    favoriteDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
            )
            _favoritesRevision.intValue++
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

    fun unlockDeepInsights(fortune: FortuneResult) {
        viewModelScope.launch {
            _isAnalyzingDeepInsights.value = true
            
            val proxyRecipe = Recipe(
                name = fortune.dishName,
                ingredients = fortune.ingredients ?: emptyList()
            )
            
            // Parallel execution with synchronized update
            val nutritionDeferred = async { aiRepository.getNutritionAnalysis(proxyRecipe) }
            val wineDeferred = async { aiRepository.getWinePairing(proxyRecipe) }
            
            val nutritionRes = nutritionDeferred.await()
            val wineRes = wineDeferred.await()
            
            _fortuneResult.value = _fortuneResult.value?.copy(
                nutritionAnalysis = nutritionRes.getOrNull(),
                winePairing = wineRes.getOrNull()
            )
            
            _isAnalyzingDeepInsights.value = false
        }
    }
}
