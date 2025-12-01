package com.example.sotugyo_kenkyu.recipe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.favorite.AddToFolderBottomSheet
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SearchResultFragment : Fragment(R.layout.fragment_search_result) {

    private lateinit var recipeAdapter: RecipeAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView

    // ★Algoliaのキーを設定
    private val appID = "3C1TM3RQXM"
    private val searchKey = "5968a7682ec81ac7ff865cdb36dea95e"
    private val indexName = "recipes"

    // ★OkHttpクライアント（これが新しい通信役！）
    private val client = OkHttpClient()
    private val myJsonParser = Json { ignoreUnknownKeys = true }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 前画面からのキーワード取得
        val initialKeyword = arguments?.getString("KEY_SEARCH_WORD") ?: ""

        val searchEditText = view.findViewById<EditText>(R.id.resultSearchEditText)
        val btnBack = view.findViewById<View>(R.id.btnBack)
        recyclerView = view.findViewById(R.id.recyclerResult)

        val topBarContainer = view.findViewById<View>(R.id.topBarContainer)
        ViewCompat.setOnApplyWindowInsetsListener(topBarContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // デザイン上の余白 16dp を計算
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()

            // 「ステータスバーの高さ」＋「16dp」を、白いバーの上のパディングに設定！
            // これで「時計の裏」まで白くなりつつ、検索バーは16dp下に下がるわ
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // UI初期設定
        searchEditText.setText(initialKeyword)

        // アダプターの準備（最初は空っぽで作成）
        recyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter(
            recipeList = emptyList(),
            onFavoriteClick = { recipe -> toggleFavorite(recipe) }, // お気に入り処理
            onItemClick = { recipe -> navigateToDetail(recipe) }    // 詳細画面へ
        )
        recyclerView.adapter = recipeAdapter

        // 戻るボタン
        btnBack.setOnClickListener {
            hideKeyboard(view)
            parentFragmentManager.popBackStack()
        }

        // ここでも再検索できるように
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val newKeyword = searchEditText.text.toString()
                performSearch(newKeyword)
                hideKeyboard(view)
                true
            } else {
                false
            }
        }

        // 最初の検索実行！
        performSearch(initialKeyword)
    }

    private fun performSearch(keyword: String) {
        if (keyword.isBlank()) return
        Log.d("Algolia", "検索開始: $keyword")

        // 通信なのでコルーチンを使う（ここは変わらない）
        lifecycleScope.launch {
            try {
                // 1. JSONを作る（デモと同じ！）
                val jsonBodyString = buildJsonObject {
                    put("query", keyword)
                }.toString() // ← 文字列にするのがポイント

                // 2. リクエストを作る（ここが新しい！）
                // AlgoliaのURLを直接指定するの
                val url = "https://$appID-dsn.algolia.net/1/indexes/$indexName/query"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-Algolia-API-Key", searchKey)
                    .addHeader("X-Algolia-Application-Id", appID)
                    .post(jsonBodyString.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                // 3. 通信実行！ (withContext(Dispatchers.IO) で裏方でやる)
                val responseString = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw Exception("通信エラー: ${response.code}")
                    response.body?.string() ?: ""
                }

                // 4. 結果をパースする（デモと同じ！）
                // decodeFromJsonElement ではなく decodeFromString を使うわ
                val algoliaResponse = myJsonParser.decodeFromString<AlgoliaResponse>(responseString)
                val hits = algoliaResponse.hits

                // 5. アプリのRecipe型に変換（前回教えたやつと同じ！）
                val recipeList = hits.map { item ->
                    Recipe(
                        recipeTitle = item.recipeTitle ?: "",
                        foodImageUrl = item.foodImageUrl ?: "",
                        recipeCost = item.recipeCost ?: "",
                        recipeIndication = item.recipeIndication ?: "",
                        recipeMaterial = item.recipeMaterial ?: emptyList(),
                        categoryPathNames = item.categoryPathNames ?: emptyList()
                    ).apply {
                        id = item.objectID
                    }
                }

                // 6. 表示更新
                recipeAdapter.updateData(recipeList)

                if (recipeList.isEmpty()) {
                    Toast.makeText(context, "見つかりませんでした", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("Algolia", "エラー", e)
                Toast.makeText(context, "検索失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ★お気に入り登録処理（RecipeListFragmentから移植）
    private fun toggleFavorite(recipe: Recipe) {
        val user = auth.currentUser ?: return
        val favoriteRef = db.collection("users")
            .document(user.uid)
            .collection("favorites")
            .document(recipe.id)

        if (recipe.isFavorite) { // UI上はすでにONになっている状態
            favoriteRef.set(recipe)
                .addOnSuccessListener {
                    Snackbar.make(recyclerView, "お気に入りに追加しました", Snackbar.LENGTH_LONG)
                        .setAction("フォルダへ") {
                            val bottomSheet = AddToFolderBottomSheet(recipe)
                            bottomSheet.show(parentFragmentManager, "AddToFolder")
                        }
                        .show()
                }
        } else {
            favoriteRef.delete()
        }
    }

    // 詳細画面への遷移
    private fun navigateToDetail(recipe: Recipe) {
        val fragment = RecipeDetailFragment() // ← 自分で作った詳細画面のFragmentクラス名にしてね
        val args = Bundle()
        args.putSerializable("RECIPE_DATA", recipe) // RecipeがSerializableかParcelableであること
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // ID確認！
            .addToBackStack(null)
            .commit()
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}