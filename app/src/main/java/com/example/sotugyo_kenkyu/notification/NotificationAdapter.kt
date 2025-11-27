package com.example.sotugyo_kenkyu.notification

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R
import java.text.SimpleDateFormat
import java.util.Locale

// ★修正点1: ユーザー情報のマップ(ID -> 画像URL)を受け取るように変更
class NotificationAdapter(
    private val items: List<Notification>,
    private val userIconMap: Map<String, String>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iconNotificationSender)
        val title: TextView = view.findViewById(R.id.textTitle)
        val content: TextView = view.findViewById(R.id.textContent)
        val date: TextView = view.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.title.text = item.title
        holder.content.text = item.content

        if (item.date != null) {
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
            holder.date.text = sdf.format(item.date.toDate())
        } else {
            holder.date.text = ""
        }

        // ★修正点2: データベース通信を削除し、マップからURLを取得して表示
        if (item.senderUid != null) {
            // マップから画像URLを探す
            val photoUrl = userIconMap[item.senderUid]

            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(photoUrl)
                    .circleCrop()
                    .into(holder.icon)
                holder.icon.setPadding(0, 0, 0, 0)
            } else {
                // 画像がない場合のデフォルト
                holder.icon.setImageResource(R.drawable.outline_account_circle_24)
                holder.icon.setPadding(1, 1, 1, 1)
            }

        } else {
            // 運営からの通知
            holder.icon.setImageResource(R.drawable.ic_ai)
            holder.icon.setColorFilter(Color.parseColor("#4CAF50"))
            holder.icon.setPadding(8, 8, 8, 8)
        }
    }

    override fun getItemCount(): Int = items.size
}