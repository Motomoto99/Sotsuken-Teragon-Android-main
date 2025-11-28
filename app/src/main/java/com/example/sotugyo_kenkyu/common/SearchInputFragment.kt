package com.example.sotugyo_kenkyu.common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.sotugyo_kenkyu.R
import androidx.core.view.updatePadding
import com.example.sotugyo_kenkyu.recipe.SearchHistoryManager
import com.example.sotugyo_kenkyu.recipe.SearchResultFragment

class SearchInputFragment : Fragment(R.layout.fragment_search_input) {

    private lateinit var historyManager: SearchHistoryManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyManager = SearchHistoryManager(requireContext())
        val editText = view.findViewById<EditText>(R.id.searchEditText)

        // ★ここで履歴リスト(RecyclerView)のアダプターを設定して表示する処理が入る
        // showHistoryList(historyManager.getHistory())

        // 検索実行時の処理
        fun doSearch(keyword: String) {
            if (keyword.isBlank()) return

            // 1. 履歴に保存
            historyManager.saveHistory(keyword)

            // 2. キーボード閉じる
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            // 3. 結果画面へ遷移
            val resultFragment = SearchResultFragment()
            val args = Bundle()
            args.putString("KEY_SEARCH_WORD", keyword)
            resultFragment.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, resultFragment) // addじゃなくてreplaceでもいいかも？仕様次第
                .addToBackStack(null)
                .commit()
        }

        // Enterキー監視
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(editText.text.toString())
                true
            } else {
                false
            }
        }

        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)

        val searchContainer = view.findViewById<View>(R.id.searchContainer)
        // ステータスバーの高さ分だけ、強制的にパディングを増やす処理
        ViewCompat.setOnApplyWindowInsetsListener(searchContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // XMLで指定した16dpなどをピクセルに変換して確保しておく
            // (もしXMLのpaddingTopを動的に変えたくないなら、単純に systemBars.top だけ足すのでもOK)
            // ここではHomeFragmentと同じ計算式にするわね
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()

            // 「ステータスバーの高さ」＋「デザイン上の余白」を設定
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // 1. 画面が開いたら自動でフォーカスしてキーボードを出す
        searchEditText.requestFocus()
        searchEditText.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        // 2. キーボードの「検索」ボタンが押されたら
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = searchEditText.text.toString()
                performSearch(keyword)
                true
            } else {
                false
            }
        }

        // 3. キャンセルボタンで戻る
        btnCancel.setOnClickListener {
            hideKeyboard(searchEditText)
            parentFragmentManager.popBackStack()
        }
    }

    private fun performSearch(keyword: String) {
        hideKeyboard(view)

        // ★ここに「結果画面」への遷移を書く
        // 今回はまだSearchResultFragmentを作ってないからログだけ
        android.util.Log.d("Search", "検索実行: $keyword")

        // 本来はこんな感じで遷移する
        /*
        val resultFragment = SearchResultFragment()
        val args = Bundle()
        args.putString("KEY_WORD", keyword)
        resultFragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, resultFragment)
            .addToBackStack(null)
            .commit()
        */
    }

    private fun hideKeyboard(view: View?) {
        view?.let {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}