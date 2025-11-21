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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecipeListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var targetCategoryId: String? = null
    private var categoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetCategoryId = it.getString("CATEGORY_ID")
            categoryName = it.getString("CATEGORY_NAME")
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

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val titleText: TextView = view.findViewById(R.id.textPageTitle)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)

        titleText.text = categoryName ?: "検索結果"

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (targetCategoryId != null) {
            // コルーチンを使って非同期処理を行う
            CoroutineScope(Dispatchers.Main).launch {
                searchRecipesWithFavoriteStatus(targetCategoryId!!)
            }
        }
    }

    private suspend fun searchRecipesWithFavoriteStatus(categoryIdRaw: String) {
        val user = auth.currentUser
        if (user == null) return

        // 1. まずはレシピを普通に検索して取得
        val targetIds = categoryIdRaw.split(",").map { it.trim() }
        val collectionRef = db.collection("recipes")

        val query = if (targetIds.size == 1) {
            collectionRef.whereArrayContains("categoryPathIds", targetIds[0])
        } else {
            collectionRef.whereArrayContainsAny("categoryPathIds", targetIds)
        }

        try {
            // レシピ取得 (awaitで待機)
            val recipeSnapshot = withContext(Dispatchers.IO) { query.get().await() }
            val recipeList = ArrayList<Recipe>()

            for (document in recipeSnapshot) {
                val recipe = document.toObject(Recipe::class.java)
                // ★重要：ドキュメントIDをセットする
                recipe.id = document.id
                recipeList.add(recipe)
            }

            // 2. 次に、ユーザーの「お気に入りリスト」のIDだけを取得
            // これで「どれがお気に入り済みか」を確認する
            val myFavoritesSnapshot = withContext(Dispatchers.IO) {
                db.collection("users")
                    .document(user.uid)
                    .collection("favorites")
                    .get()
                    .await()
            }

            // お気に入りされているレシピIDのセット(集合)を作る
            val favoriteIds = myFavoritesSnapshot.documents.map { it.id }.toSet()

            // 3. 突き合わせ処理：お気に入りならフラグを立てる
            for (recipe in recipeList) {
                if (favoriteIds.contains(recipe.id)) {
                    recipe.isFavorite = true
                }
            }

            // 4. アダプターにセットして表示
            setupAdapter(recipeList)

        } catch (e: Exception) {
            Log.e("RecipeList", "Error", e)
            Toast.makeText(context, "読み込みエラーが発生しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAdapter(recipeList: List<Recipe>) {
        val adapter = RecipeAdapter(
            recipeList,
            onFavoriteClick = { recipe ->
                // 星が押されたときの処理（保存 or 削除）
                toggleFavorite(recipe)
            },
            onItemClick = { recipe ->
                // 詳細画面へ
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

    // お気に入りの登録・削除処理
    private fun toggleFavorite(recipe: Recipe) {
        val user = auth.currentUser ?: return

        // 保存先：users/{uid}/favorites/{recipeId}
        val favoriteRef = db.collection("users")
            .document(user.uid)
            .collection("favorites")
            .document(recipe.id) // レシピIDをそのままドキュメントIDにする

        if (recipe.isFavorite) {
            // ONになった -> Firestoreに保存
            // レシピデータをそのままコピーして保存しておくと、一覧表示が速い
            favoriteRef.set(recipe)
                .addOnFailureListener { e ->
                    Log.e("Fav", "保存失敗", e)
                }
        } else {
            // OFFになった -> Firestoreから削除
            favoriteRef.delete()
                .addOnFailureListener { e ->
                    Log.e("Fav", "削除失敗", e)
                }
        }
    }
}