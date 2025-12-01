package com.example.sotugyo_kenkyu.recipe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

class SearchInputFragment : Fragment(R.layout.fragment_search_input) {

    private lateinit var historyManager: SearchHistoryManager
    private lateinit var historyAdapter: SearchHistoryAdapter // アダプターも変数にしておく

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 必要なViewを全部最初に見つける（整理整頓！）
        historyManager = SearchHistoryManager(requireContext())
        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val recyclerHistory = view.findViewById<RecyclerView>(R.id.recyclerHistory)
        val layoutEmptyHistory = view.findViewById<View>(R.id.layoutEmptyHistory) // 履歴なし時の表示
        val searchContainer = view.findViewById<View>(R.id.searchContainer)

        // 2. ステータスバーの重なり対策（パディング調整）
        ViewCompat.setOnApplyWindowInsetsListener(searchContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // 3. 履歴リストのセットアップ
        val historyList = historyManager.getHistory()

        // 履歴があるかないかで表示を切り替え
        if (historyList.isEmpty()) {
            recyclerHistory.visibility = View.GONE
            layoutEmptyHistory.visibility = View.VISIBLE
        } else {
            recyclerHistory.visibility = View.VISIBLE
            layoutEmptyHistory.visibility = View.GONE
        }

        recyclerHistory.layoutManager = LinearLayoutManager(context)
        // アダプターの初期化（履歴をタップしたときの処理もここに書く）
        historyAdapter = SearchHistoryAdapter(historyList) { clickedKeyword ->
            performSearch(clickedKeyword, view) // タップされたら検索実行！
        }
        recyclerHistory.adapter = historyAdapter


        // 4. キーボード自動表示
        searchEditText.requestFocus()
        searchEditText.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)


        // 5. 検索ボタン（エンターキー）の監視
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            // IME_ACTION_SEARCH または Enterキー押下を検知
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {

                val keyword = searchEditText.text.toString()
                performSearch(keyword, view) // 検索実行！
                true
            } else {
                false
            }
        }

        // 6. キャンセルボタン
        btnCancel.setOnClickListener {
            hideKeyboard(view)
            parentFragmentManager.popBackStack()
        }
    }

    // 検索実行のロジック（共通化）
    private fun performSearch(keyword: String, view: View) {
        if (keyword.isBlank()) return

        // 1. 履歴に保存＆リスト更新
        historyManager.saveHistory(keyword)
        // さっき作った updateData を使って表示を更新（これで履歴が増える！）
        historyAdapter.updateData(historyManager.getHistory())

        // 2. キーボードを閉じる
        hideKeyboard(view)

        // 3. 結果画面へ遷移
        val resultFragment = SearchResultFragment()
        val args = Bundle()
        // 受け取る側(SearchResultFragment)が決めたキー名と合わせること！
        args.putString("KEY_SEARCH_WORD", keyword)
        resultFragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, resultFragment)
            .addToBackStack(null)
            .commit()

        Log.d("Search", "検索実行: $keyword")
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}