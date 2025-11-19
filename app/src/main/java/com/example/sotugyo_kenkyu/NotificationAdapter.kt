package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // 1行分の見た目を生成
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    // データをViewにセット
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]
        holder.bind(notification)
    }

    // リストの件数を返す
    override fun getItemCount(): Int = notificationList.size

    // ViewHolderクラス
    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val content: TextView = itemView.findViewById(R.id.textContent)
        private val date: TextView = itemView.findViewById(R.id.textDate)

        fun bind(notification: Notification) {
            title.text = notification.title
            content.text = notification.content

            // 日付のフォーマット変換 (Timestamp -> String)
            if (notification.date != null) {
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
                date.text = sdf.format(notification.date.toDate())
            } else {
                date.text = ""
            }
        }
    }
}