package com.example.sotugyo_kenkyu

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sotugyo_kenkyu.recipe.RecipeListFragment // ※パッケージ構成に合わせて調整してください
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
// ★ BuildConfigをインポート（赤字なら一度 Rebuild Project してください）
import com.example.sotugyo_kenkyu.BuildConfig

class ImageResultFragment : Fragment() {

    private var selectedUriString: String? = null

    // ★★★ 安全にAPIキーを読み込みます ★★★
    // local.properties に書いた GEMINI_API_KEY がここで使われます
    private val apiKey = BuildConfig.GEMINI_API_KEY

    // 通信クライアント (OkHttp)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // タイムアウトを30秒に設定
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 前画面から画像のURIを受け取る
        selectedUriString = arguments?.getString("IMAGE_URI")

        val imageView = view.findViewById<ImageView>(R.id.selectedImageView)
        val btnSearchAction = view.findViewById<Button>(R.id.btnSearchAction)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // 画像を表示
        if (selectedUriString != null) {
            imageView.setImageURI(Uri.parse(selectedUriString))
        }

        // キャンセルボタン
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 検索ボタン
        btnSearchAction.setOnClickListener {
            if (selectedUriString != null) {
                // UI更新（連打防止）
                btnSearchAction.isEnabled = false
                btnSearchAction.text = "解析中..."
                view.findViewById<TextView>(R.id.txtMessage).text = "AIに問い合わせています..."

                // 解析開始
                analyzeImageDirectly(Uri.parse(selectedUriString!!))
            }
        }
    }

    // ★ Gemini API (REST) を直接叩く処理
    private fun analyzeImageDirectly(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 画像をリサイズして取得（メモリ不足対策）
                val bitmap = uriToBitmap(uri)
                if (bitmap == null) {
                    showError("画像の読み込みに失敗しました")
                    return@launch
                }

                // 2. 画像をBase64文字列に変換
                val base64Image = bitmapToBase64(bitmap)

                // 3. 送信するJSONデータを作成
                val jsonBody = JSONObject()
                val contentsArray = JSONArray()
                val contentObject = JSONObject()
                val partsArray = JSONArray()

                // プロンプト（質問）
                val textPart = JSONObject()
                textPart.put("text", "この画像の料理名は何ですか？料理名だけを単語で答えてください。（例：カレーライス）。料理でなければ「判定不能」と答えてください。")
                partsArray.put(textPart)

                // 画像データ
                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mime_type", "image/jpeg")
                inlineData.put("data", base64Image)
                imagePart.put("inline_data", inlineData)
                partsArray.put(imagePart)

                contentObject.put("parts", partsArray)
                contentsArray.put(contentObject)
                jsonBody.put("contents", contentsArray)

                // 4. HTTPリクエストを作成 (Gemini 1.5 Flashを使用)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                // 5. 送信実行
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    // JSONから答えを取り出す
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.getJSONObject(0)?.optString("text")?.trim() ?: "判定なし"

                        // 成功！結果を表示
                        withContext(Dispatchers.Main) {
                            showResult(text)
                        }
                    } else {
                        showError("AIからの応答が空でした")
                    }
                } else {
                    // エラーハンドリング
                    val errorMsg = if (response.code == 429) {
                        "利用制限（混雑）のため、少し時間を置いてください"
                    } else {
                        "通信エラー: ${response.code}"
                    }
                    showError(errorMsg)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showError("エラー: ${e.localizedMessage}")
            }
        }
    }

    // 解析結果を受け取り、次の画面へ遷移する処理
    private suspend fun showResult(dishName: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, "「$dishName」が見つかりました！", Toast.LENGTH_LONG).show()

        // 検索結果画面（RecipeListFragment）へ遷移
        val fragment = RecipeListFragment()
        val args = Bundle()
        // 検索ワードとして料理名を渡す
        args.putString("SEARCH_QUERY", dishName)
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        resetButtonState()
    }

    // エラー表示処理
    private suspend fun showError(msg: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        view?.findViewById<TextView>(R.id.txtMessage)?.text = "エラーが発生しました"
        resetButtonState()
    }

    // ボタンの状態を元に戻す
    private fun resetButtonState() {
        val btn = view?.findViewById<Button>(R.id.btnSearchAction)
        btn?.isEnabled = true
        btn?.text = "検索する"
    }

    // BitmapをBase64文字列に変換する関数
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // JPEG品質70%で圧縮（サイズ軽量化のため）
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // 画像をURIから読み込み、メモリ不足にならないようリサイズする関数
    private suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            val resolver = requireContext().contentResolver
            inputStream = resolver.openInputStream(uri)

            // まず画像のサイズだけを読み込む（メモリには展開しない）
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // リサイズ比率を計算（長辺が800px以下になるように）
            val maxDimension = 800
            var sampleSize = 1
            while ((options.outHeight / sampleSize) > maxDimension || (options.outWidth / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            // 計算したサイズで実際に画像を読み込む
            val finalOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
            }
            inputStream = resolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, finalOptions)
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            inputStream?.close()
        }
    }
}