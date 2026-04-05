package com.shijian.data.model

data class FortuneResult(
    val id: String = "",
    val type: String = "daily", // daily, mood, couple, number
    val date: String = "",
    val dishName: String = "",
    val reason: String = "",
    val luckyIndex: Int = 0,
    val description: String = "",
    val tips: List<String> = emptyList(),
    val difficulty: String = "medium",
    val cookingTime: Int = 0,
    val mysticalMessage: String = "",
    val luckyAdvice: List<String> = emptyList(),
    val tabooAdvice: List<String> = emptyList(),
    val ingredients: List<String>? = emptyList(),
    val steps: List<String>? = emptyList(),
    val nutritionAnalysis: NutritionAnalysis? = null,
    val winePairing: WinePairing? = null
)





data class ZodiacConfig(
    val id: String,
    val name: String,
    val symbol: String,
    val element: String,
    val traits: List<String>,
    val luckyColors: List<String>,
    val dates: String
)

data class AnimalConfig(
    val id: String,
    val name: String,
    val symbol: String,
    val element: String,
    val traits: List<String>,
    val luckyNumbers: List<Int>,
    val years: List<Int>
)

data class MoodConfig(
    val id: String,
    val name: String,
    val emoji: String,
    val color: String,
    val cookingStyle: List<String>,
    val description: String
)
