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
    private var targetCategoryId: String? = null // 検索するカテゴリID (例: "11" または "41,42,43")
    private var categoryName: String? = null     // 表示用タイトル

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
            // IDが渡されていれば検索開始
            fetchRecipesByCategoryId(targetCategoryId!!)
        } else {
            Toast.makeText(context, "カテゴリIDが指定されていません", Toast.LENGTH_SHORT).show()
        }
    }

    // ★ここを修正！カンマ区切りの複数IDに対応
    private fun fetchRecipesByCategoryId(categoryIdRaw: String) {
        // 1. カンマ区切りの文字列をリストに変換 (例: "41, 42" -> ["41", "42"])
        val targetIds = categoryIdRaw.split(",").map { it.trim() }

        Log.d("RecipeDebug", "★検索開始: 対象IDリスト = $targetIds")

        val collectionRef = db.collection("recipes")

        // 2. IDの数によってクエリを使い分ける
        val query = if (targetIds.size == 1) {
            // 通常検索（1つのIDを含むものを検索）
            collectionRef.whereArrayContains("categoryPathIds", targetIds[0])
        } else {
            // その他検索（リスト内のいずれかのIDを含むものを検索）
            // ※注意: Firestoreの制限で、このリストは最大10個までです
            collectionRef.whereArrayContainsAny("categoryPathIds", targetIds)
        }

        // 3. 検索実行
        query.get()
            .addOnSuccessListener { result ->
                val recipeList = ArrayList<Recipe>()
                for (document in result) {
                    try {
                        val recipe = document.toObject(Recipe::class.java)
                        recipeList.add(recipe)
                        Log.d("RecipeDebug", "〇 発見: ${recipe.recipeTitle}")
                    } catch (e: Exception) {
                        Log.e("RecipeDebug", "変換エラー", e)
                    }
                }

                if (recipeList.isEmpty()) {
                    Log.e("RecipeDebug", "× 0件でした。")
                    // 必要ならここで調査用関数を呼ぶ（複数IDの場合は最初のIDを使って調査）
                    if (targetIds.isNotEmpty()) {
                        investigateRealIds(targetIds[0])
                    }
                    Toast.makeText(context, "該当するレシピがありませんでした", Toast.LENGTH_SHORT).show()
                }

                // アダプターをセット
                val adapter = RecipeAdapter(
                    recipeList,
                    onFavoriteClick = { recipe ->
                        Toast.makeText(context, "お気に入り: ${recipe.recipeTitle}", Toast.LENGTH_SHORT).show()
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
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "読み込みエラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // データの中身を覗き見るための調査用関数（デバッグ用）
    private fun investigateRealIds(targetId: String) {
        val parentId = targetId.split("-").firstOrNull() ?: return
        Log.d("RecipeDebug", "親ID '$parentId' を持つデータを調査中...")

        db.collection("recipes")
            .whereArrayContains("categoryPathIds", parentId)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val ids = doc.get("categoryPathIds")
                    val title = doc.getString("recipeTitle")
                    Log.d("RecipeDebug", "調査結果 -> 料理: $title, ID: $ids")
                }
            }
    }
}