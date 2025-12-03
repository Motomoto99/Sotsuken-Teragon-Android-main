package com.example.sotugyo_kenkyu.recipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

class SubCategoryAdapter(
    private val categoryList: List<SubCategoryItem>,
    private val onItemClick: (String, String) -> Unit
) : RecyclerView.Adapter<SubCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textCategoryName)

        // レイアウトファイルから取得（CardViewとしてではなくViewとして取得でOK）
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

        // ★背景色の設定を削除し、表示切り替えのみにしました

        // 1. アイコン枠を表示
        holder.iconContainer.visibility = View.VISIBLE

        // 2. 画像は隠す
        holder.imgPhoto.visibility = View.GONE

        // 3. 絵文字を表示
        holder.textEmoji.visibility = View.VISIBLE
        holder.textEmoji.text = item.emoji

        // 背景色をクリア（XMLのデフォルト背景色または白に戻すため念のため指定してもよいが、今回は指定なし）
        // holder.textEmoji.setBackgroundColor(...) のようなコードは削除

        holder.itemView.setOnClickListener {
            onItemClick(item.id, item.name)
        }
    }

    override fun getItemCount(): Int = categoryList.size
}