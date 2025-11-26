package com.example.sotugyo_kenkyu.recipe

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.ImageResultFragment
import com.example.sotugyo_kenkyu.R

class SearchFragment : Fragment() {

    // ★★★ 画像選択の結果を受け取るランチャー ★★★
    // ポイント：この定義は必ず「クラスの直下（一番上）」に置いてください。
    // これで "Attempting to launch an unregistered..." エラーを防ぎます。
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // 画像が選択されたら、遷移処理メソッドを呼び出す
            navigateToImageSearchResult(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. ステータスバーの余白調整 ---
        val searchTopBar = view.findViewById<ConstraintLayout>(R.id.searchTopBar)
        ViewCompat.setOnApplyWindowInsetsListener(searchTopBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // --- 2. カメラ検索ボタンの処理 ---
        val btnCameraSearch = view.findViewById<LinearLayout>(R.id.btnCameraSearch)
        btnCameraSearch.setOnClickListener {
            // ボタンが押されたら画像選択画面(ギャラリー等)を開く
            pickImageLauncher.launch("image/*")
        }

        // --- 3. カテゴリー一覧の設定 ---
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerCategory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // データリストの取得（絵文字付き）
        val categoryList = getCategoryData()

        recyclerView.adapter = CategoryAdapter(categoryList) { category ->
            if (category.isOther) {
                // 中分類がないカテゴリ（レシピ一覧へ直行）
                val fragment = RecipeListFragment()
                val args = Bundle()
                args.putString("CATEGORY_ID", category.apiId)
                args.putString("CATEGORY_NAME", category.name)
                fragment.arguments = args

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                // 通常のカテゴリ（中分類画面へ）
                val fragment = SubCategoryFragment()
                val args = Bundle()
                args.putString("PARENT_ID", category.apiId)
                args.putString("PARENT_NAME", category.name)
                fragment.arguments = args

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    // --- 画像検索結果画面への遷移処理 ---
    private fun navigateToImageSearchResult(imageUri: Uri) {
        val fragment = ImageResultFragment()

        // 画像の情報を渡すためのバンドルを作る
        val args = Bundle()
        args.putString("IMAGE_URI", imageUri.toString())
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // --- カテゴリーデータの生成 ---
    private fun getCategoryData(): List<CategoryData> {
        return listOf(
            CategoryData("10", "お肉", null, "🍖"),
            CategoryData("11", "魚介", null, "🐟"),
            CategoryData("12", "野菜", null, "🥬"),
            CategoryData("14", "ご飯もの", null, "🍚"),
            CategoryData("15", "パスタ", null, "🍝"),
            CategoryData("16", "麺類", null, "🍜"),
            CategoryData("17", "スープ・汁物", null, "🥣"),
            CategoryData("18", "サラダ", null, "🥗"),
            CategoryData("23", "鍋料理", null, "🍲"),
            CategoryData("21", "お菓子", null, "🍩"),
            CategoryData("22", "パン", null, "🍞"),
            // グループ系設定（中分類なし）
            CategoryData("GROUP_WORLD", "世界の料理", null, "🌍", false),
            CategoryData("GROUP_EVENTS", "行事・イベント", null, "🎉", false)
        )
    }
}