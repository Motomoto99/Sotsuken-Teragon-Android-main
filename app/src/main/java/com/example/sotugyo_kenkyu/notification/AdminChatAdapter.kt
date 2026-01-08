package com.example.sotugyo_kenkyu.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // ★ 追加
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // ★ 追加
import com.example.sotugyo_kenkyu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminChatAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<AdminChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentItem = notificationList[position]

        // 日付ヘッダーを表示するか判定
        var showDateHeader = false
        if (position == 0) {
            showDateHeader = true
        } else {
            val prevItem = notificationList[position - 1]
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
        private val dateHeader: TextView = itemView.findViewById(R.id.textDateHeader)
        private val imageIcon: ImageView = itemView.findViewById(R.id.imageIcon) // ★ 追加

        fun bind(notification: Notification, showDateHeader: Boolean) {
            content.text = notification.content

            // ★★★ 追加: 運営アイコンを丸く表示 ★★★
            Glide.with(itemView.context)
                .load(R.drawable.new_splash_icon) // 運営アイコン画像
                .circleCrop() // 丸く切り抜く
                .into(imageIcon)

            if (notification.date != null) {
                val date = notification.date.toDate()

                // 時間のみ表示
                val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)
                time.text = timeFormat.format(date)

                // 日付ヘッダーの表示切り替え
                if (showDateHeader) {
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