package com.example.sotugyo_kenkyu

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class Recipe(
    // Firestoreから取得時にセットするID（保存はしないのでExclude）
    @get:Exclude var id: String = "",

    // アプリ表示用のお気に入りフラグ（保存はしないのでExclude）
    @get:Exclude var isFavorite: Boolean = false,

    val recipeTitle: String = "",
    val foodImageUrl: String = "",
    val recipeUrl: String = "",
    val recipeCost: String = "",
    val recipeIndication: String = "",
    val recipeMaterial: List<String> = emptyList(),
    val categoryPathNames: List<String> = emptyList(),
    val categoryPathIds: List<String> = emptyList()
) : Serializable