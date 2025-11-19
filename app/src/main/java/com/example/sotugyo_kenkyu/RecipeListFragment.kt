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
        // デバッグログ：何を探そうとしているか表示
        Log.d("RecipeDebug", "★検索開始: ID='$categoryId' を含むレシピを探します")

        db.collection("recipes")
            .whereArrayContains("categoryPathIds", categoryId)
            .get()
            .addOnSuccessListener { result ->
                val recipeList = ArrayList<Recipe>()
                for (document in result) {
                    try {
                        val recipe = document.toObject(Recipe::class.java)
                        recipeList.add(recipe)
                        // 見つかったレシピのタイトルをログに出す
                        Log.d("RecipeDebug", "〇 発見: ${recipe.recipeTitle}")
                    } catch (e: Exception) {
                        Log.e("RecipeDebug", "変換エラー", e)
                    }
                }

                // ★ここが重要！
                // もし0件だった場合、そのカテゴリ（親ID）のデータを全件取ってきて、
                // 「本当はどんなIDが入っているのか」をログに出してカンニングします。
                if (recipeList.isEmpty()) {
                    Log.e("RecipeDebug", "× 0件でした。データの調査を開始します...")
                    investigateRealIds(categoryId) // 下で作る関数を呼び出す

                    // 画面には「ありません」を表示
                    Toast.makeText(context, "該当するレシピがありませんでした", Toast.LENGTH_SHORT).show()
                }

                val adapter = RecipeAdapter(
                    recipeList,
                    onFavoriteClick = { recipe ->
                        Toast.makeText(context, "お気に入り: ${recipe.recipeTitle}", Toast.LENGTH_SHORT).show()
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
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "読み込みエラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ★追加：データの中身を覗き見るための調査用関数
    private fun investigateRealIds(targetId: String) {
        // ターゲットIDのハイフンより前（親ID）を取得。例: "10-276" -> "10"
        val parentId = targetId.split("-").firstOrNull() ?: return

        Log.d("RecipeDebug", "親ID '$parentId' を持つデータを全検索して、正しいサブIDを調べます...")

        db.collection("recipes")
            .whereArrayContains("categoryPathIds", parentId)
            .limit(10) // 全部見ると多いので10件だけ
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val ids = doc.get("categoryPathIds") as? List<String>
                    val names = doc.get("categoryPathNames") as? List<String>
                    val title = doc.getString("recipeTitle")

                    Log.d("RecipeDebug", "--------------------------------------")
                    Log.d("RecipeDebug", "料理名: $title")
                    Log.d("RecipeDebug", "入っているID: $ids")
                    Log.d("RecipeDebug", "入っている名前: $names")
                }
                Log.d("RecipeDebug", "--------------------------------------")
                Log.d("RecipeDebug", "↑ このログに出てくるIDを SubCategoryFragment に書けば動きます！")
            }
    }
}