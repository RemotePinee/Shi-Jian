package com.eatwhat.data.model

data class IngredientCategory(
    val id: String,
    val name: String,
    val icon: String,
    val items: List<String>
)

