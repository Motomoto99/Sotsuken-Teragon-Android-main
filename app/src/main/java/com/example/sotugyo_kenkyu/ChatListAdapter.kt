package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val items: List<ChatSession>,
    private val onClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    class ChatListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val textUpdatedAt: TextView = itemView.findViewById(R.id.textUpdatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_session, parent, false)
        return ChatListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val session = items[position]

        // タイトルが空ならデフォルト名
        val title = if (session.title.isNotBlank()) session.title else "新しいチャット"
        holder.textTitle.text = title

        // updatedAt は秒で持っているので、簡単に日付っぽく表示（本格的にやるなら SimpleDateFormat などを使う）
        holder.textUpdatedAt.text = "更新: ${session.updatedAt}"

        holder.itemView.setOnClickListener {
            onClick(session)
        }
    }

    override fun getItemCount(): Int = items.size
}
