package com.example.sotugyo_kenkyu.favorite

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.constraintlayout.widget.ConstraintLayout // 追加
import androidx.core.view.ViewCompat // 追加
import androidx.core.view.WindowInsetsCompat // 追加
import androidx.core.view.updatePadding // 追加
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
            targetFolder = it.getSerializable("TARGET_FOLDER") as RecipeFolder
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recipe_list_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topBar = view.findViewById<ConstraintLayout>(R.id.topBar)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 元々のpaddingTop (16dp) にステータスバーの高さを足す
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        val titleText: TextView = view.findViewById(R.id.textPageTitle)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)

        titleText.text = targetFolder.name

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = RecipeAdapter(
            recipeList,
            onFavoriteClick = { recipe ->
                showActionDialog(recipe)
            },
            onItemClick = { recipe ->
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

    /**
     * メニュー表示のロジック
     */
    private fun showActionDialog(recipe: Recipe) {
        val isAllFavorites = (targetFolder.id == "ALL_FAVORITES")

        // ★修正: 文言と選択肢を要件に合わせて変更
        val options = if (isAllFavorites) {
            // すべてのお気に入り：「フォルダに追加」（コピー）
            arrayOf("フォルダに追加", "完全削除（すべてのお気に入りから解除）")
        } else {
            // 個別フォルダ：「フォルダに移動」（移動）
            arrayOf("このフォルダから外す", "フォルダに移動", "完全削除（すべてのお気に入りから解除）")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("操作を選択")
            .setItems(options) { _, which ->
                if (isAllFavorites) {
                    when (which) {
                        0 -> showAddToFolderDialog(recipe, null) // null = 移動元なし（コピー扱い）
                        1 -> deleteRecipeCompletely(recipe)
                    }
                } else {
                    when (which) {
                        0 -> removeRecipeFromThisFolder(recipe)
                        1 -> showAddToFolderDialog(recipe, targetFolder.id) // IDあり = 移動扱い
                        2 -> deleteRecipeCompletely(recipe)
                    }
                }
            }
            .setOnDismissListener {
                adapter.notifyDataSetChanged()
            }
            .show()
    }

    /**
     * ★修正: 移動元のフォルダID (sourceFolderId) を渡せるように変更
     * sourceFolderId が null なら「追加（コピー）」、あれば「移動」として扱います
     */
    private fun showAddToFolderDialog(recipe: Recipe, sourceFolderId: String?) {
        val sheet = AddToFolderBottomSheet(recipe, sourceFolderId)

        // 処理が終わって閉じたときにリストを更新する（移動した場合、この画面から消すため）
        sheet.onDismissListener = {
            loadRecipes()
        }

        sheet.show(parentFragmentManager, "AddToFolderBottomSheet")
    }

    private fun removeRecipeFromThisFolder(recipe: Recipe) {
        lifecycleScope.launch {
            try {
                FolderRepository.removeRecipeFromFolder(targetFolder.id, recipe.id)
                recipeList.remove(recipe)
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "フォルダから外しました", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteRecipeCompletely(recipe: Recipe) {
        lifecycleScope.launch {
            try {
                FolderRepository.deleteRecipeCompletely(recipe.id)
                recipeList.remove(recipe)
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "お気に入りを解除しました", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "削除失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            try {
                val recipes = if (targetFolder.id == "ALL_FAVORITES") {
                    FolderRepository.getAllFavorites()
                } else {
                    FolderRepository.getRecipesInFolder(targetFolder.id)
                }

                recipeList.clear()
                recipes.forEach { it.isFavorite = true }
                recipeList.addAll(recipes)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }
}