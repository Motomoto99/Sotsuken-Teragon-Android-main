package com.example.sotugyo_kenkyu

import com.google.firebase.Timestamp

data class Notification(
    val title: String = "",
    val content: String = "",
    val date: Timestamp? = null,
    // ★追加: 送信者のUID（これがユーザーアイコン表示に必要です）
    val senderUid: String? = null
)