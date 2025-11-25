package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Record(
    var id: String = "",
    val userId: String = "",
    val menuName: String = "",
    val date: Timestamp? = null,
    val memo: String = "",
    val imageUrl: String = "",

    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    val rating: Float = 0f,
    val postedAt: Timestamp? = null,

    // ★追加: いいねしたユーザーIDのリスト
    val likedUserIds: List<String> = emptyList()
) : Serializable