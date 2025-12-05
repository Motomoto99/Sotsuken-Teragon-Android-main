package com.example.sotugyo_kenkyu.recipe

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * レシピ手順の生成（Gemini API呼び出し）とFirestoreへの保存を担当するヘルパークラス
 */
class RecipeGenerationHelper {

    private val db = FirebaseFirestore.getInstance()

    // Geminiモデルの初期化
    private val generativeModel by lazy {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app).generativeModel("gemini-2.5-flash")
    }

    /**
     * 手順がない場合にAI生成を実行し、Firestoreに保存する
     */
    fun checkAndRequestGeneration(recipe: Recipe, onStatusUpdate: (String) -> Unit) {
        // 1. 既に手順がある場合は何もしない
        if (recipe.recipeSteps.orEmpty().isNotEmpty()) {
            return
        }

        // 2. 生成開始
        onStatusUpdate("AIが手順を生成中です...\n(数秒かかります)")

        // ネットワーク処理なのでバックグラウンドスレッドで実行
        CoroutineScope(Dispatchers.IO).launch {
            val generatedSteps = callGeminiApi(recipe.recipeTitle, recipe.recipeMaterial.orEmpty())

            if (generatedSteps != null && generatedSteps.isNotEmpty()) {
                // 3. 生成成功 -> Firestoreに保存
                // ここで saveStepsToFirestore が見つからない場合、この関数の外に定義があるか確認してください
                saveStepsToFirestore(recipe.id, generatedSteps)

                withContext(Dispatchers.Main) {
                    onStatusUpdate("生成完了！表示を更新します...")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("手順の生成に失敗しました。\n通信環境などを確認してください。")
                }
            }
        }
    }

    /**
     * Firebase SDK (Vertex AI) を呼び出して手順リストを取得する
     */
    private suspend fun callGeminiApi(title: String, materials: List<String>): List<String>? {
        return try {
            val materialString = materials.joinToString(", ")

            val promptText = """
                あなたはプロの料理家です。
                以下の料理名と材料リストだけを使って、「料理の手順」を生成してください。
                材料リストにない食材は絶対に追加しないでください。
                
                料理名: $title
                材料: $materialString
                
                手順は、簡潔なJSON配列（Stringのリスト）形式のみで出力してください。
                余計なマークダウン記法（```json など）は不要です。
                例: ["まず具材を切ります。", "次に炒めます。"]
            """.trimIndent()

            val response = generativeModel.generateContent(promptText)
            val responseText = response.text

            if (!responseText.isNullOrEmpty()) {
                parseGeneratedText(responseText)
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e("GeminiAPI", "Request failed", e)
            null
        }
    }

    /**
     * 生成されたテキストからJSON配列部分を抽出してリストにする
     */
    private fun parseGeneratedText(rawText: String): List<String> {
        val steps = ArrayList<String>()
        try {
            // マークダウン記法が含まれている場合の除去
            var text = rawText.replace("```json", "").replace("```", "").trim()

            val start = text.indexOf('[')
            val end = text.lastIndexOf(']')

            if (start != -1 && end != -1) {
                text = text.substring(start, end + 1)
                val jsonArray = JSONArray(text)
                for (i in 0 until jsonArray.length()) {
                    steps.add(jsonArray.getString(i))
                }
            } else {
                Log.w("GeminiAPI", "JSON format not found in response: $rawText")
            }

        } catch (e: Exception) {
            Log.e("GeminiAPI", "Parse error", e)
        }
        // ★ここ重要: try-catchを抜けた後に必ずリストを返す
        return steps
    }

    /**
     * Firestoreに手順リストを保存する
     */
    private fun saveStepsToFirestore(docId: String, steps: List<String>) {
        if (docId.isEmpty()) return

        val data = mapOf("recipeSteps" to steps)

        db.collection("recipes").document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe steps saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to save steps", e)
            }
    }
}