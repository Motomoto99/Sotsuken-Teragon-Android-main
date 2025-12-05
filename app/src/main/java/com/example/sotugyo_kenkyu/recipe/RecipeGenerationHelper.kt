package com.example.sotugyo_kenkyu.recipe

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * レシピ手順の生成（Gemini API呼び出し）とFirestoreへの保存を担当するヘルパークラス
 */
class RecipeGenerationHelper {

    private val db = FirebaseFirestore.getInstance()

    // ★★★ ここにGemini APIキーを設定してください ★★★
    // 注意: アプリ内にキーを置くことはセキュリティリスクがあります（テスト用・個人用として扱ってください）
    private val GEMINI_API_KEY = "ここにAPIいれてくれぱいせん"

    private val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"

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
                saveStepsToFirestore(recipe.id, generatedSteps)

                withContext(Dispatchers.Main) {
                    onStatusUpdate("生成完了！表示を更新します...")
                }
            } else {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("手順の生成に失敗しました。\n通信環境やAPIキーを確認してください。")
                }
            }
        }
    }

    /**
     * Gemini APIを呼び出して手順リストを取得する
     */
    private fun callGeminiApi(title: String, materials: List<String>): List<String>? {
        try {
            val materialString = materials.joinToString(", ")

            // プロンプト（指示文）
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

            // JSONボディの作成
            val jsonBody = JSONObject()
            val contents = JSONArray()
            val part = JSONObject()
            val text = JSONObject()

            text.put("text", promptText)
            part.put("parts", JSONArray().put(text))
            contents.put(part)

            jsonBody.put("contents", contents)

            // HTTPリクエスト
            val url = URL(GEMINI_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                // レスポンス解析
                return parseGeminiResponse(response.toString())
            } else {
                Log.e("GeminiAPI", "Error Code: $responseCode")
            }

        } catch (e: Exception) {
            Log.e("GeminiAPI", "Request failed", e)
        }
        return null
    }

    /**
     * GeminiのレスポンスJSONから手順リストを抽出する
     */
    private fun parseGeminiResponse(jsonString: String): List<String> {
        val steps = ArrayList<String>()
        try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            var text = parts.getJSONObject(0).getString("text")

            // マークダウン記法の除去 (```json ... ```)
            text = text.replace("```json", "").replace("```", "").trim()

            // JSON配列としてパース
            // JSON配列の開始位置を探す（余計な文字が含まれている場合対策）
            val start = text.indexOf('[')
            val end = text.lastIndexOf(']')

            if (start != -1 && end != -1) {
                text = text.substring(start, end + 1)
                val jsonArray = JSONArray(text)
                for (i in 0 until jsonArray.length()) {
                    steps.add(jsonArray.getString(i))
                }
            }

        } catch (e: Exception) {
            Log.e("GeminiAPI", "Parse error", e)
        }
        return steps
    }

    /**
     * Firestoreに手順リストを保存する
     */
    private fun saveStepsToFirestore(docId: String, steps: List<String>) {
        if (docId.isEmpty()) return

        val data = mapOf("recipeSteps" to steps)

        db.collection("recipes").document(docId)
            .set(data, SetOptions.merge()) // 既存データを消さずにマージ
            .addOnSuccessListener {
                Log.d("Firestore", "Recipe steps saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to save steps", e)
            }
    }
}