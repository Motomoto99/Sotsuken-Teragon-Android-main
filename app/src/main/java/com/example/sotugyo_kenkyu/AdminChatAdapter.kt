package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AdminChatAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<AdminChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(notificationList[position])
    }

    override fun getItemCount(): Int = notificationList.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ★変更: title の取得と設定を削除
        private val content: TextView = itemView.findViewById(R.id.textContent)
        private val time: TextView = itemView.findViewById(R.id.textTime)

        fun bind(notification: Notification) {
            // title.text = ... (削除)
            content.text = notification.content

            if (notification.date != null) {
                val sdf = SimpleDateFormat("HH:mm", Locale.JAPAN)
                time.text = sdf.format(notification.date.toDate())
            } else {
                time.text = ""
            }
        }
    }
}