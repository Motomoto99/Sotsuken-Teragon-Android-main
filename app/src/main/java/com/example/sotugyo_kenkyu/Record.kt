package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName // ★追加

data class Record(
    var id: String = "",
    val userId: String = "",
    val menuName: String = "",
    val date: Timestamp? = null,
    val memo: String = "",
    val imageUrl: String = "",

    // ★修正: Firestore上のフィールド名を "isPublic" に強制する
    @get:PropertyName("isPublic") @set:PropertyName("isPublic") var isPublic: Boolean = false,

    val rating: Float = 0f
)