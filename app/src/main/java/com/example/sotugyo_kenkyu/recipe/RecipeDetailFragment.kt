package com.example.sotugyo_kenkyu.recipe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R
import com.google.firebase.firestore.FirebaseFirestore
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.sotugyo_kenkyu.common.DataLoadingActivity // インポート追加
import com.example.sotugyo_kenkyu.home.HomeActivity

class RecipeDetailFragment : Fragment() {

    private var recipe: Recipe? = null
    private val db = FirebaseFirestore.getInstance()
    private val generationHelper = RecipeGenerationHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recipe = it.getSerializable("RECIPE_DATA") as? Recipe
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recipe_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topBar: ConstraintLayout = view.findViewById(R.id.topBar)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // XMLのpaddingTop="36dp"を上書きし、動的に「ステータスバー + 16dp」にします
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        val currentRecipe = recipe
        if (currentRecipe == null) {
            Toast.makeText(context, "レシピデータの読み込みに失敗しました", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        val imageFood: ImageView = view.findViewById(R.id.imageFoodDetail)
        val textTitle: TextView = view.findViewById(R.id.textTitleDetail)
        val textTimeCost: TextView = view.findViewById(R.id.textTimeCostDetail)
        val textMaterial: TextView = view.findViewById(R.id.textMaterialDetail)
        val textSteps: TextView = view.findViewById(R.id.textStepsDetail)
        val buttonWeb: Button = view.findViewById(R.id.buttonOpenWeb)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)
        val buttonAiArrange: Button = view.findViewById(R.id.buttonAiArrange)

        // --- 初期表示 ---
        textTitle.text = currentRecipe.recipeTitle
        textTimeCost.text = "読み込み中..."
        textMaterial.text = "読み込み中..."
        textSteps.text = "読み込み中..."

        Glide.with(this)
            .load(currentRecipe.foodImageUrl)
            .placeholder(R.drawable.spinner_loader)
            .into(imageFood)

        // --- Firestoreから最新データを取得（リアルタイム監視） ---
        val docId = currentRecipe.id
        if (docId.isNotEmpty()) {
            db.collection("recipes").document(docId)
                .addSnapshotListener { document, e ->
                    if (e != null) {
                        Log.e("Firestore", "Listen failed", e)
                        textSteps.text = "読み込みエラー: ${e.message}"
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        try {
                            val fetchedRecipe = document.toObject(Recipe::class.java)
                            if (fetchedRecipe != null) {
                                fetchedRecipe.id = docId
                                recipe = fetchedRecipe // 最新データで更新

                                // UI更新
                                updateUI(fetchedRecipe, textTimeCost, textMaterial, textSteps)

                                // 自動生成リクエスト (Helperを使用)
                                generationHelper.checkAndRequestGeneration(fetchedRecipe) { statusMessage ->
                                    textSteps.text = statusMessage
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Firestore", "Data conversion failed", e)
                            textSteps.text = "データ形式エラー"
                        }
                    } else {
                        textSteps.text = "データが見つかりませんでした"
                    }
                }
        } else {
            updateUI(currentRecipe, textTimeCost, textMaterial, textSteps)
        }

        buttonWeb.setOnClickListener {
            val url = recipe?.recipeUrl ?: ""
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        // ★追加: AIとアレンジボタンのクリックリスナー
        buttonAiArrange.setOnClickListener {
            val current = recipe
            if (current == null) return@setOnClickListener

            // 作り方がまだない（生成待ち）場合のガード
            val steps = current.recipeSteps
            val stepsText = current.recipeStepsText
            val hasSteps = (!steps.isNullOrEmpty()) || (!stepsText.isNullOrEmpty())

            if (!hasSteps) {
                Toast.makeText(context, "作り方を生成中です。少々お待ちください。", Toast.LENGTH_SHORT).show()
                // 必要であればここで再度生成リクエストを投げるなどの処理も可能ですが、
                // 基本はSnapshotListenerが更新してくれるのを待ちます。
                return@setOnClickListener
            }

            // ★修正: DataLoadingActivityを経由せず、直接HomeActivityへ遷移する
            val intent = Intent(requireContext(), HomeActivity::class.java)
            // レシピデータを渡す
            intent.putExtra("EXTRA_RECIPE_DATA", current)
            // 遷移先を指定するフラグ
            intent.putExtra("EXTRA_DESTINATION", "DESTINATION_AI_ARRANGE")

            // メッセージ設定
            intent.putExtra("EXTRA_LOADING_MESSAGE", "AIとアレンジを準備中です...")

            // ★追加: 重い読み込みをスキップする指示
            intent.putExtra("EXTRA_SKIP_DATA_LOAD", true)

            startActivity(intent)
            // 詳細画面は閉じる必要がなければそのままでOK（戻ってきたときに残っている方が自然）
        }
    }

    private fun updateUI(
        data: Recipe,
        timeCostView: TextView,
        materialView: TextView,
        stepsView: TextView
    ) {
        val time = if (data.recipeIndication.isNotEmpty()) data.recipeIndication else "-"
        val cost = if (data.recipeCost.isNotEmpty()) data.recipeCost else "-"
        timeCostView.text = "⏰ $time   💰 $cost"

        // ★修正: 材料と分量の表示
        val materials = data.recipeMaterial.orEmpty()
        val amounts = data.servingAmounts // Recipe.ktに追加されたフィールド

        if (materials.isNotEmpty()) {
            val builder = StringBuilder()
            for (i in materials.indices) {
                val materialName = materials[i]
                // 分量があれば結合して表示 (例: "・ 豚肉 ... 100g")
                val amountStr = if (i < amounts.size) " ... ${amounts[i]}" else ""
                builder.append("・ $materialName$amountStr\n")
            }
            materialView.text = builder.toString().trim()
        } else {
            materialView.text = "材料情報なし"
        }

        // 手順
        val steps = data.recipeSteps.orEmpty()
        if (steps.isNotEmpty()) {
            val stepsText = steps.mapIndexed { index, step ->
                "${index + 1}. $step"
            }.joinToString("\n\n")
            stepsView.text = stepsText
        } else if (!data.recipeStepsText.isNullOrEmpty()) {
            stepsView.text = data.recipeStepsText
        } else {
            // 何もない場合はHelperからのメッセージ待ちか初期状態
        }
    }
}