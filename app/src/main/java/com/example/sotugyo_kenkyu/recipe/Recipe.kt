package com.example.sotugyo_kenkyu.recipe

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class Recipe(
    // Firestoreから取得時にセットするID（保存はしないのでExclude）
    @get:Exclude var id: String = "",

    // アプリ表示用のお気に入りフラグ（保存はしないのでExclude）
    @get:Exclude var isFavorite: Boolean = false,

    // --- Firestoreのフィールド (すべて var に変更) ---
    var recipeTitle: String = "",
    var foodImageUrl: String = "",
    var recipeUrl: String = "",
    var recipeCost: String = "",
    var recipeIndication: String = "",
    var recipeMaterial: List<String> = emptyList(),

    // カテゴリ情報
    var primaryCategoryId: String = "", // ★追加: 検索やフィルタリングで重要
    var categoryPathNames: List<String> = emptyList(),
    var categoryPathIds: List<String> = emptyList(),

    // --- Gemini連携用 ---
    var aiPrompt: String? = null,
    var recipeStepsText: String? = null,

    // アプリ表示用（整形後）
    var recipeSteps: List<String>? = null

) : Serializable