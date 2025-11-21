package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp

data class Record(
    var id: String = "",
    val userId: String = "",
    val menuName: String = "",
    val date: Timestamp? = null,
    val memo: String = "",
    val imageUrl: String = "",
    val isPublic: Boolean = false,
    val rating: Float = 0f // ★ 追加: 評価（0.0〜5.0）
)