package com.eatwhat.data.model

data class CuisineType(
    val id: String,
    val name: String,
    val description: String,
    val avatar: String,
    val specialty: String,
    val prompt: String
)

data class Recipe(
    val id: String = "",
    val name: String = "",
    val cuisine: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<RecipeStep> = emptyList(),
    val cookingTime: Int = 0,
    val difficulty: String = "medium", // "easy", "medium", "hard"
    val tips: List<String> = emptyList(),
    val nutritionAnalysis: NutritionAnalysis? = null,
    val winePairing: WinePairing? = null
)

data class RecipeStep(
    val step: Int = 0,
    val description: String = "",
    val time: Int? = null,
    val temperature: String? = null,
    val image: String? = null
)

data class NutritionInfo(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int,
    val sodium: Int,
    val sugar: Int,
    val vitaminC: Int? = null,
    val calcium: Int? = null,
    val iron: Int? = null
)

data class NutritionAnalysis(
    val nutrition: NutritionInfo,
    val healthScore: Int,
    val balanceAdvice: List<String>,
    val dietaryTags: List<String>,
    val servingSize: String
)

data class WinePairing(
    val name: String,
    val type: String,
    val reason: String,
    val servingTemperature: String,
    val glassType: String? = null,
    val alcoholContent: String? = null,
    val flavor: String,
    val origin: String? = null
)
