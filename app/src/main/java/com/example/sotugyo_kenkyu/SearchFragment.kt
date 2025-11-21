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
import androidx.recyclerview.widget.LinearLayoutManager // 追加
import androidx.recyclerview.widget.RecyclerView // 追加

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 余白処理（そのまま）
        val searchTopBar = view.findViewById<ConstraintLayout>(R.id.searchTopBar)
        ViewCompat.setOnApplyWindowInsetsListener(searchTopBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // 2. RecyclerViewのセットアップ
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerCategory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // データを準備（リスト形式でスッキリ！）
        val categoryList = getCategoryData()

        // Adapterをセット
        // 第2引数のラムダ式 { category -> ... } が、クリックされた時の処理になるよ
        recyclerView.adapter = CategoryAdapter(categoryList) { category ->
            // 画面遷移処理
            val fragment = SubCategoryFragment()
            val args = Bundle()
            args.putString("PARENT_ID", category.apiId)
            args.putString("PARENT_NAME", category.name)
            fragment.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // 戻るボタンで戻れるように
                .commit()
        }
    }

    // データを生成するメソッド（長くなるので分離）
    private fun getCategoryData(): List<CategoryData> {
        // iconは全部 outline_cottage_24 になっていたので統一してますが、
        // 必要なら個別に変更してね！
        val defaultIcon = R.drawable.outline_cottage_24

        return listOf(
            CategoryData("10", "肉", defaultIcon),
            CategoryData("11", "魚", defaultIcon),
            CategoryData("12", "野菜", defaultIcon),
            CategoryData("13", "その他の食材", defaultIcon),
            CategoryData("14", "ご飯もの", defaultIcon),
            CategoryData("15", "パスタ", defaultIcon),
            CategoryData("16", "麺・粉物料理", defaultIcon),
            CategoryData("17", "汁物・スープ", defaultIcon),
            CategoryData("18", "サラダ", defaultIcon),
            CategoryData("19", "ソース・調味料・ドレッシング", defaultIcon),
            CategoryData("20", "お弁当", defaultIcon),
            CategoryData("21", "お菓子", defaultIcon),
            CategoryData("22", "パン", defaultIcon),
            CategoryData("23", "鍋料理", defaultIcon),
            CategoryData("24", "行事・イベント", defaultIcon),
            CategoryData("25", "西洋料理", defaultIcon),
            CategoryData("26", "その他の目的・シーン", defaultIcon),
            CategoryData("27", "飲みもの", defaultIcon),
            CategoryData("30", "人気メニュー", defaultIcon),
            CategoryData("31", "定番の肉料理", defaultIcon),
            CategoryData("32", "定番の魚料理", defaultIcon),
            CategoryData("33", "卵料理", defaultIcon),
            CategoryData("34", "果物", defaultIcon),
            CategoryData("35", "大豆・豆腐", defaultIcon),
            CategoryData("36", "簡単料理・時短", defaultIcon),
            CategoryData("37", "節約料理", defaultIcon),
            CategoryData("38", "今日の献立", defaultIcon),
            CategoryData("39", "健康料理", defaultIcon),
            CategoryData("40", "調理器具", defaultIcon),
            CategoryData("41", "中華料理", defaultIcon),
            CategoryData("42", "韓国料理", defaultIcon),
            CategoryData("43", "イタリア料理", defaultIcon),
            CategoryData("44", "フランス料理", defaultIcon),
            CategoryData("46", "エスニック料理・中南米", defaultIcon),
            CategoryData("47", "沖縄料理", defaultIcon),
            CategoryData("48", "日本各地の郷土料理", defaultIcon),
            CategoryData("49", "おせち料理", defaultIcon),
            CategoryData("50", "クリスマス", defaultIcon),
            CategoryData("51", "ひな祭り", defaultIcon),
            CategoryData("52", "春（3月～5月）", defaultIcon),
            CategoryData("53", "夏（6月～8月）", defaultIcon),
            CategoryData("54", "秋（9月～11月）", defaultIcon),
            CategoryData("55", "冬（12月～2月）", defaultIcon)
        )
    }
}