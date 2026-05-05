package com.shijian.data.repository

import android.content.Context
import com.google.gson.Gson
import com.shijian.data.model.CookbookCategory
import com.shijian.data.model.CookbookData
import com.shijian.data.model.CookbookRecipe
import com.shijian.data.model.CookbookTip

/**
 * Repository for accessing the offline HowToCook recipe library.
 * Loads data from assets/recipes.json on first access and caches in memory.
 */
class CookbookRepository(private val context: Context) {

    private var cachedData: CookbookData? = null

    private fun ensureLoaded(): CookbookData {
        cachedData?.let { return it }
        val json = context.assets.open("recipes.json").bufferedReader().use { it.readText() }
        val data = Gson().fromJson(json, CookbookData::class.java)
        cachedData = data
        return data
    }

    fun getAllRecipes(): List<CookbookRecipe> = ensureLoaded().recipes

    fun getCategories(): List<CookbookCategory> = ensureLoaded().categories

    fun getByCategory(categoryId: String): List<CookbookRecipe> {
        return getAllRecipes().filter { it.category == categoryId }
    }

    fun getByDifficulty(stars: Int): List<CookbookRecipe> {
        return getAllRecipes().filter { it.difficulty == stars }
    }

    fun search(keyword: String): List<CookbookRecipe> {
        if (keyword.isBlank()) return getAllRecipes()
        val lower = keyword.lowercase()
        return getAllRecipes().filter { recipe ->
            recipe.name.lowercase().contains(lower) ||
            recipe.ingredients.any { it.lowercase().contains(lower) } ||
            recipe.categoryName.lowercase().contains(lower)
        }
    }

    fun getRecipeById(id: String): CookbookRecipe? {
        return getAllRecipes().find { it.id == id }
    }

    fun getRandomRecipe(): CookbookRecipe? {
        val all = getAllRecipes()
        return if (all.isNotEmpty()) all.random() else null
    }

    fun getRandomRecipes(count: Int): List<CookbookRecipe> {
        val all = getAllRecipes()
        return if (all.size <= count) all.shuffled() else all.shuffled().take(count)
    }

    fun getTotalCount(): Int = ensureLoaded().totalCount

    fun getAllTips(): List<CookbookTip> {
        val allTips = ensureLoaded().tips
        val priorityMap = mapOf(
            "烹饪技巧" to 1,
            "烹饪基础" to 2,
            "进阶技巧" to 3
        )
        return allTips.sortedWith(compareBy<CookbookTip> { 
            priorityMap[it.category] ?: 99 
        }.thenBy { it.id }) // Keep stable sort within category
    }

    /**
     * Search recipes by a list of ingredients the user has.
     * Returns recipes sorted by how many ingredients match.
     */
    fun matchByIngredients(userIngredients: List<String>): List<CookbookRecipe> {
        if (userIngredients.isEmpty()) return emptyList()
        val lowerIngredients = userIngredients.map { it.lowercase() }
        return getAllRecipes()
            .map { recipe ->
                val matchCount = recipe.ingredients.count { ing ->
                    lowerIngredients.any { userIng -> ing.lowercase().contains(userIng) || userIng.contains(ing.lowercase()) }
                }
                recipe to matchCount
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
