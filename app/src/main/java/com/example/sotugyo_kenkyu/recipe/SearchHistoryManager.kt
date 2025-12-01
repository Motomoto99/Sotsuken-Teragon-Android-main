package com.example.sotugyo_kenkyu.recipe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val MAX_HISTORY = 20

    // 履歴を保存する
    fun saveHistory(keyword: String) {
        if (keyword.isBlank()) return

        val list = getHistory().toMutableList()

        // すでに同じ単語があったら削除（先頭に持ってくるため）
        list.remove(keyword)
        // 先頭に追加
        list.add(0, keyword)

        // 20件を超えたら古いものを削除
        if (list.size > MAX_HISTORY) {
            list.removeAt(list.size - 1)
        }

        // 保存（リストをJSON文字列に変換して保存）
        val json = gson.toJson(list)
        prefs.edit().putString("history_list", json).apply()
    }

    // 履歴を取得する
    fun getHistory(): List<String> {
        val json = prefs.getString("history_list", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // 履歴を全消去（必要なら）
    fun clearHistory() {
        prefs.edit().remove("history_list").apply()
    }
}