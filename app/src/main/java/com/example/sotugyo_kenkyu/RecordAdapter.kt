package com.example.sotugyo_kenkyu

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

class RecordAdapter(
    private val items: List<Record>
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTime: TextView = view.findViewById(R.id.textTime)
        val textFoodName: TextView = view.findViewById(R.id.textFoodName)
        val imageFood: ImageView = view.findViewById(R.id.imageFood)
        val container: View = view // クリック判定用
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = items[position]

        holder.textFoodName.text = item.menuName

        if (item.date != null) {
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
            holder.textTime.text = sdf.format(item.date.toDate())
        } else {
            holder.textTime.text = ""
        }

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.imageFood)
        } else {
            Glide.with(holder.itemView.context)
                .load(R.drawable.background_with_logo)
                .centerCrop()
                .into(holder.imageFood)
        }

        // クリックしたら詳細画面へ遷移
        holder.container.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RecordDetailActivity::class.java)

            intent.putExtra("RECORD_ID", item.id)
            intent.putExtra("USER_ID", item.userId)
            intent.putExtra("MENU_NAME", item.menuName)
            intent.putExtra("MEMO", item.memo)
            intent.putExtra("IMAGE_URL", item.imageUrl)
            intent.putExtra("IS_PUBLIC", item.isPublic)
            intent.putExtra("RATING", item.rating)
            if (item.date != null) {
                intent.putExtra("DATE_TIMESTAMP", item.date.toDate().time)
            }
            if (item.postedAt != null) {
                intent.putExtra("POSTED_TIMESTAMP", item.postedAt.toDate().time)
            }

            // ★重要: いいねリストを詳細画面へ渡す
            // (ArrayListに変換して渡します)
            intent.putStringArrayListExtra("LIKED_USER_IDS", ArrayList(item.likedUserIds))

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}