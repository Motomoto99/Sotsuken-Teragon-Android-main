package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchTopBar = view.findViewById<ConstraintLayout>(R.id.searchTopBar)
        ViewCompat.setOnApplyWindowInsetsListener(searchTopBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerCategory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val categoryList = getCategoryData()

        recyclerView.adapter = CategoryAdapter(categoryList) { category ->
            // ★ クリック時の分岐処理
            if (category.isOther) {
                // 「その他」などは中分類を飛ばして、直接レシピ一覧へ（複数ID検索）
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
                // 通常のカテゴリは中分類画面へ
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

    private fun getCategoryData(): List<CategoryData> {
        val defaultImg = R.drawable.ic_launcher_background

        return listOf(
            // --- メインカテゴリ (中分類へ遷移) ---
            CategoryData("10", "お肉", null, "🍖"),
            CategoryData("11", "魚介", null, "🐟"),
            CategoryData("12", "野菜", null, "🥬"),
            CategoryData("14", "ご飯もの", null, "🍚"),
            CategoryData("15", "パスタ", null, "🍝"),
            CategoryData("16", "麺類", null, "🍜"),
            CategoryData("17", "スープ・汁物", null, "🥣"),
            CategoryData("18", "サラダ", null, "🥗"),
            CategoryData("23", "鍋料理", null, "🍲"),
            // ★お菓子とパンをメインに復帰
            CategoryData("21", "お菓子", null, "🍩"),
            CategoryData("22", "パン", null, "🍞"),

            // --- グループ系 (中分類画面を使ってリスト表示させるため isOther = false にする) ---
            // IDには数字ではなく、識別用の文字列 ("GROUP_WORLD" など) を入れます
            CategoryData(
                apiId = "GROUP_WORLD",
                name = "世界の料理",
                imageRes = null,
                emoji = "🌍",
                isOther = false // ★ falseにして SubCategoryFragment へ飛ばす
            ),

            CategoryData(
                apiId = "GROUP_EVENTS",
                name = "行事・イベント",
                imageRes = null,
                emoji = "🎉",
                isOther = false // ★ falseにして SubCategoryFragment へ飛ばす
            )
        )
    }
}