package com.example.sotugyo_kenkyu.ai

data class ChatMessage(
    val message: String,
    val isUser: Boolean // trueならユーザー、falseならAI
)