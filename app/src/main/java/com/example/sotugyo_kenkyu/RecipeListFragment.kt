package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class RecipeListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private var targetCategoryId: String? = null // 検索するカテゴリID (例: "11")
    private var categoryName: String? = null     // 表示用タイトル (例: "魚介系")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 前の画面からIDと名前を受け取る
        arguments?.let {
            targetCategoryId = it.getString("CATEGORY_ID")
            categoryName = it.getString("CATEGORY_NAME")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // レイアウトファイルを読み込む
        return inflater.inflate(R.layout.fragment_recipe_list_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val titleText: TextView = view.findViewById(R.id.textPageTitle)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)

        // タイトル設定
        titleText.text = categoryName ?: "検索結果"

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (targetCategoryId != null) {
            fetchRecipesByCategoryId(targetCategoryId!!)
        } else {
            Toast.makeText(context, "カテゴリIDが指定されていません", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchRecipesByCategoryId(categoryId: String) {
        // ★重要: categoryPathIds配列の中に、指定したIDが含まれているか検索
        db.collection("recipes")
            .whereArrayContains("categoryPathIds", categoryId)
            .get()
            .addOnSuccessListener { result ->
                val recipeList = ArrayList<Recipe>()
                for (document in result) {
                    try {
                        val recipe = document.toObject(Recipe::class.java)
                        recipeList.add(recipe)
                    } catch (e: Exception) {
                        Log.e("RecipeList", "Parse Error", e)
                    }
                }

                if (recipeList.isEmpty()) {
                    Toast.makeText(context, "該当するレシピがありませんでした", Toast.LENGTH_SHORT).show()
                }

                // ★ここが変更点！
                // リスト項目タップ時の処理(onItemClick)を追加しています
                val adapter = RecipeAdapter(
                    recipeList,
                    onFavoriteClick = { recipe ->
                        // お気に入り処理（後で実装）
                        Toast.makeText(context, "お気に入り: ${recipe.recipeTitle}", Toast.LENGTH_SHORT).show()
                    },
                    onItemClick = { recipe ->
                        // ★詳細画面へ遷移する処理
                        val fragment = RecipeDetailFragment()
                        val args = Bundle()
                        // Recipeクラスは Serializable なのでそのまま渡せます
                        args.putSerializable("RECIPE_DATA", recipe)
                        fragment.arguments = args

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                )
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "読み込みエラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}