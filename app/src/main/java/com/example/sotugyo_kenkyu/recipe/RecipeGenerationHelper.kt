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
import org.json.JSONObject

/**
 * レシピ手順と分量の生成（Gemini API呼び出し）とFirestoreへの保存を担当するヘルパークラス
 */
class RecipeGenerationHelper {

    private val db = FirebaseFirestore.getInstance()

    // Geminiモデルの初期化
    private val generativeModel by lazy {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app).generativeModel("gemini-2.5-flash")
    }

    /**
     * 手順や分量がない場合にAI生成を実行し、Firestoreに保存する
     */
    fun checkAndRequestGeneration(recipe: Recipe, onStatusUpdate: (String) -> Unit) {
        // 1. 既に手順と分量の両方がある場合は何もしない
        if (!recipe.recipeSteps.isNullOrEmpty() && recipe.servingAmounts.isNotEmpty()) {
            return
        }

        // 2. 生成開始
        onStatusUpdate("AIが手順と分量を生成中です...\n(数秒かかります)")

        // ネットワーク処理なのでバックグラウンドスレッドで実行
        CoroutineScope(Dispatchers.IO).launch {
            val result = callGeminiApi(recipe.recipeTitle, recipe.recipeMaterial.orEmpty())

            if (result != null) {
                val (generatedSteps, generatedAmounts) = result

                // 3. 生成成功 -> Firestoreに保存
                saveDataToFirestore(recipe.id, generatedSteps, generatedAmounts)

                withContext(Dispatchers.Main) {
                    onStatusUpdate("生成完了！表示を更新します...")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("情報の生成に失敗しました。\n通信環境などを確認してください。")
                }
            }
        }
    }

    /**
     * Firebase SDK (Vertex AI) を呼び出して手順と分量を取得する
     * 戻り値: Pair<手順リスト, 分量リスト>?
     */
    private suspend fun callGeminiApi(title: String, materials: List<String>): Pair<List<String>, List<String>>? {
        return try {
            val materialString = materials.joinToString(", ")

            val promptText = """
                あなたはプロの料理家です。
                以下の料理名と材料リストから、一人暮らしの料理初心者向けの「一人前の分量」と「料理の手順」を生成してください。
                材料にないものは勝手に追加しないでください。
                料理名: $title
                材料リスト: $materialString
                
                【重要】
                1. "amounts" 配列には、提供された「材料リスト」と**全く同じ順番**で、一人前の分量（例: "100g", "大さじ1"）を入れてください。材料が適量の場合は"適量"としてください。要素数は材料リストと必ず一致させてください。
                2. "steps" 配列には、調理手順を箇条書きで入れてください。
                
                出力は以下のJSON形式のみで行ってください（Markdown記法は不要）：
                {
                  "amounts": ["分量1", "分量2", ...],
                  "steps": ["手順1", "手順2", ...]
                }
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
     * 生成されたテキストからJSONを抽出してリストにする
     */
    private fun parseGeneratedText(rawText: String): Pair<List<String>, List<String>>? {
        val steps = ArrayList<String>()
        val amounts = ArrayList<String>()

        try {
            // マークダウン記法が含まれている場合の除去
            var text = rawText.replace("```json", "").replace("```", "").trim()

            // JSONオブジェクト { ... } の範囲を抽出
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')

            if (start != -1 && end != -1) {
                text = text.substring(start, end + 1)
                val jsonObject = JSONObject(text)

                // steps配列の抽出
                if (jsonObject.has("steps")) {
                    val stepsArray = jsonObject.getJSONArray("steps")
                    for (i in 0 until stepsArray.length()) {
                        steps.add(stepsArray.getString(i))
                    }
                }

                // amounts配列の抽出
                if (jsonObject.has("amounts")) {
                    val amountsArray = jsonObject.getJSONArray("amounts")
                    for (i in 0 until amountsArray.length()) {
                        amounts.add(amountsArray.getString(i))
                    }
                }

                return Pair(steps, amounts)
            } else {
                Log.w("GeminiAPI", "JSON format not found in response: $rawText")
            }

        } catch (e: Exception) {
            Log.e("GeminiAPI", "Parse error", e)
        }
        return null
    }

    /**
     * Firestoreに手順と分量を保存する
     */
    private fun saveDataToFirestore(docId: String, steps: List<String>, amounts: List<String>) {
        if (docId.isEmpty()) return

        val data = mapOf(
            "recipeSteps" to steps,
            "servingAmounts" to amounts
        )

        db.collection("recipes").document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to save data", e)
            }
    }
}