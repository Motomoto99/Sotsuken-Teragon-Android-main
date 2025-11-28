package com.example.sotugyo_kenkyu.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.recipe.Recipe
import com.example.sotugyo_kenkyu.recipe.RecipeAdapter
import com.example.sotugyo_kenkyu.recipe.RecipeDetailFragment
import kotlinx.coroutines.launch

class FolderDetailFragment : Fragment() {

    private lateinit var targetFolder: RecipeFolder
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Serializableとして受け取る
            targetFolder = it.getSerializable("TARGET_FOLDER") as RecipeFolder
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 既存のレシピ一覧画面のレイアウトを使い回す
        return inflater.inflate(R.layout.fragment_recipe_list_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleText: TextView = view.findViewById(R.id.textPageTitle)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)

        // タイトル設定
        titleText.text = targetFolder.name

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = RecipeAdapter(
            recipeList,
            onFavoriteClick = { recipe ->
                // ★修正: 削除処理を実行するメソッドを呼ぶ
                removeRecipe(recipe)
            },
            onItemClick = { recipe ->
                // 詳細画面へ遷移
                val fragment = RecipeDetailFragment()
                val args = Bundle()
                args.putSerializable("RECIPE_DATA", recipe)
                fragment.arguments = args

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        recyclerView.adapter = adapter

        loadRecipes()
    }

    // ★追加: レシピを削除するメソッド
    private fun removeRecipe(recipe: Recipe) {
        lifecycleScope.launch {
            try {
                // ★修正前：条件分岐で片方だけ消していた
                /*
                if (targetFolder.id == "ALL_FAVORITES") {
                    FolderRepository.removeGlobalFavorite(recipe.id)
                } else {
                    FolderRepository.removeRecipeFromFolder(targetFolder.id, recipe.id)
                }
                */

                // ★修正後：どこから呼んでも「完全削除」を実行する
                FolderRepository.deleteRecipeCompletely(recipe.id)

                // 成功したらリストから消して画面更新
                recipeList.remove(recipe)
                adapter.notifyDataSetChanged()

                Toast.makeText(context, "お気に入りから削除しました", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "削除に失敗しました", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            try {
                val recipes = if (targetFolder.id == "ALL_FAVORITES") {
                    // 「すべて」の場合はお気に入り全件を取得
                    FolderRepository.getAllFavorites()
                } else {
                    // 特定のフォルダ内のレシピを取得
                    FolderRepository.getRecipesInFolder(targetFolder.id)
                }

                recipeList.clear()
                // 一覧表示用にすべて「お気に入り済み(true)」として扱う
                recipes.forEach { it.isFavorite = true }
                recipeList.addAll(recipes)
                adapter.notifyDataSetChanged()

                if (recipes.isEmpty()) {
                    Toast.makeText(context, "このフォルダは空です", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "読み込みエラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}