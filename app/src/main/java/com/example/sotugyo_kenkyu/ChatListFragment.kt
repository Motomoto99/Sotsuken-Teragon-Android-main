package com.example.sotugyo_kenkyu

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewChatList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ChatListAdapter(sessions) { session ->
            openChat(session.id)
        }
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = ChatRepository.loadChatSessions()
                sessions.clear()
                sessions.addAll(list)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openChat(chatId: String) {
        val fragment = AiFragment().apply {
            arguments = Bundle().apply {
                putString("chatId", chatId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // ←あなたのActivityのコンテナIDに変更
            .addToBackStack(null)
            .commit()
    }
}
