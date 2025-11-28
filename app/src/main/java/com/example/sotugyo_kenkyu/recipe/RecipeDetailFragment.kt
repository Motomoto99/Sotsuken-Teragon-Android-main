package com.example.sotugyo_kenkyu.recipe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R

class RecipeDetailFragment : Fragment() {

    private lateinit var recipe: Recipe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // データを受け取る
        arguments?.let {
            recipe = it.getSerializable("RECIPE_DATA") as Recipe
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // ★レイアウトファイル (fragment_recipe_detail.xml) は後で作ってください
        return inflater.inflate(R.layout.fragment_recipe_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageFood: ImageView = view.findViewById(R.id.imageFoodDetail)
        val textTitle: TextView = view.findViewById(R.id.textTitleDetail)
        val textMaterial: TextView = view.findViewById(R.id.textMaterialDetail)
        val buttonWeb: Button = view.findViewById(R.id.buttonOpenWeb)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)

        // データをセット
        textTitle.text = recipe.recipeTitle
        textMaterial.text = recipe.recipeMaterial.joinToString("\n") // 材料を改行して表示

        Glide.with(this).load(recipe.foodImageUrl).placeholder(R.drawable.spinner_loader).into(imageFood)

        // 楽天レシピのWebサイトを開くボタン
        buttonWeb.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.recipeUrl))
            startActivity(intent)
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}