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
import com.example.sotugyo_kenkyu.recipe.SearchResultFragment // ★ここを変更（RecipeListFragmentではなくこちらをインポート）
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
import com.example.sotugyo_kenkyu.BuildConfig

class ImageResultFragment : Fragment() {

    private var selectedUriString: String? = null
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
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

        selectedUriString = arguments?.getString("IMAGE_URI")

        val imageView = view.findViewById<ImageView>(R.id.selectedImageView)
        val btnSearchAction = view.findViewById<Button>(R.id.btnSearchAction)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        if (selectedUriString != null) {
            imageView.setImageURI(Uri.parse(selectedUriString))
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSearchAction.setOnClickListener {
            if (selectedUriString != null) {
                btnSearchAction.isEnabled = false
                btnSearchAction.text = "解析中..."
                view.findViewById<TextView>(R.id.txtMessage).text = "AIに問い合わせています..."

                analyzeImageDirectly(Uri.parse(selectedUriString!!))
            }
        }
    }

    private fun analyzeImageDirectly(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = uriToBitmap(uri)
                if (bitmap == null) {
                    showError("画像の読み込みに失敗しました")
                    return@launch
                }

                val base64Image = bitmapToBase64(bitmap)

                val jsonBody = JSONObject()
                val contentsArray = JSONArray()
                val contentObject = JSONObject()
                val partsArray = JSONArray()

                val textPart = JSONObject()
                textPart.put("text", "この画像の料理名は何ですか？料理名だけを単語で答えてください。（例：カレーライス）。料理でなければ「判定不能」と答えてください。")
                partsArray.put(textPart)

                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mime_type", "image/jpeg")
                inlineData.put("data", base64Image)
                imagePart.put("inline_data", inlineData)
                partsArray.put(imagePart)

                contentObject.put("parts", partsArray)
                contentsArray.put(contentObject)
                jsonBody.put("contents", contentsArray)

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.getJSONObject(0)?.optString("text")?.trim() ?: "判定なし"

                        withContext(Dispatchers.Main) {
                            showResult(text)
                        }
                    } else {
                        showError("AIからの応答が空でした")
                    }
                } else {
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

    // ★★★ ここを修正しました ★★★
    // 解析結果(料理名)を受け取り、Algolia検索画面へ遷移する
    private suspend fun showResult(dishName: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, "「$dishName」を検索します", Toast.LENGTH_SHORT).show()

        // 遷移先を SearchResultFragment に変更
        val fragment = SearchResultFragment()
        val args = Bundle()

        // SearchResultFragment が受け取るキー名 "KEY_SEARCH_WORD" に合わせる
        args.putString("KEY_SEARCH_WORD", dishName)
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        resetButtonState()
    }

    private suspend fun showError(msg: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        view?.findViewById<TextView>(R.id.txtMessage)?.text = "エラーが発生しました"
        resetButtonState()
    }

    private fun resetButtonState() {
        val btn = view?.findViewById<Button>(R.id.btnSearchAction)
        btn?.isEnabled = true
        btn?.text = "検索する"
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            val resolver = requireContext().contentResolver
            inputStream = resolver.openInputStream(uri)

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDimension = 800
            var sampleSize = 1
            while ((options.outHeight / sampleSize) > maxDimension || (options.outWidth / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

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