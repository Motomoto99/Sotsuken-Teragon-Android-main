package com.example.sotugyo_kenkyu.recipe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import android.app.Activity
import android.content.Intent
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchResultFragment : Fragment(R.layout.fragment_search_result) {

    private lateinit var recipeAdapter: RecipeAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView

    // ★Algoliaのキーを設定
    private val appID = "3C1TM3RQXM"
    private val searchKey = "5968a7682ec81ac7ff865cdb36dea95e"
    private val indexName = "recipes"

    // ★ページネーション用の変数
    private var currentPage = 0      // 今何ページ目か
    private var isLoading = false    // 読み込み中か（連打防止）
    private var isLastPage = false   // もうデータがないか
    private var currentKeyword = ""  // 今検索してる言葉
    private val hitsPerPage = 20     // 1回に取る件数

    private lateinit var historyManager: SearchHistoryManager

    // ★OkHttpクライアント（これが新しい通信役！）
    private val client = OkHttpClient()
    private val myJsonParser = Json { ignoreUnknownKeys = true }

    // ★追加: 選択モードフラグ
    private var isSelectionMode: Boolean = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 前画面からのキーワード取得
        val initialKeyword = arguments?.getString("KEY_SEARCH_WORD") ?: ""
        currentKeyword = initialKeyword
        // ★追加: 選択モードフラグ取得
        isSelectionMode = arguments?.getBoolean("IS_SELECTION_MODE") ?: false

        historyManager = SearchHistoryManager(requireContext())

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

        // ★2. 検索バー（またはチップエリア）が押されたら入力画面へ！
        // これがしょうたんのやりたかった「編集モードへの遷移」よ
        val openInputScreen = View.OnClickListener {
            val inputFragment = SearchInputFragment()
            val args = Bundle()

            // ★ポイント：arguments でデータを渡す（setFragmentResultは使わない）
            args.putString("EDIT_KEYWORD", currentKeyword)
            inputFragment.arguments = args

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, inputFragment)
                .addToBackStack(null) // ★履歴に残す（これで戻ったらここに来れる）
                .commit()
        }

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupResult)
        val searchBarClickArea = view.findViewById<View>(R.id.searchBarClickArea)

        // 1. キーワードを空白で区切ってチップを表示
        if (initialKeyword.isNotBlank()) {
            val words = initialKeyword.replace("　", " ").split(" ")

            words.forEach { word ->
                if (word.isNotBlank()) {
                    val chip = Chip(context)
                    chip.text = word

                    // ★修正：スタイルとバツボタンの設定
                    chip.isCheckable = false
                    chip.isClickable = true // 編集画面への遷移用
                    chip.isCloseIconVisible = true // ★バツボタンを表示！

                    // ★見た目の調整（参考画像っぽく薄いグレーに）
                    // 既存の色リソースがあればそれを使って。なければカラーコード指定でもOK
                    chip.setChipBackgroundColorResource(android.R.color.white) // 背景白（検索バーと同化させるなら）
                    // もしくは薄いグレーなら: chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F0F0F0")))

                    // 枠線を消す（フラットにする場合）
                    chip.chipStrokeWidth = 0f

                    // ★バツボタンが押された時の処理（再検索！）
                    chip.setOnCloseIconClickListener {
                        // 1. チップを画面から消す
                        chipGroup.removeView(chip)

                        // 2. 残っているチップの文字を集めて、新しい検索ワードを作る
                        val remainingWords = mutableListOf<String>()
                        for (i in 0 until chipGroup.childCount) {
                            val child = chipGroup.getChildAt(i) as? Chip
                            child?.let { remainingWords.add(it.text.toString()) }
                        }

                        if (remainingWords.isEmpty()) {
                            // 「空文字」を持たせて戻る！
                            val bundle = Bundle().apply {
                                putString("UPDATED_KEYWORD", "")
                            }
                            parentFragmentManager.setFragmentResult("SEARCH_INPUT_UPDATE", bundle)

                            // 前の画面（検索入力）に戻る
                            parentFragmentManager.popBackStack()
                        } else {
                            // まだワードが残っている場合
                            val newQuery = remainingWords.joinToString(" ")
                            currentKeyword = newQuery
                            // これをやっておかないと、画面を行き来した時に「最初の検索ワード」に戻っちゃうの
                            arguments?.putString("KEY_SEARCH_WORD", newQuery)
                            // 再検索実行
                            resetSearch(newQuery)
                        }
                    }

                    // チップ本体を押したら入力画面へ（既存の処理）
                    chip.setOnClickListener {
                        // 入力画面へ遷移する処理...（openInputScreenの中身と同じ）
                        // ここは既存の openInputScreen を呼べばOK
                        openInputScreen.onClick(it)
                    }

                    chipGroup.addView(chip)
                }
            }
        }



        searchBarClickArea.setOnClickListener(openInputScreen)
        // ChipGroup自体を押した時も反応するようにしておくと親切
        chipGroup.setOnClickListener(openInputScreen)


        // アダプターの準備（最初は空っぽで作成）
        recyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter(
            recipeList = emptyList(),
            onFavoriteClick = { recipe -> toggleFavorite(recipe) }, // お気に入り処理
            // ★変更: クリック時の処理をメソッドに委譲
            onItemClick = { recipe -> handleItemClick(recipe) }
        )
        recyclerView.adapter = recipeAdapter

        // 戻るボタン
        btnBack.setOnClickListener {
            hideKeyboard(view)
            parentFragmentManager.popBackStack()
        }

        // ★最初に検索するときはリセット
        resetSearch(initialKeyword)



        // ★【重要】スクロール検知リスナーを追加
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // 条件：
                // 1. 読み込み中じゃない
                // 2. まだ続きがある（LastPageじゃない）
                // 3. リストの一番下が見えた
                if (!isLoading && !isLastPage && totalItemCount <= (lastVisibleItem + 2)) {
                    // 次のページを読み込む！
                    currentPage++
                    performSearch(currentKeyword, isAppend = true)
                }
            }
        })


    }

    // ★検索状態をリセットして、最初から検索する関数
    private fun resetSearch(keyword: String) {
        currentKeyword = keyword
        currentPage = 0
        isLastPage = false
        performSearch(keyword, isAppend = false) // 上書きモード
    }

    private fun performSearch(keyword: String, isAppend: Boolean) {
        if (isLoading) return // 重複読み込みガード
        isLoading = true

        Log.d("Algolia", "検索: $keyword, ページ: $currentPage")

        // 通信なのでコルーチンを使う（ここは変わらない）
        lifecycleScope.launch {
            try {
                // 1. JSONを作る（デモと同じ！）
                val jsonBodyString = buildJsonObject {
                    put("query", keyword)
                    put("page", currentPage)       // ★ページ番号を指定
                    put("hitsPerPage", hitsPerPage) // ★1回の件数
                    // 意味: "もし完全一致（全部の単語を含む）が見つからなかったら、
                    //       単語を減らして（OR検索っぽくして）でも結果を返して！"
                    put("removeWordsIfNoResults", "allOptional")
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

                if (recipeList.isEmpty()) {
                    isLastPage = true // もう空っぽなら次は読まない
                }

                if (isAppend) {
                    // 2ページ目以降なら「継ぎ足し」
                    recipeAdapter.addData(recipeList)
                } else {
                    // 1ページ目なら「総入れ替え」
                    recipeAdapter.updateData(recipeList)
                    // 1ページ目なのに0件なら「見つかりません」
                    if (recipeList.isEmpty()) {
                        Toast.makeText(context, "見つかりませんでした", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("Algolia", "エラー", e)
                Toast.makeText(context, "検索失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }finally {
                // ★終わったらフラグを下ろす（重要！）
                isLoading = false
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

    // ★追加・修正: アイテムクリック時のハンドリング
    private fun handleItemClick(recipe: Recipe) {
        if (isSelectionMode) {
            // 選択モードなら、結果をセットしてActivityを終了
            val resultIntent = Intent()
            resultIntent.putExtra("SELECTED_RECIPE", recipe) // RecipeはSerializable
            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
            requireActivity().finish()
        } else {
            // 通常モードなら詳細画面へ遷移（既存の処理）
            navigateToDetail(recipe)
        }
    }

    // 詳細画面への遷移
    private fun navigateToDetail(recipe: Recipe) {
        val fragment = RecipeDetailFragment() // ← 自分で作った詳細画面のFragmentクラス名にしてね
        val args = Bundle()
        args.putSerializable("RECIPE_DATA", recipe) // RecipeがSerializableかParcelableであること
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, fragment) // ID確認！
            .addToBackStack(null)
            .commit()
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}