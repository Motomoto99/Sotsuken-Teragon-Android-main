package com.example.sotugyo_kenkyu.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val items: List<ChatSession>,
    private val onClick: (ChatSession) -> Unit,
    private val onDelete: (ChatSession) -> Unit
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

        val date = Date(session.updatedAt * 1000)
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        holder.textUpdatedAt.text = "更新: ${format.format(date)}"

        holder.itemView.setOnClickListener { onClick(session) }
        holder.buttonDelete.setOnClickListener { onDelete(session) }
    }

    override fun getItemCount(): Int = items.size
}