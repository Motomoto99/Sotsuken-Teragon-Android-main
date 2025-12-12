package com.example.sotugyo_kenkyu.ai

/**
 * チャット履歴一覧に表示するためのセッション情報
 * (日時を計算できるように Long 型に変更しました)
 */
data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    // ★追加: アレンジモードかどうか判定するフラグ
    val isArrangeMode: Boolean = false
)