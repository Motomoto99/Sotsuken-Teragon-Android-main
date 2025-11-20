package com.example.sotugyo_kenkyu

data class ChatMessage(
    val message: String,
    val isUser: Boolean // trueならユーザー、falseならAI
)