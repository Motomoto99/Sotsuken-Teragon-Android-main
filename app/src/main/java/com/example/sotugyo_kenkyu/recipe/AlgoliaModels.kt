package com.example.sotugyo_kenkyu.recipe

import kotlinx.serialization.Serializable

// Algoliaからのレスポンス全体
@Serializable
data class AlgoliaResponse(
    val hits: List<RecipeItem>
)

// Algoliaの1件分のデータ（ここをRecipeに合わせて拡張したわ！）
@Serializable
data class RecipeItem(
    val objectID: String,               // AlgoliaのID（必須）
    val recipeTitle: String? = null,    // 料理名
    val foodImageUrl: String? = null,   // 画像URL
    val recipeCost: String? = null,     // 費用
    val recipeIndication: String? = null, // 目安時間
    val recipeMaterial: List<String>? = null, // 材料リスト
    val categoryPathNames: List<String>? = null // カテゴリ名リスト
)