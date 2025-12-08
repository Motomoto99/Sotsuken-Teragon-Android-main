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
    private lateinit var historyAdapter: SearchHistoryAdapter

    // ★追加: 選択モードかどうか
    private var isSelectionMode: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ★追加: Argumentsから選択モードを取得
        isSelectionMode = arguments?.getBoolean("IS_SELECTION_MODE") ?: false

        // 1. 必要なViewを全部最初に見つける
        historyManager = SearchHistoryManager(requireContext())
        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val recyclerHistory = view.findViewById<RecyclerView>(R.id.recyclerHistory)
        val layoutEmptyHistory = view.findViewById<View>(R.id.layoutEmptyHistory)
        val searchContainer = view.findViewById<View>(R.id.searchContainer)

        // 2. ステータスバーの重なり対策
        ViewCompat.setOnApplyWindowInsetsListener(searchContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        // 結果画面から「編集したい」と言って戻ってきた場合、その文字をセットする
        val editKeyword = arguments?.getString("EDIT_KEYWORD")
        if (!editKeyword.isNullOrEmpty()) {
            searchEditText.setText(editKeyword)
            // カーソルを末尾に移動（これがないと編集しにくい）
            searchEditText.setSelection(editKeyword.length)
        }

        // 3. 履歴リストのセットアップ
        val historyList = historyManager.getHistory()

        if (historyList.isEmpty()) {
            recyclerHistory.visibility = View.GONE
            layoutEmptyHistory.visibility = View.VISIBLE
        } else {
            recyclerHistory.visibility = View.VISIBLE
            layoutEmptyHistory.visibility = View.GONE
        }

        recyclerHistory.layoutManager = LinearLayoutManager(context)
        historyAdapter = SearchHistoryAdapter(historyList) { clickedKeyword ->
            performSearch(clickedKeyword, view)
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
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val keyword = searchEditText.text.toString()
                performSearch(keyword, view)
                true
            } else {
                false
            }
        }

        // "SEARCH_INPUT_UPDATE" という合言葉でデータが来たら、入力欄を更新するわ
        parentFragmentManager.setFragmentResultListener("SEARCH_INPUT_UPDATE", viewLifecycleOwner) { _, bundle ->
            val updatedKeyword = bundle.getString("UPDATED_KEYWORD")
            if (updatedKeyword != null) {
                searchEditText.setText(updatedKeyword)
                // カーソルを末尾に移動
                if (updatedKeyword.isNotEmpty()) {
                    searchEditText.setSelection(updatedKeyword.length)
                }
            }
        }

        // 6. キャンセルボタン（修正箇所）
        btnCancel.setOnClickListener {
            hideKeyboard(view)
            if (isSelectionMode) {
                // ★修正: 選択モード（レシピ選択画面）なら、Activityごと閉じる
                requireActivity().finish()
            } else {
                // 通常モード（ホーム画面からの検索）なら、前のFragmentに戻る
                parentFragmentManager.popBackStack()
            }
        }
    }

    // 検索実行のロジック
    private fun performSearch(keyword: String, view: View) {
        if (keyword.isBlank()) return

        // 1. 履歴に保存＆リスト更新
        historyManager.saveHistory(keyword)
        historyAdapter.updateData(historyManager.getHistory())
        // 2. キーボードを閉じる
        hideKeyboard(view)

        // 3. 結果画面へ遷移
        val resultFragment = SearchResultFragment()
        val args = Bundle()
        args.putString("KEY_SEARCH_WORD", keyword)
        // ★追加: 選択モードフラグを次の画面へ渡す
        args.putBoolean("IS_SELECTION_MODE", isSelectionMode)
        resultFragment.arguments = args

        val resultTag = "SEARCH_RESULT_TAG"

        // 「もし履歴の中に『SEARCH_RESULT_TAG』という名札がついた画面があったら、
        //  そこまで時間を巻き戻して（pop）、その画面自体も含めて（INCLUSIVE）消して！」
        parentFragmentManager.popBackStack(resultTag, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, resultFragment)
            .addToBackStack(resultTag)
            .commit()

        Log.d("Search", "検索実行: $keyword")
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}