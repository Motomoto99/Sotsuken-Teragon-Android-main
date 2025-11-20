package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout // 追加
import androidx.core.view.ViewCompat // 追加
import androidx.core.view.WindowInsetsCompat // 追加
import androidx.core.view.updatePadding // 追加
import androidx.fragment.app.Fragment

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ★ここから修正：ホーム画面と同じ余白処理を追加
        val searchTopBar = view.findViewById<ConstraintLayout>(R.id.searchTopBar)

        ViewCompat.setOnApplyWindowInsetsListener(searchTopBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // XMLで設定した元のpaddingTop (16dp) を保持しつつ、ステータスバーの高さを足す
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()

            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }
        // ★修正ここまで

        // --- 以下、各ボタンの設定（前回と同じ） ---
        setupCategory(view, R.id.cat_meat, "肉", "10", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_fish, "魚", "11", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_vegetable, "野菜", "12", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_other, "その他の食材", "13", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_rice, "ご飯もの", "14", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_pasta, "パスタ", "15", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_noodle, "麺・粉物料理", "16", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_soup, "汁物・スープ", "17", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_salad, "サラダ", "18", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_sauce, "ソース・調味料・ドレッシング", "19", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_bento, "お弁当", "20", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_sweets, "お菓子", "21", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_bread, "パン", "22", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_nabe, "鍋料理", "23", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_event, "行事・イベント", "24", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_western, "西洋料理", "25", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_purpose, "その他の目的・シーン", "26", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_drink, "飲みもの", "27", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_popular, "人気メニュー", "30", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_classicmeat, "定番の肉料理", "31", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_classicfish, "定番の魚料理", "32", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_egg, "卵料理", "33", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_fruit, "果物", "34", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_soy, "大豆・豆腐", "35", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_easy, "簡単料理・時短", "36", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_save, "節約料理", "37", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_today, "今日の献立", "38", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_health, "健康料理", "39", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_utensils, "調理器具", "40", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_chinese, "中華料理", "41", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_korean, "韓国料理", "42", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_italian, "イタリア料理", "43", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_french, "フランス料理", "44", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_ethnic, "エスニック料理・中南米", "46", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_okinawa, "沖縄料理", "47", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_japanese, "日本各地の郷土料理", "48", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_osechi, "おせち料理", "49", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_xmas, "クリスマス", "50", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_hina, "ひな祭り", "51", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_spring, "春（3月～5月）", "52", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_summer, "夏（6月～8月）", "53", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_autumn, "秋（9月～11月）", "54", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_winter, "冬（12月～2月）", "55", R.drawable.outline_cottage_24)

    }

    private fun setupCategory(view: View, includeId: Int, name: String, id: String, iconRes: Int) {
        val layout = view.findViewById<View>(includeId)
        val textName = layout.findViewById<TextView>(R.id.textCategoryName)
        val icon = layout.findViewById<ImageView>(R.id.iconCategory)

        textName.text = name
        icon.setImageResource(iconRes)

        layout.setOnClickListener {
            // ★ここを変更！
            // RecipeListFragment ではなく SubCategoryFragment へ遷移
            val fragment = SubCategoryFragment()
            val args = Bundle()
            args.putString("PARENT_ID", id)
            args.putString("PARENT_NAME", name)
            fragment.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}