package com.example.sotugyo_kenkyu.ai

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox // ★追加
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    // ★追加: フィルタリング用チェックボックス
    private lateinit var checkFilterFavorite: CheckBox

    // 全データ保持用
    private val allSessions = mutableListOf<ChatSession>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.header)
        val buttonBack = view.findViewById<ImageButton>(R.id.buttonBack)
        checkFilterFavorite = view.findViewById(R.id.checkFilterFavorite) // ★取得

        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // ★追加: フィルター切り替え時の処理
        checkFilterFavorite.setOnCheckedChangeListener { _, isChecked ->
            updateListDisplay()
        }

        recyclerView = view.findViewById(R.id.recyclerViewChatList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ChatListAdapter(
            items = mutableListOf(), // 初期は空で渡す
            onClick = { session ->
                AiChatSessionManager.attachExistingChat(session.id)
                parentFragmentManager.popBackStack()
            },
            onDelete = { session ->
                deleteChat(session)
            },
            // ★追加: お気に入り処理
            onFavoriteClick = { session ->
                toggleFavorite(session)
            }
        )

        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val list = ChatRepository.loadChatSessions()
            allSessions.clear()
            allSessions.addAll(list)
            updateListDisplay() // データを反映
        }
    }

    /**
     * ★追加: 現在のフィルター設定に基づいてリストを更新表示
     */
    private fun updateListDisplay() {
        val showFavoritesOnly = checkFilterFavorite.isChecked
        val displayList = if (showFavoritesOnly) {
            allSessions.filter { it.isFavorite }
        } else {
            allSessions
        }
        adapter.updateData(displayList)
    }

    /**
     * ★追加: お気に入りの切り替え処理
     */
    private fun toggleFavorite(session: ChatSession) {
        val newStatus = !session.isFavorite

        // ローカルのデータを即時更新
        session.isFavorite = newStatus

        // 表示を更新
        updateListDisplay() // アダプター更新

        // Firestoreを更新
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ChatRepository.updateChatFavorite(session.id, newStatus)
            } catch (e: Exception) {
                // エラー時は元に戻すなどの処理が必要であれば記述
                e.printStackTrace()
            }
        }
    }

    private fun deleteChat(session: ChatSession) {
        AlertDialog.Builder(requireContext())
            .setTitle("削除しますか？")
            .setMessage(session.title)
            .setPositiveButton("削除") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    ChatRepository.deleteChat(session.id)

                    if (AiChatSessionManager.currentChatId == session.id) {
                        AiChatSessionManager.startNewSession()
                    }

                    allSessions.remove(session)
                    updateListDisplay() // 表示更新
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}