package com.example.sotugyo_kenkyu.notification

import android.text.TextUtils
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
    private val iconMap: Map<String, String>,
    private val lastSeenDate: Date?
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // ★追加: 展開されているアイテムの位置を記憶するセット
    private val expandedPositions = mutableSetOf<Int>()

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val unreadMark: View = itemView.findViewById(R.id.viewUnreadMark)
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

        // ★★★ 追加・変更部分: タップで文章を展開する処理 ★★★
        val isExpanded = expandedPositions.contains(position)

        if (isExpanded) {
            // 展開されている場合：行数制限なし、省略記号なし
            holder.content.maxLines = Int.MAX_VALUE
            holder.content.ellipsize = null
        } else {
            // 閉じている場合：2行まで、末尾に「...」
            holder.content.maxLines = 2
            holder.content.ellipsize = TextUtils.TruncateAt.END
        }

        // アイテム全体をタップした時の動作
        holder.itemView.setOnClickListener {
            if (isExpanded) {
                expandedPositions.remove(position) // 閉じる
            } else {
                expandedPositions.add(position)    // 開く
            }
            // 変更があった行だけアニメーション付きで更新
            notifyItemChanged(position)
        }
        // ★★★ ここまで ★★★


        // 2. 未読マークの制御
        val notifDate = notification.date?.toDate()
        val threshold = lastSeenDate?.time ?: 0L

        if (notifDate != null && notifDate.time > threshold) {
            holder.unreadMark.visibility = View.VISIBLE
        } else {
            holder.unreadMark.visibility = View.INVISIBLE
        }

        // 3. アイコン設定 (運営 vs ユーザー)
        val isLikeNotification = notification.title.contains("いいね")

        if (isLikeNotification) {
            val senderUid = notification.senderUid
            val iconUrl = if (senderUid != null) iconMap[senderUid] else null
            if (!iconUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context).load(iconUrl).circleCrop().into(holder.icon)
            } else {
                Glide.with(holder.itemView.context).load(R.drawable.outline_account_circle_24).circleCrop().into(holder.icon)
            }
            holder.badge.visibility = View.VISIBLE
            holder.badge.setImageResource(R.drawable.ic_heart_filled)
        } else {
            Glide.with(holder.itemView.context).load(R.drawable.new_splash_icon).circleCrop().into(holder.icon)
            holder.badge.visibility = View.GONE
        }

        // 4. 日付変換
        if (notifDate != null) {
            holder.date.text = getTwitterStyleDate(notifDate)
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