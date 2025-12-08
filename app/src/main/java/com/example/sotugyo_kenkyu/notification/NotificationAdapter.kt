package com.example.sotugyo_kenkyu.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R
import java.util.Date

class NotificationAdapter(
    private val notificationList: List<Notification>,
    private val iconMap: Map<String, String>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imgNotificationIcon)
        val title: TextView = itemView.findViewById(R.id.textNotificationTitle)
        val content: TextView = itemView.findViewById(R.id.textNotificationContent)
        val date: TextView = itemView.findViewById(R.id.textNotificationDate)
        val badge: ImageView = itemView.findViewById(R.id.imgNotificationTypeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]

        // 1. テキスト設定
        holder.title.text = notification.title
        holder.content.text = notification.content

        // 2. アイコン設定 (運営 vs ユーザー)
        val isLikeNotification = notification.title.contains("いいね")

        if (isLikeNotification) {
            // --- いいね通知の場合: ユーザーアイコンを表示 ---
            val senderUid = notification.senderUid
            val iconUrl = if (senderUid != null) iconMap[senderUid] else null

            if (!iconUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(iconUrl)
                    .circleCrop()
                    .into(holder.icon)
            } else {
                // アイコン未設定のユーザーは人型アイコン
                Glide.with(holder.itemView.context)
                    .load(R.drawable.outline_account_circle_24)
                    .circleCrop()
                    .into(holder.icon)
            }

            // 右下のバッジ（ハート）を表示
            holder.badge.visibility = View.VISIBLE
            holder.badge.setImageResource(R.drawable.ic_heart_filled)

        } else {
            // --- 運営からのお知らせの場合: new_splash_icon を表示 ---
            Glide.with(holder.itemView.context)
                .load(R.drawable.new_splash_icon) // ★ここを変更しました
                .circleCrop()
                .into(holder.icon)

            // バッジは非表示（または公式マークなどを出す）
            holder.badge.visibility = View.GONE
        }

        // 3. 日付変換
        val date = notification.date?.toDate()
        if (date != null) {
            holder.date.text = getTwitterStyleDate(date)
        } else {
            holder.date.text = ""
        }
    }

    override fun getItemCount(): Int = notificationList.size

    private fun getTwitterStyleDate(date: Date): String {
        val now = Date().time
        val diff = now - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "今"
            minutes < 60 -> "${minutes}分"
            hours < 24 -> "${hours}時間"
            days < 7 -> "${days}日"
            else -> java.text.SimpleDateFormat("MM/dd", java.util.Locale.JAPAN).format(date)
        }
    }
}