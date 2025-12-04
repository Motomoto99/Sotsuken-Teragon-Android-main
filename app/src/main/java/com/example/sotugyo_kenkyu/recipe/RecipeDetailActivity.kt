package com.example.sotugyo_kenkyu.recipe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sotugyo_kenkyu.R

class RecipeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 汎用的なフレームレイアウトを使用（専用のXMLファイルを作らなくて済みます）
        val frameLayout = android.widget.FrameLayout(this).apply {
            id = android.view.View.generateViewId()
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(frameLayout)

        if (savedInstanceState == null) {
            val recipe = intent.getSerializableExtra("RECIPE_DATA") as? Recipe
            if (recipe != null) {
                // 検索画面と同じFragmentを表示
                val fragment = RecipeDetailFragment()
                val args = Bundle()
                args.putSerializable("RECIPE_DATA", recipe)
                fragment.arguments = args

                supportFragmentManager.beginTransaction()
                    .replace(frameLayout.id, fragment)
                    .commit()
            }
        }
    }
}