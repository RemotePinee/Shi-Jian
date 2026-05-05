package com.shijian.data.model

/**
 * 来自 HowToCook 开源项目的离线食谱数据模型。
 * 数据源自 assets/recipes.json，由 Python 脚本从 Markdown 解析生成。
 */

data class CookbookData(
    val version: Int = 1,
    val generatedAt: String = "",
    val totalCount: Int = 0,
    val categories: List<CookbookCategory> = emptyList(),
    val recipes: List<CookbookRecipe> = emptyList(),
    val tips: List<CookbookTip> = emptyList()
)

data class CookbookRecipe(
    val id: String = "",
    val name: String = "",
    val category: String = "",          // e.g. "meat_dish"
    val categoryName: String = "",      // e.g. "荤菜"
    val difficulty: Int = 1,            // 1-5
    val ingredients: List<String> = emptyList(),
    val portions: List<String> = emptyList(),
    val steps: List<CookbookStep> = emptyList(),
    val tips: List<String> = emptyList(),
    val source: String = "HowToCook",
    val imageUrl: String? = null
)

data class CookbookStep(
    val step: Int = 0,
    val description: String = ""
)

data class CookbookCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val count: Int = 0
)

data class CookbookTip(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val content: String = ""
)
