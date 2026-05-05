package com.shijian.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.shijian.data.model.CookbookCategory
import com.shijian.data.model.CookbookRecipe
import com.shijian.data.model.CookbookTip
import com.shijian.data.repository.CookbookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CookbookViewModel(
    private val cookbookRepository: CookbookRepository
) : ViewModel() {

    // --- State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<Int?>(null)
    val selectedDifficulty: StateFlow<Int?> = _selectedDifficulty.asStateFlow()

    private val _recipes = MutableStateFlow<List<CookbookRecipe>>(emptyList())
    val recipes: StateFlow<List<CookbookRecipe>> = _recipes.asStateFlow()

    private val _categories = MutableStateFlow<List<CookbookCategory>>(emptyList())
    val categories: StateFlow<List<CookbookCategory>> = _categories.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<CookbookRecipe?>(null)
    val selectedRecipe: StateFlow<CookbookRecipe?> = _selectedRecipe.asStateFlow()

    private val _tips = MutableStateFlow<List<CookbookTip>>(emptyList())
    val tips: StateFlow<List<CookbookTip>> = _tips.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _categories.value = cookbookRepository.getCategories()
        _totalCount.value = cookbookRepository.getTotalCount()
        _tips.value = cookbookRepository.getAllTips()
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onCategorySelected(categoryId: String?) {
        _selectedCategory.value = if (_selectedCategory.value == categoryId) null else categoryId
        applyFilters()
    }

    fun onDifficultySelected(difficulty: Int?) {
        _selectedDifficulty.value = if (_selectedDifficulty.value == difficulty) null else difficulty
        applyFilters()
    }

    fun selectRecipe(recipe: CookbookRecipe?) {
        _selectedRecipe.value = recipe
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedDifficulty.value = null
        applyFilters()
    }

    private fun applyFilters() {
        var result = if (_searchQuery.value.isNotBlank()) {
            cookbookRepository.search(_searchQuery.value)
        } else {
            cookbookRepository.getAllRecipes()
        }

        _selectedCategory.value?.let { catId ->
            result = result.filter { it.category == catId }
        }

        _selectedDifficulty.value?.let { diff ->
            result = result.filter { it.difficulty == diff }
        }

        _recipes.value = result
    }

    /**
     * Build a prompt for AI to improve this recipe.
     */
    fun buildAiImprovementPrompt(recipe: CookbookRecipe): String {
        val ingredientText = if (recipe.portions.isNotEmpty()) {
            recipe.portions.joinToString("、")
        } else {
            recipe.ingredients.joinToString("、")
        }
        val stepsText = recipe.steps.joinToString("\n") { "${it.step}. ${it.description}" }
        return buildString {
            append("我有一道菜叫「${recipe.name}」（${recipe.categoryName}），")
            append("难度 ${"★".repeat(recipe.difficulty)}，")
            append("食材：$ingredientText。\n\n")
            append("原始做法：\n$stepsText\n\n")
            append("请帮我改良优化这道菜的做法，可以提出创新的调味、烹饪技巧或摆盘建议。")
        }
    }
}
