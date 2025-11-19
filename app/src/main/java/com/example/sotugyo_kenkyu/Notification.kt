package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp

// Firestoreのデータを受け取るためのクラス
// プロパティ名はFirestoreのフィールド名と一致させる必要があります
data class Notification(
    val title: String = "",
    val content: String = "",
    val date: Timestamp? = null
)