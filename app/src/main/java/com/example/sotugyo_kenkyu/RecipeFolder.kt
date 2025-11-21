package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class RecipeFolder(
    @get:Exclude var id: String = "", // FirestoreのドキュメントID
    val name: String = "",
    val createdAt: Timestamp? = null,
    val recipeCount: Int = 0 // 含まれるレシピ数（表示用にあると便利）
) : Serializable