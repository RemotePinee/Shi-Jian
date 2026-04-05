package com.shijian.data.model

data class SauceRecipe(
    val id: String = "",
    val name: String = "",
    val category: String = "other",
    val ingredients: List<String> = emptyList(),
    val steps: List<SauceStep> = emptyList(),
    val makingTime: Int = 0,
    val difficulty: String = "easy",
    val tips: List<String> = emptyList(),
    val storage: StorageInfo = StorageInfo(),
    val pairings: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val spiceLevel: Int? = null,
    val sweetLevel: Int? = null,
    val saltLevel: Int? = null,
    val sourLevel: Int? = null,
    val description: String? = null
)

data class SauceStep(
    val step: Int = 0,
    val description: String = "",
    val time: Int? = null,
    val temperature: String? = null,
    val technique: String? = null
)

data class StorageInfo(
    val method: String = "常温",
    val duration: String = "即刻食用",
    val temperature: String = "室温"
)

data class SaucePreference(
    val spiceLevel: Int,
    val sweetLevel: Int,
    val saltLevel: Int,
    val sourLevel: Int,
    val useCase: List<String>,
    val availableIngredients: List<String>
)
