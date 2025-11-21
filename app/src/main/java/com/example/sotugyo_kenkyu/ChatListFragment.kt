package com.example.sotugyo_kenkyu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private val sessions = mutableListOf<ChatSession>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewChatList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ChatListAdapter(
            items = sessions,
            onClick = { session ->
                // ★ このチャットを今のアクティブチャットにする
                AiChatSessionManager.attachExistingChat(session.id)

                // AIタブに戻る
                parentFragmentManager.popBackStack()
            },
            onDelete = { session ->
                deleteChat(session)
            }
        )

        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val list = ChatRepository.loadChatSessions()
            sessions.clear()
            sessions.addAll(list)
            adapter.notifyDataSetChanged()
        }
    }

    private fun deleteChat(session: ChatSession) {
        AlertDialog.Builder(requireContext())
            .setTitle("削除しますか？")
            .setMessage(session.title)
            .setPositiveButton("削除") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    ChatRepository.deleteChat(session.id)

                    // 削除したチャットが現在のチャットだったら、新規状態に戻す
                    if (AiChatSessionManager.currentChatId == session.id) {
                        AiChatSessionManager.startNewSession()
                    }

                    sessions.remove(session)
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}
