package com.example.sotugyo_kenkyu

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
import com.example.sotugyo_kenkyu.ai.PromptRepository
import com.example.sotugyo_kenkyu.recipe.SearchResultFragment
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class ImageResultFragment : Fragment() {

    private var selectedUriString: String? = null

    // 画像判定専用の GenerativeModel
    private val imageJudgeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        selectedUriString?.let {
            imageView.setImageURI(Uri.parse(it))
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSearchAction.setOnClickListener {
            val uriStr = selectedUriString ?: return@setOnClickListener

            btnSearchAction.isEnabled = false
            btnSearchAction.text = "解析中..."
            view.findViewById<TextView>(R.id.txtMessage).text = "AIに問い合わせています..."

            analyzeImageWithAiLogic(Uri.parse(uriStr))
        }
    }

    /**
     * Firebase AI Logic + PromptRepository を使って画像を解析
     */
    private fun analyzeImageWithAiLogic(uri: Uri) {
        lifecycleScope.launch {
            try {
                // ① プロンプトを PromptRepository から取得（料理名抽出用）
                val promptText = PromptRepository.getDishNamePrompt()

                // ② URI → Bitmap（IOスレッド）
                val bitmap = withContext(Dispatchers.IO) { uriToBitmap(uri) }
                if (bitmap == null) {
                    showError("画像の読み込みに失敗しました")
                    return@launch
                }

                // ③ 画像＋テキストの Content を作成
                val prompt = content {
                    image(bitmap)
                    text(promptText)
                }

                // ④ モデル呼び出し
                val response = withContext(Dispatchers.IO) {
                    imageJudgeModel.generateContent(prompt)
                }

                // ⑤ 結果テキストを TextPart として取り出す
                val dishName = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.mapNotNull { part ->
                        if (part is TextPart) part.text else null
                    }
                    ?.joinToString("\n")
                    ?.trim()
                    ?: "判定なし"

                showResult(dishName)

            } catch (e: Exception) {
                e.printStackTrace()
                showError("エラー: ${e.localizedMessage}")
            }
        }
    }

    // 解析結果(料理名)を受け取り、Algolia 検索画面へ遷移する
    private suspend fun showResult(dishName: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, "「$dishName」を検索します", Toast.LENGTH_SHORT).show()

        val fragment = SearchResultFragment()
        val args = Bundle()
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

    /**
     * 画像を適度に縮小しつつ Bitmap に変換
     */
    private suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            val resolver = requireContext().contentResolver
            inputStream = resolver.openInputStream(uri)

            // 1回目：サイズだけ取得
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDimension = 800
            var sampleSize = 1
            while ((options.outHeight / sampleSize) > maxDimension ||
                (options.outWidth / sampleSize) > maxDimension
            ) {
                sampleSize *= 2
            }

            // 2回目：実際にデコード
            val finalOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
            }
            inputStream = resolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, finalOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
        }
    }
}
