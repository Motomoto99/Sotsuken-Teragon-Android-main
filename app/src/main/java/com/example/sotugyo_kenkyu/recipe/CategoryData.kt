package com.example.sotugyo_kenkyu.recipe

// カテゴリー情報の「箱」を、画像も絵文字も入るように改造！
data class CategoryData(
    val apiId: String,           // カテゴリID
    val name: String,            // 表示名
    val imageRes: Int?,          // 画像ID (null許容：画像がない場合はnullが入る)
    val emoji: String?,          // 絵文字 (null許容：絵文字がない場合はnullが入る)
    val isOther: Boolean = false // 「その他」かどうか (デフォルトは false)
)