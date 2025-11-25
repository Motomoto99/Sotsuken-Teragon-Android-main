package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class Record(
    var id: String = "",
    val userId: String = "",
    val menuName: String = "",
    val date: Timestamp? = null,      // 記録日（食べた日）
    val memo: String = "",
    val imageUrl: String = "",

    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    val rating: Float = 0f,

    // ★追加: 公開日時（みんなの投稿の並び順用）
    val postedAt: Timestamp? = null
) : Serializable