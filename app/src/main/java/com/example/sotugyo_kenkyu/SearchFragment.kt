package com.example.sotugyo_kenkyu

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

class SearchFragment : Fragment() {

    // 1. 画像選択の結果を受け取るランチャー
    // ギャラリーを開き、画像が選択されるとここに戻ってきます
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

        // --- ステータスバーの余白調整 ---
        val searchTopBar = view.findViewById<ConstraintLayout>(R.id.searchTopBar)
        ViewCompat.setOnApplyWindowInsetsListener(searchTopBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // --- カメラ検索ボタンの処理 ---
        val btnCameraSearch = view.findViewById<LinearLayout>(R.id.btnCameraSearch)
        btnCameraSearch.setOnClickListener {
            // 2. ボタンが押されたら画像選択画面(ギャラリー等)を開く
            pickImageLauncher.launch("image/*")
        }

        // --- カテゴリー一覧の設定 ---
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerCategory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val categoryList = getCategoryData()

        recyclerView.adapter = CategoryAdapter(categoryList) { category ->
            if (category.isOther) {
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

    // ★ 3. 画像検索結果画面への遷移処理（実装済み）
    private fun navigateToImageSearchResult(imageUri: Uri) {
        // 移動先のFragmentを作る (※ImageResultFragmentクラスを作成しておく必要があります)
        val fragment = ImageResultFragment()

        // 画像の情報を渡すためのバンドルを作る
        val args = Bundle()
        args.putString("IMAGE_URI", imageUri.toString()) // URIを文字列にして渡す
        fragment.arguments = args

        // 画面を切り替える
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun getCategoryData(): List<CategoryData> {
        val defaultImg = R.drawable.ic_launcher_background
        return listOf(
            CategoryData("10", "お肉", defaultImg, null),
            CategoryData("11", "魚介", defaultImg, null),
            CategoryData("12", "野菜", defaultImg, null),
            CategoryData("14", "ご飯もの", defaultImg, null),
            CategoryData("15", "パスタ", defaultImg, null),
            CategoryData("16", "麺類", defaultImg, null),
            CategoryData("17", "スープ・汁物", defaultImg, null),
            CategoryData("18", "サラダ", defaultImg, null),
            CategoryData("23", "鍋料理", defaultImg, null),
            CategoryData("21", "お菓子", defaultImg, null),
            CategoryData("22", "パン", defaultImg, null),
            CategoryData("GROUP_WORLD", "世界の料理", null, "🌍", false),
            CategoryData("GROUP_EVENTS", "行事・イベント", null, "🎉", false)
        )
    }
}