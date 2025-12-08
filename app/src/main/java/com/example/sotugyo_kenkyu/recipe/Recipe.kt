package com.example.sotugyo_kenkyu.recipe

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class Recipe(
    // Firestoreから取得時にセットするID（保存はしないのでExclude）
    @get:Exclude var id: String = "",

    // アプリ表示用のお気に入りフラグ（保存はしないのでExclude）
    @get:Exclude var isFavorite: Boolean = false,

    // --- Firestoreのフィールド (読み書きできるよう var に変更) ---
    var recipeTitle: String = "",
    var foodImageUrl: String = "",
    var recipeUrl: String = "",
    var recipeCost: String = "",
    var recipeIndication: String = "",

    // null許容かつ初期値を持たせる
    var recipeMaterial: List<String>? = null,

    // ★追加: 材料に対応する分量リスト
    var servingAmounts: List<String> = emptyList(),

    // カテゴリ情報
    var primaryCategoryId: String = "",
    var categoryPathNames: List<String> = emptyList(),
    var categoryPathIds: List<String> = emptyList(),

    // --- Gemini連携用 ---
    var aiPrompt: String? = null,
    var recipeStepsText: String? = null,

    // アプリ表示用（整形後）
    var recipeSteps: List<String>? = null

) : Serializable