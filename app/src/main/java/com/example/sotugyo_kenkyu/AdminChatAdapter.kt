package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date // Date型を使用するために追加

class AdminChatAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<AdminChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentItem = notificationList[position]

        // ★★★ 日付ヘッダーを表示するか判定 ★★★
        var showDateHeader = false
        if (position == 0) {
            // リストの最初は必ず日付を表示
            showDateHeader = true
        } else {
            val prevItem = notificationList[position - 1]
            // 1つ前のメッセージと日付が違うなら表示
            if (!isSameDate(currentItem.date?.toDate(), prevItem.date?.toDate())) {
                showDateHeader = true
            }
        }

        holder.bind(currentItem, showDateHeader)
    }

    // 日付が同じかチェックする関数
    private fun isSameDate(date1: Date?, date2: Date?): Boolean {
        if (date1 == null || date2 == null) return false
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.JAPAN)
        return sdf.format(date1) == sdf.format(date2)
    }

    override fun getItemCount(): Int = notificationList.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: TextView = itemView.findViewById(R.id.textContent)
        private val time: TextView = itemView.findViewById(R.id.textTime)
        private val dateHeader: TextView = itemView.findViewById(R.id.textDateHeader) // ★ 追加

        fun bind(notification: Notification, showDateHeader: Boolean) {
            content.text = notification.content

            if (notification.date != null) {
                val date = notification.date.toDate()

                // ★★★ 時間のみ表示 (例: 12:00) ★★★
                val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)
                time.text = timeFormat.format(date)

                // ★★★ 日付ヘッダーの表示切り替え ★★★
                if (showDateHeader) {
                    // 例: 2023/11/20(月)
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd(E)", Locale.JAPAN)
                    dateHeader.text = dateFormat.format(date)
                    dateHeader.visibility = View.VISIBLE
                } else {
                    dateHeader.visibility = View.GONE
                }

            } else {
                time.text = ""
                dateHeader.visibility = View.GONE
            }
        }
    }
}