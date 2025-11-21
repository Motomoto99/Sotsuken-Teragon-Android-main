package com.example.sotugyo_kenkyu

// カテゴリー情報の「箱」
data class CategoryData(
    val apiId: String,   // "10" とか "12"
    val name: String,    // "肉" とか
    val iconRes: Int     // R.drawable.xxx　これはintじゃないかもしれないから、将来的に変更する可能性がある。
)