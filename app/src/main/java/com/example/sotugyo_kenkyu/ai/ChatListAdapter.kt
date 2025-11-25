package com.example.sotugyo_kenkyu.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

class ChatListAdapter(
    private val items: List<ChatSession>,
    private val onClick: (ChatSession) -> Unit,
    private val onDelete: (ChatSession) -> Unit      // ★ 追加
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    class ChatListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val textUpdatedAt: TextView = itemView.findViewById(R.id.textUpdatedAt)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_session, parent, false)
        return ChatListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val session = items[position]

        val title = if (session.title.isNotBlank()) session.title else "新しいチャット"
        holder.textTitle.text = title
        holder.textUpdatedAt.text = "更新: ${session.updatedAt}"

        holder.itemView.setOnClickListener {
            onClick(session)
        }
        holder.buttonDelete.setOnClickListener {
            onDelete(session)       // ★ ゴミ箱タップで削除処理へ
        }
    }

    override fun getItemCount(): Int = items.size
}

