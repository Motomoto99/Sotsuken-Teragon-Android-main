package com.example.sotugyo_kenkyu.recipe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sotugyo_kenkyu.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class ImageResultFragment : Fragment() {

    private var selectedUriString: String? = null

    // ★ここにGoogle AI Studioで取得したAPIキーを入れてください
    private val apiKey = "AIzaSyCKu_ilU4mAeqan30b_LRAx-jcIyvnEnPE"

    // Geminiモデルの初期化 (高速な gemini-1.5-flash を使用)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-pro",
        apiKey = apiKey
    )

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
        val txtMessage = view.findViewById<TextView>(R.id.txtMessage)
        val btnSearchAction = view.findViewById<Button>(R.id.btnSearchAction)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // 画像表示
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
                // UIの更新（ボタンを無効化し、メッセージを変更）
                btnSearchAction.isEnabled = false
                btnSearchAction.text = "解析中..."
                txtMessage.text = "Geminiが画像を分析しています..."

                // 解析開始
                analyzeImageWithGemini(Uri.parse(selectedUriString!!))
            }
        }
    }

    private fun analyzeImageWithGemini(uri: Uri) {
        // コルーチンを開始（非同期処理）
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 画像をBitmap形式に変換
                val bitmap = uriToBitmap(uri)

                if (bitmap != null) {
                    // 2. Geminiへの入力データを作成
                    val inputContent = content {
                        image(bitmap)
                        text("この画像の料理名は何ですか？料理名だけを答えてください。（例：カレーライス）もし料理でなければ「料理ではありません」と答えてください。")
                    }

                    // 3. Geminiに送信して回答を待つ
                    val response = generativeModel.generateContent(inputContent)
                    val resultText = response.text ?: "判定できませんでした"

                    // 4. メインスレッド（画面）に戻って結果を表示
                    withContext(Dispatchers.Main) {
                        showResult(resultText)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                        resetButtonState()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "エラーが発生しました: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    resetButtonState()
                }
            }
        }
    }

    // 結果を受け取った後の処理
    private fun showResult(dishName: String) {
        // 画面のメッセージを更新
        val txtMessage = view?.findViewById<TextView>(R.id.txtMessage)
        txtMessage?.text = "判定結果: $dishName"

        // トーストも出す
        Toast.makeText(context, "「$dishName」が見つかりました！", Toast.LENGTH_LONG).show()

        // ここで navigateToSearchResult(dishName) を呼べば、
        // その料理名でレシピ検索画面へ遷移できます

        // ボタンを元に戻す
        resetButtonState()
    }

    private fun resetButtonState() {
        val btnSearchAction = view?.findViewById<Button>(R.id.btnSearchAction)
        btnSearchAction?.isEnabled = true
        btnSearchAction?.text = "検索する"
    }

    // UriからBitmapに変換する補助関数
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}