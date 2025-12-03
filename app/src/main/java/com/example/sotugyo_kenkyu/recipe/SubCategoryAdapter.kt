package com.example.sotugyo_kenkyu.recipe

import android.graphics.Color // ★Colorクラスをインポート
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.sotugyo_kenkyu.R

class SubCategoryAdapter(
    private val categoryList: List<SubCategoryItem>,
    private val onItemClick: (String, String) -> Unit
) : RecyclerView.Adapter<SubCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textCategoryName)
        val iconContainer: View = view.findViewById(R.id.cardIconContainer)
        val imgPhoto: ImageView = view.findViewById(R.id.imgCategoryPhoto)
        val textEmoji: TextView = view.findViewById(R.id.textEmoji)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_button, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categoryList[position]

        holder.textName.text = item.name
        holder.iconContainer.visibility = View.VISIBLE

        if (item.imageFileName != null) {
            // --- 1. 画像指定がある場合 ---
            holder.textEmoji.visibility = View.GONE
            holder.imgPhoto.visibility = View.VISIBLE

            // アイコンのレイアウト調整
            holder.imgPhoto.scaleType = ImageView.ScaleType.FIT_CENTER
            val density = holder.itemView.context.resources.displayMetrics.density
            val paddingPx = (8 * density).toInt()
            holder.imgPhoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            // URL生成
            val storageBucket = "sotugyo-kenkyu-a0e36.firebasestorage.app"
            val folderName = "middle_category"
            val fileName = item.imageFileName
            val encodedPath = "$folderName%2F$fileName"
            val imageUrl = "https://firebasestorage.googleapis.com/v0/b/$storageBucket/o/$encodedPath?alt=media"

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    // ▼ 読み込み失敗時（絵文字に切り替え）
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.imgPhoto.visibility = View.GONE
                        holder.textEmoji.visibility = View.VISIBLE
                        holder.textEmoji.text = item.emoji

                        // ★修正: 背景を白にする
                        holder.textEmoji.setBackgroundColor(Color.WHITE)

                        return false
                    }

                    // ▼ 読み込み成功時
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.imgPhoto)

        } else {
            // --- 2. 画像指定がない場合（最初から絵文字） ---
            holder.imgPhoto.visibility = View.GONE
            holder.imgPhoto.setPadding(0, 0, 0, 0)

            holder.textEmoji.visibility = View.VISIBLE
            holder.textEmoji.text = item.emoji

            // ★修正: 背景を白にする（XMLのグレーを上書き）
            holder.textEmoji.setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item.id, item.name)
        }
    }

    override fun getItemCount(): Int = categoryList.size
}