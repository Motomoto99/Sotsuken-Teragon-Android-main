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
        setupCategory(view, R.id.cat_meat, "お肉系", "10", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_veg, "野菜系", "12", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_fish, "魚介系", "11", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_rice, "ごはんもの", "14", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_pasta, "パスタ", "15", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_noodle, "麺類", "16", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_soup, "スープ・汁物", "17", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_salad, "サラダ", "18", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_bread, "パン", "19", R.drawable.outline_cottage_24)
        setupCategory(view, R.id.cat_dessert, "お菓子", "21", R.drawable.outline_cottage_24)
    }

    private fun setupCategory(view: View, includeId: Int, name: String, id: String, iconRes: Int) {
        val layout = view.findViewById<View>(includeId)
        val textName = layout.findViewById<TextView>(R.id.textCategoryName)
        val icon = layout.findViewById<ImageView>(R.id.iconCategory)

        textName.text = name
        icon.setImageResource(iconRes)

        layout.setOnClickListener {
            val fragment = RecipeListFragment()
            val args = Bundle()
            args.putString("CATEGORY_ID", id)
            args.putString("CATEGORY_NAME", "$name レシピ")
            fragment.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}