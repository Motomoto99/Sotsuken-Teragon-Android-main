package com.example.sotugyo_kenkyu.recipe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sotugyo_kenkyu.R

class RecipeSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_select)

        if (savedInstanceState == null) {
            // 検索入力画面を「選択モード」で表示
            val fragment = SearchInputFragment()
            val args = Bundle()
            args.putBoolean("IS_SELECTION_MODE", true)
            fragment.arguments = args

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}