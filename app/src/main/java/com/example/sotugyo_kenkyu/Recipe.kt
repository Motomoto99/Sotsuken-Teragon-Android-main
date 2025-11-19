package com.example.sotugyo_kenkyu

import java.io.Serializable

data class Recipe(
    val recipeTitle: String = "",
    val foodImageUrl: String = "",
    val recipeUrl: String = "",
    val recipeCost: String = "",
    val recipeIndication: String = "",
    // 配列データ
    val recipeMaterial: List<String> = emptyList(),
    val categoryPathNames: List<String> = emptyList(),
    val categoryPathIds: List<String> = emptyList() // ★ここを追加！検索に使います
) : Serializable